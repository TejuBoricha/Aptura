package org.aptura.player.ui.library

import org.aptura.player.domain.model.DomainMediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * On a real device a library that contained files deleted outside the app made "Next" land on a
 * "Storage Inaccessible" error screen, because the queue was the whole grid including the Missing entries.
 */
class PlayQueueRequestTest {

    private fun video(n: Int, missing: Boolean = false) = DomainMediaItem(
        uri = "content://media/$n",
        title = "Video $n",
        durationMs = 60_000L,
        sizeBytes = 1_000L,
        mimeType = "video/mp4",
        isMissing = missing
    )

    @Test
    fun missingFiles_areLeftOutOfTheQueue_andTheStartIndexFollowsTheFilteredList() {
        val grid = listOf(video(1, missing = true), video(2), video(3, missing = true), video(4), video(5))

        val request = buildPlayQueueRequest(clicked = grid[3], visibleItems = grid)

        assertEquals(listOf("Video 2", "Video 4", "Video 5"), request.items.map { it.title })
        assertEquals("Video 4", request.items[request.startIndex].title)
    }

    @Test
    fun clickingAMissingFile_stillOpensIt_soTheUserSeesTheMissingFileMessage() {
        val grid = listOf(video(1), video(2, missing = true), video(3))

        val request = buildPlayQueueRequest(clicked = grid[1], visibleItems = grid)

        assertEquals("Video 2", request.items[request.startIndex].title)
        assertEquals(listOf("Video 1", "Video 2", "Video 3"), request.items.map { it.title })
    }

    @Test
    fun anItemThatIsNotInTheGrid_playsAlone() {
        val request = buildPlayQueueRequest(clicked = video(9), visibleItems = listOf(video(1), video(2)))

        assertEquals(listOf("Video 9"), request.items.map { it.title })
        assertEquals(0, request.startIndex)
    }

    @Test
    fun theLibrarysOwnItemsAreKeptUntouched_soResumePositionsSurvive() {
        val resumed = video(2).copy(playbackPositionMs = 125_000L, playbackSpeed = 1.5f)
        val grid = listOf(video(1), resumed)

        val request = buildPlayQueueRequest(clicked = resumed, visibleItems = grid)

        assertEquals(125_000L, request.items[request.startIndex].playbackPositionMs)
        assertEquals(1.5f, request.items[request.startIndex].playbackSpeed)
    }
}
