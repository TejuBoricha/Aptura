@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package org.aptura.player.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * Factual explainer sheet launched from "Why?" on the compatibility mode chip (SCR-010h, FR-PLAY-003).
 * Explains which codec failed, which platform software path is used, battery impact, and provides
 * manual decoder override controls with required plain-language descriptions.
 */
@Composable
fun CompatibilityExplainerSheet(
    failedCodecName: String?,
    failureReason: String?,
    activeDecoderPath: String,
    currentStrategy: Int,
    onStrategySelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ApturaColors.SurfaceElevated2,
        modifier = modifier.testTag("compatibility_explainer_sheet")
    ) {
        CompatibilityExplainerContent(
            failedCodecName = failedCodecName,
            failureReason = failureReason,
            activeDecoderPath = activeDecoderPath,
            currentStrategy = currentStrategy,
            onStrategySelected = onStrategySelected,
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
fun CompatibilityExplainerContent(
    failedCodecName: String?,
    failureReason: String?,
    activeDecoderPath: String,
    currentStrategy: Int,
    onStrategySelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ApturaColors.StatusWarning.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = ApturaColors.StatusWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Compatibility Mode",
                        style = ApturaTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ApturaColors.TextHighEmphasis
                    )
                    Text(
                        text = "Why is this video using software decoding?",
                        style = ApturaTypography.bodySmall,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
            }

            // Factual Section 1: What Happened
            ExplainerFactCard(
                icon = Icons.Filled.Memory,
                title = "What happened",
                description = if (!failedCodecName.isNullOrBlank()) {
                    "The device hardware decoder ($failedCodecName) could not initialize or decode this stream profile."
                } else {
                    "Hardware acceleration could not decode this video format or profile."
                },
                detail = failureReason
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Factual Section 2: What is Being Used
            ExplainerFactCard(
                icon = Icons.Filled.Info,
                title = "What is being used instead",
                description = "Aptura automatically engaged the platform software decoder ($activeDecoderPath) so playback can continue without crashing.",
                detail = null
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Factual Section 3: Trade-off / Impact
            ExplainerFactCard(
                icon = Icons.Filled.BatteryAlert,
                title = "What it means for you",
                description = "Software decoding provides maximum format compatibility, but processes frames on the CPU. This may use more battery and can occasionally drop frames on 4K content.",
                detail = null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Manual Decoder Engine Strategy chooser (FR-PLAY-003, Q13)
            Text(
                text = "DECODER OVERRIDE",
                style = ApturaTypography.labelSmall,
                color = ApturaColors.AccentPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val strategies = listOf(
                Triple(0, "Auto", "Hardware first, then Android's platform software decoder"),
                Triple(1, "Hardware", "GPU/ISP decoders only; fails instead of falling back"),
                Triple(2, "Software", "Android's CPU decoders; slower, but rescues broken hardware paths")
            )

            strategies.forEach { (strategy, title, description) ->
                val isSelected = currentStrategy == strategy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ApturaColors.AccentPrimary.copy(alpha = 0.12f) else ApturaColors.SurfaceElevated3,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onStrategySelected(strategy) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = ApturaTypography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                style = ApturaTypography.bodySmall,
                                color = ApturaColors.TextMediumEmphasis
                            )
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dismiss Button
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApturaColors.SurfaceElevated3,
                    contentColor = ApturaColors.TextHighEmphasis
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Got It",
                    style = ApturaTypography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

@Composable
private fun ExplainerFactCard(
    icon: ImageVector,
    title: String,
    description: String,
    detail: String?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ApturaColors.SurfaceElevated3,
        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = ApturaTypography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ApturaColors.TextHighEmphasis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = ApturaTypography.bodySmall,
                    color = ApturaColors.TextMediumEmphasis
                )
                if (!detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = detail,
                        style = ApturaTypography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = ApturaColors.TextLowEmphasis
                    )
                }
            }
        }
    }
}
