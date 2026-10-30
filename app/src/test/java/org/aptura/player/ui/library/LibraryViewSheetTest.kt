package org.aptura.player.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The view sheet reports every choice it offers; what it offers is the library's whole layout vocabulary. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1400dp-xhdpi")
class LibraryViewSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var prefs by mutableStateOf(LibraryViewPrefs())
    private var sort by mutableStateOf(LibrarySortOrder.DATE_ADDED_DESC)

    private fun show() {
        composeRule.setContent {
            ApturaTheme {
                LibraryViewSheetContent(
                    prefs = prefs,
                    sortOrder = sort,
                    onPrefsChange = { prefs = it },
                    onSortChange = { sort = it }
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun itOffersLayoutSizeDetailsAndSort() {
        show()
        listOf("Grid", "List", "Compact", "Comfortable", "Large", "Title only", "Standard", "Detailed").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
        LibrarySortOrder.entries.forEach { composeRule.onNodeWithText(it.displayName).assertIsDisplayed() }
    }

    @Test
    fun everyChoice_isReported() {
        show()

        composeRule.onNodeWithText("List").performClick()
        assertEquals(LibraryLayout.LIST, prefs.layout)

        composeRule.onNodeWithText("Large").performClick()
        assertEquals(LibraryDensity.LARGE, prefs.density)

        composeRule.onNodeWithText("Detailed").performClick()
        assertEquals(LibraryDetail.DETAILED, prefs.detail)

        composeRule.onNodeWithText(LibrarySortOrder.SIZE_DESC.displayName).performClick()
        assertEquals(LibrarySortOrder.SIZE_DESC, sort)

        // The earlier choices are kept when a later one changes.
        assertEquals(LibraryLayout.LIST, prefs.layout)
    }

    @Test
    fun theDetailsCaption_saysWhatTheLevelShows() {
        show()
        composeRule.onNodeWithText("Title, size and folder.").assertIsDisplayed()
        composeRule.onNodeWithText("Title only").performClick()
        composeRule.onNodeWithText("Just the title.").assertIsDisplayed()
    }
}
