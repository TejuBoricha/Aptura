@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.ui.player

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.selectable
import org.aptura.player.ui.components.ApturaTextButton
import org.aptura.player.ui.components.ApturaOutlinedButton
import org.aptura.player.ui.components.ApturaButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import org.aptura.player.ui.components.ApturaModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.core.media.MediaLanguageLabels
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import org.aptura.player.data.repository.DetectedSidecarSubtitle

/**
 * Studio-Grade Subtitle Control & Calibration Sheet.
 *
 * Provides a 3-tab segmented interface:
 * 1. Tracks & Import (manage embedded tracks, sidecars, web search, and recent downloads)
 * 2. Synchronization (tactile delay calibration with offset visualizer and continuous slider)
 * 3. Style & Appearance (live cinematic subtitle preview, font size, palette swatches, backdrop mode)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSheet(
    uiState: PlayerUiState,
    onDismissRequest: () -> Unit,
    onSubtitleTrackSelected: (Int?) -> Unit,
    onSubtitleDelayChanged: (Long) -> Unit,
    onImportSubtitleClick: () -> Unit,
    onRemoveExternalSubtitle: (String) -> Unit = {},
    onSubtitleFontSizeChanged: (Int) -> Unit,
    onSubtitleTextColorChanged: (String) -> Unit,
    onSubtitleBackgroundChanged: (Boolean) -> Unit,
    onSearchWebClick: () -> Unit = {},
    onAttachRecentSubtitle: (DetectedSidecarSubtitle) -> Unit = {},
    onDismissRecentSubtitle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    ApturaModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        containerColor = ApturaColors.SurfaceElevated2,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            val activeTrackLabel = remember(uiState.selectedSubtitleTrackIndex, uiState.subtitleTracks) {
                val active = uiState.subtitleTracks.firstOrNull { it.index == uiState.selectedSubtitleTrackIndex }
                active?.label ?: if (uiState.selectedSubtitleTrackIndex == null) "Off" else "Default"
            }
            PlayerSheetHeader(
                icon = Icons.Default.Subtitles,
                title = "Subtitles",
                subtitle = "Showing: $activeTrackLabel",
                subtitleIsActive = uiState.selectedSubtitleTrackIndex != null,
                onClose = onDismissRequest
            )

            Spacer(modifier = Modifier.height(14.dp))

            PlayerSheetTabs(
                tabs = listOf(
                    "Tracks" to Icons.Default.Subtitles,
                    "Timing" to Icons.Default.Sync,
                    "Style" to Icons.Default.Palette
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> SubtitleTracksTab(
                        uiState = uiState,
                        onSubtitleTrackSelected = onSubtitleTrackSelected,
                        onImportSubtitleClick = onImportSubtitleClick,
                        onRemoveExternalSubtitle = onRemoveExternalSubtitle,
                        onSearchWebClick = onSearchWebClick,
                        onAttachRecentSubtitle = onAttachRecentSubtitle,
                        onDismissRecentSubtitle = onDismissRecentSubtitle
                    )
                    1 -> SubtitleSyncTab(
                        subtitleDelayMs = uiState.subtitleDelayMs,
                        onSubtitleDelayChanged = onSubtitleDelayChanged
                    )
                    2 -> SubtitleStyleTab(
                        fontSizeSp = uiState.subtitleFontSizeSp,
                        textColorHex = uiState.subtitleTextColorHex,
                        backgroundEnabled = uiState.subtitleBackgroundEnabled,
                        onFontSizeChanged = onSubtitleFontSizeChanged,
                        onTextColorChanged = onSubtitleTextColorChanged,
                        onBackgroundChanged = onSubtitleBackgroundChanged
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: Subtitle Tracks & Import
 */
