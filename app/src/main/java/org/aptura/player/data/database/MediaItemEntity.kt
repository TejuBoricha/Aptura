package org.aptura.player.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.aptura.player.domain.model.DomainMediaItem

/**
 * Room database entity representing a indexed video item in Aptura's local media library.
 * Designed for ultra-fast query performance with index coverage over folder path,
 * date added, and title (NFR-PERF-005, NFR-PERF-006).
 */
@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["folderPath"]),
        Index(value = ["dateAddedSec"]),
        Index(value = ["title"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mediaStoreId: Long,
    val uri: String,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val dateAddedSec: Long = 0L,
    val dateModifiedSec: Long = 0L,
    val folderPath: String = "",
    val playbackPositionMs: Long = 0L,
    val isCompleted: Boolean = false,
    val lastPlayedTimestamp: Long = 0L
)

fun MediaItemEntity.toDomainMediaItem(): DomainMediaItem {
    return DomainMediaItem(
        uri = uri,
        title = title.ifBlank { displayName },
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        width = width,
        height = height,
        frameRate = frameRate,
        dateAddedSec = dateAddedSec
    )
}
