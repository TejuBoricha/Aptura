package org.aptura.player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.repository.MediaRepository
import org.aptura.player.domain.repository.PlaybackController
import java.io.File

/**
 * ViewModel orchestrating player HUD state, transport controls, and progress persistence.
 * Follows Unidirectional Data Flow (UDF) and remains pure Kotlin (decoupled from Android framework).
 */
class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val mediaRepository: MediaRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var autoHideJob: Job? = null
    private var volumeFeedbackJob: Job? = null
    private var brightnessFeedbackJob: Job? = null
    private var seekFeedbackJob: Job? = null
    private var lastPersistedPositionMs: Long = 0L

    private var lastSeekTimestampMs: Long = 0L
    private var accumulatedSeekDeltaMs: Long = 0L

    companion object {
        const val AUTO_HIDE_DELAY_MS = 5000L // FR-CTRL-001, Media3 DEFAULT_SHOW_TIMEOUT_MS
        const val FEEDBACK_OVERLAY_DELAY_MS = 1200L
        const val SEEK_ACCUMULATION_THRESHOLD_MS = 800L
        const val CHECKPOINT_INTERVAL_MS = 5000L // TST-REST-01, spec/12A §3.3
    }

    init {
        observePlaybackState()
        observePlaybackQueue()
        observeTrackSelection()
    }

    private fun observePlaybackState() {
        viewModelScope.launch(dispatcher) {
            playbackController.playbackState.collect { state ->
                when (state) {
                    is DomainPlaybackState.Idle -> {
                        _uiState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                isEnded = false
                            )
                        }
                    }
                    is DomainPlaybackState.Buffering -> {
                        _uiState.update {
                            it.copy(
                                isBuffering = true,
                                isPlaying = false,
                                isEnded = false,
                                currentPositionMs = state.positionMs,
                                errorMessage = null
                            )
                        }
                    }
                    is DomainPlaybackState.Ready -> {
                        _uiState.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = state.isPlaying,
                                isEnded = false,
                                currentPositionMs = state.positionMs,
                                durationMs = state.durationMs,
                                playbackSpeed = state.playbackSpeed,
                                errorMessage = null
                            )
                        }
                        if (state.isPlaying) {
                            if (Math.abs(state.positionMs - lastPersistedPositionMs) >= CHECKPOINT_INTERVAL_MS) {
                                lastPersistedPositionMs = state.positionMs
                                persistCurrentPosition()
                            }
                            if (_uiState.value.isControlsVisible) {
                                scheduleAutoHide()
                            }
                        }
                    }
                    is DomainPlaybackState.Ended -> {
                        _uiState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                isEnded = true,
                                currentPositionMs = state.durationMs,
                                durationMs = state.durationMs,
                                isControlsVisible = true // Always show controls on playback end
                            )
                        }
                        val currentUri = _uiState.value.videoUri
                        if (currentUri.isNotBlank()) {
                            mediaRepository.savePlaybackPosition(
                                uri = currentUri,
                                positionMs = state.durationMs,
                                isCompleted = true
                            )
                        }
                    }
                    is DomainPlaybackState.Error -> {
                        _uiState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                errorMessage = state.message,
                                errorCode = state.errorCode,
                                isRecoverableError = state.isRecoverable,
                                isControlsVisible = true // Lock controls open during error
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observePlaybackQueue() {
        viewModelScope.launch(dispatcher) {
            playbackController.playbackQueue.collect { queue ->
                _uiState.update {
                    it.copy(playMode = queue.playMode)
                }
            }
        }
    }

    private fun observeTrackSelection() {
        viewModelScope.launch(dispatcher) {
            playbackController.trackSelection.collect { selection ->
                _uiState.update {
                    it.copy(
                        playbackSpeed = selection.playbackSpeed,
                        selectedAudioTrackIndex = selection.selectedAudioTrackIndex,
                        selectedSubtitleTrackIndex = selection.selectedSubtitleTrackIndex
                    )
                }
            }
        }
    }

    fun loadAndPlay(videoUri: String) {
        viewModelScope.launch(dispatcher) {
            val item = mediaRepository.getVideoByUri(videoUri)
            val title = item?.title ?: extractTitleFromUri(videoUri)
            val initialDuration = item?.durationMs ?: 0L

            _uiState.update {
                it.copy(
                    videoUri = videoUri,
                    videoTitle = title,
                    durationMs = initialDuration,
                    isControlsVisible = true
                )
            }

            val domainItem = item ?: DomainMediaItem(
                uri = videoUri,
                title = title,
                durationMs = initialDuration,
                sizeBytes = 0L,
                mimeType = "video/*"
            )

            playbackController.playMediaItem(domainItem)

            // Restore playback position if > 3s and not completed (spec/12A §3.3)
            val savedPosition = item?.playbackPositionMs ?: 0L
            if (savedPosition > 3000L && !(item?.isCompleted ?: false)) {
                val preRollPosition = (savedPosition - 2000L).coerceAtLeast(0L)
                playbackController.seekTo(preRollPosition)
            }

            scheduleAutoHide()
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch(dispatcher) {
            if (_uiState.value.isEnded) {
                playbackController.seekTo(0L)
                playbackController.play()
                _uiState.update { it.copy(isEnded = false) }
            } else if (_uiState.value.isPlaying) {
                playbackController.pause()
                persistCurrentPosition()
            } else {
                playbackController.play()
            }
            scheduleAutoHide()
        }
    }

    fun seekRelative(deltaMs: Long) {
        viewModelScope.launch(dispatcher) {
            val now = System.currentTimeMillis()
            val isSameDirection = (deltaMs > 0 && accumulatedSeekDeltaMs > 0) ||
                    (deltaMs < 0 && accumulatedSeekDeltaMs < 0)

            if (now - lastSeekTimestampMs < SEEK_ACCUMULATION_THRESHOLD_MS && isSameDirection) {
                accumulatedSeekDeltaMs += deltaMs
            } else {
                accumulatedSeekDeltaMs = deltaMs
            }
            lastSeekTimestampMs = now

            val currentPos = _uiState.value.currentPositionMs
            val duration = _uiState.value.durationMs
            val targetPos = (currentPos + deltaMs).coerceIn(0L, duration.coerceAtLeast(0L))

            playbackController.seekTo(targetPos)

            _uiState.update {
                it.copy(seekDeltaSeconds = (accumulatedSeekDeltaMs / 1000).toInt())
            }

            seekFeedbackJob?.cancel()
            seekFeedbackJob = viewModelScope.launch(dispatcher) {
                delay(FEEDBACK_OVERLAY_DELAY_MS)
                _uiState.update { it.copy(seekDeltaSeconds = null) }
                accumulatedSeekDeltaMs = 0L
            }

            scheduleAutoHide()
        }
    }

    fun onScrubStart(positionMs: Long) {
        autoHideJob?.cancel()
        _uiState.update {
            it.copy(
                isScrubbing = true,
                scrubPositionMs = positionMs.coerceIn(0L, it.durationMs.coerceAtLeast(0L))
            )
        }
    }

    fun onScrubMove(positionMs: Long) {
        _uiState.update {
            it.copy(
                scrubPositionMs = positionMs.coerceIn(0L, it.durationMs.coerceAtLeast(0L))
            )
        }
    }

    fun onScrubEnd() {
        val targetMs = _uiState.value.scrubPositionMs
        _uiState.update { it.copy(isScrubbing = false) }
        viewModelScope.launch(dispatcher) {
            playbackController.seekTo(targetMs)
            scheduleAutoHide()
        }
    }

    fun toggleControlsVisibility() {
        if (_uiState.value.isLocked) {
            // Briefly trigger unlock hint
            return
        }
        val willBeVisible = !_uiState.value.isControlsVisible
        _uiState.update { it.copy(isControlsVisible = willBeVisible) }
        if (willBeVisible) {
            scheduleAutoHide()
        } else {
            autoHideJob?.cancel()
        }
    }

    fun showControls() {
        if (!_uiState.value.isLocked) {
            _uiState.update { it.copy(isControlsVisible = true) }
            scheduleAutoHide()
        }
    }

    fun toggleLock() {
        val nextLockState = !_uiState.value.isLocked
        _uiState.update {
            it.copy(
                isLocked = nextLockState,
                isControlsVisible = !nextLockState
            )
        }
        if (!nextLockState) {
            scheduleAutoHide()
        } else {
            autoHideJob?.cancel()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch(dispatcher) {
            playbackController.setPlaybackSpeed(speed)
            _uiState.update {
                it.copy(
                    playbackSpeed = speed,
                    isSpeedSheetVisible = false
                )
            }
            scheduleAutoHide()
        }
    }

    fun toggleResizeMode() {
        _uiState.update {
            it.copy(resizeMode = it.resizeMode.next())
        }
        scheduleAutoHide()
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _uiState.update { it.copy(volumePercent = clamped) }
        volumeFeedbackJob?.cancel()
        volumeFeedbackJob = viewModelScope.launch(dispatcher) {
            delay(FEEDBACK_OVERLAY_DELAY_MS)
            _uiState.update { it.copy(volumePercent = null) }
        }
    }

    fun setBrightnessPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _uiState.update { it.copy(brightnessPercent = clamped) }
        brightnessFeedbackJob?.cancel()
        brightnessFeedbackJob = viewModelScope.launch(dispatcher) {
            delay(FEEDBACK_OVERLAY_DELAY_MS)
            _uiState.update { it.copy(brightnessPercent = null) }
        }
    }

    fun setAudioSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isAudioSheetVisible = visible) }
        if (visible) autoHideJob?.cancel() else scheduleAutoHide()
    }

    fun setSubtitleSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isSubtitleSheetVisible = visible) }
        if (visible) autoHideJob?.cancel() else scheduleAutoHide()
    }

    fun setSpeedSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isSpeedSheetVisible = visible) }
        if (visible) autoHideJob?.cancel() else scheduleAutoHide()
    }

    fun selectAudioTrack(index: Int?) {
        viewModelScope.launch(dispatcher) {
            playbackController.selectAudioTrack(index)
            setAudioSheetVisible(false)
        }
    }

    fun selectSubtitleTrack(index: Int?) {
        viewModelScope.launch(dispatcher) {
            playbackController.selectSubtitleTrack(index)
            setSubtitleSheetVisible(false)
        }
    }

    fun retryPlayback() {
        val currentUri = _uiState.value.videoUri
        if (currentUri.isNotBlank()) {
            loadAndPlay(currentUri)
        }
    }

    private fun scheduleAutoHide() {
        autoHideJob?.cancel()
        if (_uiState.value.isPlaying &&
            !_uiState.value.isScrubbing &&
            !_uiState.value.isAudioSheetVisible &&
            !_uiState.value.isSubtitleSheetVisible &&
            !_uiState.value.isSpeedSheetVisible &&
            _uiState.value.errorMessage == null
        ) {
            autoHideJob = viewModelScope.launch(dispatcher) {
                delay(AUTO_HIDE_DELAY_MS)
                if (_uiState.value.isPlaying) {
                    _uiState.update { it.copy(isControlsVisible = false) }
                }
            }
        }
    }

    private fun persistCurrentPosition() {
        val uri = _uiState.value.videoUri
        val pos = _uiState.value.currentPositionMs
        val isEnded = _uiState.value.isEnded
        if (uri.isNotBlank() && pos > 0L) {
            viewModelScope.launch(dispatcher) {
                mediaRepository.savePlaybackPosition(uri, pos, isEnded)
            }
        }
    }

    private fun extractTitleFromUri(uri: String): String {
        return try {
            val path = File(uri).name
            if (path.isNotBlank()) path else uri
        } catch (_: Throwable) {
            uri
        }
    }

    fun clearForTesting() {
        onCleared()
    }

    override fun onCleared() {
        autoHideJob?.cancel()
        volumeFeedbackJob?.cancel()
        brightnessFeedbackJob?.cancel()
        seekFeedbackJob?.cancel()
        persistCurrentPosition()
        super.onCleared()
    }
}
