package org.aptura.player.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings on a phone held sideways. The large title (a 64 dp gap, then 36 sp type) took a third of a 360 dp window
 * before the first setting. With no room to spare there is no large title: the compact bar is simply always there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsShortWindowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show() {
        composeRule.setContent {
            ApturaTheme {
                SettingsScreen(
                    settings = ApturaSettings(),
                    libraryStats = LibraryStatistics(totalVideos = 8, totalDurationMs = 33_802_000, totalSizeBytes = 14_400_000_000),
                    onBackClick = {}, onUpdateSpeed = {}, onUpdateResizeMode = {}, onUpdateSeekDelta = {},
                    onUpdateBackgroundAudio = {}, onUpdateAutoAdvanceCountdown = {}, onRescanClick = {},
                    onReplayOnboardingClick = {}
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun settingsTitleNode() = composeRule.onNodeWithText("Settings", useUnmergedTree = true)

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test
    fun aSmallSidewaysPhone_hasNoLargeTitle_andTheFirstSettingIsHigherUp() {
        show()
        val title = settingsTitleNode().getUnclippedBoundsInRoot()
        assertTrue("the title should be in the compact bar at the top, but it is at ${title.top}", title.top.value < 56f)
        // It is the page's heading for TalkBack, since the large title that normally is does not exist here.
        assertTrue(SemanticsProperties.Heading in settingsTitleNode().fetchSemanticsNode().config)

        val readout = composeRule.onNodeWithText("VIDEOS", substring = true).getUnclippedBoundsInRoot()
        assertTrue("the library readout starts at ${readout.top}, too far down for a 360 dp window", readout.top.value < 110f)
        composeRule.onNodeWithText("APPEARANCE").assertIsDisplayed()
    }

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test
    fun aSidewaysPhone_hasNoLargeTitle() {
        show()
        assertTrue(settingsTitleNode().getUnclippedBoundsInRoot().top.value < 56f)
        composeRule.onNodeWithText("APPEARANCE").assertIsDisplayed()
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun anUprightPhone_keepsItsLargeTitle() {
        show()
        val title = settingsTitleNode().getUnclippedBoundsInRoot()
        assertTrue("the large title should sit below the 56 dp bar, but it is at ${title.top}", title.top.value >= 56f)
        assertTrue(SemanticsProperties.Heading in settingsTitleNode().fetchSemanticsNode().config)
        // The compact bar's own title is hidden from TalkBack here, so there is exactly one heading called Settings.
        assertEquals(1, composeRule.onAllNodes(hasText("Settings"), useUnmergedTree = true).fetchSemanticsNodes().size)
    }
}
