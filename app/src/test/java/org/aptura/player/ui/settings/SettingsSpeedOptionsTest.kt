package org.aptura.player.ui.settings

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import org.aptura.player.core.playback.PlaybackSpeedOptions
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for the Settings/player mismatch (audit 2026-10-24): every speed the player can use must be selectable
 * as the default, including the slow speeds and 1.75x. The ramp exposes one slider node rather than eight cards, so
 * the tests drive the real semantics action and the real touch path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsSpeedOptionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(defaultSpeed: Float = 1.0f, onSpeed: (Float) -> Unit = {}) {
        composeRule.setContent {
            ApturaTheme(themeMode = org.aptura.player.theme.ThemeMode.DARK) {
                SettingsScreen(
                    settings = ApturaSettings(defaultPlaybackSpeed = defaultSpeed),
                    libraryStats = LibraryStatistics(),
                    onBackClick = {},
                    onUpdateSpeed = onSpeed,
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

    private fun scrollToRail() {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("speed_rail"))
    }

    @Test
    fun everyPlayerSpeed_canBeReachedFromTheRail() {
        var chosen: Float? = null
        show(onSpeed = { chosen = it })
        scrollToRail()

        PlaybackSpeedOptions.all.forEachIndexed { index, speed ->
            composeRule.onNodeWithTag("speed_rail")
                .performSemanticsAction(SemanticsActions.SetProgress) { it(index.toFloat()) }
            assertEquals(speed, chosen)
        }
    }

    @Test
    fun choosingAHalfSpeed_reportsHalfSpeed() {
        var chosen: Float? = null
        show(onSpeed = { chosen = it })
        scrollToRail()

        composeRule.onNodeWithTag("speed_rail")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }

        assertEquals(0.5f, chosen)
    }

    @Test
    fun tappingTheFarRightOfTheRail_reportsDoubleSpeed() {
        var chosen: Float? = null
        show(onSpeed = { chosen = it })
        scrollToRail()

        composeRule.onNodeWithTag("speed_rail").performTouchInput {
            click(Offset(width * 0.98f, height / 2f))
        }

        assertEquals(2.0f, chosen)
    }

    @Test
    fun theStoredDefault_isTheAnnouncedValue() {
        show(defaultSpeed = 1.75f)
        scrollToRail()

        composeRule.onNodeWithTag("speed_rail").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1.75x")
        )
    }

    @Test
    fun anUnknownStoredSpeed_snapsToTheNearestSpeed_andAnnouncesIt() {
        show(defaultSpeed = 1.1f)
        scrollToRail()

        composeRule.onNodeWithTag("speed_rail").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1.0x")
        )
    }

    @Test
    fun theRail_isStillReachableWhenScrolled() {
        show()
        scrollToRail()
        composeRule.onNodeWithTag("speed_rail").assertIsDisplayed()
    }
}
