package org.aptura.player.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aptura.player.R
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

private val ExpandedSidebarWidth = 248.dp
private val CompactSidebarWidth = 88.dp

/**
 * The wide-screen library navigation. It is deliberately more than a row of icons: it carries the collection's
 * runtime and footprint, the folder index, the two maintenance actions, and the privacy promise. It collapses to
 * [LibraryCompactRail] so a viewer can hand the width back to the grid, and remembers that choice for the session.
 */
@Composable
internal fun LibrarySidebar(
    state: MediaLibraryUiState.Success,
    topPadding: Dp,
    bottomPadding: Dp,
    onFolderSelected: (String?) -> Unit,
    onAddFolderClick: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = state.libraryVideoCount.takeIf { it > 0 } ?: state.folderCounts.values.sum().takeIf { it > 0 } ?: state.totalCount
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(ExpandedSidebarWidth)
            .background(ApturaColors.SurfaceElevated1)
            .padding(top = topPadding, bottom = bottomPadding)
    ) {
        SidebarBrand(onCollapse = onCollapse)

        LibraryPulse(
            total = total,
            durationMs = state.libraryDurationMs,
            sizeBytes = state.librarySizeBytes
        )

        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item(key = "collections_label") {
                    CollectionsHeader(onAddFolderClick = onAddFolderClick)
                }
                item(key = "all_videos") {
                    SidebarCollectionRow(
                        icon = Icons.Default.VideoLibrary,
                        label = "All videos",
                        count = total,
                        selected = state.selectedFolder == null,
                        onClick = { onFolderSelected(null) }
                    )
                }
                items(state.folders, key = { "folder:$it" }) { folder ->
                    val selected = state.selectedFolder == folder
                    SidebarCollectionRow(
                        icon = if (selected) Icons.Default.FolderOpen else Icons.Default.Folder,
                        label = friendlyFolderName(folder),
                        count = state.folderCounts[folder],
                        selected = selected,
                        onClick = { onFolderSelected(folder) }
                    )
                }
                if (state.folders.isEmpty()) {
                    item(key = "folders_hint") {
                        Text(
                            text = "Folders appear here after a scan or when you add a storage folder.",
                            style = ApturaTypography.bodySmall,
                            color = ApturaColors.TextLowEmphasis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            // On an 800 dp-tall tablet the list genuinely scrolls; the fade says so without a scrollbar.
            if (listState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to ApturaColors.SurfaceElevated1
                            )
                        )
                )
            }
        }

        SidebarTools(
            isRefreshing = state.isRefreshing,
            onOpenFilePicker = onOpenFilePicker,
            onRefresh = onRefresh,
            onSettingsClick = onSettingsClick
        )
    }
}

/** Collapsed fallback: the same information architecture, compressed to an icon rail. */
@Composable
internal fun LibraryCompactRail(
    state: MediaLibraryUiState.Success,
    topPadding: Dp,
    bottomPadding: Dp,
    canExpand: Boolean,
    onExpand: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = state.libraryVideoCount.takeIf { it > 0 } ?: state.totalCount
    // 88 dp cannot hold a wordmark or navigational labels at 200% type: "APTURA" wraps to two lines and
    // "Refresh" touches both edges. Below 150% the labels are the friendlier affordance; above it the rail
    // becomes icon-only and the icons' content descriptions carry the names for TalkBack.
    val showLabels = LocalDensity.current.fontScale < 1.5f
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = ApturaColors.SurfaceElevated1,
        contentColor = ApturaColors.TextHighEmphasis,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = topPadding + if (canExpand) 0.dp else 14.dp, bottom = 10.dp)
                    .width(CompactSidebarWidth)
            ) {
                if (canExpand) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onExpand,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Expand sidebar",
                                tint = ApturaColors.TextMediumEmphasis
                            )
                        }
                    }
                }
                SidebarMark(size = 34.dp)
                if (showLabels) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "APTURA",
                        style = ApturaTypography.labelSmall.copy(letterSpacing = 1.4.sp),
                        color = ApturaColors.TextHighEmphasis,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(if (showLabels) 10.dp else 16.dp))
                Text(
                    text = total.toString(),
                    style = ApturaTypography.headlineSmall,
                    color = ApturaColors.TextHighEmphasis
                )
                Text(
                    text = if (total == 1) "FILM" else "FILMS",
                    style = ApturaTypography.labelSmall,
                    color = ApturaColors.TextLowEmphasis
                )
            }
        }
    ) {
        NavigationRailItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Videos") },
            label = if (showLabels) {
                { Text("Videos", style = ApturaTypography.labelSmall) }
            } else {
                null
            },
            colors = railItemColors(selected = true)
        )
        NavigationRailItem(
            selected = false,
            onClick = onOpenFilePicker,
            icon = { Icon(Icons.Filled.FolderOpen, contentDescription = "Open File") },
            label = if (showLabels) {
                { Text("Open", style = ApturaTypography.labelSmall) }
            } else {
                null
            },
            colors = railItemColors(selected = false)
        )
        NavigationRailItem(
            selected = false,
            onClick = onRefresh,
            icon = {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics { contentDescription = "Rescanning library" },
                        strokeWidth = 2.dp,
                        color = ApturaColors.AccentPrimary
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            label = if (showLabels) {
                { Text("Refresh", style = ApturaTypography.labelSmall) }
            } else {
                null
            },
            colors = railItemColors(selected = false)
        )
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = if (showLabels) {
                { Text("Settings", style = ApturaTypography.labelSmall) }
            } else {
                null
            },
            colors = railItemColors(selected = false)
        )
        Spacer(Modifier.size(bottomPadding))
    }
}

