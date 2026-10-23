package org.aptura.player.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for Bookmark entity persistence and queries (FR-CTRL-011).
 */
@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE mediaUri = :mediaUri ORDER BY timestampMs ASC")
    fun getBookmarksForMedia(mediaUri: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("UPDATE bookmarks SET note = :note WHERE id = :id")
    suspend fun updateBookmarkNote(id: Long, note: String)

    @Query("DELETE FROM bookmarks WHERE mediaUri = :mediaUri")
    suspend fun deleteBookmarksForMedia(mediaUri: String)

    /** Keeps bookmarks attached when a file moves/renames and its URI changes (SAF documents). */
    @Query("UPDATE bookmarks SET mediaUri = :newUri WHERE mediaUri = :oldUri")
    suspend fun remapMediaUri(oldUri: String, newUri: String)
}
