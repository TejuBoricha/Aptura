package org.aptura.player.ui.player

/**
 * Picture-in-picture helpers kept free of Android types so the maths is unit-testable.
 */
object PipSupport {
    /** Android rejects PiP aspect ratios outside 1:2.39 … 2.39:1. */
    const val MIN_RATIO = 0.4184f
    const val MAX_RATIO = 2.39f

    /** Returns the (numerator, denominator) PiP aspect ratio for a video, clamped to what Android accepts. */
    fun aspectRatioFor(videoWidth: Int, videoHeight: Int): Pair<Int, Int> {
        val ratio = if (videoWidth > 0 && videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else 16f / 9f
        return when {
            ratio > MAX_RATIO -> 239 to 100
            ratio < MIN_RATIO -> 100 to 239
            else -> (ratio * 1000).toInt() to 1000
        }
    }
}
