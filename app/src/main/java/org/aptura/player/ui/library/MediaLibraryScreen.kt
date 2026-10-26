package org.aptura.player.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
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
import org.aptura.player.core.media.MediaRenameResult
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import java.io.File
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.core.media.MediaFileTransfer
import org.aptura.player.core.media.MediaTransferProgress
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * Algebraic sum type representing the single active modal/dialog on the Media Library screen.
 * Enforces mutual exclusion per Aptura Coding Standards §6 (at most one active modal at a time).
 */
internal sealed interface LibraryActiveModal {
    data class Details(val item: DomainMediaItem) : LibraryActiveModal
    data class Rename(val item: DomainMediaItem) : LibraryActiveModal
    data class Hide(val item: DomainMediaItem) : LibraryActiveModal
    data class Delete(val item: DomainMediaItem) : LibraryActiveModal
    data class RemoveMissing(val item: DomainMediaItem) : LibraryActiveModal
    data class MoreFileActions(val item: DomainMediaItem) : LibraryActiveModal
    data class DeleteSelection(val items: List<DomainMediaItem>) : LibraryActiveModal
    data class HideSelection(val items: List<DomainMediaItem>) : LibraryActiveModal
    data class MoveToFolder(
        val uris: List<String>,
        val fromClipboard: Boolean = false
    ) : LibraryActiveModal
}

