package org.aptura.player.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/*
 * The Settings screen's building blocks. The look is deliberately quiet: flat panels with a hairline edge, one accent
 * (cyan) reserved for "this is selected / this is interactive", red only where an action destroys something, and
 * green only for the offline guarantee. Nothing here is decoration; the one piece of craft is the selector switch.
 */

private val Hairline: Color @Composable get() = ApturaColors.BorderSubtle

// ---- structure --------------------------------------------------------------------------------------------------

/** A flat panel that groups related settings. */
@Composable
internal fun SettingsPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ApturaColors.SurfaceElevated1)
            .border(1.dp, Hairline, shape),
        content = content
    )
}

@Composable
internal fun PanelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Hairline)
    )
}

/** A section heading: small, spaced capitals. It is a real heading for TalkBack. */
@Composable
internal fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = ApturaTypography.labelSmall.copy(letterSpacing = 1.8.sp),
        color = ApturaColors.TextMediumEmphasis,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .semantics { heading() }
    )
}

// ---- rows -------------------------------------------------------------------------------------------------------

@Composable
internal fun RowText(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    titleColor: Color = ApturaColors.TextHighEmphasis
) {
    Column(modifier = modifier) {
        Text(text = title, style = ApturaTypography.titleMedium, color = titleColor)
        if (subtitle != null) {
            Text(text = subtitle, style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
        }
    }
}

/**
 * A tappable row: a plain icon, the text, and an optional trailing element. The icon is neutral grey; red is used only
 * when the action destroys something, so colour tells you something.
 */
@Composable
internal fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    tint: Color = ApturaColors.TextMediumEmphasis,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        RowText(title, subtitle, Modifier.weight(1f))
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
internal fun NavigationChevron() {
    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = ApturaColors.TextLowEmphasis)
}

/** A small outlined action (Add, Clear, Restore). The visible pill is compact; the tap area is still 48 dp. */
@Composable
internal fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ApturaColors.AccentPrimary,
    leading: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f))
                .border(BorderStroke(1.dp, tint.copy(alpha = 0.38f)), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leading?.invoke()
            Text(text = text, style = ApturaTypography.labelSmall.copy(fontSize = 12.sp), color = tint, fontWeight = FontWeight.Bold)
        }
    }
}

/** A switch row: the whole row toggles, and the switch itself is just the indicator. */
@Composable
internal fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowText(title, subtitle, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ApturaColors.SurfacePureBlack,
                checkedTrackColor = ApturaColors.AccentPrimary,
                checkedBorderColor = ApturaColors.AccentPrimary,
                uncheckedThumbColor = ApturaColors.TextMediumEmphasis,
                uncheckedTrackColor = ApturaColors.SurfaceElevated2,
                uncheckedBorderColor = ApturaColors.TextLowEmphasis
            )
        )
    }
}

// ---- selector ---------------------------------------------------------------------------------------------------

/**
 * A selector switch: a dark recessed track and a cyan thumb that slides to the chosen option on a spring.
 * Every option is a real radio button for TalkBack, 48 dp tall, and announces its state.
 * [selectedIndex] may be -1 (a stored value that is not among the options): the thumb simply hides.
 */
