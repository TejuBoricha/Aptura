package org.aptura.player.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.DiagnosticStore
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.ui.library.LibrarySortOrder
import org.aptura.player.ui.library.MediaFormatters
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.VideoCardUiModel
import org.aptura.player.ui.settings.SettingsScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The two user-facing halves of diagnostics persistence: the quiet post-crash prompt on the library, and Settings'
 * "Delete Diagnostic Data". They run against the real [ApturaDiagnostics] and real files, so "deleted" means the
 * files are gone, not that a callback fired.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiagnosticsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var originalHandler: Thread.UncaughtExceptionHandler
    private lateinit var store: DiagnosticStore

    @Before
    fun setUp() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler() ?: Thread.UncaughtExceptionHandler { _, _ -> }
        store = DiagnosticStore(File(folder.root, "diagnostics"))
        ApturaDiagnostics.ringBuffer.clear()
        ApturaDiagnostics.initialize(
            context,
            diagnosticStore = store,
            executor = { it.run() },
            installCrashHandler = false,
            previousExit = null
        )
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        ApturaDiagnostics.ringBuffer.clear()
    }

    private val item = DomainMediaItem(
        uri = "content://media/1",
        title = "Oppenheimer",
        durationMs = 10_800_000L,
        sizeBytes = 8_589_934_592L,
        mimeType = "video/mp4",
        folderPath = "/storage/Movies"
    )

    private fun libraryState() = MediaLibraryUiState.Success(
        videos = listOf(
            VideoCardUiModel(
                uri = item.uri,
                title = item.title,
                durationFormatted = MediaFormatters.formatDuration(item.durationMs),
                fileSizeFormatted = MediaFormatters.formatFileSize(item.sizeBytes),
                folderName = "Movies",
                rawItem = item
            )
        ),
        folders = listOf("Movies"),
        selectedFolder = null,
        searchQuery = "",
        sortOrder = LibrarySortOrder.DATE_ADDED_DESC
    )

    private fun showLibrary(showCrashNotice: Boolean, onShare: () -> Unit = {}, onDismiss: () -> Unit = {}) {
        composeRule.setContent {
            ApturaTheme {
                MediaLibraryScreen(
                    uiState = libraryState(),
                    onVideoClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onFolderSelected = {},
                    onSortOrderChanged = {},
                    onRefresh = {},
                    showCrashNotice = showCrashNotice,
                    onShareCrashDetails = onShare,
                    onDismissCrashNotice = onDismiss
                )
            }
        }
    }

    private fun showSettings(onDelete: (() -> Unit)? = null) {
        composeRule.setContent {
            ApturaTheme {
                if (onDelete == null) {
                    SettingsScreen(
                        settings = ApturaSettings(),
                        libraryStats = LibraryStatistics(),
                        onBackClick = {},
                        onUpdateSpeed = {},
                        onUpdateResizeMode = {},
                        onUpdateSeekDelta = {},
                        onUpdateBackgroundAudio = {},
                        onUpdateAutoAdvanceCountdown = {},
                        onRescanClick = {},
                        onReplayOnboardingClick = {}
                    )
                } else {
                    SettingsScreen(
                        settings = ApturaSettings(),
                        libraryStats = LibraryStatistics(),
                        onBackClick = {},
                        onUpdateSpeed = {},
                        onUpdateResizeMode = {},
                        onUpdateSeekDelta = {},
                        onUpdateBackgroundAudio = {},
                        onUpdateAutoAdvanceCountdown = {},
                        onRescanClick = {},
                        onReplayOnboardingClick = {},
                        onDeleteDiagnosticData = onDelete
                    )
                }
            }
        }
    }

    private fun openDeleteDialog() {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Delete diagnostic data"))
        composeRule.onNodeWithText("Delete diagnostic data").performClick()
    }

    // ---- post-crash prompt ----------------------------------------------------------------------------------------

    @Test
    fun theCrashPrompt_isShownOnTheLibrary_whenThereIsAnUnseenCrash() {
        showLibrary(showCrashNotice = true)

        composeRule.onNodeWithText("Aptura closed unexpectedly last time").assertIsDisplayed()
        composeRule.onNodeWithText("Share details").assertIsDisplayed()
        composeRule.onNodeWithText("Not now").assertIsDisplayed()
        composeRule.onNodeWithText("Oppenheimer").assertIsDisplayed() // never blocks the library
    }

    @Test
    fun theCrashPrompt_isAbsent_whenNothingCrashed() {
        showLibrary(showCrashNotice = false)

        composeRule.onNodeWithText("Oppenheimer").assertIsDisplayed()
        assertTrue(composeRule.onAllNodes(hasText("closed unexpectedly", substring = true)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theCrashPromptButtons_callTheirHandlers() {
        var shared = 0
        var dismissed = 0
        showLibrary(showCrashNotice = true, onShare = { shared++ }, onDismiss = { dismissed++ })

        composeRule.onNodeWithText("Share details").performClick()
        composeRule.onNodeWithText("Not now").performClick()

        assertEquals(1, shared)
        assertEquals(1, dismissed)
    }

    // ---- Settings → Delete Diagnostic Data -------------------------------------------------------------------------

    @Test
    fun cancelling_theDeleteDialog_keepsEverything() {
        ApturaDiagnostics.logEvent(EventCode.EV_011, payload = mapOf("source" to "player_vm"))
        store.writeCrash(System.currentTimeMillis(), "boom")
        assertEquals("precondition: a crash is on disk", 1, store.loadCrashes().size)
        showSettings()

        openDeleteDialog()
        composeRule.onNodeWithText("Delete diagnostic data?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        assertTrue(store.loadEvents().isNotEmpty())
        assertEquals(1, store.loadCrashes().size)
    }

    @Test
    fun confirming_theDeleteDialog_removesTheFilesAndTheInMemoryLog() {
        ApturaDiagnostics.logEvent(EventCode.EV_011, payload = mapOf("source" to "player_vm"))
        store.writeCrash(System.currentTimeMillis(), "boom")
        assertTrue(store.loadEvents().isNotEmpty())
        assertEquals("precondition: a crash is on disk", 1, store.loadCrashes().size)
        showSettings() // the real default handler: ApturaDiagnostics.clearAll()

        openDeleteDialog()
        composeRule.onNodeWithText("Delete").performClick()

        assertTrue("events file is empty", store.loadEvents().isEmpty())
        assertTrue("crash reports are gone", store.loadCrashes().isEmpty())
        assertEquals(0, ApturaDiagnostics.ringBuffer.size)
        assertFalse(File(folder.root, "diagnostics/${DiagnosticStore.EVENTS_FILE}").exists())
    }

    @Test
    fun confirming_callsTheDeleteHandlerOnce_andCancelNeverDoes() {
        var deletions = 0
        showSettings(onDelete = { deletions++ })

        openDeleteDialog()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(0, deletions)

        openDeleteDialog()
        composeRule.onNodeWithText("Delete").performClick()
        assertEquals(1, deletions)
    }
}
