package org.aptura.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SectionTitle` is shared by the Library header (title + count, no trailing slot) and the player sheets (title +
 * trailing action). F6 moved the free width onto the title so a long title is measured against the whole row; this
 * pins the two layouts that fix must not break.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1700dp-xhdpi")
class SectionTitleUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rowWidth = 320.dp

    @Test
    fun theCountSitsNextToTheTitle_whenThereIsNoTrailingSlot() {
        composeRule.setContent {
            ApturaTheme {
                Box(Modifier.width(rowWidth)) {
                    SectionTitle(title = "Library", count = 8)
                }
            }
        }
        val title = composeRule.onNodeWithText("Library").getUnclippedBoundsInRoot()
        val count = composeRule.onNodeWithText("8").getUnclippedBoundsInRoot()
        val gap = count.left - title.right
        assertTrue("count starts $gap after the title; it should follow it by the 8 dp spacer", gap in 0.dp..12.dp)
        // The title node must hug its text, not stretch: if it fills the row, the count is pushed to the far edge even
        // though its bounds still start 8 dp after the (invisible) end of the stretched title box.
        assertTrue("count starts at ${count.left} in a ${rowWidth} row; it should stay next to the title", count.left < rowWidth / 2)
    }

    @Test
    fun theTrailingSlotSitsAtTheEndOfTheRow() {
        composeRule.setContent {
            ApturaTheme {
                Box(Modifier.width(rowWidth)) {
                    SectionTitle(title = "Playback", count = 3, trailing = { Text("Reset") })
                }
            }
        }
        val title = composeRule.onNodeWithText("Playback").getUnclippedBoundsInRoot()
        val count = composeRule.onNodeWithText("3").getUnclippedBoundsInRoot()
        val trailing = composeRule.onNodeWithText("Reset").getUnclippedBoundsInRoot()
        val gap = count.left - title.right
        assertTrue("count starts $gap after the title; it should follow it by the 8 dp spacer", gap in 0.dp..12.dp)
        val trailingGap = rowWidth - trailing.right
        assertTrue("trailing ends $trailingGap before the row edge; it should sit at the end", trailingGap in 0.dp..1.dp)
        assertTrue("trailing overlaps the count", trailing.left > count.right)
    }
}
