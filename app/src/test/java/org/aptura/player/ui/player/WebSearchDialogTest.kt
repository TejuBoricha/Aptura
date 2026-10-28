package org.aptura.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The subtitle web search sends the (editable) file-name-derived text to a third-party site through the browser, while
 * the rest of the app promises "100% offline". The dialog has to say what leaves the device, and must not push one
 * third-party provider as "Recommended".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1200dp") // the dialog is tall; Robolectric's default screen would clip it
class WebSearchDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(onSearch: (String, Int) -> Unit = { _, _ -> }) {
        composeRule.setContent {
            ApturaTheme {
                WebSearchDialog(initialQuery = "Some Movie 2020", onDismissRequest = {}, onLaunchSearch = onSearch)
            }
        }
    }

    @Test
    fun theDialogDisclosesThatTextLeavesTheDeviceThroughTheBrowser() {
        show()

        composeRule.onNodeWithText(WEB_SEARCH_DISCLOSURE).assertIsDisplayed()
        assert(WEB_SEARCH_DISCLOSURE.contains("browser") && WEB_SEARCH_DISCLOSURE.contains("not your video"))
    }

    @Test
    fun noProviderIsLabelledRecommended() {
        show()

        assertEquals(0, composeRule.onAllNodesWithText("Recommended", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun searchStillHandsTheEditableQueryToTheChosenProvider() {
        var query: String? = null
        var provider: Int? = null
        show { q, p -> query = q; provider = p }

        composeRule.onNodeWithText("OpenSubtitles Web").performClick()
        composeRule.onNodeWithText("Search in browser").performClick()

        assertEquals("Some Movie 2020", query)
        assertEquals(1, provider)
    }
}
