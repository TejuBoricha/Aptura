package org.aptura.player.ui.player

import androidx.compose.runtime.Immutable
import org.aptura.player.domain.model.AbRepeatState
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.PictureAdjustmentState
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
 * Edge side for double-tap seek gesture feedback (FR-CTRL-002).
 */
enum class SeekTapSide {
    LEFT,
    RIGHT
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
    val isSelected: Boolean = false,
    val isExternal: Boolean = false
)

/**
 * Algebraic sum type representing the currently active modal sheet or dialog on the player screen.
 * Conforms to Aptura Coding Standards §6 (eliminating boolean explosion & illegal overlapping states).
 */
sealed interface PlayerSheet {
    data object Audio : PlayerSheet
    data object Subtitle : PlayerSheet
    data object Speed : PlayerSheet
    data object Advanced : PlayerSheet
    data object Equalizer : PlayerSheet
    data object Picture : PlayerSheet
    data object Queue : PlayerSheet
    data object PlayMode : PlayerSheet
    data object WhyCompatibility : PlayerSheet
    data object WebSearch : PlayerSheet
}

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
    val activeSheet: PlayerSheet? = null,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val delayStepMs: Long = 50L,
    val audioEnhancement: AudioEnhancementState = AudioEnhancementState(),
    val pictureAdjustment: PictureAdjustmentState = PictureAdjustmentState(),
    val abRepeat: AbRepeatState = AbRepeatState(),
    val sleepTimerRemainingMs: Long? = null,
    /** The sleep timer is set to pause when this video ends rather than after a time. */
    val sleepAtEndOfVideo: Boolean = false,
    val volumePercent: Int? = null,
    val brightnessPercent: Int? = null,
    val seekDeltaSeconds: Int? = null,
    val doubleTapSeekSide: SeekTapSide? = null,
    val swipeSeekTargetMs: Long? = null,
    val swipeSeekDeltaMs: Long? = null,
    val isLockHintVisible: Boolean = false,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val errorTaxonomyCode: String? = null,
    val canSwitchToSoftware: Boolean = false,
    val isCompatibilityMode: Boolean = false,
    val compatibilityNotice: String? = null,
    val isRecoverableError: Boolean = true,
    val isScrubbing: Boolean = false,
    val scrubPositionMs: Long = 0L,
    val autoAdvanceCountdownSeconds: Int? = null,
    val autoAdvanceTotalSeconds: Int = 10,
    val nextItemTitle: String? = null,
    val subtitleFontSizeSp: Int = 18,
    val subtitleTextColorHex: String = "#FFFFFF",
    val subtitleBackgroundEnabled: Boolean = false,
    val bookmarks: List<org.aptura.player.domain.model.Bookmark> = emptyList(),
    /** The decoder in effect for the current video (its own choice, else the Settings default, else a fallback). */
    val decoderStrategy: Int = 0,
    /** True when [decoderStrategy] is this video's own choice rather than the Settings default. */
    val decoderOverridden: Boolean = false,
    val cleanSearchQuery: String = "",
    val sanitizedTitle: String = "",
    val recentDownloadedSubtitle: org.aptura.player.data.repository.DetectedSidecarSubtitle? = null,
    val autoPairedSubtitleToast: String? = null,
    val queueItems: List<org.aptura.player.domain.model.DomainMediaItem> = emptyList(),
    val currentQueueIndex: Int = -1,
    val nextItemDurationMs: Long? = null,
    val nextItemUri: String? = null,
    val isStatsOverlayVisible: Boolean = false,
    val playbackStats: org.aptura.player.domain.model.PlaybackStatsInfo = org.aptura.player.domain.model.PlaybackStatsInfo(),
    val failedCodecName: String? = null,
    val failureReason: String? = null,
    /** Double-tap / transport seek step from Settings (was hard-coded to 10 s). */
    val seekStepMs: Long = 10_000L,
    /** Audio-only mode: video is not decoded or rendered; audio keeps playing. */
    val isAudioOnly: Boolean = false
) {
    // Computed property compatibility bridges for Compose bindings & legacy tests
    val isAudioSheetVisible: Boolean get() = activeSheet == PlayerSheet.Audio
    val isSubtitleSheetVisible: Boolean get() = activeSheet == PlayerSheet.Subtitle
    val isSpeedSheetVisible: Boolean get() = activeSheet == PlayerSheet.Speed
    val isAdvancedSheetVisible: Boolean get() = activeSheet == PlayerSheet.Advanced
    val isEqualizerSheetVisible: Boolean get() = activeSheet == PlayerSheet.Equalizer
    val isPictureSheetVisible: Boolean get() = activeSheet == PlayerSheet.Picture
    val isQueueSheetVisible: Boolean get() = activeSheet == PlayerSheet.Queue
    val isPlayModeSheetVisible: Boolean get() = activeSheet == PlayerSheet.PlayMode
    val isWhyCompatibilitySheetVisible: Boolean get() = activeSheet == PlayerSheet.WhyCompatibility
    val isWebSearchDialogVisible: Boolean get() = activeSheet == PlayerSheet.WebSearch

    val isAnySheetVisible: Boolean get() = activeSheet != null

    fun countActiveModals(): Int = if (activeSheet != null) 1 else 0

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
