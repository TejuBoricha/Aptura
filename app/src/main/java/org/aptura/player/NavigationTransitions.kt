package org.aptura.player

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEvent

/*
 * How the app moves between screens.
 *
 * Navigation 3's own defaults are a 700 ms cross-fade for going forward and back, and, for the Back gesture, a page that
 * shrinks to 70% without ever fading and then vanishes when the gesture ends. Both read as flicker: for most of the
 * cross-fade two whole screens are half transparent on top of each other (a title drawn through the page behind it),
 * and the shrunken page is still fully opaque on the frame it disappears. So no screen is ever faded:
 *
 *  - Going forward the new page slides in from the right over the page it came from, which moves a fifth of its width
 *    to the left underneath. Going back is the same movement in reverse. Both pages are opaque the whole time, so
 *    nothing shows through anything.
 *  - The Back gesture drags the page along (it follows the finger and shrinks a little), and letting go carries the
 *    same movement on to the edge, so the gesture and the Back button end in the same place instead of the page
 *    snapping out of existence.
 *
 * The player is different because its picture is drawn by a SurfaceView, which lives behind the window and is only
 * visible through a hole Compose cuts for it. Sliding, scaling or fading the player itself leaves that hole where it was for a frame or two (video floating over a moving
 * page) and, while the player is semi-transparent, shows whatever is composed beneath it through the hole. So the
 * player is never animated, and Navigation always stacks it above the screen it came from, so that screen cannot be
 * animated over it either. The only animation is a plain cover drawn above the video inside the player
 * ([PlayerRouteFrame]), which darkens the picture correctly because it is drawn after the hole is cut:
 *
 *  - Opening: the page is replaced by black at once, and the player's cover lifts, so the controls and picture appear
 *    out of the dark instead of the whole screen snapping.
 *  - Closing: the player's cover rises to the colour of the page it returns to, and the player leaves when it has
 *    covered everything, so the library appears on a page that is already its own colour and not on black.
 */

/** How long the cover takes to lift when the player opens. */
internal const val PLAYER_REVEAL_MS = 160

/** How long the cover takes to rise when the player closes. */
internal const val PLAYER_COVER_MS = 120

private const val PLAYER_ROUTE_KEY = "aptura.route.player"

/** Entry metadata that marks a destination as the player. Read back by the transition specs below. */
internal val PlayerRouteMetadata: Map<String, Any> = mapOf(PLAYER_ROUTE_KEY to true)

internal val Scene<*>.isPlayerRoute: Boolean
    get() = metadata[PLAYER_ROUTE_KEY] == true

/** How long a page takes to slide in or out. */
internal const val SCREEN_SLIDE_MS = 200

/** Quick to start, long soft landing (Material's emphasized decelerate). */
private val SlideEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private fun <T> slideSpec() = tween<T>(durationMillis = SCREEN_SLIDE_MS, easing = SlideEasing)

/** Pushing a destination. The player itself never animates; its own cover does (see [PlayerRouteFrame]). */
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.appTransition(): ContentTransform =
    if (targetState.isPlayerRoute) {
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None,
            sizeTransform = null
        )
    } else {
        ContentTransform(
            targetContentEnter = slideInHorizontally(slideSpec()) { fullWidth -> fullWidth },
            initialContentExit = ExitTransition.None,
            sizeTransform = null
        )
    }

/** Popping a destination. The player stays until its cover has risen; the page beneath it does not move. */
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.appPopTransition(): ContentTransform =
    if (initialState.isPlayerRoute) {
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.KeepUntilTransitionsFinished,
            sizeTransform = null
        )
    } else {
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = slideOutHorizontally(slideSpec()) { fullWidth -> fullWidth },
            sizeTransform = null
        )
    }

/**
 * The system Back gesture. Out of the player it previews the same cover, driven by the finger. Out of any other page
 * the page is dragged the way it will leave, linearly so that the page moves exactly as far as the finger has, at 100%
 * full scale with no underlay drift.
 */
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.appPredictivePopTransition(
    @NavigationEvent.SwipeEdge swipeEdge: Int
): ContentTransform =
    if (initialState.isPlayerRoute) {
        appPopTransition()
    } else {
        val toTheRight = swipeEdge != NavigationEvent.EDGE_RIGHT
        val drag = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = SCREEN_SLIDE_MS, easing = LinearEasing)
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = slideOutHorizontally(drag) { fullWidth -> if (toTheRight) fullWidth else -fullWidth },
            sizeTransform = null
        )
    }

/**
 * Wraps the player route's content with the cover that does the opening and closing.
 *
 * The cover is a flat colour drawn above [content] with an alpha, never an alpha on the player itself: the picture is a
 * SurfaceView, and a cover drawn after its hole darkens the video correctly while a semi-transparent player would let
 * the page beneath show through the hole. It is black while the player opens and [closingColor] (the colour of the page
 * the player returns to) while it closes.
 */
@Composable
internal fun PlayerRouteFrame(closingColor: Color, content: @Composable () -> Unit) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val cover by transition.animateFloat(
        transitionSpec = {
            when {
                // Opening: lift the cover. Closing: raise it.
                initialState == EnterExitState.PreEnter -> tween(PLAYER_REVEAL_MS)
                else -> tween(PLAYER_COVER_MS)
            }
        },
        label = "playerCover"
    ) { state -> if (state == EnterExitState.Visible) 0f else 1f }
    val leaving = transition.targetState == EnterExitState.PostExit

    Box(Modifier.fillMaxSize()) {
        content()
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = cover }
                .background(if (leaving) closingColor else Color.Black)
        )
    }
}
