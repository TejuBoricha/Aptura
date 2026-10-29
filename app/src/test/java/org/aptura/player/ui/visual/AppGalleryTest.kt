package org.aptura.player.ui.visual

import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

    @Config(qualifiers = "w411dp-h5200dp-xhdpi")
    @Test fun dialogs() {
        settings(ThemeMode.LIGHT)
        composeRule.onNodeWithText("Reset").performClick()
        settle()
        shoot("settings_reset_dialog")
    }

    @Config(qualifiers = "w411dp-h5200dp-xhdpi")
    @Test fun deleteDiagnosticsDialog() {
        settings(ThemeMode.DARK)
        composeRule.onNodeWithText("Delete diagnostic data").performClick()
        settle()
        shoot("settings_delete_diag_dialog")
    }

    @Config(qualifiers = "w411dp-h5200dp-xhdpi")
    @Test fun codecDialog() {
        settings(ThemeMode.LIGHT)
        composeRule.onNodeWithText("Codec support").performClick()
        settle()
        shoot("settings_codec_dialog")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class OnboardingGalleryTest : GalleryBase() {

    private fun pages(
        mode: ThemeMode,
        prefix: String,
        access: Boolean,
        fontScale: Float = 1f,
        library: org.aptura.player.ui.library.MediaLibraryUiState.Success? = null
    ) {
        themed(mode) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)
            ) {
                OnboardingScreen(
                    onRequestPermissionClick = {},
                    onContinueWithoutPermissionClick = {},
                    hasStorageAccess = access,
                    animateDemos = false,
                    libraryState = library
                )
            }
        }
        for (page in 0 until 5) {
            shoot("${prefix}_$page")
            val next = composeRule.onAllNodes(hasText("Continue"))
            if (next.fetchSemanticsNodes().isEmpty()) break
            next[0].performClick()
            settle()
        }
    }

    @Test fun light() = pages(ThemeMode.LIGHT, "onboarding_light", access = false)
    @Test fun dark() = pages(ThemeMode.DARK, "onboarding_dark", access = true)

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test fun tablet() = pages(ThemeMode.LIGHT, "onboarding_tablet", access = false)

    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    @Test fun smallPhone() = pages(ThemeMode.DARK, "onboarding_small", access = false)

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test fun landscape() = pages(ThemeMode.DARK, "onboarding_landscape", access = false)

    @Test fun largeFont() = pages(ThemeMode.LIGHT, "onboarding_font200", access = false, fontScale = 2f)

    /** The replay from Settings: the person's own videos, with their own frames, on the library page. */
    @Test fun replayWithTheirOwnLibrary() =
        pages(ThemeMode.DARK, "onboarding_replay", access = true, library = librarySuccess(continueWatching = false))
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
        nextItemUri = sampleItems[1].uri,
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
    @Test fun buffering() { player(base.copy(isBuffering = true, isPlaying = false, isControlsVisible = false)); shoot("player_buffering") }
    @Test fun stats() { player(base.copy(isStatsOverlayVisible = true, isControlsVisible = false)); shoot("player_stats") }
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
    @Test fun abLoop() { player(base.copy(abRepeat = AbRepeatState(1_800_000, 2_400_000, true), sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 1_200_000)); shoot("player_ab_sleep") }
    @Test fun sleepExpired() { player(base.copy(isPlaying = false, sleepTimerExpired = true)); shoot("player_sleep_expired") }

    @Test fun speed() { player(base.copy(activeSheet = PlayerSheet.Speed)); shoot("sheet_speed") }
    @Test fun audio() { player(base.copy(activeSheet = PlayerSheet.Audio)); shoot("sheet_audio") }
    @Test fun subtitle() { player(base.copy(activeSheet = PlayerSheet.Subtitle)); shoot("sheet_subtitle") }
    @Test fun advanced() { player(base.copy(activeSheet = PlayerSheet.Advanced)); shoot("sheet_advanced") }
    @Test fun queue() { player(base.copy(activeSheet = PlayerSheet.Queue)); shoot("sheet_queue") }
    @Test fun why() { player(base.copy(activeSheet = PlayerSheet.WhyCompatibility)); shoot("sheet_why") }
    @Test fun web() { player(base.copy(activeSheet = PlayerSheet.WebSearch, cleanSearchQuery = "Blade Runner 2049")); shoot("sheet_websearch") }

    @Test fun audioOnly() { player(base.copy(isAudioOnly = true, videoTitle = "VID-20260928-WA0034")); shoot("player_audio_only") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun audioOnlySmallSideways() { player(base.copy(isAudioOnly = true, videoTitle = "VID-20260928-WA0034")); shoot("player_audio_only_small_sideways") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitAudioOnly() { player(base.copy(isAudioOnly = true, videoTitle = "VID-20260928-WA0034")); shoot("player_audio_only_portrait") }

    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    @Test fun smallPortraitAudioOnly() { player(base.copy(isAudioOnly = true, videoTitle = "VID-20260928-WA0034")); shoot("player_audio_only_small_portrait") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysHud() { player(base); shoot("player_small_sideways_hud") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysSubtitle() { player(base.copy(activeSheet = PlayerSheet.Subtitle)); shoot("sheet_subtitle_small_sideways") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysAdvanced() { player(base.copy(activeSheet = PlayerSheet.Advanced)); shoot("sheet_advanced_small_sideways") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysQueue() { player(base.copy(activeSheet = PlayerSheet.Queue)); shoot("sheet_queue_small_sideways") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysAbLoop() { player(base.copy(abRepeat = AbRepeatState(1_800_000, 2_400_000, true), sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 1_200_000)); shoot("player_small_sideways_ab_sleep") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysUpNext() { player(base.copy(isEnded = true, isPlaying = false, autoAdvanceCountdownSeconds = 6)); shoot("player_small_sideways_up_next") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysStats() { player(base.copy(isStatsOverlayVisible = true, isControlsVisible = true)); shoot("player_small_sideways_stats") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysCompat() { player(base.copy(isCompatibilityMode = true, compatibilityNotice = "Playing with software decoding")); shoot("player_small_sideways_compat") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysLocked() { player(base.copy(isLocked = true, isLockHintVisible = true)); shoot("player_small_sideways_locked") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun smallSidewaysSwipeSeek() { player(base.copy(swipeSeekTargetMs = 2_160_000, swipeSeekDeltaMs = 60_000, isControlsVisible = true)); shoot("player_small_sideways_swipe_seek") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitHud() { player(base); shoot("player_portrait_hud") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitSubtitle() { player(base.copy(activeSheet = PlayerSheet.Subtitle)); shoot("sheet_subtitle_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitAdvanced() { player(base.copy(activeSheet = PlayerSheet.Advanced)); shoot("sheet_advanced_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitWhy() { player(base.copy(activeSheet = PlayerSheet.WhyCompatibility, isCompatibilityMode = true)); shoot("sheet_why_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitQueue() { player(base.copy(activeSheet = PlayerSheet.Queue)); shoot("sheet_queue_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitSpeed() { player(base.copy(activeSheet = PlayerSheet.Speed, playbackSpeed = 1.5f)); shoot("sheet_speed_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun portraitAudio() { player(base.copy(activeSheet = PlayerSheet.Audio)); shoot("sheet_audio_portrait") }

    // ---- Every tab of every sheet, on a tall canvas so nothing hides below the fold --------------------------------

    private val tuned = base.copy(
        audioDelayMs = 150L,
        subtitleDelayMs = -250L,
        sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 1_200_000L,
        abRepeat = AbRepeatState(1_800_000, 2_400_000, true),
        bookmarks = listOf(
            org.aptura.player.domain.model.Bookmark(1, base.videoUri, 1_250_000, "Opening shot"),
            org.aptura.player.domain.model.Bookmark(2, base.videoUri, 4_810_000, "")
        )
        // Picture adjustments stay at default here: any change switches the video to a TextureView (for the colour
        // filter), which Robolectric cannot draw into a software canvas.
    )

    private fun tab(sheet: PlayerSheet, tabLabel: String?, name: String) {
        player(tuned.copy(activeSheet = sheet))
        if (tabLabel != null) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText(tabLabel))[0].performClick()
            settle()
        }
        shoot(name)
    }

    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tAudioTracks() = tab(PlayerSheet.Audio, null, "tab_audio_tracks")
    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tAudioEq() = tab(PlayerSheet.Audio, "Equalizer", "tab_audio_eq")
    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tAudioFx() = tab(PlayerSheet.Audio, "Effects", "tab_audio_fx")
    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tSubTracks() = tab(PlayerSheet.Subtitle, null, "tab_sub_tracks")
    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tSubTiming() = tab(PlayerSheet.Subtitle, "Timing", "tab_sub_timing")
    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tSubStyle() = tab(PlayerSheet.Subtitle, "Style", "tab_sub_style")
    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tPlayback() = tab(PlayerSheet.Advanced, null, "tab_playback")
    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tPicture() = tab(PlayerSheet.Advanced, "Picture", "tab_picture")

    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tPlaybackSleepAtEnd() {
        player(tuned.copy(activeSheet = PlayerSheet.Advanced, sleepTimerEndsAtElapsedRealtime = null, sleepAtEndOfVideo = true))
        shoot("tab_playback_sleep_at_end")
    }

    @Config(qualifiers = "w411dp-h1700dp-xhdpi") @Test fun tAudioTracksBluetooth() {
        val audio = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSystemService(android.media.AudioManager::class.java)
        org.robolectric.Shadows.shadowOf(audio).addOutputDevice(
            org.robolectric.shadows.AudioDeviceInfoBuilder.newBuilder()
                .setType(android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
                .build(),
            false
        )
        player(tuned.copy(activeSheet = PlayerSheet.Audio, audioDelayMs = 0L))
        shoot("tab_audio_tracks_bluetooth")
    }

    @Config(qualifiers = "w411dp-h2000dp-xhdpi") @Test fun tPlaybackContent() {
        // The whole tab outside its sheet: a sheet shows only what fits, and Robolectric won't scroll it.
        themed(ThemeMode.DARK) {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier
                    .background(org.aptura.player.theme.ApturaColors.SurfaceElevated2)
                    .padding(20.dp)
            ) {
                org.aptura.player.ui.player.PlaybackToolsTab(
                    uiState = tuned.copy(currentPositionMs = 3_723_417L),
                    onAudioOnlyChanged = {}, onSpeedSelected = {}, onStartSleepTimer = {}, onCancelSleepTimer = {},
                    onExtendSleepTimer = {}, onSleepAtEndOfVideo = {}, onSetAbPointA = {}, onSetAbPointB = {},
                    onClearAbRepeat = {}, onStepFrame = {}, onAddBookmark = {}, onSeekTo = {}, onDeleteBookmark = {},
                    onUpdateBookmarkNote = { _, _ -> }, onToggleStatsOverlay = {}, onResetVideoPreferences = {}
                )
            }
        }
        shoot("tab_playback_content")
    }
}
