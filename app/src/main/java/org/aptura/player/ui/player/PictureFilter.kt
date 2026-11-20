package org.aptura.player.ui.player

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.TextureView
import org.aptura.player.domain.model.PictureAdjustmentState

/** Applies the Picture & Display adjustments to the video by colour-filtering and transforming the hardware layer of its TextureView. */
object PictureFilter {
    fun apply(view: TextureView?, adjustment: PictureAdjustmentState) {
        val target = view ?: return
        val matrix = PictureColorMatrix.build(adjustment)
        if (matrix == null) {
            target.setLayerPaint(null)
        } else {
            target.setLayerPaint(Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix)) })
        }
        applyTransform(target, adjustment.rotationDegrees, adjustment.isMirrored)
    }

    fun applyTransform(target: TextureView, rotationDegrees: Int, isMirrored: Boolean) {
        val rot = (rotationDegrees % 360 + 360) % 360
        val mirror = if (isMirrored) -1f else 1f
        target.rotation = rot.toFloat()
        target.scaleX = mirror
        target.scaleY = 1f

        fun updateScale(w: Int, h: Int) {
            if (rot % 180 != 0 && w > 0 && h > 0) {
                val s = minOf(w.toFloat() / h.toFloat(), h.toFloat() / w.toFloat())
                target.scaleX = mirror * s
                target.scaleY = s
            } else {
                target.scaleX = mirror
                target.scaleY = 1f
            }
        }

        if (target.width > 0 && target.height > 0) {
            updateScale(target.width, target.height)
        } else {
            target.addOnLayoutChangeListener(object : android.view.View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: android.view.View?,
                    left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                ) {
                    target.removeOnLayoutChangeListener(this)
                    updateScale(right - left, bottom - top)
                }
            })
        }
    }
}
