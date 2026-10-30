package org.aptura.player.ui.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaPalettes
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.visual.captureScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tablet sidebar's folder list scrolls under the library summary above it. A row that slides under that edge used to
 * be sliced through its text ("Tutorials 37" half hidden under the "655 videos" card in the owner's screenshot). The
 * edge is now the panel's own colour for its first stretch, so nothing can show through it, and fades after that.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h560dp-xhdpi")
class LibrarySidebarScrollEdgeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val panel = ApturaPalettes.dark(AccentColor.CYAN).surfaceElevated1
    private val folders = (0 until 30).map { "Folder %02d".format(it) }

    private fun show(selected: String?) {
        val state = MediaLibraryUiState.Success(
            videos = emptyList(),
            folders = folders,
            totalCount = 300,
            libraryVideoCount = 300,
            libraryDurationMs = 3_600_000L,
            librarySizeBytes = 1_073_741_824L,
            folderCounts = folders.associateWith { 10 },
            selectedFolder = selected
        )
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.DARK) {
                LibrarySidebar(
                    state = state,
                    topPadding = androidx.compose.ui.unit.Dp(0f),
                    bottomPadding = androidx.compose.ui.unit.Dp(0f),
                    onFolderSelected = {},
                    onAddFolderClick = {},
                    onOpenFilePicker = {},
                    onRefresh = {},
                    onSettingsClick = {},
                    onCollapse = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    @Test
    fun aListScrolledToAFarFolder_hasNoRowShowingThroughItsTopEdge() {
        show(selected = folders.last())

        val edge = composeRule.onNodeWithTag(SIDEBAR_TOP_EDGE_TAG)
        edge.assertExists()
        val bounds = edge.fetchSemanticsNode().boundsInRoot
        val density = composeRule.density.density
        val solid = (11 * density).toInt()
        val screen = composeRule.captureScreen()

        // Wherever the scroll left the rows, none of them is visible in the edge's solid stretch.
        screen.assertRegionIs(
            expected = panel,
            left = bounds.left.toInt() + 2,
            top = bounds.top.toInt(),
            right = bounds.right.toInt() - 2,
            bottom = bounds.top.toInt() + solid,
            label = "the top edge of the scrolled folder list"
        )

        // And it is an edge, not an empty panel: the fade lets the rows return below the solid stretch.
        var inked = 0
        for (y in (bounds.top + solid).toInt() until bounds.bottom.toInt()) {
            for (x in bounds.left.toInt() + 2 until bounds.right.toInt() - 2) {
                if (screen.colorAt(x, y) != panel) inked++
            }
        }
        assertTrue("the fade should blend into the list below the solid stretch", inked > 0)
    }

    @Test
    fun aListAtItsStart_hasNoTopEdge() {
        show(selected = null)
        composeRule.onNodeWithTag(SIDEBAR_TOP_EDGE_TAG).assertDoesNotExist()
    }
}
