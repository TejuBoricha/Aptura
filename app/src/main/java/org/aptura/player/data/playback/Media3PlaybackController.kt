@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import org.aptura.player.domain.model.PlaybackStatsInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.ErrorTaxonomy
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.domain.model.DomainExternalSubtitle
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.DomainTrackInfo
import org.aptura.player.domain.model.DomainTrackSelection
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.domain.model.cleanDisplayTitle
import org.aptura.player.domain.repository.PlaybackController
import org.aptura.player.domain.repository.SidecarSubtitleResolver
import org.aptura.player.domain.usecase.PlaybackNavigationRules
import org.aptura.player.domain.usecase.PreviousActionOutcome
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Concrete implementation of domain PlaybackController backed by Media3 Player.
 * Maps low-level ExoPlayer/MediaController events into immutable domain states
 * and enforces platform navigation rules (FR-CTRL-002, W8, W9).
 *
 * The queue lives INSIDE the player's playlist (audit P0-03/P0-07). Earlier versions played one item at a time with
 * `setMediaItem`, which made `REPEAT_MODE_ALL` loop a single file, made shuffle a no-op, and disabled next/previous
 * in the notification, lock screen and Bluetooth headsets. Now repeat/shuffle/next/previous are the player's own.
 */
class Media3PlaybackController(
    val player: Player,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val enablePositionTicker: Boolean = true,
    initialDecoderStrategy: Int = 0,
    var apturaTextRenderer: ApturaTextRenderer? = null,
    private val sidecarResolver: SidecarSubtitleResolver? = null,
    private val artworkProvider: (suspend (mediaUri: String) -> ByteArray?)? = null,
    var audioDelayProcessor: AudioDelayProcessor? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PlaybackController {

    var currentDecoderStrategy: Int = initialDecoderStrategy
        private set

    private var hasAttemptedSoftwareFallback = false
    private var hasAttemptedAudioTrackFallback = false

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

    private val _audioSessionIds = MutableStateFlow(audioSessionId.coerceAtLeast(0))
    override val audioSessionIds: StateFlow<Int> = _audioSessionIds.asStateFlow()

    private val _playbackState = MutableStateFlow<DomainPlaybackState>(DomainPlaybackState.Idle)
    override val playbackState: StateFlow<DomainPlaybackState> = _playbackState.asStateFlow()

    private val _skippedItems = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val skippedItems: SharedFlow<String> = _skippedItems.asSharedFlow()

    /** Items skipped in a row because they could not be opened; bounds the skipping so an all-broken queue cannot loop. */
    private var consecutiveSkips = 0

    private val _autoPairedSubtitles = MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val autoPairedSubtitles: SharedFlow<String> = _autoPairedSubtitles.asSharedFlow()

    private var positionTickerJob: Job? = null

    // ---- Queue (mirror of the player's playlist; index N here == media item N in the player) ----------------
    private val queueItems = mutableListOf<DomainMediaItem>()
    private val externalSubtitleLabels = mutableSetOf<String>()
    private var playMode: PlayMode = PlayMode.NORMAL
    private var pauseAtEndOfItems = false

    /** Sidecar subtitles already resolved per media URI (they are baked into that item's MediaItem). */
    private val sidecarsByUri = mutableMapOf<String, List<DomainExternalSubtitle>>()
    private var currentMediaId: String? = null

    /** True while an item has finished and the player is parked at its end waiting for the Up Next decision. */
    private var pausedAtItemEnd = false

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
            publishQueue()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                pausedAtItemEnd = false
                consecutiveSkips = 0
            }
            checkPositionTicker(isPlaying)
            updatePlaybackState()
            updatePlaybackStats()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // `pauseAtEndOfMediaItems`: the player parks at the end of a finished item instead of advancing.
            // Surface that as "Ended" so the Up Next countdown can run (same contract as the last item ending).
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                pausedAtItemEnd = true
                checkPositionTicker(false)
                updatePlaybackState()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            pausedAtItemEnd = false
            val newId = mediaItem?.mediaId
            if (newId != currentMediaId) {
                currentMediaId = newId
                resetPerItemState()
                registerExternalLabels(sidecarsByUri[newId].orEmpty())
                announceAutoPair(sidecarsByUri[newId].orEmpty())
                prefetchNeighbours()
            }
            publishQueue()
            updatePlaybackState()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            publishQueue()
        }

        override fun onPlayerError(error: PlaybackException) {
            checkPositionTicker(false)

            // A file that has vanished or cannot be opened must not strand the whole queue on an error screen.
            if (error.errorCode in UNOPENABLE_FILE_ERRORS && skipUnopenableItem()) return

            val isAudioFailure = ErrorTaxonomy.isAudioError(error)
            if (isAudioFailure && !hasAttemptedAudioTrackFallback && player.currentMediaItem != null) {
                val altTrack = _trackSelection.value.availableAudioTracks.firstOrNull {
                    !it.isSelected && it.index != _trackSelection.value.selectedAudioTrackIndex
                }
                if (altTrack != null) {
                    hasAttemptedAudioTrackFallback = true
                    _compatibilityNotice.value = "Switched to alternate audio track (${altTrack.label})"
                    scope.launch(mainDispatcher) {
                        selectAudioTrack(altTrack.index)
                        player.stop()
                        player.prepare()
                        player.play()
                    }
                    return
                }
            }

            val isDecoderFailure = !isAudioFailure && (
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES
                )

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
            if (isDecoderFailure && !hasAttemptedSoftwareFallback && currentDecoderStrategy != 2 && hasSoftwareDecoder && player.currentMediaItem != null) {
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

                // Keeps the playlist and the position; prepare() clears the error and re-selects decoders.
                player.stop()
                player.prepare()
                player.play()
                return
            }

            // Fallback unavailable or failed: clear any active compatibility notice so the HUD never claims
            // "Playing in compatibility mode" on top of a terminal Playback Error modal, and log EV_011 (spec/14 §4).
            _isCompatibilityMode.value = false
            _compatibilityNotice.value = null
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

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            _audioSessionIds.value = audioSessionId.coerceAtLeast(0)
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
            val isSoftwareAlreadyAttempted = hasAttemptedSoftwareFallback || currentDecoderStrategy == 2
            val taxonomyError = ErrorTaxonomy.mapException(error, isSoftwareAlreadyAttempted)
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

        if (pausedAtItemEnd && player.playbackState == Player.STATE_READY) {
            _playbackState.value = DomainPlaybackState.Ended(duration)
            return
        }

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
            null -> "Unknown"
            else -> "${audioFormat.channelCount} ch"
        }
        val audioSampleRateHz = audioFormat?.sampleRate?.takeIf { it > 0 } ?: 0
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
            videoMimeType = videoFormat?.sampleMimeType.orEmpty(),
            videoCodec = videoFormat?.codecs ?: videoFormat?.sampleMimeType?.substringAfter('/') ?: "",
            videoResolution = if (width > 0 && height > 0) "${width}x${height}" else "",
            frameRate = frameRate,
            videoBitrateKbps = videoBitrateKbps,
            droppedFrames = totalDroppedFrames,
            dropPercentage = dropPct,
            audioDecoderName = currentAudioDecoderName,
            audioMimeType = audioFormat?.sampleMimeType.orEmpty(),
            audioCodec = audioFormat?.codecs ?: audioFormat?.sampleMimeType?.substringAfter('/') ?: "",
            audioChannels = audioChannels,
            audioSampleRateHz = audioSampleRateHz,
            audioBitrateKbps = audioBitrateKbps,
            audioDelayMs = _trackSelection.value.audioDelayMs,
            subtitleDelayMs = _trackSelection.value.subtitleDelayMs,
            hdrFormat = hdrFormat
        )
    }

    // ------------------------------------------------------------------------------------------------------
    // Queue
    // ------------------------------------------------------------------------------------------------------

    /** Pushes the player's real queue state (index, next/previous availability incl. shuffle order) to the domain. */
    private fun publishQueue() {
        val index = if (queueItems.isEmpty()) -1 else player.currentMediaItemIndex.coerceIn(0, queueItems.lastIndex)
        _playbackQueue.value = DomainPlaybackQueue(
            items = queueItems.toList(),
            currentIndex = index,
            playMode = playMode,
            nextAvailable = player.hasNextMediaItem(),
            previousAvailable = player.hasPreviousMediaItem(),
            nextIndex = player.nextMediaItemIndex.takeIf { it != C.INDEX_UNSET }
        )
    }

    private fun buildMediaItem(
        item: DomainMediaItem,
        subtitles: List<DomainExternalSubtitle>,
        artwork: ByteArray?
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(cleanDisplayTitle(item.title))
            .setDisplayTitle(cleanDisplayTitle(item.title))
            .apply { if (artwork != null) setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
            .build()
        return MediaItem.Builder()
            .setUri(item.uri)
            .setMediaId(item.uri)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.uri))
                    .setMimeType(sub.mimeType)
                    .setLanguage(sub.language ?: "und")
                    .setLabel(sub.label)
                    .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            })
            .build()
    }

    /** Moves on to the next queue item after the current one could not be opened. @return false when there is nowhere to go. */
    private fun skipUnopenableItem(): Boolean {
        if (queueItems.size < 2 || !player.hasNextMediaItem()) return false
        if (consecutiveSkips >= queueItems.size - 1) return false
        consecutiveSkips++
        queueItems.getOrNull(player.currentMediaItemIndex)?.let { _skippedItems.tryEmit(cleanDisplayTitle(it.title)) }
        pausedAtItemEnd = false
        player.seekToNextMediaItem()
        player.prepare() // recovers the player from its error state; the playlist and settings are kept
        player.play()
        return true
    }

    private suspend fun resolveSidecars(item: DomainMediaItem): List<DomainExternalSubtitle> {
        val resolver = sidecarResolver ?: return emptyList()
        return try {
            withContext(ioDispatcher) { resolver.resolve(item.uri) }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private suspend fun resolveArtwork(item: DomainMediaItem): ByteArray? {
        val provider = artworkProvider ?: return null
        return try {
            withContext(ioDispatcher) { provider(item.uri) }
        } catch (_: Throwable) {
            null
        }
    }

    private fun registerExternalLabels(subs: List<DomainExternalSubtitle>) {
        subs.forEach { externalSubtitleLabels.add(it.label) }
    }

    private fun announceAutoPair(subs: List<DomainExternalSubtitle>) {
        subs.firstOrNull { it.isDefault }?.let {
            _autoPairedSubtitles.tryEmit(it.label.substringBefore(" ("))
        }
    }

    /** A default sidecar must actually turn subtitles on, even if the user switched them off for a previous video. */
    private fun enableSubtitlesFor(subs: List<DomainExternalSubtitle>) {
        if (subs.none { it.isDefault }) return
        val params = try { player.trackSelectionParameters } catch (_: Throwable) { null } ?: return
        player.trackSelectionParameters = params.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }

    /** Resolves subtitles for the next/previous items in the background and bakes them into those media items. */
    private fun prefetchNeighbours() {
        if (sidecarResolver == null) return
        val candidates = listOf(player.nextMediaItemIndex, player.previousMediaItemIndex)
            .filter { it != C.INDEX_UNSET && it in queueItems.indices }
            .distinct()
        for (index in candidates) {
            val item = queueItems[index]
            if (sidecarsByUri.containsKey(item.uri)) continue
            scope.launch(mainDispatcher) {
                val subs = resolveSidecars(item)
                sidecarsByUri[item.uri] = subs
                if (subs.isEmpty()) return@launch
                val liveIndex = queueItems.indexOfFirst { it.uri == item.uri }
                // Never replace the item that is playing now (that would restart it); it was resolved up front.
                if (liveIndex >= 0 && liveIndex != player.currentMediaItemIndex &&
                    liveIndex < player.mediaItemCount && player.getMediaItemAt(liveIndex).mediaId == item.uri
                ) {
                    player.replaceMediaItem(liveIndex, buildMediaItem(item, subs, null))
                }
            }
        }
    }

    /** Everything that belongs to "the video that is playing" and must start fresh for each new item. */
    private fun resetPerItemState() {
        externalSubtitleLabels.clear()
        apturaTextRenderer?.subtitleDelayMs = 0L
        _trackSelection.value = _trackSelection.value.copy(subtitleDelayMs = 0L)
        hasAttemptedSoftwareFallback = false
        hasAttemptedAudioTrackFallback = false
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
    }

    private fun applyPlayModeToPlayer() {
        player.repeatMode = when (playMode) {
            PlayMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
            PlayMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            PlayMode.NORMAL, PlayMode.SHUFFLE -> Player.REPEAT_MODE_OFF
        }
        player.shuffleModeEnabled = (playMode == PlayMode.SHUFFLE)
        (player as? ExoPlayer)?.pauseAtEndOfMediaItems = pauseAtEndOfItems && playMode != PlayMode.REPEAT_ONE
    }

    /**
     * ExoPlayer's default shuffle order is a random permutation in which the playing item can sit anywhere; "next"
     * then only reaches the items AFTER it in that permutation, silently skipping the rest. Make the playing item
     * the head of the order so that one pass visits every other video exactly once.
     */
    private fun shuffleRemainingAfterCurrent() {
        val exo = player as? ExoPlayer ?: return
        val count = exo.mediaItemCount
        if (count == 0) return
        val current = exo.currentMediaItemIndex.coerceIn(0, count - 1)
        val others = (0 until count).filter { it != current }.shuffled()
        exo.setShuffleOrder(DefaultShuffleOrder((listOf(current) + others).toIntArray(), System.nanoTime()))
    }

    override suspend fun playMediaItem(item: DomainMediaItem) {
        playQueue(listOf(item), 0)
    }

    override suspend fun playQueue(items: List<DomainMediaItem>, startIndex: Int) {
        if (items.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, items.size - 1)
        val target = items[clampedIndex]

        // Resolve everything that has to be baked into the first MediaItem BEFORE touching the player, off the main
        // thread, so playback never starts and then restarts when subtitles are found (audit P0-10).
        val (subtitles, artwork) = coroutineScope {
            val subs = async { resolveSidecars(target) }
            val art = async { resolveArtwork(target) }
            subs.await() to art.await()
        }

        pausedAtItemEnd = false
        consecutiveSkips = 0
        queueItems.clear()
        queueItems.addAll(items)
        sidecarsByUri.clear()
        sidecarsByUri[target.uri] = subtitles
        currentMediaId = target.uri

        resetPerItemState()
        registerExternalLabels(subtitles)
        enableSubtitlesFor(subtitles)

        val mediaItems = items.mapIndexed { index, item ->
            if (index == clampedIndex) buildMediaItem(item, subtitles, artwork) else buildMediaItem(item, emptyList(), null)
        }
        player.setMediaItems(mediaItems, clampedIndex, 0L)
        applyPlayModeToPlayer()
        if (playMode == PlayMode.SHUFFLE) shuffleRemainingAfterCurrent()
        player.prepare()
        player.play()

        publishQueue()
        announceAutoPair(subtitles)
        prefetchNeighbours()
    }

    override suspend fun play() {
        pausedAtItemEnd = false
        player.play()
    }

    override suspend fun pause() {
        player.pause()
    }

    override suspend fun seekTo(positionMs: Long) {
        // Right after a queue is loaded the player does not know the duration yet (TIME_UNSET). Clamping to that
        // "duration" used to turn every early seek — notably "resume where you left off" — into a seek to 0:00.
        val duration = player.duration
        val clampedPosition = if (duration == C.TIME_UNSET || duration <= 0L) {
            positionMs.coerceAtLeast(0L)
        } else {
            PlaybackNavigationRules.clampSeekPosition(positionMs, duration)
        }
        pausedAtItemEnd = false
        player.seekTo(clampedPosition)
    }

    override suspend fun skipToNext() {
        if (!player.hasNextMediaItem()) return
        pausedAtItemEnd = false
        player.seekToNextMediaItem()
        player.play()
    }

    override suspend fun skipToPrevious() {
        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val queue = _playbackQueue.value
        pausedAtItemEnd = false
        when (PlaybackNavigationRules.evaluatePreviousAction(currentPosition, queue)) {
            PreviousActionOutcome.RestartCurrentItem -> player.seekTo(0L)
            PreviousActionOutcome.NavigateToPreviousItem -> {
                player.seekToPreviousMediaItem()
                player.play()
            }
        }
    }

    override suspend fun playQueueItem(index: Int) {
        if (index !in queueItems.indices) return
        pausedAtItemEnd = false
        player.seekToDefaultPosition(index)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    override suspend fun addToQueue(item: DomainMediaItem) {
        if (queueItems.isEmpty()) {
            playQueue(listOf(item), 0)
            return
        }
        queueItems.add(item)
        player.addMediaItem(buildMediaItem(item, emptyList(), null))
        publishQueue()
    }

    override suspend fun playNext(item: DomainMediaItem) {
        if (queueItems.isEmpty()) {
            playQueue(listOf(item), 0)
            return
        }
        val insertIndex = (player.currentMediaItemIndex + 1).coerceIn(0, queueItems.size)
        queueItems.add(insertIndex, item)
        player.addMediaItem(insertIndex, buildMediaItem(item, emptyList(), null))
        publishQueue()
    }

    override suspend fun removeFromQueue(index: Int) {
        if (index !in queueItems.indices) return
        queueItems.removeAt(index)
        player.removeMediaItem(index)
        if (queueItems.isEmpty()) {
            player.stop()
            player.clearMediaItems()
            currentMediaId = null
            _playbackQueue.value = DomainPlaybackQueue()
            _playbackState.value = DomainPlaybackState.Idle
            return
        }
        publishQueue()
    }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in queueItems.indices || toIndex !in queueItems.indices || fromIndex == toIndex) return
        queueItems.add(toIndex, queueItems.removeAt(fromIndex))
        player.moveMediaItem(fromIndex, toIndex)
        publishQueue()
    }

    override suspend fun clearQueue() {
        if (queueItems.isEmpty()) return
        val current = player.currentMediaItemIndex
        if (current !in queueItems.indices) {
            player.stop()
            player.clearMediaItems()
            queueItems.clear()
            currentMediaId = null
            _playbackQueue.value = DomainPlaybackQueue()
            _playbackState.value = DomainPlaybackState.Idle
            return
        }
        val keep = queueItems[current]
        if (current < queueItems.lastIndex) player.removeMediaItems(current + 1, queueItems.size)
        if (current > 0) player.removeMediaItems(0, current)
        queueItems.clear()
        queueItems.add(keep)
        publishQueue()
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.1f, 4.0f)
        player.setPlaybackSpeed(clampedSpeed)
    }

    override suspend fun setPlayMode(mode: PlayMode) {
        val enteringShuffle = mode == PlayMode.SHUFFLE && playMode != PlayMode.SHUFFLE
        playMode = mode
        applyPlayModeToPlayer()
        if (enteringShuffle) shuffleRemainingAfterCurrent()
        publishQueue()
    }

    override suspend fun setPauseAtEndOfItems(enabled: Boolean) {
        pauseAtEndOfItems = enabled
        (player as? ExoPlayer)?.pauseAtEndOfMediaItems = enabled && playMode != PlayMode.REPEAT_ONE
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
        audioDelayProcessor?.delayMs = delayMs
        _trackSelection.value = _trackSelection.value.copy(audioDelayMs = delayMs)
        updatePlaybackStats()
    }

    override suspend fun addExternalSubtitle(uri: String, mimeType: String, label: String) {
        addExternalSubtitles(
            listOf(
                DomainExternalSubtitle(
                    uri = uri,
                    mimeType = mimeType,
                    label = label,
                    isDefault = true
                )
            )
        )
    }

    /** Manual import: rebuilds the playing item with the extra subtitle(s) and resumes exactly where it was. */
    override suspend fun addExternalSubtitles(subtitles: List<DomainExternalSubtitle>) {
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

        replaceCurrentItem(currentItem.buildUpon().setSubtitleConfigurations(retainedConfigs + newConfigs).build())
        enableSubtitlesFor(subtitles)
    }

    override suspend fun removeExternalSubtitles() {
        externalSubtitleLabels.clear()
        val currentItem = player.currentMediaItem ?: return
        replaceCurrentItem(currentItem.buildUpon().setSubtitleConfigurations(emptyList()).build())
    }

    override suspend fun removeExternalSubtitle(trackLabel: String) {
        externalSubtitleLabels.remove(trackLabel)
        val currentItem = player.currentMediaItem ?: return
        val existingConfigs = currentItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        val remaining = existingConfigs.filterNot { it.label == trackLabel }
        replaceCurrentItem(currentItem.buildUpon().setSubtitleConfigurations(remaining).build())
    }

    /** Swaps the playing media item in place and restores position + play state (no global seek-mode side effects). */
    private fun replaceCurrentItem(updated: MediaItem) {
        val index = player.currentMediaItemIndex
        val position = player.currentPosition
        val playWhenReady = player.playWhenReady
        player.replaceMediaItem(index, updated)
        player.seekTo(index, position)
        player.playWhenReady = playWhenReady
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

        if (player.currentMediaItem != null && player.playbackState != Player.STATE_IDLE) {
            val wasPlaying = player.playWhenReady
            // stop() keeps the playlist and position; prepare() re-creates the codecs with the new strategy.
            player.stop()
            player.prepare()
            player.playWhenReady = wasPlaying
        }
    }

    override suspend fun retryPlayback() {
        if (player.currentMediaItem == null) return
        player.prepare()
        player.play()
    }

    /** Frame stepping: pauses, seeks *exactly* one frame (using the real frame rate), then restores seek mode. */
    private var lastStepTargetMs: Double? = null

    override suspend fun stepFrame(forward: Boolean) {
        player.pause()
        val exo = player as? ExoPlayer
        val fps = (exo?.videoFormat ?: currentVideoFormat)?.frameRate?.takeIf { it > 0f } ?: DEFAULT_FRAME_RATE
        val frameMs = 1000.0 / fps
        val position = player.currentPosition.coerceAtLeast(0L)
        // Continue from the previous *unrounded* target while stepping repeatedly: rounding every step to whole
        // milliseconds drifts and eventually repeats or skips frames.
        val base = lastStepTargetMs?.takeIf { abs(it - position) <= 2.0 } ?: position.toDouble()
        val duration = player.duration.coerceAtLeast(0L).toDouble()
        val target = (base + if (forward) frameMs else -frameMs).coerceIn(0.0, duration)
        lastStepTargetMs = target

        val previous = exo?.seekParameters
        exo?.setSeekParameters(SeekParameters.EXACT)
        player.seekTo(target.roundToLong())
        exo?.setSeekParameters(previous ?: SeekParameters.DEFAULT)
    }

    override suspend fun setVideoEnabled(enabled: Boolean) {
        val params = try { player.trackSelectionParameters } catch (_: Throwable) { null } ?: return
        player.trackSelectionParameters = params.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !enabled)
            .build()
    }

    override suspend fun release() {
        positionTickerJob?.cancel()
        positionTickerJob = null
        player.removeListener(playerListener)
        (player as? ExoPlayer)?.removeAnalyticsListener(analyticsListener)
        player.release()
        _playbackState.value = DomainPlaybackState.Idle
    }

    private companion object {
        const val DEFAULT_FRAME_RATE = 30f
        val UNOPENABLE_FILE_ERRORS = setOf(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        )
    }
}
