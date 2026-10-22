package org.aptura.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import org.aptura.player.core.media.ApturaMediaFileManager
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
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
    onSettingsClick: () -> Unit = {},
    onOpenFilePicker: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onRemoveVideo: (String) -> Unit = {},
    onResetProgress: (String) -> Unit = {},
    onRenameVideo: (String, String) -> Unit = { _, _ -> },
    onShowDetails: ((DomainMediaItem) -> Unit)? = null,
    onAddFolderClick: () -> Unit = {},
    onPlayNext: (DomainMediaItem) -> Unit = {},
    onAddToQueue: (DomainMediaItem) -> Unit = {},
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
                        isPermissionDenied = uiState.isPermissionDenied,
                        onClearSearch = { onSearchQueryChanged("") },
                        onRefresh = onRefresh,
                        onOpenFilePicker = onOpenFilePicker,
                        onRequestPermission = onRequestPermission,
                        onAddFolderClick = onAddFolderClick,
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
                        onSettingsClick = onSettingsClick,
                        onOpenFilePicker = onOpenFilePicker,
                        onRequestPermission = onRequestPermission,
                        onRemoveVideo = onRemoveVideo,
                        onResetProgress = onResetProgress,
                        onRenameVideo = onRenameVideo,
                        onShowDetails = onShowDetails,
                        onAddFolderClick = onAddFolderClick,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
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
    onSettingsClick: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onRequestPermission: () -> Unit,
    onRemoveVideo: (String) -> Unit,
    onResetProgress: (String) -> Unit,
    onRenameVideo: (String, String) -> Unit = { _, _ -> },
    onShowDetails: ((DomainMediaItem) -> Unit)? = null,
    onAddFolderClick: () -> Unit = {},
    onPlayNext: (DomainMediaItem) -> Unit = {},
    onAddToQueue: (DomainMediaItem) -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedDetailItem by remember { mutableStateOf<DomainMediaItem?>(null) }
    var itemToRename by remember { mutableStateOf<DomainMediaItem?>(null) }
    var itemPendingRenameUri by remember { mutableStateOf<String?>(null) }
    var itemPendingRenameNewName by remember { mutableStateOf<String?>(null) }
    var itemToHide by remember { mutableStateOf<DomainMediaItem?>(null) }
    var itemToDelete by remember { mutableStateOf<DomainMediaItem?>(null) }
    var itemPendingSystemDeleteUri by remember { mutableStateOf<String?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            itemPendingSystemDeleteUri?.let { uri ->
                onRemoveVideo(uri)
                itemPendingSystemDeleteUri = null
            }
        }
    }

    val renameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            itemPendingRenameUri?.let { uri ->
                itemPendingRenameNewName?.let { newName ->
                    onRenameVideo(uri, newName)
                    Toast.makeText(context, "Renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                }
            }
        }
        itemPendingRenameUri = null
        itemPendingRenameNewName = null
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidth = maxWidth
        val isWideScreen = containerWidth >= 600.dp && windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    containerColor = ApturaColors.SurfaceElevated1,
                    contentColor = ApturaColors.TextHighEmphasis,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Aptura",
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Aptura",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.AccentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                ) {
                    NavigationRailItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Videos") },
                        label = { Text("Videos", style = ApturaTypography.labelSmall) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = ApturaColors.AccentPrimary,
                            selectedTextColor = ApturaColors.AccentPrimary,
                            indicatorColor = ApturaColors.AccentSubtle,
                            unselectedIconColor = ApturaColors.TextMediumEmphasis,
                            unselectedTextColor = ApturaColors.TextMediumEmphasis
                        )
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = onOpenFilePicker,
                        icon = { Icon(Icons.Filled.FolderOpen, contentDescription = "Open File") },
                        label = { Text("Open", style = ApturaTypography.labelSmall) },
                        colors = NavigationRailItemDefaults.colors(
                            unselectedIconColor = ApturaColors.TextMediumEmphasis,
                            unselectedTextColor = ApturaColors.TextMediumEmphasis
                        )
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = onRefresh,
                        icon = { Icon(Icons.Default.Refresh, contentDescription = "Refresh") },
                        label = { Text("Refresh", style = ApturaTypography.labelSmall) },
                        colors = NavigationRailItemDefaults.colors(
                            unselectedIconColor = ApturaColors.TextMediumEmphasis,
                            unselectedTextColor = ApturaColors.TextMediumEmphasis
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = false,
                        onClick = onSettingsClick,
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", style = ApturaTypography.labelSmall) },
                        colors = NavigationRailItemDefaults.colors(
                            unselectedIconColor = ApturaColors.TextMediumEmphasis,
                            unselectedTextColor = ApturaColors.TextMediumEmphasis
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Search & Sort Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 680.dp)
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

                    if (!isWideScreen) {
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

                        Spacer(modifier = Modifier.width(4.dp))

                        // Settings Button
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(48.dp)
                                .background(ApturaColors.SurfaceElevated1, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
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
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 680.dp)
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

                item {
                    FilterChip(
                        selected = false,
                        onClick = onAddFolderClick,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Add custom media folder",
                                modifier = Modifier.size(16.dp),
                                tint = ApturaColors.AccentPrimary
                            )
                        },
                        label = { Text("+ Folder", style = ApturaTypography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ApturaColors.SurfaceElevated1,
                            labelColor = ApturaColors.AccentPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = ApturaColors.AccentPrimary.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Limited Storage Mode Banner (SCR-012)
        if (state.isLimitedMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ApturaColors.SurfaceElevated2)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ApturaColors.AccentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Limited Storage Access",
                            style = ApturaTypography.titleSmall,
                            color = ApturaColors.TextHighEmphasis,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Only selected or shared files appear here",
                            style = ApturaTypography.bodyMedium,
                            color = ApturaColors.TextMediumEmphasis
                        )
                    }
                    TextButton(onClick = onOpenFilePicker) {
                        Text("Open File", style = ApturaTypography.labelMedium, color = ApturaColors.AccentPrimary)
                    }
                    TextButton(onClick = onRequestPermission) {
                        Text("Grant", style = ApturaTypography.labelMedium, color = ApturaColors.AccentPrimary)
                    }
                }
            }
        }

        // Adaptive Video Grid (Responds from 1 to 4+ columns automatically)
        val allRawVideos = remember(state.videos) { state.videos.map { it.rawItem } }

        val gridMinSize = when {
            containerWidth >= 1200.dp -> 240.dp
            isWideScreen -> 200.dp
            else -> 140.dp
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = gridMinSize),
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
                    onClick = { onVideoClick(card.rawItem, allRawVideos) },
                    onMoreClick = {
                        if (onShowDetails != null) {
                            onShowDetails(card.rawItem)
                        } else {
                            selectedDetailItem = card.rawItem
                        }
                    }
                )
            }
        }

        // Media Details Bottom Sheet (SCR-009)
        selectedDetailItem?.let { item ->
            MediaDetailsSheet(
                item = item,
                onDismiss = { selectedDetailItem = null },
                onPlayClick = {
                    selectedDetailItem = null
                    onVideoClick(item, allRawVideos)
                },
                onPlayAudioOnlyClick = {
                    selectedDetailItem = null
                    onVideoClick(item, allRawVideos)
                },
                onPlayNextClick = {
                    selectedDetailItem = null
                    onPlayNext(item)
                },
                onAddToQueueClick = {
                    selectedDetailItem = null
                    onAddToQueue(item)
                },
                onOpenWithClick = {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(item.uri), item.mimeType ?: "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(viewIntent, "Open Video With..."))
                    } catch (e: Exception) {
                        // ignore if no handler
                    }
                },
                onShareClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = item.mimeType ?: "video/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                },
                onResetProgressClick = {
                    onResetProgress(item.uri)
                    selectedDetailItem = item.copy(playbackPositionMs = 0L, isCompleted = false)
                },
                onRenameClick = {
                    itemToRename = item
                    selectedDetailItem = null
                },
                onHideClick = {
                    itemToHide = item
                    selectedDetailItem = null
                },
                onDeleteClick = {
                    itemToDelete = item
                    selectedDetailItem = null
                }
            )
        }

        // Rename Video Dialog (FR-LIB-013)
        itemToRename?.let { item ->
            RenameVideoDialog(
                itemTitle = item.title,
                onConfirm = { newName ->
                    val uri = Uri.parse(item.uri)
                    val hasExt = item.title.contains('.')
                    val ext = if (hasExt) item.title.substringAfterLast('.') else ""
                    val fullName = if (ext.isNotBlank() && !newName.endsWith(".$ext", ignoreCase = true)) {
                        "$newName.$ext"
                    } else newName

                    ApturaMediaFileManager.renameMediaFile(
                        context = context,
                        uri = uri,
                        newNameWithExt = fullName,
                        onNeedsWritePermission = { intentSender ->
                            itemPendingRenameUri = item.uri
                            itemPendingRenameNewName = newName
                            renameLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                    )
                    onRenameVideo(item.uri, newName)
                    Toast.makeText(context, "Renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                    itemToRename = null
                },
                onDismiss = { itemToRename = null }
            )
        }

        // Real Storage Deletion Confirmation
        itemToDelete?.let { item ->
            DeleteFromDeviceDialog(
                itemTitle = item.title,
                onConfirm = {
                    val uri = Uri.parse(item.uri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                            itemPendingSystemDeleteUri = item.uri
                            deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                        } catch (e: Exception) {
                            onRemoveVideo(item.uri)
                        }
                    } else {
                        try {
                            context.contentResolver.delete(uri, null, null)
                            onRemoveVideo(item.uri)
                        } catch (e: Exception) {
                            onRemoveVideo(item.uri)
                        }
                    }
                    itemToDelete = null
                },
                onDismiss = { itemToDelete = null }
            )
        }

        // Hide from Library Dialog
        itemToHide?.let { item ->
            HideFromLibraryDialog(
                itemTitle = item.title,
                onConfirm = {
                    onRemoveVideo(item.uri)
                    itemToHide = null
                },
                onDismiss = { itemToHide = null }
            )
        }
            }
        }
    }
}

