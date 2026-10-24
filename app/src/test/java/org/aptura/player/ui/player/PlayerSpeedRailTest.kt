package org.aptura.player.ui.player

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The advanced sheet used to repeat the old two-row speed grid even though Settings had moved to the ramp
 * (audit 2026-10-24). Both surfaces now share the same control, so the same speed is one drag away wherever it is used.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerSpeedRailTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showSheet(speed: Float = 1.0f, onSpeed: (Float) -> Unit = {}) {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Inception",
                        durationMs = 120_000L,
                        isPlaying = true,
                        isControlsVisible = true,
                        activeSheet = PlayerSheet.Advanced,
                        playbackSpeed = speed
                    ),
                    player = null,
                    onBackClick = {}, onPlayPauseClick = {}, onSeekRelative = {}, onScrubStart = {}, onScrubMove = {},
                    onScrubEnd = {}, onControlsVisibilityToggle = {}, onLockToggle = {}, onSpeedSelected = onSpeed,
                    onResizeModeToggle = {}, onSpeedSheetVisible = {}, onAudioSheetVisible = {}, onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {}, onSubtitleTrackSelected = {}, onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {}, onRetryPlayback = {}
                )
            }
        }
    }

    @Test
    fun theAdvancedSheet_offersTheSameRampAsSettings() {
        showSheet()

        composeRule.onNodeWithTag("speed_rail").assertExists()
    }

    @Test
    fun theRamp_reportsTheChosenSpeedToThePlayer() {
        var chosen: Float? = null
        showSheet(onSpeed = { chosen = it })

        composeRule.onNodeWithTag("speed_rail").performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }

        assertEquals(2.0f, chosen)
    }
}
