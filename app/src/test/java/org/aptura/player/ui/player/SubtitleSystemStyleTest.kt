package org.aptura.player.ui.player

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        composeRule.onNodeWithText("Follow Android accessibility settings").assertExists()

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

    @Test
    fun subtitleSheet_rendersEncodingPicker_andSelectingEncodingCallsCallback() {
        var selectedEncoding: String? = null

        composeRule.setContent {
            ApturaTheme {
                SubtitleSheet(
                    uiState = PlayerUiState(
                        activeSheet = PlayerSheet.Subtitle,
                        subtitleEncoding = null
                    ),
                    onDismissRequest = {},
                    onSubtitleTrackSelected = {},
                    onSubtitleDelayChanged = {},
                    onImportSubtitleClick = {},
                    onSubtitleFontSizeChanged = {},
                    onSubtitleTextColorChanged = {},
                    onSubtitleBackgroundChanged = {},
                    onSubtitleUseSystemStyleChanged = {},
                    onSubtitleEncodingChanged = { selectedEncoding = it },
                    initialTab = 2
                )
            }
        }

        composeRule.waitForIdle()

        // Verify encoding section and options exist
        composeRule.onNodeWithText("Character set").assertExists()
        composeRule.onNodeWithText("Auto").assertExists()
        composeRule.onNodeWithText("Arabic").assertExists()

        // Select Arabic encoding
        composeRule.onNodeWithText("Arabic").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals("windows-1256", selectedEncoding)
    }

    @Test
    fun subtitleSheet_whenSystemStyleEnabled_displaysCustomStyleDisabledNotice() {
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
                    initialTab = 2
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Custom font size and color are disabled while following system caption settings.").assertExists()
    }

    @Test
    fun subtitleSheet_tracksTab_unsupportedTrackIsDisabledAndShowsBadge() {
        var selectedTrackIndex: Int? = null

        composeRule.setContent {
            ApturaTheme {
                SubtitleSheet(
                    uiState = PlayerUiState(
                        activeSheet = PlayerSheet.Subtitle,
                        subtitleTracks = listOf(
                            TrackInfo(id = "1", index = 0, label = "English (SRT)", isSupported = true),
                            TrackInfo(id = "2", index = 1, label = "PGS Graphic Subtitles", isSupported = false)
                        )
                    ),
                    onDismissRequest = {},
                    onSubtitleTrackSelected = { selectedTrackIndex = it },
                    onSubtitleDelayChanged = {},
                    onImportSubtitleClick = {},
                    onSubtitleFontSizeChanged = {},
                    onSubtitleTextColorChanged = {},
                    onSubtitleBackgroundChanged = {},
                    initialTab = 0
                )
            }
        }

        composeRule.waitForIdle()

        // Verify unsupported message and badge
        composeRule.onNodeWithText("Not supported on this device").assertExists()
        composeRule.onNodeWithText("UNSUPPORTED").assertExists()

        // Attempt to click unsupported track
        composeRule.onNodeWithText("PGS Graphic Subtitles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        // Should not select track index 1
        assertNull(selectedTrackIndex)

        // Click supported track
        composeRule.onNodeWithText("English (SRT)").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(0, selectedTrackIndex)
    }
}