@Composable
fun VideoGridCard(
    card: VideoCardUiModel,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
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

                // Missing / Unmounted Badge (FR-LIB-018, FR-LIB-021)
                if (card.isMissing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ApturaColors.SurfacePureBlack.copy(alpha = 0.55f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(ApturaColors.StatusError.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = ApturaColors.TextHighEmphasis,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Missing",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextHighEmphasis
                            )
                        }
                    }
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
                    .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.title,
                        style = ApturaTypography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onMoreClick != null) {
                        IconButton(
                            onClick = onMoreClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = ApturaColors.TextMediumEmphasis,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

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
    isPermissionDenied: Boolean = false,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFilePicker: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onAddFolderClick: () -> Unit = {},
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
                    imageVector = when {
                        isPermissionDenied -> Icons.Default.FolderOff
                        isFiltered -> Icons.Default.Search
                        else -> Icons.Default.VideoLibrary
                    },
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = ApturaColors.AccentPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when {
                    isPermissionDenied -> "Storage Permission Required"
                    isFiltered -> "No videos match '$query'"
                    else -> "No videos found"
                },
                style = ApturaTypography.headlineMedium,
                color = ApturaColors.TextHighEmphasis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isPermissionDenied -> "Aptura needs access to your device media to index and display videos. Grant permission, or add custom storage directories."
                    isFiltered -> "Try checking for spelling errors or searching for a different title"
                    else -> "Videos stored on your device storage, SD card, or custom folders can be scanned into your library."
                },
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isPermissionDenied) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApturaColors.AccentPrimary,
                            contentColor = ApturaColors.SurfacePureBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Access", style = ApturaTypography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = onAddFolderClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Folder", style = ApturaTypography.titleSmall, color = ApturaColors.TextHighEmphasis)
                    }
                }
            } else if (isFiltered) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                    androidx.compose.material3.OutlinedButton(
                        onClick = onAddFolderClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = ApturaColors.AccentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Folder", style = ApturaTypography.titleMedium, color = ApturaColors.TextHighEmphasis)
                    }
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
