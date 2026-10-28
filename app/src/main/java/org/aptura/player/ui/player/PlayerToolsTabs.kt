package org.aptura.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.aptura.player.core.playback.PlaybackSpeedOptions
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.ApturaButton
import org.aptura.player.ui.components.ApturaOutlinedButton
import org.aptura.player.ui.components.ApturaTextButton
import org.aptura.player.ui.components.ApturaTone
import org.aptura.player.ui.components.SpeedRail
import org.aptura.player.ui.library.MediaFormatters
import org.aptura.player.ui.settings.SegmentedPicker

/*
 * The two tabs of the Playback tools sheet, built only from the shared player controls. Every section is a labelled
 * panel; every on/off is a switch; every one-of-N is the selector switch Settings uses; every slider is ApturaSlider.
 */

@Composable
internal fun PlaybackToolsTab(
    uiState: PlayerUiState,
    onAudioOnlyChanged: (Boolean) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onExtendSleepTimer: (Int) -> Unit,
    onSleepAtEndOfVideo: () -> Unit,
    onSetAbPointA: () -> Unit,
    onSetAbPointB: () -> Unit,
    onClearAbRepeat: () -> Unit,
    onStepFrame: (Boolean) -> Unit,
    onAddBookmark: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onUpdateBookmarkNote: (Long, String) -> Unit,
    onToggleStatsOverlay: () -> Unit,
    onResetVideoPreferences: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        SheetSection("Playback") {
            SheetRowText("Speed", null)
            SpeedRail(
                speeds = PlaybackSpeedOptions.all,
                selectedSpeed = uiState.playbackSpeed,
                onSelect = onSpeedSelected,
                label = "Playback speed"
            )
            SheetSwitchRow(
                title = "Audio only",
                subtitle = "Turn the picture off and keep listening",
                checked = uiState.isAudioOnly,
                onCheckedChange = onAudioOnlyChanged
            )
        }

        val remaining = rememberSleepTimerRemainingMs(uiState.sleepTimerEndsAtElapsedRealtime)
        val atEnd = uiState.sleepAtEndOfVideo
        SheetSection(
            "Sleep timer",
            trailing = if (remaining != null) {
                { ApturaTextButton(text = "+5 min", onClick = { onExtendSleepTimer(5) }, tone = ApturaTone.Accent) }
            } else null
        ) {
            SheetRowText(
                title = when {
                    remaining != null -> "Stops in ${MediaFormatters.formatDuration(remaining)}"
                    atEnd -> "Stops when this video ends"
                    else -> "Off"
                },
                subtitle = when {
                    remaining != null -> "The sound fades out over the last 10 seconds"
                    atEnd -> "Up next won't start"
                    else -> "Pause playback after a while"
                }
            )
            // The Settings selector: Off is a real choice, and four timings fit without truncating.
            val minutes = listOf(15, 30, 45, 60)
            SegmentedPicker(
                options = listOf("Off") + minutes.map { "$it min" },
                selectedIndex = if (remaining == null && !atEnd) 0 else -1,
                onSelect = { index -> if (index == 0) onCancelSleepTimer() else onStartSleepTimer(minutes[index - 1]) },
                label = "Sleep timer"
            )
            SheetSwitchRow(
                title = "At the end of this video",
                subtitle = "Instead of a set time",
                checked = atEnd,
                onCheckedChange = { on -> if (on) onSleepAtEndOfVideo() else onCancelSleepTimer() }
            )
        }

        val pointA = uiState.abRepeat.pointA
        val pointB = uiState.abRepeat.pointB
        SheetSection(
            "Repeat a part",
            trailing = if (pointA != null || pointB != null) {
                { ApturaTextButton(text = "Clear", onClick = onClearAbRepeat) }
            } else null
        ) {
            SheetRowText(
                title = when {
                    pointA != null && pointB != null ->
                        "Looping ${MediaFormatters.formatDuration(pointA)} – ${MediaFormatters.formatDuration(pointB)}"
                    pointA != null -> "Start set at ${MediaFormatters.formatDuration(pointA)}"
                    else -> "A–B repeat"
                },
                subtitle = when {
                    pointA != null && pointB != null -> null
                    pointA != null -> "Play to where the part ends, then set the end"
                    else -> "Set where the part starts and ends; it plays on a loop"
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AbButton(
                    label = if (pointA != null) "Start  ${MediaFormatters.formatDuration(pointA)}" else "Set start",
                    set = pointA != null,
                    onClick = onSetAbPointA,
                    modifier = Modifier.weight(1f)
                )
                AbButton(
                    label = if (pointB != null) "End  ${MediaFormatters.formatDuration(pointB)}" else "Set end",
                    set = pointB != null,
                    onClick = onSetAbPointB,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SheetSection("Frame by frame") {
            // To the millisecond: frame stepping is how people find an exact cut, and m:ss can't tell frames apart.
            SheetRowText(
                title = "At ${MediaFormatters.formatDuration(uiState.currentPositionMs)}.${"%03d".format(uiState.currentPositionMs.coerceAtLeast(0L) % 1000)}",
                subtitle = "Pauses the video and moves one frame at a time"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ApturaOutlinedButton(
                    text = "Previous",
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    onClick = { onStepFrame(false) },
                    modifier = Modifier.weight(1f)
                )
                ApturaOutlinedButton(
                    text = "Next",
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = { onStepFrame(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SheetSection(
            "Bookmarks",
            trailing = {
                ApturaTextButton(
                    text = "Add at ${MediaFormatters.formatDuration(uiState.currentPositionMs)}",
                    onClick = onAddBookmark,
                    tone = ApturaTone.Accent
                )
            }
        ) {
            if (uiState.bookmarks.isEmpty()) {
                SheetRowText("No bookmarks yet", "Add one to jump back to this moment later")
            } else {
                var editingId by remember { mutableStateOf<Long?>(null) }
                var editingText by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    uiState.bookmarks.forEach { bookmark ->
                        BookmarkRow(
                            time = MediaFormatters.formatDuration(bookmark.timestampMs),
                            note = bookmark.note,
                            editing = editingId == bookmark.id,
                            editingText = editingText,
                            onEditingTextChange = { editingText = it },
                            onJump = { onSeekTo(bookmark.timestampMs) },
                            onEdit = {
                                if (editingId == bookmark.id) {
                                    editingId = null
                                } else {
                                    editingId = bookmark.id
                                    editingText = bookmark.note
                                }
                            },
                            onSave = {
                                onUpdateBookmarkNote(bookmark.id, editingText)
                                editingId = null
                            },
                            onDelete = { onDeleteBookmark(bookmark.id) }
                        )
                    }
                }
            }
        }

        SheetSection("For troubleshooting") {
            SheetSwitchRow(
                title = "Playback stats",
                subtitle = "Decoder, resolution, frame rate and dropped frames over the video",
                checked = uiState.isStatsOverlayVisible,
                onCheckedChange = { onToggleStatsOverlay() }
            )
            SheetRowText(
                title = "Reset this video",
                subtitle = "Speed, aspect ratio, decoder, and audio and subtitle sync go back to your defaults"
            )
            ApturaOutlinedButton(
                text = "Reset to defaults",
                onClick = {
                    onResetVideoPreferences()
                    android.widget.Toast.makeText(context, "This video's settings were reset", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AbButton(label: String, set: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (set) {
        ApturaButton(text = label, icon = Icons.Default.Check, onClick = onClick, tone = ApturaTone.Neutral, modifier = modifier)
    } else {
        ApturaOutlinedButton(text = label, onClick = onClick, modifier = modifier)
    }
}

@Composable
private fun BookmarkRow(
    time: String,
    note: String,
    editing: Boolean,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onJump: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onJump)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    style = ApturaTypography.labelLarge,
                    color = ApturaColors.AccentPrimary,
                    modifier = Modifier
                        .background(ApturaColors.AccentSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = note.ifBlank { "Untitled bookmark" },
                    style = ApturaTypography.bodyLarge,
                    color = if (note.isBlank()) ApturaColors.TextMediumEmphasis else ApturaColors.TextHighEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Rename bookmark", tint = ApturaColors.TextMediumEmphasis, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete bookmark", tint = ApturaColors.TextMediumEmphasis, modifier = Modifier.size(20.dp))
            }
        }
        if (editing) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = onEditingTextChange,
                    placeholder = { Text("Name this moment", color = ApturaColors.TextLowEmphasis) },
                    singleLine = true,
                    textStyle = ApturaTypography.bodyLarge.copy(color = ApturaColors.TextHighEmphasis),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ApturaColors.AccentPrimary,
                        unfocusedBorderColor = ApturaColors.BorderSubtle,
                        cursorColor = ApturaColors.AccentPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                ApturaButton(text = "Save", onClick = onSave)
            }
        }
    }
}

@Composable
internal fun PictureTab(
    uiState: PlayerUiState,
    onResizeModeSelected: (AspectRatioMode) -> Unit,
    onNightModeToggle: (Boolean) -> Unit,
    onNightModeWarmthChanged: (Float) -> Unit,
    onContrastChanged: (Float) -> Unit,
    onSaturationChanged: (Float) -> Unit,
    onResetPictureAdjustment: () -> Unit,
    onDecoderStrategyChanged: (Int) -> Unit
) {
    val picture = uiState.pictureAdjustment
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        SheetSection("Aspect ratio") {
            SegmentedPicker(
                options = AspectRatioMode.entries.map { it.label },
                selectedIndex = uiState.resizeMode.ordinal,
                onSelect = { index ->
                    val mode = AspectRatioMode.entries[index]
                    if (mode != uiState.resizeMode) onResizeModeSelected(mode)
                },
                label = "Aspect ratio"
            )
        }

        SheetSection(
            "Picture",
            trailing = if (picture.contrast != 1f || picture.saturation != 1f || picture.isNightModeEnabled) {
                { ApturaTextButton(text = "Reset", onClick = onResetPictureAdjustment) }
            } else null
        ) {
            SheetSliderRow(
                title = "Contrast",
                valueText = relativePercent(picture.contrast),
                value = picture.contrast,
                onValueChange = { onContrastChanged(snapToPercent(it)) },
                valueRange = 0.5f..1.5f,
                origin = 1f
            )
            SheetSliderRow(
                title = "Color",
                valueText = relativePercent(picture.saturation),
                value = picture.saturation,
                onValueChange = { onSaturationChanged(snapToPercent(it)) },
                valueRange = 0f..2f,
                origin = 1f
            )
            SheetSwitchRow(
                title = "Night mode",
                subtitle = "Warmer colors for watching in the dark",
                checked = picture.isNightModeEnabled,
                onCheckedChange = onNightModeToggle
            )
            if (picture.isNightModeEnabled) {
                SheetSliderRow(
                    title = "Warmth",
                    valueText = "${(picture.nightModeWarmth * 100).roundToInt()}%",
                    value = picture.nightModeWarmth,
                    onValueChange = onNightModeWarmthChanged,
                    valueRange = 0.1f..0.8f
                )
            }
        }

        SheetSection(
            "Decoder",
            trailing = {
                Text(
                    text = if (uiState.decoderOverridden) "Set for this video" else "Using the default",
                    style = ApturaTypography.bodySmall,
                    color = ApturaColors.TextLowEmphasis,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(0, "Auto", "Hardware first, then software if the hardware decoder fails"),
                    Triple(1, "Hardware", "Fastest and coolest, but some formats won't play"),
                    Triple(2, "Software", "Slower and uses more battery, but can rescue a garbled video")
                ).forEach { (strategy, title, description) ->
                    SheetOptionRow(
                        title = title,
                        subtitle = description,
                        selected = uiState.decoderStrategy == strategy,
                        onClick = { onDecoderStrategyChanged(strategy) }
                    )
                }
            }
        }
    }
}

/** 1.10 → "+10%", 0.90 → "−10%", 1.0 → "Normal": how far a picture setting is from the original. */
private fun relativePercent(factor: Float): String {
    val percent = ((factor - 1f) * 100f).roundToInt()
    return when {
        percent == 0 -> "Normal"
        percent > 0 -> "+$percent%"
        else -> "−${-percent}%"
    }
}

/** Whole percent steps, so "Normal" is reachable by dragging and the value never reads +0.3%. */
private fun snapToPercent(factor: Float): Float = (factor * 100f).roundToInt() / 100f
