package org.aptura.player.ui.settings

import android.app.Application
import android.content.Intent
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
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaPalettes
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Appearance is a real preference: choosing a mode or an accent must report exactly that choice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsAppearanceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        mode: ThemeMode = ThemeMode.DARK,
        accent: AccentColor = AccentColor.CYAN,
        isSupporter: Boolean = false,
        onMode: (ThemeMode) -> Unit = {},
        onAccent: (AccentColor) -> Unit = {},
        onSupporter: (Boolean) -> Unit = {}
    ) {
        composeRule.setContent {
            ApturaTheme(themeMode = mode, accentColor = accent) {
                SettingsScreen(
                    settings = ApturaSettings(themeMode = mode, accentColor = accent, isSupporter = isSupporter),
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
                    onUpdateAccentColor = onAccent,
                    onUpdateSupporter = onSupporter
                )
            }
        }
    }

    @Test
    fun choosingLight_reportsLight() {
        var chosen: ThemeMode? = null
        show(onMode = { chosen = it })

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Theme"))
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

    @Test
    fun choosingGoldWhenSupporter_reportsGold() {
        var chosen: AccentColor? = null
        show(isSupporter = true, onAccent = { chosen = it })

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Gold accent"))
        composeRule.onNodeWithContentDescription("Gold accent")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(AccentColor.GOLD, chosen)
    }

    @Test
    fun togglingSupporter_callsCallback() {
        var toggled: Boolean? = null
        show(isSupporter = false, onSupporter = { toggled = it })

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("I Supported Aptura 🏅"))
        composeRule.onNode(hasText("I Supported Aptura 🏅")).performClick()

        assertEquals(true, toggled)
    }

    @Test
    fun supporterBadge_isDisplayedWhenSupporter() {
        show(isSupporter = true)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("⭐ Supporter"))
        composeRule.onNode(hasText("⭐ Supporter")).assertIsDisplayed()
    }

    @Test
    fun helpAndFeedback_isDisplayed() {
        show()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Help & feedback"))
        composeRule.onNode(hasText("Help & feedback")).assertIsDisplayed()
    }

    @Test
    fun helpAndFeedback_clickLaunchesSendToIntentWithPrefilledQueryAndExtras() {
        show()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Help & feedback"))
        composeRule.onNode(hasText("Help & feedback")).performClick()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val chooserIntent = shadowOf(app).nextStartedActivity
        @Suppress("DEPRECATION")
        val mailIntent = chooserIntent?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: chooserIntent

        val data = mailIntent?.data
        assertEquals("mailto", data?.scheme)
        val mailTo = android.net.MailTo.parse(data.toString())
        assertEquals("tejasboricha225@gmail.com", mailTo.to)
        assertTrue(mailTo.subject?.contains("[Aptura Feedback]") == true)
        assertTrue(mailTo.body?.contains("Diagnostic Info") == true)
        assertTrue(mailIntent?.getStringExtra(Intent.EXTRA_SUBJECT)?.contains("[Aptura Feedback]") == true)
        assertTrue(mailIntent?.getStringExtra(Intent.EXTRA_TEXT)?.contains("Diagnostic Info") == true)
    }
}
