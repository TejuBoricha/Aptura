package org.aptura.player.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        dao.updatePlaybackPreferences(uri, "FILL", 1.5f, 2, 150L, -200L)
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

    /** v4 -> v5 keeps all existing rows and defaults the new columns (rename = none, hidden = false). */
    @Test
    fun migration_4_to_5_keepsUserDataAndAddsColumns() {
        val name = "migration-v4-to-v5.db"
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
            old.version = 4
        }

        val migrated = Room.databaseBuilder(context, ApturaDatabase::class.java, name)
            .addMigrations(ApturaDatabase.MIGRATION_2_3, ApturaDatabase.MIGRATION_3_4, ApturaDatabase.MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()
        try {
            val item = runBlocking { migrated.mediaItemDao().getVideoByUri("content://media/1") }
            assertNotNull("row must survive the migration (no destructive fallback)", item)
            assertEquals(33_000L, item!!.playbackPositionMs)
            assertEquals(1.25f, item.playbackSpeed)
            assertNull(item.customTitle)
            assertFalse(item.isHidden)
        } finally {
            migrated.close()
            file.delete()
        }
    }
}
