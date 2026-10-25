package org.aptura.player.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.videoFrameMillis
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

const val CONTINUE_WATCHING_TITLE = "Continue watching"

/** A horizontal "pick up where you left off" shelf shown above the library grid. */
@Composable
fun ContinueWatchingRow(
    cards: List<VideoCardUiModel>,
    onResume: (VideoCardUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = CONTINUE_WATCHING_TITLE,
            style = ApturaTypography.titleMedium,
            color = ApturaColors.TextHighEmphasis,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics { heading() }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(cards, key = { it.uri }, contentType = { "continue_watching_card" }) { card ->
                ContinueWatchingCard(card = card, onClick = { onResume(card) })
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(card: VideoCardUiModel, onClick: () -> Unit) {
    val remaining = MediaFormatters.formatRemaining(card.rawItem.playbackPositionMs, card.rawItem.durationMs)
    Card(
        modifier = Modifier
            .width(220.dp)
            // One TalkBack stop per card, announced as an action: "Resume Title, 12 min left".
            .clearAndSetSemantics { contentDescription = "Resume ${card.title}, $remaining" }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
        colors = CardDefaults.cardColors(containerColor = ApturaColors.SurfaceElevated1)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(ApturaColors.SurfacePureBlack)
            ) {
                val context = LocalContext.current
                var thumbnailFile by remember(card.uri, card.rawItem.dateModifiedSec) {
                    mutableStateOf(ThumbnailRepository.getCachedThumbnailFile(context, card.uri, card.rawItem.dateModifiedSec))
                }

                LaunchedEffect(card.uri, card.rawItem.dateModifiedSec) {
                    if (thumbnailFile == null) {
                        val file = ThumbnailRepository.getOrCreateThumbnail(context, card.uri, card.rawItem.dateModifiedSec)
                        if (file != null) {
                            thumbnailFile = file
                        }
                    }
                }

                val request = remember(thumbnailFile) {
                    ImageRequest.Builder(context)
                        .data(thumbnailFile)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ApturaColors.SurfaceScrim.copy(alpha = 0.12f))
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ApturaColors.SurfaceElevated2.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Resume",
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { card.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = ApturaColors.AccentPrimary,
                            trackColor = ApturaColors.SurfaceScrim
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = card.title,
                    style = ApturaTypography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ApturaColors.TextHighEmphasis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = remaining,
                    style = ApturaTypography.bodyMedium,
                    maxLines = 1,
                    color = ApturaColors.TextMediumEmphasis
                )
            }
        }
    }
}
