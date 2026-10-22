package org.aptura.player.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Regression for audit P1-01: the selected option of every Settings chip group was ~1.8:1 contrast because
 * the typography styles hard-coded a text colour that overrides FilterChip's `selectedLabelColor`.
 * This renders the REAL SettingsScreen and reads the colour Compose actually resolved for the text.
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

    private fun resolvedTextColor(text: String): Color {
        val node = composeRule.onAllNodes(hasText(text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .first { it.config.contains(SemanticsActions.GetTextLayoutResult) }
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().layoutInput.style.color
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
    fun selectedSettingsChips_areReadable_onTheAccentBackground() {
        composeRule.setContent {
            ApturaTheme {
                SettingsScreen(
                    settings = ApturaSettings(), // defaults: 1.0x speed, Auto decoder selected
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

        val selectedLabels = listOf("1.0x", "Auto")
        selectedLabels.forEach { label ->
            val color = resolvedTextColor(label)
            val ratio = contrast(color, ApturaColors.AccentPrimary)
            assertTrue(
                "selected chip \"$label\" label contrast on accent is $ratio (< 4.5:1); colour=$color",
                ratio >= 4.5
            )
        }
    }
}
