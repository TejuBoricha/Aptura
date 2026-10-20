package org.aptura.player.domain

import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.usecase.PlaybackNavigationRules
import org.aptura.player.domain.usecase.PreviousActionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for domain playback navigation rules (W8 / W9 platform contracts).
 */
class PlaybackNavigationRulesTest {

    private val sampleItem1 = DomainMediaItem(
        uri = "content://media/1",
        title = "Video 1",
        durationMs = 60000L,
        sizeBytes = 1048576L,
        mimeType = "video/mp4"
    )
    private val sampleItem2 = DomainMediaItem(
        uri = "content://media/2",
        title = "Video 2",
        durationMs = 120000L,
        sizeBytes = 2097152L,
        mimeType = "video/mkv"
    )

    @Test
    fun evaluatePreviousAction_restartsCurrent_whenPastThreshold() {
        val queue = DomainPlaybackQueue(
            items = listOf(sampleItem1, sampleItem2),
            currentIndex = 1
        )
        // Position > 3000ms restarts current item
        val outcome = PlaybackNavigationRules.evaluatePreviousAction(
            currentPositionMs = 3001L,
            queue = queue
        )
        assertEquals(PreviousActionOutcome.RestartCurrentItem, outcome)
    }

    @Test
    fun evaluatePreviousAction_navigatesToPrevious_whenWithinThresholdAndHasPrevious() {
        val queue = DomainPlaybackQueue(
            items = listOf(sampleItem1, sampleItem2),
            currentIndex = 1
        )
        // Position <= 3000ms navigates to previous queue item
        val outcome = PlaybackNavigationRules.evaluatePreviousAction(
            currentPositionMs = 2500L,
            queue = queue
        )
        assertEquals(PreviousActionOutcome.NavigateToPreviousItem, outcome)
    }

    @Test
    fun evaluatePreviousAction_restartsCurrent_whenAtFirstItemWithinThreshold() {
        val queue = DomainPlaybackQueue(
            items = listOf(sampleItem1, sampleItem2),
            currentIndex = 0 // First item: no previous exists
        )
        val outcome = PlaybackNavigationRules.evaluatePreviousAction(
            currentPositionMs = 1500L,
            queue = queue
        )
        assertEquals(PreviousActionOutcome.RestartCurrentItem, outcome)
    }

    @Test
    fun calculateTapAccumulationOffset_symmetricLadder() {
        assertEquals(0L, PlaybackNavigationRules.calculateTapAccumulationOffset(1))
        assertEquals(10000L, PlaybackNavigationRules.calculateTapAccumulationOffset(2))
        assertEquals(20000L, PlaybackNavigationRules.calculateTapAccumulationOffset(3))
        assertEquals(30000L, PlaybackNavigationRules.calculateTapAccumulationOffset(4))
    }

    @Test
    fun clampSeekPosition_staysWithinBoundaries() {
        assertEquals(0L, PlaybackNavigationRules.clampSeekPosition(-500L, 10000L))
        assertEquals(5000L, PlaybackNavigationRules.clampSeekPosition(5000L, 10000L))
        assertEquals(10000L, PlaybackNavigationRules.clampSeekPosition(15000L, 10000L))
    }

    @Test
    fun queueNavigation_respectsRepeatModes() {
        val queue = DomainPlaybackQueue(
            items = listOf(sampleItem1),
            currentIndex = 0,
            playMode = PlayMode.REPEAT_ONE
        )
        assertTrue("REPEAT_ONE has next item available", queue.hasNext)
        assertTrue("REPEAT_ONE has previous item available", queue.hasPrevious)
    }
}
