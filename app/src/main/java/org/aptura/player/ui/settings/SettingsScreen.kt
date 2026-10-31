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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import org.aptura.player.ui.components.StableNavigationBars
import org.aptura.player.ui.components.StableStatusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
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
import org.aptura.player.ui.components.ApturaDialog
import org.aptura.player.ui.components.ApturaTone
import org.aptura.player.theme.ThemeMode
import org.aptura.player.ui.components.SpeedRail
import org.aptura.player.ui.components.isShortWindowHeight
import org.aptura.player.ui.library.MediaFormatters

private val ResizeModeChoices = listOf("Fit", "Fill", "Zoom", "16:9", "4:3")
private val SeekSteps = listOf(5, 10, 15, 30)
private val CountdownChoices = listOf(5, 10, 15, 0) // seconds; 0 plays the next video straight away
private val DecoderChoices = listOf("Auto", "Hardware", "Software")

internal const val VIDEO_ACCESS_BUTTON_TAG = "settings_video_access_button"

/** How far (in dp) the large title has to scroll before the compact title bar has fully taken over. */
private const val TITLE_COLLAPSE_DP = 90

/** Height of the compact bar's content row. The bar is drawn on top of the list, so it must cover exactly this. */
internal const val SETTINGS_TOP_BAR_HEIGHT_DP = 56

/**
 * The compact bar must be an opaque fill. A gradient that fades out inside the bar's own height lets the rows
 * scrolling underneath show through the back button and title (audit 2026-10-24).
 */
