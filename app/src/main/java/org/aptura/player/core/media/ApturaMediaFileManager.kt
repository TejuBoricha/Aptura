package org.aptura.player.core.media

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
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
        val cleanName = newNameWithExt.trim()
        if (cleanName.isBlank()) return false

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
}
