package org.aptura.player.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.MediaFormatters

/**
 * Settings Root Screen (SCR-013, SCR-014, SCR-015, SCR-016).
 * Unified configuration for playback preferences, storage statistics, diagnostics, and privacy proof.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onRescanClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onUpdateDecoderStrategy: (Int) -> Unit = {},
    onClearThumbnailCache: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isLicensesDialogOpen by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        cacheSizeBytes = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ApturaColors.SurfaceBase,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = ApturaTypography.headlineMedium,
                        color = ApturaColors.TextHighEmphasis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ApturaColors.TextHighEmphasis
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ApturaColors.SurfaceBase
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Playback Preferences
            item {
                SettingsSectionHeader("Playback Preferences")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ApturaColors.SurfaceElevated1)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Default Speed
                        Text(
                            text = "Default Playback Speed",
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                FilterChip(
                                    selected = settings.defaultPlaybackSpeed == speed,
                                    onClick = { onUpdateSpeed(speed) },
                                    label = { Text("${speed}x", style = ApturaTypography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ApturaColors.AccentPrimary,
                                        selectedLabelColor = ApturaColors.SurfacePureBlack,
                                        containerColor = ApturaColors.SurfaceElevated2,
                                        labelColor = ApturaColors.TextHighEmphasis
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Double-tap Seek Delta
                        Text(
                            text = "Double-Tap Seek Step",
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15, 30).forEach { seconds ->
                                FilterChip(
                                    selected = settings.seekDeltaSeconds == seconds,
                                    onClick = { onUpdateSeekDelta(seconds) },
                                    label = { Text("${seconds}s", style = ApturaTypography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ApturaColors.AccentPrimary,
                                        selectedLabelColor = ApturaColors.SurfacePureBlack,
                                        containerColor = ApturaColors.SurfaceElevated2,
                                        labelColor = ApturaColors.TextHighEmphasis
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-Advance Countdown
                        Text(
                            text = "Up Next Countdown",
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15, 0).forEach { seconds ->
                                val label = if (seconds == 0) "Immediate" else "${seconds}s"
                                FilterChip(
                                    selected = settings.autoAdvanceCountdownSeconds == seconds,
                                    onClick = { onUpdateAutoAdvanceCountdown(seconds) },
                                    label = { Text(label, style = ApturaTypography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ApturaColors.AccentPrimary,
                                        selectedLabelColor = ApturaColors.SurfacePureBlack,
                                        containerColor = ApturaColors.SurfaceElevated2,
                                        labelColor = ApturaColors.TextHighEmphasis
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Background Audio Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Audio Playback",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis
                                )
                                Text(
                                    text = "Continue audio when switching apps or locking screen",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                            Switch(
                                checked = settings.backgroundAudioEnabled,
                                onCheckedChange = onUpdateBackgroundAudio,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ApturaColors.SurfacePureBlack,
                                    checkedTrackColor = ApturaColors.AccentPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Decoder Engine Strategy (FR-PLAY-003, Q13)
                        Text(
                            text = "Decoder Engine Strategy",
                            style = ApturaTypography.titleMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                        val decoderExplanation = when (settings.defaultDecoderStrategy) {
                            1 -> "Hardware · Low battery consumption, relies on GPU decoders"
                            2 -> "Software · CPU decoding fallback for problematic streams"
                            else -> "Auto · Optimal balance between hardware decoders and compatibility"
                        }
                        Text(
                            text = decoderExplanation,
                            style = ApturaTypography.bodySmall,
                            color = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "Auto",
                                1 to "Hardware",
                                2 to "Software"
                            ).forEach { (strategy, label) ->
                                FilterChip(
                                    selected = settings.defaultDecoderStrategy == strategy,
                                    onClick = { onUpdateDecoderStrategy(strategy) },
                                    label = { Text(label, style = ApturaTypography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ApturaColors.AccentPrimary,
                                        selectedLabelColor = ApturaColors.SurfacePureBlack,
                                        containerColor = ApturaColors.SurfaceElevated2,
                                        labelColor = ApturaColors.TextHighEmphasis
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Storage & Indexing (SCR-014)
            item {
                SettingsSectionHeader("Library & Storage")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ApturaColors.SurfaceElevated1)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StorageStatPill("Videos", "${libraryStats.totalVideos}")
                            StorageStatPill("Total Time", MediaFormatters.formatDuration(libraryStats.totalDurationMs))
                            StorageStatPill("Footprint", MediaFormatters.formatFileSize(libraryStats.totalSizeBytes))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ApturaColors.SurfaceElevated2)
                                .clickable(onClick = onRescanClick)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rescan Media Library",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis
                                )
                                Text(
                                    text = "Discover newly added, renamed or moved video files",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Thumbnail Cache Management (FR-LIB-022)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ApturaColors.SurfaceElevated2)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Thumbnail Cache",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis
                                )
                                Text(
                                    text = "Preview storage: ${MediaFormatters.formatFileSize(cacheSizeBytes)}",
                                    style = ApturaTypography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ApturaColors.SurfaceElevated3,
                                border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onClearThumbnailCache()
                                    cacheSizeBytes = 0L
                                }
                            ) {
                                Text(
                                    text = "Clear",
                                    style = ApturaTypography.labelSmall,
                                    color = ApturaColors.StatusError,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Privacy & Diagnostics (SCR-015, SCR-016)
            item {
                SettingsSectionHeader("Privacy & Transparency")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ApturaColors.SurfaceElevated1)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Offline Trust Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ApturaColors.SurfaceElevated2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "100% Offline & Private",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zero INTERNET permission in Manifest · 0 telemetry SDKs",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.AccentPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Diagnostics Export Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ApturaColors.SurfaceElevated2)
                                .clickable {
                                    val markdown = ApturaDiagnostics.generateDiagnosticMarkdown(context)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Aptura Redacted Diagnostic Report")
                                        putExtra(Intent.EXTRA_TEXT, markdown)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export Diagnostic Report"))
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Redacted Diagnostics",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis
                                )
                                Text(
                                    text = "Shareable local error log with file paths and PII stripped",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Replay Onboarding
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ApturaColors.SurfaceElevated2)
                                .clickable(onClick = onReplayOnboardingClick)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ApturaColors.TextMediumEmphasis,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replay Welcome Guide",
                                    style = ApturaTypography.titleMedium,
                                    color = ApturaColors.TextHighEmphasis
                                )
                                Text(
                                    text = "Review Aptura features and privacy architecture",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = ApturaColors.TextLowEmphasis
                            )
                        }
                    }
                }
            }

            // Section 4: App Information & Licenses
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aptura Video Player v1.0.0 (Build 63)",
                        style = ApturaTypography.labelMedium,
                        color = ApturaColors.TextLowEmphasis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Media3 · Jetpack Compose · Room · Coil · LGPL FFmpeg",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextLowEmphasis
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = ApturaTypography.titleSmall,
        color = ApturaColors.AccentPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun StorageStatPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(ApturaColors.SurfaceElevated2, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = ApturaTypography.titleMedium,
            color = ApturaColors.TextHighEmphasis,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = ApturaTypography.labelSmall,
            color = ApturaColors.TextMediumEmphasis
        )
    }
}
