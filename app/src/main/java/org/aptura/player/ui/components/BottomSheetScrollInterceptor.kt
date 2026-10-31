package org.aptura.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Creates a [NestedScrollConnection] that stabilizes scrollable content nested within a
 * [androidx.compose.material3.ModalBottomSheet].
 *
 * It prevents the sheet from violently oscillating ("dancing") or bouncing when a user flings
 * or scrolls rapidly to the bottom of an inner list, grid, or scroll container.
 *
 * Specifically:
 * 1. Consumes boundary upward scroll deltas (`available.y < 0`) in [NestedScrollConnection.onPostScroll],
 *    preventing the parent sheet from dragging upward or jittering when the bottom boundary is reached.
 * 2. Consumes leftover boundary fling velocity in [NestedScrollConnection.onPostFling], preventing
 *    unconsumed momentum from kicking the parent sheet's draggable state into spring oscillation
 *    against Android 12+ stretch overscroll.
 * 3. Preserves downward drag deltas (`available.y > 0`) at the top boundary so swipe-to-dismiss
 *    remains fully responsive.
 */
@Composable
fun rememberBottomSheetScrollInterceptor(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // When dragging upward past the bottom boundary (available.y < 0), consume it
                // locally so it does not pull or shake the bottom sheet.
                // Allow downward delta (available.y > 0) to pass through to ModalBottomSheet
                // so the user can swipe down from the top to dismiss.
                return if (available.y < 0f) Offset(0f, available.y) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Consume leftover boundary fling velocity so excess momentum does
                // not kick the sheet's AnchoredDraggable state into violent oscillation.
                return available
            }
        }
    }
}

/**
 * Applies [rememberBottomSheetScrollInterceptor] to this [Modifier].
 */
@Composable
fun Modifier.bottomSheetScrollInterceptor(): Modifier {
    return this.nestedScroll(rememberBottomSheetScrollInterceptor())
}
