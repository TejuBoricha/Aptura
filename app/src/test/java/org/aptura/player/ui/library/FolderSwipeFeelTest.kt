package org.aptura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.aptura.player.ui.visual.captureScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Swiping between folders felt slow (the owner's report; the look was liked and stays). Two things made it slow: the
 * gesture only counted after the finger had gone 64 dp *and lifted*, so a short, quick flick did nothing and a long one
 * started moving only once it was over; and the slide took a quarter of a second with an easing that starts slowly.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class FolderSwipeFeelTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ---- the slide ---------------------------------------------------------------------------------------------

    private val from = Color(0xFF202020)
    private val to = Color(0xFFE0E0E0)

    /** How far each 16 ms frame is from the finished folder (1 = nothing has arrived, 0 = it is all there). */
    private fun errorPerFrame(frames: Int): List<Double> {
        var selected by mutableStateOf<String?>(null)
        composeRule.setContent {
            LibraryVideoPaneHost(
                state = MediaLibraryUiState.Success(videos = emptyList(), folders = listOf("Movies"), selectedFolder = selected),
                swipeEnabled = true,
                onSwipe = {},
                pane = { state, modifier -> Box(modifier.fillMaxSize().background(if (state.selectedFolder == null) from else to)) }
            )
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { selected = "Movies" }
        composeRule.mainClock.autoAdvance = false
        return List(frames) {
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
            val screen = composeRule.captureScreen()
            var error = 0.0
            var count = 0
            for (gx in 1 until 20) for (gy in 1 until 20) {
                error += Math.abs(screen.colorAt(screen.width * gx / 20, screen.height * gy / 20).red - to.red)
                count++
            }
            error / count / (to.red - from.red)
        }
    }

    private fun msUntil(error: List<Double>, atMost: Double) = (error.indexOfFirst { it <= atMost } + 1) * 16

    @Test
    fun theNewFolder_isHalfwayInAboutAFifthOfASecond_andAllThereBeforeTheOldOneTook() {
        val error = errorPerFrame(30)
        val halfway = msUntil(error, 0.5)
        val settled = msUntil(error, 0.01)
        assertTrue("halfway took $halfway ms (it was 112 ms)", halfway in 1..80)
        assertTrue("settling took $settled ms (it was 240 ms)", settled in 1..160)
    }

    // ---- the gesture -------------------------------------------------------------------------------------------

    private val swipes = mutableListOf<Int>()

    private fun showSwipeArea() {
        swipes.clear()
        composeRule.setContent {
            Box(Modifier.size(300.dp).testTag("area").folderSwipe(enabled = true) { swipes += it })
        }
        composeRule.waitForIdle()
    }

    /** The finger goes [dp] to the left over [overMillis], in small steps, and lets go only if [lift]. */
    private fun drag(dp: Float, overMillis: Long, lift: Boolean = true) {
        composeRule.onNodeWithTag("area").performTouchInput {
            val total = dp.dp.toPx()
            val steps = (overMillis / 16).toInt().coerceAtLeast(1)
            down(Offset(centerX + 100f, centerY))
            repeat(steps) { moveBy(Offset(-total / steps, 0f), delayMillis = 16) }
            if (lift) up()
        }
    }

    @Test
    fun aSlowDragThatGoesFarEnough_movesTheFolderBeforeTheFingerLifts() {
        showSwipeArea()
        drag(dp = 80f, overMillis = 640, lift = false)
        assertEquals("the folder should already have moved, the finger is still down", listOf(1), swipes)
        composeRule.onNodeWithTag("area").performTouchInput { up() }
        assertEquals("lifting does not move it again", listOf(1), swipes)
    }

    @Test
    fun aShortQuickFlick_movesTheFolder() {
        showSwipeArea()
        drag(dp = 50f, overMillis = 64)
        assertEquals(listOf(1), swipes)
    }

    @Test
    fun aShortSlowDrag_doesNothing() {
        showSwipeArea()
        drag(dp = 56f, overMillis = 900)
        assertEquals(emptyList<Int>(), swipes)
    }

    @Test
    fun aTouchTooShortToBeADrag_doesNothing_evenIfQuick() {
        showSwipeArea()
        drag(dp = 6f, overMillis = 32)
        assertEquals(emptyList<Int>(), swipes)
    }

    @Test
    fun oneLongDrag_isOneSwipe_notOnePerStep() {
        showSwipeArea()
        drag(dp = 250f, overMillis = 800)
        assertEquals(listOf(1), swipes)
    }
}
