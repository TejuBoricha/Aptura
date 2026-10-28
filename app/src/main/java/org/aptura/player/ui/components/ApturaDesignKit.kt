package org.aptura.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/*
 * Aptura's shared building blocks. Screens used to style their own dialogs, buttons, menus and banners, so the same
 * decision ("confirm a destructive action") looked different in every place. These components fix one answer for each
 * decision: one dialog shape and button order, one button height and corner, one menu row, one section heading.
 */

/** What an action does, which decides its colour. Red is reserved for actions that destroy something. */
enum class ApturaTone { Accent, Neutral, Destructive }

private val ButtonShape = RoundedCornerShape(14.dp)
private val ButtonMinHeight = 48.dp

private fun toneContainer(tone: ApturaTone): Color = when (tone) {
    ApturaTone.Accent -> ApturaColors.AccentPrimary
    ApturaTone.Neutral -> ApturaColors.SurfaceElevated3
    ApturaTone.Destructive -> ApturaColors.StatusError
}

private fun toneContent(tone: ApturaTone): Color = when (tone) {
    ApturaTone.Accent -> ApturaColors.TextOnAccent
    ApturaTone.Neutral -> ApturaColors.TextHighEmphasis
    // Black on the bright dark-mode red, white on the deep light-mode red: both clear 4.5:1.
    ApturaTone.Destructive -> ApturaColors.SurfacePureBlack
}

@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(
        text = text,
        style = ApturaTypography.labelLarge.copy(fontFeatureSettings = ""),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** The one filled button. Use it for the single most important action in a view. */
@Composable
fun ApturaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ApturaTone = ApturaTone.Accent,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = toneContainer(tone),
            contentColor = toneContent(tone),
            disabledContainerColor = ApturaColors.SurfaceElevated3,
            disabledContentColor = ApturaColors.TextLowEmphasis
        )
    ) { ButtonLabel(text, icon) }
}

/** A secondary action that should still read as a button: a hairline outline, never a fill. */
@Composable
fun ApturaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: ApturaTone = ApturaTone.Neutral,
    enabled: Boolean = true
) {
    val content = when (tone) {
        ApturaTone.Destructive -> ApturaColors.StatusError
        else -> ApturaColors.TextHighEmphasis
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        border = BorderStroke(
            1.dp,
            if (tone == ApturaTone.Destructive) ApturaColors.StatusError.copy(alpha = 0.45f) else ApturaColors.BorderSubtle
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = content)
    ) { ButtonLabel(text, icon) }
}

/** A quiet action (Cancel, Not now). */
@Composable
fun ApturaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ApturaTone = ApturaTone.Neutral,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = ApturaTypography.labelLarge.copy(fontFeatureSettings = ""),
            fontWeight = FontWeight.SemiBold,
            color = when (tone) {
                ApturaTone.Accent -> ApturaColors.AccentPrimary
                ApturaTone.Destructive -> ApturaColors.StatusError
                ApturaTone.Neutral -> ApturaColors.TextMediumEmphasis
            }
        )
    }
}

/** A second, non-default way to confirm, offered next to the main one (e.g. "Move to trash" beside "Delete"). */
data class DialogAlternative(val label: String, val onClick: () -> Unit)

