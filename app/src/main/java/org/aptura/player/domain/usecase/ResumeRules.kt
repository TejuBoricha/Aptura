package org.aptura.player.domain.usecase

/**
 * When a saved position counts as "finished", and where playback should start for a saved position (audit FUNC-08).
 * Pure Kotlin: the same rule is used when a position is saved and when it is resumed.
 */
object ResumeRules {
    /** A saved position at or before this is not worth resuming. */
    const val MIN_RESUME_MS = 3_000L

    /** Playback starts this much before the saved position, so the person picks the scene up again. */
    const val PRE_ROLL_MS = 2_000L

    /** The most that can count as "near the end": the last ten seconds of any video. */
    private const val NEAR_END_MAX_MS = 10_000L

    /** A short clip's window is a twentieth of it, so a clip cannot count as finished a third of the way from its end. */
    private const val NEAR_END_FRACTION_DIVISOR = 20L

    /**
     * Whether [positionMs] is so near the end of a video of [durationMs] that the video counts as finished: within the
     * last ten seconds, or the last twentieth of a short clip. A length that is not known yet (zero) never counts.
     */
    fun isFinished(positionMs: Long, durationMs: Long): Boolean {
        if (durationMs <= 0L) return false
        val window = minOf(NEAR_END_MAX_MS, durationMs / NEAR_END_FRACTION_DIVISOR)
        return positionMs >= durationMs - window
    }

    /**
     * Where to start a video saved at [positionMs] ([durationMs] long, [isCompleted] when it was marked watched), or
     * null to start from the beginning.
     */
    fun resumeStartMs(positionMs: Long, durationMs: Long, isCompleted: Boolean): Long? =
        if (isCompleted || positionMs <= MIN_RESUME_MS || isFinished(positionMs, durationMs)) {
            null
        } else {
            (positionMs - PRE_ROLL_MS).coerceAtLeast(0L)
        }
}
