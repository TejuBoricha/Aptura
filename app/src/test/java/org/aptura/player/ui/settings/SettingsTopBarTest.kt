package org.aptura.player.ui.settings

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for the compact Settings bar (audit 2026-10-24): its background faded to transparent inside its own
 * height, so the rows scrolling underneath showed through the back button and title. The bar must be an opaque
 * strip that covers the full control row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show() {
        composeRule.setContent {
            ApturaTheme {
                SettingsScreen(
                    settings = ApturaSettings(),
                    libraryStats = LibraryStatistics(),
                    onBackClick = {},
                    onUpdateSpeed = {},
                    onUpdateResizeMode = {},
                    onUpdateSeekDelta = {},
                    onUpdateBackgroundAudio = {},
                    onUpdateAutoAdvanceCountdown = {},
                    onRescanClick = {},
                    onReplayOnboardingClick = {}
                )
            }
        }
    }

    @Test
    fun theCompactBarBackground_isAnOpaqueFill() {
        val solid = SettingsTopBarBackground as? SolidColor
        assertTrue("the compact bar must not use a translucent/gradient scrim", solid != null)
        assertEquals(1f, solid!!.value.alpha, 0f)
        assertEquals(ApturaColors.SurfaceBase, solid.value)
    }

    @Test
    fun theCompactBar_coversTheBackButtonAndTitleRow() {
        show()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Default aspect ratio"))

        val bar = composeRule.onNodeWithTag("settings_compact_bar").getUnclippedBoundsInRoot()
        val back = composeRule.onNodeWithContentDescription("Back").getUnclippedBoundsInRoot()

        composeRule.onNodeWithTag("settings_compact_bar").assertIsDisplayed()
        assertTrue(
            "the bar's bottom (${bar.bottom}) must reach below the back button (${back.bottom})",
            bar.bottom >= back.bottom
        )
        assertTrue(
            "the bar must be at least the 56 dp control row tall, was ${bar.bottom - bar.top}",
            bar.bottom - bar.top >= 56.dp
        )
    }
}
