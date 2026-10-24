@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.ui.player

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
import androidx.compose.material3.ModalBottomSheet
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

    ModalBottomSheet(
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
            // Header: Title & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ApturaColors.AccentSubtle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Subtitles",
                            style = ApturaTypography.headlineMedium.copy(fontSize = 18.sp),
                            color = ApturaColors.TextHighEmphasis
                        )
                        val activeTrackLabel = remember(uiState.selectedSubtitleTrackIndex, uiState.subtitleTracks) {
                            val active = uiState.subtitleTracks.firstOrNull { it.index == uiState.selectedSubtitleTrackIndex }
                            active?.label ?: if (uiState.selectedSubtitleTrackIndex == null) "Off" else "Default"
                        }
                        Text(
                            text = "Active: $activeTrackLabel",
                            style = ApturaTypography.labelSmall,
                            color = if (uiState.selectedSubtitleTrackIndex != null) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Pill Tab Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ApturaColors.SurfaceElevated1,
                border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "Tracks" to Icons.Default.Subtitles,
                        "Sync Delay" to Icons.Default.Sync,
                        "Style" to Icons.Default.Palette
                    )
                    tabs.forEachIndexed { index, (label, icon) ->
                        val isSelected = selectedTab == index
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ApturaColors.SurfaceElevated3 else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                }
                                .clickable(role = Role.Tab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedTab = index
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = ApturaTypography.labelSmall,
                                    color = if (isSelected) ApturaColors.TextHighEmphasis else ApturaColors.TextMediumEmphasis,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

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
            text = "CAPTION TRACK",
            style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = ApturaColors.TextLowEmphasis
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
            val language = track.language
                ?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
                ?.uppercase()
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
 * Tab 2: Tactile Subtitle Synchronization (Delay Calibration)
 */
@Composable
private fun SubtitleSyncTab(
    subtitleDelayMs: Long,
    onSubtitleDelayChanged: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Visualizer Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated3,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SYNC OFFSET",
                    style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = ApturaColors.AccentPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val sign = if (subtitleDelayMs > 0) "+" else ""
                Text(
                    text = "$sign${subtitleDelayMs} ms",
                    style = ApturaTypography.displayLarge.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (subtitleDelayMs != 0L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val statusText = when {
                    subtitleDelayMs == 0L -> "Perfect sync with video timestamps"
                    subtitleDelayMs > 0L -> "Subtitles delayed by ${subtitleDelayMs}ms (text appears later)"
                    else -> "Subtitles advanced by ${-subtitleDelayMs}ms (text appears earlier)"
                }
                Text(
                    text = statusText,
                    style = ApturaTypography.bodyMedium,
                    color = ApturaColors.TextMediumEmphasis,
                    textAlign = TextAlign.Center
                )

                // Reset Affordance
                AnimatedVisibility(
                    visible = subtitleDelayMs != 0L,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ApturaColors.AccentSubtle,
                        border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSubtitleDelayChanged(0L)
                            }
                    ) {
                        Text(
                            text = "Reset to 0 ms",
                            style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ApturaColors.AccentPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Continuous Slider Calibration (-5.0s to +5.0s)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated3,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "-5.0s (Earlier)",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextLowEmphasis
                    )
                    Text(
                        text = "0s",
                        style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ApturaColors.TextMediumEmphasis
                    )
                    Text(
                        text = "+5.0s (Later)",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextLowEmphasis
                    )
                }

                Slider(
                    value = subtitleDelayMs.toFloat().coerceIn(-5000f, 5000f),
                    onValueChange = { newValue ->
                        val snapped = if (kotlin.math.abs(newValue) < 25f) 0L else (newValue / 50f).toLong() * 50L
                        if (snapped == 0L && subtitleDelayMs != 0L) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSubtitleDelayChanged(snapped)
                    },
                    valueRange = -5000f..5000f,
                    colors = SliderDefaults.colors(
                        thumbColor = ApturaColors.AccentPrimary,
                        activeTrackColor = ApturaColors.AccentPrimary,
                        inactiveTrackColor = ApturaColors.SurfaceElevated1
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Nudge Buttons
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val row1 = listOf(
                            "-250ms" to -250L,
                            "-50ms" to -50L,
                            "+50ms" to 50L,
                            "+250ms" to 250L
                        )
                        row1.forEach { (label, delta) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ApturaColors.SurfaceElevated1,
                                border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSubtitleDelayChanged(subtitleDelayMs + delta)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = ApturaTypography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = ApturaColors.TextHighEmphasis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val row2 = listOf(
                            "-1.0s (Quick Jump)" to -1000L,
                            "+1.0s (Quick Jump)" to 1000L
                        )
                        row2.forEach { (label, delta) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ApturaColors.SurfaceElevated1,
                                border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSubtitleDelayChanged(subtitleDelayMs + delta)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = ApturaTypography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = ApturaColors.TextHighEmphasis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tap-to-Sync Interactive Calibration Card
        var speechTimestamp by remember { mutableStateOf<Long?>(null) }
        var subtitleTimestamp by remember { mutableStateOf<Long?>(null) }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated3,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TAP-TO-SYNC CALIBRATOR",
                        style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = ApturaColors.AccentPrimary
                    )
                    if (speechTimestamp != null || subtitleTimestamp != null) {
                        Text(
                            text = "Reset",
                            style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ApturaColors.AccentPrimary,
                            modifier = Modifier.clickable {
                                speechTimestamp = null
                                subtitleTimestamp = null
                            }
                        )
                    }
                }

                Text(
                    text = "Listen & Watch: Tap [1. Actor Spoke] when dialogue starts, then tap [2. Sub Appeared] when text flashes.",
                    style = ApturaTypography.bodyMedium,
                    color = ApturaColors.TextMediumEmphasis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val now = System.currentTimeMillis()
                            val sub = subtitleTimestamp
                            if (sub != null) {
                                val delta = now - sub
                                onSubtitleDelayChanged(subtitleDelayMs + delta)
                                speechTimestamp = null
                                subtitleTimestamp = null
                            } else {
                                speechTimestamp = now
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (speechTimestamp != null) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated1,
                            contentColor = if (speechTimestamp != null) Color.Black else ApturaColors.TextHighEmphasis
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (speechTimestamp != null) "✓ Voice Marked" else "1. Actor Spoke",
                            style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val now = System.currentTimeMillis()
                            val voice = speechTimestamp
                            if (voice != null) {
                                val delta = voice - now
                                onSubtitleDelayChanged(subtitleDelayMs + delta)
                                speechTimestamp = null
                                subtitleTimestamp = null
                            } else {
                                subtitleTimestamp = now
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subtitleTimestamp != null) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated1,
                            contentColor = if (subtitleTimestamp != null) Color.Black else ApturaColors.TextHighEmphasis
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (subtitleTimestamp != null) "✓ Sub Marked" else "2. Sub Appeared",
                            style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Explanatory Tip
        Text(
            text = "Tip: If dialogue sounds before subtitles appear, tap (+) to delay text. If subtitles appear before the actor speaks, tap (-) to advance text.",
            style = ApturaTypography.labelSmall,
            color = ApturaColors.TextLowEmphasis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/**
 * Tab 3: Subtitle Style & Appearance with LIVE CINEMATIC PREVIEW
 */
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // LIVE CINEMATIC PREVIEW HERO BOX
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F1015),
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1B1E26),
                                Color(0xFF0A0B0E)
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Video scene simulation badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "LIVE PREVIEW",
                        style = ApturaTypography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        ),
                        color = ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Subtitle rendered with current settings
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
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
                        text = "The quick brown fox jumps over the lazy dog.",
                        style = TextStyle(
                            fontSize = fontSizeSp.sp,
                            fontWeight = FontWeight.Medium,
                            color = previewTextColor,
                            textAlign = TextAlign.Center,
                            shadow = if (!backgroundEnabled) {
                                Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            } else null
                        )
                    )
                }
            }
        }

        // Section: Font Size
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated3,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Text Size",
                        style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ApturaColors.TextHighEmphasis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val sizes = listOf(
                    "Small" to 14,
                    "Regular" to 18,
                    "Large" to 22,
                    "Huge" to 26
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizes.forEach { (label, sp) ->
                        val isSelected = fontSizeSp == sp
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.SurfaceElevated1,
                            border = if (isSelected) null else BorderStroke(1.dp, ApturaColors.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onFontSizeChanged(sp)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) ApturaColors.SurfacePureBlack else ApturaColors.TextHighEmphasis
                                )
                                Text(
                                    text = "${sp}sp",
                                    style = ApturaTypography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (isSelected) ApturaColors.SurfacePureBlack.copy(alpha = 0.8f) else ApturaColors.TextLowEmphasis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Text Color Swatches
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated3,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Subtitle Color",
                        style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ApturaColors.TextHighEmphasis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val palette = listOf(
                    "White" to "#FFFFFF",
                    "Cinema Yellow" to "#FFEB3B",
                    "Neon Cyan" to "#00E5FF",
                    "Mint Green" to "#00E676"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    palette.forEach { (name, hex) ->
                        val isSelected = textColorHex.equals(hex, ignoreCase = true)
                        val swatchColor = Color(android.graphics.Color.parseColor(hex))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTextColorChanged(hex)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = if (hex == "#FFFFFF" || hex == "#FFEB3B") Color.Black else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = name,
                                style = ApturaTypography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis
                            )
                        }
                    }
                }
            }
        }

        // Section: Background Box vs Text Outline Shadow
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ApturaColors.SurfaceElevated3,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Background & Contrast",
                    style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ApturaColors.TextHighEmphasis
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Drop Shadow Outline
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!backgroundEnabled) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated1,
                        border = BorderStroke(
                            1.dp,
                            if (!backgroundEnabled) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBackgroundChanged(false)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Text Outline",
                                style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (!backgroundEnabled) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Dark edge shadow",
                                style = ApturaTypography.labelSmall.copy(fontSize = 10.sp),
                                color = ApturaColors.TextLowEmphasis
                            )
                        }
                    }

                    // Option 2: Translucent Backdrop Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (backgroundEnabled) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated1,
                        border = BorderStroke(
                            1.dp,
                            if (backgroundEnabled) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBackgroundChanged(true)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Backdrop Box",
                                style = ApturaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (backgroundEnabled) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Solid contrast box",
                                style = ApturaTypography.labelSmall.copy(fontSize = 10.sp),
                                color = ApturaColors.TextLowEmphasis
                            )
                        }
                    }
                }
            }
        }
    }
}
