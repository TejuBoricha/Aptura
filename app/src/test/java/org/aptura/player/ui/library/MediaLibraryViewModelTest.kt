package org.aptura.player.ui.library

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

        override suspend fun renameVideo(uri: String, newName: String) {
            videosFlow.value = videosFlow.value.map {
                if (it.uri == uri) it.copy(title = newName) else it
            }
        }

        override fun getBookmarks(mediaUri: String): Flow<List<org.aptura.player.domain.model.Bookmark>> =
            MutableStateFlow(emptyList())

        override suspend fun addBookmark(mediaUri: String, timestampMs: Long, note: String): Long = 0L

        override suspend fun deleteBookmark(id: Long) {}

        override suspend fun getLibraryStatistics(): org.aptura.player.domain.repository.LibraryStatistics {
            val list = videosFlow.value
            return org.aptura.player.domain.repository.LibraryStatistics(
                totalVideos = list.size,
                totalDurationMs = list.sumOf { it.durationMs },
                totalSizeBytes = list.sumOf { it.sizeBytes }
            )
        }
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
}
