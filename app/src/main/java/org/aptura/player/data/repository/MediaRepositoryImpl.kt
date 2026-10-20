package org.aptura.player.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.aptura.player.data.database.MediaItemDao
import org.aptura.player.data.database.toDomainMediaItem
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.MediaRepository

/**
 * Concrete implementation of MediaRepository backed by Room MediaItemDao.
 * Enforces explicit IO dispatcher for database operations and reactive Flow streaming.
 */
class MediaRepositoryImpl(
    private val mediaItemDao: MediaItemDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

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
}
