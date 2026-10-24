package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** On a tablet-width window there is room for everything, so nothing hides behind the overflow menu. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h600dp-xhdpi")
class PlayerWideTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onAWideWindow_theActionsStayInTheBar_withFullTouchTargets() {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(videoTitle = "Show.S01E01", durationMs = 120_000L, isPlaying = true, isControlsVisible = true),
                    player = null,
                    onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
                    onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = {},
                    onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {}, onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {}, onSubtitleTrackSelected = {}, onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {}, onRetryPlayback = {}
                )
            }
        }

        assertEquals(0, composeRule.onAllNodes(hasContentDescription("More options")).fetchSemanticsNodes().size)
        listOf("Screen Orientation", "Picture in Picture", "Aspect Ratio", "Speed").forEach { description ->
            composeRule.onNodeWithContentDescription(description).assertIsDisplayed()
                .assertTouchTarget(composeRule.density)
        }
    }

    @Test
    fun withoutPipSupport_theInlineControlIsHidden_butTheOtherActionsStay() {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(videoTitle = "Show.S01E01", durationMs = 120_000L, isPlaying = true, isControlsVisible = true),
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

        assertEquals(0, composeRule.onAllNodes(hasContentDescription("Picture in Picture")).fetchSemanticsNodes().size)
        listOf("Screen Orientation", "Aspect Ratio", "Speed").forEach { description ->
            composeRule.onNodeWithContentDescription(description).assertIsDisplayed()
        }
    }
}
