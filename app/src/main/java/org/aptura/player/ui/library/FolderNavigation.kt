package org.aptura.player.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import org.aptura.player.ui.components.ApturaModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.ApturaOutlinedButton

/*
 * Moving around the folders: the strip under the search field, the pill that says where a swipe landed, and the browser
 * that shows every folder at once. They exist so that, whatever the number of folders and however a folder was reached
 * (a tap, a swipe, a swipe five folders ago), the current one is on screen and named, and "Add folder" never has to be
 * hunted for at the end of a long row.
 */

/** Test tags, so a test can find the strip's parts without relying on their text. */
internal const val FOLDER_STRIP_TAG = "folder_strip"
internal const val ADD_FOLDER_TAG = "add_folder_button"
internal const val BROWSE_FOLDERS_TAG = "browse_folders_button"

/**
 * The row of folder chips: a "browse all folders" button, the chips, and an "Add folder" button.
 *
 * Both buttons sit outside the scrolling chips, so neither scrolls away. The chips scroll to keep the selected one
 * centred, so a folder selected by swiping is never off screen.
 */
@Composable
internal fun FolderStrip(
    folders: List<String>,
    labels: Map<String, String>,
    counts: Map<String, Int>,
    totalCount: Int,
    selectedFolder: String?,
    onFolderSelected: (String?) -> Unit,
    onBrowseFolders: () -> Unit,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp
) {
    val listState = rememberLazyListState()
    // Index 0 is "All videos"; the folders follow.
    val selectedIndex = if (selectedFolder == null) 0 else folders.indexOf(selectedFolder).takeIf { it >= 0 }?.plus(1)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null) listState.centerOn(selectedIndex)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = horizontalPadding - 8.dp, end = horizontalPadding - 8.dp)
            .testTag(FOLDER_STRIP_TAG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBrowseFolders, modifier = Modifier.size(48.dp).testTag(BROWSE_FOLDERS_TAG)) {
            Icon(
                imageVector = Icons.Outlined.FolderCopy,
                contentDescription = "Browse all folders",
                tint = ApturaColors.TextMediumEmphasis
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item(key = "all_videos", contentType = "folder_chip") {
                    FolderFilterChip(
                        label = "All videos",
                        count = totalCount,
                        selected = selectedFolder == null,
                        onClick = { onFolderSelected(null) }
                    )
                }
                items(folders, key = { it }, contentType = { "folder_chip" }) { folder ->
                    FolderFilterChip(
                        label = labels[folder] ?: friendlyFolderName(folder),
                        count = counts[folder],
                        selected = selectedFolder == folder,
                        onClick = { onFolderSelected(folder) }
                    )
                }
            }
            // The fades say "there is more this way" without a scrollbar; the pinned buttons stay clear of them.
            if (listState.canScrollBackward) EdgeFade(Alignment.CenterStart)
            if (listState.canScrollForward) EdgeFade(Alignment.CenterEnd)
        }

        IconButton(onClick = onAddFolder, modifier = Modifier.size(48.dp).testTag(ADD_FOLDER_TAG)) {
            Icon(
                imageVector = Icons.Outlined.CreateNewFolder,
                contentDescription = "Add custom media folder",
                tint = ApturaColors.AccentPrimary
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.EdgeFade(alignment: Alignment) {
    val start = alignment == Alignment.CenterStart
    Box(
        modifier = Modifier
            .align(alignment)
            .width(20.dp)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    if (start) listOf(ApturaColors.SurfaceBase, Color.Transparent) else listOf(Color.Transparent, ApturaColors.SurfaceBase)
                )
            )
    )
}

/** A folder filter. Selection is shown by fill, a check and weight, never by colour alone. */
@Composable
internal fun FolderFilterChip(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = ApturaColors.AccentPrimary
                )
            }
        } else {
            null
        },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = ApturaTypography.labelLarge.copy(
                        fontFeatureSettings = "",
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
                if (count != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = count.toString(),
                        style = ApturaTypography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ApturaColors.AccentSubtle,
            selectedLabelColor = ApturaColors.AccentPrimary,
            containerColor = ApturaColors.SurfaceElevated1,
            labelColor = ApturaColors.TextHighEmphasis
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = ApturaColors.BorderSubtle,
            selectedBorderColor = ApturaColors.AccentPrimary.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        // 36 dp visible, 48 dp to touch.
        modifier = Modifier
            .height(48.dp)
            .padding(vertical = 6.dp)
    )
}

/**
 * Scrolls so the item at [index] is in the middle of the viewport (or as near as the ends of the list allow).
 * An item that is far off screen is brought into the layout first, because only laid-out items have a position.
 */
internal suspend fun LazyListState.centerOn(index: Int) {
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
        scrollToItem(index)
        snapshotFlow { layoutInfo.visibleItemsInfo.any { it.index == index } }.first { it }
    }
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val itemCenter = item.offset + item.size / 2f
    animateScrollBy(itemCenter - viewportCenter)
}

// ---- where a swipe landed ---------------------------------------------------------------------------------------

/** What the pill says after a swipe: the folder reached and its place in the order. [token] restarts the pill's timer. */
@Immutable
internal data class FolderSwipeHint(val label: String, val position: Int, val total: Int, val token: Long)

/**
 * A short-lived pill naming the folder a swipe landed on ("Movies, 3 of 6"). The grid under a swipe changes without
 * the finger ever touching a chip, so this is what tells the viewer where they are. It is also a live region, so
 * TalkBack announces it.
 */
