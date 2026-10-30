package org.aptura.player.theme

import androidx.compose.ui.graphics.Color

/**
 * How Aptura chooses between its light and dark palettes.
 *
 * [SYSTEM] is the Android default and the recommended setting: the app follows the device's dark theme. [DARK] is
 * Aptura's cinematic baseline. The player itself is always dark, whatever this is set to: video is the hero, and a
 * bright control surface around it would fight the picture.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * A curated set of accents. Each one has a dark-palette and a light-palette variant, because a cyan that glows on
 * near-black is unreadable on white. Both variants are checked for contrast in `ApturaPalettesTest`.
 */
enum class AccentColor(
    val label: String,
    internal val darkPrimary: Color,
    internal val darkSecondary: Color,
    internal val lightPrimary: Color,
    internal val lightSecondary: Color
) {
    CYAN("Cyan", Color(0xFF00E5FF), Color(0xFF00B4D8), Color(0xFF00707F), Color(0xFF005E6B)),
    BLUE("Blue", Color(0xFF6EA8FF), Color(0xFF3D6FD8), Color(0xFF1D4ED8), Color(0xFF173F9E)),
    VIOLET("Violet", Color(0xFFB388FF), Color(0xFF8C5CE0), Color(0xFF6D28D9), Color(0xFF4C1D95)),
    AMBER("Amber", Color(0xFFFFC857), Color(0xFFD99A1F), Color(0xFF8A5300), Color(0xFF6B3F00)),
    ROSE("Rose", Color(0xFFFF6E9C), Color(0xFFD94F7C), Color(0xFFB3124B), Color(0xFF8A0F38)),
    GOLD("Gold", Color(0xFFFFD700), Color(0xFFE5B800), Color(0xFF7A5200), Color(0xFF5E3F00));

    /** The colour a swatch should show in the palette that is currently on screen. */
    internal fun previewPrimary(dark: Boolean): Color = if (dark) darkPrimary else lightPrimary
}
