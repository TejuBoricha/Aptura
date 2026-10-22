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
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.repository.MediaRepository
import org.aptura.player.domain.repository.PlaybackController
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
    fun stepFrame_seeksForwardAndBackwardByDelta() = runTest(testDispatcher) {
        val controller = FakePlaybackController()
        val repository = FakeMediaRepository()
        val viewModel = PlayerViewModel(controller, repository, testDispatcher)

        viewModel.loadAndPlay(sampleVideo.uri)
        runCurrent()

        controller._playbackState.value = DomainPlaybackState.Ready(
            positionMs = 5000L,
            durationMs = sampleVideo.durationMs,
            isPlaying = false
        )
        runCurrent()

        viewModel.stepFrame(forward = true)
        runCurrent()
        assertEquals(5042L, controller.lastSeekPosition)
        assertEquals(5042L, viewModel.uiState.value.currentPositionMs)

        viewModel.stepFrame(forward = false)
        runCurrent()
        assertEquals(5000L, controller.lastSeekPosition)
        assertEquals(5000L, viewModel.uiState.value.currentPositionMs)

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
}