@Composable
internal fun FolderSwipeHintPill(hint: FolderSwipeHint?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = hint != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        // Keep the last text while fading out.
        val shown = remember(hint?.token) { hint } ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(ApturaColors.SurfaceScrim)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "${shown.label}, folder ${shown.position} of ${shown.total}"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = ApturaColors.TextOnScrim, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = shown.label,
                style = ApturaTypography.labelLarge.copy(fontFeatureSettings = ""),
                color = ApturaColors.TextOnScrim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${shown.position} of ${shown.total}",
                style = ApturaTypography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = ApturaColors.TextOnScrim.copy(alpha = 0.75f)
            )
        }
    }
}

// ---- the folder browser -----------------------------------------------------------------------------------------

/**
 * Every folder at once, as rows or as cover cards, with "Add folder" at the top. For a library with many folders this
 * is faster than scrolling the strip, and the covers make a folder recognisable before its name is read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderBrowserSheet(
    folders: List<String>,
    labels: Map<String, String>,
    summaries: Map<String, FolderSummary>,
    libraryVideoCount: Int,
    libraryDurationMs: Long,
    librarySizeBytes: Long,
    selectedFolder: String?,
    layout: FolderLayout,
    onLayoutChange: (FolderLayout) -> Unit,
    onSelect: (String?) -> Unit,
    onAddFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    ApturaModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ApturaColors.SurfaceElevated1
    ) {
        val listState = rememberLazyListState()
        val gridState = rememberLazyGridState()
        // The covers wait while the list is flung, like the pictures in the library (see rememberFlingGate).
        val flinging = rememberFlingGate(scrollKey = layout) {
            if (layout == FolderLayout.LIST) listState.firstVisibleItemIndex else gridState.firstVisibleItemIndex
        }
        CompositionLocalProvider(LocalThumbnailsPaused provides flinging) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.heightIn(max = maxHeight * 0.92f).padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Folders",
                        style = ApturaTypography.headlineSmall,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    IconButton(
                        onClick = {
                            onLayoutChange(if (layout == FolderLayout.LIST) FolderLayout.GRID else FolderLayout.LIST)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (layout == FolderLayout.LIST) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                            contentDescription = if (layout == FolderLayout.LIST) "Show folders as a grid" else "Show folders as a list",
                            tint = ApturaColors.TextMediumEmphasis
                        )
                    }
                }
                ApturaOutlinedButton(
                    text = "Add folder",
                    icon = Icons.Outlined.CreateNewFolder,
                    onClick = {
                        onDismiss()
                        onAddFolder()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                val allEntry = FolderEntry(
                    folder = null,
                    label = "All videos",
                    summary = FolderSummary(libraryVideoCount, librarySizeBytes, libraryDurationMs)
                )
                val entries = listOf(allEntry) + folders.map { folder ->
                    FolderEntry(folder, labels[folder] ?: friendlyFolderName(folder), summaries[folder])
                }
                val pick: (String?) -> Unit = { folder ->
                    onSelect(folder)
                    onDismiss()
                }
                when (layout) {
                    FolderLayout.LIST -> LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(entries, key = { it.folder ?: "<all>" }) { entry ->
                            FolderRow(entry, entry.folder == selectedFolder) { pick(entry.folder) }
                        }
                    }
                    FolderLayout.GRID -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        state = gridState,
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(entries, key = { it.folder ?: "<all>" }) { entry ->
                            FolderCard(entry, entry.folder == selectedFolder) { pick(entry.folder) }
                        }
                    }
                }
            }
        }
        }
    }
}

@Immutable
internal data class FolderEntry(val folder: String?, val label: String, val summary: FolderSummary?)

private fun FolderSummary.describe(): String = buildString {
    append(if (videoCount == 1) "1 video" else "$videoCount videos")
    if (totalBytes > 0L) append("  ·  ${MediaFormatters.formatFileSize(totalBytes)}")
    if (totalDurationMs > 0L) append("  ·  ${MediaFormatters.formatRuntime(totalDurationMs)}")
}

@Composable
private fun FolderCover(entry: FolderEntry, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(ApturaColors.SurfaceElevated2)
            .border(1.dp, ApturaColors.BorderSubtle, shape),
        contentAlignment = Alignment.Center
    ) {
        val cover = entry.summary?.coverUri
        if (entry.folder != null && cover != null) {
            VideoThumbnail(cover, entry.summary.coverDateModifiedSec, null, Modifier.fillMaxSize())
        } else {
            Icon(
                imageVector = if (entry.folder == null) Icons.Default.VideoLibrary else Icons.Default.Folder,
                contentDescription = null,
                tint = ApturaColors.TextLowEmphasis,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FolderRow(entry: FolderEntry, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) ApturaColors.AccentSubtle else Color.Transparent)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { this.selected = selected }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolderCover(entry, Modifier.width(96.dp), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = ApturaTypography.titleMedium,
                color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            entry.summary?.let {
                Text(
                    text = it.describe(),
                    style = ApturaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = ApturaColors.TextMediumEmphasis,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Current folder",
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier.padding(end = 8.dp).size(22.dp)
            )
        }
    }
}

@Composable
private fun FolderCard(entry: FolderEntry, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { this.selected = selected }
    ) {
        Box {
            FolderCover(entry, Modifier.fillMaxWidth(), shape)
            if (selected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(3.dp, ApturaColors.AccentPrimary, shape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(ApturaColors.AccentPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Current folder",
                        tint = ApturaColors.TextOnAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            text = entry.label,
            style = ApturaTypography.titleMedium,
            color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
        entry.summary?.let {
            Text(
                text = it.describe(),
                style = ApturaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = ApturaColors.TextMediumEmphasis,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
    }
}
