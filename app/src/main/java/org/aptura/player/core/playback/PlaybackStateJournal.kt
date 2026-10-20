package org.aptura.player.core.playback

import java.util.concurrent.ConcurrentHashMap

/**
 * Playback state restoration journal.
 * Source of truth: spec/09_ARCHITECTURE_AND_DATA.md §4 & spec/12A §3.3
 * Preserves playback position, queue, audio track, and subtitle track across process death.
 */
class PlaybackStateJournal(
    private val saveIntervalMs: Long = 5000L
) {
    private val entries = ConcurrentHashMap<String, PlaybackJournalEntry>()
    @Volatile private var activeMediaId: String? = null
    @Volatile private var lastSaveTimestampMs: Long = 0L

    sealed interface RestoreResult {
        data class Resumed(val resumePositionMs: Long, val entry: PlaybackJournalEntry) : RestoreResult
        data class RestartWithNotice(val reason: String, val entry: PlaybackJournalEntry) : RestoreResult
        data class CompletedOrAtStart(val entry: PlaybackJournalEntry) : RestoreResult
        object NoState : RestoreResult
    }

    /**
     * Updates the playback state for an active item.
     * Periodic updates are throttled to [saveIntervalMs] unless [immediate] is true (e.g. on onPause).
     */
    fun updatePosition(
        mediaId: String,
        mediaUri: String,
        positionMs: Long,
        durationMs: Long,
        audioTrackId: String? = null,
        subtitleTrackId: String? = null,
        playbackSpeed: Float = 1.0f,
        immediate: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!immediate && (now - lastSaveTimestampMs) < saveIntervalMs && mediaId == activeMediaId) {
            return
        }

        val completed = durationMs > 0 && (durationMs - positionMs) < 10_000L
        val entry = PlaybackJournalEntry(
            mediaId = mediaId,
            mediaUri = mediaUri,
            positionMs = positionMs,
            durationMs = durationMs,
            audioTrackId = audioTrackId,
            subtitleTrackId = subtitleTrackId,
            playbackSpeed = playbackSpeed,
            completed = completed,
            lastUpdatedTimestampMs = now
        )

        entries[mediaId] = entry
        activeMediaId = mediaId
        lastSaveTimestampMs = now
    }

    /**
     * Evaluates state restoration on launch or process recreation.
     * Never restarts silently from zero if a valid prior session existed.
     */
    fun restoreSession(mediaId: String, sourceAvailable: Boolean = true): RestoreResult {
        val entry = entries[mediaId] ?: return RestoreResult.NoState

        if (!sourceAvailable) {
            return RestoreResult.RestartWithNotice(
                reason = "Media source is currently inaccessible (storage unmounted or permission revoked).",
                entry = entry
            )
        }

        if (entry.completed) {
            return RestoreResult.CompletedOrAtStart(entry)
        }

        if (entry.isResumeCandidate()) {
            val resumePos = entry.calculateResumePositionMs()
            return RestoreResult.Resumed(resumePos, entry)
        }

        return RestoreResult.CompletedOrAtStart(entry)
    }

    fun getActiveEntry(): PlaybackJournalEntry? {
        val id = activeMediaId ?: return null
        return entries[id]
    }

    fun markCompleted(mediaId: String) {
        val existing = entries[mediaId]
        if (existing != null) {
            entries[mediaId] = existing.copy(completed = true, positionMs = existing.durationMs)
        }
    }

    fun clear() {
        entries.clear()
        activeMediaId = null
        lastSaveTimestampMs = 0L
    }

    val entryCount: Int
        get() = entries.size
}
