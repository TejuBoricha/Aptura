package org.aptura.player.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.aptura.player.theme.ApturaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h1400dp-xhdpi")
class FolderDestinationSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleFolders = listOf("/storage/emulated/0/Movies", "/storage/emulated/0/DCIM/Camera", "/storage/emulated/0/Download")
    private val sampleLabels = mapOf(
        "/storage/emulated/0/Movies" to "Movies",
        "/storage/emulated/0/DCIM/Camera" to "Camera",
        "/storage/emulated/0/Download" to "Download"
    )
    private val sampleSummaries = mapOf(
        "/storage/emulated/0/Movies" to FolderSummary(videoCount = 10, totalBytes = 4_000_000_000L, totalDurationMs = 12_000_000L),
        "/storage/emulated/0/DCIM/Camera" to FolderSummary(videoCount = 42, totalBytes = 15_000_000_000L, totalDurationMs = 36_000_000L),
        "/storage/emulated/0/Download" to FolderSummary(videoCount = 3, totalBytes = 500_000_000L, totalDurationMs = 1_500_000L)
    )

    @Test
    fun displaysFoldersWithCountsAndSummaries() {
        composeRule.setContent {
            ApturaTheme {
                FolderDestinationSheetContent(
                    title = "Move 2 videos to...",
                    folders = sampleFolders,
                    labels = sampleLabels,
                    summaries = sampleSummaries,
                    currentFolder = "/storage/emulated/0/DCIM/Camera",
                    onSelectDestination = {},
                    onBrowseStorage = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Move 2 videos to...").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a destination folder").assertIsDisplayed()
        composeRule.onNodeWithText("Movies").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Download").assertIsDisplayed()
        composeRule.onNodeWithText("10 videos", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Browse device storage...").assertIsDisplayed()
    }

    @Test
    fun currentFolder_isMarkedAndDisabled() {
        var selectedDestination: String? = null
        composeRule.setContent {
            ApturaTheme {
                FolderDestinationSheetContent(
                    title = "Move 1 video to...",
                    folders = sampleFolders,
                    labels = sampleLabels,
                    summaries = sampleSummaries,
                    currentFolder = "/storage/emulated/0/DCIM/Camera",
                    onSelectDestination = { selectedDestination = it },
                    onBrowseStorage = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("(Current)").assertIsDisplayed()
        // Tapping the current folder row should not invoke selection
        composeRule.onNodeWithText("Camera").performClick()
        assertNull(selectedDestination)
    }

    @Test
    fun selectingDestinationFolder_invokesCallback() {
        var selectedDestination: String? = null
        composeRule.setContent {
            ApturaTheme {
                FolderDestinationSheetContent(
                    title = "Move 1 video to...",
                    folders = sampleFolders,
                    labels = sampleLabels,
                    summaries = sampleSummaries,
                    currentFolder = "/storage/emulated/0/DCIM/Camera",
                    onSelectDestination = { selectedDestination = it },
                    onBrowseStorage = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Movies").performClick()
        assertEquals("/storage/emulated/0/Movies", selectedDestination)
    }

    @Test
    fun clickingBrowseStorage_invokesBrowseCallback() {
        var browseClicked = false
        composeRule.setContent {
            ApturaTheme {
                FolderDestinationSheetContent(
                    title = "Copy 3 videos to...",
                    folders = sampleFolders,
                    labels = sampleLabels,
                    summaries = sampleSummaries,
                    currentFolder = null,
                    onSelectDestination = {},
                    onBrowseStorage = { browseClicked = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BROWSE_STORAGE_BUTTON_TAG).performClick()
        assertTrue(browseClicked)
    }

    @Test
    fun emptyFolders_showsEmptyNotice() {
        composeRule.setContent {
            ApturaTheme {
                FolderDestinationSheetContent(
                    title = "Move 1 video to...",
                    folders = emptyList(),
                    labels = emptyMap(),
                    summaries = emptyMap(),
                    currentFolder = null,
                    onSelectDestination = {},
                    onBrowseStorage = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No in-app folders found").assertIsDisplayed()
    }
}
