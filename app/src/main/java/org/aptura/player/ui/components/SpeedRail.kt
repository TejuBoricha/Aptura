package org.aptura.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.aptura.player.core.playback.PlaybackSpeedOptions
import org.aptura.player.core.playback.SpeedRailMath
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A speed ramp, not a grid of boxes. Eight fixed cards consumed two rows and made a single number feel like a
 * spreadsheet; this reads as one control: a live value rides the finger, while the ticks rise from left to right so
 * the direction (slower to faster) is visible before you touch anything.
 *
 * The whole rail is one accessible slider: TalkBack changes it with the standard SetProgress action, and the current
 * value is exposed as its state description. The ends name the range, so the scale needs no legend.
 */
@Composable
internal fun SpeedRail(
    speeds: List<Float>,
    selectedSpeed: Float,
    onSelect: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    if (speeds.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val currentOnSelect by rememberUpdatedState(onSelect)
    val externalIndex = SpeedRailMath.indexForValue(speeds, selectedSpeed)

    // Optimistic local index: the setting is persisted asynchronously, and the bubble must not lag the finger.
    var localIndex by remember { mutableIntStateOf(externalIndex) }
    LaunchedEffect(externalIndex) { localIndex = externalIndex }

    // While dragging the needle follows the finger exactly; on release it springs from the finger to the nearest
    // detent. An Animatable is used instead of animateFloatAsState so the snap starts where the finger let go.
    val scope = rememberCoroutineScope()
    val needleSpring = remember { spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val needle = remember { Animatable(SpeedRailMath.fractionForIndex(externalIndex, speeds.size)) }
    val needleFraction = dragFraction ?: needle.value

    LaunchedEffect(externalIndex) {
        if (dragFraction == null) {
            needle.animateTo(SpeedRailMath.fractionForIndex(externalIndex, speeds.size), needleSpring)
        }
    }
    val displayIndex = if (dragFraction != null) {
        SpeedRailMath.indexForFraction(dragFraction!!, speeds.size)
    } else {
        localIndex
    }
    val valueLabel = PlaybackSpeedOptions.label(speeds[displayIndex])
    val firstLabel = PlaybackSpeedOptions.label(speeds.first())
    val lastLabel = PlaybackSpeedOptions.label(speeds.last())

    val textMeasurer = rememberTextMeasurer()
    val valueStyle = ApturaTypography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum"
    )
    val measuredValue = remember(valueLabel, valueStyle, fontScale) {
        textMeasurer.measure(AnnotatedString(valueLabel), style = valueStyle)
    }
    val currentLocalIndex by rememberUpdatedState(localIndex)

    // The bubble grows with the font instead of clipping the number at 200% text size.
    val bubbleHeightPx = with(density) { (measuredValue.size.height + 8.dp.toPx()).coerceAtLeast(24.dp.toPx()) }
    val bubbleWidthPx = with(density) { measuredValue.size.width + 22.dp.toPx() }
    val baselinePx = with(density) { bubbleHeightPx + 5.dp.toPx() + 22.dp.toPx() }
    val railHeight = with(density) { (baselinePx + 6.dp.toPx()).toDp() }

    fun selectAt(x: Float, width: Float) {
        val edgePx = with(density) { 14.dp.toPx() }
        val span = (width - edgePx * 2f).coerceAtLeast(1f)
        val fraction = ((x - edgePx) / span).coerceIn(0f, 1f)
        val index = SpeedRailMath.indexForFraction(fraction, speeds.size)
        if (index != currentLocalIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            localIndex = index
            currentOnSelect(speeds[index])
            scope.launch {
                needle.animateTo(SpeedRailMath.fractionForIndex(index, speeds.size), needleSpring)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(railHeight)
                .testTag("speed_rail")
                .semantics {
                    contentDescription = "$label, $firstLabel to $lastLabel"
                    stateDescription = valueLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = displayIndex.toFloat(),
                        range = 0f..(speeds.size - 1).toFloat(),
                        steps = (speeds.size - 2).coerceAtLeast(0)
                    )
                    setProgress { target ->
                        val index = target.roundToInt().coerceIn(speeds.indices)
                        if (index != currentLocalIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            localIndex = index
                            currentOnSelect(speeds[index])
                            scope.launch {
                                needle.animateTo(SpeedRailMath.fractionForIndex(index, speeds.size), needleSpring)
                            }
                        }
                        true
                    }
                }
                .pointerInput(speeds.size) {
                    detectTapGestures { offset -> selectAt(offset.x, size.width.toFloat()) }
                }
                .pointerInput(speeds.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val edgePx = 14.dp.toPx()
                            val span = (size.width - edgePx * 2f).coerceAtLeast(1f)
                            dragFraction = ((offset.x - edgePx) / span).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val releasedAt = dragFraction
                            dragFraction = null
                            if (releasedAt != null) {
                                scope.launch {
                                    needle.snapTo(releasedAt)
                                    needle.animateTo(
                                        SpeedRailMath.fractionForIndex(currentLocalIndex, speeds.size),
                                        needleSpring
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            dragFraction = null
                            scope.launch {
                                needle.snapTo(SpeedRailMath.fractionForIndex(currentLocalIndex, speeds.size))
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val edgePx = 14.dp.toPx()
                            val span = (size.width - edgePx * 2f).coerceAtLeast(1f)
                            val fraction = ((change.position.x - edgePx) / span).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val index = SpeedRailMath.indexForFraction(fraction, speeds.size)
                            if (index != currentLocalIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                localIndex = index
                                currentOnSelect(speeds[index])
                            }
                        }
                    )
                }
        ) {
            // Read here, not in the draw lambda: tokens belong to the theme this rail is composed in.
            val accent = ApturaColors.AccentPrimary
            val hairline = ApturaColors.BorderSubtle
            val onAccent = ApturaColors.TextOnAccent
            val isLightSurface = ApturaColors.SurfaceBase.luminance() > 0.5f
            Canvas(modifier = Modifier.fillMaxSize()) {
                val edge = 14.dp.toPx()
                val span = (size.width - edge * 2f).coerceAtLeast(1f)
                val needleX = edge + span * needleFraction

                // The ember under the needle: a soft gradient, not a flat disc, so the control feels lit. On a light
                // surface a strong glow reads as a smudge, so it is kept faint there.
                val glowAlpha = if (isLightSurface) 0.10f else 0.30f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(needleX, baselinePx - 8.dp.toPx()),
                        radius = 22.dp.toPx()
                    ),
                    radius = 22.dp.toPx(),
                    center = Offset(needleX, baselinePx - 8.dp.toPx())
                )

                // Baseline and the rising ticks: a shape that already means "slower → faster".
                drawLine(
                    color = hairline,
                    start = Offset(edge, baselinePx),
                    end = Offset(size.width - edge, baselinePx),
                    strokeWidth = 1.dp.toPx()
                )
                speeds.indices.forEach { index ->
                    val tickX = edge + span * SpeedRailMath.fractionForIndex(index, speeds.size)
                    val current = index == displayIndex
                    val tickHeight = (6f + index * 1.5f + if (current) 2f else 0f).dp.toPx()
                    drawRoundRect(
                        color = when {
                            current -> accent
                            index < displayIndex -> accent.copy(alpha = 0.8f)
                            else -> hairline
                        },
                        topLeft = Offset(tickX - (if (current) 1.25.dp.toPx() else 1.dp.toPx()), baselinePx - tickHeight),
                        size = Size(if (current) 2.5.dp.toPx() else 2.dp.toPx(), tickHeight),
                        cornerRadius = CornerRadius(1.25.dp.toPx())
                    )
                }

                // Value bubble + pointer + needle, drawn last so they sit above the ticks.
                val bubbleX = (needleX - bubbleWidthPx / 2f).coerceIn(0f, size.width - bubbleWidthPx)
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(bubbleX, 0f),
                    size = Size(bubbleWidthPx, bubbleHeightPx),
                    cornerRadius = CornerRadius(bubbleHeightPx / 2f)
                )
                val pointer = Path().apply {
                    moveTo(needleX - 5.dp.toPx(), bubbleHeightPx - 1.dp.toPx())
                    lineTo(needleX + 5.dp.toPx(), bubbleHeightPx - 1.dp.toPx())
                    lineTo(needleX, bubbleHeightPx + 5.dp.toPx())
                    close()
                }
                drawPath(pointer, accent)
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(needleX - 1.5.dp.toPx(), bubbleHeightPx + 3.dp.toPx()),
                    size = Size(3.dp.toPx(), baselinePx - bubbleHeightPx - 6.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
                drawText(
                    textLayoutResult = measuredValue,
                    color = onAccent,
                    topLeft = Offset(
                        bubbleX + (bubbleWidthPx - measuredValue.size.width) / 2f,
                        (bubbleHeightPx - measuredValue.size.height) / 2f
                    )
                )
            }
        }

        // The ends name the range; the ticks between them stay unlabelled so the control remains one quiet line.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp)
                .clearAndSetSemantics {},
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = firstLabel,
                style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = ApturaColors.TextMediumEmphasis,
                maxLines = 1
            )
            Text(
                text = lastLabel,
                style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = ApturaColors.TextMediumEmphasis,
                maxLines = 1
            )
        }
    }
}
