package org.aptura.player.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.aptura.player.theme.ApturaColors
import org.aptura.player.ui.components.ApturaTextButton
import org.aptura.player.ui.components.ApturaTone
import org.aptura.player.ui.components.NoticeBanner

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
    NoticeBanner(
        icon = Icons.Outlined.ReportProblem,
        title = "Aptura closed unexpectedly last time",
        message = "A crash report is saved on this device only. You can share it to help fix the problem, or ignore this.",
        tint = ApturaColors.StatusWarning,
        modifier = modifier
    ) {
        ApturaTextButton(text = "Not now", onClick = onDismiss)
        ApturaTextButton(text = "Share details", onClick = onShareDetails, tone = ApturaTone.Accent)
    }
}
