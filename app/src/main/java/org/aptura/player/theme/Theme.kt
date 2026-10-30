package org.aptura.player.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Resolves the theme a route must use. Playback is always a dark room, even when the app itself is pinned to Light;
 * every other route follows the chosen mode. Pure on purpose: the navigation tree reads it, and the rule itself is
 * unit-tested without composing a single screen.
 */
fun themeModeForRoute(isPlayerRouteActive: Boolean, appThemeMode: ThemeMode): ThemeMode =
    if (isPlayerRouteActive) ThemeMode.DARK else appThemeMode

/**
 * Aptura Theme Definition
 * Source of truth: DESIGN.md §1 & §2
 *
 * Follows the system by default, can be pinned light or dark, and takes one of a curated set of accents. The theme is
 * scoped to the composables inside it, so the always-dark player can wrap itself in `ApturaTheme(DARK)` while the
 * light library is still on screen beneath it (a transition, a predictive back swipe) and each draws its own colours.
 * The window itself (system bar icons, window background) is not touched here: see [ApturaWindowChrome].
 */
@Composable
fun ApturaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.CYAN,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = remember(dark, accentColor) { ApturaPalettes.forMode(dark, accentColor) }
    val colorScheme = remember(palette) { palette.toColorScheme() }

    CompositionLocalProvider(LocalApturaPalette provides palette, LocalApturaAccent provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ApturaTypography,
            content = content
        )
    }
}

/**
 * Makes the parts of the window that no composable owns match the screen on top: the status and navigation bar icons,
 * and the window background that shows wherever nothing is drawn (a fade, a gap between two screens).
 *
 * There is exactly one of these, placed where the navigation stack is known, because the window is shared by every
 * screen. Letting each [ApturaTheme] set it would leave the icons of whichever theme composed last.
 *
 * @param darkScreen whether the screen on top is dark, so the bar icons must be light.
 * @param background the colour of the screen on top.
 */
@Composable
fun ApturaWindowChrome(darkScreen: Boolean, background: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        // Light bars need dark icons; dark bars need light icons. This is what makes status/nav icons legible
        // when the app overrides the system's own mode.
        insetsController.isAppearanceLightStatusBars = !darkScreen
        insetsController.isAppearanceLightNavigationBars = !darkScreen
        // The manifest theme's window background is fixed dark; in the Light theme any frame that does not paint
        // every pixel would flash it.
        window.setBackgroundDrawable(ColorDrawable(background.toArgb()))
    }
}

private fun ApturaPalette.toColorScheme() = if (isDark) {
    darkColorScheme(
        primary = accentPrimary,
        onPrimary = textOnAccent,
        primaryContainer = accentSubtle,
        onPrimaryContainer = accentPrimary,
        secondary = accentSecondary,
        onSecondary = textOnAccent,
        background = surfaceBase,
        onBackground = textHighEmphasis,
        surface = surfaceElevated1,
        onSurface = textHighEmphasis,
        surfaceVariant = surfaceElevated2,
        onSurfaceVariant = textMediumEmphasis,
        surfaceContainer = surfaceElevated1,
        surfaceContainerHigh = surfaceElevated2,
        surfaceContainerHighest = surfaceElevated3,
        outline = textLowEmphasis,
        error = statusError,
        onError = textHighEmphasis
    )
} else {
    lightColorScheme(
        primary = accentPrimary,
        onPrimary = textOnAccent,
        primaryContainer = accentSubtle,
        onPrimaryContainer = accentPrimary,
        secondary = accentSecondary,
        onSecondary = textOnAccent,
        background = surfaceBase,
        onBackground = textHighEmphasis,
        surface = surfaceElevated1,
        onSurface = textHighEmphasis,
        surfaceVariant = surfaceElevated2,
        onSurfaceVariant = textMediumEmphasis,
        surfaceContainer = surfaceElevated1,
        surfaceContainerHigh = surfaceElevated2,
        surfaceContainerHighest = surfaceElevated3,
        outline = textLowEmphasis,
        error = statusError,
        // The light error red is dark enough for white text; the dark palette keeps its near-white label.
        onError = Color.White
    )
}
