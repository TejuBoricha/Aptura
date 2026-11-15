package org.aptura.player.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.PlaybackStatsInfo
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import java.util.Locale

internal const val STATS_TAG = "stats_overlay"

/** The widest the panel gets; the player uses it to tell whether the panel and the transport buttons fit side by side. */
internal val STATS_OVERLAY_MAX_WIDTH = 380.dp

/**
 * Playback stats studio HUD overlay (SCR-010e, FR-DIAG-001).
 *
 * A pinned header (title, Copy, Close) over a scrolling readout, so the buttons are reachable however little room the
 * window leaves. Nothing is ever cut mid-letter: the readout's padding sits inside the scrolling area, and the top and
 * bottom edges fade out while more rows lie beyond them. Each row decides for itself whether its label and value fit on
 * one line or the value goes under the label (see [StatsRowLayout]), so no value is squeezed into a few characters of
 * width and broken in the middle of a word.
 *
 * The readout is selectable text via [SelectionContainer], with tabular figures (tnum) per taste-compose.md.
 */
@Composable
fun StatsOverlay(
    stats: PlaybackStatsInfo,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val largeFont = LocalDensity.current.fontScale >= 1.4f
    val scroll = rememberScrollState()

    Surface(
        color = ApturaColors.SurfaceElevated2.copy(alpha = 0.94f),
        shape = RoundedCornerShape(STATS_CORNER_RADIUS),
        border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.35f)),
        modifier = modifier
            .widthIn(min = 280.dp, max = STATS_OVERLAY_MAX_WIDTH)
            .testTag(STATS_TAG)
    ) {
        Column {
            // Header: pinned. Its side padding is at least the panel's corner radius, so nothing sits in a corner.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = STATS_SIDE_PADDING, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = null,
                    tint = ApturaColors.AccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
                // Named as the switch that opens it is ("Playback stats"), and short enough to stay on one line.
                Text(
                    text = "Playback stats",
                    style = ApturaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ApturaColors.TextHighEmphasis,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy stats",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close stats",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // The readout takes the room that is left (never more than it needs) and scrolls inside it.
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fadeScrolledEdges(scroll)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scroll)
                        .padding(start = STATS_SIDE_PADDING, end = STATS_SIDE_PADDING, top = 2.dp, bottom = STATS_SIDE_PADDING)
                ) {
                    // Path & Acceleration Chip: wraps cleanly onto a second line at large accessibility font scales (ISSUE-005).
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = if (largeFont) Arrangement.spacedBy(8.dp) else Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
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
                                    color = if (stats.isHardwareAccelerated) ApturaColors.AccentPrimary else ApturaColors.StatusWarning,
                                    maxLines = 2
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

                    // Selectable diagnostics readout (FR-DIAG-001)
                    SelectionContainer {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(if (largeFont) 8.dp else 6.dp)
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
                                value = if (stats.audioSampleRateHz > 0) "${stats.audioChannels} · ${stats.audioSampleRateHz} Hz" else stats.audioChannels,
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
    }
}

/** The panel's corner radius, and the side padding that keeps every piece of text clear of the curve. */
private val STATS_CORNER_RADIUS = 16.dp
private val STATS_SIDE_PADDING = 16.dp

/** How far the top and bottom edges of the readout dissolve while there is more to scroll to in that direction. */
private val STATS_EDGE_FADE = 22.dp

/**
 * Fades the content's top edge while it has been scrolled down, and its bottom edge while more lies below, instead of
 * cutting a row in half at a hard line. At the very top and the very bottom nothing fades, so a readout that fits is
 * drawn exactly as it is. The scroll position is read while drawing, so scrolling redraws but never recomposes.
 */
private fun Modifier.fadeScrolledEdges(scroll: ScrollState, fade: Dp = STATS_EDGE_FADE): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadePx = fade.toPx().coerceAtMost(size.height / 2f)
        if (fadePx <= 0f) return@drawWithContent
        val above = (scroll.value / fadePx).coerceIn(0f, 1f)
        val below = ((scroll.maxValue - scroll.value) / fadePx).coerceIn(0f, 1f)
        if (above > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 1f - above), Color.Black),
                    startY = 0f,
                    endY = fadePx
                ),
                size = Size(size.width, fadePx),
                blendMode = BlendMode.DstIn
            )
        }
        if (below > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - below)),
                    startY = size.height - fadePx,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - fadePx),
                size = Size(size.width, fadePx),
                blendMode = BlendMode.DstIn
            )
        }
    }

