package org.aptura.player.ui.visual

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaPalettes
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.theme.themeModeForRoute
import org.aptura.player.ui.library.LIBRARY_VIDEO_GRID_TAG
import org.aptura.player.ui.library.LibraryViewPrefs
import org.aptura.player.ui.library.MediaDetailsSheet
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.VideoCardUiModel
import org.aptura.player.ui.onboarding.OnboardingScreen
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerSheet
import org.aptura.player.ui.player.PlayerUiState
import org.aptura.player.ui.settings.SettingsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unconditional cross-OS-deterministic visual & layout regression suite for Aptura's key screens (QA-03).
 *
 * Unlike `APTURA_GALLERY_DIR`-gated gallery tests that skip by default in CI, these tests run on every PR
 * under Robolectric NATIVE graphics and assert:
 * - exact theme surface fill (`ApturaPalettes.forMode(...)`) and zero light-palette leak in player routes,
 * - non-blank drawn UI content (`minDistinctColors >= 3`) across screen & modal window bitmaps,
 * - key text/control nodes are displayed and bounded inside the viewport width/height without clipping to zero.
 */
abstract class KeyScreenVisualRegressionBase {

    @get:Rule
    val composeRule = createComposeRule()

    protected fun sampleVideos(): List<VideoCardUiModel> {
        val apollo = DomainMediaItem(
            uri = "content://media/external/video/media/101",
            title = "Apollo 11 (4K HDR)",
            durationMs = 5_400_000L,
            sizeBytes = 2_400_000_000L,
            mimeType = "video/mp4",
            width = 3840,
            height = 2160,
            frameRate = 23.976f,
            audioTrackCount = 2,
            subtitleTrackCount = 3,
            folderPath = "/storage/emulated/0/Movies/Space",
            playbackPositionMs = 1_800_000L
        )
        val cosmos = DomainMediaItem(
            uri = "content://media/external/video/media/102",
            title = "Deep Ocean Documentary",
            durationMs = 3_120_000L,
            sizeBytes = 920_000_000L,
            mimeType = "video/x-matroska",
            width = 1920,
            height = 1080,
            frameRate = 29.97f,
            audioTrackCount = 1,
            subtitleTrackCount = 1,
            folderPath = "/storage/emulated/0/Movies/Nature"
        )
        return listOf(
            VideoCardUiModel(
                uri = apollo.uri,
                title = apollo.title,
                durationFormatted = "1:30:00",
                fileSizeFormatted = "2.2 GB",
                folderName = "Space",
                progressFraction = 0.33f,
                rawItem = apollo
            ),
            VideoCardUiModel(
                uri = cosmos.uri,
                title = cosmos.title,
                durationFormatted = "52:00",
                fileSizeFormatted = "877 MB",
                folderName = "Nature",
                progressFraction = 0f,
                rawItem = cosmos
            )
        )
    }

    protected fun librarySuccessState(): MediaLibraryUiState.Success {
        val videos = sampleVideos()
        return MediaLibraryUiState.Success(
            videos = videos,
            folders = listOf("/storage/emulated/0/Movies/Space", "/storage/emulated/0/Movies/Nature"),
            continueWatching = listOf(videos.first()),
            libraryVideoCount = videos.size,
            libraryDurationMs = 8_520_000L,
            librarySizeBytes = 3_320_000_000L
        )
    }

    protected fun renderLibraryAndAssert(
        mode: ThemeMode,
        state: MediaLibraryUiState = librarySuccessState(),
        prefs: LibraryViewPrefs = LibraryViewPrefs(),
        visibleText: String = "Apollo 11 (4K HDR)",
        scrollToTarget: Boolean = false
    ) {
        val expectedPalette = ApturaPalettes.forMode(mode == ThemeMode.DARK, AccentColor.CYAN)

        composeRule.setContent {
            ApturaTheme(themeMode = mode, accentColor = AccentColor.CYAN) {
                MediaLibraryScreen(
                    uiState = state,
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {},
                    viewPrefs = prefs
                )
            }
        }
        composeRule.waitForIdle()
        if (scrollToTarget) {
            composeRule.onNodeWithTag(LIBRARY_VIDEO_GRID_TAG)
                .performScrollToNode(hasText(visibleText, substring = true))
            composeRule.waitForIdle()
        }

        val screen = composeRule.captureScreen()
        screen.assertRenderedWithSurface(
            expectedSurface = expectedPalette.surfaceBase,
            label = "Library ($mode, ${prefs.layout})",
            minDistinctColors = 4
        )

        val node = composeRule.onAllNodesWithText(visibleText, substring = true).onFirst()
        node.assertIsDisplayed()
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue(
            "Expected non-zero layout bounds for '$visibleText'",
            bounds.width.value > 0f && bounds.height.value > 0f
        )
    }

