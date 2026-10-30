package org.aptura.player.ui.settings

import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaPalettes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ApturaTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for audit P1-01: the selected option of every Settings option group was ~1.8:1 contrast because
 * the typography styles hard-coded a text colour that overrides the component's own selected-label colour.
 * This renders the REAL SettingsScreen and reads the colour Compose actually resolved for the text.
 *
 * The selected option sits on a solid AccentPrimary thumb that slides between options.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsChipContrastTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun resolvedTextColor(text: String): Color {
        scrollTo(text)
        val node = composeRule.onAllNodes(hasText(text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .first { it.config.contains(SemanticsActions.GetTextLayoutResult) }
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().layoutInput.style.color
    }

    private fun showSettings(settings: ApturaSettings = ApturaSettings(), onSpeed: (Float) -> Unit = {}) {
        composeRule.setContent {
            ApturaTheme {
                SettingsScreen(
                    settings = settings, // defaults: 1.0x speed, Auto decoder selected
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

    @Test
    fun typographyStyles_carryNoBakedInColor() {
        val styles = listOf(
            ApturaTypography.displayLarge,
            ApturaTypography.headlineMedium,
            ApturaTypography.titleMedium,
            ApturaTypography.bodyMedium,
            ApturaTypography.labelSmall
        )
        styles.forEach { assertEquals(Color.Unspecified, it.color) }
    }

    @Test
    fun selectedSettingsOptions_areReadable_onTheThumb() {
        showSettings()

        listOf("Auto", "Fit").forEach { label ->
            val color = resolvedTextColor(label)
            val ratio = contrast(color, ApturaPalettes.dark(AccentColor.CYAN).accentPrimary)
            assertTrue(
                "selected option \"$label\" label contrast on the thumb is $ratio (< 4.5:1); colour=$color",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun unselectedSettingsOptions_areReadable_onTheRecessedTrack() {
        showSettings()

        val track = Color(0xFF0A0B0D) // the track is near-black: SurfacePureBlack at 50% over a panel
        listOf("Fill", "Hardware").forEach { label ->
            val ratio = contrast(resolvedTextColor(label), track)
            assertTrue("unselected \"$label\" contrast is $ratio (< 4.5:1)", ratio >= 4.5)
        }
    }

    @Test
    fun theSelectedOption_isAnnouncedAsASelectedRadioButton_andTheOthersAreNot() {
        showSettings()
        scrollTo("Fit")

        composeRule.onNode(hasText("Fit") and androidx.compose.ui.test.isSelectable()).assertIsSelected()
        composeRule.onNode(hasText("Fill") and androidx.compose.ui.test.isSelectable()).assertIsNotSelected()
        val node = composeRule.onNode(hasText("Fit") and androidx.compose.ui.test.isSelectable()).fetchSemanticsNode()
        assertEquals(Role.RadioButton, node.config[SemanticsProperties.Role])
    }
}
