package org.aptura.player.ui.player

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.MediaFormatters

/**
 * Studio Up-Next Countdown Overlay (SCR-010g, FR-CTRL-017, FR-LIB-015).
 * Displayed when an item in a playback queue reaches ENDED state.
 * Presents a cancellable countdown ring, upcoming media preview card,
 * Play Now, Replay current, and Cancel Auto-Advance actions.
 */
@Composable
fun UpNextOverlay(
    countdownSeconds: Int,
    nextTitle: String,
    nextDurationMs: Long?,
    onPlayNow: () -> Unit,
    onReplay: () -> Unit,
    onCancel: () -> Unit,
    totalSeconds: Int = 10,
    nextItem: org.aptura.player.domain.model.DomainMediaItem? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val safeTotal = totalSeconds.coerceAtLeast(1).toFloat()
    val progress by animateFloatAsState(
        targetValue = (countdownSeconds / safeTotal).coerceIn(0f, 1f),
        label = "up_next_countdown_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ApturaColors.SurfaceScrim,
                        Color(0xE60D0E11)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ApturaColors.SurfaceElevated2,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Countdown Ring with center countdown number
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = ApturaColors.AccentPrimary,
                        trackColor = ApturaColors.SurfaceElevated3,
                        strokeWidth = 5.dp
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$countdownSeconds",
                            color = ApturaColors.TextHighEmphasis,
                            style = ApturaTypography.headlineMedium.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        Text(
                            text = "SEC",
                            color = ApturaColors.TextLowEmphasis,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Badge
                Surface(
                    color = ApturaColors.AccentSubtle,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ApturaColors.AccentPrimary)
                ) {
                    Text(
                        text = "UP NEXT",
                        color = ApturaColors.AccentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Next Item Preview Card
                Surface(
                    color = ApturaColors.SurfaceElevated1,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(80.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp)),
                            color = ApturaColors.SurfaceElevated3
                        ) {
                            if (nextItem != null) {
                                // The next episode's own frame, so "what's next" is recognisable before it plays.
                                org.aptura.player.ui.library.VideoThumbnail(
                                    uri = nextItem.uri,
                                    dateModifiedSec = nextItem.dateModifiedSec,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VideoFile,
                                        contentDescription = null,
                                        tint = ApturaColors.AccentPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nextTitle,
                                color = ApturaColors.TextHighEmphasis,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (nextDurationMs != null && nextDurationMs > 0L) {
                                Text(
                                    text = MediaFormatters.formatDuration(nextDurationMs),
                                    color = ApturaColors.TextMediumEmphasis,
                                    style = ApturaTypography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary & Secondary Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Replay Current Button
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReplay()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ApturaColors.TextHighEmphasis
                        ),
                        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Replay", fontSize = 13.sp)
                    }

                    // Play Now Primary Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayNow()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApturaColors.AccentPrimary,
                            contentColor = ApturaColors.TextOnAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play now",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel Button
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancel()
                    }
                ) {
                    Text(
                        text = "Stop countdown",
                        color = ApturaColors.TextMediumEmphasis,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
