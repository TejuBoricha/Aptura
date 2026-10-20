package org.aptura.player.domain.model

/**
 * Pure Kotlin domain models for Aptura playback orchestration.
 * Invariant: ZERO Android framework or Media3 dependencies in domain/ (DEC-002, ADR-001).
 * This layer enforces clean architecture, enabling decoupling of presentation logic,
 * playback state machines, and potential future multiplatform/iOS adaptations.
 */

enum class PlayMode {
    NORMAL,
    REPEAT_ONE,
    REPEAT_ALL,
    SHUFFLE
}

sealed class DomainPlaybackState {
    object Idle : DomainPlaybackState()
    
    data class Buffering(
        val positionMs: Long
    ) : DomainPlaybackState()
    
    data class Ready(
        val positionMs: Long,
        val durationMs: Long,
        val isPlaying: Boolean,
        val playbackSpeed: Float = 1.0f
    ) : DomainPlaybackState()
    
    data class Ended(
        val durationMs: Long
    ) : DomainPlaybackState()
    
    data class Error(
        val errorCode: String,
        val message: String,
        val isRecoverable: Boolean,
        val suggestedAction: String? = null
    ) : DomainPlaybackState()
}

data class DomainMediaItem(
    val uri: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val audioTrackCount: Int = 0,
    val subtitleTrackCount: Int = 0,
    val dateAddedSec: Long = 0,
    val folderPath: String = "",
    val playbackPositionMs: Long = 0L,
    val isCompleted: Boolean = false
)

data class DomainTrackSelection(
    val selectedAudioTrackIndex: Int? = null,
    val selectedSubtitleTrackIndex: Int? = null,
    val subtitlesEnabled: Boolean = false,
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingMs: Long? = null
)

data class DomainPlaybackQueue(
    val items: List<DomainMediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val playMode: PlayMode = PlayMode.NORMAL
) {
    val currentItem: DomainMediaItem?
        get() = if (currentIndex in items.indices) items[currentIndex] else null

    val hasNext: Boolean
        get() = when (playMode) {
            PlayMode.REPEAT_ONE -> currentItem != null
            PlayMode.REPEAT_ALL -> items.isNotEmpty()
            PlayMode.NORMAL, PlayMode.SHUFFLE -> currentIndex + 1 < items.size
        }

    val hasPrevious: Boolean
        get() = when (playMode) {
            PlayMode.REPEAT_ONE -> currentItem != null
            PlayMode.REPEAT_ALL -> items.isNotEmpty()
            PlayMode.NORMAL, PlayMode.SHUFFLE -> currentIndex > 0
        }
}
