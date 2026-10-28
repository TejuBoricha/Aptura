@file:androidx.annotation.OptIn(
    androidx.media3.common.util.UnstableApi::class
)
@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package org.aptura.player.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.model.PlayMode
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.MediaFormatters

/**
 * Interactive Playback Queue Bottom Sheet (SCR-010f, FR-CTRL-001, FR-CTRL-017).
 * Features live queue inspection, active item highlight, tap-to-jump,
 * reorder handles, swipe-to-remove, and play mode switcher.
 */
@Composable
fun PlaybackQueueSheet(
    queueItems: List<DomainMediaItem>,
    currentIndex: Int,
    playMode: PlayMode,
    onDismiss: () -> Unit,
    onJumpToItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onTogglePlayMode: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApturaColors.SurfaceElevated2,
        scrimColor = ApturaColors.SurfaceScrim,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        PlaybackQueueContent(
            queueItems = queueItems,
            currentIndex = currentIndex,
            playMode = playMode,
            onJumpToItem = onJumpToItem,
            onMoveItem = onMoveItem,
            onRemoveItem = onRemoveItem,
            onClearQueue = onClearQueue,
            onTogglePlayMode = onTogglePlayMode
        )
    }
}

/**
 * Pure composable content for PlaybackQueueSheet.
 */
@Composable
fun PlaybackQueueContent(
    queueItems: List<DomainMediaItem>,
    currentIndex: Int,
    playMode: PlayMode,
    onJumpToItem: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onTogglePlayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
            // Header Row: Queue Title + Count + PlayMode Chip + Clear Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ApturaColors.AccentSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = ApturaColors.AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Queue",
                        color = ApturaColors.TextHighEmphasis,
                        style = ApturaTypography.titleLarge.copy(fontSize = 18.sp)
                    )
                    Surface(
                        color = ApturaColors.SurfaceElevated3,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ApturaColors.BorderSubtle)
                    ) {
                        Text(
                            text = "${queueItems.size}",
                            color = ApturaColors.AccentPrimary,
                            style = ApturaTypography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // PlayMode Chip (SCR-010f)
                    val modeIcon: ImageVector = when (playMode) {
                        PlayMode.NORMAL -> Icons.Default.PlayArrow
                        PlayMode.REPEAT_ALL -> Icons.Default.Repeat
                        PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                        PlayMode.SHUFFLE -> Icons.Default.Shuffle
                    }
                    val modeLabel = when (playMode) {
                        PlayMode.NORMAL -> "Play all"
                        PlayMode.REPEAT_ALL -> "Repeat all"
                        PlayMode.REPEAT_ONE -> "Repeat one"
                        PlayMode.SHUFFLE -> "Shuffle"
                    }

                    FilterChip(
                        selected = playMode != PlayMode.NORMAL,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePlayMode()
                        },
                        label = {
                            Text(
                                text = modeLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = modeLabel,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ApturaColors.SurfaceElevated3,
                            labelColor = ApturaColors.TextMediumEmphasis,
                            iconColor = ApturaColors.TextMediumEmphasis,
                            selectedContainerColor = ApturaColors.AccentSubtle,
                            selectedLabelColor = ApturaColors.AccentPrimary,
                            selectedLeadingIconColor = ApturaColors.AccentPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = playMode != PlayMode.NORMAL,
                            borderColor = ApturaColors.BorderSubtle,
                            selectedBorderColor = ApturaColors.AccentPrimary
                        )
                    )

                    if (queueItems.size > 1) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClearQueue()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear queue",
                                tint = ApturaColors.TextMediumEmphasis,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = ApturaColors.BorderSubtle,
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Queue Items List
            if (queueItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = ApturaColors.TextMediumEmphasis,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = queueItems,
                        key = { index, item -> "${item.uri}_$index" },
                        contentType = { _, _ -> "queue_item" }
                    ) { index, item ->
                        val isPlaying = (index == currentIndex)
                        QueueItemRow(
                            index = index,
                            item = item,
                            isPlaying = isPlaying,
                            canMoveUp = index > 0,
                            canMoveDown = index < queueItems.size - 1,
                            canRemove = queueItems.size > 1,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onJumpToItem(index)
                            },
                            onMoveUp = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMoveItem(index, index - 1)
                            },
                            onMoveDown = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMoveItem(index, index + 1)
                            },
                            onRemove = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRemoveItem(index)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

@Composable
private fun QueueItemRow(
    index: Int,
    item: DomainMediaItem,
    isPlaying: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) ApturaColors.SurfaceElevated3 else ApturaColors.SurfaceElevated1,
        label = "queue_row_bg"
    )
    val borderColor = if (isPlaying) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Index or Playing Icon Indicator
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ApturaColors.AccentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Now Playing",
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Text(
                    text = "${index + 1}",
                    color = ApturaColors.TextLowEmphasis,
                    style = ApturaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.width(28.dp)
                )
            }

            // The video's own frame: a column of identical file glyphs gave no way to recognise an episode at a glance.
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ApturaColors.SurfaceElevated3)
                    .border(
                        1.dp,
                        if (isPlaying) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                org.aptura.player.ui.library.VideoThumbnail(
                    uri = item.uri,
                    dateModifiedSec = item.dateModifiedSec,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Video Title & Duration
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = MediaFormatters.cleanTitle(item.title),
                        color = if (isPlaying) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                        fontSize = 14.sp,
                        fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isPlaying) {
                        Surface(
                            color = ApturaColors.AccentPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NOW",
                                color = ApturaColors.TextOnAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (item.durationMs > 0L) {
                    Text(
                        text = MediaFormatters.formatDuration(item.durationMs),
                        color = ApturaColors.TextMediumEmphasis,
                        style = ApturaTypography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }

            // Reorder & Remove Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (canMoveUp) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (canMoveDown) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove from Queue",
                            tint = ApturaColors.TextLowEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