@Composable
internal fun SegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val position by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "selector-thumb"
    )
    val thumbAlpha by animateFloatAsState(if (selectedIndex in options.indices) 1f else 0f, tween(180), label = "selector-alpha")
    val trackShape = RoundedCornerShape(15.dp)
    val inset = 3.dp
    val thumbColor = ApturaColors.AccentPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(trackShape)
            .background(ApturaColors.SurfaceRecessed)
            .border(1.dp, Hairline, trackShape)
            .drawBehind {
                if (options.isEmpty()) return@drawBehind
                val pad = inset.toPx()
                val segment = (size.width - pad * 2) / options.size
                drawRoundRect(
                    color = thumbColor.copy(alpha = thumbAlpha),
                    topLeft = Offset(pad + position * segment, pad),
                    size = Size(segment, size.height - pad * 2),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .padding(inset)
            .selectableGroup()
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, text ->
            val selected = index == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (selected) ApturaColors.TextOnAccent else ApturaColors.TextMediumEmphasis,
                animationSpec = tween(180),
                label = "selector-label"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
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
                    style = ApturaTypography.titleMedium.copy(fontSize = 14.sp, fontFeatureSettings = "tnum"),
                    color = textColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** A setting with a title above a [SegmentedPicker]; the optional [subtitle] explains the setting, not its value. */
@Composable
internal fun PickerRow(
    title: String,
    subtitle: String?,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        RowText(title, subtitle)
        Spacer(Modifier.height(12.dp))
        SegmentedPicker(options = options, selectedIndex = selectedIndex, onSelect = onSelect, label = title)
        footer?.invoke()
    }
}

/**
 * A row of colour swatches, one per accent. The swatch shows the variant that belongs to the palette on screen, so
 * choosing "Amber" in light mode shows the exact amber the controls will use. Each dot is a real radio button.
 */
@Composable
internal fun AccentPickerRow(
    title: String,
    subtitle: String,
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
    isSupporter: Boolean = false,
    onLockedAccentClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val isDark = ApturaColors.IsDark
    val goldBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFEEB2), // Specular champagne highlight
                    Color(0xFFF5D77F), // Luminous champagne primary
                    Color(0xFFD4AF37), // Metallic bullion depth
                    Color(0xFFF7E19A)  // Golden sheen edge
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFF9E7C1F),
                    Color(0xFF745700),
                    Color(0xFFB89332),
                    Color(0xFF6B4F00)
                )
            )
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        RowText(title, subtitle)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccentColor.entries.forEach { accent ->
                val isSelected = accent == selected
                val isLocked = accent == AccentColor.GOLD && !isSupporter
                val isGold = accent == AccentColor.GOLD
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                if (isLocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onLockedAccentClick?.invoke()
                                } else if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(accent)
                                }
                            }
                        )
                        .semantics { contentDescription = "${accent.label} accent" }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = when {
                                    isSelected && isGold -> if (ApturaColors.IsDark) Color(0xFFF5D77F) else Color(0xFF745700)
                                    isSelected -> ApturaColors.TextHighEmphasis
                                    else -> Color.Transparent
                                },
                                shape = CircleShape
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .then(
                                if (isGold) Modifier.background(goldBrush)
                                else Modifier.background(accent.previewPrimary(ApturaColors.IsDark))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (ApturaColors.IsDark) Color(0xFF001217) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = if (ApturaColors.IsDark) Color(0xFF001217) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            accent == AccentColor.GOLD && isSupporter -> "Gold ⭐"
                            accent == AccentColor.GOLD -> "Gold 🔒"
                            else -> accent.label
                        },
                        style = ApturaTypography.labelMedium.copy(fontFeatureSettings = ""),
                        color = if (isSelected) ApturaColors.TextHighEmphasis else ApturaColors.TextMediumEmphasis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---- readouts and controls --------------------------------------------------------------------------------------

/** One figure of the library strip: a large tabular number over a small caption. */
@Composable
internal fun ReadoutCell(value: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { }.padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = ApturaTypography.headlineMedium.copy(fontSize = 21.sp, fontFeatureSettings = "tnum"),
            color = ApturaColors.TextHighEmphasis,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption.uppercase(),
            style = ApturaTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.2.sp),
            color = ApturaColors.TextMediumEmphasis,
            maxLines = 1
        )
    }
}

@Composable
internal fun ReadoutDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Hairline)
    )
}

/** A round icon button that stays legible over scrolled content. */
@Composable
internal fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ApturaColors.SurfaceElevated1)
            .border(1.dp, Hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = ApturaColors.TextHighEmphasis)
    }
}
