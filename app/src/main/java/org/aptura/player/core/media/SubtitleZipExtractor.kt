package org.aptura.player.core.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
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
 * Defends against Zip Slip and zip bombs, and extracts the best candidate (.srt, .vtt, .ass, .ssa, .sub, .ttml, .dfxp).
 *
 * The archive as a whole is bounded, not only each subtitle (audit SEC-02): [ZipInputStream] unpacks every entry it
 * passes, so an entry that is skipped still costs the CPU of unpacking it. Every byte unpacked counts toward
 * [MAX_TOTAL_BYTES] and every entry toward [MAX_ENTRIES]. Each import gets its own folder, so two archives with an
 * entry of the same name cannot overwrite the file the player is reading, and a refused import leaves nothing behind.
 */
object SubtitleZipExtractor {

    /** 20 MB ceiling prevents disk exhaustion / Zip Bomb DoS attacks (FR-SUB-006). */
    const val MAX_SUBTITLE_BYTES = 20L * 1024L * 1024L

    /**
     * Everything one archive may unpack to, subtitle or not. A real subtitle pack is well under a few MB; this leaves
     * room for one largest allowed subtitle and a large cover image or two.
     */
    const val MAX_TOTAL_BYTES = 64L * 1024L * 1024L

    /** Entries in one archive, directories included. A season pack has tens; this is generous and still bounded. */
    const val MAX_ENTRIES = 1000

    /** Import folders older than this are removed the next time a zip is imported. A recent one may still be playing. */
    const val STALE_IMPORT_MS = 7L * 24L * 60L * 60L * 1000L

    /** Folder under the app's cache that holds the per-import folders. */
    const val EXTRACT_DIR_NAME = "subtitles_extracted"

    private const val TOO_MANY_ENTRIES =
        "This zip has too many files (more than 1000) to import safely. Extract the subtitle file from it first."
    private const val TOO_LARGE_UNPACKED =
        "This zip unpacks to more than 64 MB, which is too large to import safely. Extract the subtitle file from it first."
    private const val ENTRY_TOO_LARGE = "Security error: Subtitle file exceeds maximum allowed size (20 MB)"

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
        val root = File(context.cacheDir, EXTRACT_DIR_NAME).apply { mkdirs() }
        pruneStaleImports(root)
        val importDir = File(root, "import_${UUID.randomUUID()}").apply { mkdirs() }

        val result = try {
            ZipInputStream(inputStream).use { zis -> scan(zis, importDir) }
        } catch (e: Throwable) {
            ZipExtractionResult.Failure("Corrupted zip archive: ${e.message}")
        }

        // A refused import must not leave files, or even an empty folder, in the cache.
        if (result !is ZipExtractionResult.Success) importDir.deleteRecursively()
        return result
    }

    private class Candidate(val file: File, val name: String, val mimeType: String)

    private fun scan(zis: ZipInputStream, importDir: File): ZipExtractionResult {
        val canonicalImportDir = importDir.canonicalPath + File.separator
        val buffer = ByteArray(8192)
        var best: Candidate? = null
        var entries = 0
        var unpacked = 0L

        var entry = zis.nextEntry
        while (entry != null) {
            if (++entries > MAX_ENTRIES) return ZipExtractionResult.Failure(TOO_MANY_ENTRIES)

            val fileName = entry.name.substringAfterLast("/").substringAfterLast("\\")
            val mimeType = SUPPORTED_SUBTITLE_EXTENSIONS[fileName.substringAfterLast(".", "").lowercase()]
            val wanted = !entry.isDirectory && mimeType != null

            var destination: File? = null
            if (wanted) {
                // Validate path against Zip Slip vulnerability
                destination = File(importDir, fileName)
                if (!destination.canonicalPath.startsWith(canonicalImportDir)) {
                    return ZipExtractionResult.Failure("Security error: Zip entry path escapes output directory")
                }
            }

            // Unpack every entry through the same counted loop, wanted or not: skipping one inflates it anyway.
            var entryBytes = 0L
            val out = destination?.let { FileOutputStream(it) }
            try {
                var read = zis.read(buffer)
                while (read >= 0) {
                    entryBytes += read
                    unpacked += read
                    if (unpacked > MAX_TOTAL_BYTES) return ZipExtractionResult.Failure(TOO_LARGE_UNPACKED)
                    if (wanted && entryBytes > MAX_SUBTITLE_BYTES) return ZipExtractionResult.Failure(ENTRY_TOO_LARGE)
                    out?.write(buffer, 0, read)
                    read = zis.read(buffer)
                }
            } finally {
                out?.close()
            }

            if (wanted && destination != null && mimeType != null) {
                // Prioritize English or .srt if multiple exist
                val isEnglish = fileName.contains("eng", ignoreCase = true) || fileName.contains(".en.", ignoreCase = true)
                val previous = best
                if (previous == null || isEnglish) {
                    // Only the chosen file is kept: the one it replaces is deleted as soon as it loses.
                    if (previous != null && previous.file != destination) previous.file.delete()
                    best = Candidate(destination, fileName, mimeType)
                    if (isEnglish) break // Found preferred track
                } else if (destination != previous.file) {
                    destination.delete()
                }
            }

            zis.closeEntry()
            entry = zis.nextEntry
        }

        val chosen = best
        return if (chosen != null) {
            ZipExtractionResult.Success(extractedFile = chosen.file, entryName = chosen.name, mimeType = chosen.mimeType)
        } else {
            ZipExtractionResult.Failure("Zip archive contains no recognized subtitle files (.srt, .vtt, .ass)")
        }
    }

    /** Removes import folders (and loose files from older versions) that have not been touched for [STALE_IMPORT_MS]. */
    private fun pruneStaleImports(root: File) {
        val cutoff = System.currentTimeMillis() - STALE_IMPORT_MS
        root.listFiles()?.forEach { child ->
            if (child.lastModified() < cutoff) child.deleteRecursively()
        }
    }
}
