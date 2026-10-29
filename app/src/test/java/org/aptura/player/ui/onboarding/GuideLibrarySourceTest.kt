package org.aptura.player.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.VideoCardUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Replaying the guide from Settings shows the person's own library on its library page (point 4: no stand-ins where the
 * real thing is available). The first run, and any library too small or filtered to fill the picture, keep the samples.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class GuideLibrarySourceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun card(index: Int, missing: Boolean = false): VideoCardUiModel {
        val item = DomainMediaItem(
            uri = "content://media/$index",
            title = "Holiday $index",
            durationMs = 60_000L * (index + 1),
            sizeBytes = 50_000_000L,
            mimeType = "video/mp4",
            folderPath = "/storage/emulated/0/Movies",
            isMissing = missing
        )
        return VideoCardUiModel(
            uri = item.uri,
            title = item.title,
            durationFormatted = "${index + 1}:00",
            fileSizeFormatted = "48 MB",
            folderName = "Movies",
            isMissing = missing,
            rawItem = item
        )
    }

    private fun state(
        videos: List<VideoCardUiModel> = (0 until 6).map { card(it) },
        selectedFolder: String? = null,
        searchQuery: String = ""
    ) = MediaLibraryUiState.Success(
        videos = videos,
        folders = (0 until 12).map { "/storage/emulated/0/Folder $it" },
        selectedFolder = selectedFolder,
        searchQuery = searchQuery,
        libraryVideoCount = 321,
        folderCounts = mapOf("/storage/emulated/0/Folder 0" to 9)
    )

    @Test
    fun aLibraryWithEnoughVideos_isShownAsItIs() {
        val library = guideLibraryFrom(state())
        assertNotNull(library)
        library!!
        assertEquals(listOf("Holiday 0", "Holiday 1", "Holiday 2", "Holiday 3"), library.videos.map { it.card.title })
        // Their own frames: no drawn stand-in is chosen for them.
        assertTrue(library.videos.all { it.still == null })
        assertEquals(321, library.totalCount)
        assertEquals("a long folder list is cut to what the bar can show at a glance", 8, library.folders.size)
    }

    @Test
    fun missingFilesAreNotShown() {
        val videos = listOf(card(0, missing = true), card(1), card(2), card(3, missing = true), card(4), card(5))
        val library = guideLibraryFrom(state(videos))!!
        assertEquals(listOf("Holiday 1", "Holiday 2", "Holiday 4", "Holiday 5"), library.videos.map { it.card.title })
    }

    @Test
    fun aSmallLibrary_keepsTheSamples() {
        assertNull(guideLibraryFrom(state(videos = (0 until 3).map { card(it) })))
        assertNull("missing files do not count towards the four", guideLibraryFrom(state(videos = listOf(card(0), card(1), card(2), card(3, missing = true)))))
        assertNull(guideLibraryFrom(state(videos = emptyList())))
    }

    @Test
    fun aFilteredLibrary_keepsTheSamples_soThePictureNeverContradictsItsFolderBar() {
        assertNull(guideLibraryFrom(state(selectedFolder = "/storage/emulated/0/Folder 3")))
        assertNull(guideLibraryFrom(state(searchQuery = "holiday")))
    }

    @Test
    fun theLibraryPage_showsTheirVideos_withNoDrawnStandIns() {
        val library = guideLibraryFrom(state())!!
        composeRule.setContent {
            ApturaTheme(themeMode = ThemeMode.DARK) {
                CompositionLocalProvider(LocalGuideMotion provides false) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) { LibraryDemo(library = library) }
                }
            }
        }
        composeRule.waitForIdle()

        library.videos.forEach { composeRule.onNodeWithText(it.card.title, useUnmergedTree = true).assertExists() }
        composeRule.onNodeWithText("Beach day", useUnmergedTree = true).assertDoesNotExist()
        val drawn = composeRule.onAllNodes(hasTestTag("guide_still_0") or hasTestTag("guide_still_2") or hasTestTag("guide_still_3") or hasTestTag("guide_still_4"), useUnmergedTree = true)
        assertEquals("their own frames, not drawn ones", 0, drawn.fetchSemanticsNodes().size)
        // The folder bar carries their folder and its count.
        composeRule.onNodeWithText("321", substring = true, useUnmergedTree = true).assertExists()
    }
}
