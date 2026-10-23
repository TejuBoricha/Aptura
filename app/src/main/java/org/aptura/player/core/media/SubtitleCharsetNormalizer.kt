package org.aptura.player.core.media

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Normalizes subtitle files to clean UTF-8 without BOM.
 * Prevents Media3 parser crashes and accented character corruption on UTF-16, UTF-8-BOM,
 * and Windows-1252/ISO-8859-1 files.
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

        val text = decodeToCleanText(bytes, detectedCharset)
        val ext = sourceFile.extension.ifBlank { "srt" }
        val normalizedDir = File(cacheDir, "subtitles_normalized").apply { mkdirs() }
        val normalizedFile = File(normalizedDir, "norm_${sourceFile.nameWithoutExtension}.$ext")
        FileOutputStream(normalizedFile).use { fos ->
            fos.write(text.toByteArray(StandardCharsets.UTF_8))
        }

        return normalizedFile
    }

    fun normalizeBytesToUtf8File(bytes: ByteArray, displayName: String, cacheDir: File): File? {
        if (bytes.isEmpty()) return null
        val detectedCharset = detectCharset(bytes)
        if (detectedCharset == StandardCharsets.UTF_8 && !hasUtf8Bom(bytes)) {
            return null // Already clean UTF-8 without BOM; caller can use original URI directly
        }
        val text = decodeToCleanText(bytes, detectedCharset)
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "subtitle.srt" }
        val normalizedDir = File(cacheDir, "subtitles_normalized").apply { mkdirs() }
        val normalizedFile = File(normalizedDir, "norm_$safeName")
        FileOutputStream(normalizedFile).use { fos ->
            fos.write(text.toByteArray(StandardCharsets.UTF_8))
        }
        return normalizedFile
    }

    private fun decodeToCleanText(bytes: ByteArray, detectedCharset: Charset): String {
        val raw = when {
            hasUtf8Bom(bytes) -> String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
            hasUtf16Bom(bytes) -> String(bytes, 2, bytes.size - 2, detectedCharset)
            else -> String(bytes, detectedCharset)
        }
        return raw.removePrefix("\uFEFF")
    }

    private fun hasUtf8Bom(bytes: ByteArray): Boolean {
        return bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
    }

    internal fun hasUtf16Bom(bytes: ByteArray, length: Int = bytes.size): Boolean {
        if (length < 2) return false
        val isLe = bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()
        val isBe = bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()
        return isLe || isBe
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
