package org.aptura.player.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
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
 * Finding the current folder and the "Add folder" button, however many folders there are and however the folder was
 * reached; and the swipe between folders.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class FolderNavigationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val folders = (1..30).map { "Folder %02d".format(it) }

    private fun video(index: Int) = DomainMediaItem(
        uri = "content://media/$index",
        title = "Clip $index",
        durationMs = 60_000L,
        sizeBytes = 10_000_000L,
        mimeType = "video/mp4",
        folderPath = folders[index % folders.size]
    )

    private fun card(item: DomainMediaItem) = VideoCardUiModel(
        uri = item.uri,
        title = item.title,
        durationFormatted = "1:00",
        fileSizeFormatted = "10 MB",
        folderName = item.folderPath,
        rawItem = item
    )

    private var selected by mutableStateOf<String?>(null)
    private val picked = mutableListOf<String?>()

    private fun show(folderList: List<String> = folders, initial: String? = null) {
        selected = initial
        picked.clear()
        composeRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = (1..6).map { card(video(it)) },
                        folders = folderList,
                        selectedFolder = selected,
                        folderCounts = folderList.associateWith { 4 },
                        libraryVideoCount = 6
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {
                        picked += it
                        selected = it
                    },
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun addFolder_isOnScreenWithoutScrolling_evenWithThirtyFolders() {
        show()

        composeRule.onNodeWithTag(ADD_FOLDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BROWSE_FOLDERS_TAG).assertIsDisplayed()
    }

    @Test
    fun aFolderFarAlongTheStrip_isScrolledIntoView_andMarkedSelected() {
        show(initial = folders[24])

        // 25 chips along: without scrolling it is far off screen.
        composeRule.onNode(hasText("Folder 25") and androidx.compose.ui.test.isSelectable()).assertIsDisplayed()
        composeRule.onNode(hasText("Folder 25") and androidx.compose.ui.test.isSelectable()).assertIsSelected()
    }

    @Test
    fun selectingAnotherFolder_scrollsTheStripToIt() {
        show()
        composeRule.runOnIdle { selected = folders[17] }
        composeRule.waitForIdle()

        composeRule.onNode(hasText("Folder 18") and androidx.compose.ui.test.isSelectable()).assertIsDisplayed()
    }

    @Test
    fun swipingLeft_goesToTheNextFolder() {
        show(listOf("Movies", "Series", "Camera"))

        composeRule.onNodeWithTag(LIBRARY_VIDEO_GRID_TAG).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(listOf<String?>("Movies"), picked)
    }

    @Test
    fun swipingRight_goesBack_andNeverPastAllVideos() {
        show(listOf("Movies", "Series", "Camera"), initial = "Movies")

        composeRule.onNodeWithTag(LIBRARY_VIDEO_GRID_TAG).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertEquals(listOf<String?>(null), picked)

        // Already at All videos: a further swipe back stays put instead of wrapping round to the last folder.
        composeRule.onNodeWithTag(LIBRARY_VIDEO_GRID_TAG).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertEquals(listOf<String?>(null), picked)
    }

    @Test
    fun swipingForwardFromTheLastFolder_staysPut() {
        show(listOf("Movies", "Series", "Camera"), initial = "Camera")

        composeRule.onNodeWithTag(LIBRARY_VIDEO_GRID_TAG).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertTrue("no folder change at the end: $picked", picked.isEmpty())
    }

    @Test
    fun whileSelectingVideos_swipingDoesNotChangeFolder() {
        show(listOf("Movies", "Series", "Camera"))
        composeRule.onNode(hasText("Clip 1")).performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(LIBRARY_VIDEO_GRID_TAG).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertTrue("selecting disables the folder swipe: $picked", picked.isEmpty())
    }
}
