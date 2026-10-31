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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/*
 * The chrome every player sheet shares. Each sheet used to invent its own header (an icon tile here, a bare title
 * there, a close button on one of five) and its own tab style (outlined, filled, dark pill). Speed, Audio, Subtitles,
 * Playback, Queue and the explainers now open the same way, so the player feels like one instrument.
 */

/** Icon tile, title, one line of status, and an optional close button. */
@Composable
internal fun PlayerSheetHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleIsActive: Boolean = false,
    onClose: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ApturaColors.AccentSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ApturaColors.AccentPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ApturaTypography.titleLarge.copy(fontSize = 18.sp),
                color = ApturaColors.TextHighEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ApturaTypography.bodySmall,
                    color = if (subtitleIsActive) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
        if (onClose != null) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ApturaColors.TextMediumEmphasis,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * The one tab style: a recessed track, and the chosen tab raised onto the next surface with accent text and icon.
 * Each tab is 44 dp inside a 52 dp track, announced as a tab with its selected state.
 */
@Composable
internal fun PlayerSheetTabs(
    tabs: List<Pair<String, ImageVector>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ApturaColors.SurfaceElevated1,
        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .semantics {
                    collectionInfo = CollectionInfo(rowCount = 1, columnCount = tabs.size)
                }
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                val isSelected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ApturaColors.SurfaceElevated3 else Color.Transparent)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                            collectionItemInfo = CollectionItemInfo(
                                rowIndex = 0,
                                rowSpan = 1,
                                columnIndex = index,
                                columnSpan = 1
                            )
                        }
                        .clickable(role = Role.Tab) {
                            if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(index)
                        }
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = ApturaTypography.labelLarge.copy(fontFeatureSettings = ""),
                        color = if (isSelected) ApturaColors.TextHighEmphasis else ApturaColors.TextMediumEmphasis,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** A group heading inside a sheet: quiet spaced capitals, the same everywhere (accent is kept for state). */
@Composable
internal fun PlayerSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = ApturaTypography.labelSmall.copy(letterSpacing = 1.4.sp, fontFeatureSettings = ""),
        color = ApturaColors.TextMediumEmphasis,
        modifier = modifier.semantics { heading() }
    )
}
