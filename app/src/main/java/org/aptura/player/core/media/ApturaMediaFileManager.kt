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
 * The outcome of a rename request.
 *
 * A rename is not a boolean: MediaStore keeps the same URI, a SAF document provider allocates a new one, and
 * a direct file rename changes the path. Callers must persist the URI the file now has, otherwise the next scan
 * sees the old row as missing and the renamed document as a new video (audit 2026-10-24, section 4.2).
 */
sealed interface MediaRenameResult {
    /** The file was renamed. [uri] is the identifier to use from now on. */
    data class Renamed(val uri: Uri, val displayName: String) : MediaRenameResult

    /** Android needs the user to grant write access before the rename can proceed. */
    data class PermissionRequired(val intentSender: IntentSender) : MediaRenameResult

    /** The provider refused, the document disappeared, or the file system rejected the name. */
    data object Failed : MediaRenameResult
}

/**
 * Resilient file operations manager for video renaming and storage manipulation.
 * Handles Scoped Storage on Android 10+ (API 29) and Android 11+ (API 30+),
 * SAF documents, and direct storage files.
 */
object ApturaMediaFileManager {

    fun renameMediaFile(
        context: Context,
        uri: Uri,
        newNameWithExt: String
    ): MediaRenameResult {
        val requestedName = newNameWithExt.trim().trimEnd('.')
        if (requestedName.isBlank()) return MediaRenameResult.Failed
        // Callers only know the library title, which often has no extension. Keep the real file extension so a
        // rename does not turn "Holiday.mp4" into an extension-less file that Android can no longer play.
        val cleanName = withPreservedExtension(context, uri, requestedName)

        // 1. MediaStore content URI update. The URI is stable, so no remap is needed. A SAF document URI must
        //    go to renameDocument instead: a provider that happens to implement update() may accept the values
        //    without renaming the file, and returning a stable URI would then be a false success.
        if (uri.authority == MediaStore.AUTHORITY) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    val baseTitle = if (cleanName.contains('.')) cleanName.substringBeforeLast('.') else cleanName
                    put(MediaStore.MediaColumns.TITLE, baseTitle)
                }
                val count = context.contentResolver.update(uri, values, null, null)
                if (count > 0) {
                    return MediaRenameResult.Renamed(uri, cleanName)
                }
            } catch (e: SecurityException) {
                permissionIntent(context, uri, e)?.let { return MediaRenameResult.PermissionRequired(it) }
            } catch (_: Throwable) {
                // fall through to SAF or file
            }
        }

        // 2. DocumentsContract / Storage Access Framework (SAF). renameDocument returns the NEW document URI.
        //    Attempt it for any non-MediaStore content URI: the provider call itself is the authority check, and
        //    it keeps the path testable with a plain ContentProvider instead of a full DocumentsProvider shell.
        if (uri.scheme == "content" && uri.authority != MediaStore.AUTHORITY) {
            try {
                val renamedUri = DocumentsContract.renameDocument(context.contentResolver, uri, cleanName)
                if (renamedUri != null) return MediaRenameResult.Renamed(renamedUri, cleanName)
            } catch (_: Throwable) {
                // fall through
            }
        }

        // 3. Direct File path (file:// or local internal storage)
        if (uri.scheme == "file") {
            try {
                val currentFile = File(uri.path ?: return MediaRenameResult.Failed)
                val targetFile = File(currentFile.parentFile, cleanName)
                if (currentFile.renameTo(targetFile)) {
                    return MediaRenameResult.Renamed(Uri.fromFile(targetFile), cleanName)
                }
            } catch (_: Throwable) {
                return MediaRenameResult.Failed
            }
        }

        return MediaRenameResult.Failed
    }

    private fun permissionIntent(context: Context, uri: Uri, error: SecurityException): IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && error is RecoverableSecurityException) {
            return error.userAction.actionIntent.intentSender
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender
            } catch (_: Throwable) {
                null
            }
        }
        return null
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
