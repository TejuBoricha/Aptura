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
    val lastPlayedTimestamp: Long = 0L,
    val isMissing: Boolean = false,
    val resizeMode: String = "FIT",
    val playbackSpeed: Float = 1.0f,
    /** The decoder the USER picked for this video; only meaningful when [decoderOverridden] is true. */
    val decoderStrategy: Int = 0,
    /** True once the user chose a decoder for this video. False = follow the default in Settings. */
    val decoderOverridden: Boolean = false,
    val audioDelayMs: Long = 0L,
    val subtitleDelayMs: Long = 0L,
    /** User-chosen title. Survives rescans (the scanner only owns [title]); null = use the scanned title. */
    val customTitle: String? = null,
    /** Hidden from the library by the user. Survives rescans until the user restores it. */
    val isHidden: Boolean = false
) {
    /**
     * Columns the *user* owns (as opposed to the scanner). A rescan must carry every one of these over
     * from the existing row, otherwise REPLACE-on-conflict silently resets them (audit P0-05).
     */
    fun withUserDataFrom(existing: MediaItemEntity): MediaItemEntity = copy(
        id = existing.id,
        playbackPositionMs = existing.playbackPositionMs,
        isCompleted = existing.isCompleted,
        lastPlayedTimestamp = existing.lastPlayedTimestamp,
        resizeMode = existing.resizeMode,
        playbackSpeed = existing.playbackSpeed,
        decoderStrategy = existing.decoderStrategy,
        decoderOverridden = existing.decoderOverridden,
        audioDelayMs = existing.audioDelayMs,
        subtitleDelayMs = existing.subtitleDelayMs,
        customTitle = existing.customTitle,
        isHidden = existing.isHidden
    )
}

fun MediaItemEntity.toDomainMediaItem(): DomainMediaItem {
    return DomainMediaItem(
        uri = uri,
        title = customTitle?.takeIf { it.isNotBlank() } ?: title.ifBlank { displayName },
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        width = width,
        height = height,
        frameRate = frameRate,
        dateAddedSec = dateAddedSec,
        dateModifiedSec = dateModifiedSec,
        folderPath = folderPath,
        playbackPositionMs = playbackPositionMs,
        isCompleted = isCompleted,
        isMissing = isMissing,
        resizeMode = resizeMode,
        playbackSpeed = playbackSpeed,
        decoderStrategy = if (decoderOverridden) decoderStrategy else null,
        audioDelayMs = audioDelayMs,
        subtitleDelayMs = subtitleDelayMs
    )
}
