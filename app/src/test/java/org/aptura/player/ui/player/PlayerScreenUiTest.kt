package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for PlayerScreen on JVM via Robolectric.
 * Validates semantic node tree, button click callbacks, transport visibility,
 * and lock mode transitions without requiring an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playerScreen_rendersTitle_andTransportButtonsWork() {
        var playPauseClicked = false
        var seekDelta: Long? = null

        composeTestRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Inception",
                        durationMs = 120_000L,
                        currentPositionMs = 30_000L,
                        isPlaying = true,
                        isControlsVisible = true
                    ),
                    player = null,
                    onBackClick = {},
                    onPlayPauseClick = { playPauseClicked = true },
                    onSeekRelative = { seekDelta = it },
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
        }

        // 1. Verify Video Title is rendered
        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()

        // 2. Verify Pause button is rendered when playing, and click works
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        assertTrue(playPauseClicked)

        // 3. Verify Rewind 10s button triggers callback
        composeTestRule.onNodeWithContentDescription("Rewind 10s").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Rewind 10s").performClick()
        assertEquals(-10_000L, seekDelta)

        // 4. Verify Forward 10s button triggers callback
        composeTestRule.onNodeWithContentDescription("Forward 10s").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Forward 10s").performClick()
        assertEquals(10_000L, seekDelta)
    }

    @Test
    fun playerScreen_whenPaused_displaysPlayButton() {
        var playClicked = false

        composeTestRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Interstellar",
                        durationMs = 60_000L,
                        currentPositionMs = 0L,
                        isPlaying = false,
                        isControlsVisible = true
                    ),
                    player = null,
                    onBackClick = {},
                    onPlayPauseClick = { playClicked = true },
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
        }

        // Verify Play button is displayed when paused
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Play").performClick()
        assertTrue(playClicked)
    }

    @Test
    fun playerScreen_whenLocked_showsLockPill_andHidesTransport() {
        var unlockClicked = false

        composeTestRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Dune",
                        durationMs = 90_000L,
                        currentPositionMs = 10_000L,
                        isPlaying = true,
                        isLocked = true,
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
                    onLockToggle = { unlockClicked = true },
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
        }

        // In locked mode, transport buttons must not exist
        composeTestRule.onNodeWithContentDescription("Pause").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Rewind 10s").assertDoesNotExist()

        // Locked indicator pill must be visible and clickable to toggle lock
        composeTestRule.onNodeWithText("Unlock").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unlock").performClick()
        assertTrue(unlockClicked)
    }

    @Test
    fun playerScreen_displaysSpeedBadge_andTriggersSpeedSheet() {
        var speedSheetTriggered = false

        composeTestRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Oppenheimer",
                        durationMs = 100_000L,
                        currentPositionMs = 10_000L,
                        playbackSpeed = 1.25f,
                        isControlsVisible = true
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
                    onSpeedSheetVisible = { speedSheetTriggered = it },
                    onAudioSheetVisible = {},
                    onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {},
                    onSubtitleTrackSelected = {},
                    onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {},
                    onRetryPlayback = {}
                )
            }
        }

        // Verify speed text is rendered on the badge
        composeTestRule.onNodeWithText("1.25x").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.25x").performClick()
        assertTrue(speedSheetTriggered)
    }

    @Test
    fun playerScreen_whenErrorState_displaysErrorBanner() {
        var retryTriggered = false

        composeTestRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Corrupted.mkv",
                        errorMessage = "Unsupported video track",
                        isControlsVisible = true
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
                    onRetryPlayback = { retryTriggered = true }
                )
            }
        }

        // Verify error message is rendered
        composeTestRule.onNodeWithText("Unsupported video track").assertIsDisplayed()

        // Verify Retry button exists and triggers callback
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(retryTriggered)
    }
}
