package org.aptura.player.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.PlaybackStatsInfo
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import java.util.Locale

/**
 * Playback stats studio HUD overlay (SCR-010e, FR-DIAG-001).
 * Features a compact 2-column live telemetry readout, selectable text
 * via [SelectionContainer], and tabular figures (tnum) per taste-compose.md.
 */
@Composable
fun StatsOverlay(
    stats: PlaybackStatsInfo,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    Surface(
        color = ApturaColors.SurfaceElevated2.copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.35f)),
        modifier = modifier
            .widthIn(min = 280.dp, max = 380.dp)
            .testTag("stats_overlay")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Icon, Title, Path Badge, Copy and Close Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Diagnostics & Stats",
                        style = ApturaTypography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ApturaColors.TextHighEmphasis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Copy Diagnostics Text Button
                    IconButton(
                        onClick = {
                            val bundle = buildDiagnosticsString(stats)
                            clipboardManager.setText(AnnotatedString(bundle))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Stats",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Stats",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Path & Acceleration Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (stats.isHardwareAccelerated) {
                        ApturaColors.AccentPrimary.copy(alpha = 0.15f)
                    } else {
                        ApturaColors.StatusWarning.copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        0.5.dp,
                        if (stats.isHardwareAccelerated) ApturaColors.AccentPrimary else ApturaColors.StatusWarning
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (stats.isHardwareAccelerated) ApturaColors.AccentPrimary else ApturaColors.StatusWarning,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (stats.isHardwareAccelerated) "Hardware Accelerated" else "Compatibility Fallback",
                            style = ApturaTypography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stats.isHardwareAccelerated) ApturaColors.AccentPrimary else ApturaColors.StatusWarning
                        )
                    }
                }

                Text(
                    text = stats.hdrFormat,
                    style = ApturaTypography.labelSmall,
                    color = ApturaColors.TextMediumEmphasis
                )
            }

            HorizontalDivider(
                color = ApturaColors.BorderSubtle.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Two-column selectable diagnostics readout (FR-DIAG-001)
            SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Decoder Section
                    StatsRow(label = "Decode Path", value = stats.decoderPath)
                    StatsRow(label = "Video Decoder", value = stats.videoDecoderName.ifBlank { "Auto" })
                    if (stats.videoCodec.isNotBlank()) {
                        StatsRow(label = "Video Codec", value = stats.videoCodec)
                    }

                    // Video Stream Section
                    if (stats.videoResolution.isNotBlank()) {
                        val fpsStr = if (stats.frameRate > 0f) " @ %.1f fps".format(Locale.US, stats.frameRate) else ""
                        StatsRow(
                            label = "Resolution / FPS",
                            value = "${stats.videoResolution}$fpsStr",
                            isTabular = true
                        )
                    }

                    val bitrateStr = if (stats.videoBitrateKbps > 0) {
                        if (stats.videoBitrateKbps >= 1000) {
                            "%.1f Mbps".format(Locale.US, stats.videoBitrateKbps / 1000f)
                        } else {
                            "${stats.videoBitrateKbps} kbps"
                        }
                    } else "Unknown"
                    StatsRow(label = "Video Bitrate", value = bitrateStr, isTabular = true)

                    val dropPctStr = "%.1f%%".format(Locale.US, stats.dropPercentage)
                    StatsRow(
                        label = "Dropped Frames",
                        value = "${stats.droppedFrames} ($dropPctStr)",
                        isTabular = true,
                        isWarning = stats.dropPercentage > 5f
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        color = ApturaColors.BorderSubtle.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Audio Section
                    StatsRow(label = "Audio Decoder", value = stats.audioDecoderName.ifBlank { "Auto" })
                    if (stats.audioCodec.isNotBlank()) {
                        StatsRow(label = "Audio Codec", value = stats.audioCodec)
                    }
                    StatsRow(
                        label = "Audio Channels",
                        value = "${stats.audioChannels} · ${stats.audioSampleRateHz} Hz",
                        isTabular = true
                    )
                    if (stats.audioBitrateKbps > 0) {
                        StatsRow(
                            label = "Audio Bitrate",
                            value = "${stats.audioBitrateKbps} kbps",
                            isTabular = true
                        )
                    }

                    // A/V Sync Section
                    val avSign = if (stats.audioDelayMs >= 0) "+" else ""
                    val subSign = if (stats.subtitleDelayMs >= 0) "+" else ""
                    StatsRow(
                        label = "A/V Sync Offset",
                        value = "$avSign${stats.audioDelayMs} ms",
                        isTabular = true
                    )
                    StatsRow(
                        label = "Subtitle Offset",
                        value = "$subSign${stats.subtitleDelayMs} ms",
                        isTabular = true
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    label: String,
    value: String,
    isTabular: Boolean = false,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ApturaTypography.labelSmall,
            color = ApturaColors.TextMediumEmphasis
        )
        Text(
            text = value,
            style = ApturaTypography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontFeatureSettings = if (isTabular) "tnum" else null
            ),
            fontWeight = FontWeight.Medium,
            color = if (isWarning) ApturaColors.StatusWarning else ApturaColors.TextHighEmphasis
        )
    }
}

private fun buildDiagnosticsString(stats: PlaybackStatsInfo): String {
    return buildString {
        appendLine("Aptura Playback Diagnostics (FR-DIAG-001)")
        appendLine("Decode Path: ${stats.decoderPath}")
        appendLine("Video Decoder: ${stats.videoDecoderName}")
        appendLine("Video Codec: ${stats.videoCodec}")
        appendLine("Resolution: ${stats.videoResolution}")
        appendLine("Frame Rate: ${stats.frameRate} fps")
        appendLine("Video Bitrate: ${stats.videoBitrateKbps} kbps")
        appendLine("Dropped Frames: ${stats.droppedFrames} (${stats.dropPercentage}%)")
        appendLine("Audio Decoder: ${stats.audioDecoderName}")
        appendLine("Audio Codec: ${stats.audioCodec}")
        appendLine("Audio Channels: ${stats.audioChannels}")
        appendLine("Audio Sample Rate: ${stats.audioSampleRateHz} Hz")
        appendLine("A/V Delay Offset: ${stats.audioDelayMs} ms")
        appendLine("Subtitle Offset: ${stats.subtitleDelayMs} ms")
        appendLine("HDR Transfer: ${stats.hdrFormat}")
    }
}
