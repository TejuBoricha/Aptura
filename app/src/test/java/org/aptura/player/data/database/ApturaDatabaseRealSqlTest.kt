package org.aptura.player.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.repository.MediaRepositoryImpl
import org.aptura.player.data.scanner.FileReconciliationEngine
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
 * Tests against a REAL Room/SQLite database (no hand-written DAO fakes) so the SQL itself is exercised:
 * REPLACE-on-conflict semantics, `isHidden` filtering, and the v4 -> v5 migration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaDatabaseRealSqlTest {

    private lateinit var context: Context
    private lateinit var db: ApturaDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ApturaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun scanned(
        mediaStoreId: Long = 100L,
        uri: String = "content://media/external/video/media/100",
        title: String = "Movie",
        sizeBytes: Long = 5_000_000L
    ) = MediaItemEntity(
        mediaStoreId = mediaStoreId,
        uri = uri,
        title = title,
        displayName = "$title.mkv",
        durationMs = 60_000L,
        sizeBytes = sizeBytes,
        mimeType = "video/x-matroska",
        dateAddedSec = 1_000L,
        folderPath = "Movies"
    )

    /** Regression for audit P0-05: the scan wiped speed/aspect/decoder/delays/rename on every run. */
    @Test
    fun rescan_preservesEveryUserOwnedColumn_throughRealRoomReplace() = runBlocking {
        val dao = db.mediaItemDao()
        dao.insertOrUpdate(scanned())
        val uri = "content://media/external/video/media/100"
        dao.updatePlaybackPosition(uri, 41_314L, true, 1234L)
        dao.updatePlaybackPreferences(uri, "FILL", 1.5f, 2, true, 150L, -200L)
        dao.updateCustomTitle(uri, "My Renamed Movie")
        dao.hideByUri(uri)
        val before = dao.getVideoByUri(uri)!!

        // A rescan: the scanner knows nothing about user data and may even report a new title.
        val freshFromScanner = scanned(title = "Movie (scanner title changed)")
        val result = FileReconciliationEngine.reconcile(listOf(freshFromScanner), dao.getAllVideosRaw())
        dao.insertOrUpdateAll(result.itemsToUpsert)

        val after = dao.getVideoByUri(uri)!!
        assertEquals(before.id, after.id)
        assertEquals(41_314L, after.playbackPositionMs)
        assertTrue(after.isCompleted)
        assertEquals(1234L, after.lastPlayedTimestamp)
        assertEquals("FILL", after.resizeMode)
        assertEquals(1.5f, after.playbackSpeed)
        assertEquals(2, after.decoderStrategy)
        assertTrue("the user's decoder choice must survive a rescan", after.decoderOverridden)
        assertEquals(150L, after.audioDelayMs)
        assertEquals(-200L, after.subtitleDelayMs)
        assertEquals("My Renamed Movie", after.customTitle)
        assertTrue("hide must survive a rescan", after.isHidden)
        assertEquals("scanner-owned fields still refresh", "Movie (scanner title changed)", after.title)
        assertEquals("My Renamed Movie", after.toDomainMediaItem().title)
    }

    @Test
    fun hiddenVideos_areExcludedEverywhere_untilRestored() = runBlocking {
        val dao = db.mediaItemDao()
        dao.insertOrUpdate(scanned(mediaStoreId = 1, uri = "content://media/1", title = "Keep", sizeBytes = 1_000L))
        dao.insertOrUpdate(
            scanned(mediaStoreId = 2, uri = "content://media/2", title = "Secret", sizeBytes = 2_000L)
                .copy(folderPath = "Private")
        )
        dao.hideByUri("content://media/2")

        assertEquals(listOf("Keep"), dao.getAllVideosSortedByDate().first().map { it.title })
        assertEquals(listOf("Movies"), dao.getAllFolders().first())
        assertTrue(dao.searchVideos("Secret").first().isEmpty())
        assertEquals(1, dao.getMediaItemCount())
        assertEquals(1_000L, dao.getTotalSizeBytes())
        assertEquals(1, dao.getHiddenCount().first())

        dao.restoreAllHidden()

        assertEquals(setOf("Keep", "Secret"), dao.getAllVideosSortedByDate().first().map { it.title }.toSet())
        assertEquals(0, dao.getHiddenCount().first())
    }

    // --- DATA-12: removing a folder removes that folder and nothing that merely starts like it ---

    private val safAuthority = "content://com.android.externalstorage.documents"

    /** A video the way the SAF scan stores it: `<tree uri>/document/<document id>`. */
    private fun safVideo(tree: String, path: String, id: Long) = scanned(
        mediaStoreId = id,
        uri = "$safAuthority/tree/$tree/document/$tree%2F$path",
        title = path
    )

    private suspend fun remainingUris(): Set<String> = db.mediaItemDao().getAllVideosRaw().map { it.uri }.toSet()

    @Test
    fun removingAFolder_keepsASiblingWhoseNameStartsTheSame() = runBlocking {
        val dao = db.mediaItemDao()
        val inFolder = safVideo("primary%3AMovies", "a.mkv", 1)
        val inSubfolder = safVideo("primary%3AMovies", "sub%2Fb.mkv", 2)
        val sibling = safVideo("primary%3AMovies2", "c.mkv", 3)
        listOf(inFolder, inSubfolder, sibling).forEach { dao.insertOrUpdate(it) }

        dao.deleteByUriPrefix("$safAuthority/tree/primary%3AMovies")

        assertEquals("only the sibling folder is left", setOf(sibling.uri), remainingUris())
    }

    @Test
    fun removingAFolder_readsPercentAndUnderscoreAsPlainCharacters() = runBlocking {
        val dao = db.mediaItemDao()
        val target = safVideo("primary%3AMy_Movies", "a.mkv", 1)
        // In a LIKE pattern `_` stands for any one character and `%` for any run of them, and every SAF tree URI
        // carries a `%` (the encoded colon). A folder that differs only at one of those places is another folder.
        val underscoreLookAlike = safVideo("primary%3AMyXMovies", "b.mkv", 2)
        val percentLookAlike = safVideo("primaryQ3AMy_Movies", "c.mkv", 3)
        listOf(target, underscoreLookAlike, percentLookAlike).forEach { dao.insertOrUpdate(it) }

        dao.deleteByUriPrefix("$safAuthority/tree/primary%3AMy_Movies")

        assertEquals(setOf(underscoreLookAlike.uri, percentLookAlike.uri), remainingUris())
    }

    @Test
    fun movedFile_remapsBookmarksToTheNewUri() = runBlocking {
        val dao = db.mediaItemDao()
        val bookmarks = db.bookmarkDao()
        val old = scanned(mediaStoreId = -7L, uri = "content://tree/old/Show.mkv", title = "Show", sizeBytes = 9_999L)
        dao.insertOrUpdate(old)
        bookmarks.insertBookmark(BookmarkEntity(mediaUri = old.uri, timestampMs = 5_000L, note = "intro"))

        val movedScan = scanned(mediaStoreId = -8L, uri = "content://tree/new/Show renamed.mkv", title = "Show renamed", sizeBytes = 9_999L)
        val result = FileReconciliationEngine.reconcile(listOf(movedScan), dao.getAllVideosRaw())
        dao.insertOrUpdateAll(result.itemsToUpsert)
        result.uriRemaps.forEach { (oldUri, newUri) -> bookmarks.remapMediaUri(oldUri, newUri) }

        assertEquals(mapOf(old.uri to movedScan.uri), result.uriRemaps)
        assertEquals("intro", bookmarks.getBookmarksForMedia(movedScan.uri).first().single().note)
        assertTrue(bookmarks.getBookmarksForMedia(old.uri).first().isEmpty())
    }

    /**
     * v4 -> latest keeps all existing rows and defaults the new columns (rename = none, hidden = false), and turns the
     * old "decoderStrategy = 0 means auto/unset" into "follow the default" while keeping a non-zero value as a choice.
     */
    @Test
    fun migration_4_to_latest_keepsUserDataAndAddsColumns() {
        val name = "migration-v4-to-latest.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL(
                """CREATE TABLE media_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    mediaStoreId INTEGER NOT NULL, uri TEXT NOT NULL, title TEXT NOT NULL,
                    displayName TEXT NOT NULL, durationMs INTEGER NOT NULL, sizeBytes INTEGER NOT NULL,
                    mimeType TEXT, width INTEGER NOT NULL, height INTEGER NOT NULL, frameRate REAL NOT NULL,
                    dateAddedSec INTEGER NOT NULL, dateModifiedSec INTEGER NOT NULL, folderPath TEXT NOT NULL,
                    playbackPositionMs INTEGER NOT NULL, isCompleted INTEGER NOT NULL,
                    lastPlayedTimestamp INTEGER NOT NULL, isMissing INTEGER NOT NULL DEFAULT 0,
                    resizeMode TEXT NOT NULL DEFAULT 'FIT', playbackSpeed REAL NOT NULL DEFAULT 1.0,
                    decoderStrategy INTEGER NOT NULL DEFAULT 0, audioDelayMs INTEGER NOT NULL DEFAULT 0,
                    subtitleDelayMs INTEGER NOT NULL DEFAULT 0)"""
            )
            old.execSQL("CREATE UNIQUE INDEX index_media_items_mediaStoreId ON media_items (mediaStoreId)")
            old.execSQL("CREATE INDEX index_media_items_folderPath ON media_items (folderPath)")
            old.execSQL("CREATE INDEX index_media_items_dateAddedSec ON media_items (dateAddedSec)")
            old.execSQL("CREATE INDEX index_media_items_title ON media_items (title)")
            old.execSQL(
                """CREATE TABLE bookmarks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mediaUri TEXT NOT NULL,
                    timestampMs INTEGER NOT NULL, note TEXT NOT NULL, createdAtSec INTEGER NOT NULL)"""
            )
            old.execSQL("CREATE INDEX index_bookmarks_mediaUri ON bookmarks (mediaUri)")
            old.execSQL("CREATE INDEX index_bookmarks_mediaUri_timestampMs ON bookmarks (mediaUri, timestampMs)")
            old.execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, playbackSpeed)
                   VALUES (1, 'content://media/1', 'Old Movie', 'Old Movie.mp4', 90000, 4096, 'video/mp4',
                    1920, 1080, 24.0, 10, 10, 'Movies', 33000, 0, 99, 1.25)"""
            )
            old.execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, decoderStrategy)
                   VALUES (2, 'content://media/2', 'Picky Movie', 'Picky Movie.mkv', 90000, 4096, 'video/x-matroska',
                    1920, 1080, 24.0, 10, 10, 'Movies', 0, 0, 0, 2)"""
            )
            old.version = 4
        }

        val migrated = Room.databaseBuilder(context, ApturaDatabase::class.java, name)
            .addMigrations(*ApturaDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val item = runBlocking { migrated.mediaItemDao().getVideoByUri("content://media/1") }
            assertNotNull("row must survive the migration (no destructive fallback)", item)
            assertEquals(33_000L, item!!.playbackPositionMs)
            assertEquals(1.25f, item.playbackSpeed)
            assertNull(item.customTitle)
            assertFalse(item.isHidden)
            assertFalse("a 0 was never a choice: it must follow the Settings default", item.decoderOverridden)

            val picky = runBlocking { migrated.mediaItemDao().getVideoByUri("content://media/2") }!!
            assertTrue("a non-zero decoder can only have been chosen by the user", picky.decoderOverridden)
            assertEquals(2, picky.decoderStrategy)
        } finally {
            migrated.close()
            file.delete()
        }
    }

    // --- DATA-01: a video moved by copy-then-delete keeps its progress and bookmarks ---

    private suspend fun watchedVideoWithABookmark(): MediaItemEntity {
        val original = scanned(mediaStoreId = -7L, uri = "content://tree/old/Show.mkv", title = "Show", sizeBytes = 9_999L)
        db.mediaItemDao().insertOrUpdate(original)
        db.mediaItemDao().updatePlaybackPosition(original.uri, 41_314L, false, 1234L)
        db.bookmarkDao().insertBookmark(BookmarkEntity(mediaUri = original.uri, timestampMs = 5_000L, note = "intro"))
        return original
    }

    /** The scan after a move: the original is gone, and a copy of the same size and length sits in a new folder. */
    private suspend fun scanFindsTheCopy(original: MediaItemEntity): MediaItemEntity {
        val copy = scanned(mediaStoreId = -8L, uri = "content://tree/new/Show.mkv", title = "Show", sizeBytes = original.sizeBytes)
        val result = FileReconciliationEngine.reconcile(listOf(copy), db.mediaItemDao().getAllVideosRaw())
        db.mediaItemDao().insertOrUpdateAll(result.itemsToUpsert)
        result.uriRemaps.forEach { (oldUri, newUri) -> db.bookmarkDao().remapMediaUri(oldUri, newUri) }
        return copy
    }

    @Test
    fun aMovedVideoMarkedMissing_carriesItsProgressAndBookmarksToTheCopy() = runBlocking {
        val repository = MediaRepositoryImpl(db.mediaItemDao(), db.bookmarkDao())
        val original = watchedVideoWithABookmark()

        repository.markVideoMissing(original.uri) // what a move does once the original is removed

        val kept = db.mediaItemDao().getVideoByUri(original.uri)!!
        assertTrue(kept.isMissing)
        assertEquals("progress survives until the scan", 41_314L, kept.playbackPositionMs)
        assertEquals("bookmarks survive until the scan", 1, db.bookmarkDao().getBookmarksForMedia(original.uri).first().size)

        val copy = scanFindsTheCopy(original)

        val carried = db.mediaItemDao().getVideoByUri(copy.uri)!!
        assertFalse(carried.isMissing)
        assertEquals("the copy continues where the original stopped", 41_314L, carried.playbackPositionMs)
        assertNull("the original's row is replaced by the copy's", db.mediaItemDao().getVideoByUri(original.uri))
        assertEquals("intro", db.bookmarkDao().getBookmarksForMedia(copy.uri).first().single().note)
        assertTrue(db.bookmarkDao().getBookmarksForMedia(original.uri).first().isEmpty())
    }

    @Test
    fun aMovedVideoRemovedFromTheIndexFirst_leavesTheScanNothingToCarryOver() = runBlocking {
        // What a move did before DATA-01: it deleted the row and its bookmarks before any scan could pair the copy.
        val repository = MediaRepositoryImpl(db.mediaItemDao(), db.bookmarkDao())
        val original = watchedVideoWithABookmark()

        repository.removeVideoFromLibrary(original.uri)
        val copy = scanFindsTheCopy(original)

        val fresh = db.mediaItemDao().getVideoByUri(copy.uri)!!
        assertEquals("the copy starts unwatched", 0L, fresh.playbackPositionMs)
        assertTrue("and the bookmarks are gone for good", db.bookmarkDao().getBookmarksForMedia(copy.uri).first().isEmpty())
    }

    @Test
    fun aMovedVideoNothingMatches_staysMissingWithItsDataUntilTheUserRemovesIt() = runBlocking {
        val repository = MediaRepositoryImpl(db.mediaItemDao(), db.bookmarkDao())
        val original = watchedVideoWithABookmark()
        repository.markVideoMissing(original.uri)

        // The copy went to a folder the library does not scan, so the scan finds only an unrelated file.
        val unrelated = scanned(mediaStoreId = -9L, uri = "content://media/external/video/media/9", title = "Other", sizeBytes = 1_234_567L)
        val result = FileReconciliationEngine.reconcile(listOf(unrelated), db.mediaItemDao().getAllVideosRaw())
        db.mediaItemDao().insertOrUpdateAll(result.itemsToUpsert)

        val stays = db.mediaItemDao().getVideoByUri(original.uri)!!
        assertTrue(stays.isMissing)
        assertEquals(41_314L, stays.playbackPositionMs)
        assertEquals(1, db.bookmarkDao().getBookmarksForMedia(original.uri).first().size)
        assertTrue("it is listed as a missing video", db.mediaItemDao().getMissingVideos().first().any { it.uri == original.uri })
    }

    @Test
    fun updateBookmarkNote_persistsUpdatedNoteInDatabase() = runBlocking {
        val bookmarkDao = db.bookmarkDao()
        val id = bookmarkDao.insertBookmark(
            BookmarkEntity(
                mediaUri = "content://media/1",
                timestampMs = 15_000L,
                note = "Initial note"
            )
        )

        bookmarkDao.updateBookmarkNote(id, "Opening sequence")
        val bookmarks = bookmarkDao.getBookmarksForMedia("content://media/1").first()
        assertEquals(1, bookmarks.size)
        assertEquals("Opening sequence", bookmarks[0].note)
    }

    @Test
    fun clearAllPlaybackHistory_resetsPlaybackPositionAndCompletedAcrossAllVideos() = runBlocking {
        val dao = db.mediaItemDao()
        val video1 = scanned(mediaStoreId = 1L, uri = "content://media/1", title = "V1")
            .copy(playbackPositionMs = 25_000L, isCompleted = false, lastPlayedTimestamp = 1000L)
        val video2 = scanned(mediaStoreId = 2L, uri = "content://media/2", title = "V2")
            .copy(playbackPositionMs = 50_000L, isCompleted = true, lastPlayedTimestamp = 2000L)
        dao.insertOrUpdateAll(listOf(video1, video2))

        dao.clearAllPlaybackHistory()

        val after1 = dao.getVideoByUri("content://media/1")!!
        val after2 = dao.getVideoByUri("content://media/2")!!

        assertEquals(0L, after1.playbackPositionMs)
        assertFalse(after1.isCompleted)
        assertEquals(0L, after1.lastPlayedTimestamp)

        assertEquals(0L, after2.playbackPositionMs)
        assertFalse(after2.isCompleted)
        assertEquals(0L, after2.lastPlayedTimestamp)
    }
}
