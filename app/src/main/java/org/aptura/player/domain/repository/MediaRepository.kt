package org.aptura.player.domain.repository

import kotlinx.coroutines.flow.Flow
import org.aptura.player.domain.model.DomainMediaItem

/**
 * Pure domain repository interface for local media querying and library management.
 * Invariant: ZERO Android framework dependencies in domain/ (DEC-002, ADR-001).
 * Implemented by MediaRepositoryImpl in the data layer.
 */
interface MediaRepository {

    /**
     * Emits the complete list of indexed videos sorted by date added descending (W9 default).
     */
    fun getAllVideos(): Flow<List<DomainMediaItem>>

    /**
     * Emits all videos located within a specific directory folder path.
     */
    fun getVideosInFolder(folderPath: String): Flow<List<DomainMediaItem>>

    /**
     * Emits all unique folder paths containing media files.
     */
    fun getAllFolders(): Flow<List<String>>

    /**
     * Emits videos matching the text search query (title or display name).
     */
    fun searchVideos(query: String): Flow<List<DomainMediaItem>>

    /**
     * Retrieves a single media item by its URI, or null if not indexed.
     */
    suspend fun getVideoByUri(uri: String): DomainMediaItem?

    /**
     * Updates the saved playback progress position for state restoration (TST-REST-01, spec/12A).
     */
    suspend fun savePlaybackPosition(uri: String, positionMs: Long, isCompleted: Boolean)
}
