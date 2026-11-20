package org.aptura.player.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.Chapter
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.library.MediaFormatters

/**
 * Bottom sheet displaying embedded chapters for the active video (FEAT-10, FR-CTRL-011).
 */
@Composable
internal fun ChaptersSheet(
    chapters: List<Chapter>,
    currentPositionProvider: () -> Long,
    onChapterSelected: (Chapter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPos = currentPositionProvider()
    val activeIndex = chapters.indexOfLast { it.startTimeMs <= currentPos }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (chapters.isNotEmpty() && activeIndex in chapters.indices) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ChaptersSheet")
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        PlayerSheetHeader(
            icon = Icons.AutoMirrored.Filled.List,
            title = "Chapters",
            subtitle = if (chapters.size == 1) "1 chapter" else "${chapters.size} chapters",
            onClose = onDismiss,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(chapters, key = { _, ch -> ch.id }) { index, chapter ->
                val nextChapter = chapters.getOrNull(index + 1)
                val chapterEndMs = chapter.endTimeMs ?: nextChapter?.startTimeMs
                val isActive = if (chapterEndMs != null) {
                    currentPos in chapter.startTimeMs until chapterEndMs
                } else {
                    currentPos >= chapter.startTimeMs && (index == chapters.lastIndex || currentPos < (nextChapter?.startTimeMs ?: Long.MAX_VALUE))
                }

                ChapterItemRow(
                    index = index + 1,
                    chapter = chapter,
                    isActive = isActive,
                    onClick = { onChapterSelected(chapter) }
                )
            }
        }
    }
}

@Composable
internal fun ChapterItemRow(
    index: Int,
    chapter: Chapter,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) ApturaColors.SurfaceElevated2 else ApturaColors.SurfaceElevated1,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ChapterItemRow_$index")
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Currently playing",
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "$index",
                        style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = ApturaColors.TextMediumEmphasis,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = ApturaTypography.bodyMedium,
                    color = if (isActive) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (chapter.endTimeMs != null && chapter.endTimeMs > chapter.startTimeMs) {
                    val duration = chapter.endTimeMs - chapter.startTimeMs
                    Text(
                        text = "Duration: ${MediaFormatters.formatDuration(duration)}",
                        style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ApturaColors.SurfaceBase,
                modifier = Modifier.padding(2.dp)
            ) {
                Text(
                    text = MediaFormatters.formatDuration(chapter.startTimeMs),
                    style = ApturaTypography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = if (isActive) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
