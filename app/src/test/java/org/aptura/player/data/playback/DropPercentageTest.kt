package org.aptura.player.data.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Audit FUNC-10: the stats overlay showed 100% dropped frames after a single dropped frame, and turns the line into a
 * warning above 5%. The rendered frames were never counted, so every drop was a share of itself; and the first few
 * frames of a video are too few to say anything about a rate.
 */
class DropPercentageTest {

    @Test
    fun oneDroppedFrame_andNothingRenderedYet_isNotOneHundredPercent() {
        assertEquals(0f, dropPercentage(dropped = 1, rendered = 0), 0.001f)
    }

    @Test
    fun oneDroppedFrameInAThousand_isATenthOfAPercent() {
        assertEquals(0.1f, dropPercentage(dropped = 1, rendered = 999), 0.001f)
    }

    @Test
    fun aRateIsOnlyShownOnceThereAreEnoughFramesToSayAnythingAbout() {
        // Fewer than 30 frames (about a second) is too little: 3 of 23 would read as 13% and warn for nothing.
        assertEquals(0f, dropPercentage(dropped = 3, rendered = 20), 0.001f)
        assertEquals(0f, dropPercentage(dropped = 0, rendered = 0), 0.001f)
        // At 30 frames the rate is real.
        assertEquals(10f, dropPercentage(dropped = 3, rendered = 27), 0.001f)
    }

    @Test
    fun aGenuinelyStutteringVideo_isReported() {
        assertEquals(20f, dropPercentage(dropped = 20, rendered = 80), 0.001f)
    }

    @Test
    fun noDroppedFrames_isZero() {
        assertEquals(0f, dropPercentage(dropped = 0, rendered = 5_000), 0.001f)
    }
}
