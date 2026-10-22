package org.aptura.player.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.aptura.player.data.database.BookmarkDao
import org.aptura.player.data.database.BookmarkEntity
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

        override suspend fun updateCustomTitle(uri: String, customTitle: String?) {
            items.value = items.value.map { if (it.uri == uri) it.copy(customTitle = customTitle) else it }
        }

        override suspend fun hideByUri(uri: String) {
            items.value = items.value.map { if (it.uri == uri) it.copy(isHidden = true) else it }
        }

        override suspend fun restoreAllHidden() {
            items.value = items.value.map { it.copy(isHidden = false) }
        }

        override fun getHiddenCount(): Flow<Int> = items.map { list -> list.count { it.isHidden } }

        override suspend fun getAllVideosRaw(): List<MediaItemEntity> = items.value

        override suspend fun deleteByIds(ids: List<Long>) {
            items.value = items.value.filter { it.id !in ids }
        }

        override suspend fun deleteByUri(uri: String) {
            items.value = items.value.filter { it.uri != uri }
        }

        override suspend fun updateMissingStatusByIds(ids: List<Long>, isMissing: Boolean) {
            items.value = items.value.map {
                if (it.id in ids) it.copy(isMissing = isMissing) else it
            }
        }

        override suspend fun updateMissingStatusByUri(uri: String, isMissing: Boolean) {
            items.value = items.value.map {
                if (it.uri == uri) it.copy(isMissing = isMissing) else it
            }
        }

        override fun getMissingVideos(): Flow<List<MediaItemEntity>> {
            return items.map { list -> list.filter { it.isMissing } }
        }

        override suspend fun clearMissingVideos() {
            items.value = items.value.filter { !it.isMissing }
        }

        override suspend fun deleteRemovedVideos(existingMediaStoreIds: List<Long>) {
            items.value = items.value.filter { it.mediaStoreId in existingMediaStoreIds }
        }

        override suspend fun getMediaItemCount(): Int = items.value.size

        override suspend fun getTotalDurationMs(): Long? =
            items.value.sumOf { it.durationMs }.takeIf { items.value.isNotEmpty() }

        override suspend fun getTotalSizeBytes(): Long? =
            items.value.sumOf { it.sizeBytes }.takeIf { items.value.isNotEmpty() }

        override suspend fun updatePlaybackPreferences(
            uri: String,
            resizeMode: String,
            playbackSpeed: Float,
            decoderStrategy: Int,
            audioDelayMs: Long,
            subtitleDelayMs: Long
        ) {
            items.value = items.value.map {
                if (it.uri == uri) {
                    it.copy(
                        resizeMode = resizeMode,
                        playbackSpeed = playbackSpeed,
                        decoderStrategy = decoderStrategy,
                        audioDelayMs = audioDelayMs,
                        subtitleDelayMs = subtitleDelayMs
                    )
                } else it
            }
        }

        override suspend fun resetPlaybackPreferences(uri: String) {
            items.value = items.value.map {
                if (it.uri == uri) {
                    it.copy(
                        resizeMode = "FIT",
                        playbackSpeed = 1.0f,
                        decoderStrategy = 0,
                        audioDelayMs = 0L,
                        subtitleDelayMs = 0L
                    )
                } else it
            }
        }

        override suspend fun clearAll() {
            items.value = emptyList()
        }
    }

    private class FakeBookmarkDao : BookmarkDao {
        val bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
        private var autoId = 1L

        override fun getBookmarksForMedia(mediaUri: String): Flow<List<BookmarkEntity>> {
            return bookmarks.map { list -> list.filter { it.mediaUri == mediaUri } }
        }

        override suspend fun insertBookmark(bookmark: BookmarkEntity): Long {
            val assignedId = if (bookmark.id == 0L) autoId++ else bookmark.id
            val entity = bookmark.copy(id = assignedId)
            bookmarks.value = bookmarks.value + entity
            return assignedId
        }

        override suspend fun deleteBookmark(bookmark: BookmarkEntity) {
            bookmarks.value = bookmarks.value.filter { it.id != bookmark.id }
        }

        override suspend fun deleteBookmarkById(id: Long) {
            bookmarks.value = bookmarks.value.filter { it.id != id }
        }

        override suspend fun deleteBookmarksForMedia(mediaUri: String) {
            bookmarks.value = bookmarks.value.filter { it.mediaUri != mediaUri }
        }

        override suspend fun remapMediaUri(oldUri: String, newUri: String) {
            bookmarks.value = bookmarks.value.map { if (it.mediaUri == oldUri) it.copy(mediaUri = newUri) else it }
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

    @Test
    fun removeVideoFromLibrary_deletesFromDao() = runTest(testDispatcher) {
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
            mimeType = "video/mp4"
        )
        fakeDao.insertOrUpdate(item)

        repo.removeVideoFromLibrary("content://media/1")

        val result = fakeDao.getVideoByUri("content://media/1")
        assertEquals(null, result)
    }

    @Test
    fun getLibraryStatistics_computesTotalsCorrectly() = runTest(testDispatcher) {
        val fakeDao = FakeMediaItemDao()
        val repo = MediaRepositoryImpl(fakeDao, testDispatcher)

        val item1 = MediaItemEntity(
            id = 1,
            mediaStoreId = 101,
            uri = "content://media/1",
            title = "Movie 1",
            displayName = "movie1.mp4",
            durationMs = 60000L,
            sizeBytes = 1000L,
            mimeType = "video/mp4"
        )
        val item2 = MediaItemEntity(
            id = 2,
            mediaStoreId = 102,
            uri = "content://media/2",
            title = "Movie 2",
            displayName = "movie2.mp4",
            durationMs = 140000L,
            sizeBytes = 3000L,
            mimeType = "video/mp4"
        )
        fakeDao.insertOrUpdateAll(listOf(item1, item2))

        val stats = repo.getLibraryStatistics()
        assertEquals(2, stats.totalVideos)
        assertEquals(200000L, stats.totalDurationMs)
        assertEquals(4000L, stats.totalSizeBytes)
    }

    @Test
    fun bookmarks_insertQueryAndDelete_worksCorrectly() = runTest(testDispatcher) {
        val fakeMediaDao = FakeMediaItemDao()
        val fakeBookmarkDao = FakeBookmarkDao()
        val repo = MediaRepositoryImpl(fakeMediaDao, fakeBookmarkDao, testDispatcher)

        val uri = "content://media/test"

        repo.getBookmarks(uri).test {
            // Initial empty
            assertEquals(emptyList<org.aptura.player.domain.model.Bookmark>(), awaitItem())

            // Add first bookmark
            val id1 = repo.addBookmark(uri, 5000L, "Intro")
            val list1 = awaitItem()
            assertEquals(1, list1.size)
            assertEquals("Intro", list1.first().note)
            assertEquals(5000L, list1.first().timestampMs)

            // Add second bookmark
            val id2 = repo.addBookmark(uri, 12000L, "Climax")
            val list2 = awaitItem()
            assertEquals(2, list2.size)

            // Delete first bookmark
            repo.deleteBookmark(id1)
            val list3 = awaitItem()
            assertEquals(1, list3.size)
            assertEquals(id2, list3.first().id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun renameVideo_setsCustomTitle_shownByRepository() = runTest(testDispatcher) {
        val fakeMediaDao = FakeMediaItemDao()
        val repo = MediaRepositoryImpl(fakeMediaDao, testDispatcher)

        val item = MediaItemEntity(
            id = 1L,
            mediaStoreId = 100L,
            uri = "content://media/1",
            title = "Original Title",
            displayName = "Original Title.mp4",
            durationMs = 60000L,
            sizeBytes = 1024L,
            mimeType = "video/mp4",
            dateAddedSec = 1000L,
            folderPath = "Movies"
        )
        fakeMediaDao.insertOrUpdate(item)

        repo.renameVideo("content://media/1", "New Cinematic Title")

        val updated = repo.getVideoByUri("content://media/1")
        assertNotNull(updated)
        assertEquals("New Cinematic Title", updated?.title)
    }

    @Test
    fun savePlaybackPreferences_persistsPerVideoSettings_and_resetRestoresDefaults() = runTest(testDispatcher) {
        val fakeMediaDao = FakeMediaItemDao()
        val repo = MediaRepositoryImpl(fakeMediaDao, testDispatcher)

        val item = MediaItemEntity(
            id = 1L,
            mediaStoreId = 100L,
            uri = "content://media/sample_vid",
            title = "Sample Video",
            displayName = "sample.mp4",
            durationMs = 120000L,
            sizeBytes = 2048L,
            mimeType = "video/mp4",
            dateAddedSec = 1000L,
            folderPath = "Movies"
        )
        fakeMediaDao.insertOrUpdate(item)

        // 1. Save per-video preferences (FR-CTRL-012)
        repo.savePlaybackPreferences(
            uri = "content://media/sample_vid",
            resizeMode = "ZOOM",
            playbackSpeed = 1.5f,
            decoderStrategy = 2,
            audioDelayMs = 150L,
            subtitleDelayMs = -200L
        )

        val updated = repo.getVideoByUri("content://media/sample_vid")
        assertNotNull(updated)
        assertEquals("ZOOM", updated?.resizeMode)
        assertEquals(1.5f, updated?.playbackSpeed)
        assertEquals(2, updated?.decoderStrategy)
        assertEquals(150L, updated?.audioDelayMs)
        assertEquals(-200L, updated?.subtitleDelayMs)

        // 2. Reset preferences restores defaults
        repo.resetPlaybackPreferences("content://media/sample_vid")

        val resetItem = repo.getVideoByUri("content://media/sample_vid")
        assertNotNull(resetItem)
        assertEquals("FIT", resetItem?.resizeMode)
        assertEquals(1.0f, resetItem?.playbackSpeed)
        assertEquals(0, resetItem?.decoderStrategy)
        assertEquals(0L, resetItem?.audioDelayMs)
        assertEquals(0L, resetItem?.subtitleDelayMs)
    }
}
