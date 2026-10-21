package org.aptura.player.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.DomainPlaybackQueue
import org.aptura.player.domain.model.DomainPlaybackState
import org.aptura.player.domain.model.DomainTrackSelection
import org.aptura.player.domain.model.PlayMode

/**
 * Pure domain interface for playback orchestration.
 * Invariant: ZERO Android framework or Media3 types in this interface.
 * Implemented by Media3SessionController in the data layer.
 */
interface PlaybackController {
    val playbackState: StateFlow<DomainPlaybackState>
    val playbackQueue: StateFlow<DomainPlaybackQueue>
    val trackSelection: StateFlow<DomainTrackSelection>

    suspend fun playMediaItem(item: DomainMediaItem)
    suspend fun playQueue(items: List<DomainMediaItem>, startIndex: Int = 0)
    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setPlayMode(mode: PlayMode)
    suspend fun selectAudioTrack(index: Int?)
    suspend fun selectSubtitleTrack(index: Int?)
    suspend fun setSubtitleDelay(delayMs: Long)
    suspend fun setAudioDelay(delayMs: Long)
    suspend fun addExternalSubtitle(uri: String, mimeType: String = "application/x-subrip", label: String = "External Subtitle")
    suspend fun release()
}
