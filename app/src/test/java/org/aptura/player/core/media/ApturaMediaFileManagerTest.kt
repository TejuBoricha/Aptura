package org.aptura.player.core.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
        var updatedValues: ContentValues? = null

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
        ): Int {
            updatedValues = values
            return 1
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

    private fun assertRename(requested: String, expected: String) {
        val renamed = ApturaMediaFileManager.renameMediaFile(
            context = context,
            uri = Uri.parse("content://$AUTHORITY/video/1"),
            newNameWithExt = requested
        )

        assertEquals(true, renamed)
        assertEquals(expected, provider.updatedValues?.getAsString(OpenableColumns.DISPLAY_NAME))
    }

    private companion object {
        const val AUTHORITY = "org.aptura.test.rename"
    }
}
