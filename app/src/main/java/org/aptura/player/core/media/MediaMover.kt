package org.aptura.player.core.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
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
    fun canMoveInPlace(uriString: String, destinationPath: String): Boolean =
        canMoveAllInPlace(listOf(uriString), destinationPath)

    /**
     * True if every video can be moved in place to [destinationPath]: each is a MediaStore video, the destination is
     * not a SAF folder, and each video is already on the destination's storage volume. An in-place move only rewrites
     * `RELATIVE_PATH`, so it can never carry a file to an SD card or back (audit DATA-02). Those moves have to copy.
     */
    fun canMoveAllInPlace(uris: List<String>, destinationPath: String): Boolean {
        if (uris.isEmpty()) return false
        if (destinationPath.startsWith("content://")) return false
        val mediaUris = uris.map { mediaStoreUriOrNull(it) ?: return false }
        // A folder with no volume in it (`Movies/`) means "wherever the video is", so there is nothing to look up.
        if (StorageVolumes.ofFolderPath(destinationPath) == null) return true
        val volumes = sourceVolumes(mediaUris)
        return mediaUris.all { StorageVolumes.sameVolume(volumes[idOrNull(it)], destinationPath) }
    }

    private fun mediaStoreUriOrNull(uriString: String): Uri? {
        val uri = try {
            uriString.toUri()
        } catch (_: Throwable) {
            return null
        }
        return uri.takeIf { it.scheme == "content" && it.authority == MediaStore.AUTHORITY }
    }

    private fun idOrNull(uri: Uri): Long? = try {
        ContentUris.parseId(uri)
    } catch (_: Exception) {
        null
    }

    /**
     * The storage volume of each video (`VOLUME_NAME`: [StorageVolumes.PRIMARY], or an SD card's id), by MediaStore id,
     * in one query per [VOLUME_QUERY_CHUNK] videos because the move buttons ask on the main thread. A row's own URI
     * names the `external` union of all volumes, so it cannot say. A video missing from the result has an unknown
     * volume: the column does not exist before Android 10, or the provider did not answer.
     */
    private fun sourceVolumes(uris: List<Uri>): Map<Long, String> {
        if (sdkInt < Build.VERSION_CODES.Q) return emptyMap()
        val ids = uris.mapNotNull { idOrNull(it) }.distinct()
        val volumes = mutableMapOf<Long, String>()
        try {
            ids.chunked(VOLUME_QUERY_CHUNK).forEach { chunk ->
                val placeholders = chunk.joinToString(",") { "?" }
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.VOLUME_NAME),
                    "${MediaStore.MediaColumns._ID} IN ($placeholders)",
                    chunk.map { it.toString() }.toTypedArray(),
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    val volumeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME)
                    if (idColumn < 0 || volumeColumn < 0) return@use
                    while (cursor.moveToNext()) {
                        if (!cursor.isNull(volumeColumn)) volumes[cursor.getLong(idColumn)] = cursor.getString(volumeColumn)
                    }
                }
            }
        } catch (_: Exception) {
            // An unknown volume is read as the phone's own storage; see StorageVolumes.sameVolume.
        }
        return volumes
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
        // Rewriting RELATIVE_PATH never changes the volume, so it would report "Moved" while the file stayed where it
        // was. The library sends such moves to the copy path; this keeps a direct call from lying too.
        if (!canMoveAllInPlace(listOf(uriString), destinationPath)) {
            return MoveOutcome.Failed("That folder is on another storage, so the video has to be copied there instead of moved in place.")
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

    private companion object {
        /** Ids per volume query: well under SQLite's limit on bound arguments (999 on older versions). */
        const val VOLUME_QUERY_CHUNK = 500
    }
}
