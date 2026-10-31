package org.aptura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.ApturaOutlinedButton

internal const val FOLDER_DESTINATION_SHEET_TAG = "folder_destination_sheet"
internal const val BROWSE_STORAGE_BUTTON_TAG = "browse_storage_button"

/**
 * An in-app destination picker for moving or copying videos.
 * Displays all known in-app folders with counts and preview thumbnails,
 * plus a fallback button to browse system storage via SAF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderDestinationSheet(
    title: String,
    folders: List<String>,
    labels: Map<String, String>,
    summaries: Map<String, FolderSummary>,
    currentFolder: String?,
    onSelectDestination: (String) -> Unit,
    onBrowseStorage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ApturaColors.SurfaceElevated1,
        modifier = modifier.testTag(FOLDER_DESTINATION_SHEET_TAG)
    ) {
        FolderDestinationSheetContent(
            title = title,
            folders = folders,
            labels = labels,
            summaries = summaries,
            currentFolder = currentFolder,
            onSelectDestination = onSelectDestination,
            onBrowseStorage = onBrowseStorage
        )
    }
}

@Composable
internal fun FolderDestinationSheetContent(
    title: String,
    folders: List<String>,
    labels: Map<String, String>,
    summaries: Map<String, FolderSummary>,
    currentFolder: String?,
    onSelectDestination: (String) -> Unit,
    onBrowseStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .heightIn(max = maxHeight * 0.90f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = null,
                    tint = ApturaColors.AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = ApturaTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Choose a destination folder",
                        style = ApturaTypography.bodySmall,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val entries = folders.map { folder ->
                FolderEntry(
                    folder = folder,
                    label = labels[folder] ?: friendlyFolderName(folder),
                    summary = summaries[folder]
                )
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No in-app folders found",
                        style = ApturaTypography.bodyMedium,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(entries, key = { it.folder ?: "" }) { entry ->
                        val isCurrent = entry.folder != null && entry.folder == currentFolder
                        DestinationFolderRow(
                            entry = entry,
                            isCurrentFolder = isCurrent,
                            onClick = {
                                if (!isCurrent && entry.folder != null) {
                                    onSelectDestination(entry.folder)
                                }
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = ApturaColors.BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

            // Fallback option to browse external device storage
            ApturaOutlinedButton(
                text = "Browse device storage...",
                icon = Icons.Outlined.FolderOpen,
                onClick = onBrowseStorage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag(BROWSE_STORAGE_BUTTON_TAG)
            )
        }
    }
}

@Composable
private fun DestinationFolderRow(
    entry: FolderEntry,
    isCurrentFolder: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val background = if (isCurrentFolder) ApturaColors.SurfaceBase.copy(alpha = 0.5f) else ApturaColors.SurfaceElevated2
    val borderCol = if (isCurrentFolder) ApturaColors.BorderSubtle.copy(alpha = 0.5f) else ApturaColors.BorderSubtle

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, borderCol, shape)
            .clickable(enabled = !isCurrentFolder, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail / icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ApturaColors.SurfaceElevated1),
            contentAlignment = Alignment.Center
        ) {
            val cover = entry.summary?.coverUri
            if (cover != null) {
                VideoThumbnail(
                    uri = cover,
                    dateModifiedSec = entry.summary.coverDateModifiedSec,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isCurrentFolder) ApturaColors.TextLowEmphasis else ApturaColors.AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.label,
                    style = ApturaTypography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentFolder) ApturaColors.TextMediumEmphasis else ApturaColors.TextHighEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentFolder) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "(Current)",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextLowEmphasis
                    )
                }
            }
            entry.summary?.let { summary ->
                Text(
                    text = buildString {
                        append(if (summary.videoCount == 1) "1 video" else "${summary.videoCount} videos")
                        if (summary.totalBytes > 0L) {
                            append("  ·  ${MediaFormatters.formatFileSize(summary.totalBytes)}")
                        }
                    },
                    style = ApturaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = ApturaColors.TextMediumEmphasis
                )
            }
        }
    }
}
