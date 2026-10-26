package org.aptura.player.ui.library

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.visual.CapturedScreen
import org.aptura.player.ui.visual.captureScreen
import org.aptura.player.ui.visual.toPx
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The selection bar is the one surface that appears directly over a populated library. Its container token is
 * opaque; this guard proves the pixels match, so a future translucent scrim (or a bar laid out under the grid)
 * fails even though the text still exists in the semantics tree.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xhdpi")
class SelectionBarPixelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val videos = listOf(
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
    ).map { item ->
        VideoCardUiModel(
            uri = item.uri,
            title = item.title,
            durationFormatted = MediaFormatters.formatDuration(item.durationMs),
            fileSizeFormatted = MediaFormatters.formatFileSize(item.sizeBytes),
            folderName = "Movies",
            rawItem = item
        )
    }

    private fun show(mode: ThemeMode) {
        composeRule.setContent {
            ApturaTheme(themeMode = mode) {
                MediaLibraryScreen(
                    uiState = MediaLibraryUiState.Success(
                        videos = videos,
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
    }

    private fun selectAndCapture(): CapturedScreen {
        composeRule.onNodeWithText("Oppenheimer").performTouchInput { longClick() }
        composeRule.onNodeWithTag("selection_action_bar").assertExists()
        return composeRule.captureScreen()
    }

    @Test
    fun theLightSelectionBar_isAnOpaqueSurface() = assertOpaque(ThemeMode.LIGHT)

    @Test
    fun theDarkSelectionBar_isAnOpaqueSurface() = assertOpaque(ThemeMode.DARK)

    private fun assertOpaque(mode: ThemeMode) {
        show(mode)
        val screen = selectAndCapture()

        val bar = composeRule.onNodeWithTag("selection_action_bar")
            .getUnclippedBoundsInRoot()
        val expected = ApturaColors.SurfaceElevated1
        // Four dp inside the left edge and sixteen dp below/above the rounded corners is a pure fill strip: inside
        // the 12 dp content padding, clear of both the label and the 14 dp corner radius.
        val left = composeRule.toPx(bar.left + 4.dp)
        val right = left + 1
        val top = composeRule.toPx(bar.top + 16.dp)
        val bottom = composeRule.toPx(bar.bottom - 16.dp)

        screen.assertRegionIs(expected, left, top, right, bottom, "selection bar fill in $mode")
    }
}
