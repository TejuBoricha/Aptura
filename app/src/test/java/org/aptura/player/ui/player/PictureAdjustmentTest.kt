package org.aptura.player.ui.player

import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.TextureView
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.domain.model.PictureAdjustmentState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit P0-08: the contrast / saturation / night-mode controls had no effect on the video. These tests check the
 * actual pixels the transform produces, and that the filter really lands on the video view's layer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PictureAdjustmentTest {

    @Test
    fun unadjustedPicture_buildsNoFilterAtAll() {
        assertNull(PictureColorMatrix.build(PictureAdjustmentState()))
    }

    @Test
    fun saturationZero_producesGrayscale() {
        val m = PictureColorMatrix.build(PictureAdjustmentState(saturation = 0f))!!
        val (r, g, b) = PictureColorMatrix.apply(m, 200, 40, 90)
        assertEquals(r, g)
        assertEquals(g, b)
        // luma of (200,40,90) with the standard weights
        assertEquals((0.213f * 200 + 0.715f * 40 + 0.072f * 90).toInt(), r)
    }

    @Test
    fun saturationAboveOne_pushesColoursApart() {
        val m = PictureColorMatrix.build(PictureAdjustmentState(saturation = 1.5f))!!
        val (r, g, b) = PictureColorMatrix.apply(m, 200, 100, 100)
        assertTrue("reds get redder", r > 200)
        assertTrue("and the others darker", g < 100 && b < 100)
    }

    @Test
    fun contrast_scalesAroundMidGray() {
        val up = PictureColorMatrix.build(PictureAdjustmentState(contrast = 1.5f))!!
        val (midR, _, _) = PictureColorMatrix.apply(up, 128, 128, 128)
        assertEquals(128, midR) // mid-gray stays put
        val (brightR, _, _) = PictureColorMatrix.apply(up, 200, 200, 200)
        val (darkR, _, _) = PictureColorMatrix.apply(up, 60, 60, 60)
        assertTrue(brightR > 200)
        assertTrue(darkR < 60)

        val down = PictureColorMatrix.build(PictureAdjustmentState(contrast = 0.5f))!!
        val (flatBright, _, _) = PictureColorMatrix.apply(down, 200, 200, 200)
        assertTrue(flatBright < 200 && flatBright > 128)
    }

    @Test
    fun nightMode_removesBlueFirst_andLeavesRedAlone() {
        val m = PictureColorMatrix.build(PictureAdjustmentState(isNightModeEnabled = true, nightModeWarmth = 0.6f))!!
        val (r, g, b) = PictureColorMatrix.apply(m, 200, 200, 200)
        assertEquals(200, r)
        assertTrue("some green removed", g < 200 && g > b)
        assertTrue("most blue removed", b < g)
    }

    /** A real [TextureView] that remembers the paint it was last given for its layer (the hook the filter goes through). */
    private class RecordingTextureView(context: Context) : TextureView(context) {
        var lastLayerPaint: Paint? = null
        override fun setLayerPaint(paint: Paint?) {
            lastLayerPaint = paint
            super.setLayerPaint(paint)
        }
    }

    @Test
    fun filterIsAppliedToTheVideoLayer_andRemovedOnReset() {
        val view = RecordingTextureView(ApplicationProvider.getApplicationContext())

        PictureFilter.apply(view, PictureAdjustmentState(contrast = 1.3f))
        assertNotNull("adjusting the picture must reach the video layer", view.lastLayerPaint)
        assertTrue(view.lastLayerPaint?.colorFilter is ColorMatrixColorFilter)

        PictureFilter.apply(view, PictureAdjustmentState())
        assertNull("reset must remove the filter", view.lastLayerPaint)
    }
}
