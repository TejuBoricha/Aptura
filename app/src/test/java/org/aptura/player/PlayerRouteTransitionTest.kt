package org.aptura.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaPalettes
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
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
 * Opening and closing the player in the Light theme, frame by frame (the "back navigation flickers" report).
 *
 * The app used to hold one global palette and flip it to dark a frame *after* the player was pushed or popped. For the
 * whole slide the screens were therefore painted with the wrong colours: a light library turned dark on the way in, and
 * the dark player turned light on the way out. Each screen now carries its own theme, and the player is not animated
 * (its video is a SurfaceView, which cannot follow a slide or a fade); the screens around it do the moving.
 *
 * Two plain screens stand in for the library and the player. What matters is the wiring, which is the real one: the
 * same transition specs, the same player-route metadata and the same theme scoping as the app's navigation.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class PlayerRouteTransitionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val light = ApturaPalettes.light(AccentColor.CYAN)
    private val dark = ApturaPalettes.dark(AccentColor.CYAN)

    /** Which palette each screen read while composing, in order. */
    private val libraryReads = mutableListOf<Boolean>()
    private val playerReads = mutableListOf<Boolean>()
    private var libraryComposed = false
    private var playerComposed = false

    private lateinit var backStack: androidx.navigation3.runtime.NavBackStack<NavKey>
    private lateinit var listState: androidx.compose.foundation.lazy.LazyListState

    @Composable
    private fun Library() {
        val isDark = ApturaColors.IsDark
        SideEffect {
            libraryReads += isDark
            libraryComposed = true
        }
        Box(Modifier.fillMaxSize().background(ApturaColors.SurfaceBase)) {
            // Scrolled by the restoration test; far too short to reach the centre swatch otherwise.
            val list = androidx.compose.foundation.lazy.rememberLazyListState()
            listState = list
            androidx.compose.foundation.lazy.LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                items(100) { index -> androidx.compose.material3.Text("Row $index", modifier = Modifier.size(width = 200.dp, height = 48.dp)) }
            }
            Box(Modifier.size(80.dp).align(Alignment.Center).background(ApturaColors.SurfaceElevated1))
        }
    }

    @Composable
    private fun Player() {
        val isDark = ApturaColors.IsDark
        SideEffect {
            playerReads += isDark
            playerComposed = true
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.size(80.dp).align(Alignment.Center).background(ApturaColors.SurfaceElevated1))
        }
    }

    private fun show() {
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.LIGHT) {
                // As in the app: read in the app's theme, outside the player's.
                val appBackground = ApturaColors.SurfaceBase
                backStack = rememberNavBackStack(MediaLibrary)
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = { appTransition() },
                    popTransitionSpec = { appPopTransition() },
                    predictivePopTransitionSpec = { edge -> appPredictivePopTransition(edge) },
                    entryProvider = entryProvider {
                        entry<MediaLibrary> { Library() }
                        entry<PlayerKey>(metadata = PlayerRouteMetadata) {
                            ApturaTheme(themeMode = ThemeMode.DARK) {
                                PlayerRouteFrame(closingColor = appBackground) { Player() }
                            }
                        }
                    }
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** Steps the animation clock one 16 ms frame at a time and reports what is on screen after each. */
    private fun frames(count: Int, perFrame: (frame: Int) -> Unit) {
        composeRule.mainClock.autoAdvance = false
        repeat(count) { frame ->
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
            perFrame(frame)
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }

    @Test
    fun theLibraryStartsInTheLightTheme() {
        show()
        assertTrue(libraryComposed)
        assertEquals(listOf(false), libraryReads.distinct())
        val screen = composeRule.captureScreen()
        assertEquals(light.surfaceBase, screen.colorAt(2, 2))
    }

    @Test
    fun openingThePlayer_cutsToBlack_thenTheControlsRiseOutOfTheDark() {
        show()
        composeRule.runOnIdle { backStack.add(PlayerKey("content://video/1")) }

        val corners = mutableListOf<Color>()
        val swatches = mutableListOf<Color>()
        frames(30) {
            val screen = composeRule.captureScreen()
            corners += screen.colorAt(2, 2)
            swatches += screen.colorAt(screen.width / 2, screen.height / 2)
        }

        assertTrue("the player was composed", playerComposed)
        assertEquals("the library read only the light palette while it was on screen", listOf(false), libraryReads.distinct())
        assertEquals("the player read only the dark palette", listOf(true), playerReads.distinct())

        // The light page is replaced by black, never by a dark version of itself, and stays black.
        assertEquals(Color.Black, corners.last())
        corners.dropWhile { it != Color.Black }.forEach { assertEquals(Color.Black, it) }

        // The player's own swatch comes up from black and settles on the dark palette's colour: it is never light.
        val fromBlack = swatches.dropWhile { it.luminanceApprox() > 0.5f }
        assertTrue("the opening never reached the player", fromBlack.isNotEmpty())
        fromBlack.zipWithNext().forEach { (before, after) ->
            assertTrue("the swatch got darker while opening: $before then $after", after.luminanceApprox() >= before.luminanceApprox() - 0.001f)
        }
        assertEquals(dark.surfaceElevated1, swatches.last())
        fromBlack.forEach { assertTrue("$it is brighter than the dark palette allows", it.luminanceApprox() <= dark.surfaceElevated1.luminanceApprox() + 0.01f) }
    }

    @Test
    fun closingThePlayer_fadesItIntoThePageColour_thenShowsTheLibrary_withoutRecolouringEitherScreen() {
        show()
        composeRule.runOnIdle { backStack.add(PlayerKey("content://video/1")) }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        libraryReads.clear()
        playerReads.clear()

        composeRule.runOnIdle { backStack.removeLastOrNull() }
        val corners = mutableListOf<Color>()
        val swatches = mutableListOf<Color>()
        frames(30) {
            val screen = composeRule.captureScreen()
            corners += screen.colorAt(2, 2)
            swatches += screen.colorAt(screen.width / 2, screen.height / 2)
        }

        assertEquals("the library read only the light palette on its way back", listOf(false), libraryReads.distinct())
        assertTrue("the player is never re-themed while it leaves", playerReads.none { !it })

        // Settled: the library, in the light palette, and nothing of the player.
        assertEquals(light.surfaceBase, corners.last())
        assertEquals(light.surfaceElevated1, swatches.last())

        // The first frame is still the player. From there the picture only moves towards the page colour: the player's
        // black rises to the library's background, one smooth ramp, with no flash of any other colour.
        assertTrue("the first frame shows the player", corners.first().luminanceApprox() < 0.1f)
        corners.zipWithNext().forEach { (before, after) ->
            assertTrue("the page got darker while closing: $before then $after", after.luminanceApprox() >= before.luminanceApprox() - 0.001f)
        }
        // It is quick: the 120 ms cover is complete well inside 15 frames of 16 ms.
        val framesUntilPage = corners.indexOfFirst { it == light.surfaceBase }
        assertTrue("the page took $framesUntilPage frames to arrive", framesUntilPage in 1..15)
    }

    @Test
    fun returningFromThePlayer_keepsTheLibrarysScrollPosition() {
        show()
        composeRule.runOnIdle { kotlinx.coroutines.runBlocking { listState.scrollToItem(37, 12) } }
        composeRule.waitForIdle()
        assertEquals(37, listState.firstVisibleItemIndex)

        composeRule.runOnIdle { backStack.add(PlayerKey("content://video/1")) }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.runOnIdle { backStack.removeLastOrNull() }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        // The grid was disposed while the player was open and composed afresh; its position must have been restored in
        // that first composition, not scrolled back a frame later (which would show as a jump).
        assertEquals(37, listState.firstVisibleItemIndex)
        assertEquals(12, listState.firstVisibleItemScrollOffset)
    }

    private fun Color.luminanceApprox(): Float = (red + green + blue) / 3f
}
