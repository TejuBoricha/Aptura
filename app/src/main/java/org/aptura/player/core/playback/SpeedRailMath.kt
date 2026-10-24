package org.aptura.player.core.playback

/**
 * Position math for the Settings speed ramp. Kept out of the composable so the mapping between a finger position and
 * one of the eight speeds can be tested without a device.
 */
object SpeedRailMath {

    /** Where a detent sits on a 0..1 rail. A single speed (or none) stays at the left edge. */
    fun fractionForIndex(index: Int, count: Int): Float {
        if (count <= 1) return 0f
        return index.coerceIn(0, count - 1).toFloat() / (count - 1).toFloat()
    }

    /** The closest detent to a 0..1 rail position; the ends clamp instead of wrapping. */
    fun indexForFraction(fraction: Float, count: Int): Int {
        if (count <= 1) return 0
        return Math.round(fraction.coerceIn(0f, 1f) * (count - 1)).coerceIn(0, count - 1)
    }

    /**
     * The detent closest to a stored speed. A value written by an older build (or by a future one) still lands on the
     * nearest speed instead of silently pretending to be the slowest.
     */
    fun indexForValue(speeds: List<Float>, value: Float): Int {
        if (speeds.isEmpty()) return 0
        return speeds.indices.minByOrNull { kotlin.math.abs(speeds[it] - value) } ?: 0
    }
}
