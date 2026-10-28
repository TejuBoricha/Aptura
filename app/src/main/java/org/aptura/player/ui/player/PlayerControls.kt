package org.aptura.player.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.ApturaTextButton
import org.aptura.player.ui.components.ApturaTone
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToLong

/*
 * The controls every player sheet is built from. Each tab used to draw its own: five kinds of selector, fake ON/OFF
 * pills next to real switches, three slider styles, and sections that were sometimes cards and sometimes not. Every
 * tab now uses these, plus the Settings selector switch for one-of-N choices, so a control looks and behaves the same
 * wherever it appears.
 */

/** A named group: a quiet heading (with an optional action on the right) over one panel. */
@Composable
internal fun SheetSection(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerSectionLabel(text = title, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(6.dp))
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(ApturaColors.SurfaceElevated1)
                .border(1.dp, ApturaColors.BorderSubtle, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

/** A setting's own name and, optionally, one plain sentence about it. */
@Composable
internal fun SheetRowText(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = title, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = ApturaTypography.bodySmall, color = ApturaColors.TextMediumEmphasis)
        }
    }
}

/** On/off. Always a real switch, and the whole row toggles it. */
@Composable
internal fun SheetSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Switch) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        SheetRowText(title, subtitle, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ApturaColors.TextOnAccent,
                checkedTrackColor = ApturaColors.AccentPrimary,
                checkedBorderColor = ApturaColors.AccentPrimary,
                uncheckedThumbColor = ApturaColors.TextMediumEmphasis,
                uncheckedTrackColor = ApturaColors.SurfaceElevated3,
                uncheckedBorderColor = ApturaColors.TextLowEmphasis
            )
        )
    }
}

/** A label with its current value on the right, over a slider. */
@Composable
internal fun SheetSliderRow(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    origin: Float = valueRange.start
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            SheetRowText(title, subtitle, Modifier.weight(1f))
            Text(
                text = valueText,
                style = ApturaTypography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = if (value != origin) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis
            )
        }
        ApturaSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            origin = origin,
            label = title,
            valueText = valueText
        )
    }
}

/**
 * The player's one slider: a 4 dp track, the filled part drawn from [origin] (the start, or the middle for an offset
 * that can go either way) to a 20 dp thumb, inside a 48 dp touch band. The Material slider drew a thick split track
 * with a stray stop dot; this matches the speed rail instead. It is a real slider for TalkBack.
 */
@Composable
internal fun ApturaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    valueText: String,
    modifier: Modifier = Modifier,
    origin: Float = valueRange.start
) {
    val currentOnChange by rememberUpdatedState(onValueChange)
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val thumbRadius = 10.dp
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                contentDescription = label
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
                setProgress { target ->
                    currentOnChange(target.coerceIn(valueRange))
                    true
                }
            }
            .pointerInput(valueRange) {
                val pad = thumbRadius.toPx()
                fun at(x: Float) = valueRange.start + ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f) * span
                detectTapGestures { currentOnChange(at(it.x)) }
            }
            .pointerInput(valueRange) {
                val pad = thumbRadius.toPx()
                fun at(x: Float) = valueRange.start + ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f) * span
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnChange(at(change.position.x))
                }
            }
    ) {
        val pad = thumbRadius.toPx()
        val usable = size.width - 2 * pad
        fun xOf(v: Float) = pad + ((v - valueRange.start) / span).coerceIn(0f, 1f) * usable
        val cy = size.height / 2f
        val track = 4.dp.toPx()
        drawRoundRect(
            color = ApturaColors.SurfaceElevated3,
            topLeft = Offset(pad, cy - track / 2),
            size = Size(usable, track),
            cornerRadius = CornerRadius(track / 2)
        )
        val from = xOf(origin)
        val to = xOf(value)
        drawRoundRect(
            color = ApturaColors.AccentPrimary,
            topLeft = Offset(minOf(from, to), cy - track / 2),
            size = Size(abs(to - from), track),
            cornerRadius = CornerRadius(track / 2)
        )
        if (origin != valueRange.start) {
            // The zero mark of a two-sided offset.
            drawRect(
                color = ApturaColors.TextMediumEmphasis,
                topLeft = Offset(from - 1.dp.toPx(), cy - 7.dp.toPx()),
                size = Size(2.dp.toPx(), 14.dp.toPx())
            )
        }
        drawCircle(color = ApturaColors.SurfaceElevated2, radius = thumbRadius.toPx() + 2.dp.toPx(), center = Offset(to, cy))
        drawCircle(color = ApturaColors.AccentPrimary, radius = thumbRadius.toPx(), center = Offset(to, cy))
    }
}

