package org.aptura.player.ui.visual

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.BROWSE_FOLDERS_TAG
import org.aptura.player.ui.library.LIBRARY_VIDEO_GRID_TAG
import org.aptura.player.ui.library.LibraryViewPrefs
import org.aptura.player.ui.library.MediaDetailsSheet
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.settings.SettingsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every screen except the player and the guide (which have their own galleries) on a phone held sideways, which is 891 x
 * 411 dp on a current phone and 740 x 360 dp on a small or older one. These widths are past the 700 dp that switches the
 * library to the tablet layout, but with a third of a tablet's height. A review tool like the rest of the gallery: it
 * runs only when `APTURA_GALLERY_DIR` is set.
 */
abstract class LandscapePhoneScreens(private val size: String) : GalleryBase() {

    private fun library(
        state: MediaLibraryUiState,
        mode: ThemeMode = ThemeMode.LIGHT,
        prefs: LibraryViewPrefs = LibraryViewPrefs(),
        showCrash: Boolean = false
    ) = themed(mode) {
        MediaLibraryScreen(
            uiState = state,
            onVideoClick = { _, _ -> },
            onSearchQueryChanged = {},
            onFolderSelected = {},
            onSortOrderChanged = {},
            onRefresh = {},
            showCrashNotice = showCrash,
            viewPrefs = prefs
        )
    }

    private fun settings(mode: ThemeMode) = themed(mode) {
        SettingsScreen(
            settings = ApturaSettings(
                themeMode = mode,
                accentColor = AccentColor.CYAN,
                persistedSafFolders = setOf("content://com.android.externalstorage.documents/tree/primary%3AMovies")
            ),
            libraryStats = LibraryStatistics(totalVideos = 8, totalDurationMs = 33_802_000, totalSizeBytes = 14_400_000_000),
            onBackClick = {}, onUpdateSpeed = {}, onUpdateResizeMode = {}, onUpdateSeekDelta = {},
            onUpdateBackgroundAudio = {}, onUpdateAutoAdvanceCountdown = {}, onRescanClick = {},
            onReplayOnboardingClick = {}, hiddenVideoCount = 2, missingVideoCount = 1
        )
    }

    private fun name(what: String) = "land_${size}_$what"

    /** On a short screen the first card sits below the Continue watching shelf, so scroll to it before touching it. */
    private fun openFirstCardMenu() {
        composeRule.onNodeWithTag(LIBRARY_VIDEO_GRID_TAG).performScrollToNode(hasContentDescription("More actions for", substring = true))
        composeRule.onAllNodesWithContentDescription("More actions for", substring = true).onFirst().performClick()
        settle()
    }

    @Test fun libraryHome() { library(librarySuccess()); shoot(name("library_home")) }
    @Test fun libraryHomeDark() { library(librarySuccess(), ThemeMode.DARK); shoot(name("library_home_dark")) }
    @Test fun libraryList() { library(librarySuccess(), prefs = LibraryViewPrefs(layout = LibraryLayout.LIST)); shoot(name("library_list")) }
    @Test fun libraryBanners() { library(librarySuccess(limited = true), showCrash = true); shoot(name("library_banners")) }
    @Test fun libraryEmpty() { library(MediaLibraryUiState.Empty()); shoot(name("library_empty")) }
    @Test fun libraryDenied() { library(MediaLibraryUiState.Empty(isPermissionDenied = true)); shoot(name("library_denied")) }
    @Test fun libraryLoading() { library(MediaLibraryUiState.Loading); shoot(name("library_loading")) }
    @Test fun libraryNoMatch() { library(MediaLibraryUiState.Empty(isFiltered = true, query = "matrix")); shoot(name("library_nomatch")) }

    @Test fun libraryFolder() {
        library(librarySuccess(items = sampleItems.filter { it.folderPath.endsWith("Series") }, selectedFolder = "/storage/emulated/0/Series", continueWatching = false))
        shoot(name("library_folder"))
    }

    @Test fun librarySelection() {
        library(librarySuccess(continueWatching = false))
        composeRule.onNode(hasText("Blade Runner 2049")).performTouchInput { longClick() }
        settle()
        shoot(name("library_selection"))
    }

    @Test fun libraryFolderBrowser() {
        library(librarySuccess())
        composeRule.onNodeWithTag(BROWSE_FOLDERS_TAG).performClick()
        settle()
        shoot(name("library_folder_browser"))
    }

    @Test fun libraryViewSheet() {
        library(librarySuccess())
        composeRule.onAllNodesWithContentDescription("View options").onFirst().performClick()
        settle()
        shoot(name("library_view_sheet"))
    }

    @Test fun libraryCardMenu() {
        library(librarySuccess())
        openFirstCardMenu()
        shoot(name("library_card_menu"))
    }

    @Test fun libraryDeleteDialog() {
        library(librarySuccess())
        openFirstCardMenu()
        composeRule.onNode(hasText("Delete", substring = true)).performClick()
        settle()
        shoot(name("library_delete_dialog"))
    }

    @Test fun libraryMoreFileActions() {
        library(librarySuccess())
        openFirstCardMenu()
        composeRule.onNodeWithText("More file actions").performClick()
        settle()
        shoot(name("library_more_file_actions"))
    }

    @Test fun detailsSheet() {
        themed(ThemeMode.DARK) { MediaDetailsSheet(item = sampleItems.first(), onDismiss = {}, onPlayClick = {}, onShareClick = {}) }
        shoot(name("details_sheet"))
    }

    @Test fun settingsTop() { settings(ThemeMode.LIGHT); shoot(name("settings_top")) }

    @Test fun settingsScrolled() {
        settings(ThemeMode.DARK)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Default aspect ratio"))
        settle()
        shoot(name("settings_scrolled"))
    }

    @Test fun settingsResetDialog() {
        settings(ThemeMode.LIGHT)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Reset"))
        composeRule.onNodeWithText("Reset").performClick()
        settle()
        shoot(name("settings_reset_dialog"))
    }

    @Test fun settingsCodecDialog() {
        settings(ThemeMode.LIGHT)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Codec support"))
        composeRule.onNodeWithText("Codec support").performClick()
        settle()
        shoot(name("settings_codec_dialog"))
    }

}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-xhdpi")
class LandscapePhoneGalleryTest : LandscapePhoneScreens("891x411")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w740dp-h360dp-xhdpi")
class LandscapeSmallPhoneGalleryTest : LandscapePhoneScreens("740x360")
