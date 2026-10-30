package org.aptura.player.core.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File

/** Result of asking Android to delete a video file. */
sealed interface DeleteOutcome {
    /** The file is gone (or already was). The index entry can be removed. */
    data object Deleted : DeleteOutcome

    /** Android needs the user's consent: launch [intentSender] and, on RESULT_OK, treat the file as deleted. */
    data class NeedsConsent(val intentSender: IntentSender) : DeleteOutcome

    /** The file could not be deleted; [message] is safe to show to the user. */
    data class Failed(val message: String) : DeleteOutcome
}

/**
 * Really deletes video files from device storage, honouring scoped storage (audit P0-06: the former
 * "Delete Permanently" button only removed the index row and the file stayed on disk).
 *
 * Strategy:
 *  - SAF documents  -> [DocumentsContract.deleteDocument]
 *  - file:// URIs   -> [File.delete]
 *  - MediaStore     -> direct delete first (works for files this app owns); if Android refuses,
 *                      API 30+ shows the system "allow delete?" dialog via [MediaStore.createDeleteRequest],
 *                      API 29 uses [RecoverableSecurityException], API 26-28 cannot delete other apps' files.
 */
class MediaDeleter(
    private val context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val consentRequestFactory: (ContentResolver, List<Uri>) -> IntentSender? = { resolver, uris ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(resolver, uris).intentSender
        } else {
            null
        }
    },
    private val isDocumentUri: (Context, Uri) -> Boolean = { ctx, uri -> DocumentsContract.isDocumentUri(ctx, uri) }
) {

    fun delete(uriString: String): DeleteOutcome {
        val uri = try {
            uriString.toUri()
        } catch (_: Throwable) {
            return DeleteOutcome.Failed("Invalid file location.")
        }
        val resolver = context.contentResolver

        return when {
            uri.scheme == "file" -> deleteFile(uri)
            isDocumentUri(context, uri) -> deleteDocument(resolver, uri)
            else -> deleteMediaStore(resolver, uri)
        }
    }

    private fun deleteFile(uri: Uri): DeleteOutcome {
        val file = File(uri.path.orEmpty())
        if (!file.exists()) return DeleteOutcome.Deleted
        return if (file.delete()) {
            DeleteOutcome.Deleted
        } else {
            DeleteOutcome.Failed("Android did not allow Aptura to delete this file. Delete it from your Files app instead.")
        }
    }

    private fun deleteDocument(resolver: ContentResolver, uri: Uri): DeleteOutcome {
        return try {
            if (DocumentsContract.deleteDocument(resolver, uri)) {
                DeleteOutcome.Deleted
            } else {
                DeleteOutcome.Failed("The storage provider refused to delete this file.")
            }
        } catch (_: Throwable) {
            DeleteOutcome.Failed("The storage provider refused to delete this file.")
        }
    }

    private fun deleteMediaStore(resolver: ContentResolver, uri: Uri): DeleteOutcome {
        return try {
            // 0 rows means it no longer exists, which is the outcome the user wanted.
            resolver.delete(uri, null, null)
            DeleteOutcome.Deleted
        } catch (e: SecurityException) {
            requestConsent(resolver, uri, e)
        } catch (_: Throwable) {
            DeleteOutcome.Failed("Could not delete this file.")
        }
    }

    private fun requestConsent(resolver: ContentResolver, uri: Uri, original: SecurityException): DeleteOutcome {
        if (sdkInt >= Build.VERSION_CODES.R) {
            val sender = try {
                consentRequestFactory(resolver, listOf(uri))
            } catch (_: Throwable) {
                null
            }
            return if (sender != null) DeleteOutcome.NeedsConsent(sender) else cannotDelete()
        }
        // `sdkInt` is injectable for tests; the real SDK_INT check is what proves the API-29 call below is safe.
        if (sdkInt == Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            consentSenderFromRecoverable(original)?.let { return DeleteOutcome.NeedsConsent(it) }
        }
        return cannotDelete()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun consentSenderFromRecoverable(e: SecurityException): IntentSender? =
        (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender

    private fun cannotDelete() = DeleteOutcome.Failed(
        "This Android version does not let Aptura delete this file. Delete it from your Files app instead."
    )
}
