package org.aptura.player.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The choice controls are radio buttons for TalkBack, report their choice, and are never smaller than a touch target. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ChoiceControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var selected by mutableIntStateOf(1)

    private fun showSwitch(selectedIndex: Int = 1) {
        selected = selectedIndex
        composeRule.setContent {
            ApturaTheme {
                TonalSegmentedPicker(
                    options = listOf("Compact", "Comfortable", "Large"),
                    selectedIndex = selected,
                    onSelect = { selected = it },
                    label = "Size"
                )
            }
        }
        composeRule.waitForIdle()
    }

    private val isRadioButton = SemanticsMatcher("is a radio button") { it.config.getOrNull(SemanticsProperties.Role) == Role.RadioButton }

    @Test
    fun everyOptionIsARadioButton_andTheChosenOneIsSelected() {
        showSwitch()
        composeRule.onAllNodes(isRadioButton).assertCountEquals3()
        composeRule.onNodeWithText("Comfortable").assertIsSelected()
        composeRule.onNodeWithText("Compact").assertIsNotSelected()
        composeRule.onNodeWithText("Large").assertIsNotSelected()
    }

    @Test
    fun choosing_reportsTheIndex_andMovesTheSelection() {
        showSwitch()
        composeRule.onNodeWithText("Large").performClick()
        assertEquals(2, selected)
        composeRule.onNodeWithText("Large").assertIsSelected()
        composeRule.onNodeWithText("Comfortable").assertIsNotSelected()
    }

    @Test
    fun aStoredValueThatIsNotAmongTheOptions_selectsNothing() {
        showSwitch(selectedIndex = -1)
        listOf("Compact", "Comfortable", "Large").forEach { composeRule.onNodeWithText(it).assertIsNotSelected() }
    }

    @Test
    fun everyOptionIsAtLeastAsTallAsATouchTarget() {
        showSwitch()
        listOf("Compact", "Comfortable", "Large").forEach {
            val bounds = composeRule.onNode(isRadioButton and androidx.compose.ui.test.hasText(it)).getUnclippedBoundsInRoot()
            assertTrue("$it is ${bounds.height} tall", bounds.height >= 48.dp)
        }
    }

    @Test
    fun aChoiceCard_isARadioButtonWithItsNameAndReportsItsChoice() {
        var chosen = "Grid"
        composeRule.setContent {
            ApturaTheme {
                var current by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Grid") }
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    listOf("Grid", "List").forEach { name ->
                        ChoiceCard(
                            label = name,
                            selected = current == name,
                            onClick = { current = name; chosen = name },
                            modifier = Modifier.weight(1f)
                        ) { }
                    }
                }
            }
        }
        composeRule.onNodeWithText("Grid").assertIsSelected()
        composeRule.onNodeWithText("List").assertIsNotSelected()
        composeRule.onNodeWithText("List").performClick()
        assertEquals("List", chosen)
        composeRule.onNodeWithText("List").assertIsSelected()
        val bounds = composeRule.onNode(isRadioButton and androidx.compose.ui.test.hasText("List")).getUnclippedBoundsInRoot()
        assertTrue("a card is ${bounds.height} tall", bounds.height >= 48.dp)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals3() =
        assertEquals(3, fetchSemanticsNodes().size)
}
