package org.aptura.player.core.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaFileTransferTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var destinationFile: File
    private lateinit var provider: DestinationProvider

    class DestinationProvider : ContentProvider() {
        lateinit var outputFile: File
        lateinit var createdUri: Uri
        var availableBytes: Long? = null
        val deletedUris = mutableListOf<Uri>()

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            if (uri == DocumentsContract.buildRootsUri("org.aptura.test.documents")) {
                return MatrixCursor(
                    arrayOf(
                        DocumentsContract.Root.COLUMN_ROOT_ID,
                        DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
                    )
                ).apply {
                    addRow(arrayOf<Any?>("root", availableBytes))
                }
            }
            return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply {
                addRow(arrayOf(uri.lastPathSegment.orEmpty()))
            }
        }

        override fun getType(uri: Uri) = "video/mp4"
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            return ParcelFileDescriptor.open(
                outputFile,
                ParcelFileDescriptor.MODE_WRITE_ONLY or
                    ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE
            )
        }

        override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
            return when (method) {
                "android:createDocument" -> Bundle().apply {
                    putParcelable("uri", createdUri)
                }

                "android:deleteDocument" -> {
                    val uri = extras?.getParcelable("uri", Uri::class.java)
                    if (uri != null) deletedUris += uri
                    outputFile.delete()
                    Bundle.EMPTY
                }

                else -> super.call(method, arg, extras)
            }
        }
    }

    @Before
    fun registerDestinationProvider() {
        destinationFile = temporaryFolder.newFile("copied.mp4")
        destinationFile.writeBytes(byteArrayOf())
        provider = Robolectric.setupContentProvider(
            DestinationProvider::class.java,
            "org.aptura.test.documents"
        )
        provider.outputFile = destinationFile
        provider.createdUri = Uri.parse("content://org.aptura.test.documents/document/copied.mp4")
    }

    @Test
    fun copyIntoChosenSafFolder_copiesBytesAndKeepsSource() = runBlocking {
        val source = temporaryFolder.newFile("source.mp4")
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        source.writeBytes(bytes)
        val destinationTree = Uri.parse("content://org.aptura.test.documents/tree/root")

        val result = MediaFileTransfer.copyIntoTree(
            context,
            listOf(Uri.fromFile(source).toString()),
            destinationTree
        )

        assertEquals(1, result.copied)
        assertTrue(result.failedUris.isEmpty())
        assertArrayEquals(bytes, destinationFile.readBytes())
        assertTrue("copying must not remove the source; cut deletes only after a successful copy", source.exists())
    }

    @Test
    fun copyReportsByteProgressAndFinishesAtOneHundredPercent() = runBlocking {
        val source = temporaryFolder.newFile("progress.mp4")
        source.writeBytes(ByteArray(300_000) { (it % 251).toByte() })
        val destinationTree = Uri.parse("content://org.aptura.test.documents/tree/root")
        val updates = mutableListOf<MediaTransferProgress>()

        val result = MediaFileTransfer.copyIntoTree(
            context,
            listOf(Uri.fromFile(source).toString()),
            destinationTree
        ) { updates += it }

        assertEquals(1, result.copied)
        assertTrue("the dialog needs at least a start and a finish update", updates.size >= 2)
        assertEquals(0L, updates.first().copiedBytes)
        val last = updates.last()
        assertEquals(1, last.completedFiles)
        assertEquals(1, last.totalFiles)
        assertEquals(source.length(), last.copiedBytes)
        assertEquals(source.length(), last.totalBytes)
        assertEquals(1f, last.fraction)
    }

    @Test
    fun resumedCopy_skipsCompletedSourceWithoutCreatingAnotherDestination() = runBlocking {
        val first = temporaryFolder.newFile("first.mp4").also { it.writeBytes(byteArrayOf(1, 2, 3)) }
        val second = temporaryFolder.newFile("second.mp4").also { it.writeBytes(byteArrayOf(4, 5, 6)) }
        val firstUri = Uri.fromFile(first).toString()
        val secondUri = Uri.fromFile(second).toString()
        val createdSources = mutableListOf<String>()

        val result = MediaFileTransfer.copyIntoTree(
            context,
            listOf(firstUri, secondUri),
            Uri.parse("content://org.aptura.test.documents/tree/root"),
            skipCopied = setOf(firstUri),
            onDocumentCreated = { source, _ -> createdSources += source }
        )

        assertEquals(listOf(firstUri, secondUri), result.copiedUris)
        assertEquals(listOf(secondUri), createdSources)
        assertArrayEquals(second.readBytes(), destinationFile.readBytes())
    }

    @Test
    fun measureSpace_reportsShortfallBeforeAnyBytesAreCopied() {
        val source = temporaryFolder.newFile("space.mp4")
        source.writeBytes(ByteArray(6))
        provider.availableBytes = 3L
        val destinationTree = Uri.parse("content://org.aptura.test.documents/tree/root")

        val space = MediaFileTransfer.measureSpace(
            context,
            listOf(Uri.fromFile(source).toString()),
            destinationTree
        )

        assertFalse(space.hasEnoughSpace)
        assertEquals(6L, space.requiredBytes)
        assertEquals(3L, space.availableBytes)
    }

    @Test
    fun measureSpace_allowsTheCopyWhenTheProviderReportsNoQuota() {
        val source = temporaryFolder.newFile("no-quota.mp4")
        source.writeBytes(ByteArray(4))
        provider.availableBytes = null
        val destinationTree = Uri.parse("content://org.aptura.test.documents/tree/root")

        val space = MediaFileTransfer.measureSpace(
            context,
            listOf(Uri.fromFile(source).toString()),
            destinationTree
        )

        assertTrue(space.hasEnoughSpace)
        assertEquals(4L, space.requiredBytes)
        assertEquals(null, space.availableBytes)
    }

    @Test
    fun cancellingMidCopyDeletesThePartialDestinationAndKeepsTheSource() = runTest {
        val source = temporaryFolder.newFile("cancel.mp4")
        val bytes = ByteArray(256 * 1024) { (it % 251).toByte() }
        source.writeBytes(bytes)
        val destinationTree = Uri.parse("content://org.aptura.test.documents/tree/root")

        val copyJob = launch {
            // The chunk loop yields, so the canceller below runs after the first chunk is written.
            MediaFileTransfer.copyIntoTree(
                context,
                listOf(Uri.fromFile(source).toString()),
                destinationTree
            )
        }
        launch {
            yield()
            copyJob.cancel()
        }
        copyJob.join()

        assertTrue("the partial destination document must be deleted", provider.deletedUris.isNotEmpty())
        assertFalse("the truncated copy must not stay on disk", destinationFile.exists())
        assertTrue("cancelling must keep the original", source.exists())
        assertArrayEquals(bytes, source.readBytes())
    }

    @Test
    fun relativePathFor_formatsFolderKeysIntoRelativePaths() {
        assertEquals("Movies/", MediaFileTransfer.relativePathFor("/storage/emulated/0/Movies"))
        assertEquals("DCIM/Camera/", MediaFileTransfer.relativePathFor("/storage/emulated/0/DCIM/Camera"))
        assertEquals("Download/", MediaFileTransfer.relativePathFor("/storage/emulated/0/Download"))
        assertEquals("Movies/", MediaFileTransfer.relativePathFor("/storage/1234-5678/Movies"))
        assertEquals("Movies/", MediaFileTransfer.relativePathFor("Movies"))
        assertEquals("Movies/Action/", MediaFileTransfer.relativePathFor("Movies/Action"))
        assertEquals("", MediaFileTransfer.relativePathFor("/storage/emulated/0"))
    }

    @Test
    fun collectionUriFor_returnsExternalOrVolumeUri() {
        assertEquals(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaFileTransfer.collectionUriFor("/storage/emulated/0/Movies")
        )
        assertEquals(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaFileTransfer.collectionUriFor("Movies")
        )
    }

    @Test
    fun copy_withSafTreeUri_delegatesToTreeCopy() = runBlocking {
        val source = temporaryFolder.newFile("unified_tree_source.mp4")
        val bytes = byteArrayOf(10, 20, 30)
        source.writeBytes(bytes)
        val destinationTree = "content://org.aptura.test.documents/tree/root"

        val result = MediaFileTransfer.copy(
            context,
            listOf(Uri.fromFile(source).toString()),
            destinationTree
        )

        assertEquals(1, result.copied)
        assertTrue(result.failedUris.isEmpty())
        assertArrayEquals(bytes, destinationFile.readBytes())
    }
}
