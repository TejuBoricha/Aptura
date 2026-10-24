package org.aptura.player.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The theme wrapper must swap the global tokens before content draws, and keep Material in sync with them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** The palette is process-global by design; leave it dark so tests that do not set a theme stay deterministic. */
    @After
    fun resetPalette() {
        ApturaColors.apply(ApturaPalettes.dark(AccentColor.CYAN))
    }

    @Test
    fun lightMode_appliesTheLightPaletteAndAccent() {
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.LIGHT, accentColor = AccentColor.AMBER) {
                Box(Modifier.size(1.dp))
            }
        }
        composeRule.waitForIdle()

        val expected = ApturaPalettes.light(AccentColor.AMBER)
        assertFalse(ApturaColors.IsDark)
        assertEquals(expected.surfaceBase, ApturaColors.SurfaceBase)
        assertEquals(expected.accentPrimary, ApturaColors.AccentPrimary)
        assertEquals(expected.textHighEmphasis, ApturaColors.TextHighEmphasis)
    }

    @Test
    fun darkMode_appliesTheDarkPalette() {
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.DARK, accentColor = AccentColor.CYAN) {
                Box(Modifier.size(1.dp))
            }
        }
        composeRule.waitForIdle()

        val expected = ApturaPalettes.dark(AccentColor.CYAN)
        assertTrue(ApturaColors.IsDark)
        assertEquals(expected.surfaceBase, ApturaColors.SurfaceBase)
        assertEquals(expected.accentPrimary, ApturaColors.AccentPrimary)
    }

    @Test
    fun changingTheAccent_swapsTheTokens() {
        var mode by mutableStateOf(ThemeMode.DARK)
        var accent by mutableStateOf(AccentColor.CYAN)
        composeRule.setContent {
            ApturaTheme(themeMode = mode, accentColor = accent) {
                Box(Modifier.size(1.dp))
            }
        }
        composeRule.waitForIdle()
        assertEquals(AccentColor.CYAN.darkPrimary, ApturaColors.AccentPrimary)

        accent = AccentColor.ROSE
        composeRule.waitForIdle()

        assertEquals(AccentColor.ROSE.darkPrimary, ApturaColors.AccentPrimary)

        mode = ThemeMode.LIGHT
        composeRule.waitForIdle()

        assertEquals(AccentColor.ROSE.lightPrimary, ApturaColors.AccentPrimary)
    }
}
