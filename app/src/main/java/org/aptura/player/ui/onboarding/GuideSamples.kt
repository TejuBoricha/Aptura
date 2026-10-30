package org.aptura.player.ui.onboarding

import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.ui.library.MediaFormatters
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.VideoCardUiModel

/*
 * The videos the welcome guide shows. Before the first scan there is nothing of the person's own to show, so the guide
 * brings four samples. They are real library cards (the same model the library builds from the device), with drawn
 * film frames for pictures ([GuideStill]), so what the guide shows is exactly what the library draws.
 */

/** A video the guide shows: its card, and which drawn film frame is its picture, or null to use the video's own frame. */
internal class GuideSample(val card: VideoCardUiModel, val still: Int?)

private fun sample(
    title: String,
    folder: String,
    durationMs: Long,
    sizeBytes: Long,
    still: Int,
    progress: Float = 0f,
    width: Int = 1920,
    height: Int = 1080
): GuideSample {
    val item = DomainMediaItem(
        uri = "guide://sample/${title.lowercase().replace(' ', '-')}",
        title = title,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        mimeType = "video/mp4",
        width = width,
        height = height,
        folderPath = folder,
        playbackPositionMs = (durationMs * progress).toLong()
    )
    return GuideSample(
        card = VideoCardUiModel(
            uri = item.uri,
            title = item.title,
            durationFormatted = MediaFormatters.formatDuration(durationMs),
            fileSizeFormatted = MediaFormatters.formatFileSize(sizeBytes),
            folderName = folder,
            progressFraction = progress,
            rawItem = item
        ),
        still = still
    )
}

internal val GuideSamples: List<GuideSample> = listOf(
    sample("Beach day", "Camera", durationMs = 252_000L, sizeBytes = 185_000_000L, still = 0, progress = 0.62f),
    sample("Lecture 04", "Download", durationMs = 2_910_000L, sizeBytes = 612_000_000L, still = 2),
    sample("Trail run", "Camera", durationMs = 168_000L, sizeBytes = 124_000_000L, still = 3),
    sample("Movie night", "Movies", durationMs = 5_527_000L, sizeBytes = 1_980_000_000L, still = 4)
)

/** What the library page of the guide shows: four videos, and the folders and counts for the folder bar. */
internal class GuideLibrary(
    val videos: List<GuideSample>,
    val folders: List<String>,
    val folderCounts: Map<String, Int>,
    val totalCount: Int
)

/** The samples, for the first run and for a library too small to fill the picture. */
internal val SampleLibrary = GuideLibrary(
    videos = GuideSamples,
    folders = listOf("Camera", "Download", "Movies"),
    folderCounts = mapOf("Camera" to 128, "Download" to 41, "Movies" to 36),
    totalCount = 205
)

private const val REAL_VIDEOS_NEEDED = 4
private const val REAL_FOLDERS_SHOWN = 8

/**
 * The person's own library for the guide's library page, when the guide is replayed from Settings and the library has
 * enough in it to fill the picture. Their videos show their own frames and their own folders, so the page is a picture
 * of *their* library and nothing in it is a stand-in. Null (use the samples) when there are fewer than four playable
 * videos, or while the library is filtered to a folder or a search, so the picture never contradicts its folder bar.
 */
internal fun guideLibraryFrom(state: MediaLibraryUiState.Success): GuideLibrary? {
    if (state.selectedFolder != null || state.searchQuery.isNotBlank()) return null
    val playable = state.videos.filter { !it.isMissing }.take(REAL_VIDEOS_NEEDED)
    if (playable.size < REAL_VIDEOS_NEEDED) return null
    return GuideLibrary(
        videos = playable.map { GuideSample(card = it, still = null) },
        folders = state.folders.take(REAL_FOLDERS_SHOWN),
        folderCounts = state.folderCounts,
        totalCount = state.libraryVideoCount.takeIf { it > 0 } ?: state.totalCount
    )
}
