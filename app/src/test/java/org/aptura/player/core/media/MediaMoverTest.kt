package org.aptura.player.core.media

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaMoverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    open class StorageProvider : ContentProvider() {
        val updated = mutableListOf<Pair<Uri, ContentValues?>>()
        var refuse: SecurityException? = null
        var rowsUpdated: Int = 1

        /** `VOLUME_NAME` by MediaStore id. Left empty, the provider answers nothing, as if the volume were unknown. */
        val volumeById = mutableMapOf<Long, String>()

        override fun onCreate() = true
        override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? {
            if (volumeById.isEmpty()) return null
            // The mover asks `_id IN (?, ?, ...)`, so the ids are the selection arguments.
            val cursor = MatrixCursor(arrayOf("_id", "volume_name"))
            a.orEmpty().mapNotNull { it.toLongOrNull() }.forEach { id ->
                volumeById[id]?.let { cursor.addRow(arrayOf<Any>(id, it)) }
            }
            return cursor
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            refuse?.let { throw it }
            updated += uri to values
            return rowsUpdated
        }
    }

    private lateinit var mediaStore: StorageProvider

    @Before
    fun registerProviders() {
        mediaStore = Robolectric.setupContentProvider(StorageProvider::class.java, "media")
    }

    private val videoUri = "content://media/external/video/media/7"
    private val safDocUri = "content://com.android.externalstorage.documents/document/primary%3AMovies%2Fclip.mp4"
    private val safTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AMovies"

    private fun mover(
        sdk: Int = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        consent: (ContentResolver, List<Uri>) -> IntentSender? = { _, _ -> null }
    ) = MediaMover(context, sdkInt = sdk, writeConsentRequestFactory = consent)

    private fun someIntentSender(): IntentSender =
        PendingIntent.getActivity(context, 0, Intent("test.write.consent"), PendingIntent.FLAG_IMMUTABLE).intentSender

    @Test
    fun canMoveInPlace_returnsTrueForMediaStoreSourceAndInternalPath() {
        val m = mover()
        assertTrue(m.canMoveInPlace(videoUri, "Movies/"))
        assertTrue(m.canMoveInPlace(videoUri, "Trailers"))
    }

    @Test
    fun canMoveInPlace_returnsFalseForSafTreeDestination() {
        val m = mover()
        assertFalse(m.canMoveInPlace(videoUri, safTreeUri))
    }

    @Test
    fun canMoveInPlace_returnsFalseForNonMediaStoreSource() {
        val m = mover()
        assertFalse(m.canMoveInPlace(safDocUri, "Movies/"))
        assertFalse(m.canMoveInPlace("file:///storage/emulated/0/Movies/test.mp4", "Movies/"))
    }

    // --- DATA-02: an in-place move only changes a file's folder, never the storage volume it is on ---

    /** Folder keys name a volume's folder by path: `/storage/emulated/0/...` is the phone, `/storage/<ID>/...` an SD card. */
    private val phoneFolder = "/storage/emulated/0/Movies"
    private val sdFolder = "/storage/1A2B-3C4D/Movies"

    @Test
    fun canMoveInPlace_isFalseWhenTheFolderIsOnAnotherVolume() {
        mediaStore.volumeById[7] = "external_primary"

        assertFalse("a video on the phone cannot be moved in place to an SD card folder", mover().canMoveInPlace(videoUri, sdFolder))
        assertTrue(mover().canMoveInPlace(videoUri, phoneFolder))
    }

    @Test
    fun canMoveInPlace_followsTheVolumeOfAVideoOnAnSdCard() {
        // MediaStore names an SD card's volume in lower case; the folder key spells the same id in upper case.
        mediaStore.volumeById[7] = "1a2b-3c4d"

        assertTrue(mover().canMoveInPlace(videoUri, "/storage/1A2B-3C4D/Download"))
        assertFalse("a video on an SD card cannot be moved in place to the phone", mover().canMoveInPlace(videoUri, phoneFolder))
    }

    @Test
    fun canMoveAllInPlace_isFalseWhenAnyVideoIsOnAnotherVolume() {
        mediaStore.volumeById[7] = "external_primary"
        mediaStore.volumeById[8] = "1a2b-3c4d"

        assertFalse(mover().canMoveAllInPlace(listOf(videoUri, "content://media/external/video/media/8"), phoneFolder))
        assertTrue(mover().canMoveAllInPlace(listOf(videoUri, "content://media/external/video/media/7"), phoneFolder))
    }

    @Test
    fun anUnknownSourceVolume_isTheSameAsThePhonesOwnStorage() {
        // The provider answers nothing. A missing volume means the phone's storage everywhere else (MediaFolderKey).
        assertTrue(mover().canMoveInPlace(videoUri, phoneFolder))
        assertFalse(mover().canMoveInPlace(videoUri, sdFolder))
    }

    @Test
    fun aFolderWithNoVolumeInIt_staysOnTheVideosOwnVolume() {
        mediaStore.volumeById[7] = "1a2b-3c4d"

        assertTrue(mover().canMoveInPlace(videoUri, "Movies/"))
    }

    @Test
    fun moveInPlace_toAnotherVolume_failsAndChangesNothing() {
        mediaStore.volumeById[7] = "external_primary"

        val result = mover().moveInPlace(videoUri, sdFolder)

        assertTrue("reported $result", result is MoveOutcome.Failed)
        assertTrue("nothing may be written when the move cannot be done in place", mediaStore.updated.isEmpty())
    }

    @Test
    fun moveInPlace_updatesRelativePathOnProvider() {
        val m = mover()
        val result = m.moveInPlace(videoUri, "Movies/Trailers")

        assertEquals(MoveOutcome.Moved, result)
        assertEquals(1, mediaStore.updated.size)
        val (uri, values) = mediaStore.updated.first()
        assertEquals(Uri.parse(videoUri), uri)
        assertEquals("Movies/Trailers/", values?.getAsString("relative_path"))
    }

    @Test
    fun moveInPlace_requestsConsentOnApi30WhenProviderRefuses() {
        mediaStore.refuse = SecurityException("Not owner")
        val sender = someIntentSender()
        val m = mover(sdk = Build.VERSION_CODES.R, consent = { _, _ -> sender })

        val result = m.moveInPlace(videoUri, "Movies/")

        assertTrue(result is MoveOutcome.NeedsConsent)
        assertEquals(sender, (result as MoveOutcome.NeedsConsent).intentSender)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun moveInPlace_requestsConsentOnApi29WhenRecoverable() {
        val pendingIntent = PendingIntent.getActivity(context, 0, Intent("test.recoverable"), PendingIntent.FLAG_IMMUTABLE)
        val action = android.app.RemoteAction(
            android.graphics.drawable.Icon.createWithData(ByteArray(0), 0, 0),
            "Allow",
            "Allow Aptura to modify this video",
            pendingIntent
        )
        val recoverable = RecoverableSecurityException(
            SecurityException("Permission needed"),
            "User consent needed",
            action
        )
        mediaStore.refuse = recoverable
        val m = mover(sdk = Build.VERSION_CODES.Q)

        val result = m.moveInPlace(videoUri, "Movies/")

        assertTrue(result is MoveOutcome.NeedsConsent)
    }

    @Test
    fun moveInPlace_returnsFailedForInvalidUri() {
        val m = mover()
        val result = m.moveInPlace(":::invalid", "Movies/")

        assertTrue(result is MoveOutcome.Failed)
    }

    @Test
    fun requestBatchWriteConsent_returnsSenderOnApi30() {
        val sender = someIntentSender()
        val m = mover(sdk = Build.VERSION_CODES.R, consent = { _, uris ->
            assertEquals(2, uris.size)
            sender
        })

        val result = m.requestBatchWriteConsent(listOf(videoUri, "content://media/external/video/media/8"))

        assertEquals(sender, result)
    }

    @Test
    fun requestBatchWriteConsent_returnsNullBelowApi30() {
        val sender = someIntentSender()
        val m = mover(sdk = Build.VERSION_CODES.Q, consent = { _, _ -> sender })

        val result = m.requestBatchWriteConsent(listOf(videoUri))

        assertNull(result)
    }

    @Test
    fun canMoveInPlace_handlesSpecialCharactersAndSpacesInFolder() {
        val m = mover()
        assertTrue(m.canMoveInPlace(videoUri, "Movies/Family & Vacation (2026)"))
        assertTrue(m.canMoveInPlace(videoUri, "Download/Sub Folder 🎬"))
    }

    @Test
    fun canMoveAllInPlace_returnsFalseIfAnyItemIsSafOrInvalid() {
        val m = mover()
        assertFalse(m.canMoveAllInPlace(listOf(videoUri, safDocUri), "Movies/"))
        assertFalse(m.canMoveAllInPlace(emptyList(), "Movies/"))
        assertTrue(m.canMoveAllInPlace(listOf(videoUri, "content://media/external/video/media/8"), "Movies/"))
    }

    @Test
    fun moveInPlace_normalizesNestedSubdirectorySlashes() {
        val m = mover()
        val result = m.moveInPlace(videoUri, "Movies/Nested/Subfolder")

        assertEquals(MoveOutcome.Moved, result)
        val (_, values) = mediaStore.updated.last()
        assertEquals("Movies/Nested/Subfolder/", values?.getAsString("relative_path"))
    }

    @Test
    fun moveInPlace_returnsFailedWhenProviderReportsZeroRows() {
        mediaStore.rowsUpdated = 0
        val m = mover()

        val result = m.moveInPlace(videoUri, "Movies/")

        assertEquals(MoveOutcome.Failed("Could not move video."), result)
    }

    @Test
    fun canMoveInPlace_returnsFalseBelowApi29() {
        val m = mover(sdk = Build.VERSION_CODES.P)
        assertFalse(m.canMoveInPlace(videoUri, "Movies/"))
        assertFalse(m.canMoveAllInPlace(listOf(videoUri), "Movies/"))
    }

    @Test
    fun canMoveInPlace_returnsFalseForDownloadFolderOnApi29() {
        val m = mover(sdk = Build.VERSION_CODES.Q)
        assertFalse(m.canMoveInPlace(videoUri, "Download/"))
        assertFalse(m.canMoveInPlace(videoUri, "Download/Subfolder"))
        assertFalse(m.canMoveInPlace(videoUri, "/storage/emulated/0/Download"))
        assertFalse(m.canMoveInPlace(videoUri, "Downloads/"))
        assertTrue(m.canMoveInPlace(videoUri, "Movies/"))
    }

    @Test
    fun canMoveInPlace_allowsDownloadFolderOnApi30Plus() {
        val m = mover(sdk = Build.VERSION_CODES.R)
        assertTrue(m.canMoveInPlace(videoUri, "Download/"))
        assertTrue(m.canMoveInPlace(videoUri, "Download/Subfolder"))
    }

    @Test
    fun moveInPlace_returnsFailedForDownloadFolderOnApi29() {
        val m = mover(sdk = Build.VERSION_CODES.Q)
        val result = m.moveInPlace(videoUri, "Download/Subfolder")
        assertTrue(result is MoveOutcome.Failed)
        assertTrue((result as MoveOutcome.Failed).message.contains("Download"))
    }

    @Test
    fun isDownloadFolder_identifiesPublicDownloadsFolders() {
        assertTrue(MediaMover.isDownloadFolder("Download"))
        assertTrue(MediaMover.isDownloadFolder("Download/"))
        assertTrue(MediaMover.isDownloadFolder("Download/Subfolder"))
        assertTrue(MediaMover.isDownloadFolder("/storage/emulated/0/Download"))
        assertTrue(MediaMover.isDownloadFolder("/storage/emulated/0/Downloads/Docs"))
        assertFalse(MediaMover.isDownloadFolder("Movies/Download"))
        assertFalse(MediaMover.isDownloadFolder("/storage/emulated/0/Movies"))
        assertFalse(MediaMover.isDownloadFolder(""))
    }

    @Test
    fun moveInPlace_returnsFailedOnApi28WhenRefusedWithoutRecoverable() {
        mediaStore.refuse = SecurityException("Permission denied")
        val m = mover(sdk = Build.VERSION_CODES.P)

        val result = m.moveInPlace(videoUri, "Movies/")

        assertTrue(result is MoveOutcome.Failed)
        assertTrue((result as MoveOutcome.Failed).message.contains("Browse device storage"))
    }


}
