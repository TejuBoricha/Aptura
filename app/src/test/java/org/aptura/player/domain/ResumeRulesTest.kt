package org.aptura.player.domain

import org.aptura.player.domain.usecase.ResumeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit FUNC-08: reopening a finished video landed on 0:30 of 0:30 with a replay icon, because a position just before
 * the end was saved and resumed like any other. One rule now decides, for saving and for resuming.
 */
class ResumeRulesTest {

    private val twoHours = 7_200_000L
    private val thirtySeconds = 30_000L

    // --- What counts as finished ---

    @Test
    fun inALongVideo_theLastTenSecondsCountAsFinished() {
        assertTrue(ResumeRules.isFinished(twoHours - 10_000L, twoHours))
        assertTrue(ResumeRules.isFinished(twoHours, twoHours))
        assertFalse(ResumeRules.isFinished(twoHours - 10_001L, twoHours))
    }

    @Test
    fun inAShortClip_theWindowShrinksToAFractionSoTheClipCanStillBeResumed() {
        // 5% of 30 s is 1.5 s, not 10 s: a clip must not count as finished a third of the way from its end.
        assertTrue(ResumeRules.isFinished(28_500L, thirtySeconds))
        assertFalse(ResumeRules.isFinished(28_499L, thirtySeconds))
        assertFalse(ResumeRules.isFinished(20_000L, thirtySeconds))
    }

    @Test
    fun aPositionPastTheEnd_isFinished_andAnUnknownLengthNeverIs() {
        assertTrue("rounding at the end can report a little past it", ResumeRules.isFinished(twoHours + 500L, twoHours))
        assertFalse("no duration known yet", ResumeRules.isFinished(50_000L, 0L))
        assertFalse(ResumeRules.isFinished(0L, 0L))
    }

    // --- Where playback starts ---

    @Test
    fun aVideoSavedMidway_resumesTwoSecondsBefore() {
        assertEquals(1_198_000L, ResumeRules.resumeStartMs(1_200_000L, twoHours, isCompleted = false))
    }

    @Test
    fun aVideoSavedJustBeforeItsEnd_startsFromTheBeginning() {
        assertNull(ResumeRules.resumeStartMs(twoHours - 1_000L, twoHours, isCompleted = false))
        assertNull(ResumeRules.resumeStartMs(thirtySeconds, thirtySeconds, isCompleted = false))
    }

    @Test
    fun aWatchedVideo_startsFromTheBeginning_whereverItWasSaved() {
        assertNull(ResumeRules.resumeStartMs(1_200_000L, twoHours, isCompleted = true))
    }

    @Test
    fun aPositionTooEarlyToBeWorthResuming_startsFromTheBeginning() {
        assertNull(ResumeRules.resumeStartMs(0L, twoHours, isCompleted = false))
        assertNull(ResumeRules.resumeStartMs(3_000L, twoHours, isCompleted = false))
        assertEquals(1_001L, ResumeRules.resumeStartMs(3_001L, twoHours, isCompleted = false))
    }

    @Test
    fun withNoDurationKnown_aSavedPositionStillResumes() {
        assertEquals(48_000L, ResumeRules.resumeStartMs(50_000L, 0L, isCompleted = false))
    }
}
