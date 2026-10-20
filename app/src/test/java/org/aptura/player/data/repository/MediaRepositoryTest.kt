package org.aptura.player.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.aptura.player.data.database.MediaItemDao
import org.aptura.player.data.database.MediaItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for MediaRepositoryImpl using Turbine and Coroutines Test.
 * Verifies reactive Flow streaming, sorting, search filtering, and state restoration progress updates.
 */
class MediaRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMediaItemDao : MediaItemDao {
        val items = MutableStateFlow<List<MediaItemEntity>>(emptyList())

        override suspend fun insertOrUpdate(item: MediaItemEntity): Long {
            items.value = items.value.filter { it.uri != item.uri } + item
            return item.id
        }

        override suspend fun insertOrUpdateAll(items: List<MediaItemEntity>) {
            this.items.value = items
        }


        override fun getAllVideosSortedByDate(): Flow<List<MediaItemEntity>> {
            return items.map { list -> list.sortedByDescending { it.dateAddedSec } }
        }

        override fun getAllVideosSortedByTitle(): Flow<List<MediaItemEntity>> {
            return items.map { list -> list.sortedBy { it.title } }
        }

        override fun getVideosByFolder(folderPath: String): Flow<List<MediaItemEntity>> {
            return items.map { list ->
                list.filter { it.folderPath == folderPath }.sortedBy { it.title }
            }
        }

        override fun getAllFolders(): Flow<List<String>> {
            return items.map { list ->
                list.map { it.folderPath }.filter { it.isNotBlank() }.distinct().sorted()
            }
        }

        override suspend fun getVideoByUri(uri: String): MediaItemEntity? {
            return items.value.find { it.uri == uri }
        }

        override suspend fun getVideoByMediaStoreId(mediaStoreId: Long): MediaItemEntity? {
            return items.value.find { it.mediaStoreId == mediaStoreId }
        }

        override fun searchVideos(query: String): Flow<List<MediaItemEntity>> {
            return items.map { list ->
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.displayName.contains(query, ignoreCase = true)
                }.sortedBy { it.title }
            }
        }

        override suspend fun updatePlaybackPosition(
            uri: String,
            positionMs: Long,
            isCompleted: Boolean,
            timestamp: Long
        ) {
            items.value = items.value.map {
                if (it.uri == uri) {
                    it.copy(
                        playbackPositionMs = positionMs,
                        isCompleted = isCompleted,
                        lastPlayedTimestamp = timestamp
                    )
                } else it
            }
        }

        override suspend fun getAllVideosRaw(): List<MediaItemEntity> = items.value

        override suspend fun deleteByIds(ids: List<Long>) {
            items.value = items.value.filter { it.id !in ids }
        }

        override suspend fun deleteRemovedVideos(existingMediaStoreIds: List<Long>) {
            items.value = items.value.filter { it.mediaStoreId in existingMediaStoreIds }
        }

        override suspend fun getMediaItemCount(): Int = items.value.size

        override suspend fun clearAll() {
            items.value = emptyList()
        }
    }

    @Test
    fun getAllVideos_emitsSortedByDateDescending() = runTest(testDispatcher) {
        val fakeDao = FakeMediaItemDao()
        val repo = MediaRepositoryImpl(fakeDao, testDispatcher)

        val item1 = MediaItemEntity(
            id = 1,
            mediaStoreId = 101,
            uri = "content://media/1",
            title = "Alpha",
            displayName = "alpha.mp4",
            durationMs = 10000,
            sizeBytes = 1024,
            mimeType = "video/mp4",
            dateAddedSec = 1000
        )
        val item2 = MediaItemEntity(
            id = 2,
            mediaStoreId = 102,
            uri = "content://media/2",
            title = "Beta",
            displayName = "beta.mp4",
            durationMs = 20000,
            sizeBytes = 2048,
            mimeType = "video/mp4",
            dateAddedSec = 2000
        )

        fakeDao.insertOrUpdateAll(listOf(item1, item2))

        repo.getAllVideos().test {
            val list = awaitItem()
            assertEquals(2, list.size)
            // Beta has higher dateAddedSec (2000 vs 1000)
            assertEquals("Beta", list[0].title)
            assertEquals("Alpha", list[1].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchVideos_filtersMatchesAccurately() = runTest(testDispatcher) {
        val fakeDao = FakeMediaItemDao()
        val repo = MediaRepositoryImpl(fakeDao, testDispatcher)

        val item1 = MediaItemEntity(
            id = 1,
            mediaStoreId = 101,
            uri = "content://media/1",
            title = "Nature Documentary",
            displayName = "nature.mkv",
            durationMs = 60000,
            sizeBytes = 4096,
            mimeType = "video/mkv"
        )
        val item2 = MediaItemEntity(
            id = 2,
            mediaStoreId = 102,
            uri = "content://media/2",
            title = "City Tour",
            displayName = "city.mp4",
            durationMs = 30000,
            sizeBytes = 2048,
            mimeType = "video/mp4"
        )

        fakeDao.insertOrUpdateAll(listOf(item1, item2))

        repo.searchVideos("nature").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Nature Documentary", list[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savePlaybackPosition_updatesEntityInDao() = runTest(testDispatcher) {
        val fakeDao = FakeMediaItemDao()
        val repo = MediaRepositoryImpl(fakeDao, testDispatcher)

        val item = MediaItemEntity(
            id = 1,
            mediaStoreId = 101,
            uri = "content://media/1",
            title = "Movie",
            displayName = "movie.mp4",
            durationMs = 120000,
            sizeBytes = 8192,
            mimeType = "video/mp4",
            playbackPositionMs = 0
        )
        fakeDao.insertOrUpdate(item)

        repo.savePlaybackPosition("content://media/1", 45000L, false)

        val updated = fakeDao.getVideoByUri("content://media/1")
        assertNotNull(updated)
        assertEquals(45000L, updated?.playbackPositionMs)
    }
}
