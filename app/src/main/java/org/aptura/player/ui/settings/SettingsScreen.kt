package org.aptura.player.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.media.CodecSupportEntry
import org.aptura.player.core.media.CodecSupportReport
import org.aptura.player.core.media.DeviceCodecSupport
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.core.playback.PlaybackSpeedOptions
import org.aptura.player.core.platform.AppVersion
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.components.SpeedRail
import org.aptura.player.ui.library.MediaFormatters

private val ResizeModeChoices = listOf("Fit", "Fill", "Zoom", "16:9", "4:3")
private val SeekSteps = listOf(5, 10, 15, 30)
private val CountdownChoices = listOf(5, 10, 15, 0) // seconds; 0 plays the next video straight away
private val DecoderChoices = listOf("Auto", "Hardware", "Software")

/** How far (in dp) the large title has to scroll before the compact title bar has fully taken over. */
private const val TITLE_COLLAPSE_DP = 90

/** Height of the compact bar's content row. The bar is drawn on top of the list, so it must cover exactly this. */
internal const val SETTINGS_TOP_BAR_HEIGHT_DP = 56

/**
 * The compact bar must be an opaque fill. A gradient that fades out inside the bar's own height lets the rows
 * scrolling underneath show through the back button and title (audit 2026-10-24).
 */
internal val SettingsTopBarBackground: Brush
    get() = SolidColor(ApturaColors.SurfaceBase)

/**
 * Settings Root Screen (SCR-013, SCR-014, SCR-015, SCR-016).
 * Unified configuration for playback preferences, storage statistics, diagnostics, and privacy proof.
 *
 * A large title that collapses into a compact bar, a library readout, and flat panels of settings. Option groups use
 * a selector switch (SettingsDesign.kt) instead of chips. Behaviour and callbacks are unchanged by the redesign.
 */
