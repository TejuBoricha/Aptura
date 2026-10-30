package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class ZipExtractionResult {
    data class Success(
        val extractedFile: File,
        val entryName: String,
        val mimeType: String
    ) : ZipExtractionResult()

    data class Failure(val reason: String) : ZipExtractionResult()
}

/**
 * Defensive extractor for subtitle files packaged inside `.zip` archives.
 * Defends against Zip Slip vulnerability and extracts the best candidate (.srt, .vtt, .ass, .ssa, .sub, .ttml, .dfxp).
 */
object SubtitleZipExtractor {

    /** 20 MB ceiling prevents disk exhaustion / Zip Bomb DoS attacks (FR-SUB-006). */
    const val MAX_SUBTITLE_BYTES = 20L * 1024L * 1024L

    private val SUPPORTED_SUBTITLE_EXTENSIONS = mapOf(
        "srt" to "application/x-subrip",
        "vtt" to "text/vtt",
        "ass" to "text/x-ssa",
        "ssa" to "text/x-ssa",
        "sub" to "application/x-subrip",
        "ttml" to "application/ttml+xml",
        "dfxp" to "application/ttml+xml"
    )

    fun extractSubtitleFromZip(context: Context, zipUri: Uri): ZipExtractionResult {
        val inputStream: InputStream = try {
            context.contentResolver.openInputStream(zipUri)
                ?: return ZipExtractionResult.Failure("Cannot open input stream for zip file")
        } catch (e: Throwable) {
            return ZipExtractionResult.Failure("Failed to read zip archive: ${e.message}")
        }

        return extractSubtitleFromStream(context, inputStream)
    }

    fun extractSubtitleFromStream(context: Context, inputStream: InputStream): ZipExtractionResult {
        val outputDir = File(context.cacheDir, "subtitles_extracted").apply { mkdirs() }
        val canonicalOutputDir = outputDir.canonicalPath

        var bestEntryName: String? = null
        var bestMimeType: String? = null
        var bestFile: File? = null

        try {
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = entry.name.substringAfterLast("/").substringAfterLast("\\")
                        val ext = fileName.substringAfterLast(".", "").lowercase()

                        if (SUPPORTED_SUBTITLE_EXTENSIONS.containsKey(ext)) {
                            // Validate path against Zip Slip vulnerability
                            val destinationFile = File(outputDir, fileName)
                            if (!destinationFile.canonicalPath.startsWith(canonicalOutputDir)) {
                                return ZipExtractionResult.Failure("Security error: Zip entry path escapes output directory")
                            }

                            // Write extracted file with bounded size check to defend against zip bombs
                            var exceededCeiling = false
                            FileOutputStream(destinationFile).use { fos ->
                                val buffer = ByteArray(8192)
                                var bytesWritten = 0L
                                var read = zis.read(buffer)
                                while (read >= 0) {
                                    bytesWritten += read
                                    if (bytesWritten > MAX_SUBTITLE_BYTES) {
                                        exceededCeiling = true
                                        break
                                    }
                                    fos.write(buffer, 0, read)
                                    read = zis.read(buffer)
                                }
                            }

                            if (exceededCeiling) {
                                destinationFile.delete()
                                return ZipExtractionResult.Failure("Security error: Subtitle file exceeds maximum allowed size (20 MB)")
                            }

                            // Prioritize English or .srt if multiple exist
                            val isEnglish = fileName.contains("eng", ignoreCase = true) || fileName.contains(".en.", ignoreCase = true)
                            if (bestFile == null || isEnglish) {
                                bestFile = destinationFile
                                bestEntryName = fileName
                                bestMimeType = SUPPORTED_SUBTITLE_EXTENSIONS[ext]
                                if (isEnglish) break // Found preferred track
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Throwable) {
            return ZipExtractionResult.Failure("Corrupted zip archive: ${e.message}")
        }

        val resultFile = bestFile
        val resultName = bestEntryName
        val resultMime = bestMimeType

        return if (resultFile != null && resultName != null && resultMime != null) {
            ZipExtractionResult.Success(
                extractedFile = resultFile,
                entryName = resultName,
                mimeType = resultMime
            )
        } else {
            ZipExtractionResult.Failure("Zip archive contains no recognized subtitle files (.srt, .vtt, .ass)")
        }
    }
}
