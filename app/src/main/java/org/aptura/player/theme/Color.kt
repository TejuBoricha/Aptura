package org.aptura.player.theme

import androidx.compose.ui.graphics.Color

/**
 * Aptura Color Palette & Design Tokens
 * Source of truth: DESIGN.md & spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md
 */
object ApturaColors {
    // Surfaces & Backgrounds (Dark Mode Baseline)
    val SurfacePureBlack = Color(0xFF000000)
    val SurfaceBase = Color(0xFF0D0E11)
    val SurfaceElevated1 = Color(0xFF16181D)
    val SurfaceElevated2 = Color(0xFF20232A)
    val SurfaceElevated3 = Color(0xFF2B2F38)
    val SurfaceScrim = Color(0xCC000000)
    val SurfaceGlass = Color(0xB316181D)

    // Brand & Functional Accents
    val AccentPrimary = Color(0xFF00E5FF)       // Aptura Electric Cyan
    val AccentSecondary = Color(0xFF00B4D8)     // Deep Teal
    val AccentSubtle = Color(0x2600E5FF)        // 15% Cyan fill

    // Semantic State Colors
    val StatusSuccess = Color(0xFF00E676)
    val StatusWarning = Color(0xFFFFD600)
    val StatusError = Color(0xFFFF5252)
    val StatusInfo = Color(0xFF40C4FF)

    // Typography Color Scale
    val TextHighEmphasis = Color(0xFFF0F4F8)
    val TextMediumEmphasis = Color(0xFF9BA3AF)
    val TextLowEmphasis = Color(0xFF606978)
    val TextOnAccent = Color(0xFF001217)
}
