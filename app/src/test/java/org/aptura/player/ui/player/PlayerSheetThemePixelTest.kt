package org.aptura.player.ui.player

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaPalettes
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.theme.themeModeForRoute
import org.aptura.player.ui.visual.captureRoot
import org.aptura.player.ui.visual.toPx
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The player route resolves to dark on purpose, but the rule lived inline in navigation, so nothing painted under a
 * Light app theme proved it (audit 2026-10-24, F-21). This renders exactly what navigation passes - the resolver's
 * output - and inspects the pixels of the quick speed sheet, which is a separate window. A regression that lets the
 * Light palette reach the player turns the sheet near-white and this fails even though every label is still present.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xhdpi")
class PlayerSheetThemePixelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showQuickSpeedSheetUnderLightApp() {
        // The user picked Light for the app; navigation asks the resolver what the player should use.
        val routeMode = themeModeForRoute(isPlayerRouteActive = true, appThemeMode = ThemeMode.LIGHT)
        composeRule.setContent {
            ApturaTheme(themeMode = routeMode, accentColor = AccentColor.CYAN) {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Inception",
                        durationMs = 120_000L,
                        isPlaying = true,
                        isControlsVisible = true,
                        activeSheet = PlayerSheet.Speed,
                        playbackSpeed = 1.0f
                    ),
                    player = null,
                    onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
                    onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = {},
                    onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {},
                    onSubtitleSheetVisible = {}, onAudioTrackSelected = {}, onSubtitleTrackSelected = {},
                    onVolumePercentChanged = {}, onBrightnessPercentChanged = {}, onRetryPlayback = {}
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun underALightAppTheme_theQuickSpeedSheet_staysOnTheDarkSurface() {
        showQuickSpeedSheetUnderLightApp()

        // The sheet is its own window; capture that root so the main player's black frame cannot mask the result.
        val sheet = composeRule.onNodeWithTag("speed_rail").captureRoot()
        val rail = composeRule.onNodeWithTag("speed_rail").getUnclippedBoundsInRoot()

        // The column leaves 24 dp between the rail and the sheet's bottom edge: pure container fill, clear of the
        // ramp's ticks, labels and rounded corners.
        val x = composeRule.toPx(rail.left + 8.dp)
        val y = composeRule.toPx(rail.bottom + 8.dp)
        // The expectation is the fixed dark palette, not the live token: reading ApturaColors here would compare the
        // pixels against themselves and pass even if a Light palette had leaked into the player.
        val darkSheet = ApturaPalettes.dark(AccentColor.CYAN).surfaceElevated2
        sheet.assertRegionIs(
            darkSheet,
            x,
            y,
            x + 1,
            y + 1,
            "quick speed sheet under a Light app theme"
        )
    }
}
