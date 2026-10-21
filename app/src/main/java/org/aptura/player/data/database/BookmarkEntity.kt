package org.aptura.player.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.aptura.player.domain.model.Bookmark

/**
 * Room database entity representing a saved bookmark for a video (FR-CTRL-011).
 */
@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["mediaUri"]),
        Index(value = ["mediaUri", "timestampMs"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mediaUri: String,
    val timestampMs: Long,
    val note: String = "",
    val createdAtSec: Long = System.currentTimeMillis() / 1000L
)

fun BookmarkEntity.toDomainBookmark(): Bookmark {
    return Bookmark(
        id = id,
        mediaUri = mediaUri,
        timestampMs = timestampMs,
        note = note,
        createdAtSec = createdAtSec
    )
}
