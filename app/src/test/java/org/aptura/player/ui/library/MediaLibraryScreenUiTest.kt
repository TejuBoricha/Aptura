package org.aptura.player.ui.library

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.unit.dp
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.aptura.player.core.media.DeleteOutcome
import org.aptura.player.data.transfer.MediaTransferJob
import org.aptura.player.data.transfer.MediaTransferJournal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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

        // The skeleton waits a moment before it appears (loading that is over sooner shows nothing at all, not a flash),
        // then fades in and stays for as long as the first scan runs.
        composeTestRule.mainClock.advanceTimeBy(SKELETON_DELAY_MS + 400)
        composeTestRule.onNodeWithText("Looking for videos\u2026").assertIsDisplayed()
    }

    @Test
    fun mediaLibraryScreen_whenLoadingIsQuick_neverFlashesTheSkeleton() {
        composeTestRule.mainClock.autoAdvance = false
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
        composeTestRule.mainClock.advanceTimeBy(SKELETON_DELAY_MS - 100)
        composeTestRule.onNodeWithText("Looking for videos\u2026").assertDoesNotExist()
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
        composeTestRule.onNodeWithText("No videos yet").assertIsDisplayed()

        // Verify Scan Device button is displayed and triggers callback
        composeTestRule.onNodeWithText("Rescan library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rescan library").performClick()
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
        composeTestRule.onNodeWithText("All videos").assertIsDisplayed()
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
        composeTestRule.onNode(hasText("Camera") and isSelectable()).assertIsSelected()
        composeTestRule.onNodeWithTag("library_video_grid").performTouchInput { swipeRight() }

        composeTestRule.onNode(hasText("Movies") and isSelectable()).assertIsSelected()
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

    // A tall window: the sheet is four sections and does not scroll under Robolectric. What the sheet reports is covered
    // by LibraryViewSheetTest; a click inside a sheet's own window does not settle under Robolectric.
    @Config(qualifiers = "w411dp-h1400dp")
    @Test
    fun theViewOptionsButton_opensTheSheetWithEveryChoice() {
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

        composeTestRule.onNodeWithContentDescription("View options").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View options").performClick()
        composeTestRule.waitForIdle()

        // Layout, size, details and sort all live in this one sheet.
        listOf("Layout", "Size", "Details", "Sort by").forEach {
            composeTestRule.onNodeWithText(it, ignoreCase = true).assertIsDisplayed()
        }
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
        composeTestRule.onNodeWithText("Play next").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add to queue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide from library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete video").assertIsDisplayed()

        composeTestRule.onNodeWithText("Play next").performClick()
        assertEquals("content://media/1", playNextItem?.uri)

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        composeTestRule.onNodeWithText("Video details").performClick()
        assertEquals("content://media/1", detailsShownItem?.uri)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w900dp-h1100dp-xhdpi")
    fun longPressEntersMultiSelect_andHideActionIsNonDestructive() {
        val hiddenUris = mutableListOf<String>()
        val unhiddenUris = mutableListOf<String>()
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
                    onRemoveVideo = { hiddenUris += it },
                    onUnhideVideo = { unhiddenUris += it }
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
        composeTestRule.onNodeWithText("2 videos hidden").assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").performClick()
        assertEquals(setOf("content://media/1", "content://media/2"), unhiddenUris.toSet())
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
    fun missingCardOffersOnlyRemove_andConfirmsBeforeDeletingTheRow() {
        val missing = sampleVideos.first().copy(isMissing = true)
        val missingCard = sampleUiVideos.first().copy(isMissing = true, rawItem = missing)
        var removedUri: String? = null
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = listOf(missingCard),
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
                    onRemoveMissingVideo = { removedUri = it }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        // A missing file cannot play, move, share or be deleted again.
        composeTestRule.onNodeWithText("Play next").assertDoesNotExist()
        composeTestRule.onNodeWithText("Select").assertDoesNotExist()
        composeTestRule.onNodeWithText("Move to folder").assertDoesNotExist()
        composeTestRule.onNodeWithText("Share").assertDoesNotExist()
        composeTestRule.onNodeWithText("Hide from library").assertDoesNotExist()
        composeTestRule.onNodeWithText("Delete video").assertDoesNotExist()

        composeTestRule.onNodeWithText("Remove from library").performClick()
        composeTestRule.onNodeWithText("Remove missing video?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "The file is already unavailable, so nothing on disk will be deleted.",
            substring = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove").performClick()

        assertEquals(missing.uri, removedUri)
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
    fun cutInOneFolderAndNavigatingToAnother_exposesDirectMoveHereButton() {
        val moviesVideo = sampleUiVideos.first().copy(
            rawItem = sampleUiVideos.first().rawItem.copy(folderPath = "/storage/emulated/0/Movies")
        )
        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = listOf(moviesVideo),
                        folders = listOf("/storage/emulated/0/Movies", "/storage/emulated/0/Trailers"),
                        selectedFolder = "/storage/emulated/0/Trailers",
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
        composeTestRule.onNodeWithText("Cut").performClick()

        // Since the current view is Trailers, the bar should offer "Move here" directly
        composeTestRule.onNodeWithText("Move here").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Choose another folder").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Delete from device?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "\u201cOppenheimer\u201d can go to Android's trash, where you can restore it later. Deleting permanently cannot be undone. Android may ask you to confirm either action."
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
            "2 videos can go to Android's trash, where you can restore them later. Deleting permanently cannot be undone. Android may ask you to confirm either action."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Move to trash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete permanently").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w900dp-h1100dp-xhdpi")
    fun bulkMoveOpensInAppFolderDestinationPicker() {
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

        composeTestRule.onNodeWithText("Move 2 videos to...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose a destination folder").assertIsDisplayed()
        composeTestRule.onNodeWithText("Browse device storage...").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Movies").assertCountEquals(2)
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

    @Test
    fun crossVolumeMoveCompletion_invokesOnVideoMovedAway_notOnVideoDeleted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        try {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
        } catch (_: IllegalStateException) {}
        MediaTransferJournal.clear(context)
        val targetUri = "content://media/1"
        val movedAwayCalls = mutableListOf<String>()
        val deletedCalls = mutableListOf<String>()

        val fakeDeleteEngine = object : MediaDeleteEngine {
            override fun delete(uri: String): DeleteOutcome = DeleteOutcome.Deleted
            override fun requestBatchDeleteConsent(uris: List<String>): android.content.IntentSender? = null
        }

        // Simulate WorkManager having finished cross-volume file copy
        val job = MediaTransferJob(
            id = "test-job-move",
            sources = listOf(targetUri),
            destination = "SAF/SDCard",
            isMove = true,
            fromClipboard = false,
            copiedUris = listOf(targetUri),
            copyFinished = true
        )
        MediaTransferJournal.write(context, job)

        composeTestRule.setContent {
            ApturaTheme {
                CompositionLocalProvider(LocalMediaDeleteEngine provides fakeDeleteEngine) {
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
                        onVideoDeleted = { deletedCalls += it },
                        onVideoMovedAway = { movedAwayCalls += it }
                    )
                }
            }
        }
        val promptDeadline = System.currentTimeMillis() + 10_000
        var dialogFound = false
        while (!dialogFound && System.currentTimeMillis() < promptDeadline) {
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            if (composeTestRule.onAllNodesWithText("Remove original from device?").fetchSemanticsNodes().isNotEmpty()) {
                dialogFound = true
                break
            }
            Thread.sleep(20)
        }
        assertTrue("Remove original from device? prompt was displayed within 10 seconds", dialogFound)
        composeTestRule.onNodeWithText("Continue").performClick()

        val deadline = System.currentTimeMillis() + 5_000
        while (movedAwayCalls.isEmpty() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            Thread.sleep(20)
        }

        // Assert: DATA-01 callback choice contract:
        // Moving away calls onVideoMovedAway so the database row is kept (marked missing with progress/bookmarks),
        // and NEVER calls onVideoDeleted (which purges the row).
        assertEquals("onVideoMovedAway was called for the original video", listOf(targetUri), movedAwayCalls)
        assertTrue("onVideoDeleted was NEVER called during move cleanup", deletedCalls.isEmpty())
        MediaTransferJournal.clear(context)
    }

    @Test
    fun permanentDeletion_invokesOnVideoDeleted_notOnVideoMovedAway() {
        val targetUri = "content://media/1"
        val movedAwayCalls = mutableListOf<String>()
        val deletedCalls = mutableListOf<String>()

        val fakeDeleteEngine = object : MediaDeleteEngine {
            override fun delete(uri: String): DeleteOutcome = DeleteOutcome.Deleted
            override fun requestBatchDeleteConsent(uris: List<String>): android.content.IntentSender? = null
        }

        composeTestRule.setContent {
            ApturaTheme {
                CompositionLocalProvider(LocalMediaDeleteEngine provides fakeDeleteEngine) {
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
                        onVideoDeleted = { deletedCalls += it },
                        onVideoMovedAway = { movedAwayCalls += it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        composeTestRule.onNodeWithContentDescription("More actions for selected videos").performClick()
        composeTestRule.onNodeWithText("Delete videos").performClick()

        // Confirmation dialog: choose permanent deletion
        composeTestRule.onNodeWithText("Delete from device?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete permanently").performClick()

        val deadline = System.currentTimeMillis() + 5_000
        while (deletedCalls.isEmpty() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            Thread.sleep(20)
        }

        assertEquals("onVideoDeleted was called for the deleted video", listOf(targetUri), deletedCalls)
        assertTrue("onVideoMovedAway was NEVER called for explicit deletion", movedAwayCalls.isEmpty())
    }

    @Test
    fun singleVideoHide_immediatelyHidesWithoutDialog_andShowsSnackbarUndo() {
        val hiddenUris = mutableListOf<String>()
        val unhiddenUris = mutableListOf<String>()

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
                    onRefresh = {},
                    onRemoveVideo = { hiddenUris += it },
                    onUnhideVideo = { unhiddenUris += it }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("More actions for Oppenheimer").performClick()
        composeTestRule.onNodeWithText("Hide from library").performClick()

        // Single-item hide is non-destructive and immediate, showing an undo snackbar instead of a modal confirmation
        assertEquals(listOf("content://media/1"), hiddenUris)
        composeTestRule.onNodeWithText("\"Oppenheimer\" hidden from library").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss undo notice")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Undo").performClick()
        assertEquals(listOf("content://media/1"), unhiddenUris)
    }

    @Test
    fun mediaDetailsContent_quickActions_meetAccessibleTouchTargetMinimum() {
        composeTestRule.setContent {
            ApturaTheme {
                MediaDetailsContent(
                    item = sampleVideos.first(),
                    onClose = {},
                    onPlayClick = {},
                    onResetProgressClick = {},
                    onUndoResetProgressClick = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Play").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Share").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Rename").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Delete").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun resetProgress_inMediaDetailsContent_showsUndo_andRestoresPreviousProgress() {
        var resetCalled = false
        var restored: Pair<Long, Boolean>? = null

        composeTestRule.setContent {
            var currentItem by remember {
                mutableStateOf(
                    sampleVideos.first().copy(playbackPositionMs = 45_000L, isCompleted = false)
                )
            }
            ApturaTheme {
                MediaDetailsContent(
                    item = currentItem,
                    onClose = {},
                    onPlayClick = {},
                    onResetProgressClick = {
                        resetCalled = true
                        currentItem = currentItem.copy(playbackPositionMs = 0L, isCompleted = false)
                    },
                    onUndoResetProgressClick = { pos, completed ->
                        restored = pos to completed
                        currentItem = currentItem.copy(playbackPositionMs = pos, isCompleted = completed)
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").performClick()
        assertTrue(resetCalled)

        composeTestRule.onNodeWithText("Progress reset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").performClick()
        assertEquals(45_000L to false, restored)
    }

    @Test
    fun partialMediaAccessBanner_showsSelectMoreAndAllowAllActions() {
        var selectMoreClicks = 0
        var allowAllClicks = 0

        composeTestRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = sampleUiVideos,
                        folders = listOf("/storage/Movies"),
                        isLimitedMode = false,
                        isPartialMediaAccess = true
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {},
                    onRequestPermission = { selectMoreClicks++ },
                    onOpenAppSettings = { allowAllClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Selected videos only").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select more").performClick()
        composeTestRule.onNodeWithText("Allow all").performClick()

        assertEquals(1, selectMoreClicks)
        assertEquals(1, allowAllClicks)
    }
}
