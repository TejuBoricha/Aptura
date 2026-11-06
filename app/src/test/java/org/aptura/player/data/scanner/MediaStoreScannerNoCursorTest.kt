package org.aptura.player.data.scanner

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit DATA-04: a MediaStore that does not answer (a null cursor) used to read as "no videos", so a scan made every
 * existing row look deleted. It is now a failed scan. A MediaStore that answers with no rows is still an empty library.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaStoreScannerNoCursorTest {

    class SwitchableProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? =
            if (answers) MatrixCursor(projection!!) else null

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
    }

    companion object {
        var answers = true
    }

    @Before
    fun registerTheProvider() {
        Robolectric.setupContentProvider(SwitchableProvider::class.java, MediaStore.AUTHORITY)
    }

    @After
    fun reset() {
        answers = true
    }

    private fun scan() = runBlocking {
        MediaStoreScanner(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver).scanAllVideos()
    }

    @Test
    fun aMediaStoreThatDoesNotAnswerIsAFailedScan() {
        answers = false
        var thrown: Throwable? = null
        try {
            scan()
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue("expected MediaStoreUnavailableException but got $thrown", thrown is MediaStoreUnavailableException)
    }

    @Test
    fun aMediaStoreThatAnswersWithNoRowsIsAnEmptyLibrary() {
        answers = true
        assertEquals(emptyList<Any>(), scan())
    }
}
