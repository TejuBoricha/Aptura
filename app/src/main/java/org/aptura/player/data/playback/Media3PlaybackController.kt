@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import org.aptura.player.domain.model.PlaybackStatsInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.ErrorTaxonomy
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.DomainTrackInfo
import org.aptura.player.domain.model.DomainTrackSelection
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.repository.PlaybackController
import org.aptura.player.domain.usecase.PlaybackNavigationRules
import org.aptura.player.domain.usecase.PreviousActionOutcome

/**
 * Concrete implementation of domain PlaybackController backed by Media3 Player.
 * Maps low-level ExoPlayer/MediaController events into immutable domain states
 * and enforces platform navigation rules (FR-CTRL-002, W8, W9).
 */
class Media3PlaybackController(
    val player: Player,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val enablePositionTicker: Boolean = true,
    initialDecoderStrategy: Int = 0,
    var apturaTextRenderer: ApturaTextRenderer? = null
) : PlaybackController {

    var currentDecoderStrategy: Int = initialDecoderStrategy
        private set

    private var hasAttemptedSoftwareFallback = false

    private val _isCompatibilityMode = MutableStateFlow(initialDecoderStrategy == 2)
    override val isCompatibilityMode: StateFlow<Boolean> = _isCompatibilityMode.asStateFlow()

    private val _compatibilityNotice = MutableStateFlow<String?>(
        if (initialDecoderStrategy == 2) "Playing in compatibility mode (Software Decoder)" else null
    )
    override val compatibilityNotice: StateFlow<String?> = _compatibilityNotice.asStateFlow()

    private val _decoderStrategy = MutableStateFlow(initialDecoderStrategy)
    override val decoderStrategy: StateFlow<Int> = _decoderStrategy.asStateFlow()

    private var totalDroppedFrames = 0
    private var renderedVideoFrames = 0
    private var currentVideoDecoderName = "Auto"
    private var currentAudioDecoderName = "Auto"
    private var isVideoHardware = true
    private var currentVideoFormat: androidx.media3.common.Format? = null
    private var currentAudioFormat: androidx.media3.common.Format? = null

    private val _playbackStats = MutableStateFlow(PlaybackStatsInfo())
    override val playbackStats: StateFlow<PlaybackStatsInfo> = _playbackStats.asStateFlow()

    private val _failedCodecName = MutableStateFlow<String?>(
        if (initialDecoderStrategy == 2) "Hardware Decoder (Manual Override)" else null
    )
    override val failedCodecName: StateFlow<String?> = _failedCodecName.asStateFlow()

    private val _failureReason = MutableStateFlow<String?>(
        if (initialDecoderStrategy == 2) "Software decoding explicitly enabled via advanced playback settings." else null
    )
    override val failureReason: StateFlow<String?> = _failureReason.asStateFlow()

    val audioSessionId: Int
        get() = (player as? androidx.media3.exoplayer.ExoPlayer)?.audioSessionId ?: 0

    private val _playbackState = MutableStateFlow<DomainPlaybackState>(DomainPlaybackState.Idle)
    override val playbackState: StateFlow<DomainPlaybackState> = _playbackState.asStateFlow()

    private var positionTickerJob: Job? = null

    private fun checkPositionTicker(isPlaying: Boolean) {
        if (!enablePositionTicker) return
        if (isPlaying) {
            if (positionTickerJob == null || positionTickerJob?.isActive == false) {
                positionTickerJob = scope.launch(mainDispatcher) {
                    while (isActive && player.isPlaying) {
                        delay(250L)
                        updatePlaybackState()
                        updatePlaybackStats()
                    }
                }
            }
        } else {
            positionTickerJob?.cancel()
            positionTickerJob = null
        }
    }

    private val _playbackQueue = MutableStateFlow(DomainPlaybackQueue())
    override val playbackQueue: StateFlow<DomainPlaybackQueue> = _playbackQueue.asStateFlow()

    private val _trackSelection = MutableStateFlow(DomainTrackSelection())
    override val trackSelection: StateFlow<DomainTrackSelection> = _trackSelection.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                checkPositionTicker(false)
            }
            updatePlaybackState()
            updatePlaybackStats()
            if (state == Player.STATE_ENDED) {
                handleAutoAdvance()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            checkPositionTicker(isPlaying)
            updatePlaybackState()
            updatePlaybackStats()
        }

        override fun onPlayerError(error: PlaybackException) {
            checkPositionTicker(false)

            val isDecoderFailure = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES

            // Check if software fallback is viable before attempting:
            // Device platform must actually have a software decoder available for this mime type.
            val mimeType = currentVideoFormat?.sampleMimeType
            val hasSoftwareDecoder = if (!mimeType.isNullOrBlank()) {
                try {
                    androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT
                        .getDecoderInfos(mimeType, false, false)
                        .any { it.softwareOnly || !it.hardwareAccelerated }
                } catch (_: Throwable) {
                    true
                }
            } else {
                true
            }

            // FR-PLAY-003: Controlled fallback bounded at most once per failure class
            if (isDecoderFailure && !hasAttemptedSoftwareFallback && currentDecoderStrategy != 2 && hasSoftwareDecoder) {
                hasAttemptedSoftwareFallback = true
                currentDecoderStrategy = 2
                _decoderStrategy.value = 2
                _isCompatibilityMode.value = true
                _failedCodecName.value = currentVideoDecoderName.ifBlank { "Hardware Decoder" }
                _failureReason.value = "${error.errorCodeName}: Hardware decoder initialization or capability failed for media profile. Automatically engaged software fallback."
                _compatibilityNotice.value = "Playing in compatibility mode (Software Decoder)"
                updatePlaybackStats()

                ApturaDiagnostics.logEvent(
                    code = EventCode.EV_010,
                    payload = mapOf(
                        "strategy" to "software_fallback",
                        "reason" to error.errorCodeName
                    )
                )

                val currentItem = player.currentMediaItem
                val resumePos = player.currentPosition.coerceAtLeast(0L)
                if (currentItem != null) {
                    player.stop()
                    player.clearMediaItems()
                    player.setMediaItem(currentItem, resumePos)
                    player.prepare()
                    player.play()
                    return
                }
            }

            // Fallback unavailable or failed: Log EV_011 (play.error) per spec/14 §4
            val isSoftwareAlreadyAttempted = hasAttemptedSoftwareFallback || currentDecoderStrategy == 2
            val taxonomy = ErrorTaxonomy.mapException(error, isSoftwareAlreadyAttempted)
            ApturaDiagnostics.logEvent(
                code = EventCode.EV_011,
                payload = mapOf(
                    "errorCode" to taxonomy.code,
                    "reason" to error.errorCodeName,
                    "stage" to when {
                        isDecoderFailure -> "decode"
                        error.errorCode in listOf(
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
                        ) -> "prepare"
                        error.errorCode in listOf(
                            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
                            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
                        ) -> "audio"
                        error.errorCode == PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "render"
                        else -> "render"
                    }
                )
            )

            // If surface or runtime check failed, clear the surface to prevent repeated renderer crash loops
            if (error.errorCode == PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK) {
                player.clearVideoSurface()
            }

            updatePlaybackState()
            updatePlaybackStats()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _trackSelection.value = _trackSelection.value.copy(
                playbackSpeed = playbackParameters.speed
            )
            updatePlaybackState()
            updatePlaybackStats()
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTracks(tracks)
            updatePlaybackStats()
        }
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            currentVideoDecoderName = decoderName
            isVideoHardware = !decoderName.startsWith("c2.android.") && !decoderName.startsWith("OMX.google.")
            updatePlaybackStats()
        }

        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            currentAudioDecoderName = decoderName
            updatePlaybackStats()
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            totalDroppedFrames += droppedFrames
            updatePlaybackStats()
        }

        override fun onVideoSizeChanged(
            eventTime: AnalyticsListener.EventTime,
            videoSize: androidx.media3.common.VideoSize
        ) {
            updatePlaybackStats()
        }

        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: androidx.media3.common.Format,
            decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
        ) {
            currentVideoFormat = format
            updatePlaybackStats()
        }

        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: androidx.media3.common.Format,
            decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
        ) {
            currentAudioFormat = format
            updatePlaybackStats()
        }
    }

    init {
        player.addListener(playerListener)
        (player as? ExoPlayer)?.addAnalyticsListener(analyticsListener)
        updatePlaybackState()
        updatePlaybackStats()
        val currentTracks = try { player.currentTracks } catch (_: Throwable) { null } ?: Tracks.EMPTY
        updateTracks(currentTracks)
    }

    private fun updatePlaybackState() {
        val error = player.playerError
        if (error != null) {
            val taxonomyError = ErrorTaxonomy.mapException(error)
            _playbackState.value = DomainPlaybackState.Error(
                errorCode = taxonomyError.code,
                message = "${taxonomyError.title}: ${taxonomyError.whatHappened}",
                isRecoverable = taxonomyError.isRecoverable,
                suggestedAction = taxonomyError.suggestedActions.firstOrNull()
            )
            return
        }

        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        val bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)

        _playbackState.value = when (player.playbackState) {
            Player.STATE_IDLE -> DomainPlaybackState.Idle
            Player.STATE_BUFFERING -> DomainPlaybackState.Buffering(currentPosition)
            Player.STATE_READY -> DomainPlaybackState.Ready(
                positionMs = currentPosition,
                durationMs = duration,
                isPlaying = player.isPlaying,
                playbackSpeed = player.playbackParameters.speed,
                bufferedPositionMs = bufferedPosition
            )
            Player.STATE_ENDED -> DomainPlaybackState.Ended(duration)
            else -> DomainPlaybackState.Idle
        }
        checkPositionTicker(player.isPlaying)
    }

    fun updatePlaybackStats() {
        val exo = player as? ExoPlayer
        val videoFormat = exo?.videoFormat ?: currentVideoFormat
        val audioFormat = exo?.audioFormat ?: currentAudioFormat

        val size = try { player.videoSize } catch (_: Throwable) { null }
        val width = videoFormat?.width?.takeIf { it > 0 } ?: size?.width ?: 0
        val height = videoFormat?.height?.takeIf { it > 0 } ?: size?.height ?: 0
        val frameRate = if (videoFormat != null && videoFormat.frameRate > 0f) videoFormat.frameRate else 0f
        val videoBitrateKbps = if (videoFormat != null && videoFormat.bitrate > 0) videoFormat.bitrate / 1000 else 0

        val audioChannels = when (audioFormat?.channelCount) {
            1 -> "Mono (1 ch)"
            2 -> "Stereo (2 ch)"
            6 -> "5.1 Surround (6 ch)"
            8 -> "7.1 Surround (8 ch)"
            null -> "Stereo (2 ch)"
            else -> "${audioFormat.channelCount} ch"
        }
        val audioSampleRateHz = audioFormat?.sampleRate ?: 48000
        val audioBitrateKbps = if (audioFormat != null && audioFormat.bitrate > 0) audioFormat.bitrate / 1000 else 0

        val hdrFormat = when (videoFormat?.colorInfo?.colorTransfer) {
            C.COLOR_TRANSFER_ST2084 -> "HDR10 (PQ)"
            C.COLOR_TRANSFER_HLG -> "HDR (HLG)"
            else -> "SDR"
        }

        val decoderPath = when {
            currentDecoderStrategy == 2 -> "Software Override"
            !isVideoHardware -> "Software Fallback"
            currentDecoderStrategy == 1 -> "Hardware Forced"
            else -> "Auto (Hardware)"
        }

        val totalFrames = (totalDroppedFrames + renderedVideoFrames).coerceAtLeast(1)
        val dropPct = (totalDroppedFrames.toFloat() / totalFrames.toFloat()) * 100f

        _playbackStats.value = PlaybackStatsInfo(
            decoderPath = decoderPath,
            isHardwareAccelerated = isVideoHardware && currentDecoderStrategy != 2,
            videoDecoderName = currentVideoDecoderName,
            videoMimeType = videoFormat?.sampleMimeType ?: "video/mp4",
            videoCodec = videoFormat?.codecs ?: videoFormat?.sampleMimeType?.substringAfter('/') ?: "",
            videoResolution = if (width > 0 && height > 0) "${width}x${height}" else "",
            frameRate = frameRate,
            videoBitrateKbps = videoBitrateKbps,
            droppedFrames = totalDroppedFrames,
            dropPercentage = dropPct,
            audioDecoderName = currentAudioDecoderName,
            audioMimeType = audioFormat?.sampleMimeType ?: "audio/mp4a-latm",
            audioCodec = audioFormat?.codecs ?: audioFormat?.sampleMimeType?.substringAfter('/') ?: "",
            audioChannels = audioChannels,
            audioSampleRateHz = audioSampleRateHz,
            audioBitrateKbps = audioBitrateKbps,
            audioDelayMs = _trackSelection.value.audioDelayMs,
            subtitleDelayMs = _trackSelection.value.subtitleDelayMs,
            hdrFormat = hdrFormat
        )
    }

    override suspend fun playMediaItem(item: DomainMediaItem) {
        playQueue(listOf(item), 0)
    }

    override suspend fun playQueue(items: List<DomainMediaItem>, startIndex: Int) {
        if (items.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, items.size - 1)
        val targetItem = items[clampedIndex]

        playMediaItemInternal(targetItem)

        _playbackQueue.value = DomainPlaybackQueue(
            items = items,
            currentIndex = clampedIndex,
            playMode = _playbackQueue.value.playMode
        )
    }

    private fun playMediaItemInternal(item: DomainMediaItem, startPositionMs: Long = 0L) {
        externalSubtitleLabels.clear()
        apturaTextRenderer?.subtitleDelayMs = 0L
        _trackSelection.value = _trackSelection.value.copy(subtitleDelayMs = 0L)
        hasAttemptedSoftwareFallback = false
        totalDroppedFrames = 0
        renderedVideoFrames = 0
        _decoderStrategy.value = currentDecoderStrategy
        _isCompatibilityMode.value = (currentDecoderStrategy == 2)
        _compatibilityNotice.value = if (currentDecoderStrategy == 2) "Playing in compatibility mode (Software Decoder)" else null
        if (currentDecoderStrategy == 2) {
            _failedCodecName.value = "Hardware Decoder (Manual Override)"
            _failureReason.value = "Software decoding explicitly selected in playback settings."
        } else {
            _failedCodecName.value = null
            _failureReason.value = null
        }
        updatePlaybackStats()

        val mediaItem = try {
            MediaItem.fromUri(item.uri)
        } catch (_: Throwable) {
            MediaItem.Builder().setMediaId(item.uri).build()
        }
        player.setMediaItem(mediaItem, startPositionMs)
        player.prepare()
        player.play()
    }

    override suspend fun play() {
        player.play()
    }

    override suspend fun pause() {
        player.pause()
    }

    override suspend fun seekTo(positionMs: Long) {
        val duration = player.duration.coerceAtLeast(0L)
        val clampedPosition = PlaybackNavigationRules.clampSeekPosition(positionMs, duration)
        player.seekTo(clampedPosition)
    }

    override suspend fun skipToNext() {
        val queue = _playbackQueue.value
        if (queue.hasNext) {
            val nextIndex = when (queue.playMode) {
                PlayMode.REPEAT_ONE -> queue.currentIndex
                PlayMode.REPEAT_ALL -> if (queue.items.isEmpty()) -1 else (queue.currentIndex + 1) % queue.items.size
                PlayMode.NORMAL, PlayMode.SHUFFLE -> (queue.currentIndex + 1).coerceAtMost(queue.items.size - 1)
            }
            if (nextIndex in queue.items.indices) {
                val nextItem = queue.items[nextIndex]
                _playbackQueue.value = queue.copy(currentIndex = nextIndex)
                playMediaItemInternal(nextItem)
            }
        }
    }

    override suspend fun skipToPrevious() {
        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val queue = _playbackQueue.value

        when (PlaybackNavigationRules.evaluatePreviousAction(currentPosition, queue)) {
            PreviousActionOutcome.RestartCurrentItem -> {
                player.seekTo(0L)
            }
            PreviousActionOutcome.NavigateToPreviousItem -> {
                val prevIndex = when (queue.playMode) {
                    PlayMode.REPEAT_ONE -> queue.currentIndex
                    PlayMode.REPEAT_ALL -> if (queue.items.isEmpty()) -1 else (queue.currentIndex - 1 + queue.items.size) % queue.items.size
                    PlayMode.NORMAL, PlayMode.SHUFFLE -> (queue.currentIndex - 1).coerceAtLeast(0)
                }
                if (prevIndex in queue.items.indices) {
                    val prevItem = queue.items[prevIndex]
                    _playbackQueue.value = queue.copy(currentIndex = prevIndex)
                    playMediaItemInternal(prevItem)
                }
            }
        }
    }

    override suspend fun playQueueItem(index: Int) {
        val queue = _playbackQueue.value
        if (index in queue.items.indices) {
            val item = queue.items[index]
            _playbackQueue.value = queue.copy(currentIndex = index)
            playMediaItemInternal(item)
        }
    }

    override suspend fun addToQueue(item: DomainMediaItem) {
        val queue = _playbackQueue.value
        if (queue.items.isEmpty()) {
            playQueue(listOf(item), 0)
        } else {
            val newItems = queue.items + item
            _playbackQueue.value = queue.copy(items = newItems)
        }
    }

    override suspend fun playNext(item: DomainMediaItem) {
        val queue = _playbackQueue.value
        if (queue.items.isEmpty()) {
            playQueue(listOf(item), 0)
        } else {
            val insertIndex = (queue.currentIndex + 1).coerceAtMost(queue.items.size)
            val newItems = queue.items.toMutableList().apply {
                add(insertIndex, item)
            }
            _playbackQueue.value = queue.copy(items = newItems)
        }
    }

    override suspend fun removeFromQueue(index: Int) {
        val queue = _playbackQueue.value
        if (index !in queue.items.indices) return
        val newItems = queue.items.toMutableList().apply { removeAt(index) }
        if (newItems.isEmpty()) {
            player.stop()
            player.clearMediaItems()
            _playbackQueue.value = DomainPlaybackQueue()
            _playbackState.value = DomainPlaybackState.Idle
            return
        }
        val currentIdx = queue.currentIndex
        val newCurrentIndex = when {
            index < currentIdx -> currentIdx - 1
            index == currentIdx -> currentIdx.coerceAtMost(newItems.size - 1)
            else -> currentIdx
        }
        _playbackQueue.value = queue.copy(items = newItems, currentIndex = newCurrentIndex)
        if (index == currentIdx) {
            playMediaItemInternal(newItems[newCurrentIndex])
        }
    }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val queue = _playbackQueue.value
        if (fromIndex !in queue.items.indices || toIndex !in queue.items.indices || fromIndex == toIndex) return
        val newItems = queue.items.toMutableList()
        val item = newItems.removeAt(fromIndex)
        newItems.add(toIndex, item)

        val oldCurrent = queue.currentIndex
        val newCurrent = when {
            oldCurrent == fromIndex -> toIndex
            fromIndex < oldCurrent && toIndex >= oldCurrent -> oldCurrent - 1
            fromIndex > oldCurrent && toIndex <= oldCurrent -> oldCurrent + 1
            else -> oldCurrent
        }
        _playbackQueue.value = queue.copy(items = newItems, currentIndex = newCurrent)
    }

    override suspend fun clearQueue() {
        val queue = _playbackQueue.value
        val current = queue.currentItem
        if (current != null) {
            _playbackQueue.value = queue.copy(items = listOf(current), currentIndex = 0)
        } else {
            player.stop()
            player.clearMediaItems()
            _playbackQueue.value = DomainPlaybackQueue()
            _playbackState.value = DomainPlaybackState.Idle
        }
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.1f, 4.0f)
        player.setPlaybackSpeed(clampedSpeed)
    }

    override suspend fun setPlayMode(mode: PlayMode) {
        _playbackQueue.value = _playbackQueue.value.copy(playMode = mode)
        player.repeatMode = when (mode) {
            PlayMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
            PlayMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            PlayMode.NORMAL, PlayMode.SHUFFLE -> Player.REPEAT_MODE_OFF
        }
        player.shuffleModeEnabled = (mode == PlayMode.SHUFFLE)
    }

    override suspend fun selectAudioTrack(index: Int?) {
        val params = try { player.trackSelectionParameters } catch (_: Throwable) { null }
        if (index == null) {
            params?.let {
                player.trackSelectionParameters = it
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .build()
            }
            _trackSelection.value = _trackSelection.value.copy(selectedAudioTrackIndex = null)
            return
        }

        val currentTracks = try { player.currentTracks } catch (_: Throwable) { null } ?: Tracks.EMPTY
        var audioCount = 0
        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    if (audioCount == index) {
                        params?.let {
                            player.trackSelectionParameters = it
                                .buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .build()
                        }
                        _trackSelection.value = _trackSelection.value.copy(selectedAudioTrackIndex = index)
                        return
                    }
                    audioCount++
                }
            }
        }
        // Fallback if currentTracks is empty or mock
        _trackSelection.value = _trackSelection.value.copy(selectedAudioTrackIndex = index)
    }

    override suspend fun selectSubtitleTrack(index: Int?) {
        val params = try { player.trackSelectionParameters } catch (_: Throwable) { null }
        if (index == null) {
            params?.let {
                player.trackSelectionParameters = it
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            }
            _trackSelection.value = _trackSelection.value.copy(
                selectedSubtitleTrackIndex = null,
                subtitlesEnabled = false
            )
            return
        }

        val currentTracks = try { player.currentTracks } catch (_: Throwable) { null } ?: Tracks.EMPTY
        var subCount = 0
        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    if (subCount == index) {
                        params?.let {
                            player.trackSelectionParameters = it
                                .buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .build()
                        }
                        _trackSelection.value = _trackSelection.value.copy(
                            selectedSubtitleTrackIndex = index,
                            subtitlesEnabled = true
                        )
                        return
                    }
                    subCount++
                }
            }
        }
        // Fallback if currentTracks is empty or mock
        _trackSelection.value = _trackSelection.value.copy(
            selectedSubtitleTrackIndex = index,
            subtitlesEnabled = true
        )
    }

    override suspend fun setSubtitleDelay(delayMs: Long) {
        apturaTextRenderer?.subtitleDelayMs = delayMs
        _trackSelection.value = _trackSelection.value.copy(subtitleDelayMs = delayMs)
        updatePlaybackStats()
    }

    override suspend fun setAudioDelay(delayMs: Long) {
        _trackSelection.value = _trackSelection.value.copy(audioDelayMs = delayMs)
        updatePlaybackStats()
    }

    private val externalSubtitleLabels = mutableSetOf<String>()

    override suspend fun addExternalSubtitle(uri: String, mimeType: String, label: String) {
        addExternalSubtitles(
            listOf(
                org.aptura.player.domain.model.DomainExternalSubtitle(
                    uri = uri,
                    mimeType = mimeType,
                    label = label,
                    isDefault = true
                )
            )
        )
    }

    override suspend fun addExternalSubtitles(subtitles: List<org.aptura.player.domain.model.DomainExternalSubtitle>) {
        if (subtitles.isEmpty()) return
        val currentItem = player.currentMediaItem ?: return
        val existingConfigs = currentItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        val newUris = subtitles.map { android.net.Uri.parse(it.uri) }.toSet()

        val retainedConfigs = existingConfigs.filter { it.uri !in newUris }

        val newConfigs = subtitles.map { sub ->
            externalSubtitleLabels.add(sub.label)
            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.uri))
                .setMimeType(sub.mimeType)
                .setLanguage(sub.language ?: "und")
                .setLabel(sub.label)
                .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }

        val updatedItem = currentItem.buildUpon()
            .setSubtitleConfigurations(retainedConfigs + newConfigs)
            .build()

        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying
        (player as? androidx.media3.exoplayer.ExoPlayer)?.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
        player.setMediaItem(updatedItem, currentPosition)
        if (wasPlaying) {
            player.play()
        }
    }

    override suspend fun removeExternalSubtitles() {
        externalSubtitleLabels.clear()
        val currentItem = player.currentMediaItem ?: return
        val updatedItem = currentItem.buildUpon()
            .setSubtitleConfigurations(emptyList())
            .build()

        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying
        player.setMediaItem(updatedItem, currentPosition)
        if (wasPlaying) {
            player.play()
        }
    }

    private fun updateTracks(tracks: Tracks) {
        val audioTracks = mutableListOf<DomainTrackInfo>()
        val subtitleTracks = mutableListOf<DomainTrackInfo>()
        var selectedAudioIndex: Int? = null
        var selectedSubtitleIndex: Int? = null

        var audioTrackCount = 0
        var subtitleTrackCount = 0

        for (group in tracks.groups) {
            val trackType = group.type
            for (i in 0 until group.length) {
                val isSelected = group.isTrackSelected(i)
                val format = group.getTrackFormat(i)
                val id = format.id ?: "${group.mediaTrackGroup.id}_$i"
                val language = format.language
                val resolvedLanguage = if (!language.isNullOrBlank() && !language.equals("und", ignoreCase = true)) {
                    try {
                        val loc = java.util.Locale.forLanguageTag(language)
                        val display = loc.getDisplayLanguage(java.util.Locale.ENGLISH)
                        if (display.isNotBlank()) display else language.uppercase()
                    } catch (_: Throwable) {
                        language.uppercase()
                    }
                } else null

                val channelDesc = when (format.channelCount) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    6 -> "5.1 Surround"
                    8 -> "7.1 Surround"
                    else -> null
                }

                val label = when {
                    !format.label.isNullOrBlank() && !format.label.equals("und", ignoreCase = true) -> format.label!!
                    resolvedLanguage != null -> if (channelDesc != null) "$resolvedLanguage ($channelDesc)" else resolvedLanguage
                    else -> if (channelDesc != null) "Track ${i + 1} ($channelDesc)" else "Track ${i + 1}"
                }

                when (trackType) {
                    C.TRACK_TYPE_AUDIO -> {
                        val index = audioTrackCount++
                        if (isSelected) {
                            selectedAudioIndex = index
                        }
                        audioTracks.add(
                            DomainTrackInfo(
                                id = id,
                                index = index,
                                label = label,
                                language = language,
                                isSelected = isSelected
                            )
                        )
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val index = subtitleTrackCount++
                        if (isSelected) {
                            selectedSubtitleIndex = index
                        }
                        val isExternal = externalSubtitleLabels.contains(label)
                        subtitleTracks.add(
                            DomainTrackInfo(
                                id = id,
                                index = index,
                                label = label,
                                language = language,
                                isSelected = isSelected,
                                isExternal = isExternal
                            )
                        )
                    }
                }
            }
        }

        _trackSelection.value = _trackSelection.value.copy(
            availableAudioTracks = audioTracks,
            availableSubtitleTracks = subtitleTracks,
            selectedAudioTrackIndex = selectedAudioIndex,
            selectedSubtitleTrackIndex = selectedSubtitleIndex,
            subtitlesEnabled = selectedSubtitleIndex != null
        )
    }

    private fun handleAutoAdvance() {
        scope.launch {
            val queue = _playbackQueue.value
            when (queue.playMode) {
                PlayMode.REPEAT_ONE -> {
                    seekTo(0L)
                    play()
                }
                PlayMode.REPEAT_ALL, PlayMode.NORMAL, PlayMode.SHUFFLE -> {
                    // Delegated to ViewModel's UpNext auto-advance countdown overlay
                }
            }
        }
    }

    override suspend fun setDecoderStrategy(strategy: Int) {
        if (currentDecoderStrategy == strategy) return
        currentDecoderStrategy = strategy
        _decoderStrategy.value = strategy
        hasAttemptedSoftwareFallback = (strategy == 2)
        _isCompatibilityMode.value = (strategy == 2)
        _compatibilityNotice.value = if (strategy == 2) "Playing in compatibility mode (Software Decoder)" else null
        if (strategy == 2) {
            _failedCodecName.value = "Hardware Decoder (Manual Override)"
            _failureReason.value = "Software decoding explicitly enabled via advanced playback settings."
        } else {
            _failedCodecName.value = null
            _failureReason.value = null
        }
        updatePlaybackStats()

        val currentItem = player.currentMediaItem
        if (currentItem != null && player.playbackState != Player.STATE_IDLE) {
            val currentPos = player.currentPosition.coerceAtLeast(0L)
            val wasPlaying = player.isPlaying
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(currentItem, currentPos)
            player.prepare()
            if (wasPlaying) {
                player.play()
            }
        }
    }

    override suspend fun retryPlayback() {
        val currentItem = player.currentMediaItem ?: return
        val currentPos = player.currentPosition.coerceAtLeast(0L)
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(currentItem, currentPos)
        player.prepare()
        player.play()
    }

    override suspend fun release() {
        positionTickerJob?.cancel()
        positionTickerJob = null
        player.removeListener(playerListener)
        (player as? ExoPlayer)?.removeAnalyticsListener(analyticsListener)
        player.release()
        _playbackState.value = DomainPlaybackState.Idle
    }
}
