package org.aptura.player.ui.settings

import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaPalettes
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.visual.CapturedScreen
import org.aptura.player.ui.visual.captureScreen
import org.aptura.player.ui.visual.toPx
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The compact Settings bar was a gradient that faded to transparent inside its own height, so scrolled rows showed
 * through the back button and title (audit 2026-10-24, F-04). The structural test next door proves the bar is tall
 * enough and uses an opaque colour token; this one proves the pixels actually drawn are that fill after a real scroll.
 * It would fail if the background became translucent, was skipped, or was drawn beneath the list.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xhdpi")
class SettingsTopBarPixelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(mode: ThemeMode) {
        composeRule.setContent {
            ApturaTheme(themeMode = mode) {
                SettingsScreen(
                    settings = ApturaSettings(themeMode = mode),
                    libraryStats = LibraryStatistics(),
                    onBackClick = {},
                    onUpdateSpeed = {},
                    onUpdateResizeMode = {},
                    onUpdateSeekDelta = {},
                    onUpdateBackgroundAudio = {},
                    onUpdateAutoAdvanceCountdown = {},
                    onRescanClick = {},
                    onReplayOnboardingClick = {}
                )
            }
        }
    }

    private fun scrollToTheBottomOfTheTitle(): CapturedScreen {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Default aspect ratio"))
        return composeRule.captureScreen()
    }

    @Test
    fun theLightTopBar_isAnOpaqueStrip_afterScrolling() = assertOpaque(ThemeMode.LIGHT)

    @Test
    fun theDarkTopBar_isAnOpaqueStrip_afterScrolling() = assertOpaque(ThemeMode.DARK)

    private fun assertOpaque(mode: ThemeMode) {
        show(mode)
        val screen = scrollToTheBottomOfTheTitle()

        val bar = composeRule.onNodeWithTag("settings_compact_bar")
            .getUnclippedBoundsInRoot()
        val expected = ApturaPalettes.forMode(mode == ThemeMode.DARK, AccentColor.CYAN).surfaceBase
        // The last two dp sit below the 48 dp back button and above the deliberate 1 dp hairline: a strip that must be
        // pure bar fill. Any scrolled list content visible here means the bar is translucent or missing.
        val y = composeRule.toPx(bar.bottom - 2.dp)
        val left = composeRule.toPx(bar.left + 1.dp)
        val right = composeRule.toPx(bar.right - 1.dp)

        assertTrue(
            "the sampled row must be inside the bar, not the hairline",
            y < composeRule.toPx(bar.bottom - 1.dp)
        )
        screen.assertRegionIs(expected, left, y, right, y + 1, "Settings compact bar row in $mode")
    }
}
