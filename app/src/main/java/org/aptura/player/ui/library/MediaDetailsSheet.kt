package org.aptura.player.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aptura.player.core.media.MediaMetadataInspector
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * Media Details Bottom Sheet (SCR-009, FR-LIB-013).
 * Truthful, granular metadata inspection with full path copy, video/audio codec specs,
 * HDR/channel detection, subtitle tracks, playback progress reset, and real Android storage contracts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsSheet(
    item: DomainMediaItem,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayAudioOnlyClick: () -> Unit = {},
    onOpenWithClick: () -> Unit = {},
    onShareClick: () -> Unit,
    onResetProgressClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onHideClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val storagePath = MediaFormatters.resolveStoragePath(item.folderPath, item.title)

    val extended = remember(item.uri) {
        try {
            MediaMetadataInspector.inspect(context, Uri.parse(item.uri))
        } catch (_: Throwable) {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApturaColors.SurfaceElevated1,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Row: Title & Container Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Video Details & Specs",
                    style = ApturaTypography.headlineMedium,
                    color = ApturaColors.TextHighEmphasis
                )

                val container = MediaFormatters.formatContainer(item.mimeType, item.title)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ApturaColors.AccentSubtle,
                    border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = container,
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.AccentPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata: Title & Path Card (with Quick Rename & Copy)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ApturaColors.SurfaceElevated2
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TITLE",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextMediumEmphasis
                        )
                        IconButton(
                            onClick = {
                                onDismiss()
                                onRenameClick()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename Video",
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        style = ApturaTypography.titleSmall,
                        color = ApturaColors.TextHighEmphasis,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ApturaColors.SurfaceElevated3)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STORAGE PATH",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextMediumEmphasis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = storagePath,
                                style = ApturaTypography.bodyMedium,
                                color = ApturaColors.TextHighEmphasis
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Video Path", storagePath)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Storage path copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Path",
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Video Specifications
            SectionHeader("VIDEO SPECIFICATIONS")
            val videoCodec = extended?.videoCodec ?: MediaFormatters.formatContainer(item.mimeType, item.title)
            MetadataRow("Video Codec", videoCodec)

            if (item.width > 0 && item.height > 0) {
                MetadataRow("Resolution", MediaFormatters.formatResolution(item.width, item.height))
            }

            if (item.frameRate > 0f) {
                MetadataRow("Frame Rate", "%.2f fps".format(item.frameRate))
            }

            val bitrateVal = extended?.videoBitrate ?: extended?.bitRateTotal
            if (bitrateVal != null && bitrateVal > 0L) {
                MetadataRow("Video Bitrate", MediaMetadataInspector.formatBitrate(bitrateVal))
            }

            if (extended != null && extended.colorTransfer != null) {
                MetadataRow("Dynamic Range", extended.colorTransfer)
            } else if (extended?.isHdr == true) {
                MetadataRow("Dynamic Range", "HDR10 (10-bit High Dynamic Range)")
            }

            if (extended?.colorStandard != null) {
                MetadataRow("Color Space", extended.colorStandard)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Audio Streams
            SectionHeader("AUDIO STREAMS")
            if (extended != null && extended.audioTracks.isNotEmpty()) {
                extended.audioTracks.forEachIndexed { idx, track ->
                    val channelStr = MediaMetadataInspector.formatChannels(track.channelCount)
                    val sampleRateStr = MediaMetadataInspector.formatSampleRate(track.sampleRate)
                    val brStr = if (track.bitrate != null && track.bitrate > 0L) " • ${MediaMetadataInspector.formatBitrate(track.bitrate)}" else ""
                    val langStr = if (!track.language.isNullOrBlank()) " (${track.language})" else ""
                    MetadataRow(
                        label = if (extended.audioTracks.size > 1) "Audio Track ${idx + 1}$langStr" else "Audio Stream$langStr",
                        value = "${track.codec} • $channelStr • $sampleRateStr$brStr"
                    )
                }
            } else {
                MetadataRow("Audio Stream", "Stereo (System Decoded)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Subtitles & Tracks
            SectionHeader("SUBTITLE TRACKS")
            if (extended != null && extended.subtitleTracks.isNotEmpty()) {
                extended.subtitleTracks.forEachIndexed { idx, sub ->
                    val langStr = if (!sub.language.isNullOrBlank()) " (${sub.language})" else ""
                    MetadataRow("Subtitle Track ${idx + 1}$langStr", sub.format)
                }
            } else {
                MetadataRow("Subtitles", "No embedded subtitles")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: File & Playback Status
            SectionHeader("FILE & STORAGE")
            MetadataRow("File Size", "${MediaFormatters.formatFileSize(item.sizeBytes)}  (${MediaFormatters.formatExactBytes(item.sizeBytes)})")
            MetadataRow("Duration", MediaFormatters.formatDuration(item.durationMs))

            if (!item.mimeType.isNullOrBlank()) {
                MetadataRow("MIME Type", item.mimeType)
            }

            if (item.dateAddedSec > 0L) {
                MetadataRow("Date Added", MediaFormatters.formatTimestamp(item.dateAddedSec))
            }

            if (item.dateModifiedSec > 0L) {
                MetadataRow("Date Modified", MediaFormatters.formatTimestamp(item.dateModifiedSec))
            }

            // Interactive Watch Status Row
            val hasProgress = item.playbackPositionMs > 0L || item.isCompleted
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Watch Status",
                    style = ApturaTypography.bodyMedium,
                    color = ApturaColors.TextMediumEmphasis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusText = when {
                        item.isCompleted -> "Watched (Completed)"
                        item.playbackPositionMs > 0 -> "In progress (${MediaFormatters.formatDuration(item.playbackPositionMs)})"
                        else -> "Unwatched"
                    }
                    Text(
                        text = statusText,
                        style = ApturaTypography.titleSmall,
                        color = if (item.isCompleted) ApturaColors.StatusSuccess else ApturaColors.TextHighEmphasis,
                        fontWeight = FontWeight.Medium
                    )

                    if (hasProgress) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ApturaColors.SurfaceElevated3,
                            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            onClick = onResetProgressClick
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Reset",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.AccentPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = ApturaColors.SurfaceElevated2)
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Primary Play & Audio-Only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onPlayClick()
                    },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApturaColors.AccentPrimary,
                        contentColor = ApturaColors.SurfacePureBlack
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Play", style = ApturaTypography.labelLarge, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPlayAudioOnlyClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ApturaColors.TextHighEmphasis
                    ),
                    border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3)
                ) {
                    Icon(imageVector = Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp), tint = ApturaColors.AccentPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Audio Only", style = ApturaTypography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Secondary Management Row 1 (Rename, Open With)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onRenameClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApturaColors.TextHighEmphasis),
                    border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = ApturaColors.AccentPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Rename", style = ApturaTypography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenWithClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApturaColors.TextHighEmphasis),
                    border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = ApturaColors.TextMediumEmphasis)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Open With", style = ApturaTypography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Secondary Management Row 2 (Share, Hide, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApturaColors.TextHighEmphasis),
                    border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = ApturaColors.TextHighEmphasis)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share", style = ApturaTypography.labelMedium)
                }

                OutlinedButton(
                    onClick = onHideClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApturaColors.TextMediumEmphasis),
                    border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = ApturaColors.TextMediumEmphasis)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Hide", style = ApturaTypography.labelMedium)
                }

                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApturaColors.StatusError),
                    border = BorderStroke(1.dp, ApturaColors.StatusError.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = ApturaColors.StatusError)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Delete", style = ApturaTypography.labelMedium, color = ApturaColors.StatusError)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = ApturaColors.AccentPrimary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ApturaTypography.bodyMedium,
            color = ApturaColors.TextMediumEmphasis,
            modifier = Modifier.weight(1f)
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
 * Modern Rename Video Dialog (FR-LIB-013).
 * Renames video files and title metadata across Scoped Storage, SAF, and Room database.
 */
