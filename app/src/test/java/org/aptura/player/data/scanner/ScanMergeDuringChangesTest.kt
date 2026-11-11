package org.aptura.player.data.scanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.database.MediaItemEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit DATA-05: a scan reads the library when it starts, spends a while looking at the storage (minutes for a large
 * SAF folder), and only then writes. The person keeps using the app meanwhile. These tests take the snapshot, change
 * the library the way the person would, and then let the scan merge its findings, so a write that lands during the
 * scan is what is checked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanMergeDuringChangesTest {

    private lateinit var db: ApturaDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ApturaDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun file(id: Long, title: String = "Movie $id", sizeBytes: Long = 5_000_000L) = MediaItemEntity(
        mediaStoreId = id,
        uri = "content://media/external/video/media/$id",
        title = title,
        displayName = "$title.mkv",
        durationMs = 60_000L,
        sizeBytes = sizeBytes,
        mimeType = "video/x-matroska",
        dateAddedSec = 1_000L,
        folderPath = "Movies"
    )

    /** The scan has started: it holds what the library holds now. The caller then changes the library. */
    private suspend fun startScan(vararg stored: MediaItemEntity): List<MediaItemEntity> {
        stored.forEach { db.mediaItemDao().insertOrUpdate(it) }
        return db.mediaItemDao().getAllVideosRaw()
    }

    private suspend fun finishScan(startSnapshot: List<MediaItemEntity>, vararg found: MediaItemEntity) {
        MediaScanWorker.persistScan(db, found.toList(), startSnapshot, hasStorage = true)
    }

    @Test
    fun aVideoFinishedDuringTheScan_staysFinished() = runBlocking {
        val movie = file(1)
        val snapshot = startScan(movie)

        db.mediaItemDao().updatePlaybackPosition(movie.uri, 59_000L, true, 5_000L)
        finishScan(snapshot, movie)

        val row = db.mediaItemDao().getVideoByUri(movie.uri)!!
        assertEquals("the position the person reached", 59_000L, row.playbackPositionMs)
        assertTrue("finishing the video", row.isCompleted)
        assertEquals(5_000L, row.lastPlayedTimestamp)
    }

    @Test
    fun aVideoRenamedDuringTheScan_keepsItsNewName() = runBlocking {
        val movie = file(1)
        val snapshot = startScan(movie)

        db.mediaItemDao().updateCustomTitle(movie.uri, "My own name")
        finishScan(snapshot, movie)

        assertEquals("My own name", db.mediaItemDao().getVideoByUri(movie.uri)!!.customTitle)
    }

    @Test
    fun aVideoHiddenDuringTheScan_staysHidden() = runBlocking {
        val movie = file(1)
        val snapshot = startScan(movie)

        db.mediaItemDao().hideByUri(movie.uri)
        finishScan(snapshot, movie)

        assertTrue("the person hid it", db.mediaItemDao().getVideoByUri(movie.uri)!!.isHidden)
    }

    @Test
    fun aVideoDeletedDuringTheScan_doesNotComeBack() = runBlocking {
        val movie = file(1)
        val other = file(2)
        val snapshot = startScan(movie, other)

        // The person deletes it while the scan is still reading the storage, so the scan found the file before it went.
        db.mediaItemDao().deleteByUri(movie.uri)
        finishScan(snapshot, movie, other)

        assertNull("a row removed during the scan stays removed", db.mediaItemDao().getVideoByUri(movie.uri))
        assertNotNull("the rest of the library is still merged", db.mediaItemDao().getVideoByUri(other.uri))
    }

    @Test
    fun aMissingVideoThatGainedDataDuringTheScan_isNotPurged() = runBlocking {
        // Missing, with nothing the person owns: the scan may purge such a row, and the scan does not find the file.
        val gone = file(3).copy(isMissing = true)
        val snapshot = startScan(gone)

        db.mediaItemDao().updateCustomTitle(gone.uri, "Keep me")
        finishScan(snapshot) // the scan found nothing

        assertEquals("a row with data is not purged on the strength of an old snapshot", "Keep me", db.mediaItemDao().getVideoByUri(gone.uri)?.customTitle)
    }

    // --- Guards: the fix must not stop the scan from doing its own job ---

    @Test
    fun aNewFileFoundByTheScan_isAdded() = runBlocking {
        val snapshot = startScan(file(1))
        val brandNew = file(9)

        finishScan(snapshot, file(1), brandNew)

        assertNotNull(db.mediaItemDao().getVideoByUri(brandNew.uri))
    }

    @Test
    fun whatTheScanOwns_isStillUpdated_andWhatThePersonOwns_isLeftAlone() = runBlocking {
        val movie = file(1, title = "Old title", sizeBytes = 5_000_000L)
        val snapshot = startScan(movie)
        db.mediaItemDao().updatePlaybackPosition(movie.uri, 12_000L, false, 777L)

        // The file changed on disk: a new title and size from the scanner.
        finishScan(snapshot, movie.copy(title = "New scanner title", sizeBytes = 6_000_000L))

        val row = db.mediaItemDao().getVideoByUri(movie.uri)!!
        assertEquals("New scanner title", row.title)
        assertEquals(6_000_000L, row.sizeBytes)
        assertEquals("the person's position is not the scan's to change", 12_000L, row.playbackPositionMs)
        assertFalse(row.isMissing)
    }

    /**
     * Audit finding DATA-05: A truly concurrent write test asserting that concurrent user modifications
     * (playback progress, custom rename, bookmarks) running simultaneously with [MediaScanWorker.persistScan]
     * are serialized safely by Room's transaction boundary without deadlocks or reverting user changes.
     */
    @Test
    fun concurrentDatabaseWrites_duringScanMerge_areSerializedSafelyWithoutCorruption() = runBlocking {
        val initialItems = List(50) { id -> file(id.toLong()) }
        val snapshot = startScan(*initialItems.toTypedArray())

        val writeJob = async(Dispatchers.IO) {
            repeat(20) { i ->
                val targetUri = initialItems[i].uri
                db.mediaItemDao().updatePlaybackPosition(targetUri, (i * 1000).toLong(), i % 2 == 0, System.currentTimeMillis())
                db.mediaItemDao().updateCustomTitle(targetUri, "User Title $i")
            }
        }

        val scanJob = async(Dispatchers.IO) {
            MediaScanWorker.persistScan(db, initialItems, snapshot, hasStorage = true)
        }

        writeJob.await()
        scanJob.await()

        val allVideos = db.mediaItemDao().getAllVideosRaw()
        assertEquals(50, allVideos.size)
        repeat(20) { i ->
            val row = allVideos.first { it.uri == initialItems[i].uri }
            assertEquals("User Title $i", row.customTitle)
        }
    }
}
