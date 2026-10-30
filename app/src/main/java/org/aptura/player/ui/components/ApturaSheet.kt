package org.aptura.player.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/*
 * The app's bottom sheet: Material's ModalBottomSheet, set up so that a finger on its content reaches the sheet only
 * the way a finger on a scrolling list does.
 *
 * When a drag on the sheet itself ends, the sheet settles with the speed the finger had. A spring that starts at its
 * resting place (the top) with speed away from it swings past the place and back, so flicking upward on a part of the
 * sheet that does not scroll (a title, eight folders that fit on the screen) lifted the whole sheet by 30 to 85 dp and
 * dropped it again, over and over ("the screen starts dancing"). A list does not do that, because its drags go to the
 * sheet as nested scrolling, and the upward speed it has left over is swallowed below.
 *
 * So the content (and the handle) sits inside a vertical scrollable that moves nothing: every drag on it is nested
 * scrolling too. The sheet is still pulled down, and still pulled up when it is not fully open (the player's sheets open
 * half way when the phone is tilted, and are pulled up to reach their options), and a flick down still closes it; only
 * the spring past the top is gone. Nothing ever consumes touch events above what the person is touching: that cancelled
 * their taps and scrolls (the second fix, which felt dead), and an earlier build that switched sheet dragging off made
 * the tilted player's options unreachable.
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
        dragHandle = { Box(Modifier.sheetDragsAreNestedScrolls(sheetState)) { BottomSheetDefaults.DragHandle() } },
        modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth().sheetDragsAreNestedScrolls(sheetState)) { content() }
    }
}

/** Makes every vertical drag on this part of a sheet a nested scroll (which moves nothing itself) with the spring removed. */
@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.sheetDragsAreNestedScrolls(sheet: SheetState): Modifier = composed {
    val noScroll = rememberScrollableState { 0f }
    val connection = remember(sheet) { UpwardFlingSettlesGently(sheet) }
    this
        .nestedScroll(connection)
        .scrollable(state = noScroll, orientation = Orientation.Vertical)
}

/**
 * Sits inside the sheet, so it hears about a fling's leftover speed before the sheet does. Upward speed that nothing in the
 * content could use would make the sheet settle with it and swing past its top. If the sheet is already all the way up, that
 * speed is simply dropped. If it is only half open, the flick meant "open it": the sheet is opened with no speed at all, which
 * is smooth (handing it the speed let a short, quick flick end back at half open, or fly past the top).
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class UpwardFlingSettlesGently(private val sheet: SheetState) : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (available.y >= 0f) return Velocity.Zero
        if (sheet.currentValue != SheetValue.Expanded && sheet.hasExpandedState) sheet.expand()
        return Velocity(0f, available.y)
    }
}
