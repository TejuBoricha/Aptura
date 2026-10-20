package org.aptura.player.ui.player

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.DomainTrackSelection
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.repository.MediaRepository
import org.aptura.player.domain.repository.PlaybackController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for PlayerViewModel.
 * Verifies UDF state emissions, 5s auto-hide HUD, multi-tap seek accumulation,
 * scrub timeline precision, aspect ratio cycling, speed ladders, and progress restoration.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

        override suspend fun skipToNext() {}
        override suspend fun skipToPrevious() {}

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

        override suspend fun setSubtitleDelay(delayMs: Long) {}
        override suspend fun setAudioDelay(delayMs: Long) {}
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
}
