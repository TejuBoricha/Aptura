package org.aptura.player.ui.player

import kotlin.math.roundToInt

/**
 * Pure Kotlin representation of a bounding rectangle for PiP animations.
 */
data class PipRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

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

    /**
     * Calculates the letterboxed or pillarboxed source rectangle of the video within the window.
     * Returns null if dimensions are invalid or window is not yet laid out.
     */
    fun calculateSourceRect(videoWidth: Int, videoHeight: Int, windowWidth: Int, windowHeight: Int): PipRect? {
        if (videoWidth <= 0 || videoHeight <= 0 || windowWidth <= 0 || windowHeight <= 0) return null
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val windowRatio = windowWidth.toFloat() / windowHeight.toFloat()
        return if (videoRatio > windowRatio) {
            // Letterboxed: video matches window width, centered vertically
            val height = (windowWidth / videoRatio).roundToInt().coerceIn(1, windowHeight)
            val top = (windowHeight - height) / 2
            PipRect(left = 0, top = top, right = windowWidth, bottom = top + height)
        } else {
            // Pillarboxed: video matches window height, centered horizontally
            val width = (windowHeight * videoRatio).roundToInt().coerceIn(1, windowWidth)
            val left = (windowWidth - width) / 2
            PipRect(left = left, top = 0, right = left + width, bottom = windowHeight)
        }
    }
}
