package org.aptura.player.data.scanner

import org.aptura.player.data.database.MediaItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for FileReconciliationEngine adhering to spec/04 FR-LIB-018 and FR-LIB-021.
 * Tests moved/renamed file detection, storage unmount handling (missing status),
 * reconnected storage restoration, and state preservation.
 */
class FileReconciliationEngineTest {

    @Test
    fun directMatch_preservesProgressAndUnsetsMissing() {
        val existing = listOf(
            MediaItemEntity(
                id = 10L,
                mediaStoreId = 500L,
                uri = "content://media/external/video/media/500",
                title = "Big Buck Bunny",
                displayName = "bbb.mp4",
                durationMs = 60000L,
                sizeBytes = 10000000L,
                mimeType = "video/mp4",
                playbackPositionMs = 30000L,
                isCompleted = false,
                lastPlayedTimestamp = 1700000000L,
                isMissing = true // was previously missing
            )
        )

        val scanned = listOf(
            MediaItemEntity(
                id = 0L,
                mediaStoreId = 500L,
                uri = "content://media/external/video/media/500",
                title = "Big Buck Bunny (Refreshed)",
                displayName = "bbb.mp4",
                durationMs = 60000L,
                sizeBytes = 10000000L,
                mimeType = "video/mp4",
                playbackPositionMs = 0L,
                isCompleted = false,
                lastPlayedTimestamp = 0L,
                isMissing = false
            )
        )

        val result = FileReconciliationEngine.reconcile(scanned, existing)

        assertEquals(1, result.itemsToUpsert.size)
        assertEquals(0, result.idsToDelete.size)
        assertEquals(1, result.restoredCount)
        assertEquals(0, result.missingCount)
        assertEquals(0, result.movedCount)

        val item = result.itemsToUpsert.first()
        assertEquals(10L, item.id)
        assertEquals(30000L, item.playbackPositionMs)
        assertFalse("Restored item must not be missing", item.isMissing)
        assertEquals("Big Buck Bunny (Refreshed)", item.title)
    }

    @Test
    fun movedFile_detectedByHeuristic_migratesProgress() {
        // Video was moved from Internal Movies to SD Card Videos with slightly altered name
        val existing = listOf(
            MediaItemEntity(
                id = 7L,
                mediaStoreId = 101L,
                uri = "content://media/external/video/media/101",
                title = "Cosmos Laundromat",
                displayName = "cosmos.mp4",
                durationMs = 720000L,
                sizeBytes = 54321000L,
                mimeType = "video/mp4",
                folderPath = "Internal/Movies",
                playbackPositionMs = 360000L,
                isCompleted = false,
                lastPlayedTimestamp = 1710000000L,
                isMissing = false
            )
        )

        val scanned = listOf(
            MediaItemEntity(
                id = 0L,
                mediaStoreId = 202L, // different MediaStore ID due to move
                uri = "content://media/external/video/media/202",
                title = "Cosmos Laundromat - 1080p",
                displayName = "cosmos_1080p.mp4",
                durationMs = 720100L, // duration matches within ±1000ms
                sizeBytes = 54321000L, // exact byte match
                mimeType = "video/mp4",
                folderPath = "SDCard/Movies",
                playbackPositionMs = 0L,
                isCompleted = false,
                lastPlayedTimestamp = 0L,
                isMissing = false
            )
        )

        val result = FileReconciliationEngine.reconcile(scanned, existing)

        assertEquals(1, result.itemsToUpsert.size)
        assertEquals(1, result.movedCount)
        assertEquals(0, result.missingCount)
        assertEquals(0, result.newCount)

        val item = result.itemsToUpsert.first()
        assertEquals("Existing ID should be maintained", 7L, item.id)
        assertEquals("Playback position must be transferred to moved file", 360000L, item.playbackPositionMs)
        assertEquals("content://media/external/video/media/202", item.uri)
        assertEquals("SDCard/Movies", item.folderPath)
        assertFalse(item.isMissing)
    }

    @Test
    fun unmountedStorage_marksItemMissingWithoutDeleting() {
        val existing = listOf(
            MediaItemEntity(
                id = 25L,
                mediaStoreId = 999L,
                uri = "content://com.android.externalstorage.documents/tree/sdcard%3AMovies/document/sdcard%3AMovies%2Fvacation.mp4",
                title = "Vacation",
                displayName = "vacation.mp4",
                durationMs = 120000L,
                sizeBytes = 25000000L,
                mimeType = "video/mp4",
                playbackPositionMs = 50000L,
                isCompleted = false,
                isMissing = false
            )
        )

        // SD Card was removed, so scanner discovered 0 items
        val scanned = emptyList<MediaItemEntity>()

        val result = FileReconciliationEngine.reconcile(scanned, existing)

        assertEquals(1, result.itemsToUpsert.size)
        assertEquals(0, result.idsToDelete.size)
        assertEquals(1, result.missingCount)

        val item = result.itemsToUpsert.first()
        assertEquals(25L, item.id)
        assertTrue("Item on disconnected storage must be flagged isMissing", item.isMissing)
        assertEquals("Progress must be preserved while missing", 50000L, item.playbackPositionMs)
    }

    @Test
    fun brandNewFile_insertedWithMissingFalse() {
        val existing = emptyList<MediaItemEntity>()

        val scanned = listOf(
            MediaItemEntity(
                id = 0L,
                mediaStoreId = 888L,
                uri = "content://media/external/video/media/888",
                title = "New Video",
                displayName = "new_video.mp4",
                durationMs = 45000L,
                sizeBytes = 8000000L,
                mimeType = "video/mp4",
                isMissing = false
            )
        )

        val result = FileReconciliationEngine.reconcile(scanned, existing)

        assertEquals(1, result.itemsToUpsert.size)
        assertEquals(1, result.newCount)
        assertFalse(result.itemsToUpsert.first().isMissing)
    }
}
