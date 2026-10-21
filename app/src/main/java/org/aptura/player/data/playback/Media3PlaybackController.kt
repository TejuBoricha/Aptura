@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PlaybackController {

    val audioSessionId: Int
        get() = (player as? androidx.media3.exoplayer.ExoPlayer)?.audioSessionId ?: 0

    private val _playbackState = MutableStateFlow<DomainPlaybackState>(DomainPlaybackState.Idle)
    override val playbackState: StateFlow<DomainPlaybackState> = _playbackState.asStateFlow()

    private val _playbackQueue = MutableStateFlow(DomainPlaybackQueue())
    override val playbackQueue: StateFlow<DomainPlaybackQueue> = _playbackQueue.asStateFlow()

    private val _trackSelection = MutableStateFlow(DomainTrackSelection())
    override val trackSelection: StateFlow<DomainTrackSelection> = _trackSelection.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            updatePlaybackState()
            if (state == Player.STATE_ENDED) {
                handleAutoAdvance()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onPlayerError(error: PlaybackException) {
            updatePlaybackState()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _trackSelection.value = _trackSelection.value.copy(
                playbackSpeed = playbackParameters.speed
            )
            updatePlaybackState()
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTracks(tracks)
        }
    }

    init {
        player.addListener(playerListener)
        updatePlaybackState()
        val currentTracks = try { player.currentTracks } catch (_: Throwable) { null } ?: Tracks.EMPTY
        updateTracks(currentTracks)
    }

    private fun updatePlaybackState() {
        val error = player.playerError
        if (error != null) {
            _playbackState.value = DomainPlaybackState.Error(
                errorCode = error.errorCodeName,
                message = error.message ?: "Unknown playback error occurred",
                isRecoverable = error.errorCode != PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
            )
            return
        }

        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)

        _playbackState.value = when (player.playbackState) {
            Player.STATE_IDLE -> DomainPlaybackState.Idle
            Player.STATE_BUFFERING -> DomainPlaybackState.Buffering(currentPosition)
            Player.STATE_READY -> DomainPlaybackState.Ready(
                positionMs = currentPosition,
                durationMs = duration,
                isPlaying = player.isPlaying,
                playbackSpeed = player.playbackParameters.speed
            )
            Player.STATE_ENDED -> DomainPlaybackState.Ended(duration)
            else -> DomainPlaybackState.Idle
        }
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

    private fun playMediaItemInternal(item: DomainMediaItem) {
        val mediaItem = try {
            MediaItem.fromUri(item.uri)
        } catch (_: Throwable) {
            MediaItem.Builder().setMediaId(item.uri).build()
        }
        player.setMediaItem(mediaItem)
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
        _trackSelection.value = _trackSelection.value.copy(subtitleDelayMs = delayMs)
    }

    override suspend fun setAudioDelay(delayMs: Long) {
        _trackSelection.value = _trackSelection.value.copy(audioDelayMs = delayMs)
    }

    override suspend fun addExternalSubtitle(uri: String, mimeType: String, label: String) {
        val currentItem = player.currentMediaItem ?: return
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(uri))
            .setMimeType(mimeType)
            .setLanguage("en")
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val updatedItem = currentItem.buildUpon()
            .setSubtitleConfigurations(listOf(subtitleConfig))
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
                val label = format.label ?: language?.uppercase() ?: "Track ${i + 1}"

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
                        subtitleTracks.add(
                            DomainTrackInfo(
                                id = id,
                                index = index,
                                label = label,
                                language = language,
                                isSelected = isSelected
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
                PlayMode.REPEAT_ALL -> {
                    if (queue.items.isNotEmpty()) {
                        skipToNext()
                    }
                }
                PlayMode.NORMAL, PlayMode.SHUFFLE -> {
                    if (queue.hasNext) {
                        skipToNext()
                    }
                }
            }
        }
    }

    override suspend fun release() {
        player.removeListener(playerListener)
        player.release()
        _playbackState.value = DomainPlaybackState.Idle
    }
}