@Composable
fun SettingsScreen(
    settings: ApturaSettings,
    libraryStats: LibraryStatistics,
    onBackClick: () -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdateResizeMode: (Int) -> Unit,
    onUpdateSeekDelta: (Int) -> Unit,
    onUpdateBackgroundAudio: (Boolean) -> Unit,
    onUpdateAutoAdvanceCountdown: (Int) -> Unit,
    onResetPlaybackDefaults: () -> Unit = {},
    onRescanClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit = {},
    onUpdateAccentColor: (AccentColor) -> Unit = {},
    onUpdateDecoderStrategy: (Int) -> Unit = {},
    onClearThumbnailCache: () -> Unit = {},
    onAddFolderClick: () -> Unit = {},
    onRemoveSafFolder: (String) -> Unit = {},
    hiddenVideoCount: Int = 0,
    onRestoreHiddenVideos: () -> Unit = {},
    missingVideoCount: Int = 0,
    onClearMissingVideos: () -> Unit = {},
    onDeleteDiagnosticData: () -> Unit = { ApturaDiagnostics.clearAll() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isDeleteDiagnosticsDialogOpen by remember { mutableStateOf(false) }
    var isResetPlaybackDialogOpen by remember { mutableStateOf(false) }
    var isCodecDialogOpen by remember { mutableStateOf(false) }
    var codecReport by remember { mutableStateOf<CodecSupportReport?>(null) }
    var cacheSizeBytes by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        cacheSizeBytes = withContext(Dispatchers.IO) {
            ThumbnailRepository.getCacheSizeBytes(context)
        }
    }

    // Reading MediaCodecList is cheap but not free; do it off the main thread and only when the report is opened.
    LaunchedEffect(isCodecDialogOpen) {
        if (isCodecDialogOpen && codecReport == null) {
            codecReport = withContext(Dispatchers.IO) { DeviceCodecSupport.report() }
        }
    }

    if (isCodecDialogOpen) {
        CodecSupportDialog(
            report = codecReport,
            onDismiss = { isCodecDialogOpen = false }
        )
    }

    if (isDeleteDiagnosticsDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteDiagnosticsDialogOpen = false },
            containerColor = ApturaColors.SurfaceElevated2,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete diagnostic data?", color = ApturaColors.TextHighEmphasis) },
            text = {
                Text(
                    "This erases the error log and crash reports Aptura keeps on this device. " +
                        "Nothing was ever sent anywhere, and this cannot be undone.",
                    color = ApturaColors.TextMediumEmphasis
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteDiagnosticsDialogOpen = false
                        onDeleteDiagnosticData()
                        Toast.makeText(context, "Diagnostic data deleted", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Delete", color = ApturaColors.StatusError) }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteDiagnosticsDialogOpen = false }) {
                    Text("Cancel", color = ApturaColors.TextMediumEmphasis)
                }
            }
        )
    }

    if (isResetPlaybackDialogOpen) {
        AlertDialog(
            onDismissRequest = { isResetPlaybackDialogOpen = false },
            containerColor = ApturaColors.SurfaceElevated2,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Reset playback defaults?", color = ApturaColors.TextHighEmphasis) },
            text = {
                Text(
                    "This restores the default speed, aspect ratio, seek step, background-audio preference, and auto-advance timing.",
                    color = ApturaColors.TextMediumEmphasis
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isResetPlaybackDialogOpen = false
                        onResetPlaybackDefaults()
                        Toast.makeText(context, "Playback defaults restored", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Reset", color = ApturaColors.AccentPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { isResetPlaybackDialogOpen = false }) {
                    Text("Cancel", color = ApturaColors.TextMediumEmphasis)
                }
            }
        )
    }

    // The compact title bar fades in as the large title scrolls away. The scroll position is read inside the
    // graphicsLayer lambdas, so scrolling redraws the bar but never recomposes the screen.
    val listState = rememberLazyListState()
    val collapseTravelPx = with(LocalDensity.current) { TITLE_COLLAPSE_DP.dp.toPx() }
    val collapse = remember(listState, collapseTravelPx) {
        {
            val scrolled = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
            (scrolled / collapseTravelPx).coerceIn(0f, 1f)
        }
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApturaColors.SurfaceBase),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusTop,
                bottom = navigationBottom + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "title", contentType = "title") {
                Text(
                    text = "Settings",
                    style = ApturaTypography.displayLarge.copy(fontSize = 36.sp, lineHeight = 42.sp),
                    color = ApturaColors.TextHighEmphasis,
                    modifier = Modifier
                        .padding(top = 64.dp, start = 4.dp)
                        .semantics { heading() }
                )
            }

            item(key = "stats", contentType = "panel") {
                SettingsPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReadoutCell("${libraryStats.totalVideos}", "Videos", Modifier.weight(1f))
                        ReadoutDivider()
                        ReadoutCell(MediaFormatters.formatDuration(libraryStats.totalDurationMs), "Total time", Modifier.weight(1f))
                        ReadoutDivider()
                        ReadoutCell(MediaFormatters.formatFileSize(libraryStats.totalSizeBytes), "Footprint", Modifier.weight(1f))
                    }
                }
            }

            // Appearance
            item(key = "appearance", contentType = "section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Appearance")
                    SettingsPanel {
                        PickerRow(
                            title = "App Theme",
                            subtitle = "System follows your device. Playback and its menus always stay dark.",
                            options = ThemeMode.entries.map { it.label },
                            selectedIndex = settings.themeMode.ordinal,
                            onSelect = { onUpdateThemeMode(ThemeMode.entries[it]) }
                        )
                        PanelDivider()
                        AccentPickerRow(
                            title = "Accent Color",
                            subtitle = "Controls, selection and progress",
                            selected = settings.accentColor,
                            onSelect = onUpdateAccentColor
                        )
                    }
                }
            }

            // Playback (SCR-013)
            item(key = "playback", contentType = "section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Playback")
                    SettingsPanel {
                        // The same eight speeds the player offers, so any speed can also be the default.
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                            RowText("Default Playback Speed", "Every new video starts here")
                            Spacer(Modifier.height(6.dp))
                            SpeedRail(
                                speeds = PlaybackSpeedOptions.all,
                                selectedSpeed = settings.defaultPlaybackSpeed,
                                onSelect = onUpdateSpeed,
                                label = "Default Playback Speed"
                            )
                        }
                        PanelDivider()
                        PickerRow(
                            title = "Default Aspect Ratio",
                            subtitle = null,
                            options = ResizeModeChoices,
                            selectedIndex = settings.defaultResizeMode.takeIf { it in ResizeModeChoices.indices } ?: 0,
                            onSelect = onUpdateResizeMode
                        )
                        PanelDivider()
                        PickerRow(
                            title = "Double-Tap Seek Step",
                            subtitle = "How far each double-tap on the video jumps",
                            options = SeekSteps.map { "${it}s" },
                            selectedIndex = SeekSteps.indexOf(settings.seekDeltaSeconds),
                            onSelect = { onUpdateSeekDelta(SeekSteps[it]) }
                        )
                        PanelDivider()
                        PickerRow(
                            title = "Up Next Countdown",
                            subtitle = "Wait before the next video starts",
                            options = CountdownChoices.map { if (it == 0) "Instant" else "${it}s" },
                            selectedIndex = CountdownChoices.indexOf(settings.autoAdvanceCountdownSeconds),
                            onSelect = { onUpdateAutoAdvanceCountdown(CountdownChoices[it]) }
                        )
                        PanelDivider()
                        ToggleRow(
                            title = "Background Audio Playback",
                            subtitle = "Continue audio when switching apps or locking screen",
                            checked = settings.backgroundAudioEnabled,
                            onCheckedChange = onUpdateBackgroundAudio
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Refresh,
                            title = "Reset Playback Defaults",
                            subtitle = "Restore the default speed, format, seek step, and auto-advance profile",
                            trailing = {
                                PillButton(
                                    text = "Reset",
                                    onClick = { isResetPlaybackDialogOpen = true }
                                )
                            }
                        )
                        PanelDivider()
                        // Decoder Engine Strategy (FR-PLAY-003, Q13)
                        PickerRow(
                            title = "Default Decoder",
                            subtitle = null,
                            options = DecoderChoices,
                            selectedIndex = settings.defaultDecoderStrategy.takeIf { it in DecoderChoices.indices } ?: -1,
                            onSelect = onUpdateDecoderStrategy,
                            footer = {
                                Column {
                                    AnimatedContent(
                                        targetState = settings.defaultDecoderStrategy,
                                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                                        label = "decoder-note"
                                    ) { strategy ->
                                        Text(
                                            text = "${decoderExplanation(strategy)}. A video you changed in the player keeps its own choice.",
                                            style = ApturaTypography.bodySmall,
                                            color = ApturaColors.TextMediumEmphasis,
                                            modifier = Modifier.padding(top = 12.dp)
                                        )
                                    }
                                    Text(
                                        text = "Aptura decodes video with this device's built-in decoders. The bundled " +
                                            "FFmpeg extension adds audio codecs only; a video this phone cannot decode " +
                                            "can be opened in an external player.",
                                        style = ApturaTypography.bodySmall,
                                        color = ApturaColors.TextLowEmphasis,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Library & Storage (SCR-014)
            item(key = "library", contentType = "section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Library & Storage")
                    SettingsPanel {
                        // Custom Storage Folders (SAF - FR-LIB-005)
                        ActionRow(
                            icon = Icons.Default.Folder,
                            title = "Custom Storage Folders",
                            subtitle = "Additional SAF directories & external SD cards",
                            trailing = {
                                PillButton(
                                    text = "Add",
                                    onClick = onAddFolderClick,
                                    leading = {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = ApturaColors.AccentPrimary, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        )
                        if (settings.persistedSafFolders.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                settings.persistedSafFolders.forEach { folderUri ->
                                    FolderChip(
                                        name = folderDisplayName(folderUri),
                                        onRemove = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onRemoveSafFolder(folderUri)
                                        }
                                    )
                                }
                            }
                        }
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Refresh,
                            title = "Rescan Media Library",
                            subtitle = "Discover newly added, renamed or moved video files",
                            onClick = onRescanClick
                        )
                        if (hiddenVideoCount > 0) {
                            PanelDivider()
                            ActionRow(
                                icon = Icons.Default.VisibilityOff,
                                title = "Hidden Videos",
                                subtitle = "$hiddenVideoCount hidden from your library",
                                trailing = { PillButton("Restore all", onRestoreHiddenVideos) }
                            )
                        }
                        if (missingVideoCount > 0) {
                            PanelDivider()
                            ActionRow(
                                icon = Icons.Default.DeleteOutline,
                                title = "Missing Videos",
                                subtitle = "$missingVideoCount unavailable or deleted files",
                                trailing = { PillButton("Remove all", onClearMissingVideos, tint = ApturaColors.StatusError) }
                            )
                        }
                        PanelDivider()
                        // Thumbnail Cache Management (FR-LIB-022)
                        ActionRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Thumbnail Cache",
                            subtitle = "Preview storage: ${MediaFormatters.formatFileSize(cacheSizeBytes)}",
                            trailing = {
                                PillButton(
                                    text = "Clear",
                                    tint = ApturaColors.StatusError,
                                    onClick = {
                                        onClearThumbnailCache()
                                        cacheSizeBytes = 0L
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Privacy & Transparency (SCR-015, SCR-016)
            item(key = "privacy", contentType = "section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Privacy & Transparency")
                    SettingsPanel {
                        // The one place the offline guarantee is stated. The build enforces it: the merged-manifest and
                        // dependency invariant tests fail if INTERNET or a telemetry SDK ever appears.
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ApturaColors.StatusSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "100% Offline & Private",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zero INTERNET permission in Manifest · 0 telemetry SDKs",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                        }
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Memory,
                            title = "Device Codec Support",
                            subtitle = "Video and audio decoders this phone exposes, plus bundled audio codecs",
                            onClick = { isCodecDialogOpen = true },
                            trailing = { NavigationChevron() }
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Share,
                            title = "Export Redacted Diagnostics",
                            subtitle = "Shareable local error log with file paths and PII stripped",
                            onClick = {
                                val markdown = ApturaDiagnostics.generateDiagnosticMarkdown(context)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Aptura Redacted Diagnostic Report")
                                    putExtra(Intent.EXTRA_TEXT, markdown)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Diagnostic Report"))
                            }
                        )
                        PanelDivider()
                        // Delete Diagnostic Data (spec/14 §2): removes the on-device event log and crash reports.
                        ActionRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Diagnostic Data",
                            subtitle = "Erase the local error log and crash reports stored on this device",
                            tint = ApturaColors.StatusError,
                            onClick = { isDeleteDiagnosticsDialogOpen = true }
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Info,
                            title = "Welcome Guide",
                            subtitle = "Gestures, tools and what stays private, in a few pages",
                            onClick = onReplayOnboardingClick,
                            trailing = { NavigationChevron() }
                        )
                    }
                }
            }

            item(key = "footer", contentType = "footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppVersion.label(LocalContext.current),
                        style = ApturaTypography.labelMedium,
                        color = ApturaColors.TextLowEmphasis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Media3 · Jetpack Compose · Room · Coil",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextLowEmphasis
                    )
                }
            }
        }

        // Compact title bar: an opaque strip exactly as tall as the back button/title row. It fades in with the
        // scroll, then hides everything that scrolls beneath it; the hairline makes the edge deliberate instead of
        // letting half-faded content show through the title (the previous gradient faded out inside the strip).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusTop + SETTINGS_TOP_BAR_HEIGHT_DP.dp)
                .graphicsLayer { alpha = collapse() }
                .background(SettingsTopBarBackground)
                .testTag("settings_compact_bar")
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ApturaColors.BorderSubtle)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusTop)
                .height(SETTINGS_TOP_BAR_HEIGHT_DP.dp)
                .padding(horizontal = 8.dp)
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = "Back",
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "Settings",
                style = ApturaTypography.headlineMedium,
                color = ApturaColors.TextHighEmphasis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = collapse() }
                    // The large title is the heading; this one only appears visually, so TalkBack skips it.
                    .clearAndSetSemantics { }
            )
        }
    }
}

