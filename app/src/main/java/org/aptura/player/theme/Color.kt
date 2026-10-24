package org.aptura.player.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Every colour the UI is allowed to use, in one palette. Dark and light palettes share this shape so a token always
 * means the same thing, whichever mode is on screen.
 */
@Immutable
data class ApturaPalette(
    val isDark: Boolean,
    val surfacePureBlack: Color,
    val surfaceBase: Color,
    val surfaceElevated1: Color,
    val surfaceElevated2: Color,
    val surfaceElevated3: Color,
    val surfaceRecessed: Color,
    val surfaceScrim: Color,
    val surfaceGlass: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentSubtle: Color,
    val borderSubtle: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val statusInfo: Color,
    val textHighEmphasis: Color,
    val textMediumEmphasis: Color,
    val textLowEmphasis: Color,
    val textOnAccent: Color,
    val textOnScrim: Color
)

/**
 * Aptura Color Palette & Design Tokens
 * Source of truth: DESIGN.md & spec/06_UX_SCREEN_AND_STATE_SPECIFICATION.md
 *
 * The tokens are state-backed rather than constants: the whole app reads them by name (~900 call sites), so
 * `ApturaTheme` swaps the active palette instead of every screen taking colours as parameters. Reading a token in a
 * composable subscribes that composable to palette changes, exactly like any other Compose state.
 */
object ApturaColors {
    private var activePalette by mutableStateOf(ApturaPalettes.dark(AccentColor.CYAN))

    /** Applies a palette before the themed content composes, so a mode switch never shows the old colours. */
    internal fun apply(palette: ApturaPalette) {
        if (activePalette != palette) activePalette = palette
    }

    /** Whether the palette on screen is a dark one. Used by widgets that draw raw accent swatches. */
    val IsDark: Boolean get() = activePalette.isDark

    // Surfaces & Backgrounds
    val SurfacePureBlack: Color get() = activePalette.surfacePureBlack
    val SurfaceBase: Color get() = activePalette.surfaceBase
    val SurfaceElevated1: Color get() = activePalette.surfaceElevated1
    val SurfaceElevated2: Color get() = activePalette.surfaceElevated2
    val SurfaceElevated3: Color get() = activePalette.surfaceElevated3

    /** A recessed track inside a panel (selector switches, folder chips). */
    val SurfaceRecessed: Color get() = activePalette.surfaceRecessed
    val SurfaceScrim: Color get() = activePalette.surfaceScrim
    val SurfaceGlass: Color get() = activePalette.surfaceGlass

    // Brand & Functional Accents
    val AccentPrimary: Color get() = activePalette.accentPrimary
    val AccentSecondary: Color get() = activePalette.accentSecondary
    val AccentSubtle: Color get() = activePalette.accentSubtle
    val BorderSubtle: Color get() = activePalette.borderSubtle

    // Semantic State Colors
    val StatusSuccess: Color get() = activePalette.statusSuccess
    val StatusWarning: Color get() = activePalette.statusWarning
    val StatusError: Color get() = activePalette.statusError
    val StatusInfo: Color get() = activePalette.statusInfo

    // Typography Color Scale
    val TextHighEmphasis: Color get() = activePalette.textHighEmphasis
    val TextMediumEmphasis: Color get() = activePalette.textMediumEmphasis
    val TextLowEmphasis: Color get() = activePalette.textLowEmphasis
    val TextOnAccent: Color get() = activePalette.textOnAccent

    /** Text and icons that sit on a dark scrim over an image, in either mode. */
    val TextOnScrim: Color get() = activePalette.textOnScrim
}

/** The concrete palettes. Dark keeps the original cinematic tokens exactly; light is its daylight counterpart. */
internal object ApturaPalettes {

    fun dark(accent: AccentColor): ApturaPalette = ApturaPalette(
        isDark = true,
        surfacePureBlack = Color(0xFF000000),
        surfaceBase = Color(0xFF0D0E11),
        surfaceElevated1 = Color(0xFF16181D),
        surfaceElevated2 = Color(0xFF20232A),
        surfaceElevated3 = Color(0xFF2B2F38),
        surfaceRecessed = Color(0x80000000),
        surfaceScrim = Color(0xCC000000),
        surfaceGlass = Color(0xB316181D),
        accentPrimary = accent.darkPrimary,
        accentSecondary = accent.darkSecondary,
        accentSubtle = accent.darkPrimary.copy(alpha = 0.15f),
        borderSubtle = Color(0x26FFFFFF),
        statusSuccess = Color(0xFF00E676),
        statusWarning = Color(0xFFFFD600),
        statusError = Color(0xFFFF5252),
        statusInfo = Color(0xFF40C4FF),
        textHighEmphasis = Color(0xFFF0F4F8),
        textMediumEmphasis = Color(0xFF9BA3AF),
        textLowEmphasis = Color(0xFF606978),
        textOnAccent = Color(0xFF001217),
        textOnScrim = Color(0xFFF0F4F8)
    )

    fun light(accent: AccentColor): ApturaPalette = ApturaPalette(
        isDark = false,
        // In light mode the "pure black" token is the brightest surface: video thumbnails, sheet backgrounds and
        // selected-chip text all need the opposite end of the scale.
        surfacePureBlack = Color(0xFFFFFFFF),
        surfaceBase = Color(0xFFF5F6F8),
        surfaceElevated1 = Color(0xFFFFFFFF),
        surfaceElevated2 = Color(0xFFFCFCFD),
        surfaceElevated3 = Color(0xFFEDEFF3),
        surfaceRecessed = Color(0x0F0F172A),
        surfaceScrim = Color(0xCC0A0C10),
        surfaceGlass = Color(0xB3FFFFFF),
        accentPrimary = accent.lightPrimary,
        accentSecondary = accent.lightSecondary,
        accentSubtle = accent.lightPrimary.copy(alpha = 0.12f),
        borderSubtle = Color(0x1F0F172A),
        statusSuccess = Color(0xFF087A42),
        statusWarning = Color(0xFF9A6B00),
        statusError = Color(0xFFC62828),
        statusInfo = Color(0xFF0B6FA4),
        textHighEmphasis = Color(0xFF111418),
        textMediumEmphasis = Color(0xFF4B5563),
        textLowEmphasis = Color(0xFF6B7280),
        textOnAccent = Color(0xFFFFFFFF),
        textOnScrim = Color(0xFFF0F4F8)
    )

    fun forMode(dark: Boolean, accent: AccentColor): ApturaPalette = if (dark) dark(accent) else light(accent)
}
