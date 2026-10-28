package org.aptura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.SectionTitle

const val CONTINUE_WATCHING_TITLE = "Continue watching"

/**
 * A horizontal "pick up where you left off" shelf shown above the library grid. It uses the grid's own thumbnail,
 * progress line and type scale, so the two read as one library rather than two different card systems.
 */
@Composable
fun ContinueWatchingRow(
    cards: List<VideoCardUiModel>,
    onResume: (VideoCardUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(title = CONTINUE_WATCHING_TITLE, modifier = Modifier.padding(bottom = 12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(cards, key = { it.uri }, contentType = { "continue_watching_card" }) { card ->
                ContinueWatchingCard(card = card, onClick = { onResume(card) })
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    card: VideoCardUiModel,
    onClick: () -> Unit
) {
    val remaining = MediaFormatters.formatRemaining(card.rawItem.playbackPositionMs, card.rawItem.durationMs)
    val thumbShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .width(248.dp)
            .clip(RoundedCornerShape(16.dp))
            // One TalkBack stop per card, announced as an action: "Resume Title, 12 min left".
            .clearAndSetSemantics { contentDescription = "Resume ${card.title}, $remaining" }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(thumbShape)
                .background(ApturaColors.SurfaceElevated2)
                .border(1.dp, ApturaColors.BorderSubtle, thumbShape)
        ) {
            VideoThumbnail(card = card, modifier = Modifier.fillMaxSize())

            // The play disc is the affordance; a separate "Resume" label on the same picture said it twice.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .background(ApturaColors.SurfaceScrim, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = ApturaColors.TextOnScrim,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (card.progressFraction > 0f) {
                WatchProgressLine(
                    fraction = card.progressFraction,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 4.dp)) {
            Text(
                text = card.title,
                style = ApturaTypography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = ApturaColors.TextHighEmphasis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = remaining,
                style = ApturaTypography.bodySmall,
                maxLines = 1,
                color = ApturaColors.AccentPrimary
            )
        }
    }
}
