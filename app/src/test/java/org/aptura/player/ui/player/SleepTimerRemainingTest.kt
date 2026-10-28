package org.aptura.player.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The countdown is derived from a deadline rather than decremented, so a late frame can never make the timer run
 * long. These cases are pure arithmetic and need no clock or coroutine scheduler.
 */
class SleepTimerRemainingTest {

    @Test
    fun noTimer_hasNoRemainingTime() {
        assertNull(sleepTimerRemainingMs(endsAtElapsedRealtime = null, nowElapsedRealtime = 1_000L))
    }

    @Test
    fun remaining_isTheDistanceToTheDeadline() {
        assertEquals(
            60_000L,
            sleepTimerRemainingMs(endsAtElapsedRealtime = 61_000L, nowElapsedRealtime = 1_000L)
        )
    }

    @Test
    fun aDeadlineInThePast_clampsToZero_insteadOfShowingNegativeTime() {
        assertEquals(
            0L,
            sleepTimerRemainingMs(endsAtElapsedRealtime = 1_000L, nowElapsedRealtime = 2_500L)
        )
    }
}
