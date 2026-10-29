package org.aptura.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Below this height a window is "short": a phone held sideways (360 to 411 dp), or a small screen with a very large
 * font. Screens drop what only decorates (a large title, big cards) so that what the person came for stays in view.
 */
internal val SHORT_WINDOW_HEIGHT = 480.dp

/**
 * The size of the window the screen is in, which in split-screen or a freeform window is not the size of the display.
 * Prefer the constraints of a `BoxWithConstraints` where one is already there; this is for a screen that has none.
 */
@Composable
internal fun windowSizeDp(): DpSize {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) { DpSize(size.width.toDp(), size.height.toDp()) }
}

@Composable
internal fun isShortWindowHeight(): Boolean = windowSizeDp().height < SHORT_WINDOW_HEIGHT
