package org.aptura.player.ui.visual

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.model.AbRepeatState
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.EqualizerBand
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.onboarding.OnboardingScreen
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerSheet
import org.aptura.player.ui.player.PlayerUiState
import org.aptura.player.ui.player.SeekTapSide
import org.aptura.player.ui.player.TrackInfo
import org.aptura.player.ui.settings.SettingsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SettingsGalleryTest : GalleryBase() {

    private fun settings(mode: ThemeMode, accent: AccentColor = AccentColor.CYAN) = themed(mode, accent) {
        SettingsScreen(
            settings = ApturaSettings(themeMode = mode, accentColor = accent, persistedSafFolders = setOf("content://com.android.externalstorage.documents/tree/primary%3AMovies")),
            libraryStats = LibraryStatistics(totalVideos = 8, totalDurationMs = 33_802_000, totalSizeBytes = 14_400_000_000),
            onBackClick = {}, onUpdateSpeed = {}, onUpdateResizeMode = {}, onUpdateSeekDelta = {},
            onUpdateBackgroundAudio = {}, onUpdateAutoAdvanceCountdown = {}, onRescanClick = {},
            onReplayOnboardingClick = {}, hiddenVideoCount = 2, missingVideoCount = 1
        )
    }

    @Config(qualifiers = "w411dp-h5200dp-xhdpi")
    @Test fun fullLight() { settings(ThemeMode.LIGHT); shoot("settings_full_light") }

    @Config(qualifiers = "w411dp-h5200dp-xhdpi")
    @Test fun fullDark() { settings(ThemeMode.DARK, AccentColor.VIOLET); shoot("settings_full_dark") }

    @Test fun phoneTop() { settings(ThemeMode.LIGHT); shoot("settings_phone_top") }

    @Test fun phoneScrolled() {
        settings(ThemeMode.DARK)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Default aspect ratio"))
        settle()
        shoot("settings_phone_scrolled_dark")
    }

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test fun tablet() { settings(ThemeMode.LIGHT); shoot("settings_tablet") }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class OnboardingGalleryTest : GalleryBase() {

    private fun pages(mode: ThemeMode, prefix: String, access: Boolean) {
        themed(mode) {
            OnboardingScreen(onRequestPermissionClick = {}, onContinueWithoutPermissionClick = {}, hasStorageAccess = access)
        }
        for (page in 0 until 5) {
            shoot("${prefix}_$page")
            val next = composeRule.onAllNodes(hasText("Continue")).fetchSemanticsNodes()
            if (next.isEmpty()) break
            composeRule.onAllNodes(hasText("Continue"))[0].performClick()
            settle()
        }
    }

    @Test fun light() = pages(ThemeMode.LIGHT, "onboarding_light", access = false)
    @Test fun dark() = pages(ThemeMode.DARK, "onboarding_dark", access = true)

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test fun tablet() = pages(ThemeMode.LIGHT, "onboarding_tablet", access = false)
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-xhdpi")
class PlayerGalleryTest : GalleryBase() {

    private val base = PlayerUiState(
        videoUri = "content://media/external/video/media/1",
        videoTitle = "Blade Runner 2049",
        durationMs = 9_840_000L,
        currentPositionMs = 2_100_000L,
        bufferedPositionMs = 2_600_000L,
        isPlaying = true,
        isControlsVisible = true,
        hasNext = true,
        hasPrevious = true,
        audioTracks = listOf(
            TrackInfo("a1", 0, "English · 5.1 · E-AC3", "en", isSelected = true),
            TrackInfo("a2", 1, "English · Commentary · AAC", "en"),
            TrackInfo("a3", 2, "Français · 5.1 · AC3", "fr")
        ),
        subtitleTracks = listOf(
            TrackInfo("s1", 0, "English · SDH", "en", isSelected = true),
            TrackInfo("s2", 1, "Español", "es"),
            TrackInfo("s3", 2, "Blade.Runner.2049.en.srt", "en", isExternal = true)
        ),
        selectedAudioTrackIndex = 0,
        selectedSubtitleTrackIndex = 0,
        audioEnhancement = AudioEnhancementState(
            enabled = true,
            bassBoostPercent = 30,
            bands = listOf(60, 230, 910, 3600, 14000).mapIndexed { i, f -> EqualizerBand(i, f, listOf(300, 150, 0, -100, 200)[i]) }
        ),
        abRepeat = AbRepeatState(),
        nextItemTitle = "Arrival",
        nextItemDurationMs = 6_960_000L,
        queueItems = sampleItems.take(5),
        currentQueueIndex = 0
    )

    private fun player(state: PlayerUiState) = themed(ThemeMode.DARK) {
        PlayerScreen(
            uiState = state, player = null, onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {},
            onScrubStart = {}, onScrubMove = {}, onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {},
            onSpeedSelected = {}, onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {},
            onSubtitleSheetVisible = {}, onAudioTrackSelected = {}, onSubtitleTrackSelected = {},
            onVolumePercentChanged = {}, onBrightnessPercentChanged = {}, onRetryPlayback = {}
        )
    }

    @Test fun hud() { player(base); shoot("player_hud") }
    @Test fun paused() { player(base.copy(isPlaying = false)); shoot("player_paused") }
    @Test fun buffering() { player(base.copy(isBuffering = true, isControlsVisible = false)); shoot("player_buffering") }
    @Test fun locked() { player(base.copy(isLocked = true, isLockHintVisible = true)); shoot("player_locked") }
    @Test fun volume() { player(base.copy(isControlsVisible = false, volumePercent = 64)); shoot("player_volume") }
    @Test fun brightness() { player(base.copy(isControlsVisible = false, brightnessPercent = 30)); shoot("player_brightness") }
    @Test fun doubleTap() { player(base.copy(isControlsVisible = false, seekDeltaSeconds = 20, doubleTapSeekSide = SeekTapSide.RIGHT)); shoot("player_doubletap") }
    @Test fun swipeSeek() { player(base.copy(isControlsVisible = false, swipeSeekTargetMs = 2_160_000, swipeSeekDeltaMs = 60_000)); shoot("player_swipe_seek") }
    @Test fun error() {
        player(base.copy(isPlaying = false, errorMessage = "This video uses a codec your device can't decode in hardware.", errorCode = "DECODER_INIT_FAILED", canSwitchToSoftware = true))
        shoot("player_error")
    }
    @Test fun upNext() { player(base.copy(isEnded = true, isPlaying = false, autoAdvanceCountdownSeconds = 6)); shoot("player_up_next") }
    @Test fun compatNotice() { player(base.copy(isCompatibilityMode = true, compatibilityNotice = "Playing with software decoding")); shoot("player_compat_notice") }
    @Test fun abLoop() { player(base.copy(abRepeat = AbRepeatState(1_800_000, 2_400_000, true), sleepTimerRemainingMs = 1_200_000)); shoot("player_ab_sleep") }

    @Test fun speed() { player(base.copy(activeSheet = PlayerSheet.Speed)); shoot("sheet_speed") }
    @Test fun audio() { player(base.copy(activeSheet = PlayerSheet.Audio)); shoot("sheet_audio") }
    @Test fun subtitle() { player(base.copy(activeSheet = PlayerSheet.Subtitle)); shoot("sheet_subtitle") }
    @Test fun advanced() { player(base.copy(activeSheet = PlayerSheet.Advanced)); shoot("sheet_advanced") }
    @Test fun equalizer() { player(base.copy(activeSheet = PlayerSheet.Equalizer)); shoot("sheet_equalizer") }
    @Test fun picture() { player(base.copy(activeSheet = PlayerSheet.Picture)); shoot("sheet_picture") }
    @Test fun queue() { player(base.copy(activeSheet = PlayerSheet.Queue)); shoot("sheet_queue") }
    @Test fun playMode() { player(base.copy(activeSheet = PlayerSheet.PlayMode)); shoot("sheet_playmode") }
    @Test fun why() { player(base.copy(activeSheet = PlayerSheet.WhyCompatibility)); shoot("sheet_why") }
    @Test fun web() { player(base.copy(activeSheet = PlayerSheet.WebSearch, cleanSearchQuery = "Blade Runner 2049")); shoot("sheet_websearch") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitHud() { player(base); shoot("player_portrait_hud") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitSubtitle() { player(base.copy(activeSheet = PlayerSheet.Subtitle)); shoot("sheet_subtitle_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitAdvanced() { player(base.copy(activeSheet = PlayerSheet.Advanced)); shoot("sheet_advanced_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitAudio() { player(base.copy(activeSheet = PlayerSheet.Audio)); shoot("sheet_audio_portrait") }
}
