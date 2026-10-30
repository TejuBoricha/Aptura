package org.aptura.player.ui.visual

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.MediaDetailsSheet
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PHONE = "w411dp-h891dp-xhdpi"
private const val TABLET = "w1280dp-h800dp-xhdpi"
private const val FOLDABLE = "w673dp-h841dp-xhdpi"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class LibraryGalleryTest : GalleryBase() {

    private fun library(state: MediaLibraryUiState, mode: ThemeMode = ThemeMode.LIGHT, accent: AccentColor = AccentColor.CYAN) =
        themed(mode, accent) {
            MediaLibraryScreen(
                uiState = state,
                onVideoClick = { _, _ -> },
                onSearchQueryChanged = {},
                onFolderSelected = {},
                onSortOrderChanged = {},
                onRefresh = {}
            )
        }

    @Test fun phoneLight() { library(librarySuccess()); shoot("library_phone_light") }
    @Test fun phoneDark() { library(librarySuccess(), ThemeMode.DARK); shoot("library_phone_dark") }
    @Test fun phoneAmberDark() { library(librarySuccess(), ThemeMode.DARK, AccentColor.AMBER); shoot("library_phone_dark_amber") }
    @Test fun phoneFolder() {
        library(librarySuccess(items = sampleItems.filter { it.folderPath.endsWith("Series") }, selectedFolder = "/storage/emulated/0/Series", continueWatching = false))
        shoot("library_phone_folder")
    }
    @Test fun phoneLimitedAndCrash() {
        themed(ThemeMode.LIGHT) {
            MediaLibraryScreen(
                uiState = librarySuccess(limited = true),
                onVideoClick = { _, _ -> }, onSearchQueryChanged = {}, onFolderSelected = {}, onSortOrderChanged = {},
                onRefresh = {}, showCrashNotice = true
            )
        }
        shoot("library_phone_banners")
    }
    @Test fun phoneLoading() { library(MediaLibraryUiState.Loading); shoot("library_phone_loading") }
    @Test fun phoneEmpty() { library(MediaLibraryUiState.Empty()); shoot("library_phone_empty") }
    @Test fun phoneEmptyDark() { library(MediaLibraryUiState.Empty(), ThemeMode.DARK); shoot("library_phone_empty_dark") }
    @Test fun phoneNoMatch() { library(MediaLibraryUiState.Empty(isFiltered = true, query = "matrix")); shoot("library_phone_nomatch") }
    @Test fun phoneDenied() { library(MediaLibraryUiState.Empty(isPermissionDenied = true)); shoot("library_phone_denied") }

    @Test fun phoneOverflowMenu() {
        library(librarySuccess())
        composeRule.onAllNodesWithContentDescription("More actions for", substring = true).onFirst().performClick()
        settle()
        shoot("library_phone_card_menu")
    }

    @Config(qualifiers = "w411dp-h1100dp-xhdpi")
    @Test fun phoneViewSheet() {
        library(librarySuccess())
        composeRule.onAllNodesWithContentDescription("View options").onFirst().performClick()
        settle()
        shoot("library_phone_view_sheet")
    }

    @Test fun phoneSelection() {
        library(librarySuccess(continueWatching = false))
        composeRule.onNode(hasText("Blade Runner 2049")).performTouchInput { longClick() }
        settle()
        shoot("library_phone_selection")
    }

    @Test fun phoneDeleteDialog() {
        library(librarySuccess())
        composeRule.onAllNodesWithContentDescription("More actions for", substring = true).onFirst().performClick()
        settle()
        composeRule.onNode(hasText("Delete", substring = true)).performClick()
        settle()
        shoot("library_phone_delete_dialog")
    }

    @Test fun phoneHideDialog() {
        library(librarySuccess())
        composeRule.onAllNodesWithContentDescription("More actions for", substring = true).onFirst().performClick()
        settle()
        composeRule.onNode(hasText("Hide", substring = true)).performClick()
        settle()
        shoot("library_phone_hide_dialog")
    }

    @Test fun phoneMoreFileActions() {
        library(librarySuccess())
        composeRule.onAllNodesWithContentDescription("More actions for", substring = true).onFirst().performClick()
        settle()
        composeRule.onNodeWithText("More file actions").performClick()
        settle()
        shoot("library_phone_more_file_actions")
    }

    @Test fun detailsSheet() {
        themed(ThemeMode.LIGHT) {
            MediaDetailsSheet(item = sampleItems.first(), onDismiss = {}, onPlayClick = {}, onShareClick = {})
        }
        shoot("details_sheet_light")
    }

    @Test fun detailsSheetDark() {
        themed(ThemeMode.DARK) {
            MediaDetailsSheet(item = sampleItems[2], onDismiss = {}, onPlayClick = {}, onShareClick = {})
        }
        shoot("details_sheet_dark")
    }

    @Config(qualifiers = "w411dp-h2200dp-xhdpi")
    @Test fun detailsSheetTall() {
        themed(ThemeMode.LIGHT) {
            MediaDetailsSheet(item = sampleItems.first(), onDismiss = {}, onPlayClick = {}, onShareClick = {})
        }
        shoot("details_sheet_full")
    }

    @Config(qualifiers = TABLET)
    @Test fun tabletLight() { library(librarySuccess()); shoot("library_tablet_light") }

    @Config(qualifiers = TABLET)
    @Test fun tabletDark() { library(librarySuccess(), ThemeMode.DARK); shoot("library_tablet_dark") }

    @Config(qualifiers = TABLET)
    @Test fun tabletRail() {
        library(librarySuccess())
        composeRule.onAllNodesWithContentDescription("Collapse sidebar").onFirst().performClick()
        settle()
        shoot("library_tablet_rail")
    }

    @Config(qualifiers = TABLET)
    @Test fun tabletSelection() {
        library(librarySuccess(continueWatching = false))
        composeRule.onNode(hasText("Blade Runner 2049")).performTouchInput { longClick() }
        settle()
        shoot("library_tablet_selection")
    }

    @Config(qualifiers = TABLET)
    @Test fun tabletEmpty() { library(MediaLibraryUiState.Empty()); shoot("library_tablet_empty") }

    @Config(qualifiers = FOLDABLE)
    @Test fun foldable() { library(librarySuccess()); shoot("library_foldable") }

    // No rename-dialog shot: under Robolectric's native graphics any dialog with a text field never goes idle
    // (the original AlertDialog version hangs the same way), so it is reviewed on a device instead.

    @Test fun phoneMoveDialogAndClipboard() {
        library(librarySuccess(continueWatching = false))
        composeRule.onNode(hasText("Blade Runner 2049")).performTouchInput { longClick() }
        settle()
        composeRule.onAllNodesWithContentDescription("More actions for selected videos").onFirst().performClick()
        settle()
        shoot("library_phone_selection_menu")
        composeRule.onNodeWithText("Copy").performClick()
        settle()
        shoot("library_phone_clipboard")
        composeRule.onNodeWithText("Move").performClick()
        settle()
        shoot("library_phone_move_dialog")
    }

    @Test fun phoneMissingAndSearch() {
        val items = sampleItems.mapIndexed { i, it -> if (i == 1) it.copy(isMissing = true) else if (i == 3) it.copy(isCompleted = true, playbackPositionMs = it.durationMs) else it }
        library(librarySuccess(items = items.take(4), query = "s", continueWatching = false))
        shoot("library_phone_search_missing")
    }

    @Test fun detailsMissing() {
        themed(ThemeMode.DARK) {
            MediaDetailsSheet(item = sampleItems[1].copy(isMissing = true), onDismiss = {}, onPlayClick = {}, onShareClick = {})
        }
        shoot("details_sheet_missing")
    }
}
