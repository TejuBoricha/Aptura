package org.aptura.player.core.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedRailMathTest {

    @Test
    fun fractions_placeTheEightSpeedsEvenly() {
        // Eight detents mean seven gaps: only the ends are at 0 and 1, and 1.0x (index 3) sits at 3/7, not the middle.
        assertEquals(0f, SpeedRailMath.fractionForIndex(0, 8), 0.0001f)
        assertEquals(1f / 7f, SpeedRailMath.fractionForIndex(1, 8), 0.0001f)
        assertEquals(3f / 7f, SpeedRailMath.fractionForIndex(3, 8), 0.0001f)
        assertEquals(1f, SpeedRailMath.fractionForIndex(7, 8), 0.0001f)
    }

    @Test
    fun everyIndex_roundTripsThroughItsFraction() {
        for (index in 0 until 8) {
            assertEquals(index, SpeedRailMath.indexForFraction(SpeedRailMath.fractionForIndex(index, 8), 8))
        }
    }

    @Test
    fun positions_snapToTheClosestSpeed() {
        assertEquals(0, SpeedRailMath.indexForFraction(-0.4f, 8))
        assertEquals(0, SpeedRailMath.indexForFraction(0.06f, 8))
        assertEquals(1, SpeedRailMath.indexForFraction(0.15f, 8))
        assertEquals(4, SpeedRailMath.indexForFraction(0.57f, 8))
        assertEquals(7, SpeedRailMath.indexForFraction(1.4f, 8))
    }

    @Test
    fun aSingleSpeed_staysAtTheLeftEdge() {
        assertEquals(0f, SpeedRailMath.fractionForIndex(0, 1), 0f)
        assertEquals(0, SpeedRailMath.indexForFraction(1f, 1))
    }

    @Test
    fun aStoredSpeed_landsOnTheNearestDetent() {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        assertEquals(3, SpeedRailMath.indexForValue(speeds, 1.0f))
        assertEquals(3, SpeedRailMath.indexForValue(speeds, 1.1f))
        assertEquals(4, SpeedRailMath.indexForValue(speeds, 1.2f))
        assertEquals(0, SpeedRailMath.indexForValue(speeds, 0.1f))
        assertEquals(7, SpeedRailMath.indexForValue(speeds, 2.5f))
        assertEquals(0, SpeedRailMath.indexForValue(emptyList(), 1.5f))
    }
}
