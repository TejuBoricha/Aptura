package org.aptura.player.ui.player

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import kotlin.math.PI
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.ApturaButton
import org.aptura.player.ui.components.ApturaDialog
import org.aptura.player.ui.components.ApturaOutlinedButton
import org.aptura.player.ui.components.ApturaTone
import org.aptura.player.ui.library.MediaFormatters

/*
 * The sleep timer in the Playback tools sheet (FR-CTRL-009, DESIGN.md §5.6).
 *
 * It reads top to bottom as: where the timer stands (a moon inside a ring that winds down with the countdown, the time
 * left in large figures and the clock time it will stop at), the two things you can do about a running timer (add five
 * minutes, turn it off), and then the choices as equal tiles, so one tap starts a timer and the one that is running is
 * always marked, even after the sheet has been closed and reopened.
 */

/** FR-CTRL-009's documented ladder, pinned by `SleepTimerOptionsTest` so a layout change cannot drop an option. */
internal val SLEEP_TIMER_PRESET_MINUTES = listOf(10, 15, 20, 30, 45, 60)

/** The custom-duration field: digits only, 1 to 720 minutes. Null until the text is a usable duration. */
internal fun customSleepMinutes(text: String): Int? = text.toIntOrNull()?.takeIf { it in 1..720 }

internal const val SLEEP_TIMER_TAG = "sleep_timer"
internal const val SLEEP_DIAL_TAG = "sleep_dial"

private const val MINUTE_MS = 60_000L

/** What the timer is doing, as far as the sheet is concerned. */
internal enum class SleepMode { Off, Counting, AtEndOfVideo, Expired }

/** Which choice below the status is the one in force, so it can be marked. */
internal sealed interface SleepChoice {
    data object None : SleepChoice
    data class Preset(val minutes: Int) : SleepChoice
    data class Custom(val minutes: Int) : SleepChoice
    data object EndOfVideo : SleepChoice
}

internal fun sleepMode(counting: Boolean, expired: Boolean, atEnd: Boolean): SleepMode = when {
    counting -> SleepMode.Counting
    expired -> SleepMode.Expired
    atEnd -> SleepMode.AtEndOfVideo
    else -> SleepMode.Off
}

/**
 * The choice in force, read from the timer itself rather than from what the sheet remembers: the sheet is built anew
 * each time it opens, and a timer started earlier must still show which time it was set to. A countdown that has been
 * extended past a preset (30 min and then +5) is a 35 minute timer and shows as a custom one.
 */
internal fun sleepChoice(counting: Boolean, totalMs: Long, atEnd: Boolean): SleepChoice = when {
    atEnd -> SleepChoice.EndOfVideo
    counting && totalMs > 0L -> {
        val minutes = ((totalMs + MINUTE_MS - 1) / MINUTE_MS).toInt()
        if (minutes in SLEEP_TIMER_PRESET_MINUTES) SleepChoice.Preset(minutes) else SleepChoice.Custom(minutes)
    }
    else -> SleepChoice.None
}

/**
 * How much of the countdown is left, 0 to 1: the length of the arc that is still lit. No countdown is an empty ring. A
 * countdown with no recorded length (the timer always records one, so this is only a safeguard) is drawn lit rather than
 * empty, because an empty ring beside "Stops in 19:59" would read as a timer that has ended.
 */