@Composable
fun RenameVideoDialog(
    itemTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(itemTitle) }
    val isBlank = text.trim().isBlank()
    val hasIllegalChars = text.any { it in listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|') }
    val isValid = !isBlank && !hasIllegalChars && text.trim() != itemTitle.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Video",
                style = ApturaTypography.headlineSmall,
                color = ApturaColors.TextHighEmphasis
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a new title for this video file:",
                    style = ApturaTypography.bodyMedium,
                    color = ApturaColors.TextMediumEmphasis
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = hasIllegalChars,
                    supportingText = if (hasIllegalChars) {
                        { Text("Title cannot contain / \\ : * ? \" < > |", color = ApturaColors.StatusError) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ApturaColors.AccentPrimary,
                        unfocusedBorderColor = ApturaColors.SurfaceElevated3,
                        focusedTextColor = ApturaColors.TextHighEmphasis,
                        unfocusedTextColor = ApturaColors.TextHighEmphasis,
                        cursorColor = ApturaColors.AccentPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(text.trim())
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApturaColors.AccentPrimary,
                    contentColor = ApturaColors.SurfacePureBlack
                )
            ) {
                Text("Rename", fontWeight = FontWeight.Bold)
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

/**
 * Native Android Platform Deletion Confirmation Dialog.
 * Transparently informs the user that the file will be physically removed from storage,
 * freeing device memory/space via Android's platform consent contract (DEC-001).
 */
@Composable
fun DeleteFromDeviceDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete from Device?",
                style = ApturaTypography.headlineSmall,
                color = ApturaColors.TextHighEmphasis
            )
        },
        text = {
            Text(
                text = "Permanently delete \"$itemTitle\" from your device's physical storage?\n\nThis will free up storage space. Android will prompt you to confirm the deletion.",
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
                Text("Delete Permanently")
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

/**
 * Non-destructive Hide from Library Dialog.
 * Leaves the physical file intact on device storage while removing it from Aptura's index.
 */
@Composable
fun HideFromLibraryDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hide from Library?",
                style = ApturaTypography.headlineSmall,
                color = ApturaColors.TextHighEmphasis
            )
        },
        text = {
            Text(
                text = "Hide \"$itemTitle\" from Aptura's media index?\n\nYour actual video file on storage will NOT be deleted.",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApturaColors.SurfaceElevated3,
                    contentColor = ApturaColors.TextHighEmphasis
                )
            ) {
                Text("Hide")
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

/**
 * Backward compatibility alias for existing callers.
 */
@Composable
fun RemoveFromLibraryDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    HideFromLibraryDialog(
        itemTitle = itemTitle,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
