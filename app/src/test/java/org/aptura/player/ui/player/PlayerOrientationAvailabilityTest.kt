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
 * On large screens (tablets sw >= 600dp), orientation locking is disabled/unsupported per UX-08
 * because Android 14+/16 per-app orientation overrides ignore or letterbox requested orientations,
 * and fixed tablet docks/kickstands make orientation locking disruptive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerOrientationAvailabilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onAPhoneWithOrientationLockDisabled_theOverflowOmitsScreenOrientation() {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(videoTitle = "Inception", durationMs = 120_000L, isPlaying = true, isControlsVisible = true),
                    player = null,
                    orientationSupported = false,
                    onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
                    onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = {},
                    onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {}, onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {}, onSubtitleTrackSelected = {}, onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {}, onRetryPlayback = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("Screen orientation").assertCountEquals(0)
        composeRule.onAllNodes(hasContentDescription("Screen orientation")).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun onATablet_theOrientationControlIsOmittedByDefault() {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(videoTitle = "Inception", durationMs = 120_000L, isPlaying = true, isControlsVisible = true),
                    player = null,
                    onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
                    onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = {},
                    onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {}, onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {}, onSubtitleTrackSelected = {}, onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {}, onRetryPlayback = {}
                )
            }
        }

        composeRule.onAllNodesWithText("Screen orientation").assertCountEquals(0)
        composeRule.onAllNodes(hasContentDescription("Screen orientation")).assertCountEquals(0)
    }
}
