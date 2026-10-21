package org.aptura.player.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for local media indexing and playback progress tracking.
 * Reactive queries return Kotlin Coroutines Flow for instant UI updates.
 */
@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items ORDER BY dateAddedSec DESC")
    fun getAllVideosSortedByDate(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items ORDER BY title ASC")
    fun getAllVideosSortedByTitle(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE folderPath = :folderPath ORDER BY title ASC")
    fun getVideosByFolder(folderPath: String): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT folderPath FROM media_items WHERE folderPath != '' ORDER BY folderPath ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getVideoByMediaStoreId(mediaStoreId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchVideos(query: String): Flow<List<MediaItemEntity>>

    @Query("UPDATE media_items SET playbackPositionMs = :positionMs, isCompleted = :isCompleted, lastPlayedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updatePlaybackPosition(uri: String, positionMs: Long, isCompleted: Boolean, timestamp: Long)

    @Query("SELECT * FROM media_items")
    suspend fun getAllVideosRaw(): List<MediaItemEntity>

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM media_items WHERE mediaStoreId NOT IN (:existingMediaStoreIds)")
    suspend fun deleteRemovedVideos(existingMediaStoreIds: List<Long>)

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaItemCount(): Int

    @Query("SELECT SUM(durationMs) FROM media_items")
    suspend fun getTotalDurationMs(): Long?

    @Query("SELECT SUM(sizeBytes) FROM media_items")
    suspend fun getTotalSizeBytes(): Long?

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
