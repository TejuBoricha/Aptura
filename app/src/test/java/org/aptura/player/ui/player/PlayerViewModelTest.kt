package org.aptura.player.ui.player

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.aptura.player.domain.model.Bookmark
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.DomainTrackSelection
import org.aptura.player.domain.model.EqualizerPreset
import org.aptura.player.domain.model.PlaybackStatsInfo
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.repository.MediaRepository
import org.aptura.player.domain.repository.PlaybackController
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.MediaLibraryViewModel
import org.aptura.player.ui.library.buildPlayQueueRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test suite for PlayerViewModel.
 * Verifies UDF state emissions, 5s auto-hide HUD, multi-tap seek accumulation,
 * scrub timeline precision, aspect ratio cycling, speed ladders, and progress restoration.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakePlaybackController : PlaybackController {
        val _playbackState = MutableStateFlow<DomainPlaybackState>(DomainPlaybackState.Idle)
        override val playbackState: StateFlow<DomainPlaybackState> = _playbackState.asStateFlow()

        val _playbackQueue = MutableStateFlow(DomainPlaybackQueue())
        override val playbackQueue: StateFlow<DomainPlaybackQueue> = _playbackQueue.asStateFlow()

        val _trackSelection = MutableStateFlow(DomainTrackSelection())
        override val trackSelection: StateFlow<DomainTrackSelection> = _trackSelection.asStateFlow()

        val _playbackStats = MutableStateFlow(PlaybackStatsInfo())
        override val playbackStats: StateFlow<PlaybackStatsInfo> = _playbackStats.asStateFlow()

        val _failedCodecName = MutableStateFlow<String?>(null)
        override val failedCodecName: StateFlow<String?> = _failedCodecName.asStateFlow()

        val _failureReason = MutableStateFlow<String?>(null)
        override val failureReason: StateFlow<String?> = _failureReason.asStateFlow()

        /** Mirrors Media3PlaybackController's 250 ms position ticker: a *new* Ready state per tick. */
        fun emitPlayingTick(positionMs: Long) {
            val current = _playbackState.value as? DomainPlaybackState.Ready
            _playbackState.value = DomainPlaybackState.Ready(
                positionMs = positionMs,
                durationMs = current?.durationMs ?: 0L,
                isPlaying = true,
                playbackSpeed = current?.playbackSpeed ?: 1.0f,
                bufferedPositionMs = positionMs + 30_000L
            )
        }

        var lastPlayedItem: DomainMediaItem? = null
        var isPlaying = false
        var lastSeekPosition: Long? = null
        var lastSpeed: Float? = null
        var lastAudioTrackIndex: Int? = null
        var lastSubtitleTrackIndex: Int? = null

        override suspend fun playMediaItem(item: DomainMediaItem) {
            lastPlayedItem = item
            isPlaying = true
            _playbackState.value = DomainPlaybackState.Ready(
                positionMs = 0L,
                durationMs = item.durationMs,
                isPlaying = true,
                playbackSpeed = 1.0f
            )
        }

        override suspend fun playQueue(items: List<DomainMediaItem>, startIndex: Int) {
            _playbackQueue.value = DomainPlaybackQueue(items, startIndex)
            if (items.isNotEmpty()) {
                playMediaItem(items[startIndex])
            }
        }

        override suspend fun play() {
            isPlaying = true
            val current = _playbackState.value
            if (current is DomainPlaybackState.Ready) {
                _playbackState.value = current.copy(isPlaying = true)
            }
        }

        override suspend fun pause() {
            isPlaying = false
            val current = _playbackState.value
            if (current is DomainPlaybackState.Ready) {
                _playbackState.value = current.copy(isPlaying = false)
            }
        }

        override suspend fun seekTo(positionMs: Long) {
            lastSeekPosition = positionMs
            val current = _playbackState.value
            if (current is DomainPlaybackState.Ready) {
                _playbackState.value = current.copy(positionMs = positionMs)
            }
        }

        var lastSubtitleDelay: Long? = null
        var lastAudioDelay: Long? = null
        val steppedFrames = mutableListOf<Boolean>()

        override suspend fun stepFrame(forward: Boolean) {
            steppedFrames += forward
        }

        override suspend fun skipToNext() {
            val queue = _playbackQueue.value
            if (queue.hasNext) {
                val nextIndex = queue.currentIndex + 1
                _playbackQueue.value = queue.copy(currentIndex = nextIndex)
                if (nextIndex in queue.items.indices) {
                    playMediaItem(queue.items[nextIndex])
                }
            }
        }

        override suspend fun skipToPrevious() {
            val queue = _playbackQueue.value
            if (queue.hasPrevious) {
                val prevIndex = queue.currentIndex - 1
                _playbackQueue.value = queue.copy(currentIndex = prevIndex)
                if (prevIndex in queue.items.indices) {
                    playMediaItem(queue.items[prevIndex])
                }
            }
        }

        override suspend fun setPlaybackSpeed(speed: Float) {
            lastSpeed = speed
            _trackSelection.value = _trackSelection.value.copy(playbackSpeed = speed)
        }

        override suspend fun setPlayMode(mode: PlayMode) {
            _playbackQueue.value = _playbackQueue.value.copy(playMode = mode)
        }

        override suspend fun selectAudioTrack(index: Int?) {
            lastAudioTrackIndex = index
            _trackSelection.value = _trackSelection.value.copy(selectedAudioTrackIndex = index)
        }

        override suspend fun selectSubtitleTrack(index: Int?) {
            lastSubtitleTrackIndex = index
            _trackSelection.value = _trackSelection.value.copy(selectedSubtitleTrackIndex = index)
        }

        override suspend fun setSubtitleDelay(delayMs: Long) {
            lastSubtitleDelay = delayMs
            _trackSelection.value = _trackSelection.value.copy(subtitleDelayMs = delayMs)
        }

        override suspend fun setAudioDelay(delayMs: Long) {
            lastAudioDelay = delayMs
            _trackSelection.value = _trackSelection.value.copy(audioDelayMs = delayMs)
        }

        var lastExternalSubtitleUri: String? = null
        var lastExternalSubtitleMimeType: String? = null
        var lastExternalSubtitleLabel: String? = null
        var lastDecoderStrategy: Int? = null

        val _isCompatibilityMode = MutableStateFlow(false)
        override val isCompatibilityMode: StateFlow<Boolean> = _isCompatibilityMode.asStateFlow()

        val _compatibilityNotice = MutableStateFlow<String?>(null)
        override val compatibilityNotice: StateFlow<String?> = _compatibilityNotice.asStateFlow()

        var hasRemovedExternalSubtitles = false

        override suspend fun addExternalSubtitle(uri: String, mimeType: String, label: String) {
            lastExternalSubtitleUri = uri
            lastExternalSubtitleMimeType = mimeType
            lastExternalSubtitleLabel = label
        }

        var lastExternalSubtitles: List<org.aptura.player.domain.model.DomainExternalSubtitle>? = null
        override suspend fun addExternalSubtitles(subtitles: List<org.aptura.player.domain.model.DomainExternalSubtitle>) {
            lastExternalSubtitles = subtitles
        }

        override suspend fun removeExternalSubtitles() {
            hasRemovedExternalSubtitles = true
            lastExternalSubtitleUri = null
            lastExternalSubtitleMimeType = null
            lastExternalSubtitleLabel = null
        }

        val _decoderStrategy = MutableStateFlow(0)
        override val decoderStrategy: StateFlow<Int> = _decoderStrategy.asStateFlow()

        override suspend fun setDecoderStrategy(strategy: Int) {
            lastDecoderStrategy = strategy
            _decoderStrategy.value = strategy
        }

        override suspend fun playQueueItem(index: Int) {
            val queue = _playbackQueue.value
            if (index in queue.items.indices) {
                _playbackQueue.value = queue.copy(currentIndex = index)
                playMediaItem(queue.items[index])
            }
        }

        override suspend fun addToQueue(item: DomainMediaItem) {
            val queue = _playbackQueue.value
            _playbackQueue.value = queue.copy(items = queue.items + item)
        }

        override suspend fun playNext(item: DomainMediaItem) {
            val queue = _playbackQueue.value
            val insertIndex = (queue.currentIndex + 1).coerceAtMost(queue.items.size)
            val newItems = queue.items.toMutableList().apply { add(insertIndex, item) }
            _playbackQueue.value = queue.copy(items = newItems)
        }

        override suspend fun removeFromQueue(index: Int) {
            val queue = _playbackQueue.value
            if (index in queue.items.indices) {
                val newItems = queue.items.toMutableList().apply { removeAt(index) }
                val newIndex = when {
                    index < queue.currentIndex -> queue.currentIndex - 1
                    index == queue.currentIndex -> queue.currentIndex.coerceAtMost(newItems.size - 1)
                    else -> queue.currentIndex
                }
                _playbackQueue.value = queue.copy(items = newItems, currentIndex = newIndex)
            }
        }

        override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) {
            val queue = _playbackQueue.value
            if (fromIndex in queue.items.indices && toIndex in queue.items.indices) {
                val newItems = queue.items.toMutableList()
                val item = newItems.removeAt(fromIndex)
                newItems.add(toIndex, item)
                _playbackQueue.value = queue.copy(items = newItems)
            }
        }

        override suspend fun clearQueue() {
            val queue = _playbackQueue.value
            val current = queue.currentItem
            _playbackQueue.value = if (current != null) queue.copy(items = listOf(current), currentIndex = 0) else DomainPlaybackQueue()
        }

        override suspend fun release() {}
    }

    private class FakeMediaRepository : MediaRepository {
        val items = mutableMapOf<String, DomainMediaItem>()
        val savedPositions = mutableMapOf<String, Pair<Long, Boolean>>()

        override fun getAllVideos(): Flow<List<DomainMediaItem>> = MutableStateFlow(items.values.toList())
        override fun getVideosInFolder(folderPath: String): Flow<List<DomainMediaItem>> = MutableStateFlow(emptyList())
        override fun getAllFolders(): Flow<List<String>> = MutableStateFlow(emptyList())
        override fun searchVideos(query: String): Flow<List<DomainMediaItem>> = MutableStateFlow(emptyList())

        override suspend fun getVideoByUri(uri: String): DomainMediaItem? = items[uri]

        override suspend fun savePlaybackPosition(uri: String, positionMs: Long, isCompleted: Boolean) {
            savedPositions[uri] = positionMs to isCompleted
        }

        override suspend fun removeVideoFromLibrary(uri: String) {
            items.remove(uri)
        }

        override suspend fun renameVideo(uri: String, newName: String) {
            items[uri]?.let { items[uri] = it.copy(title = newName) }
        }

        override suspend fun hideVideo(uri: String) {}
        override suspend fun restoreHiddenVideos() {}
        override fun getHiddenVideoCount(): Flow<Int> = MutableStateFlow(0)

        val bookmarksState = MutableStateFlow<List<Bookmark>>(emptyList())
        private var nextBookmarkId = 1L

        override fun getBookmarks(mediaUri: String): Flow<List<Bookmark>> {
            return bookmarksState.map { list -> list.filter { it.mediaUri == mediaUri } }
        }

        override suspend fun addBookmark(mediaUri: String, timestampMs: Long, note: String): Long {
            val id = nextBookmarkId++
            val item = Bookmark(id, mediaUri, timestampMs, note, System.currentTimeMillis() / 1000)
            bookmarksState.value = bookmarksState.value + item
            return id
        }

        override suspend fun deleteBookmark(id: Long) {
            bookmarksState.value = bookmarksState.value.filter { it.id != id }
        }

        override fun getMissingVideos(): Flow<List<DomainMediaItem>> =
            MutableStateFlow(items.values.filter { it.isMissing })

        override suspend fun clearMissingVideos() {
            val missingUris = items.values.filter { it.isMissing }.map { it.uri }
            missingUris.forEach { items.remove(it) }
        }

        override suspend fun getLibraryStatistics(): org.aptura.player.domain.repository.LibraryStatistics {
            return org.aptura.player.domain.repository.LibraryStatistics(
                totalVideos = items.size,
                totalDurationMs = items.values.sumOf { it.durationMs },
                totalSizeBytes = items.values.sumOf { it.sizeBytes }
            )
        }

        var lastSavedPreferencesUri: String? = null
        var lastSavedResizeMode: String? = null
        var lastSavedSpeed: Float? = null
        var lastSavedDecoderStrategy: Int? = null

        override suspend fun savePlaybackPreferences(
            uri: String,
            resizeMode: String,
            playbackSpeed: Float,
            decoderStrategy: Int,
            audioDelayMs: Long,
            subtitleDelayMs: Long
        ) {
            lastSavedPreferencesUri = uri
            lastSavedResizeMode = resizeMode
            lastSavedSpeed = playbackSpeed
            lastSavedDecoderStrategy = decoderStrategy
            items[uri]?.let {
                items[uri] = it.copy(
                    resizeMode = resizeMode,
                    playbackSpeed = playbackSpeed,
                    decoderStrategy = decoderStrategy,
                    audioDelayMs = audioDelayMs,
                    subtitleDelayMs = subtitleDelayMs
                )
            }
        }

        var lastResetPreferencesUri: String? = null
        override suspend fun resetPlaybackPreferences(uri: String) {
            lastResetPreferencesUri = uri
            items[uri]?.let {
                items[uri] = it.copy(
                    resizeMode = "FIT",
                    playbackSpeed = 1.0f,
                    decoderStrategy = 0,
                    audioDelayMs = 0L,
                    subtitleDelayMs = 0L
                )
            }
        }
    }

    private val sampleVideo = DomainMediaItem(
        uri = "content://media/1",
        title = "Interstellar",
        durationMs = 10140000L, // 2h 49m
        sizeBytes = 4294967296L,
        mimeType = "video/mp4",
        playbackPositionMs = 50000L,
        isCompleted = false
    )

    @Test
    fun loadAndPlay_startsPlayback_andSetsInitialState() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(sampleVideo.uri, state.videoUri)
        assertEquals("Interstellar", state.videoTitle)
        assertEquals(sampleVideo.durationMs, state.durationMs)
        assertTrue(state.isPlaying)
        assertTrue(state.isControlsVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun loadAndPlay_whenSavedPositionGreaterThan3s_restoresWithComfortPreRoll() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val videoWithProgress = sampleVideo.copy(playbackPositionMs = 45000L) // 45s
        repository.items[videoWithProgress.uri] = videoWithProgress

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(videoWithProgress.uri)
        runCurrent()

        // Expect pre-roll comfort window of 2s: 45000L - 2000L = 43000L (spec/12A §3.3)
        assertEquals(43000L, controller.lastSeekPosition)
        viewModel.clearForTesting()
    }

    @Test
    fun togglePlayPause_whenPlaying_pausesAndPersistsProgress() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        // Set position to 12000ms
        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 12000L,
            durationMs = sampleVideo.durationMs,
            isPlaying = true
        )
        runCurrent()

        viewModel.togglePlayPause()
        runCurrent()

        assertFalse(controller.isPlaying)
        assertEquals(12000L, repository.savedPositions[sampleVideo.uri]?.first)
        assertEquals(false, repository.savedPositions[sampleVideo.uri]?.second)
        viewModel.clearForTesting()
    }

    @Test
    fun togglePlayPause_whenEnded_restartsFromZero() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ended(durationMs = sampleVideo.durationMs)
        runCurrent()

        assertTrue(viewModel.uiState.value.isEnded)

        viewModel.togglePlayPause()
        runCurrent()

        assertEquals(0L, controller.lastSeekPosition)
        assertTrue(controller.isPlaying)
        assertFalse(viewModel.uiState.value.isEnded)
        viewModel.clearForTesting()
    }

    @Test
    fun seekRelative_singleStep_seeksAndDisplaysFeedback() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 30000L,
            durationMs = 60000L,
            isPlaying = true
        )
        runCurrent()

        viewModel.seekRelative(10000L) // +10s
        runCurrent()

        assertEquals(40000L, controller.lastSeekPosition)
        assertEquals(10, viewModel.uiState.value.seekDeltaSeconds)

        // Advance past feedback delay (1200ms)
        advanceTimeBy(1300L)
        runCurrent()
        assertNull(viewModel.uiState.value.seekDeltaSeconds)
        viewModel.clearForTesting()
    }

    @Test
    fun seekRelative_multiTapAccumulation_accumulatesWithinThreshold() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 20000L,
            durationMs = 100000L,
            isPlaying = true
        )
        runCurrent()

        viewModel.seekRelative(10000L) // First tap: +10s
        runCurrent()
        assertEquals(10, viewModel.uiState.value.seekDeltaSeconds)

        advanceTimeBy(200L) // Under 800ms threshold
        viewModel.seekRelative(10000L) // Second tap: accumulates to +20s
        runCurrent()
        assertEquals(20, viewModel.uiState.value.seekDeltaSeconds)

        advanceTimeBy(200L)
        viewModel.seekRelative(10000L) // Third tap: accumulates to +30s
        runCurrent()
        assertEquals(30, viewModel.uiState.value.seekDeltaSeconds)

        advanceTimeBy(1300L)
        runCurrent()
        assertNull(viewModel.uiState.value.seekDeltaSeconds)
        viewModel.clearForTesting()
    }

    @Test
    fun onScrub_updatesScrubPosition_andSeeksOnEnd() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        viewModel.onScrubStart(15000L)
        assertTrue(viewModel.uiState.value.isScrubbing)
        assertEquals(15000L, viewModel.uiState.value.scrubPositionMs)
        assertEquals(15000L, viewModel.uiState.value.effectivePositionMs)

        viewModel.onScrubMove(25000L)
        assertEquals(25000L, viewModel.uiState.value.scrubPositionMs)

        viewModel.onScrubEnd()
        runCurrent()

        assertFalse(viewModel.uiState.value.isScrubbing)
        assertEquals(25000L, controller.lastSeekPosition)
        viewModel.clearForTesting()
    }

    @Test
    fun controlsAutoHide_whenPlaying_hidesAfter5Seconds() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        assertTrue(viewModel.uiState.value.isControlsVisible)

        // Advance 5000ms
        advanceTimeBy(5100L)
        runCurrent()

        assertFalse(viewModel.uiState.value.isControlsVisible)
        viewModel.clearForTesting()
    }

    /**
     * Regression (audit P0-01): the real Media3PlaybackController emits a *new* Ready state every 250 ms
     * while playing. Every one of those emissions used to restart the 5 s auto-hide timer, so the HUD
     * never hid. The static-state test above cannot see this; this one replays the real stream.
     */
    @Test
    fun controlsAutoHide_whilePositionTicksEvery250ms_hidesAfter5Seconds() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()
        assertTrue(viewModel.uiState.value.isControlsVisible)

        var positionMs = 0L
        repeat(16) { // t = 4.0 s: still inside the 5 s window
            advanceTimeBy(250L)
            positionMs += 250L
            controller.emitPlayingTick(positionMs)
            runCurrent()
        }
        assertTrue("HUD must still be visible at 4 s", viewModel.uiState.value.isControlsVisible)

        repeat(14) { // t = 7.5 s
            advanceTimeBy(250L)
            positionMs += 250L
            controller.emitPlayingTick(positionMs)
            runCurrent()
        }
        assertFalse("HUD must auto-hide ~5 s after the last interaction", viewModel.uiState.value.isControlsVisible)
        viewModel.clearForTesting()
    }

    @Test
    fun controlsAutoHide_afterTap_reshowsAndRehidesWhileTicking() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        var positionMs = 0L
        repeat(24) { // 6 s of ticking -> hidden
            advanceTimeBy(250L)
            positionMs += 250L
            controller.emitPlayingTick(positionMs)
            runCurrent()
        }
        assertFalse(viewModel.uiState.value.isControlsVisible)

        viewModel.toggleControlsVisibility() // user taps the video
        assertTrue(viewModel.uiState.value.isControlsVisible)

        repeat(16) { // 4 s later: still visible
            advanceTimeBy(250L)
            positionMs += 250L
            controller.emitPlayingTick(positionMs)
            runCurrent()
        }
        assertTrue(viewModel.uiState.value.isControlsVisible)

        repeat(6) { // 5.5 s after the tap: hidden again
            advanceTimeBy(250L)
            positionMs += 250L
            controller.emitPlayingTick(positionMs)
            runCurrent()
        }
        assertFalse(viewModel.uiState.value.isControlsVisible)
        viewModel.clearForTesting()
    }

    @Test
    fun controlsAutoHide_afterResumeFromPause_hidesWhileTicking() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()
        viewModel.togglePlayPause() // pause: HUD pinned open
        runCurrent()
        viewModel.showControls()
        advanceTimeBy(6000L)
        runCurrent()
        assertTrue(viewModel.uiState.value.isControlsVisible)

        viewModel.togglePlayPause() // resume
        runCurrent()
        var positionMs = 1000L
        repeat(24) {
            advanceTimeBy(250L)
            positionMs += 250L
            controller.emitPlayingTick(positionMs)
            runCurrent()
        }
        assertFalse(viewModel.uiState.value.isControlsVisible)
        viewModel.clearForTesting()
    }

    @Test
    fun controlsAutoHide_whileScrubbing_neverHides() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()
        viewModel.onScrubStart(30_000L)

        var positionMs = 0L
        repeat(40) { // 10 s of ticks while the thumb is held
            advanceTimeBy(250L)
            positionMs += 250L
            controller.emitPlayingTick(positionMs)
            runCurrent()
        }
        assertTrue(viewModel.uiState.value.isControlsVisible)
        viewModel.clearForTesting()
    }

    /**
     * Regression (audit P0-04): the grid used to rebuild items from title/uri/duration, dropping the saved
     * position and per-video preferences, so nothing was ever restored. This goes through the real library
     * view-model -> the exact item list the grid hands to onVideoClick -> the real play-queue builder -> player.
     */
    @Test
    fun playFromLibraryGrid_resumesSavedPositionAndPerVideoPreferences() = runTest(testDispatcher) {
        val saved = sampleVideo.copy(
            playbackPositionMs = 125_000L,
            playbackSpeed = 1.5f,
            resizeMode = "FILL",
            audioDelayMs = 150L,
            subtitleDelayMs = -200L
        )
        val repository = FakeMediaRepository()
        repository.items[saved.uri] = saved

        val libraryViewModel = MediaLibraryViewModel(repository, testDispatcher, testDispatcher, searchDebounceMs = 0L)
        var visible: List<DomainMediaItem> = emptyList()
        libraryViewModel.uiState.test {
            assertEquals(MediaLibraryUiState.Loading, awaitItem())
            visible = (awaitItem() as MediaLibraryUiState.Success).videos.map { it.rawItem }
            cancelAndIgnoreRemainingEvents()
        }

        val request = buildPlayQueueRequest(clicked = visible.first(), visibleItems = visible)
        val controller = FakePlaybackController()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.playQueue(request.items, request.startIndex)
        runCurrent()

        assertEquals("resume with 2 s pre-roll", 123_000L, controller.lastSeekPosition)
        assertEquals(1.5f, controller.lastSpeed)
        assertEquals(AspectRatioMode.FILL, viewModel.uiState.value.resizeMode)
        assertEquals(150L, controller.lastAudioDelay)
        assertEquals(-200L, controller.lastSubtitleDelay)
        viewModel.clearForTesting()
    }

    @Test
    fun controlsAutoHide_whenPaused_doesNotHide() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        viewModel.togglePlayPause() // Pauses
        runCurrent()
        viewModel.showControls()

        advanceTimeBy(6000L)
        runCurrent()

        assertTrue(viewModel.uiState.value.isControlsVisible)
        viewModel.clearForTesting()
    }

    @Test
    fun lockMode_hidesControls_andPreventsInteractions() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        viewModel.toggleLock() // Lock
        assertTrue(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isControlsVisible)

        // Toggling visibility while locked should be ignored
        viewModel.toggleControlsVisibility()
        assertFalse(viewModel.uiState.value.isControlsVisible)

        // Unlock
        viewModel.toggleLock()
        assertFalse(viewModel.uiState.value.isLocked)
        assertTrue(viewModel.uiState.value.isControlsVisible)
        viewModel.clearForTesting()
    }

    @Test
    fun playbackSpeed_updatesSpeedAndClosesSheet() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        repository.items[sampleVideo.uri] = sampleVideo

        val viewModel = PlayerViewModel(controller, repository, testDispatcher)
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        viewModel.setSpeedSheetVisible(true)
        assertTrue(viewModel.uiState.value.isSpeedSheetVisible)

        viewModel.setPlaybackSpeed(1.5f)
        runCurrent()

        assertEquals(1.5f, controller.lastSpeed)
        assertEquals(1.5f, viewModel.uiState.value.playbackSpeed)
        assertFalse(viewModel.uiState.value.isSpeedSheetVisible)
        viewModel.clearForTesting()
    }

    @Test
    fun aspectRatioMode_cyclesCorrectly() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertEquals(AspectRatioMode.FIT, viewModel.uiState.value.resizeMode)

        viewModel.toggleResizeMode()
        assertEquals(AspectRatioMode.FILL, viewModel.uiState.value.resizeMode)

        viewModel.toggleResizeMode()
        assertEquals(AspectRatioMode.ZOOM, viewModel.uiState.value.resizeMode)

        viewModel.toggleResizeMode()
        assertEquals(AspectRatioMode.FIXED_16_9, viewModel.uiState.value.resizeMode)

        viewModel.toggleResizeMode()
        assertEquals(AspectRatioMode.FIXED_4_3, viewModel.uiState.value.resizeMode)

        viewModel.toggleResizeMode()
        assertEquals(AspectRatioMode.FIT, viewModel.uiState.value.resizeMode)
        viewModel.clearForTesting()
    }

    @Test
    fun volumeAndBrightnessHUD_clampsAndAutoClears() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.setVolumePercent(120) // Clamps to 100
        assertEquals(100, viewModel.uiState.value.volumePercent)

        advanceTimeBy(1300L)
        runCurrent()
        assertNull(viewModel.uiState.value.volumePercent)

        viewModel.setBrightnessPercent(-20) // Clamps to 0
        assertEquals(0, viewModel.uiState.value.brightnessPercent)

        advanceTimeBy(1300L)
        runCurrent()
        assertNull(viewModel.uiState.value.brightnessPercent)
        viewModel.clearForTesting()
    }

    @Test
    fun errorState_retainsErrorAndLocksControlsVisible() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        controller._playbackState.value = DomainPlaybackState.Error(
            errorCode = "ERROR_CODE_DECODER_INIT_FAILED",
            message = "MediaCodec hardware decoder init failed",
            isRecoverable = false
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("MediaCodec hardware decoder init failed", state.errorMessage)
        assertEquals("ERROR_CODE_DECODER_INIT_FAILED", state.errorCode)
        assertFalse(state.isRecoverableError)
        assertTrue(state.isControlsVisible) // Controls locked visible on error
        viewModel.clearForTesting()
    }

    @Test
    fun playQueue_initializesQueue_andAdvancesWithSkip() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val item1 = sampleVideo
        val item2 = sampleVideo.copy(uri = "content://media/2", title = "Tenet")
        val queue = listOf(item1, item2)

        viewModel.playQueue(queue, startIndex = 0)
        runCurrent()

        assertEquals("Interstellar", viewModel.uiState.value.videoTitle)
        assertTrue(viewModel.uiState.value.hasNext)
        assertFalse(viewModel.uiState.value.hasPrevious)

        viewModel.skipToNext()
        runCurrent()

        assertEquals("Tenet", viewModel.uiState.value.videoTitle)
        assertFalse(viewModel.uiState.value.hasNext)
        assertTrue(viewModel.uiState.value.hasPrevious)

        viewModel.skipToPrevious()
        runCurrent()

        assertEquals("Interstellar", viewModel.uiState.value.videoTitle)
        viewModel.clearForTesting()
    }

    @Test
    fun advancedSettings_updatesDelays_andSleepTimer() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.setSubtitleDelay(150L)
        runCurrent()
        assertEquals(150L, controller.lastSubtitleDelay)
        assertEquals(150L, viewModel.uiState.value.subtitleDelayMs)

        viewModel.setAudioDelay(-50L)
        runCurrent()
        assertEquals(-50L, controller.lastAudioDelay)
        assertEquals(-50L, viewModel.uiState.value.audioDelayMs)

        viewModel.startSleepTimer(30)
        assertEquals(30 * 60 * 1000L, viewModel.uiState.value.sleepTimerRemainingMs)

        viewModel.cancelSleepTimer()
        assertNull(viewModel.uiState.value.sleepTimerRemainingMs)

        viewModel.clearForTesting()
    }

    @Test
    fun togglePlayMode_cyclesAndUpdatesController() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertEquals(PlayMode.NORMAL, viewModel.uiState.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.REPEAT_ALL, viewModel.uiState.value.playMode)
        assertEquals(PlayMode.REPEAT_ALL, controller.playbackQueue.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.REPEAT_ONE, viewModel.uiState.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.SHUFFLE, viewModel.uiState.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.NORMAL, viewModel.uiState.value.playMode)

        viewModel.clearForTesting()
    }

    @Test
    fun delayStep_updatesStepSize() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertEquals(50L, viewModel.uiState.value.delayStepMs)
        viewModel.setDelayStep(100L)
        assertEquals(100L, viewModel.uiState.value.delayStepMs)

        viewModel.setDelayStep(10L)
        assertEquals(10L, viewModel.uiState.value.delayStepMs)

        viewModel.clearForTesting()
    }

    @Test
    fun equalizerAndSoundEnhancements_updateState() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertFalse(viewModel.uiState.value.audioEnhancement.enabled)
        viewModel.setEqualizerEnabled(true)
        runCurrent()
        assertTrue(viewModel.uiState.value.audioEnhancement.enabled)

        viewModel.setEqualizerPreset(EqualizerPreset.BASS_BOOST)
        runCurrent()
        assertEquals(EqualizerPreset.BASS_BOOST, viewModel.uiState.value.audioEnhancement.preset)

        viewModel.setBassBoost(80)
        runCurrent()
        assertEquals(80, viewModel.uiState.value.audioEnhancement.bassBoostPercent)

        viewModel.setVirtualizer(65)
        runCurrent()
        assertEquals(65, viewModel.uiState.value.audioEnhancement.virtualizerPercent)

        viewModel.setLoudnessBoost(40)
        runCurrent()
        assertEquals(40, viewModel.uiState.value.audioEnhancement.loudnessBoostPercent)

        viewModel.clearForTesting()
    }

    @Test
    fun pictureAdjustments_andNightMode_updateAndResetState() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertEquals(1.0f, viewModel.uiState.value.pictureAdjustment.contrast)
        assertEquals(1.0f, viewModel.uiState.value.pictureAdjustment.saturation)
        assertFalse(viewModel.uiState.value.pictureAdjustment.isNightModeEnabled)

        viewModel.setContrast(1.25f)
        assertEquals(1.25f, viewModel.uiState.value.pictureAdjustment.contrast)

        viewModel.setSaturation(1.5f)
        assertEquals(1.5f, viewModel.uiState.value.pictureAdjustment.saturation)

        viewModel.setNightMode(enabled = true, warmth = 0.5f)
        assertTrue(viewModel.uiState.value.pictureAdjustment.isNightModeEnabled)
        assertEquals(0.5f, viewModel.uiState.value.pictureAdjustment.nightModeWarmth)

        viewModel.resetPictureAdjustments()
        assertEquals(1.0f, viewModel.uiState.value.pictureAdjustment.contrast)
        assertEquals(1.0f, viewModel.uiState.value.pictureAdjustment.saturation)
        assertFalse(viewModel.uiState.value.pictureAdjustment.isNightModeEnabled)

        viewModel.clearForTesting()
    }

    @Test
    fun abRepeat_setsPoints_andLoopsBetweenAandB() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 10_000L,
            durationMs = sampleVideo.durationMs,
            isPlaying = true
        )
        runCurrent()

        // Set Point A at 10s
        viewModel.setAbPointA()
        assertEquals(10_000L, viewModel.uiState.value.abRepeat.pointA)
        assertNull(viewModel.uiState.value.abRepeat.pointB)
        assertFalse(viewModel.uiState.value.abRepeat.isLooping)

        // Advance to 25s and set Point B
        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 25_000L,
            durationMs = sampleVideo.durationMs,
            isPlaying = true
        )
        runCurrent()
        viewModel.setAbPointB()
        assertEquals(10_000L, viewModel.uiState.value.abRepeat.pointA)
        assertEquals(25_000L, viewModel.uiState.value.abRepeat.pointB)
        assertTrue(viewModel.uiState.value.abRepeat.isLooping)

        // Playback reaches 25,000ms -> should automatically loop back to Point A (10,000ms)
        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 25_001L,
            durationMs = sampleVideo.durationMs,
            isPlaying = true
        )
        runCurrent()
        assertEquals(10_000L, controller.lastSeekPosition)

        // Clear A-B repeat
        viewModel.clearAbRepeat()
        assertNull(viewModel.uiState.value.abRepeat.pointA)
        assertNull(viewModel.uiState.value.abRepeat.pointB)
        assertFalse(viewModel.uiState.value.abRepeat.isLooping)

        viewModel.clearForTesting()
    }

    @Test
    fun stepFrame_isDelegatedToThePlayerSoItCanStepByRealFrames() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        viewModel.stepFrame(forward = true)
        viewModel.stepFrame(forward = false)
        runCurrent()

        // The fixed "+42 ms" guess that used to live here is gone: the real player decides what a frame is
        // (see Media3PlaybackControllerTest for the stepping behaviour against a real ExoPlayer).
        assertEquals(listOf(true, false), controller.steppedFrames)

        viewModel.clearForTesting()
    }

    @Test
    fun playbackEnded_withQueueHasNext_triggersAutoAdvanceCountdown() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val queue = listOf(
            sampleVideo,
            DomainMediaItem(
                uri = "content://media/2",
                title = "Next Video",
                durationMs = 60_000L,
                sizeBytes = 1000L,
                mimeType = "video/mp4",
                dateAddedSec = 2000L,
                folderPath = "Movies"
            )
        )
        viewModel.playQueue(queue, 0)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ended(sampleVideo.durationMs)
        runCurrent()

        assertEquals(10, viewModel.uiState.value.autoAdvanceCountdownSeconds)
        assertEquals("Next Video", viewModel.uiState.value.nextItemTitle)

        advanceTimeBy(1001L)
        runCurrent()
        assertEquals(9, viewModel.uiState.value.autoAdvanceCountdownSeconds)

        viewModel.cancelAutoAdvance()
        assertNull(viewModel.uiState.value.autoAdvanceCountdownSeconds)
        assertNull(viewModel.uiState.value.nextItemTitle)

        viewModel.clearForTesting()
    }

    @Test
    fun playNextImmediately_skipsToNextAndCancelsCountdown() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val nextItem = DomainMediaItem(
            uri = "content://media/2",
            title = "Next Video",
            durationMs = 60_000L,
            sizeBytes = 1000L,
            mimeType = "video/mp4",
            dateAddedSec = 2000L,
            folderPath = "Movies"
        )
        val queue = listOf(sampleVideo, nextItem)
        viewModel.playQueue(queue, 0)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ended(sampleVideo.durationMs)
        runCurrent()

        assertEquals(10, viewModel.uiState.value.autoAdvanceCountdownSeconds)

        viewModel.playNextImmediately()
        runCurrent()

        assertNull(viewModel.uiState.value.autoAdvanceCountdownSeconds)
        assertNull(viewModel.uiState.value.nextItemTitle)

        viewModel.clearForTesting()
    }

    @Test
    fun addExternalSubtitle_delegatesToControllerWithInferredMimeType() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.addExternalSubtitle("content://downloads/my_subtitle.srt")
        runCurrent()

        assertEquals("content://downloads/my_subtitle.srt", controller.lastExternalSubtitleUri)
        assertEquals("application/x-subrip", controller.lastExternalSubtitleMimeType)
        assertEquals("my_subtitle.srt", controller.lastExternalSubtitleLabel)

        viewModel.addExternalSubtitle("content://downloads/captions.vtt")
        runCurrent()
        assertEquals("text/vtt", controller.lastExternalSubtitleMimeType)

        viewModel.clearForTesting()
    }

    @Test
    fun removeExternalSubtitles_delegatesToController() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.removeExternalSubtitles()
        runCurrent()

        assertTrue(controller.hasRemovedExternalSubtitles)
        assertNull(controller.lastExternalSubtitleUri)

        viewModel.clearForTesting()
    }

    @Test
    fun subtitleAppearance_updatesUiStateProperly() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.setSubtitleFontSize(26)
        assertEquals(26, viewModel.uiState.value.subtitleFontSizeSp)

        viewModel.setSubtitleTextColor("#00E5FF")
        assertEquals("#00E5FF", viewModel.uiState.value.subtitleTextColorHex)

        viewModel.setSubtitleBackgroundEnabled(true)
        assertTrue(viewModel.uiState.value.subtitleBackgroundEnabled)

        viewModel.clearForTesting()
    }

    @Test
    fun addExternalSubtitles_batchesProperly() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val sidecars = listOf(
            org.aptura.player.domain.model.DomainExternalSubtitle(
                uri = "content://media/1",
                label = "English (movie.en.srt)",
                language = "en",
                isDefault = true
            ),
            org.aptura.player.domain.model.DomainExternalSubtitle(
                uri = "content://media/2",
                label = "Spanish (movie.es.srt)",
                language = "es"
            )
        )
        viewModel.addExternalSubtitles(sidecars)
        runCurrent()

        assertEquals(2, controller.lastExternalSubtitles?.size)
        assertEquals("content://media/1", controller.lastExternalSubtitles?.first()?.uri)
        assertEquals("English (movie.en.srt)", controller.lastExternalSubtitles?.first()?.label)

        viewModel.clearForTesting()
    }

    @Test
    fun bookmarks_addAndRemove_reflectedInState() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        repository.items[sampleVideo.uri] = sampleVideo
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        assertEquals(0, viewModel.uiState.value.bookmarks.size)

        // Add bookmark
        viewModel.addBookmark("Highlight 1")
        runCurrent()

        assertEquals(1, viewModel.uiState.value.bookmarks.size)
        val created = viewModel.uiState.value.bookmarks.first()
        assertEquals("Highlight 1", created.note)
        assertEquals(sampleVideo.uri, created.mediaUri)

        // Delete bookmark
        viewModel.deleteBookmark(created.id)
        runCurrent()

        assertEquals(0, viewModel.uiState.value.bookmarks.size)

        viewModel.clearForTesting()
    }

    @Test
    fun decoderStrategy_updatesStateCorrectly() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertEquals(0, viewModel.uiState.value.decoderStrategy)

        viewModel.setDecoderStrategy(1)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.decoderStrategy)
        assertEquals(1, controller.lastDecoderStrategy)

        viewModel.setDecoderStrategy(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.decoderStrategy)
        assertEquals(2, controller.lastDecoderStrategy)

        viewModel.clearForTesting()
    }

    @Test
    fun errorState_enablesSoftwareFallbackAffordance() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        controller._playbackState.value = DomainPlaybackState.Error(
            errorCode = "UNSUPPORTED_PROFILE",
            message = "Decoder Profile Incompatible: 10-bit HEVC profile",
            isRecoverable = true
        )
        advanceUntilIdle()

        assertEquals("UNSUPPORTED_PROFILE", viewModel.uiState.value.errorCode)
        assertEquals("UNSUPPORTED_PROFILE", viewModel.uiState.value.errorTaxonomyCode)
        assertTrue(viewModel.uiState.value.canSwitchToSoftware)
        assertTrue(viewModel.uiState.value.isRecoverableError)

        viewModel.clearForTesting()
    }

    @Test
    fun compatibilityMode_updatesFromController() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        controller._isCompatibilityMode.value = true
        controller._compatibilityNotice.value = "Playing in compatibility mode (Software Decoder)"
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompatibilityMode)
        assertEquals("Playing in compatibility mode (Software Decoder)", viewModel.uiState.value.compatibilityNotice)

        viewModel.dismissCompatibilityNotice()
        assertEquals(null, viewModel.uiState.value.compatibilityNotice)

        viewModel.clearForTesting()
    }

    @Test
    fun subtitleWebSearch_queryAndDialogState_updatedCorrectly() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertFalse(viewModel.uiState.value.isWebSearchDialogVisible)
        viewModel.showWebSearchDialog(true)
        assertTrue(viewModel.uiState.value.isWebSearchDialogVisible)

        viewModel.setCleanSearchQuery("Oppenheimer 2023")
        assertEquals("Oppenheimer 2023", viewModel.uiState.value.cleanSearchQuery)

        viewModel.showWebSearchDialog(false)
        assertFalse(viewModel.uiState.value.isWebSearchDialogVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun autoPairedSubtitleToast_andRecentDownload_updatedAndDismissed() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertNull(viewModel.uiState.value.autoPairedSubtitleToast)
        viewModel.setAutoPairedSubtitleToast("Auto-paired: English")
        assertEquals("Auto-paired: English", viewModel.uiState.value.autoPairedSubtitleToast)

        viewModel.dismissAutoPairToast()
        assertNull(viewModel.uiState.value.autoPairedSubtitleToast)

        val sidecar = org.aptura.player.data.repository.DetectedSidecarSubtitle(
            uri = android.net.Uri.parse("content://subtitles/1"),
            displayName = "movie.srt",
            mimeType = "application/x-subrip",
            languageCode = "en",
            languageLabel = "English",
            isDefault = true
        )
        viewModel.setRecentDownloadedSubtitle(sidecar)
        assertEquals(sidecar, viewModel.uiState.value.recentDownloadedSubtitle)

        viewModel.setRecentDownloadedSubtitle(null)
        assertNull(viewModel.uiState.value.recentDownloadedSubtitle)

        viewModel.clearForTesting()
    }

    @Test
    fun checkpointPlayback_pausesAndPersistsPosition() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        repository.items[sampleVideo.uri] = sampleVideo
        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 1420500L,
            durationMs = sampleVideo.durationMs,
            isPlaying = true
        )
        runCurrent()

        viewModel.checkpointPlayback()
        runCurrent()

        assertFalse(controller.isPlaying)
        assertEquals(1420500L, repository.savedPositions[sampleVideo.uri]?.first)

        viewModel.clearForTesting()
    }

    @Test
    fun testQueueItemsEmittedToUiState() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val items = listOf(
            DomainMediaItem("uri:1", "Video 1", 60000L, 1000L, "video/mp4"),
            DomainMediaItem("uri:2", "Video 2", 120000L, 2000L, "video/mp4"),
            DomainMediaItem("uri:3", "Video 3", 180000L, 3000L, "video/mp4")
        )

        viewModel.playQueue(items, 1)
        runCurrent()

        assertEquals(3, viewModel.uiState.value.queueItems.size)
        assertEquals(1, viewModel.uiState.value.currentQueueIndex)
        assertEquals("uri:2", viewModel.uiState.value.videoUri)
        assertTrue(viewModel.uiState.value.hasNext)
        assertTrue(viewModel.uiState.value.hasPrevious)

        viewModel.clearForTesting()
    }

    @Test
    fun testJumpToQueueItem() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val items = listOf(
            DomainMediaItem("uri:1", "Video 1", 60000L, 1000L, "video/mp4"),
            DomainMediaItem("uri:2", "Video 2", 120000L, 2000L, "video/mp4"),
            DomainMediaItem("uri:3", "Video 3", 180000L, 3000L, "video/mp4")
        )

        viewModel.playQueue(items, 0)
        runCurrent()

        viewModel.jumpToQueueItem(2)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.currentQueueIndex)
        assertEquals("uri:3", viewModel.uiState.value.videoUri)
        assertFalse(viewModel.uiState.value.hasNext)
        assertTrue(viewModel.uiState.value.hasPrevious)

        viewModel.clearForTesting()
    }

    @Test
    fun testMoveQueueItem() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val items = listOf(
            DomainMediaItem("uri:1", "Video 1", 60000L, 1000L, "video/mp4"),
            DomainMediaItem("uri:2", "Video 2", 120000L, 2000L, "video/mp4"),
            DomainMediaItem("uri:3", "Video 3", 180000L, 3000L, "video/mp4")
        )

        viewModel.playQueue(items, 0)
        runCurrent()

        // Move Video 1 from index 0 to index 2
        viewModel.moveQueueItem(0, 2)
        runCurrent()

        assertEquals(3, viewModel.uiState.value.queueItems.size)
        assertEquals("uri:2", viewModel.uiState.value.queueItems[0].uri)
        assertEquals("uri:3", viewModel.uiState.value.queueItems[1].uri)
        assertEquals("uri:1", viewModel.uiState.value.queueItems[2].uri)

        viewModel.clearForTesting()
    }

    @Test
    fun testRemoveFromQueue() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val items = listOf(
            DomainMediaItem("uri:1", "Video 1", 60000L, 1000L, "video/mp4"),
            DomainMediaItem("uri:2", "Video 2", 120000L, 2000L, "video/mp4"),
            DomainMediaItem("uri:3", "Video 3", 180000L, 3000L, "video/mp4")
        )

        viewModel.playQueue(items, 0)
        runCurrent()

        // Remove item at index 1
        viewModel.removeFromQueue(1)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.queueItems.size)
        assertEquals("uri:1", viewModel.uiState.value.queueItems[0].uri)
        assertEquals("uri:3", viewModel.uiState.value.queueItems[1].uri)

        viewModel.clearForTesting()
    }

    @Test
    fun testClearQueue() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val items = listOf(
            DomainMediaItem("uri:1", "Video 1", 60000L, 1000L, "video/mp4"),
            DomainMediaItem("uri:2", "Video 2", 120000L, 2000L, "video/mp4")
        )

        viewModel.playQueue(items, 1)
        runCurrent()

        viewModel.clearQueue()
        runCurrent()

        assertEquals(1, viewModel.uiState.value.queueItems.size)
        assertEquals("uri:2", viewModel.uiState.value.queueItems[0].uri)
        assertEquals(0, viewModel.uiState.value.currentQueueIndex)

        viewModel.clearForTesting()
    }

    @Test
    fun testCyclePlayMode() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertEquals(PlayMode.NORMAL, viewModel.uiState.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.REPEAT_ALL, viewModel.uiState.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.REPEAT_ONE, viewModel.uiState.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.SHUFFLE, viewModel.uiState.value.playMode)

        viewModel.togglePlayMode()
        runCurrent()
        assertEquals(PlayMode.NORMAL, viewModel.uiState.value.playMode)

        viewModel.clearForTesting()
    }

    @Test
    fun testAddToQueueAndPlayNext() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val items = listOf(
            DomainMediaItem("uri:1", "Video 1", 60000L, 1000L, "video/mp4"),
            DomainMediaItem("uri:2", "Video 2", 120000L, 2000L, "video/mp4")
        )

        viewModel.playQueue(items, 0)
        runCurrent()

        val itemToInsert = DomainMediaItem("uri:inserted", "Inserted Next", 90000L, 1500L, "video/mp4")
        viewModel.playNext(itemToInsert)
        runCurrent()

        assertEquals(3, viewModel.uiState.value.queueItems.size)
        assertEquals("uri:inserted", viewModel.uiState.value.queueItems[1].uri)

        val itemToAppend = DomainMediaItem("uri:appended", "Appended Last", 45000L, 800L, "video/mp4")
        viewModel.addToQueue(itemToAppend)
        runCurrent()

        assertEquals(4, viewModel.uiState.value.queueItems.size)
        assertEquals("uri:appended", viewModel.uiState.value.queueItems[3].uri)

        viewModel.clearForTesting()
    }

    @Test
    fun testReplayCurrentItem() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ended(sampleVideo.durationMs)
        runCurrent()
        assertTrue(viewModel.uiState.value.isEnded)

        viewModel.replayCurrentItem()
        runCurrent()

        assertEquals(0L, controller.lastSeekPosition)
        assertTrue(controller.isPlaying)
        assertFalse(viewModel.uiState.value.isEnded)

        viewModel.clearForTesting()
    }

    @Test
    fun testToggleStatsOverlay() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertFalse(viewModel.uiState.value.isStatsOverlayVisible)

        viewModel.toggleStatsOverlay()
        runCurrent()
        assertTrue(viewModel.uiState.value.isStatsOverlayVisible)

        viewModel.toggleStatsOverlay(false)
        runCurrent()
        assertFalse(viewModel.uiState.value.isStatsOverlayVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun testWhyCompatibilitySheetVisibility() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertFalse(viewModel.uiState.value.isWhyCompatibilitySheetVisible)

        viewModel.setWhyCompatibilitySheetVisible(true)
        runCurrent()
        assertTrue(viewModel.uiState.value.isWhyCompatibilitySheetVisible)

        viewModel.setWhyCompatibilitySheetVisible(false)
        runCurrent()
        assertFalse(viewModel.uiState.value.isWhyCompatibilitySheetVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun testRestorePerVideoPlaybackPreferencesOnPlayQueue() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val customVideo = DomainMediaItem(
            uri = "content://media/custom",
            title = "Custom Preferences Video",
            durationMs = 90000L,
            sizeBytes = 4096L,
            mimeType = "video/mp4",
            resizeMode = "ZOOM",
            playbackSpeed = 1.75f,
            decoderStrategy = 2,
            audioDelayMs = 250L,
            subtitleDelayMs = -150L
        )

        viewModel.playQueue(listOf(customVideo), 0)
        runCurrent()

        assertEquals(AspectRatioMode.ZOOM, viewModel.uiState.value.resizeMode)
        assertEquals(1.75f, viewModel.uiState.value.playbackSpeed)
        assertEquals(2, viewModel.uiState.value.decoderStrategy)
        assertEquals(250L, viewModel.uiState.value.audioDelayMs)
        assertEquals(-150L, viewModel.uiState.value.subtitleDelayMs)

        assertEquals(1.75f, controller.lastSpeed)
        assertEquals(2, controller.lastDecoderStrategy)
        assertEquals(250L, controller.lastAudioDelay)
        assertEquals(-150L, controller.lastSubtitleDelay)

        viewModel.clearForTesting()
    }

    @Test
    fun testResetVideoPreferencesRestoresDefaults() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        val customVideo = DomainMediaItem(
            uri = "content://media/to_reset",
            title = "To Reset Video",
            durationMs = 90000L,
            sizeBytes = 4096L,
            mimeType = "video/mp4",
            resizeMode = "ZOOM",
            playbackSpeed = 1.5f,
            decoderStrategy = 1,
            audioDelayMs = 100L,
            subtitleDelayMs = 50L
        )
        repository.items[customVideo.uri] = customVideo

        viewModel.playQueue(listOf(customVideo), 0)
        runCurrent()

        assertEquals(AspectRatioMode.ZOOM, viewModel.uiState.value.resizeMode)
        assertEquals(1.5f, viewModel.uiState.value.playbackSpeed)

        viewModel.resetVideoPreferences()
        runCurrent()

        assertEquals(AspectRatioMode.FIT, viewModel.uiState.value.resizeMode)
        assertEquals(1.0f, viewModel.uiState.value.playbackSpeed)
        assertEquals(0, viewModel.uiState.value.decoderStrategy)
        assertEquals(0L, viewModel.uiState.value.audioDelayMs)
        assertEquals(0L, viewModel.uiState.value.subtitleDelayMs)
        assertEquals("content://media/to_reset", repository.lastResetPreferencesUri)

        viewModel.clearForTesting()
    }

    @Test
    fun testModalMutualExclusivity_openingWhySheetDismissesQueueSheet() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.showQueueSheet(true)
        assertTrue(viewModel.uiState.value.isQueueSheetVisible)
        assertFalse(viewModel.uiState.value.isWhyCompatibilitySheetVisible)

        // Opening Why sheet must atomically dismiss Queue sheet
        viewModel.setWhyCompatibilitySheetVisible(true)
        assertTrue(viewModel.uiState.value.isWhyCompatibilitySheetVisible)
        assertFalse(viewModel.uiState.value.isQueueSheetVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun testModalMutualExclusivity_openingAdvancedSheetDismissesAudioSheet() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.setAudioSheetVisible(true)
        assertTrue(viewModel.uiState.value.isAudioSheetVisible)
        assertFalse(viewModel.uiState.value.isAdvancedSheetVisible)

        // Opening Advanced sheet must atomically dismiss Audio sheet
        viewModel.setAdvancedSheetVisible(true)
        assertTrue(viewModel.uiState.value.isAdvancedSheetVisible)
        assertFalse(viewModel.uiState.value.isAudioSheetVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun testModalMutualExclusivity_strictSingleActiveModalInvariant() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        fun countVisibleSheets(): Int {
            val s = viewModel.uiState.value
            return listOf(
                s.isAudioSheetVisible,
                s.isSubtitleSheetVisible,
                s.isSpeedSheetVisible,
                s.isAdvancedSheetVisible,
                s.isEqualizerSheetVisible,
                s.isPictureSheetVisible,
                s.isQueueSheetVisible,
                s.isPlayModeSheetVisible,
                s.isWhyCompatibilitySheetVisible,
                s.isWebSearchDialogVisible
            ).count { it }
        }

        assertEquals(0, countVisibleSheets())
        assertFalse(viewModel.uiState.value.isAnySheetVisible)

        val sheetOpeners: List<() -> Unit> = listOf(
            { viewModel.showQueueSheet(true) },
            { viewModel.showPlayModeSheet(true) },
            { viewModel.setAudioSheetVisible(true) },
            { viewModel.setSubtitleSheetVisible(true) },
            { viewModel.setSpeedSheetVisible(true) },
            { viewModel.setWhyCompatibilitySheetVisible(true) },
            { viewModel.setAdvancedSheetVisible(true) },
            { viewModel.setEqualizerSheetVisible(true) },
            { viewModel.setPictureSheetVisible(true) },
            { viewModel.showWebSearchDialog(true) }
        )

        for (opener in sheetOpeners) {
            opener()
            assertEquals("Exactly one sheet must be visible at any given time", 1, countVisibleSheets())
            assertTrue(viewModel.uiState.value.isAnySheetVisible)
        }

        // Dismissing the last active sheet leaves 0 visible
        viewModel.showWebSearchDialog(false)
        assertEquals(0, countVisibleSheets())
        assertFalse(viewModel.uiState.value.isAnySheetVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun testModalMutualExclusivity_exhaustivePairwiseTransitionMatrix() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        data class SheetDescriptor(
            val name: String,
            val open: (PlayerViewModel) -> Unit,
            val isVisible: (PlayerUiState) -> Boolean
        )

        val sheets = listOf(
            SheetDescriptor("Queue", { it.showQueueSheet(true) }, { it.isQueueSheetVisible }),
            SheetDescriptor("PlayMode", { it.showPlayModeSheet(true) }, { it.isPlayModeSheetVisible }),
            SheetDescriptor("Audio", { it.setAudioSheetVisible(true) }, { it.isAudioSheetVisible }),
            SheetDescriptor("Subtitle", { it.setSubtitleSheetVisible(true) }, { it.isSubtitleSheetVisible }),
            SheetDescriptor("Speed", { it.setSpeedSheetVisible(true) }, { it.isSpeedSheetVisible }),
            SheetDescriptor("WhyCompatibility", { it.setWhyCompatibilitySheetVisible(true) }, { it.isWhyCompatibilitySheetVisible }),
            SheetDescriptor("Advanced", { it.setAdvancedSheetVisible(true) }, { it.isAdvancedSheetVisible }),
            SheetDescriptor("Equalizer", { it.setEqualizerSheetVisible(true) }, { it.isEqualizerSheetVisible }),
            SheetDescriptor("Picture", { it.setPictureSheetVisible(true) }, { it.isPictureSheetVisible }),
            SheetDescriptor("WebSearch", { it.showWebSearchDialog(true) }, { it.isWebSearchDialogVisible })
        )

        fun countVisible(state: PlayerUiState): Int = sheets.count { it.isVisible(state) }

        // Test all N x (N - 1) pairwise transitions (90 distinct permutations)
        for (sheetA in sheets) {
            for (sheetB in sheets) {
                if (sheetA.name == sheetB.name) continue

                sheetA.open(viewModel)
                assertEquals("After opening ${sheetA.name}, exactly 1 sheet must be visible", 1, countVisible(viewModel.uiState.value))
                assertTrue("Sheet ${sheetA.name} must be visible", sheetA.isVisible(viewModel.uiState.value))
                assertFalse("Sheet ${sheetB.name} must NOT be visible yet", sheetB.isVisible(viewModel.uiState.value))

                sheetB.open(viewModel)
                assertEquals("Transitioning from ${sheetA.name} to ${sheetB.name} must maintain count == 1", 1, countVisible(viewModel.uiState.value))
                assertTrue("Sheet ${sheetB.name} must now be visible", sheetB.isVisible(viewModel.uiState.value))
                assertFalse("Sheet ${sheetA.name} must be atomically closed", sheetA.isVisible(viewModel.uiState.value))
            }
        }

        viewModel.clearForTesting()
    }

    @Test
    fun testModalMutualExclusivity_rapidInterleavedTriggers_maintainsInvariant() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        data class SheetDescriptor(
            val name: String,
            val open: (PlayerViewModel) -> Unit,
            val isVisible: (PlayerUiState) -> Boolean
        )

        val sheets = listOf(
            SheetDescriptor("Queue", { it.showQueueSheet(true) }, { it.isQueueSheetVisible }),
            SheetDescriptor("PlayMode", { it.showPlayModeSheet(true) }, { it.isPlayModeSheetVisible }),
            SheetDescriptor("Audio", { it.setAudioSheetVisible(true) }, { it.isAudioSheetVisible }),
            SheetDescriptor("Subtitle", { it.setSubtitleSheetVisible(true) }, { it.isSubtitleSheetVisible }),
            SheetDescriptor("Speed", { it.setSpeedSheetVisible(true) }, { it.isSpeedSheetVisible }),
            SheetDescriptor("WhyCompatibility", { it.setWhyCompatibilitySheetVisible(true) }, { it.isWhyCompatibilitySheetVisible }),
            SheetDescriptor("Advanced", { it.setAdvancedSheetVisible(true) }, { it.isAdvancedSheetVisible }),
            SheetDescriptor("Equalizer", { it.setEqualizerSheetVisible(true) }, { it.isEqualizerSheetVisible }),
            SheetDescriptor("Picture", { it.setPictureSheetVisible(true) }, { it.isPictureSheetVisible }),
            SheetDescriptor("WebSearch", { it.showWebSearchDialog(true) }, { it.isWebSearchDialogVisible })
        )

        fun countVisible(state: PlayerUiState): Int = sheets.count { it.isVisible(state) }

        // Rapid interleaved firing simulating multi-touch race conditions
        repeat(5) {
            for (sheet in sheets) {
                sheet.open(viewModel)
                assertEquals(1, countVisible(viewModel.uiState.value))
                assertTrue(sheet.isVisible(viewModel.uiState.value))
                assertTrue(viewModel.uiState.value.isAnySheetVisible)
            }
        }

        // Dismissing last sheet cleanly resets isAnySheetVisible
        viewModel.showWebSearchDialog(false)
        assertEquals(0, countVisible(viewModel.uiState.value))
        assertFalse(viewModel.uiState.value.isAnySheetVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun testSeekRelative_rapidAccumulation_preservesTargetPositionAndSide() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 20000L,
            durationMs = 100000L,
            isPlaying = true
        )
        runCurrent()

        // 1st tap: +10s
        viewModel.seekRelative(10000L, SeekTapSide.RIGHT)
        runCurrent()
        assertEquals(30000L, controller.lastSeekPosition)
        assertEquals(10, viewModel.uiState.value.seekDeltaSeconds)
        assertEquals(SeekTapSide.RIGHT, viewModel.uiState.value.doubleTapSeekSide)

        // 2nd tap within 800ms: target should accumulate to 40000L even if currentPositionMs in state is still 20000L
        advanceTimeBy(200L)
        viewModel.seekRelative(10000L, SeekTapSide.RIGHT)
        runCurrent()
        assertEquals(40000L, controller.lastSeekPosition)
        assertEquals(20, viewModel.uiState.value.seekDeltaSeconds)
        assertEquals(SeekTapSide.RIGHT, viewModel.uiState.value.doubleTapSeekSide)

        // 3rd tap within 800ms: target accumulates to 50000L
        advanceTimeBy(200L)
        viewModel.seekRelative(10000L, SeekTapSide.RIGHT)
        runCurrent()
        assertEquals(50000L, controller.lastSeekPosition)
        assertEquals(30, viewModel.uiState.value.seekDeltaSeconds)
        assertEquals(SeekTapSide.RIGHT, viewModel.uiState.value.doubleTapSeekSide)

        // After feedback overlay delay (1200ms), state clears
        advanceTimeBy(1300L)
        runCurrent()
        assertNull(viewModel.uiState.value.seekDeltaSeconds)
        assertNull(viewModel.uiState.value.doubleTapSeekSide)

        viewModel.clearForTesting()
    }

    @Test
    fun testHorizontalSwipeSeek_lifecycle_calculatesDeltaAndCommits() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 30000L,
            durationMs = 120000L,
            isPlaying = true
        )
        runCurrent()

        // 1. Start swipe seek
        viewModel.onHorizontalSwipeSeekStart()
        assertEquals(30000L, viewModel.uiState.value.swipeSeekTargetMs)
        assertEquals(0L, viewModel.uiState.value.swipeSeekDeltaMs)

        // 2. Drag forward +15s
        viewModel.onHorizontalSwipeSeek(15000L)
        assertEquals(45000L, viewModel.uiState.value.swipeSeekTargetMs)
        assertEquals(15000L, viewModel.uiState.value.swipeSeekDeltaMs)

        // 3. Drag backward -10s relative to start
        viewModel.onHorizontalSwipeSeek(-10000L)
        assertEquals(20000L, viewModel.uiState.value.swipeSeekTargetMs)
        assertEquals(-10000L, viewModel.uiState.value.swipeSeekDeltaMs)

        // 4. Commit seek
        viewModel.onHorizontalSwipeSeekCommit()
        runCurrent()
        assertEquals(20000L, controller.lastSeekPosition)
        assertNull(viewModel.uiState.value.swipeSeekTargetMs)
        assertNull(viewModel.uiState.value.swipeSeekDeltaMs)

        viewModel.clearForTesting()
    }

    @Test
    fun testHorizontalSwipeSeek_cancel_revertsWithoutSeeking() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 30000L,
            durationMs = 120000L,
            isPlaying = true
        )
        runCurrent()

        val initialSeekPos = controller.lastSeekPosition

        viewModel.onHorizontalSwipeSeekStart()
        viewModel.onHorizontalSwipeSeek(25000L)
        assertEquals(55000L, viewModel.uiState.value.swipeSeekTargetMs)

        // Cancel
        viewModel.onHorizontalSwipeSeekCancel()
        runCurrent()
        assertEquals(initialSeekPos, controller.lastSeekPosition)
        assertNull(viewModel.uiState.value.swipeSeekTargetMs)
        assertNull(viewModel.uiState.value.swipeSeekDeltaMs)

        viewModel.clearForTesting()
    }

    @Test
    fun testTriggerLockHint_showsAndAutoDismisses() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        assertFalse(viewModel.uiState.value.isLockHintVisible)

        viewModel.triggerLockHint()
        assertTrue(viewModel.uiState.value.isLockHintVisible)

        advanceTimeBy(1300L)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLockHintVisible)

        viewModel.clearForTesting()
    }

    @Test
    fun testToggleControlsVisibility_whenLocked_triggersLockHintWithoutTogglingControls() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.toggleLock() // Lock screen
        assertTrue(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isControlsVisible)
        assertFalse(viewModel.uiState.value.isLockHintVisible)

        // Attempting to toggle controls should not make controls visible, but should trigger lock hint
        viewModel.toggleControlsVisibility()
        assertFalse(viewModel.uiState.value.isControlsVisible)
        assertTrue(viewModel.uiState.value.isLockHintVisible)

        advanceTimeBy(1300L)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLockHintVisible)

        viewModel.clearForTesting()
    }
}

