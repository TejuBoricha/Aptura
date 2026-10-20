package org.aptura.player.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Aptura Theme Definition
 * Source of truth: DESIGN.md §1 & §2
 * Cinematic dark-first design engineered for OLED/AMOLED displays.
 */
private val ApturaDarkColorScheme = darkColorScheme(
    primary = ApturaColors.AccentPrimary,
    onPrimary = ApturaColors.TextOnAccent,
    primaryContainer = ApturaColors.AccentSubtle,
    onPrimaryContainer = ApturaColors.AccentPrimary,
    secondary = ApturaColors.AccentSecondary,
    onSecondary = ApturaColors.TextOnAccent,
    background = ApturaColors.SurfaceBase,
    onBackground = ApturaColors.TextHighEmphasis,
    surface = ApturaColors.SurfaceElevated1,
    onSurface = ApturaColors.TextHighEmphasis,
    surfaceVariant = ApturaColors.SurfaceElevated2,
    onSurfaceVariant = ApturaColors.TextMediumEmphasis,
    surfaceContainer = ApturaColors.SurfaceElevated1,
    surfaceContainerHigh = ApturaColors.SurfaceElevated2,
    surfaceContainerHighest = ApturaColors.SurfaceElevated3,
    outline = ApturaColors.TextLowEmphasis,
    error = ApturaColors.StatusError,
    onError = ApturaColors.TextHighEmphasis
)

@Composable
fun ApturaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Aptura is cinematic dark-first. Dark color scheme is always applied.
    val colorScheme = ApturaDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = ApturaColors.SurfaceBase.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = ApturaColors.SurfacePureBlack.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ApturaTypography,
        content = content
    )
}
