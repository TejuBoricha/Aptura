package org.aptura.player.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
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
 * The library on a phone held sideways, which is wide (past the 700 dp that gives a tablet its sidebar) but only 360 to
 * 411 dp tall. The tablet sidebar needs about 450 dp before its first folder row, so on these windows it left no room
 * for folders at all (nothing to pick, nothing to add), and the narrow rail did not fit and cut off its Settings button.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibrarySidewaysTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val folders = listOf("Movies", "Series", "Camera")

    private fun state(): MediaLibraryUiState.Success {
        val videos = folders.flatMapIndexed { folderIndex, folder ->
            (0 until 3).map { n ->
                val item = DomainMediaItem(
                    uri = "content://media/$folderIndex$n",
                    title = "$folder $n",
                    durationMs = 3_600_000L,
                    sizeBytes = 500_000_000L,
                    mimeType = "video/mp4",
                    folderPath = folder
                )
                VideoCardUiModel(item.uri, item.title, "1:00:00", "476 MB", folder, rawItem = item)
            }
        }
        return MediaLibraryUiState.Success(
            videos = videos,
            folders = folders,
            totalCount = videos.size,
            libraryVideoCount = videos.size,
            libraryDurationMs = 9 * 3_600_000L,
            librarySizeBytes = 4_500_000_000L,
            folderCounts = folders.associateWith { 3 }
        )
    }

    private fun show(uiState: MediaLibraryUiState) {
        composeRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = uiState,
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {},
                    onAddFolderClick = {}
                )
            }
        }
    }

    private fun assertWithinWindow(contentDescription: String, windowHeightDp: Float) {
        val bounds = composeRule.onNodeWithContentDescription(contentDescription).getUnclippedBoundsInRoot()
        assertTrue("$contentDescription ends at ${bounds.bottom}, below the ${windowHeightDp}dp window", bounds.bottom.value <= windowHeightDp)
        assertTrue("$contentDescription starts above the window at ${bounds.top}", bounds.top.value >= 0f)
    }

    private fun assertSidewaysLibraryWorks(windowHeightDp: Float) {
        show(state())

        // No tablet sidebar: its folder list has no room here. The folders are on the folder bar, with Add folder.
        composeRule.onAllNodesWithText("COLLECTIONS").assertCountEquals(0)
        composeRule.onNodeWithTag(FOLDER_STRIP_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(ADD_FOLDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BROWSE_FOLDERS_TAG).assertIsDisplayed()
        composeRule.onNode(hasText("Movies", substring = true) and hasAnyAncestor(hasTestTag(FOLDER_STRIP_TAG))).assertIsDisplayed()

        // Every button of the rail is fully on screen, including the last one.
        listOf("Open File", "Refresh", "Settings").forEach { assertWithinWindow(it, windowHeightDp) }
    }

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test
    fun aCurrentPhoneSideways_hasFoldersAndAFullyVisibleRail() = assertSidewaysLibraryWorks(411f)

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test
    fun aSmallPhoneSideways_hasFoldersAndAFullyVisibleRail() = assertSidewaysLibraryWorks(360f)

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test
    fun aTablet_stillGetsTheCollectionIndexWithItsFolders() {
        show(state())
        composeRule.onNodeWithText("COLLECTIONS").assertIsDisplayed()
        folders.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test
    fun thePermissionScreen_sideways_hasItsMainButtonInView() {
        show(MediaLibraryUiState.Empty(isPermissionDenied = true))
        // The whole button, not just its label: the label sits in the middle of it.
        val button = composeRule.onNode(hasClickAction() and hasText("Allow access")).getUnclippedBoundsInRoot()
        // In view with room under it for the second choice ("Add a folder"), not at the very edge of a 360 dp window.
        assertTrue("\"Allow access\" ends at ${button.bottom}, too low in a 360dp window", button.bottom.value <= 300f)
    }

    private fun stateWithAShelf(): MediaLibraryUiState.Success {
        val base = state()
        val resumable = base.videos.take(2).map { card ->
            card.copy(progressFraction = 0.2f, rawItem = card.rawItem.copy(playbackPositionMs = 600_000L))
        }
        return base.copy(continueWatching = resumable)
    }

    private fun shelfCardWidth(): Dp =
        composeRule.onAllNodes(hasContentDescription("Resume", substring = true))[0].getUnclippedBoundsInRoot().width

    // The shelf's cards are 248 dp wide, which makes a 140 dp tall picture: nearly half a sideways phone's screen,
    // leaving the library under it out of sight. On a short window they are smaller.
    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test
    fun theContinueWatchingCards_areSmaller_onASidewaysPhone() {
        show(stateWithAShelf())
        assertEquals(168f, shelfCardWidth().value, 0.5f)
    }

    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    @Test
    fun theContinueWatchingCards_areSmaller_onASmallSidewaysPhone() {
        show(stateWithAShelf())
        assertEquals(168f, shelfCardWidth().value, 0.5f)
    }

    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    @Test
    fun theContinueWatchingCards_keepTheirSize_onAnUprightPhone() {
        show(stateWithAShelf())
        assertEquals(248f, shelfCardWidth().value, 0.5f)
    }

    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test
    fun theContinueWatchingCards_keepTheirSize_onATablet() {
        show(stateWithAShelf())
        assertEquals(248f, shelfCardWidth().value, 0.5f)
    }

    @Config(qualifiers = "w891dp-h411dp-xhdpi")
    @Test
    fun theViewSheet_scrolls_soTheSortChoicesAreReachableOnAShortScreen() {
        // The sheet's content in a window too short for it, as a sideways phone's sheet is.
        composeRule.setContent {
            ApturaTheme {
                Box(Modifier.height(260.dp)) {
                    LibraryViewSheetContent(
                        prefs = LibraryViewPrefs(),
                        sortOrder = LibrarySortOrder.DATE_ADDED_DESC,
                        onPrefsChange = {},
                        onSortChange = {}
                    )
                }
            }
        }
        val last = LibrarySortOrder.entries.last().displayName
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(last))
        composeRule.onNodeWithText(last).assertIsDisplayed()
    }
}
