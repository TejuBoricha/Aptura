package org.aptura.player.ui.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PiP is a system feature, not a guarantee: some TVs and low-end tablets do not offer it. The player must not show a
 * control that can only fail (audit 2026-10-24). On a phone the control lives in the overflow menu; on a tablet it is
 * inline, so both paths need the same guard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerPipAvailabilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onAPhoneWithoutPipSupport_theOverflowOmitsPictureInPicture() {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(videoTitle = "Inception", durationMs = 120_000L, isPlaying = true, isControlsVisible = true),
                    player = null,
                    pipSupported = false,
                    onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
                    onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = {},
                    onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {}, onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {}, onSubtitleTrackSelected = {}, onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {}, onRetryPlayback = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("Picture in picture").assertCountEquals(0)
        composeRule.onAllNodes(hasContentDescription("Picture in Picture")).assertCountEquals(0)
    }
}
