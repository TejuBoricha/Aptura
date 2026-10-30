package org.aptura.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** The strip at the top of every sheet: swipe it down or tap it to close the sheet. */
const val SHEET_HANDLE_TAG = "sheet_handle"

/*
 * The app's bottom sheet: Material's ModalBottomSheet, with its content kept apart from the sheet's own dragging.
 *
 * Material lets the sheet be dragged from anywhere on it, and when a drag ends the sheet settles with the speed the finger
 * had. A spring that starts at its resting place (the top) with speed away from it swings past the place and back, so
 * flicking upward lifted the whole sheet by 30 to 85 dp and dropped it again, over and over ("the screen starts
 * dancing"); on a list the sheet also took part in every scroll that reached an end. Two earlier fixes tried to keep the
 * sheet from receiving that speed. One of them took finger movement away from the sheet, and Android reads that as
 * "something else has this touch", so taps and scrolls inside the sheet were cancelled: it felt dead. So now nothing the
 * finger does on the content reaches the sheet at all. A list scrolls, a button taps, and the sheet does not move. The
 * handle at the top closes the sheet (swipe down, or tap), the area outside it and Back do too.
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
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        // The same way a tap on the dimmed area closes it: slide out, then tell the owner of the sheet.
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismissRequest() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = containerColor,
        scrimColor = scrimColor,
        shape = shape,
        dragHandle = { SheetHandle(onClose = close) },
        modifier = modifier,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetHandle(onClose: () -> Unit) {
    var dragged by remember { mutableFloatStateOf(0f) }
    val closingDistance = with(LocalDensity.current) { 40.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag(SHEET_HANDLE_TAG)
            .clickable(onClickLabel = "Close", onClick = onClose)
            .draggable(
                state = rememberDraggableState { dragged += it },
                orientation = Orientation.Vertical,
                onDragStarted = { dragged = 0f },
                onDragStopped = { velocity ->
                    if (dragged > closingDistance || velocity > CLOSING_SPEED_PX_PER_S) onClose()
                    dragged = 0f
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

/** A flick down on the handle faster than this closes the sheet however short it was. */
private const val CLOSING_SPEED_PX_PER_S = 900f
