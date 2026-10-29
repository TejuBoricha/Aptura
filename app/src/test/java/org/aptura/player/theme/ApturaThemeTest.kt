package org.aptura.player.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The theme wrapper provides the palette to the composables inside it, and keeps Material in sync with it. Tokens are
 * read while composing, so each test records what a composable inside the theme saw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** What a composable inside a theme read from the tokens. */
    private data class Seen(
        val isDark: Boolean,
        val base: Color,
        val accent: Color,
        val textHigh: Color
    )

    @androidx.compose.runtime.Composable
    private fun Probe(onSeen: (Seen) -> Unit) {
        onSeen(
            Seen(
                isDark = ApturaColors.IsDark,
                base = ApturaColors.SurfaceBase,
                accent = ApturaColors.AccentPrimary,
                textHigh = ApturaColors.TextHighEmphasis
            )
        )
        Box(Modifier.size(1.dp))
    }

    @Test
    fun lightMode_providesTheLightPaletteAndAccent() {
        var seen: Seen? = null
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.LIGHT, accentColor = AccentColor.AMBER) { Probe { seen = it } }
        }
        composeRule.waitForIdle()

        val expected = ApturaPalettes.light(AccentColor.AMBER)
        assertFalse(seen!!.isDark)
        assertEquals(expected.surfaceBase, seen!!.base)
        assertEquals(expected.accentPrimary, seen!!.accent)
        assertEquals(expected.textHighEmphasis, seen!!.textHigh)
    }

    @Test
    fun darkMode_providesTheDarkPalette() {
        var seen: Seen? = null
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.DARK, accentColor = AccentColor.CYAN) { Probe { seen = it } }
        }
        composeRule.waitForIdle()

        val expected = ApturaPalettes.dark(AccentColor.CYAN)
        assertTrue(seen!!.isDark)
        assertEquals(expected.surfaceBase, seen!!.base)
        assertEquals(expected.accentPrimary, seen!!.accent)
    }

    @Test
    fun withoutATheme_aComposableStillReadsAReadablePalette() {
        var seen: Seen? = null
        composeRule.setContent { Probe { seen = it } }
        composeRule.waitForIdle()

        assertEquals(ApturaPalettes.dark(AccentColor.CYAN).surfaceBase, seen!!.base)
    }

    @Test
    fun changingTheModeAndAccent_reachesTheContent() {
        var mode by mutableStateOf(ThemeMode.DARK)
        var accent by mutableStateOf(AccentColor.CYAN)
        var seen: Seen? = null
        composeRule.setContent {
            ApturaTheme(themeMode = mode, accentColor = accent) { Probe { seen = it } }
        }
        composeRule.waitForIdle()
        assertEquals(AccentColor.CYAN.darkPrimary, seen!!.accent)

        accent = AccentColor.ROSE
        composeRule.waitForIdle()
        assertEquals(AccentColor.ROSE.darkPrimary, seen!!.accent)

        mode = ThemeMode.LIGHT
        composeRule.waitForIdle()
        assertEquals(AccentColor.ROSE.lightPrimary, seen!!.accent)
    }

    /**
     * The whole point of scoping: a dark theme nested in a light one, as the player is inside the app, must not
     * recolour the light content around it, and the light content must not recolour it.
     */
    @Test
    fun aNestedDarkTheme_andItsLightSurroundings_eachKeepTheirOwnColours() {
        var outer: Seen? = null
        var inner: Seen? = null
        var outerAfter: Seen? = null
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.LIGHT) {
                Probe { outer = it }
                ApturaTheme(themeMode = ThemeMode.DARK) { Probe { inner = it } }
                Probe { outerAfter = it }
            }
        }
        composeRule.waitForIdle()

        val light = ApturaPalettes.light(AccentColor.CYAN)
        val dark = ApturaPalettes.dark(AccentColor.CYAN)
        assertEquals(light.surfaceBase, outer!!.base)
        assertEquals(dark.surfaceBase, inner!!.base)
        assertEquals("content after the nested theme is still the light theme's", light.surfaceBase, outerAfter!!.base)
    }
}
