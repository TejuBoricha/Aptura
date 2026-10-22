package org.aptura.player.core.media

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.app.RemoteAction
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.database.Cursor
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit P0-06: "Delete Permanently" only removed the index row and the video stayed on the phone. These tests run
 * the production [MediaDeleter] against real files and a real ContentResolver → ContentProvider round trip, so they
 * fail if the file (or the provider's row) is not actually deleted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaDeleterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A storage provider that records what it was asked to delete, and can be told to refuse. */
    class StorageProvider : ContentProvider() {
        val deleted = mutableListOf<Uri>()
        var refuse: SecurityException? = null

        override fun onCreate() = true
        override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?) = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            refuse?.let { throw it }
            deleted += uri
            return 1
        }

        /** `DocumentsContract.deleteDocument` reaches a document provider through `call()`. */
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
            if (method == "android:deleteDocument") {
                refuse?.let { throw it }
                deleted += requireNotNull(extras?.getParcelable<Uri>("uri"))
                return Bundle.EMPTY
            }
            return super.call(method, arg, extras)
        }
    }

    private lateinit var mediaStore: StorageProvider
    private lateinit var documents: StorageProvider

    @Before
    fun registerProviders() {
        mediaStore = Robolectric.setupContentProvider(StorageProvider::class.java, "media")
        documents = Robolectric.setupContentProvider(StorageProvider::class.java, "com.example.documents")
    }

    private val videoUri = "content://media/external/video/media/7"

    private fun deleter(
        sdk: Int = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        consent: (android.content.ContentResolver, List<Uri>) -> IntentSender? = { _, _ -> null },
        isDocument: (Context, Uri) -> Boolean = { _, _ -> false }
    ) = MediaDeleter(context, sdkInt = sdk, consentRequestFactory = consent, isDocumentUri = isDocument)

    private fun someIntentSender(): IntentSender =
        PendingIntent.getActivity(context, 0, Intent("test.consent"), PendingIntent.FLAG_IMMUTABLE).intentSender

    @Test
    fun fileUri_removesTheFileFromDisk() {
        val file = folder.newFile("clip.mp4").also { it.writeText("video bytes") }
        assertTrue(file.exists())

        val outcome = deleter().delete(Uri.fromFile(file).toString())

        assertEquals(DeleteOutcome.Deleted, outcome)
        assertFalse("the file must really be gone, not just un-indexed", file.exists())
    }

    @Test
    fun fileUri_thatIsAlreadyGone_countsAsDeleted() {
        val missing = folder.root.resolve("never-existed.mp4")
        assertEquals(DeleteOutcome.Deleted, deleter().delete(Uri.fromFile(missing).toString()))
    }

    @Test
    fun fileUri_thatCannotBeDeleted_reportsAFailureInsteadOfPretending() {
        // A non-empty directory cannot be removed by File.delete().
        val dir = folder.newFolder("not-a-file")
        dir.resolve("child.mp4").writeText("x")

        val outcome = deleter().delete(Uri.fromFile(dir).toString())

        assertTrue(outcome is DeleteOutcome.Failed)
        assertTrue(dir.exists())
    }

    @Test
    fun mediaStoreUri_isDeletedThroughTheContentResolver() {
        val outcome = deleter().delete(videoUri)

        assertEquals(DeleteOutcome.Deleted, outcome)
        assertEquals("the provider must have been asked to delete this video", listOf(Uri.parse(videoUri)), mediaStore.deleted)
    }

    @Test
    fun mediaStoreRefusal_onAndroid11Plus_asksTheUserForConsent() {
        mediaStore.refuse = SecurityException("not owned by this app")
        val sender = someIntentSender()
        val asked = mutableListOf<List<Uri>>()

        val outcome = deleter(sdk = Build.VERSION_CODES.R, consent = { _, uris -> asked += uris; sender }).delete(videoUri)

        assertTrue("expected a consent request, got $outcome", outcome is DeleteOutcome.NeedsConsent)
        assertSame(sender, (outcome as DeleteOutcome.NeedsConsent).intentSender)
        assertEquals(listOf(listOf(Uri.parse(videoUri))), asked)
        assertTrue("nothing may be deleted before the user agrees", mediaStore.deleted.isEmpty())
    }

    @Test
    fun mediaStoreRefusal_onAndroid10_usesTheRecoverableSecurityException() {
        val action = RemoteAction(
            Icon.createWithData(ByteArray(0), 0, 0),
            "Allow", "Allow Aptura to delete this video",
            PendingIntent.getActivity(context, 1, Intent("test.consent10"), PendingIntent.FLAG_IMMUTABLE)
        )
        mediaStore.refuse = RecoverableSecurityException(SecurityException("scoped"), "Allow?", action)

        val outcome = deleter(sdk = Build.VERSION_CODES.Q).delete(videoUri)

        assertTrue("expected a consent request, got $outcome", outcome is DeleteOutcome.NeedsConsent)
        assertTrue("nothing may be deleted before the user agrees", mediaStore.deleted.isEmpty())
    }

    @Test
    fun mediaStoreRefusal_onAndroid9AndOlder_saysSoHonestly() {
        mediaStore.refuse = SecurityException("not owned by this app")

        val outcome = deleter(sdk = Build.VERSION_CODES.P).delete(videoUri)

        assertTrue(outcome is DeleteOutcome.Failed)
        assertTrue((outcome as DeleteOutcome.Failed).message.contains("Files app"))
    }

    @Test
    fun documentUri_isDeletedThroughTheDocumentProvider() {
        val uri = "content://com.example.documents/document/video%3A42"

        val outcome = deleter(isDocument = { _, _ -> true }).delete(uri)

        assertEquals(DeleteOutcome.Deleted, outcome)
        assertEquals(listOf(Uri.parse(uri)), documents.deleted)
        assertTrue("the MediaStore provider must not be involved", mediaStore.deleted.isEmpty())
    }

    @Test
    fun documentProviderRefusal_isReportedAsAFailure() {
        documents.refuse = SecurityException("read-only volume")

        val outcome = deleter(isDocument = { _, _ -> true })
            .delete("content://com.example.documents/document/video%3A42")

        assertTrue("expected Failed, got $outcome", outcome is DeleteOutcome.Failed)
    }
}