// ---- pieces specific to this screen -----------------------------------------------------------------------------

@Composable
private fun FolderChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ApturaColors.SurfaceRecessed)
            .border(1.dp, ApturaColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = ApturaColors.TextMediumEmphasis,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            style = ApturaTypography.bodyMedium,
            color = ApturaColors.TextHighEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Remove folder",
                tint = ApturaColors.StatusError,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun folderDisplayName(folderUri: String): String = try {
    android.net.Uri.decode(folderUri).substringAfterLast(':').substringAfterLast('/').ifBlank { folderUri }
} catch (_: Throwable) {
    folderUri
}

/**
 * Honest capability sheet: what this device can decode, and why video is platform-only.
 * The report is copyable because it is the first thing a support conversation needs.
 */
@Composable
private fun CodecSupportDialog(report: CodecSupportReport?, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ApturaColors.SurfaceElevated2
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Codec Support",
                            style = ApturaTypography.titleLarge,
                            color = ApturaColors.TextHighEmphasis
                        )
                        Text(
                            text = report?.deviceLabel ?: "Reading decoder list...",
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextMediumEmphasis,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aptura uses this device's built-in decoders and bundles no software video " +
                        "decoders. If a codec is missing below, the player offers an external-player hand-off.",
                    style = ApturaTypography.bodySmall,
                    color = ApturaColors.TextMediumEmphasis
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (report == null) {
                        Text(
                            text = "Reading decoder list...",
                            style = ApturaTypography.bodySmall,
                            color = ApturaColors.TextLowEmphasis
                        )
                    } else {
                        CodecSection(title = "Video", entries = report.video)
                        Spacer(modifier = Modifier.height(16.dp))
                        CodecSection(title = "Audio", entries = report.audio)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = CodecSupportReport.FFMPEG_NOTE,
                            style = ApturaTypography.labelSmall,
                            color = ApturaColors.TextLowEmphasis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            report?.let {
                                clipboard.setText(AnnotatedString(it.toPlainText()))
                                Toast.makeText(context, "Codec report copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = report != null
                    ) {
                        Text(
                            text = "Copy report",
                            color = if (report != null) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = "Close", color = ApturaColors.TextMediumEmphasis)
                    }
                }
            }
        }
    }
}

