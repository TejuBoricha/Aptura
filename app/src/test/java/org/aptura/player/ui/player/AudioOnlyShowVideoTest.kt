package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Found on a device, invisible to the earlier tests: in Audio Only mode the "Show video" button could not be
 * pressed because the full-screen gesture layer sat above it and swallowed every touch. The click here is a real
 * touch routed through hit-testing (not a semantics shortcut), so it fails if anything covers the button.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioOnlyShowVideoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showVideoButton_receivesTheTouch_andAsksToTurnVideoBackOn() {
        val requested = mutableListOf<Boolean>()

        composeTestRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Inception",
                        durationMs = 120_000L,
                        currentPositionMs = 30_000L,
                        isPlaying = true,
                        isAudioOnly = true,
                        isControlsVisible = false
                    ),
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
                    onRetryPlayback = {},
                    onAudioOnlyChanged = { requested += it }
                )
            }
        }

        composeTestRule.onNodeWithText("Show video").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show video").performTouchInput { click() }

        assertEquals(listOf(false), requested)
    }
}
