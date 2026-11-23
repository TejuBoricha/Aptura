package org.aptura.player.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.ui.components.SkeletonBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit and UI tests for RenameVideoDialog (A11Y-6), SkeletonBlock (A11Y-8),
 * and LibraryVideoPane refresh semantics (LIB-19).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RenameVideoDialogAndSkeletonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun renameVideoDialog_displaysVideoTitleLabel_andHandlesConfirm() {
        var confirmedName: String? = null
        var dismissed = false

        composeTestRule.setContent {
            ApturaTheme {
                RenameVideoDialog(
                    itemTitle = "Original Movie",
                    onConfirm = { confirmedName = it },
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Verify title & label
        composeTestRule.onNodeWithText("Rename video").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video title").assertIsDisplayed()

        // Edit text to a valid new name
        val inputField = composeTestRule.onNodeWithText("Original Movie")
        inputField.performTextReplacement("New Movie Name")

        // Click confirm
        composeTestRule.onNodeWithText("Rename").performClick()
        assertEquals("New Movie Name", confirmedName)
    }

    @Test
    fun skeletonBlock_rendersWithoutCrash() {
        composeTestRule.setContent {
            ApturaTheme {
                Box(modifier = Modifier.size(200.dp)) {
                    SkeletonBlock(modifier = Modifier.size(100.dp))
                }
            }
        }
    }

    @Test
    fun libraryVideoPane_rescanningSpinner_hasAccessibleSemantics() {
        composeTestRule.setContent {
            ApturaTheme {
                LibraryHeader(
                    query = "",
                    scopeName = null,
                    isRefreshing = true,
                    showRefreshAndSettings = true,
                    pagePadding = 16.dp,
                    onQueryChange = {},
                    onSearchDone = {},
                    onViewOptions = {},
                    onRefresh = {},
                    onSettingsClick = {}
                )
            }
        }

        // When refreshing, the spinner must announce "Rescanning library" (audit LIB-19)
        composeTestRule.onNodeWithContentDescription("Rescanning library").assertIsDisplayed()
    }
}
