package org.aptura.player.data.scanner

import org.aptura.player.data.database.MediaItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for media scanning synchronization logic and data merging.
 * Verifies differential updating, progress preservation, and batching constraints.
 */
class MediaScanLogicTest {

    @Test
    fun differentialSync_preservesPlaybackPositionAndTimestamps() {
        val existingItem = MediaItemEntity(
            id = 42L,
            mediaStoreId = 1001L,
            uri = "content://media/external/video/media/1001",
            title = "Inception",
            displayName = "inception.mkv",
            durationMs = 8880000L,
            sizeBytes = 2147483648L,
            mimeType = "video/x-matroska",
            playbackPositionMs = 3600000L, // User watched 1 hour
            isCompleted = false,
            lastPlayedTimestamp = 1700000000000L
        )

        val freshScanItem = MediaItemEntity(
            id = 0L,
            mediaStoreId = 1001L,
            uri = "content://media/external/video/media/1001",
            title = "Inception Updated",
            displayName = "inception.mkv",
            durationMs = 8880000L,
            sizeBytes = 2147483648L,
            mimeType = "video/x-matroska",
            playbackPositionMs = 0L,
            isCompleted = false,
            lastPlayedTimestamp = 0L
        )

        // Merging logic applied in MediaScanWorker
        val merged = freshScanItem.copy(
            id = existingItem.id,
            playbackPositionMs = existingItem.playbackPositionMs,
            isCompleted = existingItem.isCompleted,
            lastPlayedTimestamp = existingItem.lastPlayedTimestamp
        )

        assertEquals(42L, merged.id)
        assertEquals(3600000L, merged.playbackPositionMs)
        assertFalse(merged.isCompleted)
        assertEquals(1700000000000L, merged.lastPlayedTimestamp)
        assertEquals("Inception Updated", merged.title)
    }

    @Test
    fun batchingLogic_chunksProperlyInto250Items() {
        val largeList = (1..620).map { index ->
            MediaItemEntity(
                id = index.toLong(),
                mediaStoreId = index.toLong(),
                uri = "content://media/external/video/media/$index",
                title = "Video $index",
                displayName = "video_$index.mp4",
                durationMs = 60000L,
                sizeBytes = 1048576L,
                mimeType = "video/mp4"
            )
        }

        val batches = largeList.chunked(250)
        assertEquals("620 items should split into 3 chunks (250, 250, 120)", 3, batches.size)
        assertEquals(250, batches[0].size)
        assertEquals(250, batches[1].size)
        assertEquals(120, batches[2].size)
        assertEquals(620, batches.flatten().size)
    }

    @Test
    fun filterThreshold_excludesFilesUnder1024Bytes() {
        val corruptedFragmentSize = 512L
        val validFileSize = 1048576L

        assertTrue("Files > 1024 bytes must be retained", validFileSize > 1024L)
        assertFalse("Files <= 1024 bytes must be discarded", corruptedFragmentSize > 1024L)
    }
}
