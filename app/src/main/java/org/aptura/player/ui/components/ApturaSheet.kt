package org.aptura.player.ui.components

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
import androidx.compose.ui.unit.Velocity

/*
 * The app's bottom sheet: Material's ModalBottomSheet that stays put when a list inside it is flung to its end.
 *
 * Material hands whatever speed a fling has left over at the end of a list to the sheet, and the sheet settles at the
 * top with that speed. A spring started at its resting place with speed away from it swings past the place and back, so
 * flicking a long list up lifted the whole sheet by about 85 dp and dropped it again, over and over while scrolling
 * ("the screen starts dancing"). Swallowing the upward speed that is left over means the list simply stops at its end.
 * Downward speed is left alone: flicking down from the top of the list still closes the sheet.
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
        Column(Modifier.fillMaxWidth().nestedScroll(SwallowUpwardLeftoverFling)) { content() }
    }
}

/** Sits inside the sheet, so it hears about a fling's leftover speed before the sheet does. */
internal val SwallowUpwardLeftoverFling = object : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
}
