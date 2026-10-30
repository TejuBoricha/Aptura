package org.aptura.player.ui.components

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.ui.library.FolderBrowserSheet
import org.aptura.player.ui.library.FolderSummary
import androidx.compose.ui.semantics.getOrNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * "If you scroll too far up the screen starts dancing" (Folders sheet, Samsung S24).
 *
 * Flicking a long list up to its end used to lift the whole sheet by about 85 dp and drop it again. The sheet's heading
 * is the thing to watch: after the flick it must never be anywhere but where it started, frame after frame. And the
 * sheet must still close when it is flicked down from the top of the list, so the guard cannot be "ignore every fling".
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ApturaSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var dismissed = false
    private var selected: List<String?> = emptyList()

    private fun showFolders(count: Int = 14) {
        val folders = (1..count).map { "/storage/emulated/0/Folder$it" }
        composeRule.setContent {
            ApturaTheme {
                FolderBrowserSheet(
                    folders = folders,
                    labels = folders.associateWith { it.substringAfterLast('/') },
                    summaries = folders.associateWith { FolderSummary(5, 1_000_000L, 600_000L) },
                    libraryVideoCount = 70,
                    libraryDurationMs = 1_000_000L,
                    librarySizeBytes = 5_000_000L,
                    selectedFolder = null,
                    layout = FolderLayout.LIST,
                    onLayoutChange = {},
                    onSelect = { selected = selected + it },
                    onAddFolder = {},
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_000)
    }

    private fun headingTop() = composeRule.onNodeWithText("Folders").getUnclippedBoundsInRoot().top.value

    /** The heading's top after each of [frames] 16 ms frames. */
    private fun watchHeading(frames: Int): List<Float> {
        composeRule.mainClock.autoAdvance = false
        return List(frames) {
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
            headingTop()
        }
    }

    private fun flingUp() {
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp(durationMillis = 100) }
    }

    @Test
    fun flickingTheListUpToItsEnd_doesNotMoveTheSheet() {
        showFolders()
        val resting = headingTop()
        flingUp()
        val seen = watchHeading(45)

        val worst = seen.maxOf { Math.abs(it - resting) }
        assertTrue("the sheet moved by up to $worst dp after a flick to the end of the list (resting at $resting dp): $seen", worst <= STILL)
    }

    @Test
    fun flickingAgainWhenAlreadyAtTheEnd_doesNotMoveTheSheet() {
        showFolders()
        flingUp()
        watchHeading(60)
        val resting = headingTop()
        flingUp()
        val seen = watchHeading(45)

        val worst = seen.maxOf { Math.abs(it - resting) }
        assertTrue("the sheet moved by up to $worst dp on a second flick (resting at $resting dp): $seen", worst <= STILL)
    }

    @Test
    fun flickingDownOnTheList_atItsTop_doesNotMoveTheSheet() {
        showFolders()
        val resting = headingTop()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasScrollAction()).performTouchInput { swipeDown(durationMillis = 60) }
        val seen = watchHeading(45)

        val worst = seen.maxOf { Math.abs(it - resting) }
        assertTrue("the sheet moved by up to $worst dp when the list was flicked down (resting at $resting dp): $seen", worst <= STILL)
        assertTrue("the sheet closed by itself", !dismissed)
    }

    @Test
    fun swipingTheHandleDown_closesTheSheet() {
        showFolders(count = 8)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(SHEET_HANDLE_TAG).performTouchInput {
            swipeDown(startY = top, endY = top + 120.dp.toPx(), durationMillis = 300)
        }
        watchHeading(90)

        assertTrue("the sheet was not closed by a swipe down on its handle", dismissed)
    }

    @Test
    fun aShortQuickFlickDownOnTheHandle_closesTheSheetBySpeedAlone() {
        showFolders(count = 8)
        composeRule.mainClock.autoAdvance = false
        // 32 dp is too short to count as "pulled far enough" (the closing distance is 40 dp after the touch slop); only
        // the speed says the finger meant to throw it away.
        composeRule.onNodeWithTag(SHEET_HANDLE_TAG).performTouchInput {
            down(androidx.compose.ui.geometry.Offset(centerX, 4.dp.toPx()))
            repeat(8) { moveBy(androidx.compose.ui.geometry.Offset(0f, 4.dp.toPx()), delayMillis = 8) }
            up()
        }
        watchHeading(90)

        assertTrue("a quick short flick down on the handle did not close the sheet", dismissed)
    }

    @Test
    fun tappingTheHandle_closesTheSheet() {
        showFolders(count = 8)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(SHEET_HANDLE_TAG).performClick()
        watchHeading(90)

        assertTrue("a tap on the handle did not close the sheet", dismissed)
    }

    /** The owner's report on the first fix: "the touch does not feel responsive, scrolling sometimes does not react". */
    @Test
    fun aTapThatDriftsUpwardABit_stillTapsTheFolder() {
        showFolders(count = 8)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Folder3").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(0f, -3.dp.toPx()), delayMillis = 30)
            up()
        }
        watchHeading(10)

        assertTrue("the folder was not selected by a tap that drifted upward by 3 dp: $selected", selected.isNotEmpty())
    }

    @Test
    fun everyScrollOnTheList_scrollsIt() {
        showFolders()
        composeRule.mainClock.autoAdvance = false
        repeat(3) {
            composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp(durationMillis = 120) }
            watchHeading(40)
            composeRule.onNode(hasScrollAction()).performTouchInput { swipeDown(durationMillis = 120) }
            watchHeading(40)
        }
        composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp(durationMillis = 120) }
        watchHeading(40)
        val position = composeRule.onNode(hasScrollAction()).fetchSemanticsNode().config
            .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)?.value?.invoke() ?: 0f

        assertTrue("the list did not scroll after repeated flicks ($position)", position > 0f)
    }

    /** A drag that starts on the heading, which is no list: the finger is on the sheet itself. */
    private fun dragOnTheHeading(durationMs: Long, vararg moves: Float) {
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Folders").performTouchInput {
            down(center)
            var t = 0L
            moves.forEach { dyDp ->
                t += durationMs
                moveBy(androidx.compose.ui.geometry.Offset(0f, dyDp.dp.toPx()), delayMillis = durationMs)
            }
            up()
        }
    }

    @Test
    fun flickingUpOnTheHeading_ofAShortList_doesNotMoveTheSheet() {
        // The owner's phone: eight folders that fit on the screen, so an upward flick moves the sheet itself.
        showFolders(count = 8)
        val resting = headingTop()
        dragOnTheHeading(30, -60f, -60f)
        val seen = watchHeading(60)

        val worst = seen.maxOf { Math.abs(it - resting) }
        assertTrue("the sheet moved by up to $worst dp when flicked upward (resting at $resting dp): $seen", worst <= STILL)
    }

    @Test
    fun draggingTheHeadingDownAndUp_doesNotMoveTheSheet() {
        showFolders(count = 8)
        val resting = headingTop()
        dragOnTheHeading(40, 120f, -80f)
        val seen = watchHeading(60)

        val worst = seen.maxOf { Math.abs(it - resting) }
        assertTrue("the sheet moved by up to $worst dp from a drag on its content (resting at $resting dp): $seen", worst <= STILL)
        assertTrue(!dismissed)
    }

    /** Every bottom sheet in the app comes from [ApturaModalBottomSheet]; a plain Material one would dance again. */
    @Test
    fun noScreenOpensAPlainMaterialSheet() {
        val sources = File("src/main/java/org/aptura/player").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("could not find the app's sources from ${File(".").absolutePath}", sources.size > 50)
        val plain = sources
            .filter { it.name != "ApturaSheet.kt" }
            .filter { file -> file.readLines().any { Regex("""(?<![A-Za-z])ModalBottomSheet\(""").containsMatchIn(it) } }
            .map { it.name }
        assertTrue("these open a plain ModalBottomSheet instead of ApturaModalBottomSheet: $plain", plain.isEmpty())
    }

    private companion object {
        /** Sub-pixel layout noise is fine; the dance was 85 dp. */
        const val STILL = 1f

    }
}
