package org.aptura.player.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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
 * Follows the system by default, can be pinned light or dark, and takes one of a curated set of accents. The player
 * route always resolves to dark so the picture is never framed by a bright control surface.
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
    // Swap the global tokens before the content composes. Doing it in a SideEffect would paint one frame with the
    // previous mode on every cold start and every player transition. The remember returns the palette it applied so
    // the lint rule against Unit-returning remember calls stays satisfied.
    val activePalette = remember(palette) {
        ApturaColors.apply(palette)
        palette
    }
    val colorScheme = remember(activePalette) { activePalette.toColorScheme() }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            // Light bars need dark icons; dark bars need light icons. This is what makes status/nav icons legible
            // when the app overrides the system's own mode.
            insetsController.isAppearanceLightStatusBars = !dark
            insetsController.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ApturaTypography,
        content = content
    )
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
