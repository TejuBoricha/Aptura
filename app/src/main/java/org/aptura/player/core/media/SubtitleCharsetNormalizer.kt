package org.aptura.player.core.media

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Normalizes subtitle files to clean UTF-8 without BOM.
 * Prevents Media3 parser crashes and accented character corruption on UTF-16, UTF-8-BOM,
 * UTF-32, CJK (GB18030, Shift-JIS), Cyrillic (KOI8-R, Windows-1251), Arabic (Windows-1256),
 * and Windows-1252/ISO-8859-1 files (DATA-11).
 */
object SubtitleCharsetNormalizer {

    const val NORMALIZED_DIR_NAME = "subtitles_normalized"
    const val STALE_IMPORT_MS = 7L * 24L * 60L * 60L * 1000L

    val COMMON_SUBTITLE_CHARSETS: List<String> = listOf(
        "UTF-8",
        "UTF-16LE",
        "UTF-16BE",
        "UTF-32LE",
        "UTF-32BE",
        "GB18030",
        "Shift_JIS",
        "KOI8-R",
        "windows-1251",
        "windows-1256",
        "windows-1252"
    )

    fun normalizeToUtf8(sourceFile: File, cacheDir: File, overrideCharset: Charset? = null): File {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return sourceFile

        val bytes = FileInputStream(sourceFile).use { it.readBytes() }
        if (bytes.size < 2) return sourceFile

        val detectedCharset = overrideCharset ?: detectCharset(bytes)
        val isSubExtension = sourceFile.extension.equals("sub", ignoreCase = true)

        // If it's already clean UTF-8 without BOM, return source directly unless it's a .sub file needing MicroDVD conversion
        if (detectedCharset == StandardCharsets.UTF_8 && !hasUtf8Bom(bytes) && !isSubExtension) {
            return sourceFile
        }

        val text = decodeToCleanText(bytes, detectedCharset)
        val isConvertedToSrt = isSubExtension || text.startsWith("1\n00:")
        val ext = if (isConvertedToSrt) "srt" else sourceFile.extension.ifBlank { "srt" }
        val root = File(cacheDir, NORMALIZED_DIR_NAME).apply { mkdirs() }
        pruneStaleImports(root)
        val importDir = File(root, "norm_${UUID.randomUUID()}").apply { mkdirs() }
        val normalizedFile = File(importDir, "${sourceFile.nameWithoutExtension}.$ext")
        FileOutputStream(normalizedFile).use { fos ->
            fos.write(text.toByteArray(StandardCharsets.UTF_8))
        }

        return normalizedFile
    }

