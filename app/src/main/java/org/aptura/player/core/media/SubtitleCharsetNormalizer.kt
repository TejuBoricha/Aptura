package org.aptura.player.core.media

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Normalizes subtitle files to clean UTF-8 without BOM.
 * Prevents Media3 parser crashes and accented character corruption on Windows-1252/ISO-8859-1 files.
 */
object SubtitleCharsetNormalizer {

    fun normalizeToUtf8(sourceFile: File, cacheDir: File): File {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return sourceFile

        val bytes = FileInputStream(sourceFile).use { it.readBytes() }
        if (bytes.size < 2) return sourceFile

        val detectedCharset = detectCharset(bytes)

        // If it's already clean UTF-8 without BOM, return source directly
        if (detectedCharset == StandardCharsets.UTF_8 && !hasUtf8Bom(bytes)) {
            return sourceFile
        }

        // Transcode to clean UTF-8
        val text = when {
            hasUtf8Bom(bytes) -> String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
            else -> String(bytes, detectedCharset)
        }

        val normalizedDir = File(cacheDir, "subtitles_normalized").apply { mkdirs() }
        val normalizedFile = File(normalizedDir, "norm_${sourceFile.nameWithoutExtension}.srt")
        FileOutputStream(normalizedFile).use { fos ->
            fos.write(text.toByteArray(StandardCharsets.UTF_8))
        }

        return normalizedFile
    }

    private fun hasUtf8Bom(bytes: ByteArray): Boolean {
        return bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        // Check 2-byte BOMs
        if (bytes.size >= 2) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
                return StandardCharsets.UTF_16LE
            }
            if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                return StandardCharsets.UTF_16BE
            }
        }

        // Check if bytes are valid UTF-8
        if (isValidUtf8(bytes)) {
            return StandardCharsets.UTF_8
        }

        // Fallback for non-UTF8 western subtitles (Spanish, French, German, Italian, Portuguese)
        return try {
            Charset.forName("windows-1252")
        } catch (_: Throwable) {
            StandardCharsets.ISO_8859_1
        }
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b in 0x00..0x7F -> i += 1
                b in 0xC2..0xDF -> {
                    if (i + 1 >= bytes.size) return false
                    val b2 = bytes[i + 1].toInt() and 0xFF
                    if (b2 !in 0x80..0xBF) return false
                    i += 2
                }
                b in 0xE0..0xEF -> {
                    if (i + 2 >= bytes.size) return false
                    val b2 = bytes[i + 1].toInt() and 0xFF
                    val b3 = bytes[i + 2].toInt() and 0xFF
                    if (b2 !in 0x80..0xBF || b3 !in 0x80..0xBF) return false
                    i += 3
                }
                b in 0xF0..0xF4 -> {
                    if (i + 3 >= bytes.size) return false
                    val b2 = bytes[i + 1].toInt() and 0xFF
                    val b3 = bytes[i + 2].toInt() and 0xFF
                    val b4 = bytes[i + 3].toInt() and 0xFF
                    if (b2 !in 0x80..0xBF || b3 !in 0x80..0xBF || b4 !in 0x80..0xBF) return false
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }
}
