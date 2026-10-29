package org.aptura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.ApturaMenuItem
import org.aptura.player.ui.components.ApturaTone

/**
 * Everything a video can be asked to do from the library. Grouped so the grid card and the list row take the same
 * single parameter, and a new action is added in one place instead of two.
 */
@Immutable
class VideoCardCallbacks(
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val onMore: (() -> Unit)? = null,
    val onPlayNext: (() -> Unit)? = null,
    val onAddToQueue: (() -> Unit)? = null,
    val onHide: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
    val onRemoveMissing: (() -> Unit)? = null,
    val onMoreFileActions: (() -> Unit)? = null,
    val onShare: (() -> Unit)? = null,
    val onMove: (() -> Unit)? = null,
    val onSelect: (() -> Unit)? = null
) {
    val hasContextActions: Boolean
        get() = onPlayNext != null || onAddToQueue != null || onHide != null || onDelete != null ||
            onRemoveMissing != null || onMoreFileActions != null || onShare != null || onMove != null || onSelect != null
}

/**
 * A video as a picture card: the frame carries the shape and the text sits on the page below it. Without a container,
 * a one-line title next to a two-line title no longer leaves a blank band inside one of the cards.
 *
 * At [LibraryDensity.COMPACT] the card is narrow (three across a phone), so the menu button moves onto the picture's
 * corner instead of taking a third of the card's width from the title.
 */
@Composable
fun VideoGridCard(
    card: VideoCardUiModel,
    callbacks: VideoCardCallbacks,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    detail: LibraryDetail = LibraryDetail.STANDARD,
    density: LibraryDensity = LibraryDensity.COMFORTABLE,
    thumbnail: (@Composable () -> Unit)? = null
) {
    val compact = density == LibraryDensity.COMPACT
    val metadata = remember(card, detail) { videoMetadataLines(card, detail) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickableCard(callbacks)
            .semantics { selected = isSelected }
    ) {
        VideoThumbnailFrame(
            card = card,
            isSelected = isSelected,
            modifier = Modifier.fillMaxWidth(),
            thumbnail = thumbnail,
            overlayEnd = if (compact && callbacks.onMore != null) {
                { VideoOverflowButton(card, callbacks, onScrim = true) }
            } else {
                null
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = if (compact) 8.dp else 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    style = when (density) {
                        LibraryDensity.COMPACT -> ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        LibraryDensity.COMFORTABLE -> ApturaTypography.titleMedium
                        LibraryDensity.LARGE -> ApturaTypography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = ApturaColors.TextHighEmphasis
                )
                MetadataLines(metadata, compact = compact)
            }
            if (!compact && callbacks.onMore != null) {
                // The touch target stays 48 dp; the offset lines the visible glyph up with the title's first line and
                // the card's right edge instead of floating in the middle of the text block.
                Box(modifier = Modifier.offset(x = 12.dp, y = (-12).dp)) {
                    VideoOverflowButton(card, callbacks, onScrim = false)
                }
            }
        }
    }
}

/**
 * A video as a row: a small picture, then the title and its small print. Denser than the grid, and easier to scan by
 * name; on a tablet the rows run in as many columns as fit.
 */
@Composable
fun VideoListRow(
    card: VideoCardUiModel,
    callbacks: VideoCardCallbacks,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    detail: LibraryDetail = LibraryDetail.STANDARD,
    density: LibraryDensity = LibraryDensity.COMFORTABLE,
    thumbnail: (@Composable () -> Unit)? = null
) {
    val metadata = remember(card, detail) { videoMetadataLines(card, detail) }
    val remaining = if (card.progressFraction > 0f && !card.isCompleted && !card.isMissing) {
        MediaFormatters.formatRemaining(card.rawItem.playbackPositionMs, card.rawItem.durationMs)
    } else {
        null
    }
    val rowShape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (isSelected) ApturaColors.AccentSubtle else androidx.compose.ui.graphics.Color.Transparent)
            .clickableCard(callbacks)
            .semantics { selected = isSelected }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoThumbnailFrame(
            card = card,
            isSelected = isSelected,
            modifier = Modifier.width(LibraryMetrics.listThumbnailWidth(density)),
            shape = RoundedCornerShape(10.dp),
            pillsFit = density != LibraryDensity.COMPACT,
            thumbnail = thumbnail
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = card.title,
                style = if (density == LibraryDensity.COMPACT) {
                    ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                } else {
                    ApturaTypography.titleMedium
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = ApturaColors.TextHighEmphasis
            )
            MetadataLines(metadata, compact = density == LibraryDensity.COMPACT)
            if (remaining != null) {
                Text(
                    text = remaining,
                    style = ApturaTypography.bodySmall,
                    maxLines = 1,
                    color = ApturaColors.AccentPrimary
                )
            } else if (card.rawItem.durationMs > 0L && density == LibraryDensity.COMPACT) {
                // The pill is hidden on a very small picture; the length is still worth knowing.
                Text(
                    text = card.durationFormatted,
                    style = ApturaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    color = ApturaColors.TextMediumEmphasis
                )
            }
        }
        if (callbacks.onMore != null) {
            VideoOverflowButton(card, callbacks, onScrim = false)
        }
    }
}

@Composable
private fun MetadataLines(lines: List<String>, compact: Boolean) {
    lines.forEachIndexed { index, line ->
        Text(
            text = line,
            style = (if (compact) ApturaTypography.labelSmall else ApturaTypography.bodySmall)
                .copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = ApturaColors.TextMediumEmphasis,
            modifier = Modifier.padding(top = if (index == 0) 3.dp else 1.dp)
        )
    }
}

