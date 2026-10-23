package org.aptura.player.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatRemainingTest {
    private val min = 60_000L

    @Test
    fun readsLikeAPerson() {
        assertEquals("12 min left", MediaFormatters.formatRemaining(positionMs = 48 * min, durationMs = 60 * min))
        assertEquals("1 h 5 min left", MediaFormatters.formatRemaining(positionMs = 60 * min, durationMs = 125 * min))
        assertEquals("2 h left", MediaFormatters.formatRemaining(positionMs = 0, durationMs = 120 * min))
    }

    @Test
    fun roundsPartialMinutesUp_andNeverShowsZeroMinutes() {
        assertEquals("1 min left", MediaFormatters.formatRemaining(positionMs = 59 * min + 1, durationMs = 60 * min))
        assertEquals("Less than 1 min left", MediaFormatters.formatRemaining(positionMs = 60 * min, durationMs = 60 * min))
    }

    @Test
    fun unknownLengthIsSaidPlainly() {
        assertEquals("Unknown length", MediaFormatters.formatRemaining(positionMs = 5 * min, durationMs = 0))
    }
}
