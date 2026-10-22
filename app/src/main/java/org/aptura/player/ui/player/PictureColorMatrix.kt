package org.aptura.player.ui.player

import org.aptura.player.domain.model.PictureAdjustmentState

/**
 * The colour transform behind the Picture & Display sliders (audit P0-08: contrast and saturation used to do
 * nothing, and "night mode" was a flat orange rectangle over the video).
 *
 * Pure Kotlin so the maths is unit-testable. The result is an Android `ColorMatrix` array: 4 rows (R, G, B, A) of
 * 5 columns (R, G, B, A, translate) in 0..255 channel space.
 */
object PictureColorMatrix {

    private const val LUM_R = 0.213f
    private const val LUM_G = 0.715f
    private const val LUM_B = 0.072f

    private val IDENTITY = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Null when nothing is adjusted (so no filter, and no cost, is applied). */
    fun build(adjustment: PictureAdjustmentState): FloatArray? {
        if (!adjustment.isModified) return null
        var matrix = IDENTITY
        matrix = multiply(saturation(adjustment.saturation), matrix)
        matrix = multiply(contrast(adjustment.contrast), matrix)
        if (adjustment.isNightModeEnabled) {
            matrix = multiply(warmth(adjustment.nightModeWarmth), matrix)
        }
        return matrix
    }

    /** Saturation 0 = grayscale, 1 = unchanged, 2 = double. Same weights as android.graphics.ColorMatrix. */
    fun saturation(s: Float): FloatArray {
        val inv = 1f - s
        val r = inv * LUM_R
        val g = inv * LUM_G
        val b = inv * LUM_B
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Contrast scales every channel around mid-gray (128). */
    fun contrast(c: Float): FloatArray {
        val t = 128f * (1f - c)
        return floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Eye-comfort: removes blue light first, a little green second, leaves red alone. */
    fun warmth(w: Float): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f - 0.30f * w, 0f, 0f, 0f,
        0f, 0f, 1f - 0.75f * w, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Applies [matrix] to an RGB colour (0..255), clamping like Android does. */
    fun apply(matrix: FloatArray, r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        fun row(i: Int): Int {
            val v = matrix[i * 5] * r + matrix[i * 5 + 1] * g + matrix[i * 5 + 2] * b + matrix[i * 5 + 4]
            return v.toInt().coerceIn(0, 255)
        }
        return Triple(row(0), row(1), row(2))
    }

    /** `first` is applied AFTER `second` (matrix product `first * second` on affine 4x5 matrices). */
    private fun multiply(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += first[row * 5 + k] * second[k * 5 + col]
                }
                if (col == 4) sum += first[row * 5 + 4]
                out[row * 5 + col] = sum
            }
        }
        return out
    }
}
