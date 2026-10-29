package org.aptura.player.ui.library

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.MediaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MediaLibraryViewModel and MediaFormatters.
 * Verifies reactive state flow, search filtering, folder segregation, sort order, and formatters.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaLibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMediaRepository : MediaRepository {
        val videosFlow = MutableStateFlow<List<DomainMediaItem>>(emptyList())
        val foldersFlow = MutableStateFlow<List<String>>(emptyList())

        override fun getAllVideos(): Flow<List<DomainMediaItem>> = videosFlow

        override fun getVideosInFolder(folderPath: String): Flow<List<DomainMediaItem>> =
            MutableStateFlow(videosFlow.value.filter { it.folderPath == folderPath })

        override fun getAllFolders(): Flow<List<String>> = foldersFlow

        override fun searchVideos(query: String): Flow<List<DomainMediaItem>> =
            MutableStateFlow(videosFlow.value.filter { it.title.contains(query, ignoreCase = true) })

        override suspend fun getVideoByUri(uri: String): DomainMediaItem? =
            videosFlow.value.firstOrNull { it.uri == uri }

        override suspend fun savePlaybackPosition(uri: String, positionMs: Long, isCompleted: Boolean) {}

        override suspend fun removeVideoFromLibrary(uri: String) {
            videosFlow.value = videosFlow.value.filter { it.uri != uri }
        }

        override suspend fun removeVideosByUriPrefix(uriPrefix: String) {
            videosFlow.value = videosFlow.value.filter { !it.uri.startsWith(uriPrefix) }
        }

        override suspend fun renameVideo(
            uri: String,
            newName: String,
            newUri: String?,
            newDisplayName: String?
        ) {
            videosFlow.value = videosFlow.value.map {
                if (it.uri == uri) it.copy(uri = newUri ?: it.uri, title = newName) else it
            }
        }

        val hiddenUris = MutableStateFlow<Set<String>>(emptySet())
        override suspend fun hideVideo(uri: String) {
            hiddenUris.value = hiddenUris.value + uri
            videosFlow.value = videosFlow.value.filter { it.uri != uri }
        }
        override suspend fun restoreHiddenVideos() { hiddenUris.value = emptySet() }
        override fun getHiddenVideoCount(): Flow<Int> = kotlinx.coroutines.flow.MutableStateFlow(0)

        override fun getBookmarks(mediaUri: String): Flow<List<org.aptura.player.domain.model.Bookmark>> =
            MutableStateFlow(emptyList())

        override suspend fun addBookmark(mediaUri: String, timestampMs: Long, note: String): Long = 0L

        override suspend fun deleteBookmark(id: Long) {}

        override fun getMissingVideos(): Flow<List<DomainMediaItem>> =
            MutableStateFlow(videosFlow.value.filter { it.isMissing })

        override suspend fun clearMissingVideos() {
            videosFlow.value = videosFlow.value.filter { !it.isMissing }
        }

        override suspend fun getLibraryStatistics(): org.aptura.player.domain.repository.LibraryStatistics {
            val list = videosFlow.value
            return org.aptura.player.domain.repository.LibraryStatistics(
                totalVideos = list.size,
                totalDurationMs = list.sumOf { it.durationMs },
                totalSizeBytes = list.sumOf { it.sizeBytes }
            )
        }

        override suspend fun savePlaybackPreferences(
            uri: String,
            resizeMode: String,
            playbackSpeed: Float,
            decoderStrategy: Int?,
            audioDelayMs: Long,
            subtitleDelayMs: Long
        ) {}

        override suspend fun resetPlaybackPreferences(uri: String) {}
    }

    private val item1 = DomainMediaItem(
        uri = "content://media/1",
        title = "Inception",
        durationMs = 8880000L, // 2h 28m = 2:28:00
        sizeBytes = 2147483648L, // 2.0 GB
        mimeType = "video/mp4",
        dateAddedSec = 1000L,
        folderPath = "Movies",
        playbackPositionMs = 4440000L, // 50%
        isCompleted = false
    )

    private val item2 = DomainMediaItem(
        uri = "content://media/2",
        title = "Avatar",
        durationMs = 9720000L,
        sizeBytes = 3221225472L, // 3.0 GB
        mimeType = "video/mkv",
        dateAddedSec = 2000L,
        folderPath = "Movies",
        playbackPositionMs = 0L,
        isCompleted = false
    )

    private val item3 = DomainMediaItem(
        uri = "content://media/3",
        title = "Vacation Clip",
        durationMs = 125000L, // 2m 5s = 2:05
        sizeBytes = 104857600L, // 100 MB
        mimeType = "video/mp4",
        dateAddedSec = 3000L,
        folderPath = "Camera",
        playbackPositionMs = 125000L,
        isCompleted = true
    )

    /** Regression (audit P0-09): the spinner used to flip back to false the instant the scan was merely enqueued. */
    @Test
    fun refreshSpinner_staysOnUntilTheRealScanFinishes() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1)
        val scan = MutableStateFlow(false)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L, scanActivity = scan)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val idle = awaitItem() as MediaLibraryUiState.Success
            assertFalse(idle.isRefreshing)

            viewModel.onRefresh { scan.value = true } // enqueue returns immediately; the work is now running
            var state = awaitItem() as MediaLibraryUiState.Success
            while (!state.isRefreshing) state = awaitItem() as MediaLibraryUiState.Success
            assertTrue("spinner must stay on while the scan runs", state.isRefreshing)

            scan.value = false // worker finished
            state = awaitItem() as MediaLibraryUiState.Success
            while (state.isRefreshing) state = awaitItem() as MediaLibraryUiState.Success
            assertFalse(state.isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun initialState_whenRepositoryHasItems_emitsSuccessWithCards() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2, item3)
        repo.foldersFlow.value = listOf("Camera", "Movies")

        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            // Initial state is Loading
            assertEquals(MediaLibraryUiState.Loading, awaitItem())

            val state = awaitItem()
            assertTrue("Expected Success state but got $state", state is MediaLibraryUiState.Success)
            val success = state as MediaLibraryUiState.Success

            assertEquals(3, success.totalCount)
            assertEquals(3, success.videos.size)
            assertEquals(2, success.folders.size)

            // Verify default sort is DATE_ADDED_DESC (item3: 3000, item2: 2000, item1: 1000)
            assertEquals("Vacation Clip", success.videos[0].title)
            assertEquals("Avatar", success.videos[1].title)
            assertEquals("Inception", success.videos[2].title)

            // Verify duration & size format
            assertEquals("2:05", success.videos[0].durationFormatted)
            assertEquals("100 MB", success.videos[0].fileSizeFormatted)
            assertEquals(1.0f, success.videos[0].progressFraction, 0.01f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFiltering_filtersItemsByTitleOrFolder() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2, item3)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            assertTrue(awaitItem() is MediaLibraryUiState.Success)

            // Search for "Avatar": the text field echoes first, then the filtered result settles.
            viewModel.onSearchQueryChanged("Avatar")
            var searchState = awaitItem()
            while (searchState !is MediaLibraryUiState.Success || searchState.videos.size != 1) {
                searchState = awaitItem()
            }
            val results = (searchState as MediaLibraryUiState.Success).videos
            assertEquals(1, results.size)
            assertEquals("Avatar", results[0].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFiltering_withNoMatch_transitionsToEmptyFilteredState() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            assertTrue(awaitItem() is MediaLibraryUiState.Success)

            viewModel.onSearchQueryChanged("NonExistent")
            var emptyState = awaitItem()
            while (emptyState !is MediaLibraryUiState.Empty) {
                emptyState = awaitItem()
            }
            val empty = emptyState as MediaLibraryUiState.Empty
            assertTrue(empty.isFiltered)
            assertEquals("NonExistent", empty.query)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun folderFiltering_onlyReturnsVideosInTargetFolder() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2, item3)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            assertTrue(awaitItem() is MediaLibraryUiState.Success)

            viewModel.onFolderSelected("Camera")
            val folderState = awaitItem()
            assertTrue(folderState is MediaLibraryUiState.Success)
            val results = (folderState as MediaLibraryUiState.Success).videos
            assertEquals(1, results.size)
            assertEquals("Vacation Clip", results[0].title)
            assertEquals("Camera", results[0].folderName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sortOrderChanged_sortsByTitleAscending() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2, item3) // Inception, Avatar, Vacation Clip
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            assertTrue(awaitItem() is MediaLibraryUiState.Success)

            viewModel.onSortOrderChanged(LibrarySortOrder.TITLE_ASC)
            val sortedState = awaitItem()
            assertTrue(sortedState is MediaLibraryUiState.Success)
            val sorted = (sortedState as MediaLibraryUiState.Success).videos

            assertEquals("Avatar", sorted[0].title)
            assertEquals("Inception", sorted[1].title)
            assertEquals("Vacation Clip", sorted[2].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyRepository_emitsEmptyUnfilteredState() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = emptyList()
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val state = awaitItem()
            assertTrue("Expected Empty state but got $state", state is MediaLibraryUiState.Empty)
            assertFalse((state as MediaLibraryUiState.Empty).isFiltered)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyRepository_withPermissionDenied_emitsEmptyWithPermissionDeniedTrue() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = emptyList()
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val state1 = awaitItem()
            assertTrue(state1 is MediaLibraryUiState.Empty)
            assertFalse((state1 as MediaLibraryUiState.Empty).isPermissionDenied)

            viewModel.setPermissionDenied(true)
            val state2 = awaitItem()
            assertTrue(state2 is MediaLibraryUiState.Empty)
            assertTrue((state2 as MediaLibraryUiState.Empty).isPermissionDenied)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun populatedRepository_withPermissionDenied_emitsSuccessWithLimitedModeTrue() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val state1 = awaitItem()
            assertTrue(state1 is MediaLibraryUiState.Success)
            assertFalse((state1 as MediaLibraryUiState.Success).isLimitedMode)

            viewModel.setPermissionDenied(true)
            val state2 = awaitItem()
            assertTrue(state2 is MediaLibraryUiState.Success)
            assertTrue((state2 as MediaLibraryUiState.Success).isLimitedMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removeVideoFromLibrary_removesItemAndUpdatesUiState() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val state1 = awaitItem()
            assertTrue(state1 is MediaLibraryUiState.Success)
            assertEquals(2, (state1 as MediaLibraryUiState.Success).videos.size)

            viewModel.removeVideoFromLibrary(item1.uri)
            val state2 = awaitItem()
            assertTrue(state2 is MediaLibraryUiState.Success)
            val videos = (state2 as MediaLibraryUiState.Success).videos
            assertEquals(1, videos.size)
            assertEquals("Avatar", videos[0].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchDebounce_echoesRawQueryImmediately_andFiltersAfterPause() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2, item3)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 300L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            assertEquals(3, (awaitItem() as MediaLibraryUiState.Success).videos.size)

            // Two rapid keystrokes: text field must echo at once, heavy filter pass must wait.
            viewModel.onSearchQueryChanged("Av")
            val echo1 = awaitItem() as MediaLibraryUiState.Success
            assertEquals("Av", echo1.searchQuery)
            assertEquals("Results must not be recomputed before the debounce window", 3, echo1.videos.size)

            viewModel.onSearchQueryChanged("Avatar")
            val echo2 = awaitItem() as MediaLibraryUiState.Success
            assertEquals("Avatar", echo2.searchQuery)
            assertEquals("Results must not be recomputed before the debounce window", 3, echo2.videos.size)

            val filtered = awaitItem() as MediaLibraryUiState.Success
            assertEquals("Avatar", filtered.searchQuery)
            assertEquals(1, filtered.videos.size)

            cancelAndIgnoreRemainingEvents()
        }
    }


    // ---- folders that share a name ----------------------------------------------------------------------------------------

    private val phoneCameraClip = item1.copy(uri = "content://media/10", title = "Birthday", folderPath = "/storage/emulated/0/DCIM/Camera")
    private val cardCameraClip = item2.copy(uri = "content://media/11", title = "Holiday", folderPath = "/storage/1A2B-3C4D/DCIM/Camera")

    @Test
    fun twoFoldersCalledCamera_areTwoFolders_eachWithItsOwnVideos() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(phoneCameraClip, cardCameraClip)
        repo.foldersFlow.value = listOf(phoneCameraClip.folderPath, cardCameraClip.folderPath)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val all = awaitItem() as MediaLibraryUiState.Success
            assertEquals(2, all.folders.size)
            assertEquals(mapOf(phoneCameraClip.folderPath to 1, cardCameraClip.folderPath to 1), all.folderCounts)

            viewModel.onFolderSelected(cardCameraClip.folderPath)
            val card = awaitItem() as MediaLibraryUiState.Success
            assertEquals(listOf("Holiday"), card.videos.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searching_matchesTheFolderNameYouSee_notThePathItIsStoredUnder() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(phoneCameraClip, cardCameraClip)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            awaitItem()

            // "storage" is in every path and in no folder's name: it must not match everything.
            viewModel.onSearchQueryChanged("storage")
            awaitState("no results for a word that is only in the storage path") { it is MediaLibraryUiState.Empty && it.isFiltered }

            viewModel.onSearchQueryChanged("Camera")
            awaitState("both videos, found by the name of their folder") {
                it is MediaLibraryUiState.Success && it.searchQuery == "Camera" && it.videos.size == 2
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The results of a search are computed off the test scheduler (the pipeline's debounce runs on the wall clock), so the
     * state is waited for rather than read: items are taken until one fits, and the test fails naming the last one seen.
     */
    private suspend fun app.cash.turbine.ReceiveTurbine<MediaLibraryUiState>.awaitState(
        expected: String,
        matches: (MediaLibraryUiState) -> Boolean
    ) {
        var last: MediaLibraryUiState? = null
        try {
            while (true) {
                val item = awaitItem()
                last = item
                if (matches(item)) return
            }
        } catch (error: AssertionError) {
            throw AssertionError("expected $expected; the last state was $last", error)
        }
    }

    @Test
    fun mediaFormatters_formatDurationAccurately() {
        assertEquals("0:00", MediaFormatters.formatDuration(0L))
        assertEquals("0:00", MediaFormatters.formatDuration(-500L))
        assertEquals("0:45", MediaFormatters.formatDuration(45000L))
        assertEquals("2:05", MediaFormatters.formatDuration(125000L))
        assertEquals("1:01:05", MediaFormatters.formatDuration(3665000L))
    }

    @Test
    fun mediaFormatters_formatFileSizeAccurately() {
        assertEquals("0 B", MediaFormatters.formatFileSize(0L))
        assertEquals("500 B", MediaFormatters.formatFileSize(500L))
        assertEquals("100 KB", MediaFormatters.formatFileSize(102400L))
        assertEquals("10 MB", MediaFormatters.formatFileSize(10485760L))
        assertEquals("2 GB", MediaFormatters.formatFileSize(2147483648L))
        // One decimal below ten units only, so sibling cards never mix "782 MB" with "753.4 MB".
        assertEquals("753 MB", MediaFormatters.formatFileSize(790_000_000L))
        assertEquals("3.9 GB", MediaFormatters.formatFileSize(4_200_000_000L))
        assertEquals("10 MB", MediaFormatters.formatFileSize(10_800_000L))
    }

    @Test
    fun renameVideo_delegatesToRepository() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.renameVideo(item1.uri, "Inception Remastered")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Inception Remastered", repo.videosFlow.value.first().title)
    }

    @Test
    fun removeMissingVideo_deletesTheIndexRow() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1.copy(isMissing = true), item2)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.removeMissingVideo(item1.uri)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(item2.uri), repo.videosFlow.value.map { it.uri })
    }

    // ---- natural sort, automatic folder order, Continue Watching (audit P1-14) ------------------------------------

    private fun episode(n: Int, dateAddedSec: Long = 1_000L, folder: String = "Show") = DomainMediaItem(
        uri = "content://media/ep$n",
        title = "Show.S01E$n",
        durationMs = 2_700_000L,
        sizeBytes = 100_000_000L,
        mimeType = "video/mp4",
        dateAddedSec = dateAddedSec + n, // later episodes were downloaded later, as they usually are
        folderPath = folder
    )

    @Test
    fun insideAFolder_episodesAppearInEpisodeOrder_notNewestFirst() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(episode(10), episode(2), episode(1), episode(11), episode(3))
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            assertTrue(awaitItem() is MediaLibraryUiState.Success)

            viewModel.onFolderSelected("Show")
            val folderState = awaitItem() as MediaLibraryUiState.Success

            assertEquals(
                "E1, E2, E3, E10, E11 — not E11, E10, E3...",
                listOf("Show.S01E1", "Show.S01E2", "Show.S01E3", "Show.S01E10", "Show.S01E11"),
                folderState.videos.map { it.rawItem.title }
            )
            assertEquals(LibrarySortOrder.TITLE_ASC, folderState.sortOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theHomeViewStaysNewestFirst_untilTheUserChoosesAnotherSort() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(episode(1), episode(2), episode(3))
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val home = awaitItem() as MediaLibraryUiState.Success
            assertEquals(LibrarySortOrder.DATE_ADDED_DESC, home.sortOrder)
            assertEquals(listOf("Show.S01E3", "Show.S01E2", "Show.S01E1"), home.videos.map { it.rawItem.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aSortTheUserChose_isKeptWhenEnteringAFolder() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(episode(1), episode(2).copy(durationMs = 9_000_000L), episode(3))
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            awaitItem(); awaitItem()
            viewModel.onSortOrderChanged(LibrarySortOrder.DURATION_DESC)
            awaitItem()
            viewModel.onFolderSelected("Show")
            val state = awaitItem() as MediaLibraryUiState.Success

            assertEquals(LibrarySortOrder.DURATION_DESC, state.sortOrder)
            assertEquals("Show.S01E2", state.videos.first().rawItem.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun continueWatching_listsStartedUnfinishedVideos_mostRecentlyPlayedFirst() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        val older = episode(1).copy(playbackPositionMs = 600_000L, lastPlayedTimestamp = 1_000L)
        val newer = episode(2).copy(playbackPositionMs = 900_000L, lastPlayedTimestamp = 5_000L)
        val barelyStarted = episode(3).copy(playbackPositionMs = 4_000L, lastPlayedTimestamp = 9_000L)
        val nearlyDone = episode(4).copy(playbackPositionMs = 2_650_000L, lastPlayedTimestamp = 9_500L) // 98 %
        val finished = episode(5).copy(playbackPositionMs = 2_700_000L, isCompleted = true, lastPlayedTimestamp = 9_900L)
        val gone = episode(6).copy(playbackPositionMs = 600_000L, isMissing = true, lastPlayedTimestamp = 9_950L)
        val neverPlayed = episode(7)
        repo.videosFlow.value = listOf(older, newer, barelyStarted, nearlyDone, finished, gone, neverPlayed)
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem() as MediaLibraryUiState.Success

            assertEquals(
                "only started + unfinished + present, newest play first",
                listOf("Show.S01E2", "Show.S01E1"),
                state.continueWatching.map { it.rawItem.title }
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun continueWatching_isHiddenWhileSearchingOrInsideAFolder() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(episode(1).copy(playbackPositionMs = 600_000L, lastPlayedTimestamp = 1_000L), episode(2))
        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            awaitItem()
            val home = awaitItem() as MediaLibraryUiState.Success
            assertEquals(1, home.continueWatching.size)

            viewModel.onFolderSelected("Show")
            val inFolder = awaitItem() as MediaLibraryUiState.Success
            assertTrue("a folder view is a list, not a home screen", inFolder.continueWatching.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun continueWatching_isCappedAtTen() {
        val many = (1..15).map { episode(it).copy(playbackPositionMs = 600_000L, lastPlayedTimestamp = it * 1_000L) }

        val rail = many.continueWatching()

        assertEquals(10, rail.size)
        assertEquals("the most recently played is first", "Show.S01E15", rail.first().title)
    }

    @Test
    fun folderSummaries_describeEachFolder_andPickTheNewestReadableVideoAsItsCover() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        // Movies holds item1 (added 1000), item2 (2000) and a newer one that is missing, which cannot give a picture.
        val missingNewest = item2.copy(uri = "content://media/9", title = "Gone", dateAddedSec = 9000L, isMissing = true)
        repo.videosFlow.value = listOf(item1, item2, missingNewest, item3)
        repo.foldersFlow.value = listOf("Camera", "Movies")

        val viewModel = MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val success = awaitItem() as MediaLibraryUiState.Success

            val movies = success.folderSummaries.getValue("Movies")
            assertEquals(3, movies.videoCount)
            assertEquals(item1.sizeBytes + item2.sizeBytes + missingNewest.sizeBytes, movies.totalBytes)
            assertEquals("the missing video cannot be the cover", item2.uri, movies.coverUri)

            val camera = success.folderSummaries.getValue("Camera")
            assertEquals(1, camera.videoCount)
            assertEquals(item3.uri, camera.coverUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theSortTheUserPickedLastTime_isUsedFromTheFirstEmission() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        repo.videosFlow.value = listOf(item1, item2, item3)
        repo.foldersFlow.value = listOf("Camera", "Movies")

        val viewModel = MediaLibraryViewModel(
            repo, testDispatcher, testDispatcher, searchDebounceMs = 0L, initialSort = LibrarySortOrder.TITLE_ASC
        )

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val success = awaitItem() as MediaLibraryUiState.Success

            assertEquals(LibrarySortOrder.TITLE_ASC, success.sortOrder)
            assertEquals(listOf("Avatar", "Inception", "Vacation Clip"), success.videos.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- the first scan after an install -----------------------------------------------------------------------------
    //
    // These run on the wall clock, as the view model's pipeline does under test (its debounce is not on the test
    // scheduler), with a short hold so they stay quick. `runCurrent()` lets the scan flag, which is collected on the
    // test dispatcher, reach the view model.

    private val hold = 150L

    private fun viewModel(repo: FakeMediaRepository, scan: Flow<Boolean>, settleMs: Long = hold) =
        MediaLibraryViewModel(repo, testDispatcher, testDispatcher, searchDebounceMs = 0L, scanActivity = scan, emptySettleMs = settleMs)

    /**
     * A fresh install has an empty database and a scan that is queued or running. The library used to say "No videos
     * yet" (with a Scan device button) the moment the database answered, while the scan was about to fill it.
     */
    @Test
    fun firstInstall_whileTheFirstScanRuns_staysLoading_andNeverSaysNoVideosYet() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        val viewModel = viewModel(repo, MutableStateFlow(true))

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            runCurrent()
            Thread.sleep(hold * 4) // the hold on "No videos yet" is long over
            expectNoEvents()

            repo.videosFlow.value = listOf(item1)
            assertTrue(awaitItem() is MediaLibraryUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** The database is empty for a moment before WorkManager reports the scan: that moment must not show anything. */
    @Test
    fun anEmptyAnswerBeforeTheScanIsReported_doesNotFlashNoVideosYet() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        val scan = MutableStateFlow(false)
        val viewModel = viewModel(repo, scan)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            runCurrent()
            Thread.sleep(hold / 3) // the empty answer is in and is being held back
            scan.value = true // the scan is reported before the hold is over
            runCurrent()
            Thread.sleep(hold * 4)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenTheScanFindsNothing_noVideosYetAppears_afterTheHold() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        val scan = MutableStateFlow(true)
        val viewModel = viewModel(repo, scan)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            runCurrent()
            scan.value = false // the scan ended with nothing
            runCurrent()
            val startedAt = System.nanoTime()
            val state = awaitItem()
            val waitedMs = (System.nanoTime() - startedAt) / 1_000_000
            assertTrue("Expected Empty but got $state", state is MediaLibraryUiState.Empty)
            assertFalse((state as MediaLibraryUiState.Empty).isFiltered)
            assertTrue("the empty screen came after only $waitedMs ms", waitedMs >= hold * 2 / 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** With no access there is no scan to wait for, so the permission screen is not held back. */
    @Test
    fun withoutAccess_thePermissionScreenAppearsAtOnce() = runTest(testDispatcher) {
        val repo = FakeMediaRepository()
        val viewModel = viewModel(repo, MutableStateFlow(false), settleMs = 2_000L)
        viewModel.setPermissionDenied(true)

        viewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            val startedAt = System.nanoTime()
            val state = awaitItem()
            val waitedMs = (System.nanoTime() - startedAt) / 1_000_000
            assertTrue(state is MediaLibraryUiState.Empty && state.isPermissionDenied)
            assertTrue("the permission screen was held back for $waitedMs ms", waitedMs < 1_000)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