/** Break opportunities after the dots, slashes and underscores of an identifier such as `c2.qti.hevc.decoder.secure`. */
internal fun withBreakOpportunities(value: String): String =
    value.replace(Regex("([./_,])(?=\\S)"), "$1​")

@Composable
private fun StatsRow(
    label: String,
    value: String,
    isTabular: Boolean = false,
    isWarning: Boolean = false
) {
    val valueStyle = ApturaTypography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        fontFeatureSettings = if (isTabular) "tnum" else null
    )
    val valueColor = if (isWarning) ApturaColors.StatusWarning else ApturaColors.TextHighEmphasis

    // A name with no spaces (a decoder, a codec string) that still has to wrap, because even a line of its own is too
    // narrow at this text size, breaks at its dots instead of one letter before the edge. Only then: whole, the text
    // stays exactly what the device reported, which is what a selection copies.
    var wrapsInTheMiddle by remember(value) { mutableStateOf(false) }

    StatsRowLayout(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = ApturaTypography.labelSmall,
            color = ApturaColors.TextMediumEmphasis
        )
        Text(
            text = if (wrapsInTheMiddle) withBreakOpportunities(value) else value,
            style = valueStyle,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            onTextLayout = { layout ->
                if (!wrapsInTheMiddle && layout.lineCount > 1 && value.none(Char::isWhitespace)) {
                    wrapsInTheMiddle = true
                }
            }
        )
    }
}

/**
 * One label and one value. If both fit on one line with a gap between them, the label is on the left and the value
 * flush right, as a table reads. If not, the value goes on its own line under the label, at the full width, where it
 * may wrap at word breaks. Measured from the text itself, so it holds at any panel width and any font size, instead of
 * guessing from the font scale: a value is never squeezed into the few characters left beside a long label.
 */
@Composable
private fun StatsRowLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val label = measurables[0]
        val value = measurables[1]
        val width = constraints.maxWidth
        val gap = 12.dp.roundToPx()
        val labelNatural = label.maxIntrinsicWidth(Constraints.Infinity)
        val valueNatural = value.maxIntrinsicWidth(Constraints.Infinity)
        if (width == Constraints.Infinity || labelNatural + gap + valueNatural <= width) {
            val labelPlaced = label.measure(Constraints(maxWidth = labelNatural + 1))
            val valuePlaced = value.measure(Constraints(maxWidth = valueNatural + 1))
            val height = maxOf(labelPlaced.height, valuePlaced.height)
            val rowWidth = if (width == Constraints.Infinity) labelPlaced.width + gap + valuePlaced.width else width
            layout(rowWidth, height) {
                labelPlaced.placeRelative(0, (height - labelPlaced.height) / 2)
                valuePlaced.placeRelative(rowWidth - valuePlaced.width, (height - valuePlaced.height) / 2)
            }
        } else {
            val labelPlaced = label.measure(Constraints(maxWidth = width))
            val valuePlaced = value.measure(Constraints(maxWidth = width))
            val between = 2.dp.roundToPx()
            layout(width, labelPlaced.height + between + valuePlaced.height) {
                labelPlaced.placeRelative(0, 0)
                valuePlaced.placeRelative(0, labelPlaced.height + between)
            }
        }
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
        appendLine("Audio Sample Rate: " + if (stats.audioSampleRateHz > 0) "${stats.audioSampleRateHz} Hz" else "Unknown")
        appendLine("A/V Delay Offset: ${stats.audioDelayMs} ms")
        appendLine("Subtitle Offset: ${stats.subtitleDelayMs} ms")
        appendLine("HDR Transfer: ${stats.hdrFormat}")
    }
}
