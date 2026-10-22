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
        val isLimitedMode: Boolean = false
    ) : MediaLibraryUiState
}

enum class LibrarySortOrder(val displayName: String) {
    DATE_ADDED_DESC("Recently Added"),
    TITLE_ASC("Title (A-Z)")
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

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> {
                val formatted = String.format(Locale.US, "%.1f", gb)
                "${formatted.removeSuffix(".0")} GB"
            }
            mb >= 1.0 -> {
                val formatted = String.format(Locale.US, "%.1f", mb)
                "${formatted.removeSuffix(".0")} MB"
            }
            kb >= 1.0 -> "${Math.round(kb)} KB"
            else -> "$bytes B"
        }
    }

    fun calculateProgressFraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L || positionMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
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

    fun resolveStoragePath(folderPath: String, title: String): String {
        val cleanFolder = folderPath.trim()
        return when {
            cleanFolder.startsWith("/") -> if (cleanFolder.endsWith(title)) cleanFolder else "${cleanFolder.trimEnd('/')}/$title"
            cleanFolder.isNotBlank() && cleanFolder != "Internal Storage" -> "/storage/emulated/0/$cleanFolder/$title"
            else -> "/storage/emulated/0/Movies/$title"
        }
    }

    fun cleanTitle(rawTitle: String): String {
        if (rawTitle.isBlank()) return "Untitled"
        val stripped = rawTitle.replace(Regex("\\.(mp4|mkv|webm|avi|mov|ts|m4v|3gp)$", RegexOption.IGNORE_CASE), "")
        return stripped.replace('_', ' ').trim()
    }
}
