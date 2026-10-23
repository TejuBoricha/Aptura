package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.net.URLDecoder

sealed class SubtitleValidationResult {
    data class Valid(
        val displayName: String,
        val mimeType: String,
        val formatDescription: String,
        val validatedUri: Uri? = null
    ) : SubtitleValidationResult()

    data class Invalid(
        val reason: String
    ) : SubtitleValidationResult()
}

/**
 * Defensive validator for external subtitle file importation.
 * Supports direct subtitle files (.srt, .vtt, .ass, .ssa, .sub, .ttml, .dfxp) and subtitle-packed .zip archives.
 * Defends against binary injection and normalizes non-standard charsets (UTF-16, UTF-8-BOM, Windows-1252).
 */
object SubtitleFileValidator {

    private val REJECTED_EXTENSIONS = mapOf(
        "png" to "Image file",
        "jpg" to "Image file",
        "jpeg" to "Image file",
        "webp" to "Image file",
        "gif" to "Image file",
        "bmp" to "Image file",
        "mp4" to "Video file",
        "mkv" to "Video file",
        "avi" to "Video file",
        "mov" to "Video file",
        "webm" to "Video file",
        "mp3" to "Audio file",
        "wav" to "Audio file",
        "flac" to "Audio file",
        "m4a" to "Audio file",
        "pdf" to "PDF document",
        "apk" to "Android package",
        "rar" to "Rar archive (please extract to .srt first)",
        "7z" to "7-Zip archive (please extract to .srt first)",
        "tar" to "Tar archive"
    )

    fun validate(context: Context, uri: Uri): SubtitleValidationResult {
        // 1. Resolve real display name from ContentResolver or URI
        val resolvedName = resolveDisplayName(context, uri)
        val extension = resolvedName.substringAfterLast('.', "").lowercase()

        // 2. Check for Zip Archive Support
        val contentMime = try {
            context.contentResolver.getType(uri)
        } catch (_: Throwable) {
            null
        }

        val isZip = extension == "zip" || contentMime == "application/zip" || contentMime == "application/x-zip-compressed"
        if (isZip) {
            return when (val zipResult = SubtitleZipExtractor.extractSubtitleFromZip(context, uri)) {
                is ZipExtractionResult.Success -> {
                    val normalizedFile = SubtitleCharsetNormalizer.normalizeToUtf8(zipResult.extractedFile, context.cacheDir)
                    SubtitleValidationResult.Valid(
                        displayName = zipResult.entryName,
                        mimeType = zipResult.mimeType,
                        formatDescription = "Extracted from Zip",
                        validatedUri = Uri.fromFile(normalizedFile)
                    )
                }
                is ZipExtractionResult.Failure -> {
                    SubtitleValidationResult.Invalid(zipResult.reason)
                }
            }
        }

        if (contentMime != null) {
            when {
                contentMime.startsWith("image/") -> {
                    return SubtitleValidationResult.Invalid("Selected file is an image ($resolvedName). Please choose a valid .srt, .vtt, or .ass subtitle file.")
                }
                contentMime.startsWith("video/") -> {
                    return SubtitleValidationResult.Invalid("Selected file is a video ($resolvedName). Please choose a subtitle file.")
                }
                contentMime.startsWith("audio/") -> {
                    return SubtitleValidationResult.Invalid("Selected file is an audio track ($resolvedName). Please choose a subtitle file.")
                }
                contentMime == "application/pdf" -> {
                    return SubtitleValidationResult.Invalid("Selected file is a PDF document ($resolvedName).")
                }
            }
        }

        // 3. Check Extension
        if (REJECTED_EXTENSIONS.containsKey(extension)) {
            val typeDesc = REJECTED_EXTENSIONS[extension]
            return SubtitleValidationResult.Invalid("Cannot import $typeDesc ($resolvedName). Please select a .srt, .vtt, or .ass subtitle file.")
        }

        // 4. Inspect file header / magic bytes (detect binary files masquerading as text)
        val rawBytes: ByteArray = try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return SubtitleValidationResult.Invalid("Cannot open file stream for $resolvedName.")
            stream.use { s ->
                val bytes = s.readBytes()
                if (bytes.isEmpty()) {
                    return SubtitleValidationResult.Invalid("Selected subtitle file ($resolvedName) is empty.")
                }
                val checkLen = minOf(bytes.size, 512)
                if (isBinaryOrImage(bytes, checkLen)) {
                    return SubtitleValidationResult.Invalid("Selected file ($resolvedName) contains binary or image data, not valid subtitle text.")
                }
                bytes
            }
        } catch (e: Exception) {
            return SubtitleValidationResult.Invalid("Unable to read file: ${e.message ?: "Access denied"}")
        }

