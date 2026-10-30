package org.aptura.player.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings says whether Aptura may list the videos, and is where it is given or taken back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SettingsVideoAccessTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var taps = 0

    private fun show(hasVideoAccess: Boolean) {
        composeRule.setContent {
            ApturaTheme {
                SettingsScreen(
                    settings = ApturaSettings(),
                    libraryStats = LibraryStatistics(),
                    onBackClick = {}, onUpdateSpeed = {}, onUpdateResizeMode = {}, onUpdateSeekDelta = {},
                    onUpdateBackgroundAudio = {}, onUpdateAutoAdvanceCountdown = {}, onRescanClick = {},
                    onReplayOnboardingClick = {},
                    hasVideoAccess = hasVideoAccess,
                    onVideoAccessClick = { taps++ }
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(VIDEO_ACCESS_BUTTON_TAG))
        composeRule.waitForIdle()
    }

    @Test
    fun withAccess_itSaysSo_andOffersToManageIt() {
        show(hasVideoAccess = true)
        composeRule.onNodeWithText("Video access").assertIsDisplayed()
        composeRule.onNodeWithText("Allowed. Aptura can list the videos on this device.").assertIsDisplayed()
        composeRule.onNodeWithText("Manage").assertIsDisplayed()

        composeRule.onNodeWithTag(VIDEO_ACCESS_BUTTON_TAG).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun withoutAccess_itSaysSo_andOffersToAllowIt() {
        show(hasVideoAccess = false)
        composeRule.onNodeWithText("Not allowed. Aptura only sees videos you open or add.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow").assertIsDisplayed()

        composeRule.onNodeWithTag(VIDEO_ACCESS_BUTTON_TAG).performClick()
        assertEquals(1, taps)
    }
}
