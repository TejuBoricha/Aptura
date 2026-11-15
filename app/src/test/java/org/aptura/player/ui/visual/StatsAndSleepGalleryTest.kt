package org.aptura.player.ui.visual

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.aptura.player.domain.model.PlaybackStatsInfo
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerSheet
import org.aptura.player.ui.player.PlayerUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Review renders of the two surfaces that are easiest to get wrong at the edges: the playback stats panel with the
 * longest strings real devices produce, and the sleep timer section in each of its states. Review only: it renders
 * nothing unless `APTURA_GALLERY_DIR` is set, like the rest of the gallery.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class StatsAndSleepGalleryTest : GalleryBase() {

    /** The longest strings a real device produces: a vendor secure decoder, a full HEVC codec string, 5.1 at 48 kHz. */
    private val longStats = PlaybackStatsInfo(
        decoderPath = "Software Fallback",
        isHardwareAccelerated = false,
        videoDecoderName = "c2.qti.hevc.decoder.secure",
        videoCodec = "hvc1.2.4.L153.B0",
        videoResolution = "3840x2160",
        frameRate = 23.976f,
        videoBitrateKbps = 24_500,
        droppedFrames = 412,
        dropPercentage = 6.4f,
        audioDecoderName = "c2.android.eac3.decoder",
        audioCodec = "ec-3",
        audioChannels = "5.1 Surround (6 ch)",
        audioSampleRateHz = 48_000,
        audioBitrateKbps = 640,
        audioDelayMs = -200L,
        subtitleDelayMs = 1_250L,
        hdrFormat = "Dolby Vision"
    )

    private val base = PlayerUiState(
        videoUri = "content://media/external/video/media/1",
        videoTitle = "Blade Runner 2049",
        durationMs = 9_840_000L,
        currentPositionMs = 2_100_000L,
        bufferedPositionMs = 2_600_000L,
        isPlaying = true,
        isControlsVisible = true,
        hasNext = true,
        hasPrevious = true
    )

    private fun player(state: PlayerUiState, fontScale: Float = 1f) = themed(ThemeMode.DARK) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlayerScreen(
                uiState = state, player = null, onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {},
                onScrubStart = {}, onScrubMove = {}, onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {},
                onSpeedSelected = {}, onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {},
                onSubtitleSheetVisible = {}, onAudioTrackSelected = {}, onSubtitleTrackSelected = {},
                onVolumePercentChanged = {}, onBrightnessPercentChanged = {}, onRetryPlayback = {}
            )
        }
    }

    private val statsOn = base.copy(isStatsOverlayVisible = true, playbackStats = longStats)

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun statsPortrait() { player(statsOn); shoot("stats_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun statsPortraitFont130() { player(statsOn, 1.3f); shoot("stats_portrait_font130") }

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test fun statsSideways() { player(statsOn); shoot("stats_sideways") }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test fun statsSmallSideways() { player(statsOn); shoot("stats_small_sideways") }

    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    @Test fun statsSmallPortrait() { player(statsOn); shoot("stats_small_portrait") }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test fun statsPortraitFont200() { player(statsOn, 2f); shoot("stats_portrait_font200") }

    // ---- Sleep timer: the Playback tools sheet in each state, on a tall window so nothing hides below the fold -------

    private fun sleepSheet(state: PlayerUiState, name: String) {
        player(state.copy(activeSheet = PlayerSheet.Advanced))
        shoot(name)
    }

    @Config(qualifiers = "w411dp-h1700dp-xhdpi")
    @Test fun sleepOff() = sleepSheet(base, "sleep_off")

    @Config(qualifiers = "w411dp-h1700dp-xhdpi")
    @Test fun sleepRunning() = sleepSheet(
        base.copy(
            sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 1_200_000L,
            sleepTimerTotalMs = 1_800_000L
        ),
        "sleep_running"
    )

    @Config(qualifiers = "w411dp-h1700dp-xhdpi")
    @Test fun sleepRunningCustom() = sleepSheet(
        base.copy(
            sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 2_000_000L,
            sleepTimerTotalMs = 2_100_000L
        ),
        "sleep_running_custom"
    )

    @Config(qualifiers = "w411dp-h1700dp-xhdpi")
    @Test fun sleepRunningLow() = sleepSheet(
        base.copy(
            sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 240_000L,
            sleepTimerTotalMs = 900_000L
        ),
        "sleep_running_low"
    )

    @Config(qualifiers = "w411dp-h1700dp-xhdpi")
    @Test fun sleepAtEnd() = sleepSheet(base.copy(sleepAtEndOfVideo = true), "sleep_at_end")

    @Config(qualifiers = "w411dp-h1700dp-xhdpi")
    @Test fun sleepExpired() = sleepSheet(base.copy(isPlaying = false, sleepTimerExpired = true), "sleep_expired")

    // The sheet is its own window, so the density override in player() does not reach it: the config's font scale does.
    @Config(qualifiers = "w411dp-h1700dp-xhdpi", fontScale = 2f)
    @Test fun sleepOffFont200() = sleepSheet(base, "sleep_off_font200")

    @Config(qualifiers = "w411dp-h1700dp-xhdpi", fontScale = 2f)
    @Test fun sleepRunningFont200() = sleepSheet(
        base.copy(
            sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 1_200_000L,
            sleepTimerTotalMs = 1_800_000L
        ),
        "sleep_running_font200"
    )

    @Config(qualifiers = "w320dp-h1700dp-xhdpi")
    @Test fun sleepExpiredNarrow() = sleepSheet(base.copy(isPlaying = false, sleepTimerExpired = true), "sleep_expired_narrow")

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test fun sleepOffSideways() = sleepSheet(base, "sleep_off_sideways")

    // A sheet shows only what fits and Robolectric will not scroll it, so a wide window is shown as the tab on its own.
    @Config(qualifiers = "w800dp-h1100dp-xhdpi")
    @Test fun sleepWideWindow() {
        themed(ThemeMode.DARK) {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier
                    .background(org.aptura.player.theme.ApturaColors.SurfaceElevated2)
                    .padding(20.dp)
            ) {
                org.aptura.player.ui.player.PlaybackToolsTab(
                    uiState = base.copy(
                        sleepTimerEndsAtElapsedRealtime = SystemClock.elapsedRealtime() + 2_400_000L,
                        sleepTimerTotalMs = 2_700_000L
                    ),
                    onAudioOnlyChanged = {}, onSpeedSelected = {}, onStartSleepTimer = {}, onCancelSleepTimer = {},
                    onExtendSleepTimer = {}, onSleepAtEndOfVideo = {}, onSetAbPointA = {}, onSetAbPointB = {},
                    onClearAbRepeat = {}, onStepFrame = {}, onAddBookmark = {}, onSeekTo = {}, onDeleteBookmark = {},
                    onUpdateBookmarkNote = { _, _ -> }, onToggleStatsOverlay = {}, onResetVideoPreferences = {}
                )
            }
        }
        shoot("sleep_wide_window")
    }
}
