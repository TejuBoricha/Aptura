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

    @Query("SELECT * FROM media_items WHERE isHidden = 0 ORDER BY dateAddedSec DESC")
    fun getAllVideosSortedByDate(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isHidden = 0 ORDER BY title ASC")
    fun getAllVideosSortedByTitle(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isHidden = 0 AND folderPath = :folderPath ORDER BY title ASC")
    fun getVideosByFolder(folderPath: String): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT folderPath FROM media_items WHERE isHidden = 0 AND folderPath != '' ORDER BY folderPath ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getVideoByMediaStoreId(mediaStoreId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE isHidden = 0 AND (title LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%' OR customTitle LIKE '%' || :query || '%') ORDER BY title ASC")
    fun searchVideos(query: String): Flow<List<MediaItemEntity>>

    @Query("UPDATE media_items SET playbackPositionMs = :positionMs, isCompleted = :isCompleted, lastPlayedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updatePlaybackPosition(uri: String, positionMs: Long, isCompleted: Boolean, timestamp: Long)

    @Query("UPDATE media_items SET resizeMode = :resizeMode, playbackSpeed = :playbackSpeed, decoderStrategy = :decoderStrategy, audioDelayMs = :audioDelayMs, subtitleDelayMs = :subtitleDelayMs WHERE uri = :uri")
    suspend fun updatePlaybackPreferences(
        uri: String,
        resizeMode: String,
        playbackSpeed: Float,
        decoderStrategy: Int,
        audioDelayMs: Long,
        subtitleDelayMs: Long
    )

    @Query("UPDATE media_items SET resizeMode = 'FIT', playbackSpeed = 1.0, decoderStrategy = 0, audioDelayMs = 0, subtitleDelayMs = 0 WHERE uri = :uri")
    suspend fun resetPlaybackPreferences(uri: String)

    @Query("UPDATE media_items SET customTitle = :customTitle WHERE uri = :uri")
    suspend fun updateCustomTitle(uri: String, customTitle: String?)

    @Query("UPDATE media_items SET isHidden = 1 WHERE uri = :uri")
    suspend fun hideByUri(uri: String)

    @Query("UPDATE media_items SET isHidden = 0 WHERE isHidden = 1")
    suspend fun restoreAllHidden()

    @Query("SELECT COUNT(*) FROM media_items WHERE isHidden = 1")
    fun getHiddenCount(): Flow<Int>

    @Query("SELECT * FROM media_items")
    suspend fun getAllVideosRaw(): List<MediaItemEntity>

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("UPDATE media_items SET isMissing = :isMissing WHERE id IN (:ids)")
    suspend fun updateMissingStatusByIds(ids: List<Long>, isMissing: Boolean)

    @Query("UPDATE media_items SET isMissing = :isMissing WHERE uri = :uri")
    suspend fun updateMissingStatusByUri(uri: String, isMissing: Boolean)

    @Query("SELECT * FROM media_items WHERE isMissing = 1 AND isHidden = 0")
    fun getMissingVideos(): Flow<List<MediaItemEntity>>

    @Query("DELETE FROM media_items WHERE isMissing = 1")
    suspend fun clearMissingVideos()

    @Query("DELETE FROM media_items WHERE mediaStoreId NOT IN (:existingMediaStoreIds)")
    suspend fun deleteRemovedVideos(existingMediaStoreIds: List<Long>)

    @Query("SELECT COUNT(*) FROM media_items WHERE isHidden = 0")
    suspend fun getMediaItemCount(): Int

    @Query("SELECT SUM(durationMs) FROM media_items WHERE isHidden = 0")
    suspend fun getTotalDurationMs(): Long?

    @Query("SELECT SUM(sizeBytes) FROM media_items WHERE isHidden = 0")
    suspend fun getTotalSizeBytes(): Long?

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
