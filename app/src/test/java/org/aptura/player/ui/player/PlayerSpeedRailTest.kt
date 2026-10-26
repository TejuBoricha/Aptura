package org.aptura.player.ui.player

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The advanced sheet used to repeat the old two-row speed grid even though Settings had moved to the ramp, and the
 * quick sheet kept a vertical list of eight rows after that (audit 2026-10-24, F-21). All three surfaces now share
 * SpeedRail, so the same speed is one drag away wherever it is used.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerSpeedRailTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showSheet(
        speed: Float = 1.0f,
        sheet: PlayerSheet = PlayerSheet.Advanced,
        onSpeed: (Float) -> Unit = {}
    ) {
        composeRule.setContent {
            ApturaTheme {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Inception",
                        durationMs = 120_000L,
                        isPlaying = true,
                        isControlsVisible = true,
                        activeSheet = sheet,
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

    @Test
    fun theQuickSheet_isTheSameRamp_notAListOfRows() {
        showSheet(sheet = PlayerSheet.Speed)

        composeRule.onNodeWithTag("speed_rail").assertExists()
        // Eight separate rows were the old design; the ramp is the only control now.
        composeRule.onNodeWithText("Normal (1.0x)").assertDoesNotExist()
    }

    @Test
    fun theQuickSheet_reportsTheChosenSpeedToThePlayer() {
        var chosen: Float? = null
        showSheet(sheet = PlayerSheet.Speed, onSpeed = { chosen = it })

        composeRule.onNodeWithTag("speed_rail").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }

        assertEquals(0.75f, chosen)
    }
}
