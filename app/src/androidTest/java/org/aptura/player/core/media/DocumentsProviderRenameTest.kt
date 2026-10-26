package org.aptura.player.core.media

import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented proof for the SAF rename contract (audit 2026-10-24, F-18): the provider allocates a new document ID,
 * and the app returns that URI rather than the one it was given. The unit tests use a plain ContentProvider; this one
 * runs the real `DocumentsProvider`/`DocumentsContract` path on a device.
 */
@RunWith(AndroidJUnit4::class)
class DocumentsProviderRenameTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetProvider() {
        RenamingDocumentsProvider.reset()
    }

    @Test
    fun renamingADocument_returnsTheNewUri_andTheOldIdStopsResolving() {
        val before = RenamingDocumentsProvider.documentUri()

        val result = ApturaMediaFileManager.renameMediaFile(context, before, "Holiday Renamed")

        assertTrue("expected a successful rename, was $result", result is MediaRenameResult.Renamed)
        val renamed = result as MediaRenameResult.Renamed
        assertEquals("Holiday Renamed.mp4", renamed.displayName)
        assertNotEquals("the provider allocated a new URI; returning the old one would orphan the row", before, renamed.uri)
        assertEquals("renamed-1", DocumentsContract.getDocumentId(renamed.uri))
        assertEquals("renamed-1", RenamingDocumentsProvider.documentId)
        assertEquals("Holiday Renamed.mp4", displayName(renamed.uri))
        assertNull("the old document ID must no longer resolve after the provider renamed it", displayName(before))
    }

    @Test
    fun aTitleThatAlreadyHasAnExtension_isSentToTheProviderUnchanged() {
        val before = RenamingDocumentsProvider.documentUri()

        val result = ApturaMediaFileManager.renameMediaFile(context, before, "Holiday.2026.mkv")

        assertTrue("expected a successful rename, was $result", result is MediaRenameResult.Renamed)
        val renamed = result as MediaRenameResult.Renamed
        assertEquals("Holiday.2026.mkv", renamed.displayName)
        assertEquals("Holiday.2026.mkv", displayName(renamed.uri))
    }

    private fun displayName(uri: Uri): String? = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}
