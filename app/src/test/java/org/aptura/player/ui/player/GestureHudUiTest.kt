package org.aptura.player.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.aptura.player.theme.ApturaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for Gesture HUD Overlays via Robolectric (FR-CTRL-002, FR-CTRL-005, FR-CTRL-006, FR-CTRL-015).
 * Validates semantic display, tabular figure badges, level gauges, and edge ripple cues.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GestureHudUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verticalLevelGaugeHud_rendersVolumePercent_andIcon() {
        composeTestRule.setContent {
            ApturaTheme {
                VerticalLevelGaugeHud(
                    valuePercent = 75,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Volume"
                )
            }
        }

        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Volume").assertIsDisplayed()
    }

    @Test
    fun verticalLevelGaugeHud_rendersBrightnessPercent() {
        composeTestRule.setContent {
            ApturaTheme {
                VerticalLevelGaugeHud(
                    valuePercent = 42,
                    icon = Icons.Filled.BrightnessMedium,
                    label = "Brightness"
                )
            }
        }

        composeTestRule.onNodeWithText("42%").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Brightness").assertIsDisplayed()
    }

    @Test
    fun doubleTapSeekRippleOverlay_rendersForwardAccumulation() {
        composeTestRule.setContent {
            ApturaTheme {
                DoubleTapSeekRippleOverlay(
                    seekDeltaSeconds = 20,
                    side = SeekTapSide.RIGHT
                )
            }
        }

        composeTestRule.onNodeWithText("+20s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ahead").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Forward seek direction").assertIsDisplayed()
        val bounds = composeTestRule.onNodeWithTag("double_tap_seek_hud").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals(132f, (bounds.right - bounds.left).value, 0.1f)
        org.junit.Assert.assertEquals(68f, (bounds.bottom - bounds.top).value, 0.1f)
    }

    @Test
    fun doubleTapSeekRippleOverlay_rendersRewindAccumulation() {
        composeTestRule.setContent {
            ApturaTheme {
                DoubleTapSeekRippleOverlay(
                    seekDeltaSeconds = -30,
                    side = SeekTapSide.LEFT
                )
            }
        }

        composeTestRule.onNodeWithText("-30s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Backward seek direction").assertIsDisplayed()
    }

    @Test
    fun horizontalSwipeSeekHud_rendersDeltaBadge_andTargetTimecode() {
        composeTestRule.setContent {
            ApturaTheme {
                HorizontalSwipeSeekHud(
                    targetMs = 45_000L,
                    durationMs = 120_000L,
                    deltaMs = 15_000L
                )
            }
        }

        composeTestRule.onNodeWithText("+0:15").assertIsDisplayed()
        composeTestRule.onNodeWithText("0:45").assertIsDisplayed()
        composeTestRule.onNodeWithText("2:00").assertIsDisplayed()
    }

    @Test
    fun lockModeGuardHint_rendersLockInstruction() {
        composeTestRule.setContent {
            ApturaTheme {
                LockModeGuardHint()
            }
        }

        composeTestRule.onNodeWithText("Screen locked · Tap Unlock to use the controls").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Screen locked").assertIsDisplayed()
    }
}