/** "0 ms", "+150 ms", "−250 ms", "+1.2 s": a real minus sign, a space before the unit, seconds past one second. */
internal fun formatOffset(ms: Long): String {
    if (ms == 0L) return "0 ms"
    val sign = if (ms > 0) "+" else "−"
    val magnitude = abs(ms)
    return if (magnitude >= 1000L) {
        val seconds = magnitude / 1000.0
        "$sign${"%.2f".format(seconds).trimEnd('0').trimEnd('.')} s"
    } else {
        "$sign$magnitude ms"
    }
}

/**
 * Timing for audio or subtitles. The old controls showed a number and two "+50ms/−50ms" buttons and left the user
 * to guess which way was which; this says what the current value *does* in words, names both ends of the slider,
 * nudges in fixed steps with − and +, and offers Reset only when there is something to reset.
 *
 * [describe] turns the offset into a sentence ("Sound plays 150 ms later than the picture").
 */
@Composable
internal fun TimingOffsetControl(
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    rangeMs: Long,
    stepMs: Long,
    earlierLabel: String,
    laterLabel: String,
    label: String,
    describe: (Long) -> String
) {
    val haptic = LocalHapticFeedback.current
    fun set(ms: Long) {
        val clamped = ms.coerceIn(-rangeMs, rangeMs)
        if (clamped != offsetMs) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onOffsetChange(clamped)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatOffset(offsetMs),
                    style = ApturaTypography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    color = if (offsetMs != 0L) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                )
                Text(
                    text = describe(offsetMs),
                    style = ApturaTypography.bodyMedium,
                    color = ApturaColors.TextMediumEmphasis,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (offsetMs != 0L) {
                ApturaTextButton(text = "Reset", onClick = { set(0L) }, tone = ApturaTone.Accent)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(Icons.Default.Remove, "${formatOffset(-stepMs)}, $earlierLabel") { set(offsetMs - stepMs) }
            ApturaSlider(
                value = offsetMs.toFloat(),
                onValueChange = { raw ->
                    // Snap to the step so dragging lands on the same values the buttons reach.
                    set((raw / stepMs).roundToLong() * stepMs)
                },
                valueRange = -rangeMs.toFloat()..rangeMs.toFloat(),
                origin = 0f,
                label = label,
                valueText = formatOffset(offsetMs),
                modifier = Modifier.weight(1f)
            )
            StepButton(Icons.Default.Add, "${formatOffset(stepMs)}, $laterLabel") { set(offsetMs + stepMs) }
        }
        Row(modifier = Modifier.padding(horizontal = 58.dp)) {
            Text(text = earlierLabel, style = ApturaTypography.bodySmall, color = ApturaColors.TextLowEmphasis, modifier = Modifier.weight(1f))
            Text(text = laterLabel, style = ApturaTypography.bodySmall, color = ApturaColors.TextLowEmphasis)
        }
    }
}

/** Hold to keep stepping: crossing a second in 50 ms taps is twenty taps. A tap is still exactly one step. */
@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onStep: () -> Unit) {
    val currentOnStep by rememberUpdatedState(onStep)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var repeatedWhileHeld by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        repeatedWhileHeld = false
        delay(STEP_HOLD_DELAY_MS)
        while (true) {
            repeatedWhileHeld = true
            currentOnStep()
            delay(STEP_REPEAT_MS)
        }
    }
    IconButton(
        onClick = { if (repeatedWhileHeld) repeatedWhileHeld = false else currentOnStep() },
        interactionSource = interaction,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ApturaColors.SurfaceElevated3)
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = ApturaColors.TextHighEmphasis, modifier = Modifier.size(20.dp))
    }
}

private const val STEP_HOLD_DELAY_MS = 400L
private const val STEP_REPEAT_MS = 90L

/** One choice in a list (an audio track, a decoder): a check marks the chosen one, not colour alone. */
@Composable
internal fun SheetOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated1)
            .border(1.dp, if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.7f) else ApturaColors.BorderSubtle, shape)
            .semantics { this.selected = selected }
            .clickableWithRole(Role.RadioButton) {
                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ApturaTypography.titleMedium,
                color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(text = subtitle, style = ApturaTypography.bodySmall, color = ApturaColors.TextMediumEmphasis)
            }
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** A quick pick among presets: outlined, filled with the accent tint and checked when chosen. */
@Composable
internal fun SheetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(if (selected) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated2)
            .border(1.dp, if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.6f) else ApturaColors.BorderSubtle, shape)
            .semantics { this.selected = selected }
            .clickableWithRole(Role.RadioButton) {
                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = ApturaTypography.labelLarge.copy(fontFeatureSettings = ""),
            color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
        )
    }
}

private fun Modifier.clickableWithRole(role: Role, onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(role = role, onClick = onClick))
