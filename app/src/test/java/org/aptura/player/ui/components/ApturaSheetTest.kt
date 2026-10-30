package org.aptura.player.ui.components

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.ui.library.FolderBrowserSheet
import org.aptura.player.ui.library.FolderSummary
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

    private fun showFolders() {
        val folders = (1..14).map { "/storage/emulated/0/Folder$it" }
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
                    onSelect = {},
                    onAddFolder = {},
                    onDismiss = {}
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
    fun flickingDownFromTheTop_stillClosesTheSheet() {
        showFolders()
        val resting = headingTop()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasScrollAction()).performTouchInput { swipeDown(durationMillis = 60) }
        val seen = watchHeading(45)

        assertTrue("the sheet did not follow a flick down, it stayed at ${seen.last()} dp (rest $resting dp)", seen.last() > resting + CLOSED)
    }

    @Test
    fun aShortQuickFlickDownFromTheTop_closesTheSheetBySpeedAlone() {
        showFolders()
        val resting = headingTop()
        composeRule.mainClock.autoAdvance = false
        // 40 dp is too short to count as "dragged far enough"; only the speed says the finger meant to throw it away.
        composeRule.onNode(hasScrollAction()).performTouchInput {
            swipeDown(startY = height * 0.3f, endY = height * 0.3f + 40.dp.toPx(), durationMillis = 40)
        }
        val seen = watchHeading(60)

        assertTrue("a quick short flick down left the sheet at ${seen.last()} dp (rest $resting dp): $seen", seen.last() > resting + CLOSED)
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

        /** Well past the resting place: gone, not just nudged. */
        const val CLOSED = 300f
    }
}
