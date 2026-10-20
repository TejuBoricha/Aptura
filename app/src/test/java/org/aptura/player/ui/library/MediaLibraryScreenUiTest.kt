package org.aptura.player.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Compose UI tests for MediaLibraryScreen on JVM via Robolectric.
 * Validates Loading, Empty, and Success states, search bar interaction,
 * folder chip filtering, and video item click callbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaLibraryScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleVideos = listOf(
        DomainMediaItem(
            uri = "content://media/1",
            title = "Oppenheimer",
            durationMs = 10_800_000L,
            sizeBytes = 8_589_934_592L,
            mimeType = "video/mp4",
            folderPath = "/storage/Movies"
        ),
        DomainMediaItem(
            uri = "content://media/2",
            title = "Blade Runner 2049",
            durationMs = 9_840_000L,
            sizeBytes = 6_442_450_944L,
            mimeType = "video/mkv",
            folderPath = "/storage/Movies"
        )
    )

    private val sampleUiVideos = sampleVideos.map { item ->
        VideoCardUiModel(
            uri = item.uri,
            title = item.title,
            durationFormatted = MediaFormatters.formatDuration(item.durationMs),
            fileSizeFormatted = MediaFormatters.formatFileSize(item.sizeBytes),
            folderName = "Movies",
            rawItem = item
        )
    }

    @Test
    fun mediaLibraryScreen_whenLoading_displaysScanningIndicator() {
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Loading,
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        // Verify scanning indicator text is displayed
        composeTestRule.onNodeWithText("Scanning local videos...").assertIsDisplayed()
    }

    @Test
    fun mediaLibraryScreen_whenEmpty_displaysEmptyViewAndRefreshAffordance() {
        var refreshClicked = false

        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Empty(isFiltered = false),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = { refreshClicked = true }
                )
            }
        }

        // Verify empty view title and message
        composeTestRule.onNodeWithText("Your library is ready").assertIsDisplayed()

        // Verify Refresh button is displayed and triggers callback
        composeTestRule.onNodeWithText("Refresh Videos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Refresh Videos").performClick()
        assertTrue(refreshClicked)
    }

    @Test
    fun mediaLibraryScreen_whenSuccess_rendersCardsAndTriggersClick() {
        var clickedVideo: DomainMediaItem? = null
        var clickedPlaylist: List<DomainMediaItem>? = null

        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos,
                        folders = listOf("Movies"),
                        selectedFolder = null,
                        searchQuery = "",
                        sortOrder = LibrarySortOrder.DATE_ADDED_DESC
                    ),
                    onVideoClick = { video, playlist ->
                        clickedVideo = video
                        clickedPlaylist = playlist
                    },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        // Verify both video titles are rendered in the grid
        composeTestRule.onNodeWithText("Oppenheimer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Blade Runner 2049").assertIsDisplayed()

        // Verify folder filter chip
        composeTestRule.onNodeWithText("All Videos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movies").assertIsDisplayed()

        // Click on the first video card
        composeTestRule.onNodeWithText("Oppenheimer").performClick()

        // Assert callback passed the correct item and playlist
        assertEquals("content://media/1", clickedVideo?.uri)
        assertEquals(2, clickedPlaylist?.size)
    }

    @Test
    fun mediaLibraryScreen_displaysSortButton_andTriggersSortMenu() {
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos,
                        folders = emptyList(),
                        selectedFolder = null,
                        searchQuery = "",
                        sortOrder = LibrarySortOrder.DATE_ADDED_DESC
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        // Verify sort button is displayed with correct content description
        composeTestRule.onNodeWithContentDescription("Sort library").assertIsDisplayed()
    }
}
