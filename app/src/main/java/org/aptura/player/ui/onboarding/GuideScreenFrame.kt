package org.aptura.player.ui.onboarding

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.LocalApturaAccent
import org.aptura.player.theme.ThemeMode

/*
 * Helpers shared by the guide's pages.
 */

/**
 * How far this page is from being the current one, as a progress from 0 (a page away) to 1 (centred). Pages read it to
 * fade and slide their parts in as they arrive, so the page turn and the page's own entrance are one motion that the
 * finger controls.
 */
internal val LocalGuidePageProgress = compositionLocalOf<() -> Float> { { 1f } }

/** Whether the guide's demonstrations play on their own. Off, each shows one still frame that says the same thing. */
internal val LocalGuideMotion = compositionLocalOf { true }

/** Provides [progress] to a page's content. */
@Composable
internal fun GuidePageScope(progress: () -> Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGuidePageProgress provides progress, content = content)
}

/**
 * Fades and lifts a part of a page in as the page arrives, each [index] a little after the one before it. Reads the
 * progress in the draw phase, so a page turn does not recompose anything.
 */
@Composable
internal fun Modifier.reveal(index: Int, distance: Dp = 22.dp): Modifier {
    val progress = LocalGuidePageProgress.current
    val distancePx = with(LocalDensity.current) { distance.toPx() }
    return graphicsLayer {
        // Parts arrive one after another, but the delay stops growing after the sixth so every part has fully arrived
        // by the time the page is centred, however far down the page it is.
        val p = ((progress() - minOf(index, 6) * 0.07f) / 0.5f).coerceIn(0f, 1f)
        alpha = p
        translationY = (1f - p) * distancePx
    }
}

/**
 * Whether the guide's demonstrations should move: not when the person has switched animations off in Android's
 * developer or accessibility settings (the animator duration scale is 0).
 */
@Composable
internal fun rememberGuideMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
            .getOrDefault(true)
    }
}

/**
 * Whether the window is too short for a decorative header above the headline: a landscape phone, or a small screen with
 * a large font. There the page's headline comes first, and the picture below it carries the page.
 */
@Composable
internal fun isShortWindow(): Boolean = windowSizeDp().height < 520.dp

/** The size of the window the guide is in, which in split-screen or a freeform window is not the size of the display. */
@Composable
internal fun windowSizeDp(): DpSize {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) { DpSize(size.width.toDp(), size.height.toDp()) }
}

/**
 * The player and its controls are always dark, whatever the app theme is. The guide's player scenes are drawn in the
 * same dark theme, with the person's own accent, so they look like the player and not like a light-mode imitation of it.
 */
@Composable
internal fun GuideDarkScene(content: @Composable () -> Unit) {
    ApturaTheme(themeMode = ThemeMode.DARK, accentColor = LocalApturaAccent.current, content = content)
}

/**
 * Shows [content] as the real screen it is, at a fixed design width, scaled to whatever room there is.
 *
 * The guide's mocks are built from the app's own composables (the library header, the folder strip, the video cards,
 * the player's gesture HUDs), so they cannot drift from the app. Those composables are laid out for a phone, so they are
 * laid out at [designWidth] and the result is scaled to fit, instead of being re-laid-out narrower (which would show a
 * layout the app never has).
 */
@Composable
internal fun GuideScreenFrame(
    modifier: Modifier = Modifier,
    designWidth: Dp = 360.dp,
    background: Color = ApturaColors.SurfaceBase,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    // On a wide screen the picture keeps its phone width and sits in the middle, instead of stretching into a wide
    // frame with the phone's layout in one corner of it.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(background)
                .border(1.dp, ApturaColors.BorderSubtle, shape)
        ) {
            Box(
                modifier = Modifier.layout { measurable, constraints ->
                    val designPx = designWidth.roundToPx()
                    val scale = if (constraints.hasBoundedWidth) minOf(1f, constraints.maxWidth / designPx.toFloat()) else 1f
                    val placeable = measurable.measure(Constraints.fixedWidth(designPx))
                    val width = (placeable.width * scale).toInt()
                    val height = (placeable.height * scale).toInt()
                    layout(width, height) {
                        placeable.placeWithLayer(0, 0) {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                }
            ) {
                // Inside the frame the density is the phone's, whatever the font scale or window it is shown in.
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                    content()
                }
            }
        }
    }
}