internal val SettingsTopBarBackground: Brush
    @Composable get() = SolidColor(ApturaColors.SurfaceBase)

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
    hasVideoAccess: Boolean = true,
    onVideoAccessClick: () -> Unit = {},
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
    onUpdateSupporter: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isDeleteDiagnosticsDialogOpen by remember { mutableStateOf(false) }
    var isResetPlaybackDialogOpen by remember { mutableStateOf(false) }
    var isCodecDialogOpen by remember { mutableStateOf(false) }
    var isPrivacyPolicyDialogOpen by remember { mutableStateOf(false) }
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
        ApturaDialog(
            onDismissRequest = { isDeleteDiagnosticsDialogOpen = false },
            icon = Icons.Default.DeleteForever,
            title = "Delete diagnostic data?",
            message = "This erases the error log and crash reports Aptura keeps on this device. " +
                "Nothing was ever sent anywhere, and this can't be undone.",
            confirmLabel = "Delete",
            confirmTone = ApturaTone.Destructive,
            onConfirm = {
                isDeleteDiagnosticsDialogOpen = false
                onDeleteDiagnosticData()
                Toast.makeText(context, "Diagnostic data deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (isResetPlaybackDialogOpen) {
        ApturaDialog(
            onDismissRequest = { isResetPlaybackDialogOpen = false },
            icon = Icons.Default.Refresh,
            title = "Reset playback defaults?",
            message = "This restores the default speed, aspect ratio, double-tap skip, background audio and " +
                "up next countdown.",
            confirmLabel = "Reset",
            onConfirm = {
                isResetPlaybackDialogOpen = false
                onResetPlaybackDefaults()
                Toast.makeText(context, "Playback defaults restored", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (isPrivacyPolicyDialogOpen) {
        ApturaDialog(
            onDismissRequest = { isPrivacyPolicyDialogOpen = false },
            icon = Icons.Default.Security,
            title = "Privacy Policy",
            message = "Aptura is an offline video player built on zero-knowledge and zero-telemetry principles:\n\n" +
                "• No Network Permission: Aptura does not request android.permission.INTERNET and cannot open any network connection.\n\n" +
                "• 100% On-Device: Your files, playback progress, bookmarks, and settings never leave your device.\n\n" +
                "• Zero Tracking: No analytics, crash reporting, advertising SDKs, or accounts.\n\n" +
                "• Scoped Storage: Access is strictly confined to standard MediaStore video reads and user-selected SAF folders.",
            confirmLabel = "View on GitHub",
            onConfirm = {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/TejuBoricha/Aptura/blob/main/docs/PRIVACY.md"))
                context.startActivity(intent)
            },
            dismissLabel = "Close"
        )
    }

    // The compact title bar fades in as the large title scrolls away. The scroll position is read inside the
    // graphicsLayer lambdas, so scrolling redraws the bar but never recomposes the screen.
    // On a short window (a phone held sideways) the large title would take a third of the screen above the first
    // setting, so there is no large title: the compact bar is simply always there, and the list starts under it.
    val shortWindow = isShortWindowHeight()
    val listState = rememberLazyListState()
    val collapseTravelPx = with(LocalDensity.current) { TITLE_COLLAPSE_DP.dp.toPx() }
    val collapse = remember(listState, collapseTravelPx, shortWindow) {
        {
            if (shortWindow) {
                1f
            } else {
                val scrolled = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
                (scrolled / collapseTravelPx).coerceIn(0f, 1f)
            }
        }
    }

    val statusTop = StableStatusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = StableNavigationBars.asPaddingValues().calculateBottomPadding()

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
                top = if (shortWindow) statusTop + SETTINGS_TOP_BAR_HEIGHT_DP.dp + 8.dp else statusTop,
                bottom = navigationBottom + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!shortWindow) {
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
            }

            item(key = "stats", contentType = "panel") {
                SettingsPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReadoutCell("${libraryStats.totalVideos}", "Videos", Modifier.weight(1f))
                        ReadoutDivider()
                        ReadoutCell(MediaFormatters.formatRuntime(libraryStats.totalDurationMs), "Runtime", Modifier.weight(1f))
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
                            title = "Theme",
                            subtitle = "System follows your device. Playback and its menus always stay dark.",
                            options = ThemeMode.entries.map { it.label },
                            selectedIndex = settings.themeMode.ordinal,
                            onSelect = { onUpdateThemeMode(ThemeMode.entries[it]) }
                        )
                        PanelDivider()
                        AccentPickerRow(
                            title = "Accent color",
                            subtitle = "Controls, selection and progress",
                            selected = settings.accentColor,
                            onSelect = onUpdateAccentColor,
                            isSupporter = settings.isSupporter,
                            onLockedAccentClick = {
                                Toast.makeText(
                                    context,
                                    "Champagne Gold is unlocked with the Supporter Badge below! 🏅",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
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
                            RowText("Default speed", "Every new video starts at this speed")
                            Spacer(Modifier.height(6.dp))
                            SpeedRail(
                                speeds = PlaybackSpeedOptions.all,
                                selectedSpeed = settings.defaultPlaybackSpeed,
                                onSelect = onUpdateSpeed,
                                label = "Default speed"
                            )
                        }
                        PanelDivider()
                        PickerRow(
                            title = "Default aspect ratio",
                            subtitle = null,
                            options = ResizeModeChoices,
                            selectedIndex = settings.defaultResizeMode.takeIf { it in ResizeModeChoices.indices } ?: 0,
                            onSelect = onUpdateResizeMode
                        )
                        PanelDivider()
                        PickerRow(
                            title = "Double-tap skip",
                            subtitle = "How far a double-tap on the video jumps",
                            options = SeekSteps.map { "${it}s" },
                            selectedIndex = SeekSteps.indexOf(settings.seekDeltaSeconds),
                            onSelect = { onUpdateSeekDelta(SeekSteps[it]) }
                        )
                        PanelDivider()
                        PickerRow(
                            title = "Up next countdown",
                            subtitle = "How long to wait before the next video starts",
                            options = CountdownChoices.map { if (it == 0) "Instant" else "${it}s" },
                            selectedIndex = CountdownChoices.indexOf(settings.autoAdvanceCountdownSeconds),
                            onSelect = { onUpdateAutoAdvanceCountdown(CountdownChoices[it]) }
                        )
                        PanelDivider()
                        ToggleRow(
                            title = "Background audio",
                            subtitle = "Keep listening when you switch apps or lock the screen",
                            checked = settings.backgroundAudioEnabled,
                            onCheckedChange = onUpdateBackgroundAudio
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Refresh,
                            title = "Reset playback defaults",
                            subtitle = "Speed, aspect ratio, double-tap skip and countdown",
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
                            title = "Default decoder",
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
                                }
                            }
                        )
                    }
                }
            }

            // Library & Storage (SCR-014)
            item(key = "library", contentType = "section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Library & storage")
                    SettingsPanel {
                        // Whether Aptura may list the videos on this device. It used to be answerable only in the first-run
                        // dialog, so there was no way to look at it, give it later or take it back.
                        ActionRow(
                            icon = Icons.Default.VideoLibrary,
                            title = "Video access",
                            subtitle = if (hasVideoAccess) {
                                "Allowed. Aptura can list the videos on this device."
                            } else {
                                "Not allowed. Aptura only sees videos you open or add."
                            },
                            trailing = {
                                PillButton(
                                    text = if (hasVideoAccess) "Manage" else "Allow",
                                    onClick = onVideoAccessClick,
                                    modifier = Modifier.testTag(VIDEO_ACCESS_BUTTON_TAG)
                                )
                            }
                        )
                        PanelDivider()
                        // Custom Storage Folders (SAF - FR-LIB-005)
                        ActionRow(
                            icon = Icons.Default.Folder,
                            title = "Extra folders",
                            subtitle = "Folders and SD cards Aptura can't find on its own",
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
                            title = "Rescan library",
                            subtitle = "Find new, renamed or moved videos",
                            onClick = onRescanClick
                        )
                        if (hiddenVideoCount > 0) {
                            PanelDivider()
                            ActionRow(
                                icon = Icons.Default.VisibilityOff,
                                title = "Hidden videos",
                                subtitle = "$hiddenVideoCount hidden from your library",
                                trailing = { PillButton("Restore all", onRestoreHiddenVideos) }
                            )
                        }
                        if (missingVideoCount > 0) {
                            PanelDivider()
                            ActionRow(
                                icon = Icons.Default.DeleteOutline,
                                title = "Missing videos",
                                subtitle = "$missingVideoCount no longer found on this device",
                                trailing = { PillButton("Remove all", onClearMissingVideos, tint = ApturaColors.StatusError) }
                            )
                        }
                        PanelDivider()
                        // Thumbnail Cache Management (FR-LIB-022)
                        ActionRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Thumbnail cache",
                            subtitle = "${MediaFormatters.formatFileSize(cacheSizeBytes)} of preview images",
                            trailing = {
                                // Clearing a cache loses nothing (previews are rebuilt), so it is not painted as destructive.
                                PillButton(
                                    text = "Clear",
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
                    SectionLabel("Privacy")
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
                                    text = "100% offline and private",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "No internet permission and no analytics. The build fails if either ever appears.",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                        }
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Memory,
                            title = "Codec support",
                            subtitle = "Which video and audio formats this device can decode",
                            onClick = { isCodecDialogOpen = true },
                            trailing = { NavigationChevron() }
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Share,
                            title = "Share diagnostic report",
                            subtitle = "An error log with file names and personal details removed",
                            onClick = {
                                val markdown = ApturaDiagnostics.generateDiagnosticMarkdown(context)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("tejasboricha225@gmail.com"))
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
                            title = "Delete diagnostic data",
                            subtitle = "Erase the error log and crash reports kept on this device",
                            tint = ApturaColors.StatusError,
                            onClick = { isDeleteDiagnosticsDialogOpen = true }
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Info,
                            title = "Welcome guide",
                            subtitle = "Gestures, tools and what stays private",
                            onClick = onReplayOnboardingClick,
                            trailing = { NavigationChevron() }
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "Help & feedback",
                            subtitle = "Questions, suggestions or bug reports directly to the developer",
                            onClick = {
                                val subject = "[Aptura Feedback] Query / Report - v${AppVersion.versionCode(context)}"
                                val body = """
                                    [Please describe your issue, question, or suggestion here]


                                    ---------------------------------------------
                                    Category: [ Bug Report / Feature Request / General Question ]
                                    Video Format (if playback issue): [ e.g. MKV, MP4, 4K, Subtitles ]

                                    --- Diagnostic Info (Please do not edit) ---
                                    App: ${AppVersion.label(context)}
                                    Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                                    Android OS: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
                                    ---------------------------------------------
                                """.trimIndent()
                                val encodedSubject = android.net.Uri.encode(subject)
                                val encodedBody = android.net.Uri.encode(body)
                                val mailtoUri = android.net.Uri.parse("mailto:tejasboricha225@gmail.com?subject=$encodedSubject&body=$encodedBody")
                                val mailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("tejasboricha225@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                    putExtra(Intent.EXTRA_TEXT, body)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(mailIntent, "Contact Developer"))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Contact: tejasboricha225@gmail.com", Toast.LENGTH_LONG).show()
                                }
                            },
                            trailing = { NavigationChevron() }
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Security,
                            title = "Privacy policy",
                            subtitle = "Zero data collection, 100% offline guarantee",
                            onClick = { isPrivacyPolicyDialogOpen = true },
                            trailing = { NavigationChevron() }
                        )
                        PanelDivider()
                        ActionRow(
                            icon = Icons.Default.Code,
                            title = "Source code",
                            subtitle = "Public Apache-2.0 repository on GitHub",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/TejuBoricha/Aptura"))
                                context.startActivity(intent)
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = ApturaColors.TextLowEmphasis,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Support Independent Development
            item(key = "support", contentType = "section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Support Aptura")
                    SettingsPanel {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6E9C),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Support Independent Development",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Aptura is 100% free, offline, and private with zero ads or tracking. If you'd like to support continued development, voluntary patronage is deeply appreciated.",
                                style = ApturaTypography.bodyMedium,
                                color = ApturaColors.TextMediumEmphasis
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PillButton(
                                    text = "Tip on Ko-fi",
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ko-fi.com/aptura"))
                                        context.startActivity(intent)
                                    },
                                    leading = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = ApturaColors.AccentPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                                PillButton(
                                    text = "GitHub Sponsors",
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/sponsors/TejuBoricha"))
                                        context.startActivity(intent)
                                    },
                                    leading = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = ApturaColors.AccentPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                        PanelDivider()
                        ToggleRow(
                            title = "I Supported Aptura 🏅",
                            subtitle = "Honor-system toggle. Unlocks the Champagne Gold theme accent and a golden supporter badge.",
                            checked = settings.isSupporter,
                            onCheckedChange = { supported ->
                                onUpdateSupporter(supported)
                                if (supported) {
                                    onUpdateAccentColor(AccentColor.GOLD)
                                    Toast.makeText(context, "Thank you for supporting Aptura! Champagne Gold unlocked ⭐", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        if (settings.isSupporter) {
                            PanelDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5D77F).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFFF5D77F).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF5D77F).copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⭐", style = ApturaTypography.titleSmall)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Champagne Gold Active",
                                        style = ApturaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (ApturaColors.IsDark) Color(0xFFF5D77F) else Color(0xFF745700)
                                    )
                                    Text(
                                        text = "Supporter status unlocked the exclusive Champagne Gold theme.",
                                        style = ApturaTypography.bodySmall,
                                        color = ApturaColors.TextMediumEmphasis
                                    )
                                }
                            }
                        }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = AppVersion.label(LocalContext.current),
                            style = ApturaTypography.labelMedium,
                            color = ApturaColors.TextLowEmphasis
                        )
                        if (settings.isSupporter) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "⭐ Supporter",
                                style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF5D77F)
                            )
                        }
                    }
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
                    // Normally the large title is the heading and this one only appears visually, so TalkBack skips it.
                    // With no large title (a short window) this is the heading.
                    .then(if (shortWindow) Modifier.semantics { heading() } else Modifier.clearAndSetSemantics { })
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
    ApturaDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Memory,
        title = "Codec support",
        message = "Aptura plays video with this device's own decoders. If a format is missing below, the player " +
            "offers to open the video in another app.",
        confirmLabel = "Copy report",
        confirmEnabled = report != null,
        onConfirm = {
            report?.let {
                clipboard.setText(AnnotatedString(it.toPlainText()))
                Toast.makeText(context, "Codec report copied", Toast.LENGTH_SHORT).show()
            }
        },
        dismissLabel = "Close"
    ) {
        Text(
            text = report?.deviceLabel ?: "Reading decoder list\u2026",
            style = ApturaTypography.labelMedium,
            color = ApturaColors.TextLowEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (report != null) {
            CodecSection(title = "Video", entries = report.video)
            Spacer(modifier = Modifier.height(16.dp))
            CodecSection(title = "Audio", entries = report.audio)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = CodecSupportReport.FFMPEG_NOTE,
                style = ApturaTypography.bodySmall,
                color = ApturaColors.TextLowEmphasis
            )
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
    1 -> "Hardware only: fastest and coolest, but a format without a hardware decoder won't play"
    2 -> "Software: slower and uses more battery, but can rescue a video the hardware decoder garbles"
    else -> "Auto: hardware first, then software if the hardware decoder fails"
}
