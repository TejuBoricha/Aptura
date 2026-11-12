package org.aptura.player.ui.player

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Audit A11Y-05: Verifies system caption styling integration and Subtitle Style tab switch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubtitleSystemStyleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun subtitleSheet_rendersSystemCaptionStyleSwitch_andTogglingCallsCallback() {
        var systemStyleToggled: Boolean? = null

        composeRule.setContent {
            ApturaTheme {
                SubtitleSheet(
                    uiState = PlayerUiState(
                        activeSheet = PlayerSheet.Subtitle,
                        subtitleUseSystemStyle = true
                    ),
                    onDismissRequest = {},
                    onSubtitleTrackSelected = {},
                    onSubtitleDelayChanged = {},
                    onImportSubtitleClick = {},
                    onSubtitleFontSizeChanged = {},
                    onSubtitleTextColorChanged = {},
                    onSubtitleBackgroundChanged = {},
                    onSubtitleUseSystemStyleChanged = { systemStyleToggled = it },
                    initialTab = 2
                )
            }
        }

        composeRule.waitForIdle()

        // Verify "Use system caption style" switch and text exists
        composeRule.onNodeWithText("Use system caption style").assertExists()
        composeRule.onNodeWithText("Follow size, colors and background set in Android accessibility settings").assertExists()

        // Toggle the switch via semantics action (Robolectric bottom sheet requirement)
        composeRule.onNodeWithText("Use system caption style").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(false, systemStyleToggled)
    }

    @Test
    fun subtitleSheet_whenSystemStyleDisabled_switchReflectsOffState() {
        var systemStyleToggled: Boolean? = null

        composeRule.setContent {
            ApturaTheme {
                SubtitleSheet(
                    uiState = PlayerUiState(
                        activeSheet = PlayerSheet.Subtitle,
                        subtitleUseSystemStyle = false
                    ),
                    onDismissRequest = {},
                    onSubtitleTrackSelected = {},
                    onSubtitleDelayChanged = {},
                    onImportSubtitleClick = {},
                    onSubtitleFontSizeChanged = {},
                    onSubtitleTextColorChanged = {},
                    onSubtitleBackgroundChanged = {},
                    onSubtitleUseSystemStyleChanged = { systemStyleToggled = it },
                    initialTab = 2
                )
            }
        }

        composeRule.waitForIdle()

        // Verify exists and toggle switch
        composeRule.onNodeWithText("Use system caption style").assertExists()
        composeRule.onNodeWithText("Use system caption style").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(true, systemStyleToggled)
    }
}