        // 5. Normalize non-UTF-8 / BOM-prefixed subtitle files so Media3 parsers receive clean UTF-8
        val normalizedFile = try {
            SubtitleCharsetNormalizer.normalizeBytesToUtf8File(rawBytes, resolvedName, context.cacheDir)
        } catch (_: Throwable) {
            null
        }
        val effectiveUri = normalizedFile?.let { Uri.fromFile(it) } ?: uri

        // 6. Determine Media3 MIME type and format description
        val (mimeType, formatDesc) = when (extension) {
            "vtt" -> Pair("text/vtt", "WebVTT")
            "ass", "ssa" -> Pair("text/x-ssa", "SubStation Alpha")
            "srt" -> Pair("application/x-subrip", "SubRip (SRT)")
            "sub" -> Pair("application/x-subrip", "MicroDVD")
            "ttml", "dfxp" -> Pair("application/ttml+xml", "Timed Text Markup Language (TTML)")
            else -> {
                // If extension is not explicitly one of .srt/.vtt/.ass/.ssa/.sub/.ttml/.dfxp
                // sniff content to verify if it has valid subtitle cues
                sniffSubtitleFormat(context, effectiveUri) ?: return SubtitleValidationResult.Invalid(
                    "Unsupported file extension '.$extension'. Supported subtitle formats are .srt, .vtt, .ass, .ssa, and .ttml."
                )
            }
        }

        return SubtitleValidationResult.Valid(
            displayName = resolvedName,
            mimeType = mimeType,
            formatDescription = formatDesc,
            validatedUri = effectiveUri
        )
    }

    fun resolveDisplayName(context: Context, uri: Uri): String {
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                return name
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        // Fallback: URL-decode the last path segment
        val segment = uri.lastPathSegment
        if (!segment.isNullOrBlank()) {
            return try {
                val decoded = URLDecoder.decode(segment, "UTF-8")
                decoded.substringAfterLast('/')
            } catch (_: Throwable) {
                segment.substringAfterLast('/')
            }
        }

        return "External Subtitle"
    }

    fun isBinaryOrImage(buffer: ByteArray, length: Int): Boolean {
        // PNG magic: 89 50 4E 47
        if (length >= 4 && buffer[0] == 0x89.toByte() && buffer[1] == 0x50.toByte() && buffer[2] == 0x4E.toByte() && buffer[3] == 0x47.toByte()) return true
        // JPEG magic: FF D8 FF
        if (length >= 3 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xD8.toByte() && buffer[2] == 0xFF.toByte()) return true
        // GIF magic: GIF8
        if (length >= 4 && buffer[0] == 'G'.code.toByte() && buffer[1] == 'I'.code.toByte() && buffer[2] == 'F'.code.toByte() && buffer[3] == '8'.code.toByte()) return true
        // WebP magic: RIFF....WEBP
        if (length >= 12 && buffer[0] == 'R'.code.toByte() && buffer[1] == 'I'.code.toByte() && buffer[2] == 'F'.code.toByte() && buffer[3] == 'F'.code.toByte()) return true
        // PDF magic: %PDF
        if (length >= 4 && buffer[0] == '%'.code.toByte() && buffer[1] == 'P'.code.toByte() && buffer[2] == 'D'.code.toByte() && buffer[3] == 'F'.code.toByte()) return true
        // ZIP/APK/JAR magic: PK\x03\x04
        if (length >= 4 && buffer[0] == 'P'.code.toByte() && buffer[1] == 'K'.code.toByte() && buffer[2] == 0x03.toByte() && buffer[3] == 0x04.toByte()) return true

        // UTF-16LE (FF FE) and UTF-16BE (FE FF) text files naturally contain 0x00 bytes on every ASCII char.
        if (SubtitleCharsetNormalizer.hasUtf16Bom(buffer, length)) {
            return false
        }

        // Check for high concentration of non-printable control characters (excluding whitespace \t, \n, \r)
        var nonPrintableCount = 0
        for (i in 0 until minOf(length, 128)) {
            val b = buffer[i].toInt() and 0xFF
            if (b == 0) return true // Null byte in non-UTF-16 text subtitle indicates binary file
            if (b < 32 && b != 9 && b != 10 && b != 13) {
                nonPrintableCount++
            }
        }
        return nonPrintableCount > 5
    }

    private fun sniffSubtitleFormat(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = stream.bufferedReader()
                val firstFewLines = (1..10).mapNotNull { reader.readLine() }.joinToString("\n")
                when {
                    firstFewLines.contains("WEBVTT", ignoreCase = true) -> Pair("text/vtt", "WebVTT")
                    firstFewLines.contains("[Script Info]", ignoreCase = true) -> Pair("text/x-ssa", "SubStation Alpha")
                    firstFewLines.contains("<tt", ignoreCase = true) -> Pair("application/ttml+xml", "Timed Text Markup Language (TTML)")
                    firstFewLines.contains("-->") -> Pair("application/x-subrip", "SubRip (SRT)")
                    else -> null
                }
            }
        } catch (_: Throwable) {
            null
        }
    }
}
