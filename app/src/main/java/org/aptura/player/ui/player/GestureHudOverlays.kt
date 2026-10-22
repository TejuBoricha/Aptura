package org.aptura.player.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.MediaFormatters

/**
 * Vertical level gauge HUD for edge drag gestures (FR-CTRL-005, FR-CTRL-015, SCR-010).
 * Displays a sleek vertical pill on the screen edge (left for brightness, right for volume)
 * with a proportional progress track and tabular percentage indicator.
 */
@Composable
fun VerticalLevelGaugeHud(
    valuePercent: Int,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val clampedPercent = valuePercent.coerceIn(0, 100)
    val fraction = (clampedPercent / 100f).coerceIn(0f, 1f)

    Surface(
        color = ApturaColors.SurfaceElevated2.copy(alpha = 0.92f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .width(44.dp)
                .height(180.dp)
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier.size(20.dp)
            )

            // Vertical Track Gauge
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(96.dp)
                    .clip(CircleShape)
                    .background(ApturaColors.SurfaceElevated1),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .clip(CircleShape)
                        .background(ApturaColors.AccentPrimary)
                )
            }

            // Tabular Percentage Display
            Text(
                text = "$clampedPercent%",
                style = ApturaTypography.labelSmall.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.SemiBold
                ),
                color = ApturaColors.TextHighEmphasis
            )
        }
    }
}

/**
 * Curved edge ripple scrim overlay for double-tap seek accumulation (FR-CTRL-002, SCR-010).
 * Sweeps inward from the left or right edge, displaying accumulated seek seconds in tabular figures.
 */
@Composable
fun DoubleTapSeekRippleOverlay(
    seekDeltaSeconds: Int,
    side: SeekTapSide,
    modifier: Modifier = Modifier
) {
    val isForward = side == SeekTapSide.RIGHT
    val absSeconds = kotlin.math.abs(seekDeltaSeconds)
    val deltaText = if (isForward) "+${absSeconds}s" else "-${absSeconds}s"

    val shape = if (isForward) {
        RoundedCornerShape(topStart = 110.dp, bottomStart = 110.dp)
    } else {
        RoundedCornerShape(topEnd = 110.dp, bottomEnd = 110.dp)
    }

    val brush = if (isForward) {
        Brush.horizontalGradient(
            listOf(
                ApturaColors.SurfaceElevated2.copy(alpha = 0.90f),
                ApturaColors.SurfaceScrim.copy(alpha = 0.95f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                ApturaColors.SurfaceScrim.copy(alpha = 0.95f),
                ApturaColors.SurfaceElevated2.copy(alpha = 0.90f)
            )
        )
    }

    Surface(
        shape = shape,
        color = ApturaColors.SurfaceElevated2.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
        shadowElevation = 10.dp,
        modifier = modifier
            .width(136.dp)
            .height(210.dp)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Filled.Forward10 else Icons.Filled.Replay10,
                    contentDescription = null,
                    tint = ApturaColors.AccentPrimary,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = deltaText,
                    style = ApturaTypography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold
                    ),
                    color = ApturaColors.TextHighEmphasis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isForward) "Fast-Forward" else "Rewind",
                    style = ApturaTypography.labelSmall,
                    color = ApturaColors.TextMediumEmphasis
                )
            }
        }
    }
}

/**
 * Floating glassmorphic HUD for horizontal swipe scrub gestures (FR-CTRL-005, FR-CTRL-003, SCR-010).
 * Displays target timecode, total duration, delta badge, and a mini progress bar in tabular figures.
 */
@Composable
fun HorizontalSwipeSeekHud(
    targetMs: Long,
    durationMs: Long,
    deltaMs: Long,
    modifier: Modifier = Modifier
) {
    val targetTimecode = MediaFormatters.formatDuration(targetMs)
    val totalTimecode = MediaFormatters.formatDuration(durationMs)
    val absDeltaMs = kotlin.math.abs(deltaMs)
    val deltaSign = if (deltaMs >= 0) "+" else "-"
    val deltaFormatted = "$deltaSign${MediaFormatters.formatDuration(absDeltaMs)}"
    val fraction = if (durationMs > 0L) (targetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        color = ApturaColors.SurfaceElevated2.copy(alpha = 0.94f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ApturaColors.SurfaceElevated3),
        shadowElevation = 12.dp,
        modifier = modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Delta Badge
            Surface(
                color = ApturaColors.AccentPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "[ $deltaFormatted ]",
                    style = ApturaTypography.labelMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold
                    ),
                    color = ApturaColors.AccentPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Timecode Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = targetTimecode,
                    style = ApturaTypography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold
                    ),
                    color = ApturaColors.TextHighEmphasis
                )
                Text(
                    text = "/",
                    style = ApturaTypography.titleMedium,
                    color = ApturaColors.TextLowEmphasis
                )
                Text(
                    text = totalTimecode,
                    style = ApturaTypography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Normal
                    ),
                    color = ApturaColors.TextMediumEmphasis
                )
            }

            // Mini Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ApturaColors.SurfaceElevated1),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(ApturaColors.AccentPrimary)
                )
            }
        }
    }
}

/**
 * Lock mode touch guard hint overlay (FR-CTRL-006, SCR-010).
 * Displays a tactile reminder when a user taps the screen while locked.
 */
@Composable
fun LockModeGuardHint(
    modifier: Modifier = Modifier
) {
    Surface(
        color = ApturaColors.SurfaceElevated2.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.6f)),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Screen Locked",
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Screen Locked • Tap the lock icon to unlock",
                style = ApturaTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = ApturaColors.TextHighEmphasis
            )
        }
    }
}