    fun normalizeBytesToUtf8File(bytes: ByteArray, displayName: String, cacheDir: File, overrideCharset: Charset? = null): File? {
        if (bytes.isEmpty()) return null
        val detectedCharset = overrideCharset ?: detectCharset(bytes)
        val isSubExtension = displayName.endsWith(".sub", ignoreCase = true)
        if (detectedCharset == StandardCharsets.UTF_8 && !hasUtf8Bom(bytes) && !isSubExtension) {
            return null // Already clean UTF-8 without BOM; caller can use original URI directly
        }
        val text = decodeToCleanText(bytes, detectedCharset)
        val isConvertedToSrt = isSubExtension || text.startsWith("1\n00:")
        val safeBaseName = if (isConvertedToSrt) {
            displayName.substringBeforeLast('.') + ".srt"
        } else {
            displayName
        }
        val safeName = safeBaseName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "subtitle.srt" }
        val root = File(cacheDir, NORMALIZED_DIR_NAME).apply { mkdirs() }
        pruneStaleImports(root)
        val importDir = File(root, "norm_${UUID.randomUUID()}").apply { mkdirs() }
        val normalizedFile = File(importDir, safeName)
        FileOutputStream(normalizedFile).use { fos ->
            fos.write(text.toByteArray(StandardCharsets.UTF_8))
        }
        return normalizedFile
    }

    internal fun pruneStaleImports(root: File, nowMs: Long = System.currentTimeMillis()) {
        val cutoff = nowMs - STALE_IMPORT_MS
        root.listFiles()?.forEach { file ->
            if (file.isDirectory && file.lastModified() < cutoff) {
                file.deleteRecursively()
            }
        }
    }

    private val MICRODVD_LINE_REGEX = Regex("""^\{(\d+)\}\{(\d+)\}(.*)$""")
    private val MICRODVD_TAG_REGEX = Regex("""\{[YyCcFfPpsS]:[^}]*\}""")

    fun isMicroDvd(text: String): Boolean {
        var matchCount = 0
        text.lineSequence().take(30).forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && MICRODVD_LINE_REGEX.matches(trimmed)) {
                matchCount++
            }
        }
        return matchCount >= 2
    }

    fun convertMicroDvdToSrt(text: String, fallbackFps: Double = 25.0): String {
        var fps = fallbackFps
        val srt = StringBuilder()
        var cueIndex = 1

        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val match = MICRODVD_LINE_REGEX.matchEntire(trimmed)
            if (match != null) {
                val startFrame = match.groupValues[1].toLongOrNull() ?: return@forEach
                val endFrame = match.groupValues[2].toLongOrNull() ?: return@forEach
                val rawContent = match.groupValues[3]

                // Check if first cue is an FPS specifier: e.g. {1}{1}25.000 or {0}{0}23.976
                if (startFrame <= 1 && endFrame <= 1 && cueIndex == 1) {
                    val parsedFps = rawContent.trim().toDoubleOrNull()
                    if (parsedFps != null && parsedFps > 0.0) {
                        fps = parsedFps
                        return@forEach
                    }
                }

                val startMs = (startFrame * 1000.0 / fps).toLong()
                val endMs = (endFrame * 1000.0 / fps).toLong()
                if (endMs >= startMs) {
                    val cleanedText = rawContent
                        .replace(MICRODVD_TAG_REGEX, "")
                        .replace('|', '\n')
                        .trim()

                    if (cleanedText.isNotEmpty()) {
                        srt.append(cueIndex++).append("\n")
                        srt.append(formatSrtTimestamp(startMs))
                            .append(" --> ")
                            .append(formatSrtTimestamp(maxOf(endMs, startMs + 500L)))
                            .append("\n")
                        srt.append(cleanedText).append("\n\n")
                    }
                }
            }
        }
        return srt.toString()
    }

    private fun formatSrtTimestamp(ms: Long): String {
        val totalSeconds = ms / 1000
        val millis = ms % 1000
        val seconds = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val minutes = totalMinutes % 60
        val hours = totalMinutes / 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    private fun decodeToCleanText(bytes: ByteArray, detectedCharset: Charset): String {
        val raw = when {
            hasUtf8Bom(bytes) -> String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
            hasUtf32Bom(bytes) -> String(bytes, 4, bytes.size - 4, detectedCharset)
            hasUtf16Bom(bytes) -> String(bytes, 2, bytes.size - 2, detectedCharset)
            else -> String(bytes, detectedCharset)
        }
        val clean = raw.removePrefix("\uFEFF")
        return if (isMicroDvd(clean)) {
            convertMicroDvdToSrt(clean)
        } else {
            clean
        }
    }

    private fun hasUtf8Bom(bytes: ByteArray): Boolean {
        return bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
    }

    internal fun hasUtf32Bom(bytes: ByteArray, length: Int = bytes.size): Boolean {
        if (length < 4) return false
        val isBe = bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() && bytes[2] == 0xFE.toByte() && bytes[3] == 0xFF.toByte()
        val isLe = bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() && bytes[2] == 0x00.toByte() && bytes[3] == 0x00.toByte()
        return isBe || isLe
    }

    internal fun hasUtf16Bom(bytes: ByteArray, length: Int = bytes.size): Boolean {
        if (length < 2 || hasUtf32Bom(bytes, length)) return false
        val isLe = bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()
        val isBe = bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()
        return isLe || isBe
    }

    internal fun isUtf16OrUtf32(bytes: ByteArray, length: Int = bytes.size): Boolean {
        if (hasUtf32Bom(bytes, length) || hasUtf16Bom(bytes, length)) return true
        return detectBomlessUnicodeCharset(bytes, length) != null
    }

    internal fun detectBomlessUnicodeCharset(bytes: ByteArray, length: Int = bytes.size): Charset? {
        val sampleSize = minOf(length, bytes.size, 400)
        // Check for UTF-32 without BOM (3 nulls out of every 4 bytes for ASCII text in SRT/VTT)
        if (sampleSize >= 8 && sampleSize % 4 == 0) {
            val n = sampleSize / 4
            val c0 = (0 until sampleSize step 4).count { bytes[it] == 0.toByte() }
            val c1 = (1 until sampleSize step 4).count { bytes[it] == 0.toByte() }
            val c2 = (2 until sampleSize step 4).count { bytes[it] == 0.toByte() }
            val c3 = (3 until sampleSize step 4).count { bytes[it] == 0.toByte() }
            if (c0 > n * 0.7 && c1 > n * 0.7 && c2 > n * 0.7 && c3 < n * 0.1) {
                return Charset.forName("UTF-32BE")
            }
            if (c1 > n * 0.7 && c2 > n * 0.7 && c3 > n * 0.7 && c0 < n * 0.1) {
                return Charset.forName("UTF-32LE")
            }
        }

        // Check for UTF-16 without BOM (alternating nulls for ASCII text in SRT/VTT)
        if (sampleSize >= 4 && sampleSize % 2 == 0) {
            val n = sampleSize / 2
            val zEven = (0 until sampleSize step 2).count { bytes[it] == 0.toByte() }
            val zOdd = (1 until sampleSize step 2).count { bytes[it] == 0.toByte() }
            if (zEven > n * 0.7 && zOdd < n * 0.1) {
                return StandardCharsets.UTF_16BE
            }
            if (zOdd > n * 0.7 && zEven < n * 0.1) {
                return StandardCharsets.UTF_16LE
            }
        }
        return null
    }

    internal fun detectCharset(bytes: ByteArray): Charset {
        // 1. Check 4-byte BOMs (UTF-32)
        if (bytes.size >= 4) {
            if (bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() && bytes[2] == 0xFE.toByte() && bytes[3] == 0xFF.toByte()) {
                return Charset.forName("UTF-32BE")
            }
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() && bytes[2] == 0x00.toByte() && bytes[3] == 0x00.toByte()) {
                return Charset.forName("UTF-32LE")
            }
        }

        // 2. Check 3-byte BOM (UTF-8)
        if (hasUtf8Bom(bytes)) {
            return StandardCharsets.UTF_8
        }

        // 3. Check 2-byte BOMs (UTF-16)
        if (bytes.size >= 2) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
                return StandardCharsets.UTF_16LE
            }
            if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                return StandardCharsets.UTF_16BE
            }
        }

        // 4. Sample-based heuristics for non-BOM multi-byte Unicode encodings
        detectBomlessUnicodeCharset(bytes)?.let { return it }

        // 5. Check if bytes are valid UTF-8
        if (isValidUtf8(bytes)) {
            return StandardCharsets.UTF_8
        }

        // Advance to first non-ASCII byte to avoid sampling purely ASCII headers (e.g. [Script Info] in SSA/ASS)
        val firstNonAscii = bytes.indexOfFirst { (it.toInt() and 0xFF) >= 0x80 }
        val sample = if (firstNonAscii != -1) {
            val end = minOf(bytes.size, firstNonAscii + 1024)
            bytes.copyOfRange(firstNonAscii, end)
        } else {
            if (bytes.size > 400) bytes.copyOfRange(0, 400) else bytes
        }

        // 6. Shift-JIS (Japanese): check for Hiragana (0x82 0x9F..0xF1) and Katakana (0x83 0x40..0x96)
        var sjisKana = 0
        var idx = 0
        while (idx < sample.size - 1) {
            val b1 = sample[idx].toInt() and 0xFF
            val b2 = sample[idx + 1].toInt() and 0xFF
            if ((b1 == 0x82 && b2 in 0x9F..0xF1) || (b1 == 0x83 && b2 in 0x40..0x96)) {
                sjisKana++
                idx += 2
            } else {
                idx++
            }
        }
        if (sjisKana >= 2) {
            return Charset.forName("Shift_JIS")
        }

        // 7. Check Arabic (Windows-1256) via Alif-Lam bigram "ال" (0xC7 0xE1)
        var arabicAlifLamCount = 0
        idx = 0
        while (idx < sample.size - 1) {
            val b1 = sample[idx].toInt() and 0xFF
            val b2 = sample[idx + 1].toInt() and 0xFF
            if (b1 == 0xC7 && b2 == 0xE1) {
                arabicAlifLamCount++
                idx += 2
            } else {
                idx++
            }
        }
        if (arabicAlifLamCount >= 2) {
            try {
                return Charset.forName("windows-1256")
            } catch (_: Throwable) {
                // Ignore fallback
            }
        }

        // 8. Check for single-byte Cyrillic (KOI8-R, Windows-1251) vs Arabic (Windows-1256) vs multi-byte Chinese (GB18030)
        val runs = mutableListOf<Int>()
        var curRun = 0
        var countE0FF = 0
        var countC0DF = 0
        for (b in sample) {
            val u = b.toInt() and 0xFF
            if (u >= 0x80) {
                curRun++
                if (u in 0xE0..0xFF) countE0FF++
                if (u in 0xC0..0xDF) countC0DF++
            } else {
                if (curRun > 0) {
                    runs.add(curRun)
                    curRun = 0
                }
            }
        }
        if (curRun > 0) runs.add(curRun)

        val avgRun = if (runs.isNotEmpty()) runs.average() else 0.0
        val hasOddLongRuns = runs.any { it >= 3 && it % 2 != 0 }

        if (hasOddLongRuns && avgRun >= 3.0) {
            // Natural Russian text consists mostly of lowercase letters (>80%):
            // In Windows-1251: lowercase is 0xE0..0xFF
            // In KOI8-R: lowercase is 0xC0..0xDF
            // In Arabic Windows-1256: letters are balanced between 0xC0..0xDF and 0xE0..0xFF
            return when {
                countC0DF > countE0FF * 1.5 -> {
                    try {
                        Charset.forName("KOI8-R")
                    } catch (_: Throwable) {
                        Charset.forName("windows-1251")
                    }
                }
                countE0FF > countC0DF * 1.5 -> {
                    try {
                        Charset.forName("windows-1251")
                    } catch (_: Throwable) {
                        Charset.forName("KOI8-R")
                    }
                }
                arabicAlifLamCount >= 1 || (countC0DF >= 5 && countE0FF >= 5) -> {
                    try {
                        Charset.forName("windows-1256")
                    } catch (_: Throwable) {
                        Charset.forName("windows-1252")
                    }
                }
                else -> Charset.forName("windows-1251")
            }
        }

        // 9. GB18030 / GBK (Chinese): check for Hanzi pairs in 0xA1..0xFE
        var gbPairs = 0
        idx = 0
        while (idx < sample.size - 1) {
            val b1 = sample[idx].toInt() and 0xFF
            val b2 = sample[idx + 1].toInt() and 0xFF
            if (b1 in 0xA1..0xFE && b2 in 0xA1..0xFE) {
                gbPairs++
                idx += 2
            } else {
                idx++
            }
        }
        if (gbPairs >= 3 && !hasOddLongRuns) {
            return Charset.forName("GB18030")
        }

        // 10. Fallback for non-UTF8 western subtitles (Spanish, French, German, Italian, Portuguese)
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
