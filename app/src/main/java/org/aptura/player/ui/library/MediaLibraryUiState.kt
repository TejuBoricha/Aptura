package org.aptura.player.ui.library

import androidx.compose.runtime.Immutable
import org.aptura.player.domain.model.DomainMediaItem
import java.util.Locale

/**
 * UI State for the Media Library Grid screen.
 * Follows Unidirectional Data Flow (UDF) with immutable data classes (coding-standards.md).
 */
@Immutable
sealed interface MediaLibraryUiState {
    data object Loading : MediaLibraryUiState

    data class Empty(
        val isFiltered: Boolean = false,
        val query: String = "",
        val isPermissionDenied: Boolean = false
    ) : MediaLibraryUiState

    data class Success(
        val videos: List<VideoCardUiModel>,
        val folders: List<String> = emptyList(),
        val selectedFolder: String? = null,
        val searchQuery: String = "",
        val sortOrder: LibrarySortOrder = LibrarySortOrder.DATE_ADDED_DESC,
        val totalCount: Int = videos.size,
        val isRefreshing: Boolean = false,
        val isLimitedMode: Boolean = false,
        /** Started-but-unfinished videos, most recently played first. Empty while searching or inside a folder. */
        val continueWatching: List<VideoCardUiModel> = emptyList(),
        /**
         * Whole-library figures for the wide-screen sidebar. They deliberately do not change when a folder or search
         * filter is active: the sidebar describes the collection, while the grid describes the current view.
         */
        val libraryVideoCount: Int = totalCount,
        val libraryDurationMs: Long = 0L,
        val librarySizeBytes: Long = 0L,
        /** Videos per folder path, including folders that are not in the current filtered result. */
        val folderCounts: Map<String, Int> = emptyMap(),
        /** What each folder holds and which video stands in for it, for the folder browser. Whole-library, like the counts. */
        val folderSummaries: Map<String, FolderSummary> = emptyMap()
    ) : MediaLibraryUiState
}

/** A folder at a glance: how much is in it and a picture to show for it. */
@Immutable
data class FolderSummary(
    val videoCount: Int,
    val totalBytes: Long,
    val totalDurationMs: Long,
    /** The video whose frame stands for the folder (the one added last that is not missing), or null if none can. */
    val coverUri: String? = null,
    val coverDateModifiedSec: Long = 0L
)

enum class LibrarySortOrder(val displayName: String) {
    DATE_ADDED_DESC("Recently added"),
    LAST_PLAYED_DESC("Recently played"),
    TITLE_ASC("Title (A–Z)"),
    DURATION_DESC("Longest first"),
    SIZE_DESC("Largest first")
}

@Immutable
data class VideoCardUiModel(
    val uri: String,
    val title: String,
    val durationFormatted: String,
    val fileSizeFormatted: String,
    val folderName: String,
    val progressFraction: Float = 0f,
    val isCompleted: Boolean = false,
    val isMissing: Boolean = false,
    val rawItem: DomainMediaItem
)

object MediaFormatters {

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "0:00"
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /** Compact collection runtime for the library sidebar: "11h 51m", "45m", "2h". */
    fun formatRuntime(durationMs: Long): String {
        if (durationMs <= 0L) return "0m"
        val totalMinutes = (durationMs + 59_999L) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    /**
     * "752 MB", "3.9 GB", "10.8 MB". One decimal only below 10, where it carries information; above that a decimal is
     * noise, and a grid that mixed "782 MB" with "753.4 MB" looked inconsistent.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        fun scaled(value: Double, unit: String): String =
            if (value < 10.0) {
                "${String.format(Locale.US, "%.1f", value).removeSuffix(".0")} $unit"
            } else {
                "${Math.round(value)} $unit"
            }

        return when {
            gb >= 1.0 -> scaled(gb, "GB")
            mb >= 1.0 -> scaled(mb, "MB")
            kb >= 1.0 -> "${Math.round(kb)} KB"
            else -> "$bytes B"
        }
    }

    /** The short quality name people use ("4K", "1080p"), or null when the size is unknown. */
    fun qualityLabel(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        return when {
            width >= 3840 || height >= 2160 -> "4K"
            width >= 2560 || height >= 1440 -> "1440p"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            width >= 854 || height >= 480 -> "480p"
            else -> "${height}p"
        }
    }

    fun calculateProgressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L || positionMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /** "12 min left", "1 h 5 min left", "Less than 1 min left" — what a viewer wants to know about a half-watched video. */
    fun formatRemaining(positionMs: Long, durationMs: Long): String {
        if (durationMs <= 0L) return "Unknown length"
        val remainingMinutes = ((durationMs - positionMs).coerceAtLeast(0L) + 59_999L) / 60_000L
        return when {
            remainingMinutes < 1L -> "Less than 1 min left"
            remainingMinutes < 60L -> "$remainingMinutes min left"
            remainingMinutes % 60L == 0L -> "${remainingMinutes / 60L} h left"
            else -> "${remainingMinutes / 60L} h ${remainingMinutes % 60L} min left"
        }
    }

    fun formatExactBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 bytes"
        return String.format(Locale.US, "%,d bytes", bytes)
    }

    fun formatTimestamp(seconds: Long): String {
        if (seconds <= 0L) return "Unknown"
        val date = java.util.Date(seconds * 1000L)
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
        return sdf.format(date)
    }

    fun formatResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "Unknown"
        val quality = when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            width >= 2560 || height >= 1440 -> "2K QHD"
            width >= 1920 || height >= 1080 -> "1080p FHD"
            width >= 1280 || height >= 720 -> "720p HD"
            width >= 854 || height >= 480 -> "480p SD"
            else -> "${height}p"
        }
        val ratio = when {
            Math.abs((width.toDouble() / height) - (16.0 / 9)) < 0.05 -> "16:9"
            Math.abs((width.toDouble() / height) - (4.0 / 3)) < 0.05 -> "4:3"
            Math.abs((width.toDouble() / height) - (21.0 / 9)) < 0.08 -> "21:9"
            else -> "$width:$height"
        }
        return "$width × $height ($quality · $ratio)"
    }

    fun formatContainer(mimeType: String?, title: String): String {
        val ext = title.substringAfterLast('.', "").uppercase()
        if (ext.isNotBlank() && ext.length in 2..5) {
            return ext
        }
        return when (mimeType?.lowercase()) {
            "video/mp4" -> "MP4"
            "video/x-matroska" -> "MKV (Matroska)"
            "video/webm" -> "WebM"
            "video/3gpp" -> "3GP"
            "video/quicktime" -> "MOV"
            "video/mp2ts" -> "MPEG-TS"
            "video/x-msvideo" -> "AVI"
            else -> mimeType?.substringAfter('/')?.uppercase() ?: "Unknown"
        }
    }

    fun cleanTitle(rawTitle: String): String = org.aptura.player.domain.model.cleanDisplayTitle(rawTitle)
}
