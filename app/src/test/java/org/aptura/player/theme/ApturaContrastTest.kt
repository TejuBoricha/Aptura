package org.aptura.player.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG contrast measured on the real palettes and on the surfaces each text level really sits on (A11Y-03, A11Y-04).
 *
 * The old DESIGN.md ratios were computed against white only, so low-emphasis text read "4.6:1" while it was 3.5:1 on
 * the dark base and 2.8:1 on a sheet. Every pair below is a surface the token is drawn on somewhere in the app.
 *
 * - Text needs 4.5:1 (WCAG 1.4.3).
 * - A control's boundary or an icon needs 3:1 (WCAG 1.4.11).
 */
class ApturaContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun ApturaPalette.surfaces(): Map<String, Color> = mapOf(
        "pure black" to surfacePureBlack,
        "base" to surfaceBase,
        "elevated 1" to surfaceElevated1,
        "elevated 2 (sheets)" to surfaceElevated2,
        "elevated 3" to surfaceElevated3
    )

    private fun failures(
        palette: ApturaPalette,
        token: Color,
        needed: Double,
        surfaces: Collection<String>
    ): List<String> = palette.surfaces()
        .filterKeys { it in surfaces }
        .mapNotNull { (name, surface) ->
            val ratio = contrast(token, surface)
            if (ratio < needed) "$name ${"%.2f".format(ratio)}:1" else null
        }

    private fun assertNone(what: String, needed: Double, failures: List<String>) {
        assertTrue("$what is below $needed:1 on ${failures.joinToString()}", failures.isEmpty())
    }

    @Test
    fun darkText_isReadableOnEverySurfaceItSitsOn() {
        val p = ApturaPalettes.dark(AccentColor.CYAN)
        val all = p.surfaces().keys

        assertNone("dark high-emphasis text", 4.5, failures(p, p.textHighEmphasis, 4.5, all))
        assertNone("dark medium-emphasis text", 4.5, failures(p, p.textMediumEmphasis, 4.5, all))
        // Low emphasis is body text on the base, cards and sheets. On the brightest surface (selected rows, chips) it
        // is only used for icons, which need 3:1; text there uses a higher level.
        assertNone(
            "dark low-emphasis text",
            4.5,
            failures(p, p.textLowEmphasis, 4.5, listOf("pure black", "base", "elevated 1", "elevated 2 (sheets)"))
        )
        assertNone("dark low-emphasis icon", 3.0, failures(p, p.textLowEmphasis, 3.0, all))
    }

    @Test
    fun lightText_isReadableOnEverySurfaceItSitsOn() {
        val p = ApturaPalettes.light(AccentColor.CYAN)
        val all = listOf("base", "elevated 1", "elevated 2 (sheets)", "elevated 3")

        assertNone("light high-emphasis text", 4.5, failures(p, p.textHighEmphasis, 4.5, all))
        assertNone("light medium-emphasis text", 4.5, failures(p, p.textMediumEmphasis, 4.5, all))
        assertNone("light low-emphasis text", 4.5, failures(p, p.textLowEmphasis, 4.5, all))
    }

    @Test
    fun theThreeTextLevels_stayDistinctInBothModes() {
        listOf(ApturaPalettes.dark(AccentColor.CYAN), ApturaPalettes.light(AccentColor.CYAN)).forEach { p ->
            val mode = if (p.isDark) "dark" else "light"
            val high = contrast(p.textHighEmphasis, p.surfaceBase)
            val medium = contrast(p.textMediumEmphasis, p.surfaceBase)
            val low = contrast(p.textLowEmphasis, p.surfaceBase)
            assertTrue("$mode: high ($high) must stand out from medium ($medium)", high > medium)
            assertTrue("$mode: medium ($medium) must stand out from low ($low)", medium > low)
            // Lifting low emphasis for AA must not erase the hierarchy: keep a visible step below medium.
            assertTrue("$mode: low ($low) is too close to medium ($medium)", medium - low >= 0.5)
        }
    }

    @Test
    fun darkSeekBarTrack_isVisibleOnTheSurfacesThePlayerDrawsItOn() {
        val p = ApturaPalettes.dark(AccentColor.CYAN)
        assertNone(
            "dark unfilled seek track",
            3.0,
            failures(p, p.controlTrack, 3.0, listOf("pure black", "base", "elevated 1"))
        )
    }

    @Test
    fun lightSeekBarTrack_isVisibleOnEveryLightSurface() {
        val p = ApturaPalettes.light(AccentColor.CYAN)
        assertNone(
            "light unfilled seek track",
            3.0,
            failures(p, p.controlTrack, 3.0, listOf("base", "elevated 1", "elevated 2 (sheets)", "elevated 3"))
        )
    }
}
