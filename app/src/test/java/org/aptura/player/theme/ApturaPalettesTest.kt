package org.aptura.player.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Colour is a contract, not a preference: every palette must keep text readable and every accent must work in both
 * modes. These tests fail if a future tweak makes a label unreadable or reuses an accent colour by accident.
 */
class ApturaPalettesTest {

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test
    fun darkPalette_keepsTheOriginalCinematicTokens() {
        val palette = ApturaPalettes.dark(AccentColor.CYAN)

        assertTrue(palette.isDark)
        assertEquals(Color(0xFF0D0E11), palette.surfaceBase)
        assertEquals(Color(0xFF00E5FF), palette.accentPrimary)
        assertEquals(Color(0xFFF0F4F8), palette.textHighEmphasis)
    }

    @Test
    fun lightPalette_hasBrightSurfacesAndDarkText() {
        val palette = ApturaPalettes.light(AccentColor.CYAN)

        assertTrue(!palette.isDark)
        assertEquals(Color(0xFFFFFFFF), palette.surfacePureBlack)
        assertTrue("light base should be bright", palette.surfaceBase.luminance() > 0.8f)
        assertTrue(
            "high-emphasis text is not readable on the light base",
            contrast(palette.textHighEmphasis, palette.surfaceBase) >= 7.0
        )
        assertTrue(
            "medium-emphasis text is not readable on a light panel",
            contrast(palette.textMediumEmphasis, palette.surfaceElevated1) >= 4.5
        )
        assertTrue(
            "low-emphasis text is not readable on a light panel",
            contrast(palette.textLowEmphasis, palette.surfaceElevated1) >= 4.5
        )
    }

    @Test
    fun everyAccent_isReadableInBothModes() {
        AccentColor.entries.forEach { accent ->
            listOf(true, false).forEach { dark ->
                val palette = ApturaPalettes.forMode(dark, accent)
                val mode = if (dark) "dark" else "light"

                assertTrue(
                    "${accent.label} label contrast on its own thumb in $mode is " +
                        contrast(palette.textOnAccent, palette.accentPrimary),
                    contrast(palette.textOnAccent, palette.accentPrimary) >= 4.5
                )
                assertTrue(
                    "${accent.label} accent contrast against the $mode background is " +
                        contrast(palette.accentPrimary, palette.surfaceBase),
                    contrast(palette.accentPrimary, palette.surfaceBase) >= 3.0
                )
            }
        }
    }

    @Test
    fun accents_areDistinctWithinAMode() {
        val dark = AccentColor.entries.map { ApturaPalettes.dark(it).accentPrimary }
        val light = AccentColor.entries.map { ApturaPalettes.light(it).accentPrimary }

        assertEquals("dark accents must be unique", dark.size, dark.distinct().size)
        assertEquals("light accents must be unique", light.size, light.distinct().size)
        assertNotEquals(
            "a light accent must not reuse the dark variant",
            ApturaPalettes.dark(AccentColor.CYAN).accentPrimary,
            ApturaPalettes.light(AccentColor.CYAN).accentPrimary
        )
    }

    @Test
    fun lightStatusColors_areReadableOnWhite() {
        val palette = ApturaPalettes.light(AccentColor.CYAN)
        listOf(
            "success" to palette.statusSuccess,
            "warning" to palette.statusWarning,
            "error" to palette.statusError,
            "info" to palette.statusInfo
        ).forEach { (name, colour) ->
            val ratio = contrast(colour, palette.surfaceElevated1)
            assertTrue("$name status colour contrast is $ratio (< 4.5:1)", ratio >= 4.5)
        }
    }
}