/**
 * The app's dialog. Title, an optional message, optional custom content, then the actions in a fixed order:
 * Cancel on the left and the confirming action on the right; when there is an alternative, the actions stack
 * full-width with the main one first so long labels never wrap into a ragged pile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApturaDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmLabel: String?,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    confirmTone: ApturaTone = ApturaTone.Accent,
    confirmEnabled: Boolean = true,
    dismissLabel: String? = "Cancel",
    onDismiss: () -> Unit = onDismissRequest,
    alternative: DialogAlternative? = null,
    properties: DialogProperties = DialogProperties(),
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ApturaColors.SurfaceElevated2,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier.widthIn(min = 280.dp, max = 460.dp)
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (confirmTone == ApturaTone.Destructive) ApturaColors.StatusError.copy(alpha = 0.14f)
                                else ApturaColors.AccentSubtle
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (confirmTone == ApturaTone.Destructive) ApturaColors.StatusError else ApturaColors.AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    style = ApturaTypography.headlineSmall,
                    color = ApturaColors.TextHighEmphasis,
                    modifier = Modifier.semantics { heading() }
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (message != null) {
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = message,
                            style = ApturaTypography.bodyLarge,
                            color = ApturaColors.TextMediumEmphasis
                        )
                    }
                    if (content != null) {
                        Spacer(Modifier.size(16.dp))
                        content()
                    }
                }
                Spacer(Modifier.size(20.dp))
                if (alternative != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (confirmLabel != null) {
                            ApturaButton(
                                text = confirmLabel,
                                onClick = onConfirm,
                                tone = confirmTone,
                                enabled = confirmEnabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ApturaOutlinedButton(
                            text = alternative.label,
                            onClick = alternative.onClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (dismissLabel != null) {
                            ApturaTextButton(text = dismissLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissLabel != null) ApturaTextButton(text = dismissLabel, onClick = onDismiss)
                        if (confirmLabel != null) {
                            ApturaButton(text = confirmLabel, onClick = onConfirm, tone = confirmTone, enabled = confirmEnabled)
                        }
                    }
                }
            }
        }
    }
}

/** One option in a list-style dialog (an icon, a label and a line of explanation). */
@Composable
fun DialogOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    tone: ApturaTone = ApturaTone.Neutral
) {
    val tint = if (tone == ApturaTone.Destructive) ApturaColors.StatusError else ApturaColors.TextHighEmphasis
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ApturaColors.SurfaceElevated1,
        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (tone == ApturaTone.Destructive) tint else ApturaColors.AccentPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = ApturaTypography.titleMedium, color = tint)
                if (subtitle != null) {
                    Text(text = subtitle, style = ApturaTypography.bodySmall, color = ApturaColors.TextMediumEmphasis)
                }
            }
        }
    }
}

/**
 * A menu row: an icon, a sentence-case label, and a check when it is the current choice. Colour is not used to mark
 * selection on its own (the check does that) and is never used for decoration; only destructive rows are red.
 */
@Composable
fun ApturaMenuItem(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    tone: ApturaTone = ApturaTone.Neutral,
    selected: Boolean = false
) {
    val color = when {
        tone == ApturaTone.Destructive -> ApturaColors.StatusError
        selected -> ApturaColors.AccentPrimary
        else -> ApturaColors.TextHighEmphasis
    }
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = ApturaTypography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = color
            )
        },
        onClick = onClick,
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (tone == ApturaTone.Destructive) ApturaColors.StatusError else if (selected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = "Selected", tint = ApturaColors.AccentPrimary, modifier = Modifier.size(18.dp)) }
        } else null,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = MenuDefaults.itemColors(),
        modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 200.dp)
    )
}

/** The small caption above a menu's options ("Sort by"). */
@Composable
fun ApturaMenuCaption(text: String) {
    Text(
        text = text,
        style = ApturaTypography.labelMedium.copy(fontFeatureSettings = ""),
        color = ApturaColors.TextLowEmphasis,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

/** A content section's heading, with an optional quiet count ("All videos  8"). */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = ApturaTypography.headlineSmall,
            color = ApturaColors.TextHighEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { heading() }
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = count.toString(),
                style = ApturaTypography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = ApturaColors.TextLowEmphasis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/**
 * A banner that tells the user something about the whole screen (limited access, a crash last time). One look for
 * all of them: a tinted icon, a title, a sentence, and its actions on their own row so they never squeeze the text.
 */
@Composable
fun NoticeBanner(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tint: Color = ApturaColors.AccentPrimary,
    actions: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ApturaColors.SurfaceElevated1,
        border = BorderStroke(1.dp, ApturaColors.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(end = 8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = title, style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
                    Spacer(Modifier.size(2.dp))
                    Text(text = message, style = ApturaTypography.bodyMedium, color = ApturaColors.TextMediumEmphasis)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

/** A placeholder block that breathes while content loads: calmer than a spinner and shaped like what is coming. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(8.dp)) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeleton-alpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            // Ink at low opacity, not a surface token: the light surfaces are too close to the page to read as shapes.
            .background(ApturaColors.TextLowEmphasis.copy(alpha = 0.16f * alpha))
    )
}
