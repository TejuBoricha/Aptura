package org.aptura.player.ui.visual

import android.os.SystemClock
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.model.AbRepeatState
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.Bookmark
import org.aptura.player.domain.model.EqualizerBand
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.onboarding.OnboardingScreen
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerSheet
import org.aptura.player.ui.player.PlayerUiState
import org.aptura.player.ui.player.TrackInfo
import org.aptura.player.ui.settings.SettingsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PLAY_STORE_PHONE = "w360dp-h800dp-xxhdpi"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PLAY_STORE_PHONE)
class PlayStoreGalleryTest : GalleryBase() {

    private val playerBase = PlayerUiState(
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
        audioDelayMs = 150L,
        subtitleDelayMs = -250L,
        audioEnhancement = AudioEnhancementState(
            enabled = true,
            bassBoostPercent = 30,
            bands = listOf(60, 230, 910, 3600, 14000).mapIndexed { i, f -> EqualizerBand(i, f, listOf(300, 150, 0, -100, 200)[i]) }
        ),
        sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 1_200_000L,
        abRepeat = AbRepeatState(1_800_000L, 2_400_000L, true),
        bookmarks = listOf(Bookmark(1L, "content://media/external/video/media/1", 1_250_000L, "Opening shot")),
        nextItemTitle = "Arrival",
        nextItemDurationMs = 6_960_000L,
        nextItemUri = "content://media/external/video/media/2",
        queueItems = emptyList(),
        currentQueueIndex = 0
    )

    private fun renderPlayer(state: PlayerUiState, accent: AccentColor = AccentColor.CYAN) = themed(ThemeMode.DARK, accent) {
        PlayerScreen(
            uiState = state,
            player = null,
            onBackClick = {},
            onPlayPauseClick = {},
            onSeekRelative = {},
            onScrubStart = {},
            onScrubMove = {},
            onScrubEnd = {},
            onControlsVisibilityToggle = {},
            onLockToggle = {},
            onSpeedSelected = {},
            onResizeModeToggle = {},
            onSpeedSheetVisible = {},
            onAudioSheetVisible = {},
            onSubtitleSheetVisible = {},
            onAudioTrackSelected = {},
            onSubtitleTrackSelected = {},
            onVolumePercentChanged = {},
            onBrightnessPercentChanged = {},
            onRetryPlayback = {}
        )
    }

    @Test
    fun store01LibraryHome() {
        themed(ThemeMode.LIGHT) {
            MediaLibraryScreen(
                uiState = librarySuccess(),
                onVideoClick = { _, _ -> },
                onSearchQueryChanged = {},
                onFolderSelected = {},
                onSortOrderChanged = {},
                onRefresh = {}
            )
        }
        shoot("play_store_01_library_home")
    }

    @Test
    fun store02PlayerPlayback() {
        renderPlayer(playerBase.copy(queueItems = sampleItems.take(5)), AccentColor.GOLD)
        shoot("play_store_02_player_playback")
    }

    @Test
    fun store03AudioSyncSheet() {
        renderPlayer(playerBase.copy(queueItems = sampleItems.take(5), activeSheet = PlayerSheet.Audio))
        shoot("play_store_03_audio_sync_sheet")
    }

    @Test
    fun store04DarkGoldSettings() {
        themed(ThemeMode.DARK, AccentColor.GOLD) {
            SettingsScreen(
                settings = ApturaSettings(
                    themeMode = ThemeMode.DARK,
                    accentColor = AccentColor.GOLD,
                    persistedSafFolders = setOf("content://com.android.externalstorage.documents/tree/primary%3AMovies")
                ),
                libraryStats = LibraryStatistics(totalVideos = 8, totalDurationMs = 33_802_000, totalSizeBytes = 14_400_000_000),
                onBackClick = {},
                onUpdateSpeed = {},
                onUpdateResizeMode = {},
                onUpdateSeekDelta = {},
                onUpdateBackgroundAudio = {},
                onUpdateAutoAdvanceCountdown = {},
                onRescanClick = {},
                onReplayOnboardingClick = {},
                hiddenVideoCount = 2,
                missingVideoCount = 1
            )
        }
        shoot("play_store_04_dark_gold_settings")
    }

    @Test
    fun store05SubtitlesSheet() {
        renderPlayer(playerBase.copy(queueItems = sampleItems.take(5), activeSheet = PlayerSheet.Subtitle))
        shoot("play_store_05_subtitles_sheet")
    }

    @Test
    fun store06PrivacyOnboarding() {
        themed(ThemeMode.LIGHT) {
            OnboardingScreen(
                onRequestPermissionClick = {},
                onContinueWithoutPermissionClick = {},
                hasStorageAccess = false,
                animateDemos = false
            )
        }
        shoot("play_store_06_privacy_onboarding")
    }
}
