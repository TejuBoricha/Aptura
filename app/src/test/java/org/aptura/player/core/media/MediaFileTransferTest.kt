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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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

    class DestinationProvider : ContentProvider() {
        lateinit var outputFile: File
        lateinit var createdUri: Uri

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply {
            addRow(arrayOf(uri.lastPathSegment.orEmpty()))
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
            if (method == "android:createDocument") {
                return Bundle().apply {
                    putParcelable("uri", createdUri)
                }
            }
            return super.call(method, arg, extras)
        }
    }

    @Before
    fun registerDestinationProvider() {
        destinationFile = temporaryFolder.newFile("copied.mp4")
        destinationFile.writeBytes(byteArrayOf())
        val provider = Robolectric.setupContentProvider(DestinationProvider::class.java, "org.aptura.test.documents")
        provider.outputFile = destinationFile
        provider.createdUri = Uri.parse("content://org.aptura.test.documents/document/copied.mp4")
    }

    @Test
    fun copyIntoChosenSafFolder_copiesBytesAndKeepsSource() {
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
}
