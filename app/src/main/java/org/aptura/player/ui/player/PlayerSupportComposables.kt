package org.aptura.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * Transport seek button icon that follows the user's seek-step setting (5 / 10 / 15 / 30 s). Material has dedicated
 * icons for 5, 10 and 30; other steps draw the generic arrow with the number on top.
 */
@Composable
fun SeekStepIcon(forward: Boolean, seconds: Int, iconSize: Dp) {
    val label = "${if (forward) "Forward" else "Rewind"} ${seconds}s"
    val dedicated = when (seconds) {
        5 -> if (forward) Icons.Filled.Forward5 else Icons.Filled.Replay5
        10 -> if (forward) Icons.Filled.Forward10 else Icons.Filled.Replay10
        30 -> if (forward) Icons.Filled.Forward30 else Icons.Filled.Replay30
        else -> null
    }
    if (dedicated != null) {
        Icon(
            imageVector = dedicated,
            contentDescription = label,
            tint = ApturaColors.TextHighEmphasis,
            modifier = Modifier.size(iconSize)
        )
    } else {
        Box(
            modifier = Modifier
                .size(iconSize)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Replay,
                contentDescription = null,
                tint = ApturaColors.TextHighEmphasis,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer { scaleX = if (forward) -1f else 1f }
            )
            Text(
                text = seconds.toString(),
                style = ApturaTypography.labelSmall,
                color = ApturaColors.TextHighEmphasis,
                fontSize = (iconSize.value * 0.34f).sp
            )
        }
    }
}

/** What the screen shows instead of the picture in audio-only mode. */
@Composable
fun AudioOnlyPanel(
    title: String,
    onShowVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(ApturaColors.SurfaceElevated2, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Headphones,
                contentDescription = null,
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = ApturaTypography.titleMedium,
            color = ApturaColors.TextHighEmphasis,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Audio only · video is off to save battery",
            style = ApturaTypography.bodyMedium,
            color = ApturaColors.TextMediumEmphasis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onShowVideo,
            colors = ButtonDefaults.buttonColors(
                containerColor = ApturaColors.AccentPrimary,
                contentColor = ApturaColors.TextOnAccent
            )
        ) {
            Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Show video")
        }
    }
}
