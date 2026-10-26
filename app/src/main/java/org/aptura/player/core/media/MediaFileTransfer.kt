package org.aptura.player.core.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.IOException

data class MediaTransferResult(
    val copiedUris: List<String>,
    val failedUris: List<String>
) {
    val copied: Int get() = copiedUris.size
}

/**
 * Copies media into a folder explicitly chosen through Android's Storage Access Framework.
 * The destination provider owns naming and storage permission decisions.
 */
object MediaFileTransfer {

    fun copyIntoTree(context: Context, sourceUris: List<String>, treeUri: Uri): MediaTransferResult {
        val resolver = context.contentResolver
        val destinationId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            return MediaTransferResult(copiedUris = emptyList(), failedUris = sourceUris)
        }
        val destination = DocumentsContract.buildDocumentUriUsingTree(treeUri, destinationId)
        val copied = mutableListOf<String>()
        val failed = mutableListOf<String>()

        sourceUris.distinct().forEach { sourceString ->
            val source = Uri.parse(sourceString)
            var created: Uri? = null
            try {
                val sourceDocumentId = try {
                    DocumentsContract.getDocumentId(source)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (sourceDocumentId != null &&
                    (sourceDocumentId == destinationId ||
                        sourceDocumentId.startsWith("$destinationId/") ||
                        sourceDocumentId.startsWith("$destinationId:"))
                ) {
                    throw IOException("Choose a different destination folder.")
                }
                val name = displayName(resolver, source)
                val mimeType = resolver.getType(source) ?: mimeTypeFromName(name)
                created = DocumentsContract.createDocument(resolver, destination, mimeType, name)
                    ?: throw IOException("The selected folder refused to create a file.")
                val input = resolver.openInputStream(source)
                    ?: throw IOException("The source video could not be opened.")
                input.use { sourceStream ->
                    val output = resolver.openOutputStream(created, "w")
                        ?: throw IOException("The selected folder could not be written.")
                    output.use { targetStream -> sourceStream.copyTo(targetStream) }
                }
                copied += sourceString
            } catch (_: Exception) {
                created?.let { partial ->
                    try {
                        DocumentsContract.deleteDocument(resolver, partial)
                    } catch (_: Exception) {
                        // The copy failed; retaining a partial destination is safer than touching the source.
                    }
                }
                failed += sourceString
            }
        }
        return MediaTransferResult(copied, failed)
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String {
        val cursor = try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        } catch (_: Exception) {
            null
        }
        cursor?.use {
            val column = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && it.moveToFirst()) {
                it.getString(column)?.takeIf(String::isNotBlank)?.let { name -> return name }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: throw IOException("The source video has no usable file name.")
    }

    private fun mimeTypeFromName(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            else -> "video/*"
        }
    }
}
