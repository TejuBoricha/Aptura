package org.aptura.player.ui.player

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.TextureView
import org.aptura.player.domain.model.PictureAdjustmentState

/** Applies the Picture & Display adjustments to the video by colour-filtering the hardware layer of its TextureView. */
object PictureFilter {
    fun apply(view: TextureView?, adjustment: PictureAdjustmentState) {
        val target = view ?: return
        val matrix = PictureColorMatrix.build(adjustment)
        if (matrix == null) {
            target.setLayerPaint(null)
        } else {
            target.setLayerPaint(Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix)) })
        }
    }
}