@Composable
private fun SidebarBrand(onCollapse: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarMark(size = 40.dp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "APTURA",
                    style = ApturaTypography.titleMedium.copy(letterSpacing = 2.4.sp),
                    color = ApturaColors.TextHighEmphasis,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = "OFFLINE CINEMA",
                    style = ApturaTypography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = ApturaColors.TextLowEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Collapse sidebar",
                    tint = ApturaColors.TextMediumEmphasis
                )
            }
        }
        HorizontalDivider(color = ApturaColors.BorderSubtle)
    }
}

@Composable
private fun SidebarMark(size: Dp) {
    // The real aperture mark, not a letter tile: the sidebar should read as Aptura even without its wordmark.
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF0D0E11)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.28f)
        )
    }
}

@Composable
private fun LibraryPulse(total: Int, durationMs: Long, sizeBytes: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ApturaColors.SurfaceRecessed)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = total.toString(),
                style = ApturaTypography.displayMedium,
                color = ApturaColors.TextHighEmphasis
            )
            Spacer(Modifier.size(7.dp))
            Text(
                text = if (total == 1) "film" else "films",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = "${MediaFormatters.formatRuntime(durationMs)} total  ·  ${MediaFormatters.formatFileSize(sizeBytes)}",
            style = ApturaTypography.bodySmall,
            color = ApturaColors.TextMediumEmphasis
        )
    }
}

@Composable
private fun CollectionsHeader(onAddFolderClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 2.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "COLLECTIONS",
            style = ApturaTypography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = ApturaColors.TextLowEmphasis
        )
        Spacer(modifier = Modifier.weight(1f))
        // Pinned to the section heading: on a short landscape window the list scrolls, so a trailing
        // "Add a folder" row would be the first thing to disappear.
        IconButton(
            onClick = onAddFolderClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add a folder",
                tint = ApturaColors.TextMediumEmphasis,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 6.dp)
    ) {
        Text(
            text = text,
            style = ApturaTypography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = ApturaColors.TextLowEmphasis
        )
    }
}

@Composable
private fun SidebarCollectionRow(
    icon: ImageVector,
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextMediumEmphasis
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ApturaColors.AccentSubtle else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = label,
            style = ApturaTypography.bodyMedium,
            color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = ApturaTypography.labelSmall,
                color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis
            )
        }
    }
}

@Composable
private fun SidebarTools(
    isRefreshing: Boolean,
    onOpenFilePicker: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        HorizontalDivider(color = ApturaColors.BorderSubtle)
        Spacer(Modifier.size(8.dp))
        SidebarSectionLabel("TOOLS")
        SidebarActionRow(
            icon = Icons.Filled.FolderOpen,
            label = "Open a file",
            onClick = onOpenFilePicker
        )
        SidebarActionRow(
            icon = Icons.Default.Refresh,
            label = if (isRefreshing) "Rescanning…" else "Rescan library",
            onClick = onRefresh,
            showProgress = isRefreshing
        )
        SidebarActionRow(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = onSettingsClick
        )
        Spacer(Modifier.size(10.dp))
        PrivacyLockup()
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun SidebarActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    showProgress: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = ApturaColors.AccentPrimary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ApturaColors.TextMediumEmphasis,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = label,
            style = ApturaTypography.bodyMedium,
            color = ApturaColors.TextMediumEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrivacyLockup() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ApturaColors.AccentPrimary.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = ApturaColors.AccentPrimary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(
                text = "100% offline",
                style = ApturaTypography.labelSmall,
                color = ApturaColors.TextMediumEmphasis
            )
            Text(
                text = "No account. No telemetry.",
                style = ApturaTypography.bodySmall,
                color = ApturaColors.TextLowEmphasis
            )
        }
    }
}

@Composable
private fun railItemColors(selected: Boolean) = NavigationRailItemDefaults.colors(
    selectedIconColor = ApturaColors.AccentPrimary,
    selectedTextColor = ApturaColors.AccentPrimary,
    indicatorColor = ApturaColors.AccentSubtle,
    unselectedIconColor = ApturaColors.TextMediumEmphasis,
    unselectedTextColor = ApturaColors.TextMediumEmphasis
)

/** Paths arrive as SAF tree identifiers; show the last segment, not a wall of percent-encoded path. */
internal fun friendlyFolderName(folder: String): String =
    if (folder.contains('/')) {
        folder.trimEnd('/').substringAfterLast('/').ifBlank { "Internal Storage" }
    } else {
        folder.ifBlank { "Internal Storage" }
    }