internal fun sleepDialFraction(remainingMs: Long?, totalMs: Long): Float = when {
    remainingMs == null -> 0f
    totalMs <= 0L -> 1f
    else -> (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

@Composable
internal fun SleepTimerSection(
    endsAtElapsedRealtime: Long?,
    remainingMs: Long?,
    totalMs: Long,
    expired: Boolean,
    atEndOfVideo: Boolean,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onExtend: (Int) -> Unit,
    onSleepAtEndOfVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = sleepMode(counting = remainingMs != null, expired = expired, atEnd = atEndOfVideo)
    val choice = sleepChoice(counting = remainingMs != null, totalMs = totalMs, atEnd = atEndOfVideo)
    var customOpen by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }

    SheetSection("Sleep timer", modifier = modifier.testTag(SLEEP_TIMER_TAG)) {
        SleepStatus(
            mode = mode,
            remainingMs = remainingMs,
            totalMs = totalMs,
            endsAtElapsedRealtime = endsAtElapsedRealtime
        )

        when (mode) {
            SleepMode.Counting, SleepMode.Expired -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ApturaButton(
                    text = "+5 min",
                    icon = Icons.Filled.Add,
                    onClick = { onExtend(5) },
                    // After the timer has fired this is the way back, so it is the one filled accent button.
                    tone = if (mode == SleepMode.Expired) ApturaTone.Accent else ApturaTone.Neutral,
                    modifier = Modifier.weight(1f)
                )
                ApturaOutlinedButton(
                    text = if (mode == SleepMode.Expired) "Dismiss" else "Turn off",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
            }
            SleepMode.AtEndOfVideo -> ApturaOutlinedButton(
                text = "Turn off",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
            SleepMode.Off -> Unit
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Pause playback in",
                style = ApturaTypography.labelMedium.copy(fontFeatureSettings = ""),
                color = ApturaColors.TextMediumEmphasis
            )
            // Eight choices do not fit one line at phone width. Equal tiles in rows of three read as a keypad, and a
            // wide window (a tablet, a phone held sideways) gets one row of six, so the section stays short.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 440.dp) 6 else 3
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SLEEP_TIMER_PRESET_MINUTES.chunked(columns).forEach { rowMinutes ->
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMinutes.forEach { minutes ->
                                SleepPresetTile(
                                    minutes = minutes,
                                    selected = choice == SleepChoice.Preset(minutes),
                                    onClick = { onStart(minutes) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val customMinutes = (choice as? SleepChoice.Custom)?.minutes
                        SleepModeTile(
                            icon = Icons.Outlined.Edit,
                            label = "Custom",
                            detail = customMinutes?.let { "$it min" },
                            selected = customMinutes != null,
                            onClick = {
                                customText = ""
                                customOpen = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SleepModeTile(
                            icon = Icons.Outlined.Movie,
                            label = "End of video",
                            detail = null,
                            selected = choice == SleepChoice.EndOfVideo,
                            onClick = onSleepAtEndOfVideo,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    if (customOpen) {
        val customMinutes = customSleepMinutes(customText)
        ApturaDialog(
            onDismissRequest = { customOpen = false },
            title = "Custom sleep timer",
            message = "Minutes from now. The sound fades out over the last 10 seconds.",
            confirmLabel = "Start timer",
            onConfirm = {
                customMinutes?.let {
                    onStart(it)
                    customOpen = false
                }
            },
            confirmEnabled = customMinutes != null,
            icon = Icons.Filled.Bedtime,
            content = {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { text -> customText = text.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Minutes") },
                    supportingText = { Text("1 to 720 minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = ApturaTypography.bodyMedium.copy(
                        color = ApturaColors.TextHighEmphasis,
                        fontWeight = FontWeight.Medium
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ApturaColors.SurfaceElevated1,
                        unfocusedContainerColor = ApturaColors.SurfaceElevated1,
                        focusedBorderColor = ApturaColors.AccentPrimary,
                        unfocusedBorderColor = ApturaColors.BorderSubtle,
                        cursorColor = ApturaColors.AccentPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        )
    }
}

/** The ring and what it means, in words. One reading for a screen reader: the ring itself says nothing. */
@Composable
private fun SleepStatus(
    mode: SleepMode,
    remainingMs: Long?,
    totalMs: Long,
    endsAtElapsedRealtime: Long?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        SleepDial(mode = mode, fraction = sleepDialFraction(remainingMs, totalMs))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            when (mode) {
                SleepMode.Counting -> {
                    val left = MediaFormatters.formatDuration(remainingMs ?: 0L)
                    // One text node ("Stops in 19:59") so it is read as a sentence and found by the device test scripts;
                    // the time is set larger than the words around it.
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ApturaColors.TextMediumEmphasis)) {
                                append("Stops in ")
                            }
                            withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ApturaColors.TextHighEmphasis)) {
                                append(left)
                            }
                        },
                        style = ApturaTypography.titleLarge.copy(fontFeatureSettings = "tnum")
                    )
                    val stopsAt = rememberStopClockTime(endsAtElapsedRealtime)
                    if (stopsAt != null) {
                        Text(
                            text = "Pauses at $stopsAt",
                            style = ApturaTypography.titleSmall,
                            color = ApturaColors.AccentPrimary
                        )
                    }
                    Text(
                        text = "The sound fades out over the last 10 seconds",
                        style = ApturaTypography.bodySmall.copy(lineBreak = LineBreak.Heading),
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
                SleepMode.AtEndOfVideo -> StatusLines("Stops when this video ends", "Up next won't start")
                SleepMode.Expired -> StatusLines(
                    "Sleep timer ended",
                    "Playback is paused. Add five more minutes, or pick a new time."
                )
                SleepMode.Off -> StatusLines("Off", "Pause playback after a while")
            }
        }
    }
}

@Composable
private fun StatusLines(title: String, subtitle: String) {
    Text(text = title, style = ApturaTypography.titleLarge, color = ApturaColors.TextHighEmphasis)
    // Balanced line breaking: a sentence that needs two lines gets two even ones, never a lone word on the second.
    Text(
        text = subtitle,
        style = ApturaTypography.bodySmall.copy(lineBreak = LineBreak.Heading),
        color = ApturaColors.TextMediumEmphasis
    )
}

/**
 * The time of day the countdown will pause playback, in the phone's own 12 or 24 hour style. Worked out once per
 * deadline from the two clocks, so it does not tick and does not depend on the half-second refresh above it.
 */
@Composable
private fun rememberStopClockTime(endsAtElapsedRealtime: Long?): String? {
    val context = LocalContext.current
    return remember(endsAtElapsedRealtime) {
        endsAtElapsedRealtime?.let { endsAt ->
            val stopsAtWallClock = System.currentTimeMillis() + (endsAt - SystemClock.elapsedRealtime())
            android.text.format.DateFormat.getTimeFormat(context).format(Date(stopsAtWallClock))
        }
    }
}

/**
 * A moon in a ring. The lit part of the ring is the part of the countdown still to run, so the ring unwinds as the
 * night goes on and a glance says "about half left" without reading a number. "At the end of this video" has no length
 * to unwind, so its ring is a dotted circle instead of a filled arc.
 */
@Composable
private fun SleepDial(mode: SleepMode, fraction: Float, modifier: Modifier = Modifier) {
    // Linear over the same half second the countdown refreshes in, so the arc moves steadily instead of in jumps.
    val shown by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "sleepDial"
    )
    val accent = ApturaColors.AccentPrimary
    val track = ApturaColors.ControlTrack
    val lit = mode == SleepMode.Counting || mode == SleepMode.AtEndOfVideo
    Box(
        modifier = modifier
            .size(84.dp)
            .testTag(SLEEP_DIAL_TAG)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f + 3.dp.toPx()
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            if (lit) {
                // The ember behind the moon, kept faint so it never reads as a smudge.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f,
                    center = center
                )
            }
            if (mode == SleepMode.AtEndOfVideo) {
                val dots = 28
                val spacing = (PI.toFloat() * arcSize.width) / dots
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(
                        width = stroke * 0.75f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, spacing - 0.1f))
                    )
                )
            } else {
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                if (shown > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * shown,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Filled.Bedtime,
            contentDescription = null,
            tint = if (lit) accent else ApturaColors.TextMediumEmphasis,
            modifier = Modifier.size(30.dp)
        )
    }
}

/** One time to choose: the number is the point, so it is large and the unit is small beside it. */
@Composable
private fun SleepPresetTile(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated2)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.75f) else ApturaColors.BorderSubtle,
                shape = shape
            )
            .selectable(selected = selected, role = Role.RadioButton) {
                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // One text node, "10 min", so it is read and found as the words it shows.
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)) { append("$minutes") }
                withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)) { append(" min") }
            },
            style = ApturaTypography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
            textAlign = TextAlign.Center
        )
        if (selected) {
            // A check as well as the colour, as everywhere else a choice is marked.
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
            )
        }
    }
}

/** A choice that is a way of stopping rather than a number of minutes: Custom, and the end of the video. */
@Composable
private fun SleepModeTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (selected) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated2)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.75f) else ApturaColors.BorderSubtle,
                shape = shape
            )
            .selectable(selected = selected, role = Role.RadioButton) {
                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.Check else icon,
            contentDescription = null,
            tint = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = ApturaTypography.labelLarge.copy(fontFeatureSettings = ""),
                color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = ApturaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = ApturaColors.AccentPrimary
                )
            }
        }
    }
}
