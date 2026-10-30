package org.aptura.player.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Velocity

/*
 * The app's bottom sheet: Material's ModalBottomSheet that stays put when it is flicked upward.
 *
 * When a drag ends, the sheet settles at the nearer of its two places with the speed the finger had. A spring that
 * starts at its resting place (the top) with speed away from it swings past the place and back, so flicking upward lifted
 * the whole sheet by 30 to 85 dp and dropped it again, over and over ("the screen starts dancing"). That happens in two
 * ways: the speed a list has left over at its end is handed to the sheet, and on a sheet with nothing to scroll (eight
 * folders that fit on the screen) the finger drags the sheet itself. Both are closed here: the sheet follows the finger
 * only downward (a move upward ends the drag and the sheet settles with no speed), and the upward speed a list leaves over
 * is swallowed. Pulling down still closes the sheet, and the handle at the top can be dragged freely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApturaModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    containerColor: Color,
    modifier: Modifier = Modifier,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        scrimColor = scrimColor,
        shape = shape,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .nestedScroll(SwallowUpwardLeftoverFling)
                .sheetFollowsOnlyDownward()
        ) { content() }
    }
}

/**
 * Takes upward finger movement away from the sheet's own drag, after the content under the finger (a list, a button) has
 * had its turn, so a drag that moves up ends there and the sheet settles with no speed instead of flying past its top.
 */
private fun Modifier.sheetFollowsOnlyDownward(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            event.changes.forEach { change ->
                if (!change.isConsumed && change.positionChange().y < 0f) change.consume()
            }
        } while (event.changes.any { it.pressed })
    }
}

/** Sits inside the sheet, so it hears about a fling's leftover speed before the sheet does. */
internal val SwallowUpwardLeftoverFling = object : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
}
