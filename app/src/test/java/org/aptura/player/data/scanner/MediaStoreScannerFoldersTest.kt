package org.aptura.player.data.scanner

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What the library is given when two folders have the same name, read through the real scanner. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaStoreScannerFoldersTest {

    class FakeVideoProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {
            val columns = projection!!
            return MatrixCursor(columns).also { cursor ->
                rows.forEach { row -> cursor.addRow(Array(columns.size) { row[columns[it]] }) }
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
    }

    companion object {
        var rows: List<Map<String, Any?>> = emptyList()
    }

    private fun video(id: Long, bucket: String, relativePath: String?, volume: String?) = mapOf(
        MediaStore.Video.Media._ID to id,
        MediaStore.Video.Media.DISPLAY_NAME to "clip$id.mp4",
        MediaStore.Video.Media.TITLE to "clip$id",
        MediaStore.Video.Media.DURATION to 60_000L,
        MediaStore.Video.Media.SIZE to 5_000_000L,
        MediaStore.Video.Media.MIME_TYPE to "video/mp4",
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME to bucket,
        MediaStore.MediaColumns.RELATIVE_PATH to relativePath,
        MediaStore.MediaColumns.VOLUME_NAME to volume
    )

    @Before
    fun registerTheProvider() {
        Robolectric.setupContentProvider(FakeVideoProvider::class.java, MediaStore.AUTHORITY)
    }

    @After
    fun clear() {
        rows = emptyList()
    }

    private fun scan() = kotlinx.coroutines.runBlocking {
        MediaStoreScanner(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver).scanAllVideos()
    }

    @Test
    fun twoFoldersCalledCamera_comeOutAsTwoFolders() {
        rows = listOf(
            video(1, "Camera", "DCIM/Camera/", "external_primary"),
            video(2, "Camera", "DCIM/Camera/", "external_primary"),
            video(3, "Camera", "DCIM/Camera/", "1a2b-3c4d")
        )

        val byFolder = scan().groupBy { it.folderPath }

        assertEquals(setOf("/storage/emulated/0/DCIM/Camera", "/storage/1A2B-3C4D/DCIM/Camera"), byFolder.keys)
        assertEquals(2, byFolder.getValue("/storage/emulated/0/DCIM/Camera").size)
        assertEquals(1, byFolder.getValue("/storage/1A2B-3C4D/DCIM/Camera").size)
    }

    @Test
    fun aRowWithNoLocation_staysInItsBucket() {
        rows = listOf(video(1, "Old downloads", null, null))
        assertEquals(listOf("Old downloads"), scan().map { it.folderPath })
    }

    @Test
    fun aRescanGivesAnExistingVideoItsNewFolder_andKeepsWhatThePersonDid() {
        // The folder key used to be the bucket name. The startup scan rewrites it; progress and hiding must survive.
        val before = org.aptura.player.data.database.MediaItemEntity(
            mediaStoreId = 1, uri = "content://media/external/video/media/1", title = "clip1", displayName = "clip1.mp4",
            durationMs = 60_000L, sizeBytes = 5_000_000L, mimeType = "video/mp4", folderPath = "Camera",
            playbackPositionMs = 30_000L, isHidden = true
        )
        rows = listOf(video(1, "Camera", "DCIM/Camera/", "external_primary"))

        val result = FileReconciliationEngine.reconcile(scan(), listOf(before))
        val after = result.itemsToUpsert.single()

        assertEquals("/storage/emulated/0/DCIM/Camera", after.folderPath)
        assertEquals(30_000L, after.playbackPositionMs)
        assertEquals(true, after.isHidden)
    }
}
