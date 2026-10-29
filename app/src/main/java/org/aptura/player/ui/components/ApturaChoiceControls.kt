package org.aptura.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/*
 * Choices that sit in a sheet or on a page. They speak the folder chips' language, a tint of the accent with a hairline
 * of it and accent-coloured text, instead of filling the chosen option with the full accent: three of those stacked in
 * one sheet were loud and flat, and a choice between two layouts was only ever a word.
 */

/**
 * A switch for two to four short options, with a tinted thumb that slides to the chosen one. Every option is a radio
 * button for TalkBack and is as tall as the 48 dp touch target. [selectedIndex] may be -1 (a stored value that is not
 * among the options): the thumb hides.
 */
@Composable
fun TonalSegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val position by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "tonal-thumb"
    )
    val thumbAlpha by animateFloatAsState(if (selectedIndex in options.indices) 1f else 0f, tween(160), label = "tonal-thumb-alpha")
    val accent = ApturaColors.AccentPrimary
    val raised = ApturaColors.SurfaceElevated1
    val trackShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(trackShape)
            .background(ApturaColors.SurfaceRecessed)
            .border(1.dp, ApturaColors.BorderSubtle, trackShape)
            .drawBehind {
                if (options.isEmpty()) return@drawBehind
                val inset = 3.dp.toPx()
                val segment = size.width / options.size
                val topLeft = Offset(position * segment + inset, inset)
                val thumb = Size(segment - inset * 2, size.height - inset * 2)
                val corner = CornerRadius(11.dp.toPx())
                // The sheet's own colour first, so the tint reads as a tint of the page and not as a smudge on the track.
                drawRoundRect(raised.copy(alpha = thumbAlpha), topLeft, thumb, corner)
                drawRoundRect(accent.copy(alpha = 0.14f * thumbAlpha), topLeft, thumb, corner)
                drawRoundRect(accent.copy(alpha = 0.6f * thumbAlpha), topLeft, thumb, corner, style = Stroke(1.25.dp.toPx()))
            }
            .selectableGroup()
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, text ->
            val selected = index == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (selected) accent else ApturaColors.TextMediumEmphasis,
                animationSpec = tween(160),
                label = "tonal-label"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = {
                            if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(index)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = ApturaTypography.titleMedium.copy(fontSize = 14.sp),
                    color = textColor,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * A card for a choice that is easier to see than to read: a small picture of each option above its name. The chosen
 * card takes the accent tint, its hairline and a check. Put the cards in a row with `Modifier.selectableGroup()` and
 * give each `Modifier.weight(1f)`. [preview] draws in the 64 x 44 dp area above the name, in the colour it is given.
 */
@Composable
fun ChoiceCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable (tint: Color) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val accent = ApturaColors.AccentPrimary
    val shape = RoundedCornerShape(16.dp)
    val tint by animateColorAsState(if (selected) accent else ApturaColors.TextLowEmphasis, tween(160), label = "choice-tint")

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.12f) else ApturaColors.SurfaceRecessed)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent.copy(alpha = 0.7f) else ApturaColors.BorderSubtle,
                shape = shape
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(width = 64.dp, height = 44.dp)) { preview(tint) }
            Text(
                text = label,
                style = ApturaTypography.titleMedium.copy(fontSize = 14.sp),
                color = if (selected) accent else ApturaColors.TextHighEmphasis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ApturaColors.TextOnAccent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(18.dp)
                    .background(accent, CircleShape)
                    .padding(3.dp)
            )
        }
    }
}
