package org.aptura.player.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerSheetChromeAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playerSheetTabs_exposesCollectionInfo_andEachTabExposesCollectionItemInfo() {
        var selectedIndex by mutableIntStateOf(0)
        val tabs = listOf(
            "Tracks" to Icons.Default.Sync,
            "Equalizer" to Icons.Default.GraphicEq,
            "Effects" to Icons.Default.SurroundSound
        )

        composeRule.setContent {
            ApturaTheme {
                PlayerSheetTabs(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it }
                )
            }
        }

        // Test each tab's semantics
        tabs.forEachIndexed { index, (label, _) ->
            val node = composeRule.onNodeWithText(label).fetchSemanticsNode()
            assertEquals("Role should be Tab", Role.Tab, node.config[SemanticsProperties.Role])
            assertEquals("Selected state", index == 0, node.config[SemanticsProperties.Selected])

            val itemInfo = node.config[SemanticsProperties.CollectionItemInfo]
            assertEquals(0, itemInfo.rowIndex)
            assertEquals(1, itemInfo.rowSpan)
            assertEquals(index, itemInfo.columnIndex)
            assertEquals(1, itemInfo.columnSpan)
        }

        // Select the second tab
        composeRule.onNodeWithText("Equalizer").performClick()
        composeRule.waitForIdle()

        assertEquals(1, selectedIndex)
        val eqNode = composeRule.onNodeWithText("Equalizer").fetchSemanticsNode()
        assertTrue(eqNode.config[SemanticsProperties.Selected])

        val tracksNode = composeRule.onNodeWithText("Tracks").fetchSemanticsNode()
        assertFalse(tracksNode.config[SemanticsProperties.Selected])
    }

    @Test
    fun apturaSlider_reportsLabel_range_andRespondsToSetProgress() {
        var sliderValue by mutableFloatStateOf(0f)

        composeRule.setContent {
            ApturaTheme {
                ApturaSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = -500f..500f,
                    label = "Audio sync",
                    valueText = "${sliderValue.toInt()} ms",
                    origin = 0f
                )
            }
        }

        val sliderNode = composeRule.onNodeWithContentDescription("Audio sync")
        val semantics = sliderNode.fetchSemanticsNode()

        assertEquals("0 ms", semantics.config[SemanticsProperties.StateDescription])
        val rangeInfo = semantics.config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(0f, rangeInfo.current, 0.01f)
        assertEquals(-500f, rangeInfo.range.start, 0.01f)
        assertEquals(500f, rangeInfo.range.endInclusive, 0.01f)

        // Perform setProgress semantics action (TalkBack swipe / volume adjustment)
        sliderNode.performSemanticsAction(SemanticsActions.SetProgress) { it(-200f) }
        composeRule.waitForIdle()

        assertEquals(-200f, sliderValue, 0.01f)
    }
}
