package org.aptura.player.ui.settings

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaPalettes
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Appearance is a real preference: choosing a mode or an accent must report exactly that choice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsAppearanceTest {

    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun resetPalette() {
        ApturaColors.apply(ApturaPalettes.dark(AccentColor.CYAN))
    }

    private fun show(
        mode: ThemeMode = ThemeMode.DARK,
        accent: AccentColor = AccentColor.CYAN,
        onMode: (ThemeMode) -> Unit = {},
        onAccent: (AccentColor) -> Unit = {}
    ) {
        composeRule.setContent {
            ApturaTheme(themeMode = mode, accentColor = accent) {
                SettingsScreen(
                    settings = ApturaSettings(themeMode = mode, accentColor = accent),
                    libraryStats = LibraryStatistics(),
                    onBackClick = {},
                    onUpdateSpeed = {},
                    onUpdateResizeMode = {},
                    onUpdateSeekDelta = {},
                    onUpdateBackgroundAudio = {},
                    onUpdateAutoAdvanceCountdown = {},
                    onRescanClick = {},
                    onReplayOnboardingClick = {},
                    onUpdateThemeMode = onMode,
                    onUpdateAccentColor = onAccent
                )
            }
        }
    }

    @Test
    fun choosingLight_reportsLight() {
        var chosen: ThemeMode? = null
        show(onMode = { chosen = it })

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("App Theme"))
        composeRule.onNode(hasText("Light") and isSelectable()).performClick()

        assertEquals(ThemeMode.LIGHT, chosen)
    }

    @Test
    fun choosingAnAccentSwatch_reportsThatAccent() {
        var chosen: AccentColor? = null
        show(onAccent = { chosen = it })

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Violet accent"))
        composeRule.onNodeWithContentDescription("Violet accent")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(AccentColor.VIOLET, chosen)
    }

    @Test
    fun theCurrentAccent_isAnnouncedAsSelected() {
        show(accent = AccentColor.ROSE)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Rose accent"))
        composeRule.onNodeWithContentDescription("Rose accent").assertIsSelected()
    }
}
