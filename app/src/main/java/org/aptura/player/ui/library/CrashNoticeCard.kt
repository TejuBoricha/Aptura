package org.aptura.player.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * The quiet post-crash prompt (spec/14 §2). Shown once after a crash; "Share details" opens the user's own share sheet
 * with the redacted report — nothing is sent by the app — and "Not now" simply stops the prompt.
 */
@Composable
fun CrashNoticeCard(
    onShareDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ApturaColors.SurfaceElevated2)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = "Aptura closed unexpectedly last time",
                style = ApturaTypography.titleSmall,
                color = ApturaColors.TextHighEmphasis,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "A crash report is saved on this device only. You can share it to help fix the problem, or ignore this.",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Not now", style = ApturaTypography.labelMedium, color = ApturaColors.TextMediumEmphasis)
                }
                TextButton(onClick = onShareDetails) {
                    Text("Share details", style = ApturaTypography.labelMedium, color = ApturaColors.AccentPrimary)
                }
            }
        }
    }
}
