package org.aptura.player

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
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
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec

/*
 * How the app moves between screens.
 *
 * Every screen but the player keeps Navigation's default slide. The player is different because its picture is drawn
 * by a SurfaceView, which lives behind the window and is only visible through a hole Compose cuts for it. Sliding,
 * scaling or fading *the player itself* leaves that hole where it was for a frame or two (video floating over a moving
 * page) and, while the player is semi-transparent, shows whatever is composed beneath it through the hole. So the
 * player is never animated, and Navigation always stacks it above the screen it came from, so that screen cannot be
 * animated over it either. The only animation is a plain cover drawn *above* the video inside the player
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

/** Pushing a destination. The player itself never animates; its own cover does (see [PlayerRouteFrame]). */
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.appTransition(): ContentTransform =
    if (targetState.isPlayerRoute) {
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None,
            sizeTransform = null
        )
    } else {
        defaultTransitionSpec<T>().invoke(this)
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
        defaultPopTransitionSpec<T>().invoke(this)
    }

/** The system Back gesture. Out of the player it previews the same cover, driven by the finger. */
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.appPredictivePopTransition(swipeEdge: Int): ContentTransform =
    if (initialState.isPlayerRoute) {
        appPopTransition()
    } else {
        defaultPredictivePopTransitionSpec<T>().invoke(this, swipeEdge)
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
