package org.aptura.player.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipSupportTest {

    @Test
    fun unknownVideoSize_fallsBackTo16by9() {
        val (n, d) = PipSupport.aspectRatioFor(0, 0)
        assertEquals(16f / 9f, n.toFloat() / d, 0.01f)
    }

    @Test
    fun normalVideo_keepsItsRatio() {
        val (n, d) = PipSupport.aspectRatioFor(1920, 1080)
        assertEquals(1920f / 1080f, n.toFloat() / d, 0.01f)
        val (pn, pd) = PipSupport.aspectRatioFor(1080, 1920)
        assertEquals(1080f / 1920f, pn.toFloat() / pd, 0.01f)
    }

    @Test
    fun extremeRatios_areClampedToWhatAndroidAccepts() {
        val (wn, wd) = PipSupport.aspectRatioFor(5000, 1000) // 5:1 ultra-wide
        assertTrue(wn.toFloat() / wd <= PipSupport.MAX_RATIO + 0.001f)
        val (tn, td) = PipSupport.aspectRatioFor(500, 5000) // 1:10 ultra-tall
        assertTrue(tn.toFloat() / td >= PipSupport.MIN_RATIO - 0.001f)
    }

    @Test
    fun calculateSourceRect_invalidDimensions_returnsNull() {
        assertNull(PipSupport.calculateSourceRect(0, 1080, 1920, 1080))
        assertNull(PipSupport.calculateSourceRect(1920, 0, 1920, 1080))
        assertNull(PipSupport.calculateSourceRect(1920, 1080, 0, 1080))
        assertNull(PipSupport.calculateSourceRect(1920, 1080, 1920, 0))
    }

    @Test
    fun calculateSourceRect_letterboxedInPortrait_centersVertically() {
        val rect = PipSupport.calculateSourceRect(
            videoWidth = 1920,
            videoHeight = 1080,
            windowWidth = 1080,
            windowHeight = 2400
        )
        assertNotNull(rect)
        assertEquals(0, rect!!.left)
        assertEquals(1080, rect.right)
        assertEquals(1080, rect.right - rect.left)
        // 1080 * 9 / 16 = 607.5 -> 608
        val expectedHeight = 608
        assertEquals(expectedHeight, rect.bottom - rect.top)
        val expectedTop = (2400 - expectedHeight) / 2
        assertEquals(expectedTop, rect.top)
    }

    @Test
    fun calculateSourceRect_pillarboxedInLandscape_centersHorizontally() {
        val rect = PipSupport.calculateSourceRect(
            videoWidth = 1920,
            videoHeight = 1080,
            windowWidth = 2400,
            windowHeight = 1080
        )
        assertNotNull(rect)
        assertEquals(0, rect!!.top)
        assertEquals(1080, rect.bottom)
        assertEquals(1920, rect.right - rect.left)
        val expectedLeft = (2400 - 1920) / 2
        assertEquals(expectedLeft, rect.left)
        assertEquals(2400 - expectedLeft, rect.right)
    }
}
