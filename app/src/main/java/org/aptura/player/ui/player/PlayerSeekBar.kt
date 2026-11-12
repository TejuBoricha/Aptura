package org.aptura.player.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.AbRepeatState
import org.aptura.player.domain.model.Bookmark
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.MediaFormatters

/**
 * Leaf elapsed-time label that reads [positionProvider] only inside its own composition scope,
 * isolating 4 Hz playback ticks from the rest of [PlayerScreen].
 */
@Composable
internal fun PlayerElapsedTimeText(
    positionProvider: () -> PlayerPositionState,
    isScrubbing: Boolean,
    scrubPositionMs: Long,
    modifier: Modifier = Modifier
) {
    val effectiveMs = if (isScrubbing) scrubPositionMs else positionProvider().currentPositionMs
    Text(
        text = MediaFormatters.formatDuration(effectiveMs),
        style = ApturaTypography.labelSmall.copy(
            fontFeatureSettings = "tnum"
        ),
        maxLines = 1,
        softWrap = false,
        color = ApturaColors.TextHighEmphasis,
        modifier = modifier
    )
}

/**
 * Video scrubber with draw-phase position reads and stable `pointerInput(Unit)` gesture detectors.
 */
@Composable
internal fun PlayerSeekBar(
    positionProvider: () -> PlayerPositionState,
    durationMs: Long,
    isScrubbing: Boolean,
    scrubPositionMs: Long,
    seekStepMs: Long,
    bookmarks: List<Bookmark>,
    abRepeat: AbRepeatState,
    onScrubStart: (Long) -> Unit,
    onScrubMove: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var scrubberWidthPx by remember { mutableFloatStateOf(0f) }

    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing) 8.dp else 4.dp,
        animationSpec = spring(),
        label = "scrubberTrackHeight"
    )
    val thumbRadius by animateDpAsState(
        targetValue = if (isScrubbing) 10.dp else 8.dp,
        animationSpec = spring(),
        label = "scrubberThumbRadius"
    )

    val currentDurationMs by rememberUpdatedState(durationMs)
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrubMove by rememberUpdatedState(onScrubMove)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)

    // Read in composition: a draw lambda cannot read theme tokens.
    // The unfilled track is the bar's boundary: ControlTrack is 3:1 on the player's black (WCAG 1.4.11), where
    // SurfaceElevated3 was 1.57:1. Buffered sits on top of it, so its alpha was raised to stay a visible step.
    val scrubberTrackColor = ApturaColors.ControlTrack
    val scrubberBufferedColor = ApturaColors.TextMediumEmphasis.copy(alpha = 0.6f)
    val scrubberAccent = ApturaColors.AccentPrimary

    Box(
        modifier = modifier
            .height(48.dp)
            .systemGestureExclusion()
            .onSizeChanged { scrubberWidthPx = it.width.toFloat() }
            // TalkBack: an adjustable seek bar (it used to be an unlabelled drawing).
            .semantics {
                val effectiveMs = if (isScrubbing) scrubPositionMs else positionProvider().currentPositionMs
                contentDescription = "Seek bar"
                stateDescription = "${MediaFormatters.formatDuration(effectiveMs)} of " +
                    MediaFormatters.formatDuration(durationMs)
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = effectiveMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(0f)),
                    range = 0f..durationMs.toFloat().coerceAtLeast(0f)
                )
                setProgress(label = "Seek") { targetMs ->
                    if (durationMs <= 0L) {
                        false
                    } else {
                        val target = targetMs.toLong().coerceIn(0L, durationMs)
                        onScrubStart(target)
                        onScrubMove(target)
                        onScrubEnd()
                        true
                    }
                }
                customActions = listOf(
                    CustomAccessibilityAction("Rewind ${seekStepMs / 1000} seconds") {
                        onSeekRelative(-seekStepMs)
                        true
                    },
                    CustomAccessibilityAction("Forward ${seekStepMs / 1000} seconds") {
                        onSeekRelative(seekStepMs)
                        true
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val dur = currentDurationMs
                        if (scrubberWidthPx > 0f && dur > 0) {
                            val fraction = (offset.x / scrubberWidthPx).coerceIn(0f, 1f)
                            val targetMs = (fraction * dur).toLong()
                            currentOnScrubStart(targetMs)
                            currentOnScrubMove(targetMs)
                            tryAwaitRelease()
                            currentOnScrubEnd()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val dur = currentDurationMs
                        if (scrubberWidthPx > 0f && dur > 0) {
                            val fraction = (offset.x / scrubberWidthPx).coerceIn(0f, 1f)
                            val targetMs = (fraction * dur).toLong()
                            currentOnScrubStart(targetMs)
                            currentOnScrubMove(targetMs)
                        }
                    },
                    onDragEnd = {
                        currentOnScrubEnd()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragCancel = {
                        currentOnScrubEnd()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val dur = currentDurationMs
                        if (scrubberWidthPx > 0f && dur > 0) {
                            val fraction = (change.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                            val targetMs = (fraction * dur).toLong()
                            currentOnScrubMove(targetMs)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val cy = size.height / 2f
            val trackH = trackHeight.toPx()
            val thumbR = thumbRadius.toPx()
            val totalW = size.width

            val pos = positionProvider()
            val effectivePosMs = if (isScrubbing) scrubPositionMs else pos.currentPositionMs
            val progressFrac = if (durationMs > 0L) {
                (effectivePosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val bufferedFrac = if (durationMs > 0L) {
                (pos.bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            // 1. Background Inactive Track
            drawRoundRect(
                color = scrubberTrackColor,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(totalW, trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
            )

            // 2. Buffered Track
            if (bufferedFrac > 0f) {
                val buffW = totalW * bufferedFrac
                drawRoundRect(
                    color = scrubberBufferedColor,
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(buffW, trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                )
            }

            // 3. Active Played Track
            val activeW = totalW * progressFrac
            if (activeW > 0f) {
                drawRoundRect(
                    color = scrubberAccent,
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(activeW, trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                )
            }

            // 3b. Bookmark Pips (FR-CTRL-011)
            if (durationMs > 0 && bookmarks.isNotEmpty()) {
                val pipW = 3.dp.toPx()
                val pipH = (trackH * 1.8f).coerceAtLeast(8.dp.toPx())
                bookmarks.forEach { bookmark ->
                    val pipFraction = (bookmark.timestampMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    val pipX = totalW * pipFraction
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.9f),
                        topLeft = Offset(pipX - pipW / 2f, cy - pipH / 2f),
                        size = Size(pipW, pipH),
                        cornerRadius = CornerRadius(pipW / 2f, pipW / 2f)
                    )
                }
            }

            // 3c. A-B Repeat Loop Markers & Span (F-19, FR-CTRL-010)
            if (durationMs > 0) {
                val ptA = abRepeat.pointA
                val ptB = abRepeat.pointB
                val markerW = 3.dp.toPx()
                val markerH = (trackH * 2.4f).coerceAtLeast(12.dp.toPx())
                if (ptA != null && ptB != null && ptB > ptA) {
                    val xA = totalW * (ptA.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    val xB = totalW * (ptB.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    drawRoundRect(
                        color = scrubberAccent.copy(alpha = 0.32f),
                        topLeft = Offset(xA, cy - trackH),
                        size = Size((xB - xA).coerceAtLeast(0f), trackH * 2f),
                        cornerRadius = CornerRadius(trackH, trackH)
                    )
                }
                if (ptA != null) {
                    val xA = totalW * (ptA.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    drawRoundRect(
                        color = scrubberAccent,
                        topLeft = Offset(xA - markerW / 2f, cy - markerH / 2f),
                        size = Size(markerW, markerH),
                        cornerRadius = CornerRadius(markerW / 2f, markerW / 2f)
                    )
                }
                if (ptB != null) {
                    val xB = totalW * (ptB.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    drawRoundRect(
                        color = scrubberAccent,
                        topLeft = Offset(xB - markerW / 2f, cy - markerH / 2f),
                        size = Size(markerW, markerH),
                        cornerRadius = CornerRadius(markerW / 2f, markerW / 2f)
                    )
                }
            }

            // 4. Thumb Circle (Concentric cy = size.height / 2f)
            val clampedThumbX = if (totalW > thumbR * 2) {
                activeW.coerceIn(thumbR, totalW - thumbR)
            } else {
                totalW / 2f
            }
            drawCircle(
                color = scrubberAccent,
                radius = thumbR,
                center = Offset(clampedThumbX, cy)
            )
        }

        // 5. Floating Timecode Preview Chip while scrubbing (DESIGN.md §5.1, P2-19)
        if (isScrubbing && scrubberWidthPx > 0f) {
            val density = LocalDensity.current
            val chipWidthPx = with(density) { 68.dp.toPx() }
            val scrubFrac = if (durationMs > 0L) {
                (scrubPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val rawThumbX = scrubberWidthPx * scrubFrac
            val chipOffsetX = (rawThumbX - chipWidthPx / 2f)
                .coerceIn(0f, (scrubberWidthPx - chipWidthPx).coerceAtLeast(0f))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ApturaColors.SurfaceElevated2,
                border = BorderStroke(1.dp, ApturaColors.AccentPrimary),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            x = chipOffsetX.toInt(),
                            y = with(density) { (-22).dp.roundToPx() }
                        )
                    }
            ) {
                Text(
                    text = MediaFormatters.formatDuration(scrubPositionMs),
                    style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = ApturaColors.AccentPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
