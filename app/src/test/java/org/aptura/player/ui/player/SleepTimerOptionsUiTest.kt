package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.aptura.player.theme.ApturaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sleep timer's visible choices. The custom field's bounds and the dialog's wiring are covered by
 * `SleepTimerOptionsTest`; a Compose test that opens the dialog and types cannot settle under Robolectric because the
 * focused text field's cursor keeps animating, so that interaction is left to the device pass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1700dp-xhdpi")
class SleepTimerOptionsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showTab() {
        composeRule.setContent {
            ApturaTheme {
                PlaybackToolsTab(
                    uiState = PlayerUiState(),
                    onAudioOnlyChanged = {},
                    onSpeedSelected = {},
                    onStartSleepTimer = {},
                    onCancelSleepTimer = {},
                    onExtendSleepTimer = {},
                    onSleepAtEndOfVideo = {},
                    onSetAbPointA = {},
                    onSetAbPointB = {},
                    onClearAbRepeat = {},
                    onStepFrame = {},
                    onAddBookmark = {},
                    onSeekTo = {},
                    onDeleteBookmark = {},
                    onUpdateBookmarkNote = { _, _ -> },
                    onToggleStatsOverlay = {},
                    onResetVideoPreferences = {}
                )
            }
        }
    }

    @Test
    fun theTenAndTwentyMinutePresets_areOffered() {
        showTab()
        composeRule.onNodeWithText("10 min").assertIsDisplayed()
        composeRule.onNodeWithText("20 min").assertIsDisplayed()
        composeRule.onNodeWithText("Custom…").assertIsDisplayed()
    }
}
