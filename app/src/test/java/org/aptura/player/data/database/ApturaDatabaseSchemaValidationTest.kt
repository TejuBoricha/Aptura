package org.aptura.player.data.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real SQL & Room schema validation suite using [MigrationTestHelper] (audit QA-06).
 * Validates every committed Room schema snapshot (v5, v6, v7) against [ApturaDatabase.ALL_MIGRATIONS]
 * so missing columns, mismatched types, or forgotten indices fail automatically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaDatabaseSchemaValidationTest {

    companion object {
        private const val TEST_DB = "aptura-migration-helper-test.db"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ApturaDatabase::class.java
    )

    @Test
    fun migrate5To6_validatesSchemaAndPreservesDecoderChoice() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, isMissing, resizeMode, playbackSpeed, decoderStrategy,
                    audioDelayMs, subtitleDelayMs, customTitle, isHidden)
                   VALUES (10, 'content://media/10', 'Default Movie', 'Default Movie.mp4', 120000, 1024, 'video/mp4',
                    1920, 1080, 24.0, 100, 100, 'Movies', 15000, 0, 500, 0, 'FIT', 1.0, 0, 0, 0, NULL, 0)"""
            )
            execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, isMissing, resizeMode, playbackSpeed, decoderStrategy,
                    audioDelayMs, subtitleDelayMs, customTitle, isHidden)
                   VALUES (11, 'content://media/11', 'Software Movie', 'Software Movie.mkv', 120000, 2048, 'video/x-matroska',
                    1920, 1080, 24.0, 100, 100, 'Movies', 0, 0, 0, 0, 'FIT', 1.25, 2, 100, -50, 'Custom', 0)"""
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 6, true, ApturaDatabase.MIGRATION_5_6)
        migratedDb.query("SELECT mediaStoreId, decoderStrategy, decoderOverridden FROM media_items ORDER BY mediaStoreId").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(10L, cursor.getLong(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))

            assertTrue(cursor.moveToNext())
            assertEquals(11L, cursor.getLong(0))
            assertEquals(2, cursor.getInt(1))
            assertEquals(1, cursor.getInt(2))
        }
        migratedDb.close()
    }

    @Test
    fun migrate6To7_validatesSchemaAndCreatesUriIndex() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, isMissing, resizeMode, playbackSpeed, decoderStrategy,
                    audioDelayMs, subtitleDelayMs, customTitle, isHidden, decoderOverridden)
                   VALUES (20, 'content://media/20', 'Indexed Movie', 'Indexed Movie.mp4', 90000, 4096, 'video/mp4',
                    1280, 720, 30.0, 200, 200, 'DCIM', 5000, 0, 800, 0, 'FIT', 1.0, 0, 0, 0, NULL, 0, 0)"""
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 7, true, ApturaDatabase.MIGRATION_6_7)
        val planSteps = mutableListOf<String>()
        migratedDb.query("EXPLAIN QUERY PLAN SELECT * FROM media_items WHERE uri = 'content://media/20'").use { cursor ->
            while (cursor.moveToNext()) {
                planSteps.add(cursor.getString(cursor.getColumnIndexOrThrow("detail")))
            }
        }
        assertTrue(
            "expected query plan to use index_media_items_uri, got $planSteps",
            planSteps.any { it.contains("index_media_items_uri") }
        )
        migratedDb.close()
    }

    @Test
    fun migrate7To8_validatesSchemaAndPreservesAudioDelayOverride() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, isMissing, resizeMode, playbackSpeed, decoderStrategy,
                    audioDelayMs, subtitleDelayMs, customTitle, isHidden, decoderOverridden)
                   VALUES (21, 'content://media/21', 'Default Sync Movie', 'Default Sync.mp4', 90000, 4096, 'video/mp4',
                    1280, 720, 30.0, 200, 200, 'DCIM', 0, 0, 0, 0, 'FIT', 1.0, 0, 0, 0, NULL, 0, 0)"""
            )
            execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, isMissing, resizeMode, playbackSpeed, decoderStrategy,
                    audioDelayMs, subtitleDelayMs, customTitle, isHidden, decoderOverridden)
                   VALUES (22, 'content://media/22', 'Manual Sync Movie', 'Manual Sync.mp4', 90000, 4096, 'video/mp4',
                    1280, 720, 30.0, 200, 200, 'DCIM', 0, 0, 0, 0, 'FIT', 1.0, 0, -150, 0, NULL, 0, 0)"""
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 8, true, ApturaDatabase.MIGRATION_7_8)
        migratedDb.query("SELECT mediaStoreId, audioDelayMs, audioDelayOverridden FROM media_items ORDER BY mediaStoreId").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(21L, cursor.getLong(0))
            assertEquals(0L, cursor.getLong(1))
            assertEquals(0, cursor.getInt(2))

            assertTrue(cursor.moveToNext())
            assertEquals(22L, cursor.getLong(0))
            assertEquals(-150L, cursor.getLong(1))
            assertEquals(1, cursor.getInt(2))
        }
        migratedDb.close()
    }

    @Test
    fun migrate5ToLatest_runsFullChainAndPreservesEntitiesAndBookmarks() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """INSERT INTO media_items (mediaStoreId, uri, title, displayName, durationMs, sizeBytes, mimeType,
                    width, height, frameRate, dateAddedSec, dateModifiedSec, folderPath, playbackPositionMs,
                    isCompleted, lastPlayedTimestamp, isMissing, resizeMode, playbackSpeed, decoderStrategy,
                    audioDelayMs, subtitleDelayMs, customTitle, isHidden)
                   VALUES (30, 'content://media/30', 'Chain Movie', 'Chain Movie.mp4', 180000, 8192, 'video/mp4',
                    3840, 2160, 60.0, 300, 300, 'Movies', 42000, 0, 999, 0, 'ZOOM', 1.5, 1, 20, -20, 'My Cut', 0)"""
            )
            execSQL(
                """INSERT INTO bookmarks (mediaUri, timestampMs, note, createdAtSec)
                   VALUES ('content://media/30', 12345, 'Intro scene', 1000)"""
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            APTURA_DB_VERSION,
            true,
            *ApturaDatabase.ALL_MIGRATIONS
        ).close()

        val roomDb = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ApturaDatabase::class.java,
            TEST_DB
        )
            .addMigrations(*ApturaDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val item = runBlocking { roomDb.mediaItemDao().getVideoByUri("content://media/30") }
            assertNotNull(item)
            assertEquals("My Cut", item!!.customTitle)
            assertEquals(42000L, item.playbackPositionMs)
            assertEquals(1.5f, item.playbackSpeed)
            assertEquals(1, item.decoderStrategy)
            assertTrue(item.decoderOverridden)
            assertEquals(20L, item.audioDelayMs)
            assertTrue(item.audioDelayOverridden)
            assertFalse(item.isHidden)

            val bookmarks = runBlocking { roomDb.bookmarkDao().getBookmarksForMedia("content://media/30").first() }
            assertEquals(1, bookmarks.size)
            assertEquals("Intro scene", bookmarks.single().note)
        } finally {
            roomDb.close()
        }
    }

    @Test
    fun allCommittedSchemas_upgradeCleanlyToLatestVersion() {
        for (startVersion in 5..APTURA_DB_VERSION) {
            val dbName = "upgrade-from-$startVersion.db"
            helper.createDatabase(dbName, startVersion).close()
            helper.runMigrationsAndValidate(
                dbName,
                APTURA_DB_VERSION,
                true,
                *ApturaDatabase.ALL_MIGRATIONS
            ).close()
        }
    }

    @Test
    fun migrate2ToLatest_runsEveryMigrationFrom2ThroughLatest() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "aptura-v2-to-latest.db"
        val file = context.getDatabasePath(dbName)
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
                    lastPlayedTimestamp INTEGER NOT NULL)"""
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
                    isCompleted, lastPlayedTimestamp)
                   VALUES (200, 'content://media/200', 'V2 Film', 'V2 Film.mp4', 60000, 1024, 'video/mp4',
                    1280, 720, 24.0, 1, 1, 'Movies', 15000, 0, 50)"""
            )
            old.version = 2
        }

        val migrated = Room.databaseBuilder(context, ApturaDatabase::class.java, dbName)
            .addMigrations(*ApturaDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val item = runBlocking { migrated.mediaItemDao().getVideoByUri("content://media/200") }
            assertNotNull(item)
            assertEquals("V2 Film", item!!.title)
            assertEquals(15000L, item.playbackPositionMs)
            assertFalse(item.isMissing)
            assertEquals("FIT", item.resizeMode)
            assertEquals(1.0f, item.playbackSpeed)
            assertEquals(0, item.decoderStrategy)
            assertFalse(item.decoderOverridden)
            assertEquals(0L, item.audioDelayMs)
            assertFalse(item.audioDelayOverridden)
            assertFalse(item.isHidden)
        } finally {
            migrated.close()
            file.delete()
        }
    }
}
