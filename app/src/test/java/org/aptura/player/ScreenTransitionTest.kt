package org.aptura.player

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.aptura.player.ui.visual.CapturedScreen
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
 * Going to Settings and coming back, frame by frame (the "back navigation flickers" report).
 *
 * Navigation 3's own defaults cross-fade for 700 ms, so for most of a move two whole screens are half see-through on
 * top of each other, and the Back gesture ends on a shrunken page that is still fully opaque and then disappears in a
 * single frame. Two plain screens in two flat colours make both visible without looking at a picture: while anything
 * moves, every pixel must be one of the two screens' colours (or the window behind them), never a blend of them; and
 * once the finger lets go, the page must keep moving until it is gone instead of waiting and vanishing.
 *
 * The wiring is the app's own: the same transition specs and the same back-gesture events the system sends.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class ScreenTransitionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val library = Color(0xFFD32F2F)
    private val settings = Color(0xFF1976D2)
    private val window = Color(0xFF388E3C)

    private lateinit var backStack: NavBackStack<NavKey>
    private lateinit var dispatcher: OnBackPressedDispatcher

    @Composable
    private fun Flat(color: Color) = Box(Modifier.fillMaxSize().background(color))

    private fun show() {
        composeRule.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            backStack = rememberNavBackStack(MediaLibrary)
            // The real window is painted the page colour behind the pages; here a third colour makes it visible.
            Box(Modifier.fillMaxSize().background(window)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = { appTransition() },
                    popTransitionSpec = { appPopTransition() },
                    predictivePopTransitionSpec = { edge -> appPredictivePopTransition(edge) },
                    entryProvider = entryProvider {
                        entry<MediaLibrary> { Flat(library) }
                        entry<Settings> { Flat(settings) }
                    }
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun settle() {
        composeRule.mainClock.autoAdvance = true
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
    }

    /** One 16 ms frame at a time, with the clock under the test's control. */
    private fun frames(count: Int, before: (Int) -> Unit = {}, after: (Int, CapturedScreen) -> Unit) {
        composeRule.mainClock.autoAdvance = false
        repeat(count) { frame ->
            before(frame)
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
            after(frame, composeRule.captureScreen())
        }
    }

    /** Where on a grid across the screen each colour is. */
    private class Census(val library: Double, val settings: Double, val window: Double, val blended: Double)

    private fun CapturedScreen.census(): Census {
        var lib = 0; var set = 0; var win = 0; var other = 0; var total = 0
        for (gx in 1 until GRID) {
            for (gy in 1 until GRID) {
                total++
                when (colorAt(width * gx / GRID, height * gy / GRID)) {
                    library -> lib++
                    settings -> set++
                    window -> win++
                    else -> other++
                }
            }
        }
        return Census(lib / total.toDouble(), set / total.toDouble(), win / total.toDouble(), other / total.toDouble())
    }

    private fun openSettings() {
        composeRule.runOnIdle { backStack.add(Settings) }
        settle()
    }

    @Test
    fun openingSettings_neverBlendsTheTwoScreens() {
        show()
        composeRule.runOnIdle { backStack.add(Settings) }
        val seen = mutableListOf<Census>()
        frames(40) { _, screen -> seen += screen.census() }

        seen.forEachIndexed { frame, c -> assertTrue("frame $frame: ${percent(c.blended)} of the screen is a blend", c.blended <= MAX_BLENDED) }
        assertTrue("the new page was never seen sliding over the old one", seen.any { it.library > 0.1 && it.settings > 0.1 })
        assertEquals("it ends on Settings alone", 1.0, seen.last().settings, 0.001)
    }

    @Test
    fun goingBack_neverBlendsTheTwoScreens() {
        show()
        openSettings()
        composeRule.runOnIdle { backStack.removeLastOrNull() }
        val seen = mutableListOf<Census>()
        frames(40) { _, screen -> seen += screen.census() }

        seen.forEachIndexed { frame, c -> assertTrue("frame $frame: ${percent(c.blended)} of the screen is a blend", c.blended <= MAX_BLENDED) }
        assertTrue("the page was never seen sliding off", seen.any { it.library > 0.1 && it.settings > 0.1 })
        assertEquals("it ends on the library alone", 1.0, seen.last().library, 0.001)
    }

    @Test
    fun theBackGesture_followsTheFinger_andLettingGoCarriesOnInsteadOfVanishing() {
        show()
        openSettings()
        composeRule.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(10f, 300f, 0f, BackEventCompat.EDGE_LEFT)) }

        // The finger travels part of the way.
        val whileDragging = mutableListOf<Census>()
        frames(6, before = { frame ->
            composeRule.runOnIdle {
                dispatcher.dispatchOnBackProgressed(BackEventCompat(10f + frame * 20f, 300f, (frame + 1) * 0.07f, BackEventCompat.EDGE_LEFT))
            }
        }) { _, screen -> whileDragging += screen.census() }
        whileDragging.forEachIndexed { frame, c -> assertTrue("drag frame $frame: ${percent(c.blended)} is a blend", c.blended <= MAX_BLENDED) }
        assertTrue("the page did not move with the finger", whileDragging.last().settings < whileDragging.first().settings - 0.05)
        assertTrue("the page left the screen while the finger was still down", whileDragging.last().settings > 0.2)

        // Then lets go.
        composeRule.runOnIdle { dispatcher.onBackPressed() }
        val afterRelease = mutableListOf<Census>()
        frames(60) { _, screen -> afterRelease += screen.census() }
        afterRelease.forEachIndexed { frame, c -> assertTrue("release frame $frame: ${percent(c.blended)} is a blend", c.blended <= MAX_BLENDED) }

        val shown = whileDragging.last().settings
        val sequence = listOf(shown) + afterRelease.map { it.settings }
        sequence.zipWithNext().forEachIndexed { frame, (before, now) ->
            assertTrue(
                "after letting go the page went from ${percent(before)} to ${percent(now)} of the screen in one frame " +
                    "(frame $frame): it waited and vanished instead of carrying on",
                before - now <= MAX_JUMP
            )
        }
        assertTrue("the page never got off the screen", afterRelease.last().settings == 0.0)
        assertEquals("it ends on the library alone", 1.0, afterRelease.last().library, 0.001)
    }

    private fun percent(fraction: Double) = "${(fraction * 100).toInt()}%"

    private companion object {
        const val GRID = 24

        /** Samples that land on the edge of a moving page may be a mix of the two colours; a cross-fade is nearly all of them. */
        const val MAX_BLENDED = 0.10

        /** The most a page may give up in one 16 ms frame: a quarter of the screen. A vanish is the whole page at once. */
        const val MAX_JUMP = 0.25
    }
}
