package org.aptura.player.core.media

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

/**
 * Resilient file operations manager for video renaming and storage manipulation.
 * Handles Scoped Storage on Android 10+ (API 29) and Android 11+ (API 30+),
 * SAF documents, and direct storage files.
 */
object ApturaMediaFileManager {

    fun renameMediaFile(
        context: Context,
        uri: Uri,
        newNameWithExt: String,
        onNeedsWritePermission: ((IntentSender) -> Unit)? = null
    ): Boolean {
        val requestedName = newNameWithExt.trim().trimEnd('.')
        if (requestedName.isBlank()) return false
        // Callers only know the library title, which often has no extension. Keep the real file extension so a
        // rename does not turn "Holiday.mp4" into an extension-less file that Android can no longer play.
        val cleanName = withPreservedExtension(context, uri, requestedName)

        // 1. MediaStore content URI update
        if (uri.authority == MediaStore.AUTHORITY || uri.scheme == "content") {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    val baseTitle = if (cleanName.contains('.')) cleanName.substringBeforeLast('.') else cleanName
                    put(MediaStore.MediaColumns.TITLE, baseTitle)
                }
                val count = context.contentResolver.update(uri, values, null, null)
                if (count > 0) {
                    return true
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    onNeedsWritePermission?.invoke(e.userAction.actionIntent.intentSender)
                    return false
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                        onNeedsWritePermission?.invoke(pi.intentSender)
                        return false
                    } catch (_: Throwable) {
                        // ignore and fall through
                    }
                }
            } catch (_: Throwable) {
                // fall through to SAF or file
            }
        }

        // 2. DocumentsContract / Storage Access Framework (SAF)
        if (DocumentsContract.isDocumentUri(context, uri)) {
            try {
                val renamedUri = DocumentsContract.renameDocument(context.contentResolver, uri, cleanName)
                if (renamedUri != null) return true
            } catch (_: Throwable) {
                // fall through
            }
        }

        // 3. Direct File path (file:// or local internal storage)
        if (uri.scheme == "file") {
            try {
                val currentFile = File(uri.path ?: return false)
                val targetFile = File(currentFile.parentFile, cleanName)
                return currentFile.renameTo(targetFile)
            } catch (_: Throwable) {
                return false
            }
        }

        return false
    }

    private fun withPreservedExtension(context: Context, uri: Uri, requestedName: String): String {
        if (extensionOf(requestedName) != null) return requestedName
        val currentName = currentDisplayName(context, uri)
        val extension = currentName?.let(::extensionOf)
            ?: extensionForMime(context.contentResolver.getType(uri))
            ?: return requestedName
        return "$requestedName.$extension"
    }

    private fun currentDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /** A dot is only an extension when the suffix looks like one, so "Dr. Strangelove" keeps its title. */
    private fun extensionOf(name: String): String? {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.lastIndex) return null
        val extension = name.substring(dot + 1)
        return extension.takeIf {
            it.length in 2..5 && it.all(Char::isLetterOrDigit) && it.any(Char::isLetter)
        }
    }

    private fun extensionForMime(mimeType: String?): String? = when (mimeType?.lowercase()) {
        "video/mp4", "video/m4v" -> "mp4"
        "video/x-matroska" -> "mkv"
        "video/webm" -> "webm"
        "video/quicktime" -> "mov"
        "video/3gpp" -> "3gp"
        "video/x-msvideo" -> "avi"
        else -> null
    }
}
