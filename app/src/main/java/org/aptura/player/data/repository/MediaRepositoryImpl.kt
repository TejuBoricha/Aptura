package org.aptura.player.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.aptura.player.data.database.BookmarkDao
import org.aptura.player.data.database.BookmarkEntity
import org.aptura.player.data.database.MediaItemDao
import org.aptura.player.data.database.toDomainBookmark
import org.aptura.player.data.database.toDomainMediaItem
import org.aptura.player.domain.model.Bookmark
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.MediaRepository

/**
 * Concrete implementation of MediaRepository backed by Room MediaItemDao and BookmarkDao.
 * Enforces explicit IO dispatcher for database operations and reactive Flow streaming.
 */
class MediaRepositoryImpl(
    private val mediaItemDao: MediaItemDao,
    private val bookmarkDao: BookmarkDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    constructor(
        mediaItemDao: MediaItemDao,
        ioDispatcher: CoroutineDispatcher
    ) : this(mediaItemDao, null, ioDispatcher)

    override fun getAllVideos(): Flow<List<DomainMediaItem>> {
        return mediaItemDao.getAllVideosSortedByDate()
            .map { list -> list.map { it.toDomainMediaItem() } }
            .flowOn(ioDispatcher)
    }

    override fun getVideosInFolder(folderPath: String): Flow<List<DomainMediaItem>> {
        return mediaItemDao.getVideosByFolder(folderPath)
            .map { list -> list.map { it.toDomainMediaItem() } }
            .flowOn(ioDispatcher)
    }

    override fun getAllFolders(): Flow<List<String>> {
        return mediaItemDao.getAllFolders()
            .flowOn(ioDispatcher)
    }

    override fun searchVideos(query: String): Flow<List<DomainMediaItem>> {
        return mediaItemDao.searchVideos(query)
            .map { list -> list.map { it.toDomainMediaItem() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getVideoByUri(uri: String): DomainMediaItem? = withContext(ioDispatcher) {
        mediaItemDao.getVideoByUri(uri)?.toDomainMediaItem()
    }

    override suspend fun savePlaybackPosition(
        uri: String,
        positionMs: Long,
        isCompleted: Boolean
    ): Unit = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        mediaItemDao.updatePlaybackPosition(uri, positionMs, isCompleted, now)
    }

    override suspend fun removeVideoFromLibrary(uri: String): Unit = withContext(ioDispatcher) {
        mediaItemDao.deleteByUri(uri)
        bookmarkDao?.deleteBookmarksForMedia(uri)
    }

    override suspend fun hideVideo(uri: String): Unit = withContext(ioDispatcher) {
        mediaItemDao.hideByUri(uri)
    }

    override suspend fun restoreHiddenVideos(): Unit = withContext(ioDispatcher) {
        mediaItemDao.restoreAllHidden()
    }

    override fun getHiddenVideoCount(): Flow<Int> {
        return mediaItemDao.getHiddenCount().flowOn(ioDispatcher)
    }

    override suspend fun renameVideo(uri: String, newName: String): Unit = withContext(ioDispatcher) {
        val clean = newName.trim()
        if (clean.isNotBlank()) {
            mediaItemDao.updateCustomTitle(uri, clean)
        }
    }

    override suspend fun getLibraryStatistics(): org.aptura.player.domain.repository.LibraryStatistics = withContext(ioDispatcher) {
        val count = mediaItemDao.getMediaItemCount()
        val duration = mediaItemDao.getTotalDurationMs() ?: 0L
        val size = mediaItemDao.getTotalSizeBytes() ?: 0L
        org.aptura.player.domain.repository.LibraryStatistics(
            totalVideos = count,
            totalDurationMs = duration,
            totalSizeBytes = size
        )
    }

    override fun getBookmarks(mediaUri: String): Flow<List<Bookmark>> {
        return (bookmarkDao?.getBookmarksForMedia(mediaUri) ?: kotlinx.coroutines.flow.flowOf(emptyList()))
            .map { list -> list.map { it.toDomainBookmark() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun addBookmark(mediaUri: String, timestampMs: Long, note: String): Long {
        return withContext(ioDispatcher) {
            bookmarkDao?.insertBookmark(
                BookmarkEntity(
                    mediaUri = mediaUri,
                    timestampMs = timestampMs,
                    note = note
                )
            ) ?: 0L
        }
    }

    override fun getMissingVideos(): Flow<List<DomainMediaItem>> {
        return mediaItemDao.getMissingVideos()
            .map { list -> list.map { it.toDomainMediaItem() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun clearMissingVideos(): Unit = withContext(ioDispatcher) {
        mediaItemDao.clearMissingVideos()
    }

    override suspend fun deleteBookmark(id: Long) {
        withContext(ioDispatcher) {
            bookmarkDao?.deleteBookmarkById(id)
        }
    }

    override suspend fun updateBookmarkNote(id: Long, note: String) {
        withContext(ioDispatcher) {
            bookmarkDao?.updateBookmarkNote(id, note.trim())
        }
    }

    override suspend fun savePlaybackPreferences(
        uri: String,
        resizeMode: String,
        playbackSpeed: Float,
        decoderStrategy: Int?,
        audioDelayMs: Long,
        subtitleDelayMs: Long
    ): Unit = withContext(ioDispatcher) {
        mediaItemDao.updatePlaybackPreferences(
            uri = uri,
            resizeMode = resizeMode,
            playbackSpeed = playbackSpeed,
            decoderStrategy = decoderStrategy ?: 0,
            decoderOverridden = decoderStrategy != null,
            audioDelayMs = audioDelayMs,
            subtitleDelayMs = subtitleDelayMs
        )
    }

    override suspend fun resetPlaybackPreferences(uri: String): Unit = withContext(ioDispatcher) {
        mediaItemDao.resetPlaybackPreferences(uri)
    }
}
