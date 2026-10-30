package org.aptura.player.ui.visual

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.FolderBrowserSheet
import org.aptura.player.ui.library.FolderSummary
import org.aptura.player.ui.library.LibraryViewPrefs
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PHONE = "w411dp-h891dp-xhdpi"
private const val SMALL_PHONE = "w320dp-h640dp-xhdpi"
private const val TABLET = "w1280dp-h800dp-xhdpi"
private const val MEDIUM = "w673dp-h841dp-xhdpi"
private const val LANDSCAPE = "w891dp-h411dp-xhdpi"

/**
 * The library in each layout, size and level of detail, and the folder navigation with a long list of folders. A review
 * tool like the rest of the gallery: it only runs when `APTURA_GALLERY_DIR` is set.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class LibraryLayoutGalleryTest : GalleryBase() {

    /** Thirty folders, so the strip has to scroll and the selected one is far from the start. */
    private val manyFolders: List<String> = listOf(
        "Movies", "Series", "Documentaries", "Camera", "Anime", "Concerts", "Courses", "Downloads", "Family", "Holidays",
        "Interviews", "Kids", "Lectures", "Music videos", "Podcasts", "Recordings", "Screen recordings", "Shorts",
        "Sport", "Talks", "Trailers", "Travel", "Tutorials", "Wedding", "Work", "WhatsApp Video", "Telegram",
        "Pictures", "Archive", "Old phone"
    ).map { "/storage/emulated/0/$it" }

    private fun manyFoldersState(selected: String?, items: List<org.aptura.player.domain.model.DomainMediaItem> = sampleItems): MediaLibraryUiState.Success {
        val base = librarySuccess(items = items, selectedFolder = selected, continueWatching = selected == null)
        val counts = manyFolders.mapIndexed { index, folder -> folder to (3 + index * 7 % 40) }.toMap()
        val covers = sampleItems.map { it }
        val summaries = manyFolders.mapIndexed { index, folder ->
            val cover = covers[index % covers.size]
            folder to FolderSummary(
                videoCount = counts.getValue(folder),
                totalBytes = counts.getValue(folder) * 650_000_000L,
                totalDurationMs = counts.getValue(folder) * 2_400_000L,
                coverUri = cover.uri,
                coverDateModifiedSec = cover.dateModifiedSec
            )
        }.toMap()
        return base.copy(
            folders = manyFolders,
            folderCounts = counts,
            folderSummaries = summaries,
            libraryVideoCount = counts.values.sum()
        )
    }

    /** The library with its preferences and selected folder held in state, as the app does. */
    @Composable
    private fun Library(
        state: MediaLibraryUiState.Success,
        prefs: LibraryViewPrefs = LibraryViewPrefs(),
        fontScale: Float = 1f
    ) {
        var current by remember { mutableStateOf(prefs) }
        var folder by remember { mutableStateOf(state.selectedFolder) }
        val density = LocalDensity.current
        androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            MediaLibraryScreen(
                // The samples stand in for whichever folder is selected, so every folder shows videos.
                uiState = state.copy(selectedFolder = folder),
                onVideoClick = { _, _ -> },
                onSearchQueryChanged = {},
                onFolderSelected = { folder = it },
                onSortOrderChanged = {},
                onRefresh = {},
                viewPrefs = current,
                onViewPrefsChanged = { current = it }
            )
        }
    }

    private fun shotLibrary(
        name: String,
        prefs: LibraryViewPrefs,
        mode: ThemeMode = ThemeMode.LIGHT,
        selected: String? = null,
        fontScale: Float = 1f,
        accent: AccentColor = AccentColor.CYAN
    ) {
        themed(mode, accent) { Library(manyFoldersState(selected), prefs, fontScale) }
        shoot(name)
    }

    // ---- layouts and sizes ----------------------------------------------------------------------------------------

    @Test fun gridCompact() = shotLibrary("layout_grid_compact", LibraryViewPrefs(density = LibraryDensity.COMPACT))
    @Test fun gridComfortable() = shotLibrary("layout_grid_comfortable", LibraryViewPrefs())
    @Test fun gridLarge() = shotLibrary("layout_grid_large", LibraryViewPrefs(density = LibraryDensity.LARGE))
    @Test fun gridTitleOnly() = shotLibrary("layout_grid_title_only", LibraryViewPrefs(detail = LibraryDetail.TITLE_ONLY))
    @Test fun gridDetailed() = shotLibrary("layout_grid_detailed", LibraryViewPrefs(detail = LibraryDetail.DETAILED))

    @Test fun listCompact() = shotLibrary("layout_list_compact", LibraryViewPrefs(LibraryLayout.LIST, LibraryDensity.COMPACT))
    @Test fun listComfortable() = shotLibrary("layout_list_comfortable", LibraryViewPrefs(LibraryLayout.LIST))
    @Test fun listLarge() = shotLibrary("layout_list_large", LibraryViewPrefs(LibraryLayout.LIST, LibraryDensity.LARGE))
    @Test fun listDetailed() = shotLibrary("layout_list_detailed", LibraryViewPrefs(LibraryLayout.LIST, detail = LibraryDetail.DETAILED))
    @Test fun listTitleOnlyDark() = shotLibrary(
        "layout_list_title_only_dark",
        LibraryViewPrefs(LibraryLayout.LIST, detail = LibraryDetail.TITLE_ONLY),
        ThemeMode.DARK
    )

    // ---- folder navigation ----------------------------------------------------------------------------------------

    @Test fun deepInTheFolders() = shotLibrary("folders_selected_far_along", LibraryViewPrefs(), selected = manyFolders[22])
    @Test fun deepInTheFoldersDark() = shotLibrary("folders_selected_far_along_dark", LibraryViewPrefs(), ThemeMode.DARK, selected = manyFolders[26])

    @Test fun afterASwipe_thePillNamesTheFolder() {
        themed(ThemeMode.LIGHT) { Library(manyFoldersState(manyFolders[1]), LibraryViewPrefs()) }
        composeRule.onNodeWithTag("library_video_grid").performTouchInput { swipeLeft() }
        composeRule.mainClock.advanceTimeBy(350)
        composeRule.waitForIdle()
        shoot("folders_after_swipe")
    }

    @Config(qualifiers = "w411dp-h1100dp-xhdpi")
    @Test fun folderBrowserList() {
        themed(ThemeMode.LIGHT) { FolderBrowser(FolderLayout.LIST) }
        shoot("folder_browser_list")
    }

    @Config(qualifiers = "w411dp-h1100dp-xhdpi")
    @Test fun folderBrowserGridDark() {
        themed(ThemeMode.DARK) { FolderBrowser(FolderLayout.GRID) }
        shoot("folder_browser_grid_dark")
    }

    @Composable
    private fun FolderBrowser(layout: FolderLayout) {
        val state = manyFoldersState(manyFolders[2])
        FolderBrowserSheet(
            folders = state.folders,
            labels = state.folders.associateWith { it.substringAfterLast('/') },
            summaries = state.folderSummaries,
            libraryVideoCount = state.libraryVideoCount,
            libraryDurationMs = 123_456_000L,
            librarySizeBytes = 98_000_000_000L,
            selectedFolder = state.selectedFolder,
            layout = layout,
            onLayoutChange = {},
            onSelect = {},
            onAddFolder = {},
            onDismiss = {}
        )
    }

    // ---- other sizes ----------------------------------------------------------------------------------------------

    @Config(qualifiers = SMALL_PHONE)
    @Test fun smallPhoneGrid() = shotLibrary("size_small_phone_grid", LibraryViewPrefs(), selected = manyFolders[9])

    @Config(qualifiers = SMALL_PHONE)
    @Test fun smallPhoneList() = shotLibrary("size_small_phone_list", LibraryViewPrefs(LibraryLayout.LIST))

    @Test fun phoneGridFont200() = shotLibrary("size_phone_grid_font200", LibraryViewPrefs(), fontScale = 2f, selected = manyFolders[5])
    @Test fun phoneListFont200() = shotLibrary("size_phone_list_font200", LibraryViewPrefs(LibraryLayout.LIST), fontScale = 2f)

    @Config(qualifiers = MEDIUM)
    @Test fun mediumList() = shotLibrary("size_medium_list", LibraryViewPrefs(LibraryLayout.LIST), selected = manyFolders[4])

    @Config(qualifiers = MEDIUM)
    @Test fun mediumGridCompact() = shotLibrary("size_medium_grid_compact", LibraryViewPrefs(density = LibraryDensity.COMPACT))

    @Config(qualifiers = LANDSCAPE)
    @Test fun phoneLandscapeGrid() = shotLibrary("size_phone_landscape_grid", LibraryViewPrefs(), selected = manyFolders[6])

    @Config(qualifiers = LANDSCAPE)
    @Test fun phoneLandscapeList() = shotLibrary("size_phone_landscape_list", LibraryViewPrefs(LibraryLayout.LIST))

    @Config(qualifiers = TABLET)
    @Test fun tabletList() = shotLibrary("size_tablet_list", LibraryViewPrefs(LibraryLayout.LIST), selected = manyFolders[24])

    @Config(qualifiers = TABLET)
    @Test fun tabletGridLargeDark() = shotLibrary("size_tablet_grid_large_dark", LibraryViewPrefs(density = LibraryDensity.LARGE), ThemeMode.DARK)

    @Config(qualifiers = TABLET)
    @Test fun tabletGridCompact() = shotLibrary("size_tablet_grid_compact", LibraryViewPrefs(density = LibraryDensity.COMPACT), selected = manyFolders[12])
}
