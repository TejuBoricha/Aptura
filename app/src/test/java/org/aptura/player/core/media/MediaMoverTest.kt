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

    class StorageProvider : ContentProvider() {
        val updated = mutableListOf<Pair<Uri, ContentValues?>>()
        var refuse: SecurityException? = null

        override fun onCreate() = true
        override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            refuse?.let { throw it }
            updated += uri to values
            return 1
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


}
