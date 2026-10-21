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
        val playbackSpeed: Float = 1.0f,
        val bufferedPositionMs: Long = 0L
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

data class DomainTrackInfo(
    val id: String,
    val index: Int,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false
)

data class DomainTrackSelection(
    val availableAudioTracks: List<DomainTrackInfo> = emptyList(),
    val availableSubtitleTracks: List<DomainTrackInfo> = emptyList(),
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

/**
 * Standard equalizer presets for audio enhancement (FR-AUD-005).
 */
enum class EqualizerPreset(val label: String) {
    FLAT("Flat"),
    BASS_BOOST("Bass Boost"),
    VOCAL_BOOST("Vocal Boost"),
    ROCK("Rock"),
    POP("Pop"),
    CLASSICAL("Classical"),
    JAZZ("Jazz"),
    CUSTOM("Custom");

    val displayName: String
        get() = label
}

/**
 * Individual frequency band configuration for the multi-band equalizer.
 */
data class EqualizerBand(
    val bandIndex: Int,
    val centerFreqHz: Int,
    val levelMilliBels: Int,
    val minMilliBels: Int = -1500,
    val maxMilliBels: Int = 1500
)

/**
 * Complete audio enhancement and sound effects state.
 */
data class AudioEnhancementState(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val loudnessBoostPercent: Int = 0,
    val bands: List<EqualizerBand> = emptyList()
)

/**
 * Video picture adjustments and eye comfort night mode state.
 */
data class PictureAdjustmentState(
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val isNightModeEnabled: Boolean = false,
    val nightModeWarmth: Float = 0.4f
) {
    val isModified: Boolean
        get() = contrast != 1.0f || saturation != 1.0f || isNightModeEnabled
}

/**
 * A-B Repeat loop state for repeated playback of a custom segment.
 */
data class AbRepeatState(
    val pointA: Long? = null,
    val pointB: Long? = null,
    val isLooping: Boolean = false
) {
    val isActive: Boolean
        get() = pointA != null || pointB != null
}
