package org.aptura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * Media Details Bottom Sheet (SCR-009, FR-LIB-013).
 * Truthful, granular metadata inspection for a single video item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsSheet(
    item: DomainMediaItem,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApturaColors.SurfaceElevated1,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Video Details",
                style = ApturaTypography.headlineMedium,
                color = ApturaColors.TextHighEmphasis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata key-value rows
            MetadataRow("Title", item.title)
            MetadataRow("Location", item.folderPath.ifBlank { "Internal Storage" })
            MetadataRow("Duration", MediaFormatters.formatDuration(item.durationMs))
            MetadataRow("File Size", MediaFormatters.formatFileSize(item.sizeBytes))

            if (item.width > 0 && item.height > 0) {
                MetadataRow("Resolution", "${item.width} × ${item.height}")
            }

            if (!item.mimeType.isNullOrBlank()) {
                MetadataRow("Format / MIME", item.mimeType)
            }

            if (item.frameRate > 0f) {
                MetadataRow("Frame Rate", "%.2f fps".format(item.frameRate))
            }

            val status = when {
                item.isCompleted -> "Watched (Completed)"
                item.playbackPositionMs > 0 -> "In progress (${MediaFormatters.formatDuration(item.playbackPositionMs)})"
                else -> "Unwatched"
            }
            MetadataRow("Watch Status", status)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = ApturaColors.SurfaceElevated2)
            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onPlayClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApturaColors.AccentPrimary,
                        contentColor = ApturaColors.SurfacePureBlack
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Play", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = ApturaColors.TextHighEmphasis)
                }

                OutlinedButton(
                    onClick = onRemoveClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove", tint = ApturaColors.StatusError)
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ApturaTypography.bodyMedium,
            color = ApturaColors.TextMediumEmphasis
        )
        Text(
            text = value,
            style = ApturaTypography.titleSmall,
            color = ApturaColors.TextHighEmphasis,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Destructive Confirmation Dialog for removing an item from the library (SCR-018).
 * Never mutates or deletes the underlying physical file on device storage (DEC-001).
 */
@Composable
fun RemoveFromLibraryDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remove from Library?",
                style = ApturaTypography.headlineSmall,
                color = ApturaColors.TextHighEmphasis
            )
        },
        text = {
            Text(
                text = "Remove \"$itemTitle\" from Aptura's media library index?\n\nYour actual video file on storage will NOT be deleted.",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApturaColors.StatusError,
                    contentColor = ApturaColors.TextHighEmphasis
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ApturaColors.TextMediumEmphasis)
            }
        },
        containerColor = ApturaColors.SurfaceElevated2,
        shape = RoundedCornerShape(16.dp)
    )
}
