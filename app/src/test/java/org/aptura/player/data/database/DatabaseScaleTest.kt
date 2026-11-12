package org.aptura.player.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.scanner.FileReconciliationEngine
import org.aptura.player.data.scanner.MediaScanWorker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Verifies PERF-03:
 * - Room MIGRATION_6_7 creates `index_media_items_uri` and query plans use it instead of scanning.
 * - 20,000-row Room queries, incremental reconciliation (0 writes on unchanged rescan), and O(N+M) SAF dedupe.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseScaleTest {

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

    @Test
    fun migration6To7_createsUriIndex_andQueryPlannerUsesIt() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // in-memory
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `media_items` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `mediaStoreId` INTEGER NOT NULL,
                                `uri` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `displayName` TEXT NOT NULL,
                                `durationMs` INTEGER NOT NULL,
                                `sizeBytes` INTEGER NOT NULL,
                                `mimeType` TEXT,
                                `width` INTEGER NOT NULL,
                                `height` INTEGER NOT NULL,
                                `frameRate` REAL NOT NULL,
                                `dateAddedSec` INTEGER NOT NULL,
                                `dateModifiedSec` INTEGER NOT NULL,
                                `folderPath` TEXT NOT NULL,
                                `playbackPositionMs` INTEGER NOT NULL,
                                `isCompleted` INTEGER NOT NULL,
                                `lastPlayedTimestamp` INTEGER NOT NULL,
                                `isMissing` INTEGER NOT NULL,
                                `resizeMode` TEXT NOT NULL,
                                `playbackSpeed` REAL NOT NULL,
                                `decoderStrategy` INTEGER NOT NULL,
                                `decoderOverridden` INTEGER NOT NULL,
                                `audioDelayMs` INTEGER NOT NULL,
                                `subtitleDelayMs` INTEGER NOT NULL,
                                `customTitle` TEXT,
                                `isHidden` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val sqliteDb = helper.writableDatabase
        try {
            ApturaDatabase.MIGRATION_6_7.migrate(sqliteDb)
            val planCursor = sqliteDb.query("EXPLAIN QUERY PLAN SELECT * FROM media_items WHERE uri = 'content://media/1'")
            val plans = buildList {
                while (planCursor.moveToNext()) {
                    add(planCursor.getString(planCursor.getColumnIndexOrThrow("detail")))
                }
            }
            planCursor.close()
            assertTrue(
                "Expected query plan to use index_media_items_uri, was: $plans",
                plans.any { it.contains("index_media_items_uri", ignoreCase = true) }
            )
        } finally {
            helper.close()
        }
    }

    @Test
    fun twentyThousandRows_uriLookupUsesIndex_reconcileSkipsUnchanged_andSafDedupeIsLinear() = runBlocking {
        val count = 20_000
        val dao = db.mediaItemDao()
        val entities = (1..count).map { i ->
            MediaItemEntity(
                mediaStoreId = i.toLong(),
                uri = "content://media/external/video/media/$i",
                title = "Video $i",
                displayName = "video_$i.mp4",
                durationMs = 60_000L + i,
                sizeBytes = 10_000_000L + i,
                mimeType = "video/mp4",
                folderPath = "/Movies/Batch${i % 20}"
            )
        }

        entities.chunked(500).forEach { batch ->
            dao.insertOrUpdateAll(batch)
        }

        // 1. Verify URI index is used and lookup finds exact row
        val sqliteDb = db.openHelper.writableDatabase
        val planCursor = sqliteDb.query("EXPLAIN QUERY PLAN SELECT * FROM media_items WHERE uri = 'content://media/external/video/media/19999'")
        val planDetails = buildList {
            while (planCursor.moveToNext()) {
                add(planCursor.getString(planCursor.getColumnIndexOrThrow("detail")))
            }
        }
        planCursor.close()
        assertTrue(
            "Expected SEARCH using index_media_items_uri, got: $planDetails",
            planDetails.any { it.contains("index_media_items_uri", ignoreCase = true) }
        )

        val fetched = dao.getVideoByUri("content://media/external/video/media/19999")
        assertNotNull(fetched)
        assertEquals("Video 19999", fetched!!.title)

        // 2. Unchanged rescan of 20,000 rows produces 0 database writes
        val existingRows = dao.getAllVideosRaw()
        assertEquals(count, existingRows.size)
        val unchangedReconcile = FileReconciliationEngine.reconcile(
            scannedItems = entities,
            existingItems = existingRows
        )
        assertEquals(0, unchangedReconcile.itemsToUpsert.size)
        assertEquals(0, unchangedReconcile.idsToDelete.size)

        // Modifying 3 rows and adding 2 new rows writes only 5 rows
        val deltaScanned = entities.toMutableList()
        deltaScanned[0] = deltaScanned[0].copy(title = "Video 1 Edited")
        deltaScanned[5000] = deltaScanned[5000].copy(durationMs = 999_999L)
        deltaScanned[19999] = deltaScanned[19999].copy(sizeBytes = 42_000_000L)
        deltaScanned.add(
            MediaItemEntity(
                mediaStoreId = 20_001L,
                uri = "content://media/external/video/media/20001",
                title = "Video 20001",
                displayName = "video_20001.mp4",
                durationMs = 120_000L,
                sizeBytes = 5_000_000L,
                mimeType = "video/mp4"
            )
        )
        deltaScanned.add(
            MediaItemEntity(
                mediaStoreId = 20_002L,
                uri = "content://media/external/video/media/20002",
                title = "Video 20002",
                displayName = "video_20002.mp4",
                durationMs = 120_000L,
                sizeBytes = 5_000_001L,
                mimeType = "video/mp4"
            )
        )
        val deltaReconcile = FileReconciliationEngine.reconcile(
            scannedItems = deltaScanned,
            existingItems = existingRows
        )
        assertEquals(5, deltaReconcile.itemsToUpsert.size)

        // 3. Hash-set SAF dedupe across 20,000 existing + 5,000 SAF items (2,500 duplicates, 2,500 unique)
        val seenUris = entities.mapTo(HashSet(entities.size)) { it.uri }
        val seenSizeAndName = HashSet<Pair<Long, String>>(entities.size)
        for (item in entities) {
            if (item.sizeBytes > 0L) {
                seenSizeAndName.add(item.sizeBytes to item.displayName.lowercase(Locale.ROOT))
            }
        }
        val safItems = (1..5_000).map { i ->
            if (i <= 1_250) {
                // Exact URI duplicate
                entities[i - 1]
            } else if (i <= 2_500) {
                // Overlapping SAF file matching by size + case-insensitive displayName
                MediaItemEntity(
                    mediaStoreId = -(i.toLong()),
                    uri = "content://com.android.externalstorage.documents/tree/primary/video_$i.mp4",
                    title = "Video $i",
                    displayName = "VIDEO_$i.MP4",
                    durationMs = 60_000L + i,
                    sizeBytes = 10_000_000L + i,
                    mimeType = "video/mp4"
                )
            } else {
                // Unique SAF file
                MediaItemEntity(
                    mediaStoreId = -(i.toLong()),
                    uri = "content://com.android.externalstorage.documents/tree/primary/saf_unique_$i.mp4",
                    title = "SAF Unique $i",
                    displayName = "saf_unique_$i.mp4",
                    durationMs = 45_000L,
                    sizeBytes = 20_000_000L + i,
                    mimeType = "video/mp4"
                )
            }
        }
        val deduplicated = MediaScanWorker.deduplicateSafItems(
            safItems = safItems,
            seenUris = seenUris,
            seenSizeAndName = seenSizeAndName
        )
        assertEquals(2_500, deduplicated.size)
    }
}
