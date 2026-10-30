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
import androidx.compose.foundation.layout.windowInsetsPadding
import org.aptura.player.ui.components.StableNavigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import org.aptura.player.ui.components.ApturaModalBottomSheet
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.core.media.MediaExtendedMetadata
import org.aptura.player.core.media.MediaLanguageLabels
import org.aptura.player.core.media.MediaMetadataInspector
import org.aptura.player.core.media.MediaPathResolver
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import org.aptura.player.ui.components.ApturaButton
import org.aptura.player.ui.components.ApturaOutlinedButton
import org.aptura.player.ui.components.ApturaTextButton
import org.aptura.player.ui.components.NoticeBanner
import org.aptura.player.ui.components.ApturaDialog
import org.aptura.player.ui.components.ApturaTone
import org.aptura.player.ui.components.DialogAlternative

/**
 * Media Details Content (FR-LIB-013, R-A02, R-A09).
 * The video first (its frame, title and the facts people scan for), then what you can do with it, then the full
 * specification. Play used to sit under twenty rows of codec data; now it is the first thing under the title.
 */
@Composable
fun MediaDetailsContent(
    item: DomainMediaItem,
    onClose: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayAudioOnlyClick: () -> Unit = {},
    onOpenWithClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onResetProgressClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onHideClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onRemoveMissingClick: () -> Unit = {},
    onPlayNextClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit = {},
    showCloseButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Both lookups touch the file system / MediaStore, so they run off the main thread. `null` = still loading.
    val resolvedPath by produceState<DetailLookup<String>?>(null, item.uri) {
        value = DetailLookup(withContext(Dispatchers.IO) { MediaPathResolver.resolve(context, item.uri) })
    }
    val inspection by produceState<MediaExtendedMetadata?>(null, item.uri) {
        value = withContext(Dispatchers.IO) {
            try {
                MediaMetadataInspector.inspect(context, Uri.parse(item.uri))
            } catch (_: Throwable) {
                MediaExtendedMetadata(inspected = false)
            }
        }
    }
    val extended: MediaExtendedMetadata? = inspection
    val storagePath: String? = resolvedPath?.value
    val storagePathLabel = when {
        resolvedPath == null -> "Locating file…"
        storagePath == null -> "Unknown"
        else -> storagePath
    }
    val title = MediaFormatters.cleanTitle(item.title)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(StableNavigationBars)
            .padding(horizontal = 20.dp)
    ) {
        if (showCloseButton) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp).offset(x = (-12).dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Details",
                    tint = ApturaColors.TextMediumEmphasis
                )
            }
        }

        // ---- Identity -------------------------------------------------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            val thumbShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .aspectRatio(16f / 9f)
                    .clip(thumbShape)
                    .background(ApturaColors.SurfaceElevated2)
                    .border(1.dp, ApturaColors.BorderSubtle, thumbShape)
            ) {
                VideoThumbnail(
                    uri = item.uri,
                    dateModifiedSec = item.dateModifiedSec,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.durationMs > 0L) {
                    ThumbnailPill(
                        text = MediaFormatters.formatDuration(item.durationMs),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ApturaTypography.titleLarge,
                    color = ApturaColors.TextHighEmphasis,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpecChip(MediaFormatters.formatContainer(item.mimeType, item.title).substringBefore(' '))
                    MediaFormatters.qualityLabel(item.width, item.height)?.let { SpecChip(it) }
                    SpecChip(MediaFormatters.formatFileSize(item.sizeBytes))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---- Actions --------------------------------------------------------------------------------------------
        if (item.isMissing) {
            MissingVideoNotice(onRemoveMissingClick = onRemoveMissingClick)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ApturaButton(
                    text = if (item.playbackPositionMs >= 1000L && !item.isCompleted) "Resume" else "Play",
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayClick,
                    modifier = Modifier.weight(1f)
                )
                ApturaOutlinedButton(
                    text = "Audio only",
                    icon = Icons.Default.Headphones,
                    onClick = onPlayAudioOnlyClick,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                QuickAction(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next", onPlayNextClick, Modifier.weight(1f))
                QuickAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Queue", onAddToQueueClick, Modifier.weight(1f))
                QuickAction(Icons.Default.Edit, "Rename", onRenameClick, Modifier.weight(1f))
                QuickAction(Icons.Default.Share, "Share", onShareClick, Modifier.weight(1f))
                QuickAction(Icons.AutoMirrored.Filled.OpenInNew, "Open with", onOpenWithClick, Modifier.weight(1f))
            }
        }

        // ---- Progress -------------------------------------------------------------------------------------------
        val hasProgress = item.playbackPositionMs >= 1000L || item.isCompleted
        DetailsSection("Progress") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        item.isCompleted -> "Watched"
                        item.playbackPositionMs >= 1000L ->
                            "Stopped at ${MediaFormatters.formatDuration(item.playbackPositionMs)} of " +
                                MediaFormatters.formatDuration(item.durationMs)
                        else -> "Not started"
                    },
                    style = ApturaTypography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = if (item.isCompleted) ApturaColors.StatusSuccess else ApturaColors.TextHighEmphasis,
                    modifier = Modifier.weight(1f)
                )
                if (hasProgress) {
                    ApturaTextButton(text = "Reset", onClick = onResetProgressClick, tone = ApturaTone.Accent)
                }
            }
        }

        // ---- Location -------------------------------------------------------------------------------------------
        DetailsSection("Location") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = storagePathLabel,
                    style = ApturaTypography.bodyMedium,
                    color = ApturaColors.TextHighEmphasis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    enabled = storagePath != null,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Video Path", storagePath.orEmpty())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Storage path copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Path",
                        tint = ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ---- Video ----------------------------------------------------------------------------------------------
        DetailsSection("Video") {
            MetadataRow("Codec", extended?.videoCodec ?: unknownOrLoading(extended))
            if (item.width > 0 && item.height > 0) {
                MetadataRow("Resolution", MediaFormatters.formatResolution(item.width, item.height))
            }
            if (item.frameRate > 0f) {
                MetadataRow("Frame rate", "%.2f fps".format(item.frameRate))
            }
            // Label what the number really is: the container's overall bitrate is not the video stream's bitrate.
            val streamBitrate = extended?.videoBitrate?.takeIf { it > 0L }
            val overallBitrate = extended?.bitRateTotal?.takeIf { it > 0L }
            when {
                streamBitrate != null -> MetadataRow("Video bitrate", MediaMetadataInspector.formatBitrate(streamBitrate))
                overallBitrate != null -> MetadataRow("Overall bitrate", MediaMetadataInspector.formatBitrate(overallBitrate))
            }
            if (extended != null && extended.colorTransfer != null) {
                MetadataRow("Dynamic range", extended.colorTransfer)
            } else if (extended?.isHdr == true) {
                MetadataRow("Dynamic range", "HDR10 (10-bit)")
            }
            if (extended?.colorStandard != null) {
                MetadataRow("Color space", extended.colorStandard)
            }
        }

        // ---- Audio ----------------------------------------------------------------------------------------------
        DetailsSection("Audio") {
            if (extended != null && extended.audioTracks.isNotEmpty()) {
                extended.audioTracks.forEachIndexed { idx, track ->
                    val channelStr = MediaMetadataInspector.formatChannels(track.channelCount)
                    val sampleRateStr = MediaMetadataInspector.formatSampleRate(track.sampleRate)
                    val brStr = if (track.bitrate != null && track.bitrate > 0L) " · ${MediaMetadataInspector.formatBitrate(track.bitrate)}" else ""
                    val langStr = MediaLanguageLabels.displayName(track.language)?.let { " ($it)" } ?: ""
                    MetadataRow(
                        label = if (extended.audioTracks.size > 1) "Track ${idx + 1}$langStr" else "Stream$langStr",
                        value = "${track.codec} · $channelStr · $sampleRateStr$brStr"
                    )
                }
            } else {
                MetadataRow("Stream", if (extended?.inspected == true) "None found" else unknownOrLoading(extended))
            }
        }

        // ---- Subtitles ------------------------------------------------------------------------------------------
        DetailsSection("Subtitles") {
            if (extended != null && extended.subtitleTracks.isNotEmpty()) {
                extended.subtitleTracks.forEachIndexed { idx, sub ->
                    val langStr = MediaLanguageLabels.displayName(sub.language)?.let { " ($it)" } ?: ""
                    MetadataRow("Track ${idx + 1}$langStr", sub.format)
                }
            } else {
                MetadataRow("Embedded", if (extended?.inspected == true) "None" else unknownOrLoading(extended))
            }
        }

        // ---- File -----------------------------------------------------------------------------------------------
        DetailsSection("File") {
            MetadataRow("Size", "${MediaFormatters.formatFileSize(item.sizeBytes)} (${MediaFormatters.formatExactBytes(item.sizeBytes)})")
            MetadataRow("Duration", MediaFormatters.formatDuration(item.durationMs))
            if (!item.mimeType.isNullOrBlank()) {
                MetadataRow("Format", "${MediaFormatters.formatContainer(item.mimeType, item.title)} · ${item.mimeType}")
            }
            if (item.dateAddedSec > 0L) {
                MetadataRow("Added", MediaFormatters.formatTimestamp(item.dateAddedSec))
            }
            if (item.dateModifiedSec > 0L) {
                MetadataRow("Modified", MediaFormatters.formatTimestamp(item.dateModifiedSec))
            }
        }

        // ---- Remove (kept apart from everything above, P2-03) ----------------------------------------------------
        if (!item.isMissing) {
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ApturaOutlinedButton(
                    text = "Hide",
                    icon = Icons.Default.VisibilityOff,
                    onClick = onHideClick,
                    modifier = Modifier.weight(1f)
                )
                ApturaOutlinedButton(
                    text = "Delete",
                    icon = Icons.Default.DeleteOutline,
                    tone = ApturaTone.Destructive,
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** A compact fact over the title ("MKV", "4K", "3.9 GB"). */
@Composable
private fun SpecChip(text: String) {
    Text(
        text = text,
        style = ApturaTypography.labelMedium,
        color = ApturaColors.TextMediumEmphasis,
        maxLines = 1,
        modifier = Modifier
            .background(ApturaColors.SurfaceElevated3, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/** An icon over a short label: the secondary actions as one even row instead of a wall of outlined buttons. */
@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(ApturaColors.SurfaceElevated2, CircleShape)
                .border(1.dp, ApturaColors.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ApturaColors.TextHighEmphasis, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = ApturaTypography.labelMedium.copy(fontFeatureSettings = ""),
            color = ApturaColors.TextMediumEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** A labelled group of rows on its own panel, the same grouping Settings uses. */
@Composable
private fun DetailsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(22.dp))
    Text(
        text = title.uppercase(),
        style = ApturaTypography.labelSmall.copy(letterSpacing = 1.4.sp),
        color = ApturaColors.TextMediumEmphasis,
        modifier = Modifier
            .padding(start = 4.dp, bottom = 8.dp)
            .semantics { heading() }
    )
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ApturaColors.SurfaceElevated2)
            .border(1.dp, ApturaColors.BorderSubtle, shape)
            .padding(vertical = 4.dp),
        content = content
    )
}

/**
 * A missing item is not a broken item: the file is gone, so every action that needs bytes is replaced by
 * the one action that makes sense. This keeps Play, Rename, Share and Delete from failing silently.
 */
@Composable
private fun MissingVideoNotice(onRemoveMissingClick: () -> Unit) {
    NoticeBanner(
        icon = Icons.Default.FolderOff,
        title = "File unavailable",
        message = "It may have been moved, deleted, or stored on a disconnected drive. Playback, rename, move and " +
            "share are off until the file returns.",
        tint = ApturaColors.StatusError
    ) {
        ApturaTextButton(text = "Remove from library", onClick = onRemoveMissingClick, tone = ApturaTone.Destructive)
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = ApturaTypography.bodyMedium,
            color = ApturaColors.TextMediumEmphasis,
            modifier = Modifier.width(112.dp)
        )
        Text(
            text = value,
            style = ApturaTypography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = ApturaColors.TextHighEmphasis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

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
    onRemoveMissingClick: () -> Unit = {},
    onPlayNextClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ApturaModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApturaColors.SurfaceElevated1,
        modifier = modifier
    ) {
        MediaDetailsContent(
            item = item,
            onClose = onDismiss,
            onPlayClick = {
                onDismiss()
                onPlayClick()
            },
            onPlayAudioOnlyClick = {
                onDismiss()
                onPlayAudioOnlyClick()
            },
            onOpenWithClick = {
                onDismiss()
                onOpenWithClick()
            },
            onShareClick = onShareClick,
            onResetProgressClick = onResetProgressClick,
            onRenameClick = {
                onDismiss()
                onRenameClick()
            },
            onHideClick = {
                onDismiss()
                onHideClick()
            },
            onDeleteClick = {
                onDismiss()
                onDeleteClick()
            },
            onRemoveMissingClick = {
                onDismiss()
                onRemoveMissingClick()
            },
            onPlayNextClick = {
                onDismiss()
                onPlayNextClick()
            },
            onAddToQueueClick = {
                onDismiss()
                onAddToQueueClick()
            },
            showCloseButton = false,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

/**
 * Rename Video Dialog (FR-LIB-013).
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

    ApturaDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Edit,
        title = "Rename video",
        message = "This changes the file name on your device. The extension is kept.",
        confirmLabel = "Rename",
        confirmEnabled = isValid,
        onConfirm = { if (isValid) onConfirm(text.trim()) }
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            isError = hasIllegalChars,
            textStyle = ApturaTypography.bodyLarge,
            supportingText = if (hasIllegalChars) {
                { Text("A name can't contain / \\ : * ? \" < > |", color = ApturaColors.StatusError) }
            } else null,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ApturaColors.AccentPrimary,
                unfocusedBorderColor = ApturaColors.BorderSubtle,
                focusedContainerColor = ApturaColors.SurfaceElevated1,
                unfocusedContainerColor = ApturaColors.SurfaceElevated1,
                focusedTextColor = ApturaColors.TextHighEmphasis,
                unfocusedTextColor = ApturaColors.TextHighEmphasis,
                cursorColor = ApturaColors.AccentPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Native Android Platform Deletion Confirmation Dialog.
 * Transparently informs the user that the file will be physically removed from storage,
 * freeing device memory/space via Android's platform consent contract (DEC-001).
 * When the system trash exists it is the gentler, recoverable alternative and is offered alongside.
 */
@Composable
fun DeleteFromDeviceDialog(
    itemTitle: String,
    itemCount: Int = 1,
    onConfirm: () -> Unit,
    allowTrash: Boolean = false,
    onTrashClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val subject = if (itemCount > 1) "$itemCount videos" else "“$itemTitle”"
    ApturaDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.DeleteOutline,
        iconTone = ApturaTone.Destructive,
        title = if (itemCount > 1) "Delete $itemCount videos from device?" else "Delete from device?",
        message = if (allowTrash) {
            "$subject can go to Android's trash, where you can restore ${if (itemCount > 1) "them" else "it"} " +
                "later. Deleting permanently cannot be undone. " +
                "Android may ask you to confirm either action."
        } else {
            "$subject will be permanently deleted from your device's storage. This frees up space and can't be " +
                "undone. Android may ask you to confirm."
        },
        // The recoverable action is the default; permanent deletion is an explicit red alternative.
        confirmLabel = if (allowTrash) "Move to trash" else "Delete permanently",
        confirmTone = if (allowTrash) ApturaTone.Accent else ApturaTone.Destructive,
        onConfirm = if (allowTrash) onTrashClick else onConfirm,
        alternative = if (allowTrash) {
            DialogAlternative("Delete permanently", onConfirm, ApturaTone.Destructive)
        } else {
            null
        }
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
    ApturaDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.VisibilityOff,
        title = "Hide from library?",
        message = "“$itemTitle” stays on your device but won't appear in Aptura, even after a rescan. " +
            "You can restore hidden videos in Settings.",
        confirmLabel = "Hide",
        onConfirm = onConfirm
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

/**
 * Removes an already-missing item from the index. Unlike Hide, this deletes the saved position and
 * bookmarks; unlike Delete, it does not touch (or fail on) a file that is no longer there.
 */
@Composable
fun RemoveMissingVideoDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ApturaDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.FolderOff,
        title = "Remove missing video?",
        message = "Remove “$itemTitle” from your library? The file is already unavailable, so nothing on disk " +
            "will be deleted. Its saved playback position and bookmarks will be removed. If the file comes back " +
            "later, it will be added as a new video.",
        confirmLabel = "Remove",
        confirmTone = ApturaTone.Destructive,
        onConfirm = onConfirm
    )
}

/** Result of a lookup that may legitimately find nothing; the holder lets "not finished yet" be told apart from "none". */
private data class DetailLookup<T>(val value: T?)

/** What to show for a value we could not read: "Reading…" while inspection runs, "Unknown" once it has failed. */
private fun unknownOrLoading(inspection: MediaExtendedMetadata?): String =
    if (inspection == null) "Reading…" else "Unknown"
