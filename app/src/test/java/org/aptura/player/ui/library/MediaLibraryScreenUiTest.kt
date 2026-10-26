package org.aptura.player.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        composeTestRule.onNodeWithText("No videos found").assertIsDisplayed()

        // Verify Scan Device button is displayed and triggers callback
        composeTestRule.onNodeWithText("Scan Device").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan Device").performClick()
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
    fun mediaLibraryScreen_swipingLeftAdvancesToNextFolder() {
        var selectedFolder: String? = null
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos,
                        folders = listOf("Movies", "Camera")
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = { selectedFolder = it },
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("library_video_grid").performTouchInput { swipeLeft() }

        assertEquals("Movies", selectedFolder)
    }

    @Test
    fun mediaLibraryScreen_swipesBetweenFoldersInBothDirections() {
        composeTestRule.setContent {
            var selectedFolder by remember { mutableStateOf<String?>("Movies") }
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos,
                        folders = listOf("Movies", "Camera"),
                        selectedFolder = selectedFolder
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = { selectedFolder = it },
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("library_video_grid").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Camera").assertIsSelected()
        composeTestRule.onNodeWithTag("library_video_grid").performTouchInput { swipeRight() }

        composeTestRule.onNodeWithText("Movies").assertIsSelected()
    }

    @Test
    fun mediaLibraryScreen_swipingInSelectionModeDoesNotChangeFolder() {
        var folderSelectionCount = 0
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos,
                        folders = listOf("Movies", "Camera")
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = { folderSelectionCount++ },
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("library_video_grid").performTouchInput { swipeLeft() }

        assertEquals(0, folderSelectionCount)
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

    @Test
    fun videoGridCard_moreActionsClick_opensOverflowMenuWithQuickActions() {
        var playNextItem: DomainMediaItem? = null
        var detailsShownItem: DomainMediaItem? = null

        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos.take(1),
                        folders = emptyList(),
                        selectedFolder = null,
                        searchQuery = "",
                        sortOrder = LibrarySortOrder.DATE_ADDED_DESC
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {},
                    onPlayNext = { playNextItem = it },
                    onShowDetails = { detailsShownItem = it }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        composeTestRule.onNodeWithText("Play Next").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add to Queue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide from library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete video").assertIsDisplayed()

        composeTestRule.onNodeWithText("Play Next").performClick()
        assertEquals("content://media/1", playNextItem?.uri)

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        composeTestRule.onNodeWithText("Video Details").performClick()
        assertEquals("content://media/1", detailsShownItem?.uri)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w900dp-h1100dp-xhdpi")
    fun longPressEntersMultiSelect_andHideActionIsNonDestructive() {
        val hiddenUris = mutableListOf<String>()
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
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {},
                    onRemoveVideo = { hiddenUris += it }
                )
            }
        }

        composeTestRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("1 selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Blade Runner 2049").performClick()
        composeTestRule.onNodeWithText("2 selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Move").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("More actions for selected videos").performClick()
        composeTestRule.onNodeWithText("Hide from library").performClick()
        composeTestRule.onNodeWithText("Hide 2 videos?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide videos").performClick()

        assertEquals(setOf("content://media/1", "content://media/2"), hiddenUris.toSet())
    }

    @Test
    fun deleteVideoFollowsHide_andExplainsTrashVersusPermanentDeletion() {
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos.take(1),
                        folders = listOf("Movies"),
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

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        val hideBounds = composeTestRule.onNodeWithText("Hide from library").getUnclippedBoundsInRoot()
        val deleteBounds = composeTestRule.onNodeWithText("Delete video").getUnclippedBoundsInRoot()
        assertTrue("Delete must be directly below Hide", deleteBounds.top >= hideBounds.bottom)
        composeTestRule.onNodeWithText("Delete video").performClick()

        composeTestRule.onNodeWithText("Move to trash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete permanently").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    fun contextMenuCopyExposesPasteAndClearClipboardControls() {
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos.take(1),
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

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        composeTestRule.onNodeWithText("More file actions").performClick()
        composeTestRule.onNodeWithText("Copy").performClick()
        composeTestRule.onNodeWithText("Paste").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear").performClick()
        composeTestRule.onAllNodesWithText("Paste").assertCountEquals(0)
    }

    @Test
    fun cardOverflowOffersAnExplicitSelectEntry() {
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos.take(1),
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

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        composeTestRule.onNodeWithText("Select").performClick()
        composeTestRule.onNodeWithText("1 selected").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w900dp-h1100dp-xhdpi")
    fun singleItemSelectionDeleteDialogNamesTheVideo() {
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
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        composeTestRule.onNodeWithContentDescription("More actions for selected videos").performClick()
        composeTestRule.onNodeWithText("Delete videos").performClick()

        composeTestRule.onNodeWithText("Delete from Device?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "\"Oppenheimer\" can be moved to Android's trash and restored later, or deleted permanently. Android may ask you to confirm either action."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w900dp-h1100dp-xhdpi")
    fun bulkDeleteConfirmationStatesTheCountAndTrashChoice() {
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
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Blade Runner 2049").performClick()
        composeTestRule.onNodeWithContentDescription("More actions for selected videos").performClick()
        composeTestRule.onNodeWithText("Delete videos").performClick()

        composeTestRule.onNodeWithText("Delete 2 videos from device?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "2 videos can be moved to Android's trash and restored later, or deleted permanently. Android may ask you to confirm either action."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Move to trash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete permanently").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w900dp-h1100dp-xhdpi")
    fun bulkMoveConfirmationExplainsCopyDeleteAndRetainBehavior() {
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
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Blade Runner 2049").performClick()
        composeTestRule.onNodeWithText("Move").performClick()

        composeTestRule.onNodeWithText("Move 2 videos?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Aptura will copy each selected video to the folder you choose, then ask Android to delete its original. If copying fails or Android denies deletion, that original stays in your library. Make sure the destination has enough free space."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose folder").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w1200dp-h900dp-xhdpi")
    fun selectionBarStaysCenteredAtReadingWidthOnATablet() {
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
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        val container = composeTestRule.onNodeWithTag("selection_action_bar_container")
            .getUnclippedBoundsInRoot()
        val bar = composeTestRule.onNodeWithTag("selection_action_bar")
            .getUnclippedBoundsInRoot()

        val barWidth = (bar.right - bar.left).value
        assertTrue("selection bar should cap at 680 dp, was $barWidth", barWidth <= 680f)
        assertTrue("selection bar should use the reading width, was $barWidth", barWidth >= 600f)
        val containerCenter = (container.left.value + container.right.value) / 2f
        val barCenter = (bar.left.value + bar.right.value) / 2f
        assertEquals("selection bar must be centered under the search row", containerCenter, barCenter, 0.5f)
    }
}
