package org.aptura.player.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The home view offers a "Continue watching" shelf; tapping a card resumes that video. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class ContinueWatchingUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val halfWatched = DomainMediaItem(
        uri = "content://media/7",
        title = "Dune Part Two",
        durationMs = 9_960_000L, // 2 h 46 min
        sizeBytes = 4_000_000_000L,
        mimeType = "video/mp4",
        folderPath = "Movies",
        playbackPositionMs = 4_980_000L, // exactly half
        lastPlayedTimestamp = 5_000L
    )
    private val untouched = halfWatched.copy(uri = "content://media/8", title = "Arrival", playbackPositionMs = 0L, lastPlayedTimestamp = 0L)

    private fun card(item: DomainMediaItem) = VideoCardUiModel(
        uri = item.uri,
        title = item.title,
        durationFormatted = MediaFormatters.formatDuration(item.durationMs),
        fileSizeFormatted = MediaFormatters.formatFileSize(item.sizeBytes),
        folderName = "Movies",
        progressFraction = MediaFormatters.calculateProgressFraction(item.playbackPositionMs, item.durationMs),
        rawItem = item
    )

    private fun show(state: MediaLibraryUiState.Success, onVideoClick: (DomainMediaItem, List<DomainMediaItem>) -> Unit = { _, _ -> }) {
        composeRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = state,
                    onVideoClick = onVideoClick,
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }
    }

    @Test
    fun theShelfShowsWhatToResume_andHowMuchIsLeft() {
        show(MediaLibraryUiState.Success(videos = listOf(card(halfWatched), card(untouched)), continueWatching = listOf(card(halfWatched))))

        composeRule.onNodeWithText(CONTINUE_WATCHING_TITLE).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Resume Dune Part Two, 1 h 23 min left").assertIsDisplayed()
    }

    @Test
    fun tappingACard_resumesThatVideo_withTheLibrarysOwnItem() {
        var resumed: DomainMediaItem? = null
        show(
            MediaLibraryUiState.Success(videos = listOf(card(halfWatched), card(untouched)), continueWatching = listOf(card(halfWatched))),
            onVideoClick = { item, _ -> resumed = item }
        )

        composeRule.onNodeWithContentDescription("Resume Dune Part Two, 1 h 23 min left").performClick()

        assertEquals("the saved position must travel with the item, or resume would restart", 4_980_000L, resumed?.playbackPositionMs)
        assertEquals("content://media/7", resumed?.uri)
    }

    @Test
    fun withNothingToContinue_thereIsNoShelf() {
        show(MediaLibraryUiState.Success(videos = listOf(card(untouched)), continueWatching = emptyList()))

        assertEquals(0, composeRule.onAllNodesWithTextCount(CONTINUE_WATCHING_TITLE))
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(text: String): Int =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
}
