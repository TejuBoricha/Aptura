package org.aptura.player.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.SemanticsMatcher
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
 * "If you scroll too far up the screen starts dancing" (Folders sheet, Samsung S24), and then "the touch does not feel
 * responsive", and then "unable to pull the player's options when the phone is tilted".
 *
 * A sheet that is flicked upward must stay where it is, whether the finger is on a long list, on a short list that fits
 * the screen or on the title; but it must still be pulled down and closed by a flick down, taps must still tap (even when
 * the finger drifts a little upward) and lists must still scroll. The sheet's heading is the thing to watch: after a
 * flick it must never be anywhere but where it started, frame after frame.
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

    /** The list of folders: the one node that reports a scroll position (the sheet's own scrolling wrapper moves nothing). */
    private val theList = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

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
        composeRule.onNode(theList).performTouchInput { swipeUp(durationMillis = 100) }
    }

    /** A drag that starts on the heading, which is no list: the finger is on the sheet itself. */
    private fun dragOnTheHeading(durationMs: Long, vararg moves: Float) {
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Folders").performTouchInput {
            down(center)
            moves.forEach { dyDp -> moveBy(Offset(0f, dyDp.dp.toPx()), delayMillis = durationMs) }
            up()
        }
    }

    private fun assertStill(seen: List<Float>, resting: Float, what: String) {
        val worst = seen.maxOf { Math.abs(it - resting) }
        assertTrue("the sheet moved by up to $worst dp $what (resting at $resting dp): $seen", worst <= STILL)
    }

    @Test
    fun flickingTheListUpToItsEnd_doesNotMoveTheSheet() {
        showFolders()
        val resting = headingTop()
        flingUp()
        assertStill(watchHeading(45), resting, "after a flick to the end of the list")
    }

    @Test
    fun flickingAgainWhenAlreadyAtTheEnd_doesNotMoveTheSheet() {
        showFolders()
        flingUp()
        watchHeading(60)
        val resting = headingTop()
        flingUp()
        assertStill(watchHeading(45), resting, "on a second flick")
    }

    @Test
    fun flickingUpOnTheHeading_ofAShortList_doesNotMoveTheSheet() {
        // The owner's phone: seven folders that fit on the screen, so an upward flick on the title moves the sheet itself.
        showFolders(count = 8)
        val resting = headingTop()
        dragOnTheHeading(30, -60f, -60f)
        assertStill(watchHeading(60), resting, "when flicked upward on its title")
    }

    @Test
    fun flickingUpOnTheHandle_doesNotMoveTheSheet() {
        showFolders(count = 8)
        val resting = headingTop()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Folders").performTouchInput {
            // The handle sits above the title: start there.
            down(Offset(centerX, -50.dp.toPx()))
            moveBy(Offset(0f, -40.dp.toPx()), delayMillis = 25)
            moveBy(Offset(0f, -40.dp.toPx()), delayMillis = 25)
            up()
        }
        assertStill(watchHeading(60), resting, "when flicked upward on its handle")
    }

    @Test
    fun flickingDownFromTheTopOfTheList_stillClosesTheSheet() {
        showFolders()
        val resting = headingTop()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(theList).performTouchInput { swipeDown(durationMillis = 60) }
        val seen = watchHeading(45)

        assertTrue("the sheet did not follow a flick down, it stayed at ${seen.last()} dp (rest $resting dp)", seen.last() > resting + CLOSED)
    }

    @Test
    fun aShortQuickFlickDownFromTheTop_closesTheSheetBySpeedAlone() {
        showFolders()
        val resting = headingTop()
        composeRule.mainClock.autoAdvance = false
        // 40 dp is too short to count as "dragged far enough"; only the speed says the finger meant to throw it away.
        composeRule.onNode(theList).performTouchInput {
            swipeDown(startY = height * 0.3f, endY = height * 0.3f + 40.dp.toPx(), durationMillis = 40)
        }
        val seen = watchHeading(60)

        assertTrue("a quick short flick down left the sheet at ${seen.last()} dp (rest $resting dp): $seen", seen.last() > resting + CLOSED)
    }

    @Test
    fun draggingDownOnTheHeading_stillClosesTheSheet() {
        showFolders(count = 8)
        val resting = headingTop()
        dragOnTheHeading(40, 150f, 250f)
        val seen = watchHeading(90)

        assertTrue("the sheet did not follow a pull down, it stayed at ${seen.last()} dp (rest $resting dp)", seen.last() > resting + CLOSED)
    }

    @Test
    fun pullingTheSheetDown_thenBackUp_comesBackWithoutRisingAboveWhereItStarted() {
        showFolders(count = 8)
        val resting = headingTop()
        // Down 40 dp, then quickly up 30 dp, and let go: the kind of wobble a thumb makes.
        dragOnTheHeading(40, 40f, -30f)
        val seen = watchHeading(90)

        val highest = seen.min()
        assertTrue("the sheet rose to $highest dp, above where it started ($resting dp): $seen", highest >= resting - STILL)
        assertTrue("and it came back to rest at the end (${seen.last()} dp)", Math.abs(seen.last() - resting) <= STILL)
    }

    /** The owner's report on the second fix: "the touch does not feel responsive, scrolling sometimes does not react". */
    @Test
    fun aTapThatDriftsUpwardABit_stillTapsTheFolder() {
        showFolders(count = 8)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Folder3").performTouchInput {
            down(center)
            moveBy(Offset(0f, -3.dp.toPx()), delayMillis = 30)
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
            composeRule.onNode(theList).performTouchInput { swipeUp(durationMillis = 120) }
            watchHeading(40)
            // A short pull back, which does not reach the top (that would close the sheet, which is its job).
            composeRule.onNode(theList).performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.3f + 60.dp.toPx(), durationMillis = 120) }
            watchHeading(40)
        }
        composeRule.onNode(theList).performTouchInput { swipeUp(durationMillis = 120) }
        watchHeading(40)
        val position = composeRule.onNode(theList).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value?.invoke() ?: 0f

        assertTrue("the list did not scroll after repeated flicks ($position)", position > 0f)
    }

    /**
     * The owner's phone, tilted: the player's Subtitles sheet opens half way, with its options below the screen, and is
     * pulled up to reach them. One build switched that pulling off, so the options could not be reached at all.
     */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    fun aHalfOpenSheet_onATiltedPhone_canBePulledUp() {
        composeRule.setContent {
            ApturaTheme {
                org.aptura.player.ui.player.SubtitleSheet(
                    uiState = org.aptura.player.ui.player.PlayerUiState(),
                    onDismissRequest = { dismissed = true },
                    onSubtitleTrackSelected = {},
                    onSubtitleDelayChanged = {},
                    onImportSubtitleClick = {},
                    onSubtitleFontSizeChanged = {},
                    onSubtitleTextColorChanged = {},
                    onSubtitleBackgroundChanged = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_000)
        val before = composeRule.onNodeWithText("Subtitles").getUnclippedBoundsInRoot().top.value
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Subtitles").performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(0f, -30.dp.toPx()), delayMillis = 16) }
            up()
        }
        val seen = List(60) {
            composeRule.mainClock.advanceTimeBy(16)
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Subtitles").getUnclippedBoundsInRoot().top.value
        }

        assertTrue("the half-open sheet did not rise when pulled up (title at $before dp, then ${seen.last()} dp)", seen.last() < before - 20f)
        assertTrue("it rose past where it should stop and came back: $seen", seen.min() >= seen.last() - STILL)
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
        /** Sub-pixel layout noise is fine; the dance was 30 to 85 dp. */
        const val STILL = 1f

        /** Well past the resting place: gone, not just nudged. */
        const val CLOSED = 300f
    }
}
