package org.aptura.player.ui.library

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The tablet library is not just a wider phone: above 700 dp the icon rail becomes a collection index with
 * whole-library figures, folder navigation, maintenance actions and the offline promise. These tests pin that
 * information architecture and the large-font fallback that keeps the rail from crushing folder names.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w900dp-h1100dp-xhdpi")
class LibrarySidebarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val videos = listOf("Alien", "Arrival", "Dune", "Heat").mapIndexed { index, title ->
        val item = DomainMediaItem(
            uri = "content://media/$index",
            title = title,
            durationMs = 5_400_000L,
            sizeBytes = 1_073_741_824L,
            mimeType = "video/mp4",
            folderPath = if (index < 3) "Movies" else "Shows"
        )
        VideoCardUiModel(
            uri = item.uri,
            title = item.title,
            durationFormatted = "1:30:00",
            fileSizeFormatted = "1 GB",
            folderName = item.folderPath,
            rawItem = item
        )
    }

    private fun state() = MediaLibraryUiState.Success(
        videos = videos,
        folders = listOf("Movies", "Shows"),
        totalCount = videos.size,
        libraryVideoCount = 9,
        libraryDurationMs = 30_600_000L,
        librarySizeBytes = 5_368_709_120L,
        folderCounts = mapOf("Movies" to 7, "Shows" to 2)
    )

    @Test
    fun wideScreen_showsTheCollectionIndexInsteadOfFolderChips() {
        var selectedFolder: String? = "not-called"
        var addFolderClicked = false
        var openFileClicked = false
        var refreshClicked = false

        composeRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = state(),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = { selectedFolder = it },
                    onSortOrderChanged = {},
                    onRefresh = { refreshClicked = true },
                    onOpenFilePicker = { openFileClicked = true },
                    onAddFolderClick = { addFolderClicked = true }
                )
            }
        }

        // Whole-library pulse: count, runtime and footprint are the reason the column earns its width.
        // "9" appears twice: the collection pulse and the "All videos" row.
        composeRule.onAllNodesWithText("9").assertCountEquals(2)
        composeRule.onNodeWithText("films").assertIsDisplayed()
        composeRule.onNodeWithText("COLLECTIONS").assertIsDisplayed()
        composeRule.onNodeWithText("8h 30m total  ·  5 GB").assertIsDisplayed()

        // Folder index, including per-folder counts.
        composeRule.onNodeWithText("All videos").assertIsDisplayed()
        composeRule.onNodeWithText("Movies").assertIsDisplayed()
        composeRule.onNodeWithText("Shows").assertIsDisplayed()
        composeRule.onNodeWithText("7").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()

        // The chips duplicate the sidebar; the top edge stays quiet.
        composeRule.onAllNodesWithText("Movies").assertCountEquals(1)

        // Tools and privacy promise stay reachable.
        composeRule.onNodeWithText("Open a file").assertIsDisplayed()
        composeRule.onNodeWithText("Rescan library").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("100% offline").assertIsDisplayed()
        composeRule.onNodeWithText("No account. No telemetry.").assertIsDisplayed()

        // The card menu names its video, which matters even more in a wide grid.
        composeRule.onNodeWithContentDescription("More actions for Alien").assertIsDisplayed()

        composeRule.onNodeWithText("Movies").performClick()
        assertEquals("Movies", selectedFolder)

        composeRule.onNodeWithContentDescription("Add a folder").performClick()
        assertTrue(addFolderClicked)

        composeRule.onNodeWithText("Open a file").performClick()
        assertTrue(openFileClicked)

        composeRule.onNodeWithText("Rescan library").performClick()
        assertTrue(refreshClicked)

        // Collapsing hands the width back to the grid, and the chips return because the rail has no folder list.
        composeRule.onNodeWithContentDescription("Collapse sidebar").performClick()
        composeRule.onAllNodesWithText("COLLECTIONS").assertCountEquals(0)
        composeRule.onAllNodesWithText("All videos").assertCountEquals(0)
        composeRule.onNodeWithText("All Videos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Expand sidebar").assertIsDisplayed()

        // Expanding restores the index without losing the page.
        composeRule.onNodeWithContentDescription("Expand sidebar").performClick()
        composeRule.onNodeWithText("COLLECTIONS").assertIsDisplayed()
        composeRule.onNodeWithText("All videos").assertIsDisplayed()
    }

    @Test
    fun wideScreenAtLargeFont_compressesToTheIconRail() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f)
            ) {
                ApturaTheme {
                    MediaLibraryScreen(
                        uiState = state(),
                        onVideoClick = { _, _ -> },
                        onSearchQueryChanged = {},
                        onFolderSelected = {},
                        onSortOrderChanged = {},
                        onRefresh = {}
                    )
                }
            }
        }

        // The rail keeps navigation; the wide index (and its stat block) yields its space to the grid.
        // At 200% the labels are gone, so the icons' descriptions are the accessible names.
        composeRule.onNodeWithContentDescription("Open File").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onAllNodesWithText("APTURA").assertCountEquals(0)
        composeRule.onAllNodesWithText("Open").assertCountEquals(0)
        composeRule.onAllNodesWithText("COLLECTIONS").assertCountEquals(0)
        composeRule.onAllNodesWithText("All videos").assertCountEquals(0)
        // The layout is forced compact at this font scale, so there is nothing to expand into.
        composeRule.onAllNodesWithContentDescription("Expand sidebar").assertCountEquals(0)

        // Chips come back because the rail has no folder list.
        composeRule.onNodeWithText("All Videos").assertIsDisplayed()
    }
}