@Composable
private fun SubtitleTracksTab(
    uiState: PlayerUiState,
    onSubtitleTrackSelected: (Int?) -> Unit,
    onImportSubtitleClick: () -> Unit,
    onRemoveExternalSubtitle: (String) -> Unit,
    onSearchWebClick: () -> Unit,
    onAttachRecentSubtitle: (DetectedSidecarSubtitle) -> Unit,
    onDismissRecentSubtitle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Affordance 0: Found in Downloads (Auto-detected recent download)
        if (uiState.recentDownloadedSubtitle != null) {
            val recent = uiState.recentDownloadedSubtitle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ApturaColors.StatusSuccess.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, ApturaColors.StatusSuccess.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ApturaColors.StatusSuccess.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = ApturaColors.StatusSuccess,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Found in Downloads",
                                style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ApturaColors.StatusSuccess
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ApturaColors.StatusSuccess.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "RECENT",
                                    style = ApturaTypography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ApturaColors.StatusSuccess,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${recent.displayName} • ${recent.languageLabel}",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextHighEmphasis,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAttachRecentSubtitle(recent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApturaColors.StatusSuccess,
                                contentColor = Color(0xFF003915)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Attach",
                                style = ApturaTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF003915)
                                )
                            )
                        }

                        IconButton(
                            onClick = onDismissRecentSubtitle,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss recent subtitle",
                                tint = ApturaColors.TextLowEmphasis,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Caption track list: one card per option keeps long sidecar names readable.
        Text(
            text = "TRACK",
            style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = ApturaColors.TextMediumEmphasis
        )

        val isOffSelected = uiState.selectedSubtitleTrackIndex == null
        TrackOptionCard(
            selected = isOffSelected,
            icon = Icons.Default.SubtitlesOff,
            title = "Off",
            subtitle = "Hide all on-screen captions",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSubtitleTrackSelected(null)
            }
        )

        uiState.subtitleTracks.forEach { track ->
            val language = MediaLanguageLabels.displayName(track.language)
            // Sidecar labels are built as "English · SDH (file.en.srt)": keep the language/role as the
            // title and let the file name take the metadata line instead of truncating both.
            val splitAt = if (track.isExternal) track.label.indexOf(" (") else -1
            val hasFileHint = splitAt > 0 && track.label.endsWith(")")
            val title = if (hasFileHint) track.label.substring(0, splitAt) else track.label
            val fileHint = if (hasFileHint) track.label.substring(splitAt + 2, track.label.length - 1) else null
            val subtitle = when {
                fileHint != null -> fileHint
                track.isExternal -> "Imported file"
                language != null -> "Embedded - $language"
                else -> "Embedded"
            }
            TrackOptionCard(
                selected = uiState.selectedSubtitleTrackIndex == track.index,
                icon = Icons.Default.Subtitles,
                title = title,
                subtitle = subtitle,
                badge = if (track.isExternal) "External" else null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSubtitleTrackSelected(track.index)
                },
                onRemove = if (track.isExternal) {
                    { onRemoveExternalSubtitle(track.label) }
                } else {
                    null
                }
            )
        }

        // Import External Subtitle Affordance Card
        Surface(
            onClick = onImportSubtitleClick,
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated1,
            border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ApturaColors.AccentSubtle,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = ApturaColors.AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import Subtitle File",
                        style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ApturaColors.TextHighEmphasis
                    )
                    Text(
                        text = "Supports .srt, .vtt, .ass, .ssa, .ttml, .sub, .zip",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = ApturaColors.AccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Subtitles on Web Card
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSearchWebClick()
            },
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated1,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ApturaColors.AccentSubtle,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = ApturaColors.AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Search Subtitles on Web",
                            style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ApturaColors.TextHighEmphasis
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ApturaColors.AccentSubtle
                        ) {
                            Text(
                                text = "ONLINE",
                                style = ApturaTypography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ApturaColors.AccentPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    val querySnippet = uiState.cleanSearchQuery.ifBlank { uiState.videoTitle }
                    Text(
                        text = "Subdl & OpenSubtitles for \"$querySnippet\"",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextMediumEmphasis,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = ApturaColors.AccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * One selectable caption track. Long sidecar file names stay on a single ellipsized line, while the
 * metadata line (source + language) gets its own row so badges never get squeezed into a vertical strip.
 */
@Composable
private fun TrackOptionCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badge: String? = null,
    onRemove: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.10f) else ApturaColors.SurfaceElevated1,
        border = BorderStroke(
            1.dp,
            if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.55f) else ApturaColors.BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated2,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subtitle,
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextMediumEmphasis,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badge != null) {
                        MetaBadge(text = badge)
                    }
                }
            }

            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove external subtitle",
                        tint = ApturaColors.StatusError.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = ApturaColors.AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MetaBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = ApturaColors.AccentSubtle,
        border = BorderStroke(0.5.dp, ApturaColors.AccentPrimary.copy(alpha = 0.35f))
    ) {
        Text(
            text = text.uppercase(),
            style = ApturaTypography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = ApturaColors.AccentPrimary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Tab 2: subtitle timing. The same timing control as audio sync, then tap-to-sync for when you can't tell by how much
 * the text is off: mark when the line is spoken and when its subtitle appears, in either order.
 */
@Composable
private fun SubtitleSyncTab(
    subtitleDelayMs: Long,
    onSubtitleDelayChanged: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        SheetSection("Subtitle timing") {
            TimingOffsetControl(
                offsetMs = subtitleDelayMs,
                onOffsetChange = onSubtitleDelayChanged,
                rangeMs = 5_000L,
                stepMs = 50L,
                earlierLabel = "Text earlier",
                laterLabel = "Text later",
                label = "Subtitle timing",
                describe = { ms ->
                    when {
                        ms == 0L -> "Subtitles follow the file's own timing"
                        ms > 0L -> "Text appears ${formatOffset(ms).drop(1)} later"
                        else -> "Text appears ${formatOffset(ms).drop(1)} earlier"
                    }
                }
            )
        }

        var speechTimestamp by remember { mutableStateOf<Long?>(null) }
        var subtitleTimestamp by remember { mutableStateOf<Long?>(null) }
        SheetSection(
            "Tap to sync",
            trailing = if (speechTimestamp != null || subtitleTimestamp != null) {
                {
                    ApturaTextButton(text = "Start over", onClick = {
                        speechTimestamp = null
                        subtitleTimestamp = null
                    })
                }
            } else null
        ) {
            Text(
                text = "Play the video. Tap the first button when you hear a line and the second when its subtitle " +
                    "appears, in whichever order they happen.",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TapMarkButton(
                    text = if (speechTimestamp != null) "Line heard" else "I heard it",
                    marked = speechTimestamp != null,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val now = System.currentTimeMillis()
                    val sub = subtitleTimestamp
                    if (sub != null) {
                        onSubtitleDelayChanged(subtitleDelayMs + (now - sub))
                        speechTimestamp = null
                        subtitleTimestamp = null
                    } else {
                        speechTimestamp = now
                    }
                }
                TapMarkButton(
                    text = if (subtitleTimestamp != null) "Text seen" else "I saw it",
                    marked = subtitleTimestamp != null,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val now = System.currentTimeMillis()
                    val voice = speechTimestamp
                    if (voice != null) {
                        onSubtitleDelayChanged(subtitleDelayMs + (voice - now))
                        speechTimestamp = null
                        subtitleTimestamp = null
                    } else {
                        subtitleTimestamp = now
                    }
                }
            }
        }
    }
}

@Composable
private fun TapMarkButton(text: String, marked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (marked) {
        ApturaButton(text = text, icon = Icons.Default.Check, onClick = onClick, modifier = modifier)
    } else {
        ApturaOutlinedButton(text = text, onClick = onClick, modifier = modifier)
    }
}

/** Tab 3: how subtitles look, with a preview that uses exactly these settings. */
@Composable
private fun SubtitleStyleTab(
    fontSizeSp: Int,
    textColorHex: String,
    backgroundEnabled: Boolean,
    onFontSizeChanged: (Int) -> Unit,
    onTextColorChanged: (String) -> Unit,
    onBackgroundChanged: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val previewTextColor = remember(textColorHex) {
        try {
            Color(android.graphics.Color.parseColor(textColorHex))
        } catch (_: Throwable) {
            Color.White
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        // A frame-like preview: the subtitle sits where it will on the video, over a picture-like gradient.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF2A3442), Color(0xFF151A22), Color(0xFF0A0C10)))
                )
                .border(1.dp, ApturaColors.BorderSubtle, RoundedCornerShape(16.dp))
                .semantics(mergeDescendants = true) { contentDescription = "Subtitle preview" },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 18.dp, start = 16.dp, end = 16.dp)
                    .then(
                        if (backgroundEnabled) {
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        } else {
                            Modifier
                        }
                    )
            ) {
                Text(
                    text = "Where are you taking me?",
                    style = TextStyle(
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.Medium,
                        color = previewTextColor,
                        textAlign = TextAlign.Center,
                        shadow = if (!backgroundEnabled) Shadow(Color.Black, Offset(2f, 2f), 4f) else null
                    )
                )
            }
        }

        SheetSection("Text") {
            val sizes = listOf(14, 18, 22, 26)
            SheetRowText("Size", null)
            org.aptura.player.ui.settings.SegmentedPicker(
                options = listOf("Small", "Medium", "Large", "Huge"),
                selectedIndex = sizes.indexOf(fontSizeSp),
                onSelect = { onFontSizeChanged(sizes[it]) },
                label = "Subtitle size"
            )
            SheetRowText("Color", null)
            val palette = listOf("White" to "#FFFFFF", "Yellow" to "#FFEB3B", "Cyan" to "#00E5FF", "Green" to "#00E676")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                palette.forEach { (name, hex) ->
                    val selected = textColorHex.equals(hex, ignoreCase = true)
                    val swatch = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = selected, role = Role.RadioButton) {
                                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTextColorChanged(hex)
                            }
                            .padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(2.dp, if (selected) ApturaColors.AccentPrimary else Color.Transparent, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(swatch),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = name,
                            style = ApturaTypography.labelMedium,
                            color = if (selected) ApturaColors.TextHighEmphasis else ApturaColors.TextMediumEmphasis
                        )
                    }
                }
            }
        }

        SheetSection("Readability") {
            SheetRowText("Background", "A box is easiest to read over bright scenes")
            org.aptura.player.ui.settings.SegmentedPicker(
                options = listOf("Shadow", "Box"),
                selectedIndex = if (backgroundEnabled) 1 else 0,
                onSelect = { onBackgroundChanged(it == 1) },
                label = "Subtitle background"
            )
        }
    }
}