@Composable
private fun CodecSection(title: String, entries: List<CodecSupportEntry>) {
    Text(
        text = title.uppercase(),
        style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = ApturaColors.TextLowEmphasis
    )
    Spacer(modifier = Modifier.height(6.dp))
    if (entries.isEmpty()) {
        Text(
            text = "None reported",
            style = ApturaTypography.bodySmall,
            color = ApturaColors.TextLowEmphasis
        )
        return
    }
    entries.forEach { entry ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    style = ApturaTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = ApturaColors.TextHighEmphasis
                )
                Text(
                    text = entry.decoderName,
                    style = ApturaTypography.labelSmall,
                    color = ApturaColors.TextLowEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (entry.hardware) {
                    ApturaColors.StatusSuccess.copy(alpha = 0.12f)
                } else {
                    ApturaColors.SurfaceElevated3
                }
            ) {
                Text(
                    text = if (entry.hardware) "Hardware" else "Software",
                    style = ApturaTypography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (entry.hardware) ApturaColors.StatusSuccess else ApturaColors.TextMediumEmphasis,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun decoderExplanation(strategy: Int): String = when (strategy) {
    1 -> "Hardware only · GPU/ISP decoders; a codec without one fails instead of falling back"
    2 -> "Platform software · Android's CPU decoders; slower and hotter, but rescues broken hardware paths"
    else -> "Auto · hardware first, then Android's platform software decoder"
}
