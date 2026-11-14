package org.aptura.player.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * DESIGN.md is the file every agent reads before it touches the UI, and its colour tables had drifted from the code: the
 * dark seek-track colour, the Amber accent and a whole sixth accent (Gold) were wrong or missing. Like the privacy
 * policy and the store text, the written promise is now checked against what ships: every hex value in the colour
 * tables must equal the colour in the real palette, and every palette token and accent must be documented.
 *
 * Contrast ratios in the prose are not compared here: [ApturaContrastTest] enforces the thresholds on the real colours.
 */
class DesignTokensDocTest {

    private val designLines: List<String> by lazy {
        val file = listOf("DESIGN.md", "../DESIGN.md").map(::File).firstOrNull { it.isFile }
        assertTrue("DESIGN.md was not found from ${File(".").absolutePath}", file != null)
        file!!.readLines(Charsets.UTF_8)
    }

    /** The lines of the `###` section whose heading starts with [headingStart], up to the next heading. */
    private fun section(headingStart: String): List<String> {
        val start = designLines.indexOfFirst { it.startsWith(headingStart) }
        assertTrue("DESIGN.md has no section starting with \"$headingStart\"", start >= 0)
        val rest = designLines.drop(start + 1)
        return rest.takeWhile { !it.startsWith("#") }
    }

    /** DESIGN.md writes `#RRGGBB` or `#RRGGBBAA`; Compose stores ARGB, so the alpha moves to the front. */
    private fun argbOf(hex: String): Int {
        val rgb = hex.take(6).toLong(16).toInt()
        val alpha = if (hex.length == 8) hex.substring(6, 8).toInt(16) else 0xFF
        return (alpha shl 24) or rgb
    }

    private fun hex(argb: Int): String = "#%08X".format(argb)

    private fun ApturaPalette.tokenArgb(token: String): Int? = when (token) {
        "SurfacePureBlack" -> surfacePureBlack
        "SurfaceBase" -> surfaceBase
        "SurfaceElevated1" -> surfaceElevated1
        "SurfaceElevated2" -> surfaceElevated2
        "SurfaceElevated3" -> surfaceElevated3
        "SurfaceRecessed" -> surfaceRecessed
        "SurfaceScrim" -> surfaceScrim
        "SurfaceGlass" -> surfaceGlass
        "ControlTrack" -> controlTrack
        "BorderSubtle" -> borderSubtle
        "StatusSuccess" -> statusSuccess
        "StatusWarning" -> statusWarning
        "StatusError" -> statusError
        "StatusInfo" -> statusInfo
        "TextHighEmphasis" -> textHighEmphasis
        "TextMediumEmphasis" -> textMediumEmphasis
        "TextLowEmphasis" -> textLowEmphasis
        "TextOnAccent" -> textOnAccent
        "TextOnScrim" -> textOnScrim
        else -> null
    }?.toArgb()

    private val tokenRow = Regex("""^\|\s*`([A-Za-z0-9]+)`\s*\|\s*`#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})`\s*\|""")

    /**
     * Compares one `Token | Hex` table with [palette]: a documented value must equal the shipped one, a documented name
     * must be a real token, and every name in [required] must be documented.
     */
    private fun problemsIn(headingStart: String, palette: ApturaPalette, required: List<String>): List<String> {
        val rows = section(headingStart).mapNotNull { tokenRow.find(it) }
            .associate { it.groupValues[1] to argbOf(it.groupValues[2]) }
        val problems = mutableListOf<String>()
        rows.forEach { (token, documented) ->
            val shipped = palette.tokenArgb(token)
            when {
                shipped == null -> problems += "$headingStart: \"$token\" is not a palette token"
                shipped != documented ->
                    problems += "$headingStart: $token is ${hex(documented)} in DESIGN.md but ${hex(shipped)} in the code"
            }
        }
        required.filter { it !in rows }.forEach { problems += "$headingStart: $it is not documented" }
        return problems
    }

    private fun assertNoProblems(problems: List<String>) =
        assertTrue("DESIGN.md disagrees with the shipped palette:\n" + problems.joinToString("\n") { " - $it" }, problems.isEmpty())

    private val darkSurfaces = listOf(
        "SurfacePureBlack", "SurfaceBase", "SurfaceElevated1", "SurfaceElevated2", "SurfaceElevated3",
        "SurfaceRecessed", "SurfaceScrim", "SurfaceGlass", "ControlTrack"
    )
    private val statuses = listOf("StatusSuccess", "StatusWarning", "StatusError", "StatusInfo")
    private val textLevels = listOf("TextHighEmphasis", "TextMediumEmphasis", "TextLowEmphasis", "TextOnAccent", "TextOnScrim")

    @Test
    fun darkSurfaceTable_matchesTheShippedDarkPalette() {
        assertNoProblems(problemsIn("### 2.1 ", ApturaPalettes.dark(AccentColor.CYAN), darkSurfaces))
    }

    @Test
    fun lightPaletteTable_matchesTheShippedLightPalette() {
        assertNoProblems(
            problemsIn("### 2.1b ", ApturaPalettes.light(AccentColor.CYAN), darkSurfaces + "BorderSubtle" + textLevels + statuses)
        )
    }

    @Test
    fun darkStatusColours_matchTheShippedDarkPalette() {
        assertNoProblems(problemsIn("### 2.3 ", ApturaPalettes.dark(AccentColor.CYAN), statuses))
    }

    @Test
    fun darkTextScale_matchesTheShippedDarkPalette() {
        assertNoProblems(problemsIn("### 2.4 ", ApturaPalettes.dark(AccentColor.CYAN), textLevels))
    }

    @Test
    fun accentTable_listsEveryAccentWithItsShippedPrimaries() {
        val row = Regex("""^\|\s*([A-Za-z]+)(?: \(default\))?\s*\|\s*`#([0-9A-Fa-f]{6})`\s*\|\s*`#([0-9A-Fa-f]{6})`\s*\|""")
        val rows = section("### 2.2 ").mapNotNull { row.find(it) }.associateBy { it.groupValues[1] }
        val problems = mutableListOf<String>()
        AccentColor.entries.forEach { accent ->
            val documented = rows[accent.label]
            if (documented == null) {
                problems += "accent ${accent.label} is in the code but not in the accent table"
                return@forEach
            }
            val dark = argbOf(documented.groupValues[2])
            val light = argbOf(documented.groupValues[3])
            if (dark != accent.previewPrimary(true).toArgb()) {
                problems += "${accent.label} dark primary is ${hex(dark)} in DESIGN.md but ${hex(accent.previewPrimary(true).toArgb())} in the code"
            }
            if (light != accent.previewPrimary(false).toArgb()) {
                problems += "${accent.label} light primary is ${hex(light)} in DESIGN.md but ${hex(accent.previewPrimary(false).toArgb())} in the code"
            }
        }
        rows.keys.filter { name -> AccentColor.entries.none { it.label == name } }
            .forEach { name -> problems += "accent $name is in DESIGN.md but not in the code" }
        assertNoProblems(problems)
    }

    @Test
    fun theIntroSentence_namesTheSameAccentsAsTheCode() {
        val sentence = Regex("""one of (\w+) accents: ([A-Za-z, ]+)""")
            .find(designLines.joinToString("\n"))
        assertTrue("DESIGN.md section 2 has no sentence of the form \"one of N accents: A, B, ...\"", sentence != null)
        val named = sentence!!.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val shipped = AccentColor.entries.map { it.label }.toSet()
        assertTrue("DESIGN.md names accents $named but the code has $shipped", named == shipped)
    }
}
