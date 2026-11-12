package org.aptura.player.ui.settings

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit DATA-09 and SEC-11 UI tests:
 * Verifies "Watch history" clear flow and "Screen security" toggle in Settings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsSecurityAndHistoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val emptyStats = LibraryStatistics(0, 0L, 0L)

    @Test
    fun watchHistoryRow_showsDialog_andConfirmCallsCallback() {
        var clearHistoryCalled = false

        composeRule.setContent {
            SettingsScreen(
                settings = ApturaSettings(),
                libraryStats = emptyStats,
                onBackClick = {},
                onUpdateSpeed = {},
                onUpdateResizeMode = {},
                onUpdateSeekDelta = {},
                onUpdateBackgroundAudio = {},
                onUpdateAutoAdvanceCountdown = {},
                onRescanClick = {},
                onReplayOnboardingClick = {},
                onClearPlaybackHistory = { clearHistoryCalled = true }
            )
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Watch history"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Watch history").assertExists()

        composeRule.onNodeWithText("Clear watch history?").assertDoesNotExist()

        // Click Clear button on Watch history
        val clearButtons = composeRule.onAllNodes(hasText("Clear"))
        clearButtons[clearButtons.fetchSemanticsNodes().size - 1].performClick()
        composeRule.waitForIdle()

        // Dialog appears
        composeRule.onNodeWithText("Clear watch history?").assertExists()
        composeRule.onNodeWithText("This resets playback progress across all videos and empties Continue Watching. Your video files and bookmarks are not affected.").assertExists()

        // Confirm clear inside the dialog
        composeRule.onNode(hasAnyAncestor(isDialog()) and hasText("Clear")).performClick()
        composeRule.waitForIdle()
        assertTrue("onClearPlaybackHistory should be called", clearHistoryCalled)
    }

    @Test
    fun screenSecurityRow_showsStateAndCallsUpdatePrivateMode() {
        var updatedPrivateMode: Boolean? = null

        composeRule.setContent {
            SettingsScreen(
                settings = ApturaSettings(isPrivateMode = false),
                libraryStats = emptyStats,
                onBackClick = {},
                onUpdateSpeed = {},
                onUpdateResizeMode = {},
                onUpdateSeekDelta = {},
                onUpdateBackgroundAudio = {},
                onUpdateAutoAdvanceCountdown = {},
                onRescanClick = {},
                onReplayOnboardingClick = {},
                onUpdatePrivateMode = { updatedPrivateMode = it }
            )
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Screen security"))
        composeRule.waitForIdle()
        val securityRow = composeRule.onNodeWithText("Screen security")
        securityRow.assertExists()
        composeRule.onNodeWithText("Block screenshots and hide app preview in Recent Apps").assertExists()

        // Tap toggle row
        securityRow.performClick()
        composeRule.waitForIdle()
        assertEquals(true, updatedPrivateMode)
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
