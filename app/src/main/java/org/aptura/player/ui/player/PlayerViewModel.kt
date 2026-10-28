package org.aptura.player.ui.player

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aptura.player.core.diagnostics.ApturaErrorHandler
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.repository.AudioEffectsStore
import org.aptura.player.domain.repository.MediaRepository
import org.aptura.player.domain.repository.PlaybackController
import org.aptura.player.data.playback.AudioEnhancementController
import org.aptura.player.domain.model.AbRepeatState
import org.aptura.player.domain.model.AudioEnhancementState
import org.aptura.player.domain.model.EqualizerPreset
import org.aptura.player.domain.model.PictureAdjustmentState
import org.aptura.player.core.media.SubtitleQuerySanitizer
import org.aptura.player.ui.library.MediaFormatters
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.domain.model.DomainExternalSubtitle
import java.io.File

/**
 * ViewModel orchestrating player HUD state, transport controls, and progress persistence.
 * Follows Unidirectional Data Flow (UDF); it holds no Compose, Activity or View types.
 */
class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val mediaRepository: MediaRepository,
    private val audioEnhancementController: AudioEnhancementController = AudioEnhancementController(),
    private val settingsRepository: ApturaSettingsRepository? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val audioEffectsStore: AudioEffectsStore? = settingsRepository,
    /** The default decoder from Settings (0 Auto, 1 Hardware, 2 Software). Videos without a choice of their own follow it. */
    private val defaultDecoderFlow: Flow<Int>? = settingsRepository?.settingsFlow?.map { it.defaultDecoderStrategy },
    /** Monotonic clock for the sleep timer. Tests pass the virtual scheduler's time so deadlines are exact. */
    private val clock: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {

    constructor(
        playbackController: PlaybackController,
        mediaRepository: MediaRepository,
        dispatcher: CoroutineDispatcher
    ) : this(
        playbackController = playbackController,
        mediaRepository = mediaRepository,
        audioEnhancementController = AudioEnhancementController(),
        settingsRepository = null,
        dispatcher = dispatcher
    )

    constructor(
        playbackController: PlaybackController,
        mediaRepository: MediaRepository,
        audioEnhancementController: AudioEnhancementController
    ) : this(
        playbackController = playbackController,
        mediaRepository = mediaRepository,
        audioEnhancementController = audioEnhancementController,
        settingsRepository = null,
        dispatcher = Dispatchers.Main
    )

    /** Q-02: uncaught coroutine failures become local diagnostics events instead of process crashes. */
    private val errorHandler = ApturaErrorHandler.create("player_vm")

    /**
     * Progress saves run here, not in `viewModelScope`: when the ViewModel is cleared Android cancels `viewModelScope`
     * BEFORE it calls `onCleared()`, so a save launched from there never ran and the last stretch of every session
     * was lost (audit P1-15). This scope outlives the ViewModel; each save is a single short database write.
     */
    private val persistScope = CoroutineScope(SupervisorJob() + dispatcher + errorHandler)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var autoHideJob: Job? = null
    private var volumeFeedbackJob: Job? = null
    private var brightnessFeedbackJob: Job? = null
    private var seekFeedbackJob: Job? = null
    private var lockHintJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var audioSyncJob: Job? = null
    private var subtitleSyncJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var bookmarksJob: Job? = null
    private var lastPersistedPositionMs: Long = 0L

    /** URI whose saved per-video preferences were last applied (so they are applied once per item, not per emission). */
    private var lastAppliedPreferencesUri: String? = null
    private var autoAdvanceCountdownSeconds: Int = 10

    /** Global defaults from Settings: used when a video has not overridden its own speed, aspect ratio, or decoder. */
    private var defaultDecoderStrategy: Int = 0
    private var defaultPlaybackSpeed: Float = 1.0f
    private var defaultResizeMode: AspectRatioMode = AspectRatioMode.FIT

    /** Saved playback speed prior to a transient hold-for-2x gesture; null when not holding. */
    private var preHoldPlaybackSpeed: Float? = null

    private var lastSeekTimestampMs: Long = 0L
    private var accumulatedSeekDeltaMs: Long = 0L
    private var accumulatedTargetPosMs: Long? = null
    private var swipeSeekStartPosMs: Long = 0L

    companion object {
        const val AUTO_HIDE_DELAY_MS = 5000L // FR-CTRL-001, Media3 DEFAULT_SHOW_TIMEOUT_MS
        const val FEEDBACK_OVERLAY_DELAY_MS = 1200L
        const val SEEK_ACCUMULATION_THRESHOLD_MS = 800L
        const val CHECKPOINT_INTERVAL_MS = 5000L // TST-REST-01, spec/12A §3.3
        const val SLEEP_FADE_MS = 10_000L
        const val SLEEP_FADE_STEPS = 10
        const val SLEEP_FADE_STEP_MS = SLEEP_FADE_MS / SLEEP_FADE_STEPS
        const val AUDIO_EFFECTS_SAVE_DEBOUNCE_MS = 400L

        /**
         * A/V sync is dragged in 50 ms steps. Applying every step edits the audio stream mid-drag (audible) and
         * writes the row on every frame, so the player is only told once the drag has been still for this long.
         */
        const val SYNC_APPLY_DEBOUNCE_MS = 200L
    }

    init {
        observePlaybackState()
        observePlaybackQueue()
        observeTrackSelection()
        observeAudioEnhancements()
        observeCompatibilityMode()
        observeAutoPairedSubtitles()
        observeSkippedItems()
        observePlaybackSettings()
        observeAudioSession()
        restoreAndPersistAudioEffects()
        observeDefaultDecoder()
    }

    private fun observeDefaultDecoder() {
        val flow = defaultDecoderFlow ?: return
        viewModelScope.launch(dispatcher + errorHandler) {
            flow.distinctUntilChanged().collect { default ->
                defaultDecoderStrategy = default
                // A new default applies to what is playing only if that video has no choice of its own.
                if (!_uiState.value.decoderOverridden) {
                    _uiState.update { it.copy(decoderStrategy = default) }
                    playbackController.setDecoderStrategy(default)
                }
            }
        }
    }

    /** Effects belong to the player's audio session; follow it instead of binding once and hoping it never changes. */
    private fun observeAudioSession() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.audioSessionIds.collect { sessionId ->
                audioEnhancementController.attachToSession(sessionId)
            }
        }
    }

    /** Equalizer / bass / virtualizer / loudness survive restarts: load them first, then save every later change. */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun restoreAndPersistAudioEffects() {
        val store = audioEffectsStore ?: return
        viewModelScope.launch(dispatcher + errorHandler) {
            audioEnhancementController.restore(store.load())
            audioEnhancementController.state
                .map { audioEnhancementController.snapshot() }
                .distinctUntilChanged()
                .drop(1) // the first value is what was just restored
                .debounce(AUDIO_EFFECTS_SAVE_DEBOUNCE_MS)
                .collect { store.save(it) }
        }
    }

    private fun observeSkippedItems() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.skippedItems.collect { title ->
                setAutoPairedSubtitleToast("Skipped \"$title\": file unavailable")
            }
        }
    }

    private fun observeAutoPairedSubtitles() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.autoPairedSubtitles.collect { label ->
                setAutoPairedSubtitleToast("Auto-paired: $label")
            }
        }
    }

    /** Playback-affecting settings: double-tap seek step, default speed/aspect ratio, and the Up Next countdown (0 = advance immediately). */
    private fun observePlaybackSettings() {
        val repository = settingsRepository ?: return
        viewModelScope.launch(dispatcher + errorHandler) {
            repository.settingsFlow.collect { settings ->
                val prevDefaultSpeed = defaultPlaybackSpeed
                val prevDefaultResize = defaultResizeMode
                defaultPlaybackSpeed = settings.defaultPlaybackSpeed
                defaultResizeMode = AspectRatioMode.entries.getOrElse(settings.defaultResizeMode) { AspectRatioMode.FIT }
                autoAdvanceCountdownSeconds = settings.autoAdvanceCountdownSeconds
                _uiState.update {
                    it.copy(
                        seekStepMs = settings.seekDeltaSeconds * 1000L,
                        autoAdvanceTotalSeconds = settings.autoAdvanceCountdownSeconds.coerceAtLeast(1),
                        resizeMode = if (it.resizeMode == prevDefaultResize) defaultResizeMode else it.resizeMode,
                        playbackSpeed = if (it.playbackSpeed == prevDefaultSpeed) defaultPlaybackSpeed else it.playbackSpeed
                    )
                }
                if (_uiState.value.playbackSpeed == defaultPlaybackSpeed && prevDefaultSpeed != defaultPlaybackSpeed) {
                    playbackController.setPlaybackSpeed(defaultPlaybackSpeed)
                }
                playbackController.setPauseAtEndOfItems(settings.autoAdvanceCountdownSeconds > 0)
            }
        }
    }

    private fun observeCompatibilityMode() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.isCompatibilityMode.collect { isCompat ->
                _uiState.update { it.copy(isCompatibilityMode = isCompat) }
            }
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.compatibilityNotice.collect { notice ->
                _uiState.update { it.copy(compatibilityNotice = notice) }
            }
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.decoderStrategy.collect { strategy ->
                _uiState.update { it.copy(decoderStrategy = strategy) }
            }
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.playbackStats.collect { stats ->
                _uiState.update { it.copy(playbackStats = stats) }
            }
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.failedCodecName.collect { codec ->
                _uiState.update { it.copy(failedCodecName = codec) }
            }
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.failureReason.collect { reason ->
                _uiState.update { it.copy(failureReason = reason) }
            }
        }
    }

    private fun observeAudioEnhancements() {
        viewModelScope.launch(dispatcher + errorHandler) {
            audioEnhancementController.state.collect { enhancementState ->
                _uiState.update { it.copy(audioEnhancement = enhancementState) }
            }
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch(dispatcher + errorHandler) {
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
                        val wasPlaying = _uiState.value.isPlaying
                        val startedPlaying = state.isPlaying && !wasPlaying
                        _uiState.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = state.isPlaying,
                                isEnded = false,
                                currentPositionMs = state.positionMs,
                                durationMs = state.durationMs,
                                bufferedPositionMs = state.bufferedPositionMs,
                                playbackSpeed = state.playbackSpeed,
                                errorMessage = null
                            )
                        }
                        if (wasPlaying && !state.isPlaying) {
                            // Paused by the notification, a headset, the sleep timer...: nothing else will save this.
                            lastPersistedPositionMs = state.positionMs
                            persistCurrentPosition()
                        }
                        val ab = _uiState.value.abRepeat
                        if (ab.isLooping && ab.pointA != null && ab.pointB != null &&
                            (state.positionMs >= ab.pointB || state.positionMs < ab.pointA)
                        ) {
                            playbackController.seekTo(ab.pointA)
                        }
                        if (state.isPlaying) {
                            if (Math.abs(state.positionMs - lastPersistedPositionMs) >= CHECKPOINT_INTERVAL_MS) {
                                lastPersistedPositionMs = state.positionMs
                                persistCurrentPosition()
                            }
                            // Only (re)arm the 5 s timer when playback *starts*. The controller emits a new Ready
                            // state every 250 ms; rearming here on every tick would cancel the timer forever.
                            // Every user interaction re-arms it explicitly (tap, seek, sheet close, ...).
                            if (startedPlaying && _uiState.value.isControlsVisible) {
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
                        if (_uiState.value.sleepAtEndOfVideo) {
                            // The sleep timer's "end of this video": stop here, don't count down to the next one.
                            clearStopAtEndOfVideo()
                        } else {
                            handleAutoAdvanceOnEnded()
                        }
                    }
                    is DomainPlaybackState.Error -> {
                        _uiState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                errorMessage = state.message,
                                errorCode = state.errorCode,
                                errorTaxonomyCode = state.errorCode,
                                canSwitchToSoftware = state.errorCode in listOf("DECODER_FAILURE", "UNSUPPORTED_PROFILE", "UNKNOWN"),
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
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.playbackQueue.collect { queue ->
                val currentItem = queue.currentItem
                val sanitized = currentItem?.let { SubtitleQuerySanitizer.sanitize(it.title) }
                if (currentItem != null && currentItem.uri != _uiState.value.videoUri) {
                    // Leaving a video: keep where it stopped BEFORE the state below switches to the next one, so its
                    // position is neither lost nor filed under the new video.
                    persistCurrentPosition()
                    lastPersistedPositionMs = 0L
                }
                _uiState.update {
                    it.copy(
                        queueItems = queue.items,
                        currentQueueIndex = queue.currentIndex,
                        playMode = queue.playMode,
                        hasNext = queue.hasNext,
                        hasPrevious = queue.hasPrevious,
                        videoUri = currentItem?.uri ?: it.videoUri,
                        videoTitle = currentItem?.let { item -> MediaFormatters.cleanTitle(item.title) } ?: it.videoTitle,
                        durationMs = if (currentItem != null && currentItem.durationMs > 0) currentItem.durationMs else it.durationMs,
                        cleanSearchQuery = sanitized?.searchQuery ?: it.cleanSearchQuery,
                        sanitizedTitle = sanitized?.displayTitle ?: it.sanitizedTitle
                    )
                }
                if (currentItem != null && currentItem.uri.isNotBlank()) {
                    observeBookmarks(currentItem.uri)
                    if (currentItem.uri != lastAppliedPreferencesUri) {
                        // Auto-advance, Next/Previous and queue jumps arrive here: restore that video's own settings.
                        lastAppliedPreferencesUri = currentItem.uri
                        applyItemPreferences(currentItem, resume = true)
                    }
                }
            }
        }
    }

    /**
     * Restores a video's saved preferences (aspect, speed, A/V delays, decoder) and, when [resume] is set,
     * its saved position with a 2 s pre-roll. Also clears state that must not leak between videos (A-B loop).
     */
    private suspend fun applyItemPreferences(item: DomainMediaItem, resume: Boolean) {
        preHoldPlaybackSpeed = null
        // A debounced edit from the video we just left must not land on this one.
        audioSyncJob?.cancel()
        subtitleSyncJob?.cancel()
        val restoredResizeMode = if (item.resizeMode == "FIT") {
            defaultResizeMode
        } else {
            try {
                AspectRatioMode.valueOf(item.resizeMode)
            } catch (_: Throwable) {
                defaultResizeMode
            }
        }
        val restoredSpeed = when {
            item.playbackSpeed <= 0f -> defaultPlaybackSpeed
            item.playbackSpeed == 1.0f -> defaultPlaybackSpeed
            else -> item.playbackSpeed
        }
        val ownDecoder = item.decoderStrategy
        val effectiveDecoder = ownDecoder ?: defaultDecoderStrategy
        _uiState.update {
            it.copy(
                resizeMode = restoredResizeMode,
                playbackSpeed = restoredSpeed,
                decoderStrategy = effectiveDecoder,
                decoderOverridden = ownDecoder != null,
                audioDelayMs = item.audioDelayMs,
                subtitleDelayMs = item.subtitleDelayMs,
                abRepeat = AbRepeatState(),
                isEnded = false
            )
        }
        playbackController.setDecoderStrategy(effectiveDecoder)
        playbackController.setPlaybackSpeed(restoredSpeed)
        playbackController.setAudioDelay(item.audioDelayMs)
        playbackController.setSubtitleDelay(item.subtitleDelayMs)
        if (resume && item.playbackPositionMs > 3000L && !item.isCompleted) {
            playbackController.seekTo((item.playbackPositionMs - 2000L).coerceAtLeast(0L))
        }
    }

    private fun observeTrackSelection() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.trackSelection.collect { selection ->
                _uiState.update {
                    it.copy(
                        playbackSpeed = selection.playbackSpeed,
                        audioTracks = selection.availableAudioTracks.map { track ->
                            TrackInfo(
                                id = track.id,
                                index = track.index,
                                label = track.label,
                                language = track.language,
                                isSelected = track.isSelected
                            )
                        },
                        subtitleTracks = selection.availableSubtitleTracks.map { track ->
                            TrackInfo(
                                id = track.id,
                                index = track.index,
                                label = track.label,
                                language = track.language,
                                isSelected = track.isSelected,
                                isExternal = track.isExternal
                            )
                        },
                        selectedAudioTrackIndex = selection.selectedAudioTrackIndex,
                        selectedSubtitleTrackIndex = selection.selectedSubtitleTrackIndex
                        // The A/V delays are deliberately not copied back: the ViewModel owns them, and the player
                        // only hears about a drag after the debounce. Reading the old value back here would snap
                        // the label back mid-drag whenever the controller emitted for any other reason.
                    )
                }
            }
        }
    }

    fun playQueue(items: List<DomainMediaItem>, startIndex: Int = 0, audioOnly: Boolean = false) {
        autoAdvanceJob?.cancel()
        val targetItem = if (items.isNotEmpty()) items[startIndex.coerceIn(0, items.size - 1)] else null
        if (targetItem != null && _uiState.value.videoUri != targetItem.uri) {
            persistCurrentPosition()
            lastPersistedPositionMs = 0L
        }
        _uiState.update {
            it.copy(
                videoUri = targetItem?.uri ?: it.videoUri,
                isAudioOnly = audioOnly,
                autoAdvanceCountdownSeconds = null,
                nextItemTitle = null
            )
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            startQueue(items, startIndex, audioOnly)
        }
    }

    /** Turns audio-only mode on/off: stops decoding/rendering video (saves battery) while audio keeps playing. */
    fun setAudioOnly(enabled: Boolean) {
        _uiState.update { it.copy(isAudioOnly = enabled, isControlsVisible = true) }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.setVideoEnabled(!enabled)
        }
        if (enabled) autoHideJob?.cancel() else scheduleAutoHide()
    }

    private suspend fun startQueue(items: List<DomainMediaItem>, startIndex: Int, audioOnly: Boolean = false) {
        if (items.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, items.size - 1)
        val currentItem = items[clampedIndex]
        val sanitized = SubtitleQuerySanitizer.sanitize(currentItem.title)

        if (_uiState.value.videoUri != currentItem.uri) {
            persistCurrentPosition() // the video being replaced keeps where it stopped
            lastPersistedPositionMs = 0L
        }
        _uiState.update {
            it.copy(
                videoUri = currentItem.uri,
                videoTitle = MediaFormatters.cleanTitle(currentItem.title),
                durationMs = currentItem.durationMs,
                cleanSearchQuery = sanitized.searchQuery,
                sanitizedTitle = sanitized.displayTitle,
                isControlsVisible = true
            )
        }
        // Mark before starting so the queue emission below doesn't re-apply the same preferences.
        lastAppliedPreferencesUri = currentItem.uri
        _uiState.update { it.copy(isAudioOnly = audioOnly) }
        playbackController.setVideoEnabled(!audioOnly)
        playbackController.playQueue(items, clampedIndex)
        applyItemPreferences(currentItem, resume = true)
        observeBookmarks(currentItem.uri)
        scheduleAutoHide()
    }

    fun skipToNext() {
        autoAdvanceJob?.cancel()
        _uiState.update { it.copy(autoAdvanceCountdownSeconds = null, nextItemTitle = null) }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.skipToNext()
            scheduleAutoHide()
        }
    }

    fun skipToPrevious() {
        autoAdvanceJob?.cancel()
        _uiState.update { it.copy(autoAdvanceCountdownSeconds = null, nextItemTitle = null) }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.skipToPrevious()
            scheduleAutoHide()
        }
    }

    private fun handleAutoAdvanceOnEnded() {
        autoAdvanceJob?.cancel()
        val queue = playbackController.playbackQueue.value
        if (queue.hasNext) {
            val nextIndex = queue.nextIndex ?: when (queue.playMode) {
                PlayMode.REPEAT_ALL -> (queue.currentIndex + 1) % queue.items.size
                else -> queue.currentIndex + 1
            }
            val nextItem = if (nextIndex in queue.items.indices) queue.items[nextIndex] else null
            val nextTitle = nextItem?.title?.let { MediaFormatters.cleanTitle(it) } ?: "Next Video"
            val nextDuration = nextItem?.durationMs
            val nextUri = nextItem?.uri

            autoAdvanceJob = viewModelScope.launch(dispatcher + errorHandler) {
                val totalSeconds = autoAdvanceCountdownSeconds.coerceAtLeast(1)
                for (s in totalSeconds downTo 1) {
                    _uiState.update {
                        it.copy(
                            autoAdvanceCountdownSeconds = s,
                            autoAdvanceTotalSeconds = totalSeconds,
                            nextItemTitle = nextTitle,
                            nextItemDurationMs = nextDuration,
                            nextItemUri = nextUri
                        )
                    }
                    delay(1000L)
                }
                _uiState.update {
                    it.copy(
                        autoAdvanceCountdownSeconds = null,
                        nextItemTitle = null,
                        nextItemDurationMs = null,
                        nextItemUri = null
                    )
                }
                skipToNext()
            }
        }
    }

    fun cancelAutoAdvance() {
        autoAdvanceJob?.cancel()
        _uiState.update {
            it.copy(
                autoAdvanceCountdownSeconds = null,
                nextItemTitle = null,
                nextItemDurationMs = null,
                nextItemUri = null
            )
        }
    }

    fun playNextImmediately() {
        autoAdvanceJob?.cancel()
        _uiState.update {
            it.copy(
                autoAdvanceCountdownSeconds = null,
                nextItemTitle = null,
                nextItemDurationMs = null,
                nextItemUri = null
            )
        }
        skipToNext()
    }

    fun replayCurrentItem() {
        autoAdvanceJob?.cancel()
        _uiState.update {
            it.copy(
                autoAdvanceCountdownSeconds = null,
                nextItemTitle = null,
                nextItemDurationMs = null,
                nextItemUri = null,
                isEnded = false
            )
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.seekTo(0L)
            playbackController.play()
            scheduleAutoHide()
        }
    }

    fun setSheet(sheet: PlayerSheet?) {
        _uiState.update { it.copy(activeSheet = sheet) }
        if (sheet != null) autoHideJob?.cancel() else scheduleAutoHide()
    }

    fun closeAllSheets() {
        setSheet(null)
    }

    fun showQueueSheet(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.Queue else null)
    }

    fun showPlayModeSheet(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.PlayMode else null)
    }

    fun setPlayMode(mode: PlayMode) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.setPlayMode(mode)
            _uiState.update { it.copy(playMode = mode) }
            scheduleAutoHide()
        }
    }

    fun jumpToQueueItem(index: Int) {
        autoAdvanceJob?.cancel()
        _uiState.update {
            it.copy(
                autoAdvanceCountdownSeconds = null,
                nextItemTitle = null,
                nextItemDurationMs = null,
                nextItemUri = null,
                activeSheet = if (it.activeSheet == PlayerSheet.Queue) null else it.activeSheet
            )
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.playQueueItem(index)
            scheduleAutoHide()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.moveQueueItem(fromIndex, toIndex)
        }
    }

    fun removeFromQueue(index: Int) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.removeFromQueue(index)
        }
    }

    fun clearQueue() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.clearQueue()
        }
    }

    fun addToQueue(item: DomainMediaItem) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.addToQueue(item)
        }
    }

    fun playNext(item: DomainMediaItem) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.playNext(item)
        }
    }

    fun loadAndPlay(videoUri: String) {
        viewModelScope.launch(dispatcher + errorHandler) {
            val item = mediaRepository.getVideoByUri(videoUri)
            val domainItem = item ?: DomainMediaItem(
                uri = videoUri,
                title = MediaFormatters.cleanTitle(extractTitleFromUri(videoUri)),
                durationMs = 0L,
                sizeBytes = 0L,
                mimeType = "video/*"
            )
            startQueue(listOf(domainItem), 0)
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch(dispatcher + errorHandler) {
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

    fun seekRelative(deltaMs: Long, side: SeekTapSide? = null) {
        viewModelScope.launch(dispatcher + errorHandler) {
            val now = System.currentTimeMillis()
            val isSameDirection = (deltaMs > 0 && accumulatedSeekDeltaMs > 0) ||
                    (deltaMs < 0 && accumulatedSeekDeltaMs < 0)

            val baseTarget = if (now - lastSeekTimestampMs < SEEK_ACCUMULATION_THRESHOLD_MS && isSameDirection && accumulatedTargetPosMs != null) {
                accumulatedSeekDeltaMs += deltaMs
                accumulatedTargetPosMs!!
            } else {
                accumulatedSeekDeltaMs = deltaMs
                _uiState.value.currentPositionMs
            }
            lastSeekTimestampMs = now

            val duration = _uiState.value.durationMs
            val targetPos = (baseTarget + deltaMs).coerceIn(0L, duration.coerceAtLeast(0L))
            accumulatedTargetPosMs = targetPos

            playbackController.seekTo(targetPos)

            val assignedSide = side ?: if (deltaMs < 0) SeekTapSide.LEFT else SeekTapSide.RIGHT

            _uiState.update {
                it.copy(
                    seekDeltaSeconds = (accumulatedSeekDeltaMs / 1000).toInt(),
                    doubleTapSeekSide = assignedSide
                )
            }

            seekFeedbackJob?.cancel()
            seekFeedbackJob = viewModelScope.launch(dispatcher + errorHandler) {
                delay(FEEDBACK_OVERLAY_DELAY_MS)
                _uiState.update {
                    it.copy(
                        seekDeltaSeconds = null,
                        doubleTapSeekSide = null
                    )
                }
                accumulatedSeekDeltaMs = 0L
                accumulatedTargetPosMs = null
            }

            scheduleAutoHide()
        }
    }

    fun onHorizontalSwipeSeekStart() {
        autoHideJob?.cancel()
        val startPos = _uiState.value.effectivePositionMs
        swipeSeekStartPosMs = startPos
        _uiState.update {
            it.copy(
                swipeSeekTargetMs = startPos,
                swipeSeekDeltaMs = 0L
            )
        }
    }

    fun onHorizontalSwipeSeek(deltaMs: Long) {
        val duration = _uiState.value.durationMs.coerceAtLeast(0L)
        val targetPos = (swipeSeekStartPosMs + deltaMs).coerceIn(0L, duration)
        val effectiveDelta = targetPos - swipeSeekStartPosMs
        _uiState.update {
            it.copy(
                swipeSeekTargetMs = targetPos,
                swipeSeekDeltaMs = effectiveDelta
            )
        }
    }

    fun onHorizontalSwipeSeekCommit() {
        val targetMs = _uiState.value.swipeSeekTargetMs ?: swipeSeekStartPosMs
        _uiState.update {
            it.copy(
                swipeSeekTargetMs = null,
                swipeSeekDeltaMs = null
            )
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.seekTo(targetMs)
            scheduleAutoHide()
        }
    }

    fun onHorizontalSwipeSeekCancel() {
        _uiState.update {
            it.copy(
                swipeSeekTargetMs = null,
                swipeSeekDeltaMs = null
            )
        }
        scheduleAutoHide()
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
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.seekTo(targetMs)
            scheduleAutoHide()
        }
    }

    fun triggerLockHint() {
        _uiState.update { it.copy(isLockHintVisible = true) }
        lockHintJob?.cancel()
        lockHintJob = viewModelScope.launch(dispatcher + errorHandler) {
            delay(1200L)
            _uiState.update { it.copy(isLockHintVisible = false) }
        }
    }

    fun toggleControlsVisibility() {
        if (_uiState.value.isLocked) {
            triggerLockHint()
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
        preHoldPlaybackSpeed = null
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.setPlaybackSpeed(speed)
            _uiState.update {
                it.copy(
                    playbackSpeed = speed,
                    activeSheet = if (it.activeSheet == PlayerSheet.Speed) null else it.activeSheet
                )
            }
            persistPlaybackPreferences()
            scheduleAutoHide()
        }
    }

    /** Transiently boosts speed while the user holds the screen (FR-CTRL-004); never writes to Room DB. */
    fun startHoldSpeedBoost(boostSpeed: Float = 2.0f) {
        if (preHoldPlaybackSpeed == null) {
            preHoldPlaybackSpeed = _uiState.value.playbackSpeed
        }
        _uiState.update { it.copy(playbackSpeed = boostSpeed) }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.setPlaybackSpeed(boostSpeed)
        }
    }

    /** Restores the exact pre-hold playback speed when the user releases or cancels a hold-for-2x gesture. */
    fun endHoldSpeedBoost() {
        val restoreSpeed = preHoldPlaybackSpeed ?: return
        preHoldPlaybackSpeed = null
        _uiState.update { it.copy(playbackSpeed = restoreSpeed) }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.setPlaybackSpeed(restoreSpeed)
            scheduleAutoHide()
        }
    }

    fun toggleResizeMode() {
        _uiState.update {
            it.copy(resizeMode = it.resizeMode.next())
        }
        persistPlaybackPreferences()
        scheduleAutoHide()
    }

    fun setResizeMode(mode: AspectRatioMode) {
        _uiState.update {
            it.copy(resizeMode = mode)
        }
        persistPlaybackPreferences()
        scheduleAutoHide()
    }

    fun togglePlayMode() {
        val nextMode = when (_uiState.value.playMode) {
            PlayMode.NORMAL -> PlayMode.REPEAT_ALL
            PlayMode.REPEAT_ALL -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.NORMAL
        }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.setPlayMode(nextMode)
            _uiState.update { it.copy(playMode = nextMode) }
            scheduleAutoHide()
        }
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _uiState.update { it.copy(volumePercent = clamped) }
        volumeFeedbackJob?.cancel()
        volumeFeedbackJob = viewModelScope.launch(dispatcher + errorHandler) {
            delay(FEEDBACK_OVERLAY_DELAY_MS)
            _uiState.update { it.copy(volumePercent = null) }
        }
    }

    fun setBrightnessPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _uiState.update { it.copy(brightnessPercent = clamped) }
        brightnessFeedbackJob?.cancel()
        brightnessFeedbackJob = viewModelScope.launch(dispatcher + errorHandler) {
            delay(FEEDBACK_OVERLAY_DELAY_MS)
            _uiState.update { it.copy(brightnessPercent = null) }
        }
    }

    fun setAudioSheetVisible(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.Audio else null)
    }

    fun setSubtitleSheetVisible(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.Subtitle else null)
    }

    fun setSpeedSheetVisible(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.Speed else null)
    }

    fun selectAudioTrack(index: Int?) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.selectAudioTrack(index)
            setAudioSheetVisible(false)
        }
    }

    fun selectSubtitleTrack(index: Int?) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.selectSubtitleTrack(index)
            setSubtitleSheetVisible(false)
        }
    }

    fun retryPlayback() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                errorCode = null,
                errorTaxonomyCode = null,
                canSwitchToSoftware = false
            )
        }
        val currentUri = _uiState.value.videoUri
        if (currentUri.isNotBlank()) {
            loadAndPlay(currentUri)
        } else {
            viewModelScope.launch(dispatcher + errorHandler) {
                playbackController.retryPlayback()
            }
        }
    }

    /**
     * Sync offsets are dragged in 50 ms steps. The value is shown immediately, but the player is only told after the
     * drag has been still for [SYNC_APPLY_DEBOUNCE_MS]: an audio offset inserts or drops PCM, so applying every step
     * garbled the sound, and every step also wrote the row and refreshed the playback stats.
     */
    fun setSubtitleDelay(delayMs: Long) {
        _uiState.update { it.copy(subtitleDelayMs = delayMs) }
        subtitleSyncJob?.cancel()
        subtitleSyncJob = viewModelScope.launch(dispatcher + errorHandler) {
            delay(SYNC_APPLY_DEBOUNCE_MS)
            playbackController.setSubtitleDelay(delayMs)
            persistPlaybackPreferences()
        }
    }

    fun setAudioDelay(delayMs: Long) {
        _uiState.update { it.copy(audioDelayMs = delayMs) }
        audioSyncJob?.cancel()
        audioSyncJob = viewModelScope.launch(dispatcher + errorHandler) {
            delay(SYNC_APPLY_DEBOUNCE_MS)
            playbackController.setAudioDelay(delayMs)
            persistPlaybackPreferences()
        }
    }

    fun addExternalSubtitle(
        uri: String,
        mimeType: String = when {
            uri.endsWith(".vtt", ignoreCase = true) -> "text/vtt"
            uri.endsWith(".ass", ignoreCase = true) || uri.endsWith(".ssa", ignoreCase = true) -> "text/x-ssa"
            else -> "application/x-subrip"
        },
        label: String = try {
            val name = File(uri).name
            if (name.isNotBlank()) name else "External Subtitle"
        } catch (_: Throwable) {
            "External Subtitle"
        }
    ) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.addExternalSubtitle(uri, mimeType, label)
        }
    }

    fun addExternalSubtitles(subtitles: List<DomainExternalSubtitle>) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.addExternalSubtitles(subtitles)
        }
    }

    fun removeExternalSubtitles() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.removeExternalSubtitles()
        }
    }

    fun removeExternalSubtitle(trackLabel: String) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.removeExternalSubtitle(trackLabel)
        }
    }

    fun setSubtitleFontSize(sizeSp: Int) {
        _uiState.update { it.copy(subtitleFontSizeSp = sizeSp) }
        viewModelScope.launch(dispatcher + errorHandler) {
            settingsRepository?.updateSubtitleFontSize(sizeSp)
        }
    }

    fun setSubtitleTextColor(colorHex: String) {
        _uiState.update { it.copy(subtitleTextColorHex = colorHex) }
        viewModelScope.launch(dispatcher + errorHandler) {
            settingsRepository?.updateSubtitleTextColor(colorHex)
        }
    }

    fun setSubtitleBackgroundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(subtitleBackgroundEnabled = enabled) }
        viewModelScope.launch(dispatcher + errorHandler) {
            settingsRepository?.updateSubtitleBackground(enabled)
        }
    }

    fun setCleanSearchQuery(query: String) {
        _uiState.update { it.copy(cleanSearchQuery = query) }
    }

    fun setRecentDownloadedSubtitle(subtitle: org.aptura.player.data.repository.DetectedSidecarSubtitle?) {
        _uiState.update { it.copy(recentDownloadedSubtitle = subtitle) }
    }

    fun showWebSearchDialog(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.WebSearch else null)
    }

    fun setAutoPairedSubtitleToast(message: String?) {
        _uiState.update { it.copy(autoPairedSubtitleToast = message) }
    }

    fun dismissAutoPairToast() {
        _uiState.update { it.copy(autoPairedSubtitleToast = null) }
    }

    private fun observeBookmarks(uri: String) {
        bookmarksJob?.cancel()
        bookmarksJob = viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.getBookmarks(uri).collect { list ->
                _uiState.update { it.copy(bookmarks = list) }
            }
        }
    }

    fun addBookmark(note: String = "") {
        val uri = _uiState.value.videoUri
        val pos = _uiState.value.currentPositionMs
        if (uri.isNotBlank()) {
            viewModelScope.launch(dispatcher + errorHandler) {
                mediaRepository.addBookmark(uri, pos, note)
            }
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.deleteBookmark(id)
        }
    }

    fun updateBookmarkNote(id: Long, note: String) {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.updateBookmarkNote(id, note)
        }
    }

    /** The user picked a decoder in the player: that is THIS video's choice and never changes the Settings default. */
    fun setDecoderStrategy(strategy: Int) {
        _uiState.update { it.copy(decoderStrategy = strategy, decoderOverridden = true) }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.setDecoderStrategy(strategy)
            persistPlaybackPreferences()
        }
    }

    fun toggleStatsOverlay(visible: Boolean? = null) {
        _uiState.update {
            it.copy(isStatsOverlayVisible = visible ?: !it.isStatsOverlayVisible)
        }
    }

    fun setWhyCompatibilitySheetVisible(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.WhyCompatibility else null)
    }

    fun resetVideoPreferences() {
        val uri = _uiState.value.videoUri
        if (uri.isNotBlank()) {
            preHoldPlaybackSpeed = null
            // The reset below sets the offsets to 0; a pending drag must not put them back afterwards.
            audioSyncJob?.cancel()
            subtitleSyncJob?.cancel()
            viewModelScope.launch(dispatcher + errorHandler) {
                mediaRepository.resetPlaybackPreferences(uri)
                playbackController.setPlaybackSpeed(defaultPlaybackSpeed)
                playbackController.setDecoderStrategy(defaultDecoderStrategy)
                playbackController.setAudioDelay(0L)
                playbackController.setSubtitleDelay(0L)
                _uiState.update {
                    it.copy(
                        resizeMode = defaultResizeMode,
                        playbackSpeed = defaultPlaybackSpeed,
                        decoderStrategy = defaultDecoderStrategy,
                        decoderOverridden = false,
                        audioDelayMs = 0L,
                        subtitleDelayMs = 0L
                    )
                }
            }
        }
    }

    private fun persistPlaybackPreferences() {
        val uri = _uiState.value.videoUri
        if (uri.isBlank()) return
        val state = _uiState.value
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.savePlaybackPreferences(
                uri = uri,
                resizeMode = state.resizeMode.name,
                playbackSpeed = state.playbackSpeed,
                // Only a decoder the user chose is saved. The automatic software fallback and the Settings default
                // are not this video's choice, and must not be frozen into it.
                decoderStrategy = if (state.decoderOverridden) state.decoderStrategy else null,
                audioDelayMs = state.audioDelayMs,
                subtitleDelayMs = state.subtitleDelayMs
            )
        }
    }



    fun dismissCompatibilityNotice() {
        _uiState.update { it.copy(compatibilityNotice = null) }
    }

    fun startSleepTimer(minutes: Int) {
        scheduleSleepTimer(clock() + minutes * 60_000L)
    }

    /** "Add 5 min" on a running timer: moves the deadline instead of restarting from a fixed choice. */
    fun extendSleepTimer(minutes: Int) {
        val endsAt = _uiState.value.sleepTimerEndsAtElapsedRealtime ?: return
        scheduleSleepTimer(endsAt + minutes * 60_000L)
    }

    /**
     * The deadline, not a running total, is the source of truth. `delay` guarantees only a minimum, so subtracting
     * each tick let every late wake-up shorten the sleep by that much. Here the pause is one deadline and the fade
     * paces itself against the same deadline; nothing ticks for the UI, which derives the countdown from
     * [PlayerUiState.sleepTimerEndsAtElapsedRealtime] while it is on screen.
     */
    private fun scheduleSleepTimer(endsAt: Long) {
        sleepTimerJob?.cancel()
        clearStopAtEndOfVideo()
        _uiState.update { it.copy(sleepTimerEndsAtElapsedRealtime = endsAt) }
        sleepTimerJob = viewModelScope.launch(dispatcher + errorHandler) {
            try {
                val fadeStart = endsAt - SLEEP_FADE_MS
                val untilFade = fadeStart - clock()
                if (untilFade > 0L) delay(untilFade)
                for (step in 1..SLEEP_FADE_STEPS) {
                    // Re-anchor every step to the deadline so a late wake-up is caught up, not added on.
                    val wait = fadeStart + step * SLEEP_FADE_STEP_MS - clock()
                    if (wait > 0L) delay(wait)
                    playbackController.setVolume(1f - step / SLEEP_FADE_STEPS.toFloat())
                }
                playbackController.pause()
                persistCurrentPosition()
                _uiState.update { it.copy(sleepTimerEndsAtElapsedRealtime = null) }
            } finally {
                // Paused, cancelled or extended: the next play starts at full volume.
                withContext(NonCancellable) { playbackController.setVolume(1f) }
            }
        }
    }

    /** Pause when this video ends; Up Next does not start. Replaces a timed countdown. */
    fun startSleepAtEndOfVideo() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _uiState.update { it.copy(sleepTimerEndsAtElapsedRealtime = null, sleepAtEndOfVideo = true) }
        viewModelScope.launch(dispatcher + errorHandler) { playbackController.setStopAfterCurrentItem(true) }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        clearStopAtEndOfVideo()
        _uiState.update { it.copy(sleepTimerEndsAtElapsedRealtime = null) }
    }

    private fun clearStopAtEndOfVideo() {
        if (!_uiState.value.sleepAtEndOfVideo) return
        _uiState.update { it.copy(sleepAtEndOfVideo = false) }
        viewModelScope.launch(dispatcher + errorHandler) { playbackController.setStopAfterCurrentItem(false) }
    }

    fun setAdvancedSheetVisible(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.Advanced else null)
    }

    fun setDelayStep(stepMs: Long) {
        _uiState.update { it.copy(delayStepMs = stepMs) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        audioEnhancementController.setEnabled(enabled)
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        audioEnhancementController.setPreset(preset)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBels: Int) {
        audioEnhancementController.setBandLevel(bandIndex, levelMilliBels)
    }

    fun setBassBoost(percent: Int) {
        audioEnhancementController.setBassBoost(percent)
    }

    fun setVirtualizer(percent: Int) {
        audioEnhancementController.setVirtualizer(percent)
    }

    fun setLoudnessBoost(percent: Int) {
        audioEnhancementController.setLoudnessBoost(percent)
    }

    fun setContrast(contrast: Float) {
        _uiState.update {
            it.copy(pictureAdjustment = it.pictureAdjustment.copy(contrast = contrast.coerceIn(0.5f, 1.5f)))
        }
    }

    fun setSaturation(saturation: Float) {
        _uiState.update {
            it.copy(pictureAdjustment = it.pictureAdjustment.copy(saturation = saturation.coerceIn(0.0f, 2.0f)))
        }
    }

    fun setNightMode(enabled: Boolean, warmth: Float = 0.4f) {
        _uiState.update {
            it.copy(
                pictureAdjustment = it.pictureAdjustment.copy(
                    isNightModeEnabled = enabled,
                    nightModeWarmth = warmth.coerceIn(0.1f, 0.8f)
                )
            )
        }
    }

    fun resetPictureAdjustments() {
        _uiState.update {
            it.copy(pictureAdjustment = PictureAdjustmentState())
        }
    }

    fun setAbPointA() {
        val current = _uiState.value.currentPositionMs
        _uiState.update {
            val nextPointB = if (it.abRepeat.pointB != null && it.abRepeat.pointB <= current) null else it.abRepeat.pointB
            it.copy(
                abRepeat = it.abRepeat.copy(
                    pointA = current,
                    pointB = nextPointB,
                    isLooping = nextPointB != null
                )
            )
        }
    }

    fun setAbPointB() {
        val current = _uiState.value.currentPositionMs
        val pointA = _uiState.value.abRepeat.pointA
        if (pointA != null && current > pointA) {
            _uiState.update {
                it.copy(
                    abRepeat = it.abRepeat.copy(
                        pointB = current,
                        isLooping = true
                    )
                )
            }
        }
    }

    fun clearAbRepeat() {
        _uiState.update {
            it.copy(abRepeat = AbRepeatState())
        }
    }

    fun stepFrame(forward: Boolean) {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.stepFrame(forward)
        }
    }

    fun setEqualizerSheetVisible(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.Equalizer else null)
    }

    fun setPictureSheetVisible(visible: Boolean) {
        setSheet(if (visible) PlayerSheet.Picture else null)
    }

    private fun scheduleAutoHide() {
        autoHideJob?.cancel()
        if (_uiState.value.isPlaying &&
            !_uiState.value.isScrubbing &&
            !_uiState.value.isAnySheetVisible &&
            _uiState.value.errorMessage == null
        ) {
            autoHideJob = viewModelScope.launch(dispatcher + errorHandler) {
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
            // Captured above, written from a scope that survives the ViewModel (see persistScope).
            persistScope.launch {
                mediaRepository.savePlaybackPosition(uri, pos, isEnded)
            }
        }
    }

    /** Saves progress without touching playback; called when the app goes to the background (ON_STOP). */
    fun persistProgress() {
        persistCurrentPosition()
    }

    internal fun extractTitleFromUri(uri: String): String {
        return try {
            val decoded = java.net.URLDecoder.decode(uri, "UTF-8")
            val segment = decoded
                .substringBefore('?')
                .substringBefore('#')
                .trimEnd('/')
                .substringAfterLast('/')
                .substringAfterLast(':')
            if (segment.isNotBlank()) segment else File(uri).name.ifBlank { uri }
        } catch (_: Throwable) {
            try {
                File(uri).name.ifBlank { uri }
            } catch (_: Throwable) {
                uri
            }
        }
    }

    fun checkpointPlayback() {
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.pause()
            persistCurrentPosition()
        }
    }

    fun onPlayerDismissed() {
        autoAdvanceJob?.cancel()
        _uiState.update { it.copy(autoAdvanceCountdownSeconds = null, nextItemTitle = null) }
        viewModelScope.launch(dispatcher + errorHandler) {
            playbackController.pause()
            persistCurrentPosition()
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
        lockHintJob?.cancel()
        sleepTimerJob?.cancel()
        if (_uiState.value.sleepAtEndOfVideo) {
            // The controller outlives this screen; don't leave it stopping after every item.
            persistScope.launch { playbackController.setStopAfterCurrentItem(false) }
        }
        autoAdvanceJob?.cancel()
        bookmarksJob?.cancel()
        persistCurrentPosition()
        // Free the system's (limited) effect instances, and keep the last tweak even if it is inside the debounce window.
        audioEffectsStore?.let { store ->
            val effects = audioEnhancementController.snapshot()
            persistScope.launch { store.save(effects) }
        }
        audioEnhancementController.releaseEffects()
        super.onCleared()
    }
}
