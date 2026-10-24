package org.aptura.player.ui.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.ui.player.assertTouchTarget
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every tappable control on the library home reaches the 48 x 48 dp touch-target minimum. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class LibraryTouchTargetsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val item = DomainMediaItem(
        uri = "content://media/1",
        title = "Oppenheimer",
        durationMs = 10_800_000L,
        sizeBytes = 8_589_934_592L,
        mimeType = "video/mp4",
        folderPath = "Movies"
    )

    @Test
    fun theHeaderButtonsAndTheCardMenuAreAllAtLeast48dp() {
        composeRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = listOf(
                            VideoCardUiModel(
                                uri = item.uri,
                                title = item.title,
                                durationFormatted = "3:00:00",
                                fileSizeFormatted = "8 GB",
                                folderName = "Movies",
                                rawItem = item
                            )
                        )
                    ),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {}
                )
            }
        }

        listOf("Sort library", "Refresh library", "Settings").forEach { description ->
            composeRule.onNodeWithContentDescription(description).assertTouchTarget(composeRule.density)
        }
        composeRule.onAllNodesWithContentDescription("More actions for", substring = true).onFirst()
            .assertTouchTarget(composeRule.density)
    }
}
