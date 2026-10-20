package org.aptura.player.ui.player

import androidx.compose.runtime.Immutable
import org.aptura.player.domain.model.PlayMode

/**
 * Aspect ratio display modes for the video surface (FR-CTRL-005, SCR-010).
 */
enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    FIXED_16_9("16:9"),
    FIXED_4_3("4:3");

    fun next(): AspectRatioMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

/**
 * Track metadata for audio and subtitle track selection sheets (FR-CTRL-009, SCR-010c/d).
 */
@Immutable
data class TrackInfo(
    val id: String,
    val index: Int,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false
)

/**
 * Immutable UI state for the video player screen (SCR-010).
 * Backed by Unidirectional Data Flow (UDF) from PlayerViewModel.
 */
@Immutable
data class PlayerUiState(
    val videoUri: String = "",
    val videoTitle: String = "",
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val playMode: PlayMode = PlayMode.NORMAL,
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val resizeMode: AspectRatioMode = AspectRatioMode.FIT,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val selectedAudioTrackIndex: Int? = null,
    val selectedSubtitleTrackIndex: Int? = null,
    val isAudioSheetVisible: Boolean = false,
    val isSubtitleSheetVisible: Boolean = false,
    val isSpeedSheetVisible: Boolean = false,
    val volumePercent: Int? = null,
    val brightnessPercent: Int? = null,
    val seekDeltaSeconds: Int? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val isRecoverableError: Boolean = true,
    val isScrubbing: Boolean = false,
    val scrubPositionMs: Long = 0L
) {
    val effectivePositionMs: Long
        get() = if (isScrubbing) scrubPositionMs else currentPositionMs

    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (effectivePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val bufferedFraction: Float
        get() = if (durationMs > 0L) {
            (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
