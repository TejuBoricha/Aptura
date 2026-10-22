package org.aptura.player.ui.player

import org.junit.Assert.assertEquals
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
}
