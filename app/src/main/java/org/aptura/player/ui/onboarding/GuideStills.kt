package org.aptura.player.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.PI
import kotlin.math.sin

/**
 * Stand-in film frames for the welcome guide, drawn rather than shipped as image files.
 *
 * Before the first scan there are no videos on the device the guide can show, and a flat grey tile reads as a picture
 * that failed to load. These are small landscapes (sky, sun, three ridges) so the guide's library and player mocks look
 * like a library of films. They are deterministic by seed, always drawn at the size they are given, and described to
 * TalkBack as nothing at all: the page's own description says what the picture is.
 */
private class StillPalette(
    val skyTop: Color,
    val skyBottom: Color,
    val sun: Color,
    val ridgeFar: Color,
    val ridgeMid: Color,
    val ridgeNear: Color,
    /** Where the sun sits across the frame, 0 to 1. */
    val sunX: Float,
    /** Where the sun sits down the frame, 0 (top) to 1 (horizon). */
    val sunY: Float
)

private val StillPalettes = listOf(
    // Dusk over a coast.
    StillPalette(Color(0xFF1B1F4B), Color(0xFFFF8A5B), Color(0xFFFFE6B8), Color(0xFF6B3A66), Color(0xFF3A2547), Color(0xFF16122B), 0.68f, 0.52f),
    // Teal dawn.
    StillPalette(Color(0xFF0E3B43), Color(0xFFF2C894), Color(0xFFFFF4D6), Color(0xFF3F7F7A), Color(0xFF1F5A5E), Color(0xFF0B2E35), 0.30f, 0.58f),
    // Deep blue night.
    StillPalette(Color(0xFF070B1F), Color(0xFF2F4E8F), Color(0xFFDCE8FF), Color(0xFF28407A), Color(0xFF16254F), Color(0xFF080F26), 0.78f, 0.22f),
    // Forest morning.
    StillPalette(Color(0xFF2E6F5E), Color(0xFFE5EAC2), Color(0xFFFFF7D0), Color(0xFF4E9174), Color(0xFF2B6650), Color(0xFF123B30), 0.45f, 0.48f),
    // Rose evening.
    StillPalette(Color(0xFF4A1D45), Color(0xFFFFA3B1), Color(0xFFFFE1D2), Color(0xFF9A4A6E), Color(0xFF5E2B55), Color(0xFF2A1230), 0.22f, 0.55f),
    // Desert noon.
    StillPalette(Color(0xFF3C7BB8), Color(0xFFF7D9A8), Color(0xFFFFFBEA), Color(0xFFC98B57), Color(0xFF9A5F3A), Color(0xFF5C341F), 0.60f, 0.30f)
)

internal fun guideStillTag(seed: Int) = "guide_still_${Math.floorMod(seed, StillPalettes.size)}"

/** A drawn film frame. [seed] picks the scene; the same seed is always the same picture. */
@Composable
internal fun GuideStill(seed: Int, modifier: Modifier = Modifier) {
    val palette = StillPalettes[Math.floorMod(seed, StillPalettes.size)]
    Canvas(modifier = modifier.fillMaxSize().testTag(guideStillTag(seed)).clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom)))

        // The sun: a soft glow and a bright core.
        val sun = Offset(w * palette.sunX, h * palette.sunY)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.sun.copy(alpha = 0.55f), Color.Transparent),
                center = sun,
                radius = h * 0.55f
            ),
            radius = h * 0.55f,
            center = sun
        )
        drawCircle(color = palette.sun, radius = h * 0.075f, center = sun)

        // Three ridges, far to near, each a slow wave, so the frame has depth without a single hard edge.
        fun ridge(baseline: Float, amplitude: Float, waves: Float, phase: Float, color: Color) {
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * baseline)
                val steps = 48
                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    val y = h * baseline + h * amplitude * (
                        sin(2.0 * PI * waves * t + phase) * 0.6 + sin(2.0 * PI * waves * 2.3 * t + phase * 1.7) * 0.4
                        ).toFloat()
                    lineTo(w * t, y)
                }
                lineTo(w, h)
                close()
            }
            drawPath(path, color)
        }
        val phase = seed * 1.3f
        ridge(baseline = 0.62f, amplitude = 0.07f, waves = 1.2f, phase = phase, color = palette.ridgeFar)
        ridge(baseline = 0.74f, amplitude = 0.07f, waves = 1.6f, phase = phase + 2f, color = palette.ridgeMid)
        ridge(baseline = 0.88f, amplitude = 0.05f, waves = 2.1f, phase = phase + 4f, color = palette.ridgeNear)

        // A little vignette, as a film frame has.
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                center = Offset(w / 2f, h / 2f),
                radius = maxOf(w, h) * 0.75f
            )
        )
    }
}