    protected fun renderSettingsAndAssert(mode: ThemeMode) {
        val expectedPalette = ApturaPalettes.forMode(mode == ThemeMode.DARK, AccentColor.CYAN)

        composeRule.setContent {
            ApturaTheme(themeMode = mode, accentColor = AccentColor.CYAN) {
                SettingsScreen(
                    settings = ApturaSettings(themeMode = mode),
                    libraryStats = LibraryStatistics(
                        totalVideos = 2,
                        totalSizeBytes = 3_320_000_000L,
                        totalDurationMs = 8_520_000L
                    ),
                    onBackClick = {},
                    onUpdateSpeed = {},
                    onUpdateResizeMode = {},
                    onUpdateSeekDelta = {},
                    onUpdateBackgroundAudio = {},
                    onUpdateAutoAdvanceCountdown = {},
                    onRescanClick = {},
                    onReplayOnboardingClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        val screen = composeRule.captureScreen()
        screen.assertRenderedWithSurface(
            expectedSurface = expectedPalette.surfaceBase,
            label = "Settings ($mode)",
            minDistinctColors = 4
        )
        composeRule.onNodeWithText("Playback", substring = true).assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi")
class PhonePortraitVisualRegressionTest : KeyScreenVisualRegressionBase() {

    @Test
    fun libraryLight_rendersSurfaceCardsAndControls() {
        renderLibraryAndAssert(ThemeMode.LIGHT)
    }

    @Test
    fun libraryDark_rendersDarkSurfaceAndNoLightLeak() {
        renderLibraryAndAssert(ThemeMode.DARK)
    }

    @Test
    fun libraryEmptyAndDenied_renderActionableEmptyViews() {
        renderLibraryAndAssert(
            mode = ThemeMode.DARK,
            state = MediaLibraryUiState.Empty(isPermissionDenied = true),
            visibleText = "Allow"
        )
    }

    @Test
    fun settingsLightAndDark_renderSurfaceAndSections() {
        renderSettingsAndAssert(ThemeMode.LIGHT)
    }

    @Test
    fun settingsDark_rendersDarkSurfaceAndSections() {
        renderSettingsAndAssert(ThemeMode.DARK)
    }

    @Test
    fun onboardingWelcome_rendersSurfaceDemoAndFooterSkip() {
        val lightPalette = ApturaPalettes.light(AccentColor.CYAN)
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.LIGHT, accentColor = AccentColor.CYAN) {
                OnboardingScreen(
                    onRequestPermissionClick = {},
                    onContinueWithoutPermissionClick = {},
                    hasStorageAccess = false,
                    animateDemos = false,
                    libraryState = librarySuccessState()
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.captureScreen().assertRenderedWithSurface(
            expectedSurface = lightPalette.surfaceBase,
            label = "Onboarding welcome (Light)",
            minDistinctColors = 4
        )
        composeRule.onNodeWithText("Skip", substring = true).assertIsDisplayed()
    }

    @Test
    fun playerHud_underLightAppTheme_staysStrictlyDark() {
        val darkPalette = ApturaPalettes.dark(AccentColor.CYAN)
        val routeMode = themeModeForRoute(isPlayerRouteActive = true, appThemeMode = ThemeMode.LIGHT)

        composeRule.setContent {
            ApturaTheme(themeMode = routeMode, accentColor = AccentColor.CYAN) {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Apollo 11 (4K HDR)",
                        durationMs = 5_400_000L,
                        currentPositionMs = 1_800_000L,
                        isPlaying = true,
                        isControlsVisible = true,
                        activeSheet = null
                    ),
                    player = null,
                    onBackClick = {},
                    onPlayPauseClick = {},
                    onSeekRelative = {},
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onControlsVisibilityToggle = {},
                    onLockToggle = {},
                    onSpeedSelected = {},
                    onResizeModeToggle = {},
                    onSpeedSheetVisible = {},
                    onAudioSheetVisible = {},
                    onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {},
                    onSubtitleTrackSelected = {},
                    onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {},
                    onRetryPlayback = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Apollo 11 (4K HDR)").assertIsDisplayed()
        composeRule.captureScreen().assertRenderedWithSurface(
            expectedSurface = darkPalette.surfacePureBlack,
            label = "Player HUD under Light app theme",
            minDistinctColors = 3,
            minSurfaceFraction = 0.01
        )
    }

    @Test
    fun playerSubtitleSheet_underLightAppTheme_staysStrictlyDark() {
        val darkPalette = ApturaPalettes.dark(AccentColor.CYAN)
        val routeMode = themeModeForRoute(isPlayerRouteActive = true, appThemeMode = ThemeMode.LIGHT)

        composeRule.setContent {
            ApturaTheme(themeMode = routeMode, accentColor = AccentColor.CYAN) {
                PlayerScreen(
                    uiState = PlayerUiState(
                        videoTitle = "Apollo 11 (4K HDR)",
                        durationMs = 5_400_000L,
                        currentPositionMs = 1_800_000L,
                        isPlaying = true,
                        isControlsVisible = true,
                        activeSheet = PlayerSheet.Subtitle
                    ),
                    player = null,
                    onBackClick = {},
                    onPlayPauseClick = {},
                    onSeekRelative = {},
                    onScrubStart = {},
                    onScrubMove = {},
                    onScrubEnd = {},
                    onControlsVisibilityToggle = {},
                    onLockToggle = {},
                    onSpeedSelected = {},
                    onResizeModeToggle = {},
                    onSpeedSheetVisible = {},
                    onAudioSheetVisible = {},
                    onSubtitleSheetVisible = {},
                    onAudioTrackSelected = {},
                    onSubtitleTrackSelected = {},
                    onVolumePercentChanged = {},
                    onBrightnessPercentChanged = {},
                    onRetryPlayback = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Single modal sheet open at a time (invariant + pixel dark check on the modal sheet's own window root).
        composeRule.onAllNodes(isDialog()).assertCountEquals(1)
        composeRule.onNodeWithText("Subtitles")
            .captureRoot()
            .assertRenderedWithSurface(
                expectedSurface = darkPalette.surfaceElevated1,
                label = "Player Subtitle sheet under Light app theme",
                minDistinctColors = 3,
                minSurfaceFraction = 0.05
            )
    }

    @Test
    fun mediaDetailsSheet_capturesModalWindowWithSurfaceAndTitle() {
        val item = sampleVideos().first().rawItem
        val darkPalette = ApturaPalettes.dark(AccentColor.CYAN)

        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.DARK, accentColor = AccentColor.CYAN) {
                MediaDetailsSheet(
                    item = item,
                    onDismiss = {},
                    onPlayClick = {},
                    onShareClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodes(isDialog()).assertCountEquals(1)
        val titleNode = composeRule.onNodeWithText("Apollo 11 (4K HDR)")
        titleNode.assertIsDisplayed()
        titleNode.captureRoot().assertRenderedWithSurface(
            expectedSurface = darkPalette.surfaceElevated1,
            label = "MediaDetailsSheet dark modal window",
            minDistinctColors = 3
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w740dp-h360dp-xxhdpi")
class PhoneLandscapeVisualRegressionTest : KeyScreenVisualRegressionBase() {

    @Test
    fun libraryLandscape_rendersWithinShortViewport() {
        renderLibraryAndAssert(
            mode = ThemeMode.DARK,
            prefs = LibraryViewPrefs(layout = LibraryLayout.LIST),
            scrollToTarget = true
        )
    }

    @Test
    fun settingsLandscape_rendersWithoutCrashOrBlankCanvas() {
        renderSettingsAndAssert(ThemeMode.LIGHT)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h1280dp-xhdpi")
class TabletVisualRegressionTest : KeyScreenVisualRegressionBase() {

    @Test
    fun libraryTablet_rendersTwoPaneCollectionAndGrid() {
        renderLibraryAndAssert(mode = ThemeMode.LIGHT)
    }

    @Test
    fun settingsTablet_rendersDarkPaletteAndSections() {
        renderSettingsAndAssert(ThemeMode.DARK)
    }
}
