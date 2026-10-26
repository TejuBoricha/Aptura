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
     * Removes a video's index row (and its bookmarks). Use after the physical file was really deleted.
     */
    suspend fun removeVideoFromLibrary(uri: String)

    /**
     * Hides a video from the library without touching the file. Unlike a plain index delete, the hide
     * survives rescans until [restoreHiddenVideos] is called (SCR-018).
     */
    suspend fun hideVideo(uri: String)

    /** Un-hides every hidden video. */
    suspend fun restoreHiddenVideos()

    /** Emits how many videos are currently hidden. */
    fun getHiddenVideoCount(): Flow<Int>

    /**
     * Renames a video in the media library index and local database (FR-LIB-013).
     *
     * [newUri] is set only when the storage provider changed the document identifier (SAF). The row and
     * its bookmarks are moved to the new URI so saved progress survives the rename.
     */
    suspend fun renameVideo(
        uri: String,
        newName: String,
        newUri: String? = null,
        newDisplayName: String? = null
    )

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
     * Emits videos flagged as missing (unmounted storage, moved or deleted) (FR-LIB-018, FR-LIB-021).
     */
    fun getMissingVideos(): Flow<List<DomainMediaItem>>

    /**
     * Purges missing items from the library database.
     */
    suspend fun clearMissingVideos()

    /**
     * Purges all indexed videos whose URI begins with [uriPrefix] (e.g. when a custom SAF folder is removed).
     */
    suspend fun removeVideosByUriPrefix(uriPrefix: String) {}

    /**
     * Deletes a bookmark by its ID (FR-CTRL-011).
     */
    suspend fun deleteBookmark(id: Long)

    /**
     * Updates the user-editable note for an existing bookmark (P2-16, FR-CTRL-011).
     */
    suspend fun updateBookmarkNote(id: Long, note: String) {}

    /**
     * Persists per-video playback preferences (FR-CTRL-012, FR-PLAY-003).
     */
    suspend fun savePlaybackPreferences(
        uri: String,
        resizeMode: String,
        playbackSpeed: Float,
        /** The user's choice for this video, or null for "use the default from Settings". */
        decoderStrategy: Int?,
        audioDelayMs: Long,
        subtitleDelayMs: Long
    )

    /**
     * Resets per-video playback preferences back to global defaults (FR-CTRL-012).
     */
    suspend fun resetPlaybackPreferences(uri: String)
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
