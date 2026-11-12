package org.aptura.player.ui.player

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.MutableStateFlow
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies PERF-01: high-frequency playback position ticks do not recompose the parent [PlayerScreen]
 * host or gesture layer, while still updating the leaf elapsed-time label.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerRecompositionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun positionTicks_doNotRecomposePlayerScreenHost_whileUpdatingElapsedTimeText() {
        val uiStateFlow = MutableStateFlow(
            PlayerUiState(
                videoTitle = "Sample",
                durationMs = 120_000L,
                isPlaying = true,
                isControlsVisible = true
            )
        )
        val positionFlow = MutableStateFlow(PlayerPositionState(currentPositionMs = 1_000L, bufferedPositionMs = 10_000L))
        var hostCompositionCount = 0

        composeTestRule.setContent {
            ApturaTheme {
                val uiState by uiStateFlow.collectAsState()
                val positionState = positionFlow.collectAsState()
                SideEffect {
                    hostCompositionCount++
                }
                PlayerScreen(
                    uiState = uiState,
                    player = null,
                    onBackClick = {},
                    onPlayPauseClick = {},
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
                    positionProvider = { positionState.value }
                )
            }
        }

        composeTestRule.waitForIdle()
        val initialCompositions = hostCompositionCount
        composeTestRule.onNodeWithText("0:01").assertIsDisplayed()

        // Simulate 10 position ticks (2.5 seconds at 4 Hz)
        for (i in 2..11) {
            positionFlow.value = PlayerPositionState(
                currentPositionMs = i * 1_000L,
                bufferedPositionMs = 20_000L
            )
            composeTestRule.waitForIdle()
        }

        // Host composable must not recompose on position ticks
        assertEquals(initialCompositions, hostCompositionCount)
        composeTestRule.onNodeWithText("0:11").assertIsDisplayed()
    }
}
