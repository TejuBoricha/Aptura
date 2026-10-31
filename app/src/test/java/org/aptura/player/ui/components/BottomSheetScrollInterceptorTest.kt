package org.aptura.player.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies [rememberBottomSheetScrollInterceptor] contract:
 * - Consumes upward boundary scroll deltas (available.y < 0) so the sheet does not jerk or oscillate.
 * - Leaves downward drag deltas (available.y > 0) untouched so swipe-to-dismiss remains responsive.
 * - Consumes leftover fling velocity in onPostFling to halt boundary bounce feedback loops.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BottomSheetScrollInterceptorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onPostScroll_consumesUpwardDeltaAtBottomBoundary() {
        composeRule.setContent {
            val interceptor = rememberBottomSheetScrollInterceptor()

            // available.y < 0 represents dragging upward past the end of the content
            val consumed = interceptor.onPostScroll(
                consumed = Offset.Zero,
                available = Offset(x = 0f, y = -150f),
                source = NestedScrollSource.UserInput
            )

            assertEquals(0f, consumed.x, 0.001f)
            assertEquals(-150f, consumed.y, 0.001f)
        }
    }

    @Test
    fun onPostScroll_passesThroughDownwardDeltaForSwipeToDismiss() {
        composeRule.setContent {
            val interceptor = rememberBottomSheetScrollInterceptor()

            // available.y > 0 represents dragging downward from the top of the content
            val consumed = interceptor.onPostScroll(
                consumed = Offset.Zero,
                available = Offset(x = 0f, y = 150f),
                source = NestedScrollSource.UserInput
            )

            // Must NOT consume downward drag, so ModalBottomSheet can handle swipe-to-dismiss
            assertEquals(Offset.Zero, consumed)
        }
    }

    @Test
    fun onPostScroll_ignoresPureHorizontalScroll() {
        composeRule.setContent {
            val interceptor = rememberBottomSheetScrollInterceptor()

            val consumed = interceptor.onPostScroll(
                consumed = Offset.Zero,
                available = Offset(x = -100f, y = 0f),
                source = NestedScrollSource.UserInput
            )

            assertEquals(Offset.Zero, consumed)
        }
    }

    @Test
    fun onPostFling_consumesResidualVelocityAtBoundaries() = runBlocking {
        composeRule.setContent {
            val interceptor = rememberBottomSheetScrollInterceptor()

            val consumedUpward = runBlocking {
                interceptor.onPostFling(
                    consumed = Velocity.Zero,
                    available = Velocity(x = 0f, y = -2500f)
                )
            }
            // Residual velocity is consumed completely
            assertEquals(Velocity(x = 0f, y = -2500f), consumedUpward)

            val consumedDownward = runBlocking {
                interceptor.onPostFling(
                    consumed = Velocity.Zero,
                    available = Velocity(x = 0f, y = 2500f)
                )
            }
            assertEquals(Velocity(x = 0f, y = 2500f), consumedDownward)
        }
    }

    @Test
    fun modifier_attachesWithoutErrorInComposeHierarchy() {
        composeRule.setContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .bottomSheetScrollInterceptor()
            ) {
                items(20) { index ->
                    androidx.compose.material3.Text("Item $index")
                }
            }
        }
        composeRule.waitForIdle()
    }
}
