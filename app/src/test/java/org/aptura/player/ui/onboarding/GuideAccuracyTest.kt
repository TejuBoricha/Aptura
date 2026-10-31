package org.aptura.player.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.VideoCardCallbacks
import org.aptura.player.ui.library.VideoGridCard
import org.aptura.player.ui.visual.captureScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The guide must not describe something the app does not have (the owner's point 4: "verify every step"). The ⋮ menu
 * line once promised "play audio only", "open in another player" and "rename", none of which that menu offers.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class GuideAccuracyTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theRealOverflowMenu_offersEveryActionTheGuideNames() {
        val everything = VideoCardCallbacks(
            onClick = {},
            onMore = {},
            onPlayNext = {},
            onAddToQueue = {},
            onHide = {},
            onDelete = {},
            onMoreFileActions = {},
            onShare = {},
            onMove = {},
            onSelect = {}
        )
        val card = GuideSamples.first().card
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.DARK) {
                VideoGridCard(card = card, callbacks = everything)
            }
        }
        composeRule.onNodeWithContentDescription("More actions for ${card.title}").performClick()
        composeRule.waitForIdle()

        GuideMenuActions.forEach { action ->
            composeRule.onNodeWithText(action).assertExists("the guide names \"$action\", but the ⋮ menu has no such action")
        }
        composeRule.onNodeWithText("Video details").assertExists()
    }

    @Test
    fun theLibraryFeatureLine_isBuiltFromThoseSameActionNames() {
        val menuLine = LibraryFeatures.single { it.first.contains("⋮") }.second
        GuideMenuActions.forEach { assertTrue("\"$it\" is missing from the guide's menu line", menuLine.contains(it)) }
        // These two live in Video details, not in the menu, and the line says so.
        assertTrue(menuLine.contains("Video details"))
        assertTrue(menuLine.contains("Rename") && menuLine.contains("Open with"))
    }

    @Test
    fun theLibraryFeatures_useTheLibrarysOwnNameForTheShelf() {
        assertTrue(LibraryFeatures.any { it.first == org.aptura.player.ui.library.CONTINUE_WATCHING_TITLE })
    }

    @Test
    fun everyFeatureListIsFreeOfDuplicatesAndBlanks() {
        listOf(LibraryFeatures, ExtraGestures).forEach { list ->
            assertEquals(list.size, list.map { it.first }.toSet().size)
            assertTrue(list.all { it.first.isNotBlank() && it.second.isNotBlank() })
        }
    }

    @Test
    fun playerDemoDoubleTapCaption_accuratelyDescribesConfiguredSeekStep() {
        listOf(5, 10, 15, 30).forEach { seconds ->
            val caption = PlayerDemoStep.DOUBLE_TAP.caption(seconds)
            assertTrue("caption '$caption' must contain '$seconds seconds'", caption.contains("$seconds seconds"))
        }
    }

    // ---- the drawn film frames ------------------------------------------------------------------------------------

    private var shownSeed by mutableIntStateOf(0)
    private var stillShown = false

    private fun stillSample(seed: Int): List<Int> {
        if (!stillShown) {
            stillShown = true
            composeRule.setContent { Box(Modifier.size(120.dp, 68.dp)) { GuideStill(seed = shownSeed) } }
        }
        shownSeed = seed
        composeRule.waitForIdle()
        val bounds = composeRule.onNodeWithTag(guideStillTag(seed), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val screen = composeRule.captureScreen()
        val points = ArrayList<Int>()
        for (row in 1..8) {
            for (column in 1..8) {
                val x = (bounds.left + bounds.width * column / 9f).toInt()
                val y = (bounds.top + bounds.height * row / 9f).toInt()
                points += screen.colorAt(x, y).hashCode()
            }
        }
        return points
    }

    @Test
    fun aFilmFrame_isNeverFlat() {
        (0 until 6).forEach { seed ->
            val colours = stillSample(seed).toSet()
            assertTrue("scene $seed is flat (${colours.size} colours)", colours.size > 12)
        }
    }

    @Test
    fun theScenes_areDistinct_andEachSceneIsDeterministic() {
        val first = stillSample(2)
        assertEquals("the same seed draws the same picture", first, stillSample(2))
        assertNotEquals("different seeds draw different pictures", first, stillSample(3))
        // Seed 8 reuses seed 2's colours, but its hills are drawn differently: two films never look identical.
        assertNotEquals("a reused palette still draws its own hills", first, stillSample(8))
    }
}
