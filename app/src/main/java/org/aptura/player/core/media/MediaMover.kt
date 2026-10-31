package org.aptura.player.core.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri

/** Result of asking Android to move a video file to another MediaStore folder in-place. */
sealed interface MoveOutcome {
    /** The file was relocated in-place to the target folder without copying bytes. */
    data object Moved : MoveOutcome

    /** Android needs write consent to modify this file; launch [intentSender] (prompts "Allow to modify"). */
    data class NeedsConsent(val intentSender: IntentSender) : MoveOutcome

    /** The file could not be moved; [message] is safe to show to the user. */
    data class Failed(val message: String) : MoveOutcome
}

/**
 * Moves MediaStore video files in-place by updating their [MediaStore.MediaColumns.RELATIVE_PATH].
 *
 * This performs an instantaneous filesystem metadata relocation without duplicating bytes or risking
 * out-of-disk-space failures. For files not owned by this app, Android 11+ prompts the user via
 * [MediaStore.createWriteRequest] ("Allow Aptura to modify this video?"), which accurately communicates
 * the move intent rather than the alarming "delete" prompt.
 */
class MediaMover(
    private val context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val writeConsentRequestFactory: (ContentResolver, List<Uri>) -> IntentSender? = { resolver, uris ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createWriteRequest(resolver, uris).intentSender
        } else {
            null
        }
    }
) {
    /**
     * True if the transfer can be done in-place via MediaStore metadata updates (same storage volume),
     * rather than copying bytes and deleting the original.
     */
    fun canMoveInPlace(uriString: String, destinationPath: String): Boolean {
        val uri = try {
            uriString.toUri()
        } catch (_: Throwable) {
            return false
        }
        val isMediaStore = uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY
        val isNotSaf = !destinationPath.startsWith("content://")
        return isMediaStore && isNotSaf
    }

    fun canMoveAllInPlace(uris: List<String>, destinationPath: String): Boolean {
        if (uris.isEmpty()) return false
        return uris.all { canMoveInPlace(it, destinationPath) }
    }

    /**
     * Moves a single MediaStore video in-place to [destinationPath] by updating its [MediaStore.MediaColumns.RELATIVE_PATH].
     */
    fun moveInPlace(uriString: String, destinationPath: String): MoveOutcome {
        val uri = try {
            uriString.toUri()
        } catch (_: Throwable) {
            return MoveOutcome.Failed("Invalid file location.")
        }
        if (uri.scheme != "content" || uri.authority != MediaStore.AUTHORITY) {
            return MoveOutcome.Failed("Only MediaStore files can be moved in-place.")
        }
        val resolver = context.contentResolver
        val relativePath = MediaFileTransfer.relativePathFor(destinationPath)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        return try {
            val rows = resolver.update(uri, values, null, null)
            if (rows > 0) MoveOutcome.Moved else MoveOutcome.Failed("Could not move video.")
        } catch (e: SecurityException) {
            requestConsent(resolver, listOf(uri), e)
        } catch (error: Exception) {
            MoveOutcome.Failed(error.message ?: "Move failed.")
        }
    }

    /**
     * Requests batch write consent for multiple MediaStore URIs on API 30+.
     */
    fun requestBatchWriteConsent(uris: List<String>): IntentSender? {
        if (sdkInt < Build.VERSION_CODES.R) return null
        val resolver = context.contentResolver
        val mediaUris = uris.mapNotNull {
            try {
                it.toUri().takeIf { u -> u.authority == MediaStore.AUTHORITY }
            } catch (_: Throwable) {
                null
            }
        }
        if (mediaUris.isEmpty()) return null
        return try {
            writeConsentRequestFactory(resolver, mediaUris)
        } catch (_: Throwable) {
            null
        }
    }

    private fun requestConsent(resolver: ContentResolver, uris: List<Uri>, original: SecurityException): MoveOutcome {
        if (sdkInt >= Build.VERSION_CODES.R) {
            val sender = try {
                writeConsentRequestFactory(resolver, uris)
            } catch (_: Throwable) {
                null
            }
            return if (sender != null) MoveOutcome.NeedsConsent(sender) else cannotModify()
        }
        if (sdkInt == Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            consentSenderFromRecoverable(original)?.let { return MoveOutcome.NeedsConsent(it) }
        }
        return cannotModify()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun consentSenderFromRecoverable(e: SecurityException): IntentSender? =
        (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender

    private fun cannotModify() = MoveOutcome.Failed(
        "This Android version does not allow moving this video directly. Choose 'Browse device storage...' to select a folder."
    )
}
