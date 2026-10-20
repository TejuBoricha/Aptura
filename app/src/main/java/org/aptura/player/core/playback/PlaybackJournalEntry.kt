package org.aptura.player.core.playback

import kotlinx.serialization.Serializable

/**
 * Playback session state journal record.
 * Source of truth: spec/09_ARCHITECTURE_AND_DATA.md §4 & spec/12A §3.3 (TST-REST)
 */
@Serializable
data class PlaybackJournalEntry(
    val mediaId: String,
    val mediaUri: String,
    val positionMs: Long,
    val durationMs: Long,
    val audioTrackId: String? = null,
    val subtitleTrackId: String? = null,
    val playbackSpeed: Float = 1.0f,
    val completed: Boolean = false,
    val lastUpdatedTimestampMs: Long = System.currentTimeMillis()
) {
    /**
     * Calculates the resume position applying the 2-second pre-roll comfort window
     * and completion boundary rules per spec/05 & spec/09 §4.
     */
    fun calculateResumePositionMs(): Long {
        if (completed) return 0L

        // If less than 10 seconds remain, treat as completed (restart from beginning)
        if (durationMs > 10_000L && (durationMs - positionMs) < 10_000L) {
            return 0L
        }

        // If within first 3 seconds, resume from 0 to avoid jitter
        if (positionMs <= 3_000L) {
            return 0L
        }

        // Apply 2-second pre-roll comfort window so user re-orients
        return maxOf(0L, positionMs - 2_000L)
    }

    /**
     * Returns true if this playback entry represents an actionable resume candidate.
     */
    fun isResumeCandidate(): Boolean {
        if (completed) return false
        if (durationMs > 10_000L && (durationMs - positionMs) < 10_000L) return false
        return positionMs > 3_000L
    }
}
