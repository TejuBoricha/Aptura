package org.aptura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * Media Library Grid Screen (SCR-001, FR-LIB-001..015).
 * Built with Jetpack Compose following anti-slop taste guidelines and adaptive breakpoints.
 */
@Composable
fun MediaLibraryScreen(
    uiState: MediaLibraryUiState,
    onVideoClick: (DomainMediaItem, List<DomainMediaItem>) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onSortOrderChanged: (LibrarySortOrder) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ApturaColors.SurfaceBase
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (uiState) {
                is MediaLibraryUiState.Loading -> {
                    MediaLibraryLoadingView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                is MediaLibraryUiState.Empty -> {
                    MediaLibraryEmptyView(
                        isFiltered = uiState.isFiltered,
                        query = uiState.query,
                        onClearSearch = { onSearchQueryChanged("") },
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

                is MediaLibraryUiState.Success -> {
                    MediaLibraryContentView(
                        state = uiState,
                        onVideoClick = onVideoClick,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onFolderSelected = onFolderSelected,
                        onSortOrderChanged = onSortOrderChanged,
                        onRefresh = onRefresh,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaLibraryContentView(
    state: MediaLibraryUiState.Success,
    onVideoClick: (DomainMediaItem, List<DomainMediaItem>) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onSortOrderChanged: (LibrarySortOrder) -> Unit,
    onRefresh: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Sort Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Search videos",
                        style = ApturaTypography.bodyMedium,
                        color = ApturaColors.TextLowEmphasis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ApturaColors.TextMediumEmphasis
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = ApturaColors.TextMediumEmphasis
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ApturaColors.AccentPrimary,
                    unfocusedBorderColor = ApturaColors.SurfaceElevated2,
                    focusedContainerColor = ApturaColors.SurfaceElevated1,
                    unfocusedContainerColor = ApturaColors.SurfaceElevated1,
                    focusedTextColor = ApturaColors.TextHighEmphasis,
                    unfocusedTextColor = ApturaColors.TextHighEmphasis,
                    cursorColor = ApturaColors.AccentPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Menu Button
            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ApturaColors.SurfaceElevated1, RoundedCornerShape(12.dp))
                ) {
                    @Suppress("DEPRECATION")
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort library",
                        tint = ApturaColors.AccentPrimary
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(ApturaColors.SurfaceElevated2)
                ) {
                    LibrarySortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.displayName,
                                    style = ApturaTypography.bodyMedium,
                                    color = if (state.sortOrder == order) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                                )
                            },
                            onClick = {
                                onSortOrderChanged(order)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Refresh Button
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .background(ApturaColors.SurfaceElevated1, RoundedCornerShape(12.dp))
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ApturaColors.AccentPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh library",
                        tint = ApturaColors.TextMediumEmphasis
                    )
                }
            }
        }

        // Folder Chips Row
        if (state.folders.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedFolder == null,
                        onClick = { onFolderSelected(null) },
                        leadingIcon = if (state.selectedFolder == null) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ApturaColors.AccentPrimary
                                )
                            }
                        } else null,
                        label = { Text("All Videos", style = ApturaTypography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ApturaColors.AccentSubtle,
                            selectedLabelColor = ApturaColors.AccentPrimary,
                            containerColor = ApturaColors.SurfaceElevated1,
                            labelColor = ApturaColors.TextMediumEmphasis
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.selectedFolder == null,
                            borderColor = ApturaColors.SurfaceElevated2,
                            selectedBorderColor = ApturaColors.AccentPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                items(state.folders, key = { it }) { folder ->
                    val friendlyName = remember(folder) {
                        if (folder.contains('/')) {
                            folder.trimEnd('/').substringAfterLast('/').ifBlank { "Internal Storage" }
                        } else {
                            folder
                        }
                    }
                    FilterChip(
                        selected = state.selectedFolder == folder,
                        onClick = { onFolderSelected(folder) },
                        leadingIcon = if (state.selectedFolder == folder) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ApturaColors.AccentPrimary
                                )
                            }
                        } else null,
                        label = { Text(friendlyName, style = ApturaTypography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ApturaColors.AccentSubtle,
                            selectedLabelColor = ApturaColors.AccentPrimary,
                            containerColor = ApturaColors.SurfaceElevated1,
                            labelColor = ApturaColors.TextMediumEmphasis
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.selectedFolder == folder,
                            borderColor = ApturaColors.SurfaceElevated2,
                            selectedBorderColor = ApturaColors.AccentPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Adaptive Video Grid (Responds from 1 to 4 columns automatically)
        val allRawVideos = remember(state.videos) { state.videos.map { it.rawItem } }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomPadding + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = state.videos,
                key = { item -> item.uri },
                contentType = { "video_card" }
            ) { card ->
                VideoGridCard(
                    card = card,
                    onClick = { onVideoClick(card.rawItem, allRawVideos) }
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(
    card: VideoCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ApturaColors.SurfaceElevated1
        )
    ) {
        Column {
            // Thumbnail Container (16:9)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(ApturaColors.SurfacePureBlack)
            ) {
                val context = LocalContext.current
                val imageRequest = remember(card.uri) {
                    ImageRequest.Builder(context)
                        .data(card.uri)
                        .videoFrameMillis(2000L) // 2-second offset avoids initial black title cards
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Duration Badge (Bottom-Right with tabular numerals)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(ApturaColors.SurfaceScrim, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = card.durationFormatted,
                        style = ApturaTypography.labelSmall,
                        color = ApturaColors.TextHighEmphasis
                    )
                }

                // Playback Progress Bar (Linear indicator along bottom edge)
                if (card.progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { card.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = ApturaColors.AccentPrimary,
                        trackColor = ApturaColors.SurfaceScrim
                    )
                }
            }

            // Metadata & Title Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = card.title,
                    style = ApturaTypography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = ApturaColors.TextHighEmphasis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${card.fileSizeFormatted} • ${card.folderName}",
                        style = ApturaTypography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaLibraryEmptyView(
    isFiltered: Boolean,
    query: String,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(ApturaColors.SurfaceElevated1, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFiltered) Icons.Default.Search else Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = ApturaColors.AccentPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isFiltered) "No videos match '$query'" else "No videos found",
                style = ApturaTypography.headlineMedium,
                color = ApturaColors.TextHighEmphasis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isFiltered) {
                    "Try checking for spelling errors or searching for a different title"
                } else {
                    "Videos stored on your device storage or SD card can be scanned into your library."
                },
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isFiltered) {
                Button(
                    onClick = onClearSearch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApturaColors.AccentPrimary,
                        contentColor = ApturaColors.TextOnAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Search", style = ApturaTypography.titleMedium)
                }
            } else {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApturaColors.AccentPrimary,
                        contentColor = ApturaColors.TextOnAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Device", style = ApturaTypography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun MediaLibraryLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = ApturaColors.AccentPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scanning local videos...",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
        }
    }
}
