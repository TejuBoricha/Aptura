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

    /**
     * Removes a video from the library index without deleting the physical file on storage (SCR-018).
     */
    suspend fun removeVideoFromLibrary(uri: String)

    /**
     * Renames a video in the media library index and local database (FR-LIB-013).
     */
    suspend fun renameVideo(uri: String, newName: String)

    /**
     * Computes high-level library statistics for SCR-014 (total videos, duration, and disk footprint).
     */
    suspend fun getLibraryStatistics(): LibraryStatistics

    /**
     * Emits the saved bookmarks for a media item, ordered chronologically by timestamp (FR-CTRL-011).
     */
    fun getBookmarks(mediaUri: String): Flow<List<org.aptura.player.domain.model.Bookmark>>

    /**
     * Saves a new bookmark at the given timestamp with an optional note (FR-CTRL-011).
     */
    suspend fun addBookmark(mediaUri: String, timestampMs: Long, note: String = ""): Long

    /**
     * Deletes a bookmark by its ID (FR-CTRL-011).
     */
    suspend fun deleteBookmark(id: Long)
}

/**
 * High-level statistics representing the indexed media library (SCR-014).
 * Pure Kotlin domain model with zero framework dependencies.
 */
data class LibraryStatistics(
    val totalVideos: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L
)
