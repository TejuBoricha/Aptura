package org.aptura.player.core.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Details screen showed `/storage/emulated/0/Verify/Verify_D` for a file that is really
 * `/storage/emulated/0/Movies/Verify/Verify_D.mp4` (guessed from the folder's display name), and Copy Path copied
 * the wrong string. These tests answer from a real ContentProvider round trip, like MediaStore does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaPathResolverTest {

    /** A MediaStore stand-in that returns whatever columns the caller asks for from a fixed row. */
    class FakeMediaStore : ContentProvider() {
        var row: Map<String, String?> = emptyMap()
        var failQuery = false

        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? {
            if (failQuery) throw SecurityException("no access")
            val columns = projection ?: row.keys.toTypedArray()
            return MatrixCursor(columns).apply { addRow(columns.map { row[it] }.toTypedArray()) }
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?) = 0
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var mediaStore: FakeMediaStore
    private val uri = "content://media/external/video/media/42"
    private val primaryRoot = { "/storage/emulated/0" }

    @Before
    fun registerProvider() {
        mediaStore = Robolectric.setupContentProvider(FakeMediaStore::class.java, "media")
    }

    @Test
    fun nestedFolder_andRealFileName_areReported() {
        mediaStore.row = mapOf(
            "relative_path" to "Movies/Verify/",
            "_display_name" to "Verify_D.mp4",
            "volume_name" to "external_primary"
        )

        assertEquals(
            "/storage/emulated/0/Movies/Verify/Verify_D.mp4",
            MediaPathResolver.resolve(context, uri, primaryRoot)
        )
    }

    @Test
    fun aFileOnAnSdCard_isNotPlacedOnInternalStorage() {
        mediaStore.row = mapOf(
            "relative_path" to "DCIM/Camera/",
            "_display_name" to "clip.mp4",
            "volume_name" to "1A2B-3C4D"
        )

        val path = MediaPathResolver.resolve(context, uri, primaryRoot)

        assertEquals("DCIM/Camera/clip.mp4", path) // honest, volume-relative — never a made-up /storage/emulated/0 prefix
    }

    @Test
    fun whenAndroidWillNotSay_theAnswerIsNullNotAGuess() {
        mediaStore.failQuery = true
        assertNull(MediaPathResolver.resolve(context, uri, primaryRoot))

        mediaStore.failQuery = false
        mediaStore.row = emptyMap()
        assertNull(MediaPathResolver.resolve(context, uri, primaryRoot))
    }

    @Test
    fun fileUris_areTheirOwnPath() {
        assertEquals("/sdcard/Movies/a.mp4", MediaPathResolver.resolve(context, "file:///sdcard/Movies/a.mp4", primaryRoot))
    }

    @Test
    fun documentsAndUnknownSchemes_areUnknown() {
        assertNull(MediaPathResolver.resolve(context, "content://com.android.externalstorage.documents/document/primary%3AMovies%2Fa.mp4", primaryRoot))
        assertNull(MediaPathResolver.resolve(context, "https://example.com/a.mp4", primaryRoot))
    }
}
