package org.aptura.player.core.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaMediaFileManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var provider: RenameProvider

    class RenameProvider : ContentProvider() {
        var displayName = "Holiday.mp4"
        var requestedName: String? = null
        var renamedUri: Uri = Uri.parse("content://$AUTHORITY/document/Holiday-renamed.mp4")

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply {
            addRow(arrayOf(displayName))
        }

        override fun getType(uri: Uri) = "video/mp4"
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ) = 0

        override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
            if (method == "android:renameDocument") {
                // DocumentsContract sends the new name under Document.COLUMN_DISPLAY_NAME and reads the new
                // document URI back from the hidden "uri" extras key.
                requestedName = extras?.getString(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                displayName = requestedName ?: displayName
                return Bundle().apply { putParcelable("uri", renamedUri) }
            }
            return super.call(method, arg, extras)
        }
    }

    @Before
    fun registerProvider() {
        provider = Robolectric.setupContentProvider(RenameProvider::class.java, AUTHORITY)
    }

    @Test
    fun renameWithoutExtensionKeepsTheRealFileExtension() {
        assertRename("Summer", "Summer.mp4")
    }

    @Test
    fun renameWithAnExplicitExtensionKeepsIt() {
        assertRename("Summer.mkv", "Summer.mkv")
    }

    @Test
    fun aYearInTheTitleIsNotMistakenForAnExtension() {
        assertRename("Summer.2024", "Summer.2024.mp4")
    }

    @Test
    fun aTrailingDotDoesNotProduceADoubleExtension() {
        assertRename("Summer.", "Summer.mp4")
    }

    @Test
    fun renamingALocalFileReturnsTheNewUri() {
        val directory = java.io.File(context.cacheDir, "rename-test").apply { mkdirs() }
        val source = java.io.File(directory, "Clip.mp4").apply { writeText("fixture") }

        val result = ApturaMediaFileManager.renameMediaFile(
            context = context,
            uri = Uri.fromFile(source),
            newNameWithExt = "Holiday"
        )

        assertTrue(result is MediaRenameResult.Renamed)
        val renamed = result as MediaRenameResult.Renamed
        assertEquals("Holiday.mp4", renamed.displayName)
        assertEquals(Uri.fromFile(java.io.File(directory, "Holiday.mp4")), renamed.uri)
        assertTrue(java.io.File(directory, "Holiday.mp4").exists())
    }

    private fun assertRename(requested: String, expected: String) {
        val uri = Uri.parse("content://$AUTHORITY/document/Holiday.mp4")
        provider.renamedUri = Uri.parse("content://$AUTHORITY/document/$expected")
        val result = ApturaMediaFileManager.renameMediaFile(
            context = context,
            uri = uri,
            newNameWithExt = requested
        )

        assertTrue(result is MediaRenameResult.Renamed)
        // A document provider allocates a new URI; the caller must persist it.
        assertEquals(provider.renamedUri, (result as MediaRenameResult.Renamed).uri)
        assertEquals(expected, result.displayName)
        assertEquals(expected, provider.requestedName)
    }

    private companion object {
        const val AUTHORITY = "org.aptura.test.rename"
    }
}
