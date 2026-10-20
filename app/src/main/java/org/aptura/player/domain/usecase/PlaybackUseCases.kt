package org.aptura.player.domain.usecase

import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.PlayMode

/**
 * Pure Kotlin business logic use cases for Aptura playback orchestration.
 * Invariant: Independent of Android framework; 100% unit-testable on host JVM.
 */
object PlaybackNavigationRules {

    /**
     * Platform standard seek-to-previous threshold (Media3 DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS = 3000ms).
     * If current position > 3000ms, "previous" restarts the current item from 0.
     * If current position <= 3000ms, "previous" transitions to the preceding queue item.
     * Source: spec/04 FR-CTRL-002 & research/platform_constants.md §4.
     */
    const val MAX_SEEK_TO_PREVIOUS_POSITION_MS = 3000L

    /**
     * Calculates the seek target when the user triggers the "previous" action.
     * Returns a [PreviousActionOutcome] detailing whether to restart the current media or navigate.
     */
    fun evaluatePreviousAction(
        currentPositionMs: Long,
        queue: DomainPlaybackQueue
    ): PreviousActionOutcome {
        return if (currentPositionMs > MAX_SEEK_TO_PREVIOUS_POSITION_MS) {
            PreviousActionOutcome.RestartCurrentItem
        } else if (queue.hasPrevious) {
            PreviousActionOutcome.NavigateToPreviousItem
        } else {
            PreviousActionOutcome.RestartCurrentItem
        }
    }

    /**
     * Calculates multi-tap seek increment accumulation (W8 standard).
     * Symmetric ±10s increments per tap.
     * 2 taps = 10s, 3 taps = 20s, 4 taps = 30s, etc.
     */
    fun calculateTapAccumulationOffset(consecutiveTaps: Int, baseIncrementMs: Long = 10000L): Long {
        if (consecutiveTaps <= 1) return 0L
        return (consecutiveTaps - 1) * baseIncrementMs
    }

    /**
     * Clamps position to duration boundaries [0, durationMs].
     */
    fun clampSeekPosition(targetMs: Long, durationMs: Long): Long {
        return targetMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    }
}

sealed class PreviousActionOutcome {
    object RestartCurrentItem : PreviousActionOutcome()
    object NavigateToPreviousItem : PreviousActionOutcome()
}
