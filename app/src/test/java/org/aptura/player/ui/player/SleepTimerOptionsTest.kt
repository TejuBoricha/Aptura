package org.aptura.player.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FR-CTRL-009 requires the documented ladder (10/15/20/30/45/60) plus a custom duration. These pin both so a UI
 * reshuffle cannot silently drop an option, and keep the custom field's bounds out of the composable.
 */
class SleepTimerOptionsTest {

    @Test
    fun theLadderMatchesTheRequirement() {
        assertEquals(listOf(10, 15, 20, 30, 45, 60), SLEEP_TIMER_PRESET_MINUTES)
    }

    @Test
    fun customDuration_acceptsOneToTwelveHours() {
        assertEquals(1, customSleepMinutes("1"))
        assertEquals(45, customSleepMinutes("45"))
        assertEquals(720, customSleepMinutes("720"))
    }

    @Test
    fun customDuration_rejectsZero_negative_blank_andMoreThanTwelveHours() {
        assertNull(customSleepMinutes(""))
        assertNull(customSleepMinutes("0"))
        assertNull(customSleepMinutes("-5"))
        assertNull(customSleepMinutes("721"))
        assertNull(customSleepMinutes("45 min"))
    }
}
