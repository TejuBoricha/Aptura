package org.aptura.player.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Settings used to offer five speeds while the player offered eight, so 0.25x, 0.5x and 1.75x could be used for one
 * video but never chosen as the default (audit 2026-10-24). Both surfaces now read this list.
 */
class PlaybackSpeedOptionsTest {

    @Test
    fun all_containsThePlayersEightSpeedsInAscendingOrder() {
        assertEquals(listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f), PlaybackSpeedOptions.all)
        assertEquals("speeds must be unique", PlaybackSpeedOptions.all.size, PlaybackSpeedOptions.all.toSet().size)
        assertTrue(PlaybackSpeedOptions.all.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun label_matchesTheCopyEverySurfaceUses() {
        assertEquals("1.0x", PlaybackSpeedOptions.label(1.0f))
        assertEquals("0.25x", PlaybackSpeedOptions.label(0.25f))
        assertEquals("1.25x", PlaybackSpeedOptions.label(1.25f))
        assertEquals("2.0x", PlaybackSpeedOptions.label(2.0f))
    }
}