private fun Modifier.clickableCard(callbacks: VideoCardCallbacks): Modifier =
    if (callbacks.onLongClick != null) {
        combinedClickable(
            onClick = callbacks.onClick,
            onLongClick = callbacks.onLongClick,
            onLongClickLabel = "Select video"
        )
    } else {
        clickable(onClick = callbacks.onClick)
    }

/**
 * The picture of a video with everything that sits on it: duration, "Watched", the selection mark, the missing badge
 * and the watch-progress line. Shared by the grid card and the list row, so a video looks like the same video in both.
 *
 * @param pillsFit false on a very small picture, where the duration pill would cover too much of it.
 */
@Composable
internal fun VideoThumbnailFrame(
    card: VideoCardUiModel,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    pillsFit: Boolean = true,
    /** Replaces the video's own frame, for content that has no file behind it (the welcome guide's samples). */
    thumbnail: (@Composable () -> Unit)? = null,
    overlayEnd: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(ApturaColors.SurfaceElevated2)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle,
                shape = shape
            )
    ) {
        if (thumbnail != null) {
            Box(modifier = Modifier.fillMaxSize()) { thumbnail() }
        } else {
            VideoThumbnail(card = card, modifier = Modifier.fillMaxSize())
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ApturaColors.AccentPrimary.copy(alpha = 0.18f))
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(28.dp),
                shape = CircleShape,
                color = ApturaColors.AccentPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = ApturaColors.TextOnAccent,
                    modifier = Modifier.padding(5.dp)
                )
            }
        } else if (card.isCompleted && !card.isMissing && pillsFit) {
            ThumbnailPill(
                text = "Watched",
                icon = Icons.Default.Check,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        if (card.rawItem.durationMs > 0L && pillsFit) {
            ThumbnailPill(
                text = card.durationFormatted,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }

        // Missing / Unmounted (FR-LIB-018, FR-LIB-021)
        if (card.isMissing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ApturaColors.SurfaceScrim.copy(alpha = 0.55f))
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(ApturaColors.StatusError.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOff,
                    contentDescription = null,
                    tint = ApturaColors.TextOnScrim,
                    modifier = Modifier.size(12.dp)
                )
                if (pillsFit) {
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Missing", style = ApturaTypography.labelSmall, color = ApturaColors.TextOnScrim)
                }
            }
        }

        // Watch progress: a flush 3 dp line along the bottom edge (no stop dot, P2-05).
        if (card.progressFraction > 0f && !card.isCompleted) {
            WatchProgressLine(
                fraction = card.progressFraction,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (overlayEnd != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) { overlayEnd() }
        }
    }
}

/**
 * The ⋮ button and its menu. [onScrim] draws the glyph on a small dark disc, for when the button sits on a picture.
 * A video with no extra actions just opens its details.
 */
@Composable
internal fun VideoOverflowButton(card: VideoCardUiModel, callbacks: VideoCardCallbacks, onScrim: Boolean) {
    val onMore = callbacks.onMore ?: return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { if (callbacks.hasContextActions) expanded = true else onMore() },
            modifier = Modifier.size(48.dp)
        ) {
            if (onScrim) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(ApturaColors.SurfaceScrim.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        // With a grid full of identical ellipses, "More actions" alone tells a screen-reader user
                        // nothing about which video will be changed.
                        contentDescription = "More actions for ${card.title}",
                        tint = ApturaColors.TextOnScrim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions for ${card.title}",
                    tint = ApturaColors.TextMediumEmphasis,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (callbacks.hasContextActions) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = ApturaColors.SurfaceElevated2,
                shape = RoundedCornerShape(16.dp)
            ) {
                fun pick(action: () -> Unit) {
                    expanded = false
                    action()
                }
                ApturaMenuItem("Video details", Icons.Outlined.Info, onClick = { pick(onMore) })
                if (card.isMissing) {
                    // A missing file cannot play, move, share or be deleted again. The only honest action is to
                    // remove its index row (and its saved progress).
                    callbacks.onRemoveMissing?.let { remove ->
                        MenuDivider()
                        ApturaMenuItem(
                            "Remove from library",
                            Icons.Default.RemoveCircleOutline,
                            onClick = { pick(remove) },
                            tone = ApturaTone.Destructive
                        )
                    }
                } else {
                    callbacks.onPlayNext?.let { ApturaMenuItem("Play next", Icons.AutoMirrored.Filled.PlaylistPlay, onClick = { pick(it) }) }
                    callbacks.onAddToQueue?.let { ApturaMenuItem("Add to queue", Icons.AutoMirrored.Filled.PlaylistAdd, onClick = { pick(it) }) }
                    callbacks.onSelect?.let { ApturaMenuItem("Select", Icons.Outlined.CheckCircle, onClick = { pick(it) }) }
                    if (callbacks.onMove != null || callbacks.onShare != null || callbacks.onMoreFileActions != null) MenuDivider()
                    callbacks.onMove?.let { ApturaMenuItem("Move to folder", Icons.AutoMirrored.Filled.DriveFileMove, onClick = { pick(it) }) }
                    callbacks.onShare?.let { ApturaMenuItem("Share", Icons.Default.Share, onClick = { pick(it) }) }
                    callbacks.onMoreFileActions?.let { ApturaMenuItem("More file actions", Icons.Default.DriveFileRenameOutline, onClick = { pick(it) }) }
                    if (callbacks.onHide != null || callbacks.onDelete != null) MenuDivider()
                    callbacks.onHide?.let { ApturaMenuItem("Hide from library", Icons.Default.VisibilityOff, onClick = { pick(it) }) }
                    callbacks.onDelete?.let {
                        ApturaMenuItem("Delete video", Icons.Default.DeleteOutline, onClick = { pick(it) }, tone = ApturaTone.Destructive)
                    }
                }
            }
        }
    }
}