private data class TransferResultMessage(val title: String, val message: String)

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
    onRenameVideo: (String, String, String?, String?) -> Unit = { _, _, _, _ -> },
    onRemoveMissingVideo: (String) -> Unit = {},
    onShowDetails: ((DomainMediaItem) -> Unit)? = null,
    onAddFolderClick: () -> Unit = {},
    onPlayNext: (DomainMediaItem) -> Unit = {},
    onAddToQueue: (DomainMediaItem) -> Unit = {},
    onPlayAudioOnly: (DomainMediaItem, List<DomainMediaItem>) -> Unit = onVideoClick,
    onVideoDeleted: (String) -> Unit = onRemoveVideo,
    showCrashNotice: Boolean = false,
    onShareCrashDetails: () -> Unit = {},
    onDismissCrashNotice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ApturaColors.SurfaceBase
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is MediaLibraryUiState.Loading -> {
                    MediaLibraryLoadingView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
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
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
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
                        onRemoveMissingVideo = onRemoveMissingVideo,
                        onShowDetails = onShowDetails,
                        onAddFolderClick = onAddFolderClick,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onPlayAudioOnly = onPlayAudioOnly,
                        onVideoDeleted = onVideoDeleted,
                        showCrashNotice = showCrashNotice,
                        onShareCrashDetails = onShareCrashDetails,
                        onDismissCrashNotice = onDismissCrashNotice,
                        topPadding = innerPadding.calculateTopPadding(),
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
    onRenameVideo: (String, String, String?, String?) -> Unit = { _, _, _, _ -> },
    onRemoveMissingVideo: (String) -> Unit = {},
    onShowDetails: ((DomainMediaItem) -> Unit)? = null,
    onAddFolderClick: () -> Unit = {},
    onPlayNext: (DomainMediaItem) -> Unit = {},
    onAddToQueue: (DomainMediaItem) -> Unit = {},
    onPlayAudioOnly: (DomainMediaItem, List<DomainMediaItem>) -> Unit = onVideoClick,
    onVideoDeleted: (String) -> Unit = onRemoveVideo,
    showCrashNotice: Boolean = false,
    onShareCrashDetails: () -> Unit = {},
    onDismissCrashNotice: () -> Unit = {},
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    // The full index is the better default on a tablet; the rail is one tap away when the viewer would
    // rather spend the width on the grid. Saved so a rotation does not undo the choice.
    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectionActionsExpanded by remember { mutableStateOf(false) }
    var activeModal by remember { mutableStateOf<LibraryActiveModal?>(null) }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var clipboardUris by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var clipboardIsCut by rememberSaveable { mutableStateOf(false) }
    var transferUris by remember { mutableStateOf(emptyList<String>()) }
    var transferIsMove by remember { mutableStateOf(false) }
    var transferFromClipboard by remember { mutableStateOf(false) }
    var transferResult by remember { mutableStateOf<TransferResultMessage?>(null) }
    var transferProgress by remember { mutableStateOf<MediaTransferProgress?>(null) }
    var transferProgressIsMove by remember { mutableStateOf(false) }
    var transferCancelling by remember { mutableStateOf(false) }
    var transferJob by remember { mutableStateOf<Job?>(null) }
    var itemPendingRenameUri by remember { mutableStateOf<String?>(null) }
    var itemPendingRenameNewName by remember { mutableStateOf<String?>(null) }
    val deleteFlow = rememberMediaDeleteFlow(onDeleted = { uri ->
        ThumbnailRepository.deleteThumbnail(context, uri)
        onVideoDeleted(uri)
        clipboardUris = clipboardUris - uri
        if (clipboardUris.isEmpty()) clipboardIsCut = false
    })
    val trashFlow = rememberMediaTrashFlow { uris ->
        uris.forEach { uri ->
            onRemoveVideo(uri)
        }
        activeModal = null
        selectedUris = emptySet()
    }

    LaunchedEffect(state.videos) {
        val visibleUris = state.videos.mapTo(mutableSetOf()) { it.uri }
        selectedUris = selectedUris.filterTo(mutableSetOf(), visibleUris::contains)
    }

    // Selection is a mode, so Back leaves it before it navigates away from the library.
    BackHandler(enabled = selectedUris.isNotEmpty()) {
        selectedUris = emptySet()
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { destination ->
        val sources = transferUris.distinct()
        val sourceNames = state.videos.associate { it.uri to it.title }
        val shouldMove = transferIsMove
        val fromClipboard = transferFromClipboard
        transferUris = emptyList()
        transferIsMove = false
        transferFromClipboard = false

        fun forgetTransferredClipboard(uris: List<String>) {
            if (!fromClipboard || uris.isEmpty()) return
            clipboardUris = clipboardUris - uris.toSet()
            if (clipboardUris.isEmpty()) clipboardIsCut = false
        }

        if (destination == null) {
            Toast.makeText(context, "Folder selection cancelled. Source files were kept.", Toast.LENGTH_SHORT).show()
        } else if (sources.isNotEmpty()) {
            // The picker is modal and the progress dialog blocks the screen, so no transfer can already be running.
            transferProgressIsMove = shouldMove
            transferCancelling = false
            transferProgress = MediaTransferProgress(
                completedFiles = 0,
                totalFiles = sources.size,
                copiedBytes = 0L,
                totalBytes = 0L,
                currentName = ""
            )
            transferJob = coroutineScope.launch {
                var lastProgress = MediaTransferProgress(0, sources.size, 0L, 0L, "")
                try {
                    // Ask the destination before writing a byte. A provider that reports no quota is allowed to try.
                    val space = withContext(Dispatchers.IO) {
                        MediaFileTransfer.measureSpace(context, sources, destination)
                    }
                    if (!space.hasEnoughSpace) {
                        transferResult = TransferResultMessage(
                            title = "Not enough space",
                            message = buildString {
                                append("The selected folder has ${MediaFormatters.formatFileSize(space.availableBytes ?: 0L)} available. ")
                                append(
                                    if (sources.size == 1) {
                                        "This video needs ${MediaFormatters.formatFileSize(space.requiredBytes)}."
                                    } else {
                                        "These videos need ${MediaFormatters.formatFileSize(space.requiredBytes)} in total."
                                    }
                                )
                                append("\n\nNothing was copied. Choose a different folder, or free up space and try again.")
                            }
                        )
                        return@launch
                    }

                    transferProgress = MediaTransferProgress(
                        completedFiles = 0,
                        totalFiles = sources.size,
                        copiedBytes = 0L,
                        totalBytes = space.requiredBytes,
                        currentName = ""
                    )
                    val result = withContext(Dispatchers.IO) {
                        MediaFileTransfer.copyIntoTree(context, sources, destination) { progress ->
                            lastProgress = progress
                            transferProgress = progress
                        }
                    }
                    // The copy phase is over, so the progress dialog gives way to the consent dialog for a move.
                    transferProgress = null

                    if (shouldMove && result.copiedUris.isNotEmpty()) {
                        deleteFlow.deleteAll(result.copiedUris) { deletion ->
                            val retainedCount = deletion.failedCount + result.failedUris.size
                            selectedUris = emptySet()
                            forgetTransferredClipboard(deletion.deletedUris)
                            onRefresh()
                            transferResult = TransferResultMessage(
                                title = when {
                                    deletion.deletedCount == 0 -> "Move not completed"
                                    retainedCount == 0 -> "Move complete"
                                    else -> "Move partially complete"
                                },
                                message = buildString {
                                    append("${deletion.deletedCount} of ${sources.size} videos moved to the selected folder.")
                                    if (deletion.failedCount > 0) {
                                        append("\nOriginals still in the library: ${describeVideoUris(deletion.failedUris, sourceNames)}.")
                                    }
                                    if (result.failedUris.isNotEmpty()) {
                                        append("\nNot copied (originals kept): ${describeVideoUris(result.failedUris, sourceNames)}.")
                                    }
                                }
                            )
                        }
                    } else if (shouldMove) {
                        selectedUris = emptySet()
                        transferResult = TransferResultMessage(
                            title = "Move not completed",
                            message = "No originals were deleted. These videos could not be copied and remain in the library: ${describeVideoUris(result.failedUris, sourceNames)}."
                        )
                    } else {
                        selectedUris = emptySet()
                        if (result.copied > 0) {
                            forgetTransferredClipboard(result.copiedUris)
                            onRefresh()
                        }
                        transferResult = TransferResultMessage(
                            title = when {
                                result.copied == 0 -> "Copy not completed"
                                result.failedUris.isEmpty() -> "Copy complete"
                                else -> "Copy partially complete"
                            },
                            message = buildString {
                                append("${result.copied} ${if (result.copied == 1) "video copied" else "videos copied"} to the selected folder.")
                                if (result.failedUris.isNotEmpty()) {
                                    append("\nNot copied (originals kept): ${describeVideoUris(result.failedUris, sourceNames)}.")
                                }
                            }
                        )
                    }
                } catch (_: CancellationException) {
                    // Cancelling never deletes originals. Completed copies stay in the destination; the partial
                    // destination file was already removed by MediaFileTransfer.
                    val copiedCount = lastProgress.completedFiles
                    selectedUris = emptySet()
                    if (copiedCount > 0) onRefresh()
                    transferResult = TransferResultMessage(
                        title = "Transfer cancelled",
                        message = buildString {
                            append("$copiedCount of ${sources.size} ${if (sources.size == 1) "video was" else "videos were"} copied before cancellation. The unfinished file was removed.")
                            if (shouldMove) {
                                append(" No originals were deleted.")
                            } else {
                                append(" Your original files are unchanged.")
                            }
                            if (fromClipboard && copiedCount < sources.size) {
                                append(" The clipboard still holds every item.")
                            }
                        }
                    )
                } finally {
                    transferProgress = null
                    transferCancelling = false
                    transferJob = null
                }
            }
        }
    }

    val renameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = itemPendingRenameUri
            val title = itemPendingRenameNewName
            if (uri != null && title != null) {
                when (
                    val outcome = ApturaMediaFileManager.renameMediaFile(
                        context = context,
                        uri = Uri.parse(uri),
                        newNameWithExt = title
                    )
                ) {
                    is MediaRenameResult.Renamed -> {
                        onRenameVideo(uri, title, outcome.uri.toString(), outcome.displayName)
                        Toast.makeText(context, "Renamed to \"$title\"", Toast.LENGTH_SHORT).show()
                    }

                    is MediaRenameResult.PermissionRequired,
                    MediaRenameResult.Failed -> {
                        Toast.makeText(context, "Android did not allow the rename.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Rename cancelled. The file name was not changed.", Toast.LENGTH_SHORT).show()
        }
        itemPendingRenameUri = null
        itemPendingRenameNewName = null
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidth = maxWidth
        val isWideScreen = containerWidth >= 600.dp && windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
        // At 700 dp there is room for the full collection index. Above 150% font the labels cannot breathe in
        // 248 dp, so the rail compresses instead of truncating every folder name.
        val canExpandSidebar = isWideScreen && containerWidth >= 700.dp && LocalDensity.current.fontScale < 1.5f
        val isExpandedSidebar = canExpandSidebar && sidebarExpanded

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                AnimatedContent(
                    targetState = isExpandedSidebar,
                    transitionSpec = {
                        val expanding = targetState
                        val enter = fadeIn(tween(180)) +
                            slideInHorizontally(tween(220)) { width -> if (expanding) -width / 10 else width / 10 }
                        val exit = fadeOut(tween(140)) +
                            slideOutHorizontally(tween(220)) { width -> if (expanding) width / 10 else -width / 10 }
                        (enter togetherWith exit).using(
                            SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> tween(220) })
                        )
                    },
                    label = "librarySidebar"
                ) { expanded ->
                    if (expanded) {
                        LibrarySidebar(
                            state = state,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding,
                            onFolderSelected = onFolderSelected,
                            onAddFolderClick = onAddFolderClick,
                            onOpenFilePicker = onOpenFilePicker,
                            onRefresh = onRefresh,
                            onSettingsClick = onSettingsClick,
                            onCollapse = { sidebarExpanded = false }
                        )
                    } else {
                        LibraryCompactRail(
                            state = state,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding,
                            canExpand = canExpandSidebar,
                            onExpand = { sidebarExpanded = true },
                            onOpenFilePicker = onOpenFilePicker,
                            onRefresh = onRefresh,
                            onSettingsClick = onSettingsClick
                        )
                    }
                }
                // The sidebar and the collection are both near-white in the light theme; a hairline keeps the
                // boundary intentional instead of letting the panel dissolve into the grid.
                VerticalDivider(color = ApturaColors.BorderSubtle)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = topPadding)
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
                                color = ApturaColors.TextLowEmphasis,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

        // Folder Chips Row (FR-LIB-009, FR-LIB-018). The expanded sidebar owns this job on wide screens and keeps the
        // top edge quiet; the compact rail still relies on the chips.
        if (state.videos.isNotEmpty() && !isExpandedSidebar) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 680.dp)
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "all_videos", contentType = "all_videos_chip") {
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

                items(state.folders, key = { it }, contentType = { "folder_chip" }) { folder ->
                    val friendlyName = remember(folder) { friendlyFolderName(folder) }
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

                item(key = "add_folder", contentType = "add_folder_chip") {
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

        // Post-crash prompt (spec/14 §2): quiet, once, never blocks the library.
        if (showCrashNotice) {
            CrashNoticeCard(
                onShareDetails = onShareCrashDetails,
                onDismiss = onDismissCrashNotice,
                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
            )
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

        val selectedItems = remember(state.videos, selectedUris) {
            state.videos.filter { it.uri in selectedUris }.map { it.rawItem }
        }
        if (selectedItems.isNotEmpty() || clipboardUris.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selection_action_bar_container"),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .testTag("selection_action_bar"),
                    color = ApturaColors.SurfaceElevated1,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        if (selectedItems.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedItems.size} selected",
                                    style = ApturaTypography.titleSmall,
                                    color = ApturaColors.TextHighEmphasis
                                )
                                TextButton(onClick = {
                                    selectedUris = if (selectedItems.size == state.videos.size) {
                                        emptySet()
                                    } else {
                                        state.videos.mapTo(mutableSetOf()) { it.uri }
                                    }
                                }) {
                                    Text(
                                        if (selectedItems.size == state.videos.size) "Clear" else "Select all",
                                        color = ApturaColors.AccentPrimary
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    activeModal = LibraryActiveModal.MoveToFolder(selectedItems.map { it.uri })
                                }) {
                                    Text("Move", color = ApturaColors.AccentPrimary)
                                }
                                TextButton(onClick = { shareVideos(context, selectedItems.map { it.uri }) }) {
                                    Text("Share", color = ApturaColors.TextHighEmphasis)
                                }
                                Box {
                                    IconButton(
                                        onClick = { selectionActionsExpanded = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More actions for selected videos",
                                            tint = ApturaColors.TextMediumEmphasis
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = selectionActionsExpanded,
                                        onDismissRequest = { selectionActionsExpanded = false },
                                        containerColor = ApturaColors.SurfaceElevated2
                                    ) {
                                        if (selectedItems.size == 1) {
                                            DropdownMenuItem(
                                                text = { Text("Rename", color = ApturaColors.TextHighEmphasis) },
                                                onClick = {
                                                    selectionActionsExpanded = false
                                                    activeModal = LibraryActiveModal.Rename(selectedItems.single())
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Details", color = ApturaColors.TextHighEmphasis) },
                                                onClick = {
                                                    selectionActionsExpanded = false
                                                    val item = selectedItems.single()
                                                    selectedUris = emptySet()
                                                    if (onShowDetails != null) onShowDetails(item)
                                                    else activeModal = LibraryActiveModal.Details(item)
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Copy", color = ApturaColors.TextHighEmphasis) },
                                            onClick = {
                                                selectionActionsExpanded = false
                                                clipboardUris = selectedItems.map { it.uri }
                                                clipboardIsCut = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Cut", color = ApturaColors.TextHighEmphasis) },
                                            onClick = {
                                                selectionActionsExpanded = false
                                                clipboardUris = selectedItems.map { it.uri }
                                                clipboardIsCut = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Hide from library", color = ApturaColors.TextHighEmphasis) },
                                            onClick = {
                                                selectionActionsExpanded = false
                                                activeModal = LibraryActiveModal.HideSelection(selectedItems)
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Delete videos", color = ApturaColors.StatusError) },
                                            onClick = {
                                                selectionActionsExpanded = false
                                                activeModal = LibraryActiveModal.DeleteSelection(selectedItems)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (clipboardUris.isNotEmpty()) {
                            if (selectedItems.isNotEmpty()) HorizontalDivider(color = ApturaColors.BorderSubtle)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${clipboardUris.size} ${if (clipboardUris.size == 1) "video" else "videos"} ${if (clipboardIsCut) "ready to move" else "copied"}",
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    if (clipboardIsCut) {
                                        activeModal = LibraryActiveModal.MoveToFolder(
                                            uris = clipboardUris,
                                            fromClipboard = true
                                        )
                                    } else {
                                        transferUris = clipboardUris
                                        transferIsMove = false
                                        transferFromClipboard = true
                                        folderPicker.launch(null)
                                    }
                                }) {
                                    Text("Paste", color = ApturaColors.AccentPrimary)
                                }
                                TextButton(onClick = {
                                    clipboardUris = emptyList()
                                    clipboardIsCut = false
                                }) {
                                    Text("Clear", color = ApturaColors.TextMediumEmphasis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Adaptive Video Grid (Responds from 1 to 4+ columns automatically)
        val allRawVideos = remember(state.videos) { state.videos.map { it.rawItem } }

        val baseGridMinSize = when {
            containerWidth >= 1200.dp -> 240.dp
            isWideScreen -> 200.dp
            else -> 140.dp
        }
        // Fixed 140 dp cells at 200% font turn titles into "Aptura Audit Cl…" and make sibling episodes
        // indistinguishable. Growing the cell with the font scale buys the extra line it needs (and at the
        // extreme falls back to a single column) without changing the normal-density layout.
        val fontScale = LocalDensity.current.fontScale
        val gridMinSize = baseGridMinSize * fontScale.coerceIn(1f, 1.6f)

        val folderOrder = state.folders
        val layoutDirection = LocalLayoutDirection.current
        val density = LocalDensity.current
        val isSelecting = selectedUris.isNotEmpty()

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = gridMinSize),
            modifier = Modifier
                .weight(1f)
                .testTag("library_video_grid")
                .pointerInput(folderOrder, state.selectedFolder, isSelecting, layoutDirection) {
                    if (!isSelecting && folderOrder.isNotEmpty()) {
                        var horizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                horizontalDrag += dragAmount
                            },
                            onDragEnd = {
                                val swipeThreshold = with(density) { 64.dp.toPx() }
                                if (kotlin.math.abs(horizontalDrag) >= swipeThreshold) {
                                    val swipedForward = if (layoutDirection == LayoutDirection.Ltr) {
                                        horizontalDrag < 0f
                                    } else {
                                        horizontalDrag > 0f
                                    }
                                    val currentFolderIndex = state.selectedFolder
                                        ?.let(folderOrder::indexOf)
                                        ?.takeIf { it >= 0 }
                                        ?.plus(1)
                                        ?: 0
                                    val folderCount = folderOrder.size + 1
                                    val nextFolderIndex = (
                                        currentFolderIndex +
                                            (if (swipedForward) 1 else -1) +
                                            folderCount
                                        ) % folderCount
                                    onFolderSelected(folderOrder.getOrNull(nextFolderIndex - 1))
                                }
                            }
                        )
                    }
                },
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomPadding + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.continueWatching.isNotEmpty()) {
                item(
                    key = "continue_watching",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "continue_watching"
                ) {
                    ContinueWatchingRow(
                        cards = state.continueWatching,
                        onResume = { card -> onVideoClick(card.rawItem, allRawVideos) }
                    )
                }
            }
            items(
                items = state.videos,
                key = { item -> item.uri },
                contentType = { "video_card" }
            ) { card ->
                VideoGridCard(
                    card = card,
                    onClick = {
                        if (selectedUris.isNotEmpty()) {
                            selectedUris = if (card.uri in selectedUris) selectedUris - card.uri else selectedUris + card.uri
                        } else {
                            onVideoClick(card.rawItem, allRawVideos)
                        }
                    },
                    onLongClick = {
                        selectedUris = if (card.uri in selectedUris) selectedUris else selectedUris + card.uri
                    },
                    isSelected = card.uri in selectedUris,
                    onMoreClick = {
                        if (onShowDetails != null) {
                            onShowDetails(card.rawItem)
                        } else {
                            activeModal = LibraryActiveModal.Details(card.rawItem)
                        }
                    },
                    onPlayNextClick = { onPlayNext(card.rawItem) },
                    onAddToQueueClick = { onAddToQueue(card.rawItem) },
                    onHideClick = { activeModal = LibraryActiveModal.Hide(card.rawItem) },
                    onDeleteClick = { activeModal = LibraryActiveModal.Delete(card.rawItem) },
                    onRemoveMissingClick = {
                        activeModal = LibraryActiveModal.RemoveMissing(card.rawItem)
                    },
                    onMoreFileActionsClick = { activeModal = LibraryActiveModal.MoreFileActions(card.rawItem) },
                    onShareClick = { shareVideos(context, listOf(card.rawItem.uri)) },
                    onMoveClick = {
                        activeModal = LibraryActiveModal.MoveToFolder(listOf(card.rawItem.uri))
                    },
                    onSelectClick = { selectedUris = selectedUris + card.uri }
                )
            }
        }

        // Single mutually-exclusive modal surface (Coding Standards §6, F-21)
        when (val modal = activeModal) {
            is LibraryActiveModal.Details -> {
                val item = modal.item
                MediaDetailsSheet(
                    item = item,
                    onDismiss = { activeModal = null },
                    onPlayClick = {
                        activeModal = null
                        onVideoClick(item, allRawVideos)
                    },
                    onPlayAudioOnlyClick = {
                        activeModal = null
                        onPlayAudioOnly(item, allRawVideos)
                    },
                    onPlayNextClick = {
                        activeModal = null
                        onPlayNext(item)
                    },
                    onAddToQueueClick = {
                        activeModal = null
                        onAddToQueue(item)
                    },
                    onOpenWithClick = {
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(item.uri), item.mimeType ?: "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(viewIntent, "Open Video With..."))
                        } catch (_: Exception) {
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
                        activeModal = LibraryActiveModal.Details(item.copy(playbackPositionMs = 0L, isCompleted = false))
                    },
                    onRenameClick = {
                        activeModal = LibraryActiveModal.Rename(item)
                    },
                    onHideClick = {
                        activeModal = LibraryActiveModal.Hide(item)
                    },
                    onDeleteClick = {
                        activeModal = LibraryActiveModal.Delete(item)
                    },
                    onRemoveMissingClick = {
                        activeModal = LibraryActiveModal.RemoveMissing(item)
                    }
                )
            }

            is LibraryActiveModal.MoreFileActions -> {
                val item = modal.item
                AlertDialog(
                    onDismissRequest = { activeModal = null },
                    title = { Text("More file actions") },
                    text = {
                        Text(
                            "${MediaFormatters.cleanTitle(item.title)}\n\nRename changes the file name. Copy and Cut add it to Aptura's clipboard; choose Paste to select a destination.",
                            color = ApturaColors.TextMediumEmphasis
                        )
                    },
                    confirmButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(onClick = { activeModal = LibraryActiveModal.Rename(item) }) {
                                Text("Rename", color = ApturaColors.TextHighEmphasis)
                            }
                            TextButton(onClick = {
                                clipboardUris = listOf(item.uri)
                                clipboardIsCut = false
                                activeModal = null
                            }) {
                                Text("Copy", color = ApturaColors.TextHighEmphasis)
                            }
                            TextButton(onClick = {
                                clipboardUris = listOf(item.uri)
                                clipboardIsCut = true
                                activeModal = null
                            }) {
                                Text("Cut", color = ApturaColors.TextHighEmphasis)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { activeModal = null }) {
                            Text("Close", color = ApturaColors.TextMediumEmphasis)
                        }
                    },
                    containerColor = ApturaColors.SurfaceElevated2
                )
            }

            is LibraryActiveModal.Rename -> {
                val item = modal.item
                RenameVideoDialog(
                    itemTitle = MediaFormatters.cleanTitle(item.title),
                    onConfirm = { newName ->
                        when (
                            val outcome = ApturaMediaFileManager.renameMediaFile(
                                context = context,
                                uri = Uri.parse(item.uri),
                                newNameWithExt = newName
                            )
                        ) {
                            is MediaRenameResult.Renamed -> {
                                onRenameVideo(item.uri, newName, outcome.uri.toString(), outcome.displayName)
                                Toast.makeText(context, "Renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                            }

                            is MediaRenameResult.PermissionRequired -> {
                                itemPendingRenameUri = item.uri
                                itemPendingRenameNewName = newName
                                renameLauncher.launch(
                                    IntentSenderRequest.Builder(outcome.intentSender).build()
                                )
                            }

                            MediaRenameResult.Failed -> {
                                Toast.makeText(context, "Could not rename this video file.", Toast.LENGTH_LONG).show()
                            }
                        }
                        activeModal = null
                    },
                    onDismiss = { activeModal = null }
                )
            }

            is LibraryActiveModal.Delete -> {
                val item = modal.item
                DeleteFromDeviceDialog(
                    itemTitle = MediaFormatters.cleanTitle(item.title),
                    onConfirm = {
                        deleteFlow.delete(item.uri)
                        activeModal = null
                    },
                    allowTrash = canMoveToSystemTrash(listOf(item.uri)),
                    onTrashClick = {
                        if (trashFlow.trash(listOf(item.uri))) activeModal = null
                    },
                    onDismiss = { activeModal = null }
                )
            }

            is LibraryActiveModal.Hide -> {
                val item = modal.item
                HideFromLibraryDialog(
                    itemTitle = MediaFormatters.cleanTitle(item.title),
                    onConfirm = {
                        onRemoveVideo(item.uri)
                        activeModal = null
                    },
                    onDismiss = { activeModal = null }
                )
            }

            is LibraryActiveModal.RemoveMissing -> {
                val item = modal.item
                RemoveMissingVideoDialog(
                    itemTitle = MediaFormatters.cleanTitle(item.title),
                    onConfirm = {
                        onRemoveMissingVideo(item.uri)
                        activeModal = null
                    },
                    onDismiss = { activeModal = null }
                )
            }

            is LibraryActiveModal.DeleteSelection -> {
                val items = modal.items
                DeleteFromDeviceDialog(
                    itemTitle = items.singleOrNull()?.title.orEmpty(),
                    itemCount = items.size,
                    onConfirm = {
                        activeModal = null
                        deleteFlow.deleteAll(items.map { it.uri }) { deletion ->
                            selectedUris = deletion.failedUris.toSet()
                        }
                    },
                    allowTrash = canMoveToSystemTrash(items.map { it.uri }),
                    onTrashClick = {
                        if (trashFlow.trash(items.map { it.uri })) activeModal = null
                    },
                    onDismiss = { activeModal = null }
                )
            }

            is LibraryActiveModal.HideSelection -> {
                val items = modal.items
                AlertDialog(
                    onDismissRequest = { activeModal = null },
                    title = { Text("Hide ${items.size} video${if (items.size == 1) "" else "s"}?") },
                    text = {
                        Text(
                            "The files stay on your device. They will be hidden from Aptura, including after a rescan, and can be restored from Settings.",
                            color = ApturaColors.TextMediumEmphasis
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            items.forEach { onRemoveVideo(it.uri) }
                            selectedUris = emptySet()
                            activeModal = null
                            Toast.makeText(context, "${items.size} video(s) hidden.", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Hide videos", color = ApturaColors.AccentPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { activeModal = null }) {
                            Text("Cancel", color = ApturaColors.TextMediumEmphasis)
                        }
                    },
                    containerColor = ApturaColors.SurfaceElevated2
                )
            }

            is LibraryActiveModal.MoveToFolder -> {
                val uris = modal.uris
                AlertDialog(
                    onDismissRequest = { activeModal = null },
                    title = { Text("Move ${uris.size} ${if (uris.size == 1) "video" else "videos"}?") },
                    text = {
                        Text(
                            "Aptura will copy each selected video to the folder you choose, then ask Android to delete its original. If copying fails or Android denies deletion, that original stays in your library. Make sure the destination has enough free space.",
                            color = ApturaColors.TextMediumEmphasis
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            activeModal = null
                            transferUris = uris
                            transferIsMove = true
                            transferFromClipboard = modal.fromClipboard
                            folderPicker.launch(null)
                        }) {
                            Text("Choose folder", color = ApturaColors.AccentPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { activeModal = null }) {
                            Text("Cancel", color = ApturaColors.TextMediumEmphasis)
                        }
                    },
                    containerColor = ApturaColors.SurfaceElevated2
                )
            }

            null -> Unit
        }
        transferProgress?.let { progress ->
            TransferProgressDialog(
                progress = progress,
                isMove = transferProgressIsMove,
                isCancelling = transferCancelling,
                onCancel = {
                    transferCancelling = true
                    transferJob?.cancel()
                }
            )
        }
        transferResult?.let { result ->
            AlertDialog(
                onDismissRequest = { transferResult = null },
                title = { Text(result.title) },
                text = { Text(result.message, color = ApturaColors.TextMediumEmphasis) },
                confirmButton = {
                    TextButton(onClick = { transferResult = null }) {
                        Text("OK", color = ApturaColors.AccentPrimary)
                    }
                },
                containerColor = ApturaColors.SurfaceElevated2
            )
        }
            }
        }
    }
}

/**
 * A transfer is a job, not a spinner: it names the file being written, shows real byte progress and can be
 * stopped without deleting the originals. The dialog cannot be dismissed by tapping outside; Cancel is the
 * deliberate way out.
 */
@Composable
private fun TransferProgressDialog(
    progress: MediaTransferProgress,
    isMove: Boolean,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    val verb = if (isMove) "Moving" else "Copying"
    val noun = if (progress.totalFiles == 1) "video" else "videos"
    val percent = (progress.fraction * 100f).roundToInt()
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(
                text = "$verb ${progress.totalFiles} $noun",
                style = ApturaTypography.headlineSmall,
                color = ApturaColors.TextHighEmphasis
            )
        },
        text = {
            Column {
                Text(
                    text = progress.currentName.ifBlank { "Preparing…" },
                    style = ApturaTypography.bodyMedium,
                    color = ApturaColors.TextHighEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ApturaColors.AccentPrimary,
                    trackColor = ApturaColors.SurfaceElevated3
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${progress.completedFiles} of ${progress.totalFiles} files",
                        style = ApturaTypography.bodySmall,
                        color = ApturaColors.TextMediumEmphasis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$percent%",
                        style = ApturaTypography.bodySmall,
                        color = ApturaColors.TextHighEmphasis,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (progress.totalBytes > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${MediaFormatters.formatFileSize(progress.copiedBytes)} of " +
                            MediaFormatters.formatFileSize(progress.totalBytes),
                        style = ApturaTypography.bodySmall,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel, enabled = !isCancelling) {
                Text(
                    text = if (isCancelling) "Cancelling…" else "Cancel",
                    color = if (isCancelling) ApturaColors.TextMediumEmphasis else ApturaColors.StatusError
                )
            }
        },
        containerColor = ApturaColors.SurfaceElevated2,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun VideoGridCard(
    card: VideoCardUiModel,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
    onPlayNextClick: (() -> Unit)? = null,
    onAddToQueueClick: (() -> Unit)? = null,
    onHideClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onRemoveMissingClick: (() -> Unit)? = null,
    onMoreFileActionsClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onMoveClick: (() -> Unit)? = null,
    onSelectClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var overflowExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onLongClickLabel = "Select video"
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            )
            .semantics { selected = isSelected }
            .then(
                if (isSelected) Modifier.border(2.dp, ApturaColors.AccentPrimary, RoundedCornerShape(12.dp))
                else Modifier
            ),
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
                val cachedThumbnail = remember(card.uri, card.rawItem.dateModifiedSec) {
                    ThumbnailRepository.getCachedThumbnailFile(context, card.uri, card.rawItem.dateModifiedSec)
                }
                val thumbnailFile by produceState<File?>(
                    initialValue = cachedThumbnail,
                    card.uri,
                    card.rawItem.dateModifiedSec
                ) {
                    if (value == null) {
                        value = ThumbnailRepository.getOrCreateThumbnail(
                            context,
                            card.uri,
                            card.rawItem.dateModifiedSec
                        )
                    }
                }

                val imageRequest = remember(thumbnailFile) {
                    ImageRequest.Builder(context)
                        .data(thumbnailFile)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(28.dp),
                        shape = CircleShape,
                        color = ApturaColors.AccentPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = ApturaColors.TextOnAccent,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }

                // Duration Badge (Bottom-Right with tabular numerals)
                if (card.rawItem.durationMs > 0L) {
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
                            color = ApturaColors.TextOnScrim
                        )
                    }
                }

                // Missing / Unmounted Badge (FR-LIB-018, FR-LIB-021)
                if (card.isMissing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ApturaColors.SurfaceScrim.copy(alpha = 0.55f))
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
                                tint = ApturaColors.TextOnScrim,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Missing",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextOnScrim
                            )
                        }
                    }
                }

                // Playback Progress Bar (Flush 3 dp indicator along bottom edge without stop dot - P2-05)
                if (card.progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                            .background(ApturaColors.SurfaceScrim)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(card.progressFraction.coerceIn(0f, 1f))
                                .background(ApturaColors.AccentPrimary)
                        )
                    }
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
                        minLines = 2, // Fixed 2-line title slot ensures consistent baseline alignment (P2-01)
                        overflow = TextOverflow.Ellipsis,
                        color = ApturaColors.TextHighEmphasis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onMoreClick != null) {
                        val hasContextActions = onPlayNextClick != null || onAddToQueueClick != null ||
                            onHideClick != null || onDeleteClick != null || onRemoveMissingClick != null ||
                            onMoreFileActionsClick != null || onShareClick != null || onMoveClick != null ||
                            onSelectClick != null
                        Box {
                            IconButton(
                                onClick = {
                                    if (hasContextActions) {
                                        overflowExpanded = true
                                    } else {
                                        onMoreClick()
                                    }
                                },
                                modifier = Modifier.size(48.dp) // the 48 dp touch-target minimum
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    // With a grid full of identical ellipses, "More actions" alone tells a
                                    // screen-reader user nothing about which video will be changed.
                                    contentDescription = "More actions for ${card.title}",
                                    tint = ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            if (hasContextActions) {
                                DropdownMenu(
                                    expanded = overflowExpanded,
                                    onDismissRequest = { overflowExpanded = false },
                                    containerColor = ApturaColors.SurfaceElevated2
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Video Details", style = ApturaTypography.bodyMedium, color = ApturaColors.TextHighEmphasis) },
                                        onClick = {
                                            overflowExpanded = false
                                            onMoreClick()
                                        }
                                    )
                                    if (card.isMissing) {
                                        // A missing file cannot play, move, share or be deleted again. The only
                                        // honest action is to remove its index row (and its saved progress).
                                        if (onRemoveMissingClick != null) {
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Remove from library", style = ApturaTypography.bodyMedium, color = ApturaColors.StatusError) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onRemoveMissingClick()
                                                }
                                            )
                                        }
                                    } else {
                                        if (onPlayNextClick != null) {
                                            DropdownMenuItem(
                                                text = { Text("Play Next", style = ApturaTypography.bodyMedium, color = ApturaColors.TextHighEmphasis) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onPlayNextClick()
                                                }
                                            )
                                        }
                                        if (onAddToQueueClick != null) {
                                            DropdownMenuItem(
                                                text = { Text("Add to Queue", style = ApturaTypography.bodyMedium, color = ApturaColors.TextHighEmphasis) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onAddToQueueClick()
                                                }
                                            )
                                        }
                                        if (onSelectClick != null) {
                                            DropdownMenuItem(
                                                text = { Text("Select", style = ApturaTypography.bodyMedium, color = ApturaColors.AccentPrimary) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onSelectClick()
                                                }
                                            )
                                        }
                                        if (onMoveClick != null) {
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Move to folder", style = ApturaTypography.bodyMedium, color = ApturaColors.AccentPrimary) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onMoveClick()
                                                }
                                            )
                                        }
                                        if (onShareClick != null) {
                                            DropdownMenuItem(
                                                text = { Text("Share", style = ApturaTypography.bodyMedium, color = ApturaColors.TextHighEmphasis) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onShareClick()
                                                }
                                            )
                                        }
                                        if (onMoreFileActionsClick != null) {
                                            DropdownMenuItem(
                                                text = { Text("More file actions", style = ApturaTypography.bodyMedium, color = ApturaColors.TextHighEmphasis) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onMoreFileActionsClick()
                                                }
                                            )
                                        }
                                        if (onHideClick != null || onDeleteClick != null) {
                                            HorizontalDivider()
                                        }
                                        if (onHideClick != null) {
                                            DropdownMenuItem(
                                                text = { Text("Hide from library", style = ApturaTypography.bodyMedium, color = ApturaColors.TextHighEmphasis) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onHideClick()
                                                }
                                            )
                                        }
                                        if (onDeleteClick != null) {
                                            DropdownMenuItem(
                                                text = { Text("Delete video", style = ApturaTypography.bodyMedium, color = ApturaColors.StatusError) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onDeleteClick()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
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
                        style = ApturaTypography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = ApturaColors.TextMediumEmphasis
                    )
                }
            }
        }
    }
}

private fun shareVideos(context: android.content.Context, uris: List<String>) {
    if (uris.isEmpty()) return
    val streams = ArrayList(uris.distinct().map(Uri::parse))
    val shareIntent = if (streams.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(streams.first()) ?: "video/*"
            putExtra(Intent.EXTRA_STREAM, streams.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams)
        }
    }.apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(
            context.contentResolver,
            "Selected videos",
            streams.first()
        ).apply {
            streams.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
        }
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share video${if (streams.size == 1) "" else "s"}"))
    } catch (_: Exception) {
        Toast.makeText(context, "No app can share these videos.", Toast.LENGTH_LONG).show()
    }
}

private fun describeVideoUris(uris: List<String>, titlesByUri: Map<String, String>): String {
    val names = uris.map { uri ->
        titlesByUri[uri] ?: Uri.parse(uri).lastPathSegment ?: "Unknown video"
    }
    val displayed = names.take(3).joinToString()
    return if (names.size > 3) "$displayed, and ${names.size - 3} more" else displayed
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
