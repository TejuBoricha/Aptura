package org.aptura.player.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.offset
import org.aptura.player.ui.components.StableSystemBars
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import org.aptura.player.ui.components.ApturaMenuItem
import org.aptura.player.ui.components.ApturaTone
import org.aptura.player.ui.components.ApturaDialog
import org.aptura.player.ui.components.DialogOptionRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.VideoLibrary
import org.aptura.player.ui.components.ApturaButton
import org.aptura.player.ui.components.ApturaOutlinedButton
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import org.aptura.player.ui.components.ApturaMenuCaption
import org.aptura.player.ui.components.ApturaTextButton
import org.aptura.player.ui.components.NoticeBanner
import org.aptura.player.ui.components.SectionTitle
import org.aptura.player.ui.components.SkeletonBlock
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
import androidx.work.WorkInfo
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import java.io.File
import java.util.UUID
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.core.media.MediaTransferProgress
import org.aptura.player.data.transfer.MediaTransferFailure
import org.aptura.player.data.transfer.MediaTransferJob
import org.aptura.player.data.transfer.MediaTransferJournal
import org.aptura.player.data.transfer.MediaTransferScheduler
import org.aptura.player.data.transfer.MediaTransferWorker
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
        containerColor = ApturaColors.SurfaceBase,
        // The player hides the system bars and shows them again as it closes. With the default, visibility-aware
        // insets the library lays out without them for the first frames of the way back and then jumps down by the
        // height of the status bar; the sizes themselves never change, so use those.
        contentWindowInsets = StableSystemBars
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is MediaLibraryUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                    ) {
                        LibraryAppBar(onSettingsClick = onSettingsClick)
                        MediaLibraryLoadingView(modifier = Modifier.weight(1f))
                    }
                }

                is MediaLibraryUiState.Empty -> {
                    // Without videos there is no sidebar or search row, but Settings (theme, folders, the guide) must
                    // still be one tap away: an empty library used to be a dead end.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                    ) {
                        LibraryAppBar(onSettingsClick = onSettingsClick)
                        MediaLibraryEmptyView(
                            isFiltered = uiState.isFiltered,
                            query = uiState.query,
                            isPermissionDenied = uiState.isPermissionDenied,
                            onClearSearch = { onSearchQueryChanged("") },
                            onRefresh = onRefresh,
                            onOpenFilePicker = onOpenFilePicker,
                            onRequestPermission = onRequestPermission,
                            onAddFolderClick = onAddFolderClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
    var transferJob by remember { mutableStateOf<MediaTransferJob?>(null) }
    var transferWorkInfo by remember { mutableStateOf<WorkInfo?>(null) }
    var transferCancelling by remember { mutableStateOf(false) }
    var handledTransferId by remember { mutableStateOf<String?>(null) }
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

    fun forgetTransferredClipboard(uris: List<String>, fromClipboard: Boolean) {
        if (!fromClipboard || uris.isEmpty()) return
        clipboardUris = clipboardUris - uris.toSet()
        if (clipboardUris.isEmpty()) clipboardIsCut = false
    }

    fun clearTransferJournal(jobId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            MediaTransferJournal.clearIf(context, jobId)
        }
        if (transferJob?.id == jobId) {
            transferJob = null
            transferWorkInfo = null
            transferCancelling = false
        }
    }

    fun finishCancelledTransfer(job: MediaTransferJob) {
        val copied = job.copiedUris.size
        selectedUris = emptySet()
        if (copied > 0) onRefresh()
        transferResult = TransferResultMessage(
            title = "Transfer cancelled",
            message = buildString {
                append(
                    "$copied of ${job.totalFiles} ${if (job.totalFiles == 1) "video was" else "videos were"} " +
                        "copied before cancellation. "
                )
                if (copied < job.totalFiles) {
                    append("The unfinished file was removed. ")
                } else {
                    append("Every video was copied before cancellation. ")
                }
                if (job.isMove) {
                    append("No originals were deleted.")
                } else {
                    append("Your original files are unchanged.")
                }
                if (job.fromClipboard && copied < job.totalFiles) {
                    append(" The clipboard still holds every item.")
                }
            }
        )
        clearTransferJournal(job.id)
    }

    fun finishFailedTransfer(job: MediaTransferJob) {
        selectedUris = emptySet()
        if (job.copiedUris.isNotEmpty()) onRefresh()
        transferResult = when (job.failure) {
            MediaTransferFailure.NOT_ENOUGH_SPACE -> {
                val required = job.requiredBytes ?: 0L
                val available = job.availableBytes ?: 0L
                val remaining = job.remainingUris.size
                TransferResultMessage(
                    title = "Not enough space",
                    message = buildString {
                        append("The selected folder has ${MediaFormatters.formatFileSize(available)} available. ")
                        if (job.copiedUris.isEmpty()) {
                            append(if (job.totalFiles == 1) "This video needs " else "These videos need ")
                            append("${MediaFormatters.formatFileSize(required)} in total.")
                            append("\n\nNothing was copied.")
                        } else {
                            append("${job.copiedUris.size} of ${job.totalFiles} videos were already copied. ")
                            append("The remaining $remaining ${if (remaining == 1) "video needs" else "videos need"} ")
                            append("${MediaFormatters.formatFileSize(required)} in total.")
                            append("\n\nNothing else was copied.")
                            if (job.isMove) append(" No originals were deleted.")
                        }
                        append(" Choose a different folder, or free up space and try again.")
                    }
                )
            }

            else -> TransferResultMessage(
                title = if (job.isMove) "Move not completed" else "Copy not completed",
                message = buildString {
                    append("The transfer could not be completed. ")
                    if (job.copiedUris.isNotEmpty()) {
                        append("${job.copiedUris.size} of ${job.totalFiles} videos were copied. ")
                    } else {
                        append("Nothing was copied. ")
                    }
                    if (job.isMove) {
                        append("No originals were deleted.")
                    } else {
                        append("Your original files are unchanged.")
                    }
                }
            )
        }
        clearTransferJournal(job.id)
    }

    fun finishTransfer(job: MediaTransferJob) {
        if (job.failure != null) {
            finishFailedTransfer(job)
            return
        }
        val titles = state.videos.associate { it.uri to it.title }
        if (job.isMove && job.copiedUris.isNotEmpty()) {
            // Copy-before-delete is unchanged. The journal stays until Android confirms the deletion, so a process
            // death at the consent dialog re-prompts on the next launch instead of silently leaving duplicates.
            deleteFlow.deleteAll(job.copiedUris) { deletion ->
                selectedUris = emptySet()
                forgetTransferredClipboard(deletion.deletedUris, job.fromClipboard)
                onRefresh()
                transferResult = moveResultMessage(
                    job = job,
                    deletedUris = deletion.deletedUris,
                    notDeletedUris = deletion.failedUris,
                    titlesByUri = titles
                )
                clearTransferJournal(job.id)
            }
            return
        }

        selectedUris = emptySet()
        if (job.copiedUris.isNotEmpty()) {
            forgetTransferredClipboard(job.copiedUris, job.fromClipboard)
            onRefresh()
        }
        transferResult = if (job.isMove) {
            TransferResultMessage(
                title = "Move not completed",
                message = "No originals were deleted. These videos could not be copied and remain in the library: " +
                    "${describeVideoUris(job.failedUris, titles)}."
            )
        } else {
            TransferResultMessage(
                title = when {
                    job.copiedUris.isEmpty() -> "Copy not completed"
                    job.failedUris.isEmpty() -> "Copy complete"
                    else -> "Copy partially complete"
                },
                message = buildString {
                    append(
                        "${job.copiedUris.size} " +
                            "${if (job.copiedUris.size == 1) "video copied" else "videos copied"} to the selected folder."
                    )
                    if (job.failedUris.isNotEmpty()) {
                        append("\nNot copied (originals kept): ${describeVideoUris(job.failedUris, titles)}.")
                    }
                }
            )
        }
        clearTransferJournal(job.id)
    }

    LaunchedEffect(Unit) {
        transferJob = withContext(Dispatchers.IO) { MediaTransferJournal.read(context) }
    }

    LaunchedEffect(transferJob?.id) {
        val jobId = transferJob?.id ?: return@LaunchedEffect
        MediaTransferScheduler.observe(context, jobId).collect { infos ->
            val info = MediaTransferScheduler.pickActive(infos)
            transferWorkInfo = info
            val latest = withContext(Dispatchers.IO) { MediaTransferJournal.read(context) }
            if (latest == null) {
                if (transferJob?.id == jobId) transferJob = null
                return@collect
            }
            if (latest.id != jobId) {
                transferJob = latest
                return@collect
            }
            transferJob = latest
            if (handledTransferId == latest.id) return@collect

            val active = MediaTransferScheduler.isActive(info)
            val terminal = infos.map { it.state }.filter(MediaTransferScheduler::isTerminal)
            when {
                latest.cancelled -> {
                    if (!active) {
                        handledTransferId = latest.id
                        finishCancelledTransfer(latest)
                    }
                }

                latest.copyFinished -> {
                    handledTransferId = latest.id
                    finishTransfer(latest)
                }

                latest.failure != null && !active -> {
                    handledTransferId = latest.id
                    finishFailedTransfer(latest.copy(copyFinished = true))
                }

                active -> Unit

                terminal.isEmpty() || terminal.all { it == WorkInfo.State.CANCELLED } -> {
                    // No live work and no recorded failure: WorkManager preempted the run (for example its
                    // ten-minute window) or pruned it. Continue from the journal instead of starting over.
                    MediaTransferScheduler.resume(context, jobId)
                }

                else -> {
                    // The worker reached a terminal state without journaling a result. Never claim a transfer the
                    // journal cannot name.
                    handledTransferId = latest.id
                    finishFailedTransfer(
                        latest.copy(failure = latest.failure ?: MediaTransferFailure.UNKNOWN, copyFinished = true)
                    )
                }
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { destination ->
        val sources = transferUris.distinct()
        val shouldMove = transferIsMove
        val fromClipboard = transferFromClipboard
        transferUris = emptyList()
        transferIsMove = false
        transferFromClipboard = false

        if (destination == null) {
            Toast.makeText(context, "Folder selection cancelled. Source files were kept.", Toast.LENGTH_SHORT).show()
        } else if (sources.isNotEmpty()) {
            try {
                // The worker may outlive this screen and even this process. Keep both grants so it can still write
                // to the chosen provider after WorkManager restores the journal.
                context.contentResolver.takePersistableUriPermission(
                    destination,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not offer persistable grants. The current copy can still proceed; a provider
                // failure after process death is reported as a failed transfer rather than deleting originals.
            }
            val job = MediaTransferJob(
                id = UUID.randomUUID().toString(),
                sources = sources,
                destination = destination.toString(),
                isMove = shouldMove,
                fromClipboard = fromClipboard
            )
            transferCancelling = false
            coroutineScope.launch {
                when (MediaTransferScheduler.start(context, job)) {
                    MediaTransferScheduler.StartResult.Started -> transferJob = job
                    MediaTransferScheduler.StartResult.AlreadyRunning -> {
                        transferJob = withContext(Dispatchers.IO) { MediaTransferJournal.read(context) }
                        Toast.makeText(
                            context,
                            "Another transfer is already running. Wait for it to finish.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    MediaTransferScheduler.StartResult.Unfinished -> {
                        transferJob = withContext(Dispatchers.IO) { MediaTransferJournal.read(context) }
                        Toast.makeText(
                            context,
                            "Finish or cancel the previous transfer before starting another.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
                // One page gutter for the header, chips, banners and grid, so every edge lines up.
                val pagePadding = if (isWideScreen) 24.dp else 16.dp

                // Search & Sort Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = pagePadding, end = pagePadding - 4.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = {
                            Text(
                                text = if (state.selectedFolder != null) {
                                    "Search ${friendlyFolderName(state.selectedFolder)}"
                                } else {
                                    "Search videos"
                                },
                                style = ApturaTypography.bodyLarge,
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
                        textStyle = ApturaTypography.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ApturaColors.AccentPrimary,
                            unfocusedBorderColor = ApturaColors.BorderSubtle,
                            focusedContainerColor = ApturaColors.SurfaceElevated1,
                            unfocusedContainerColor = ApturaColors.SurfaceElevated1,
                            focusedTextColor = ApturaColors.TextHighEmphasis,
                            unfocusedTextColor = ApturaColors.TextHighEmphasis,
                            cursorColor = ApturaColors.AccentPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 720.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Plain icon buttons: three filled tiles beside the field made the header heavier than the content.
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort library",
                                tint = ApturaColors.TextMediumEmphasis
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            containerColor = ApturaColors.SurfaceElevated2,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            ApturaMenuCaption("Sort by")
                            LibrarySortOrder.entries.forEach { order ->
                                ApturaMenuItem(
                                    text = order.displayName,
                                    icon = null,
                                    selected = state.sortOrder == order,
                                    onClick = {
                                        onSortOrderChanged(order)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (!isWideScreen) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
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

                        IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = ApturaColors.TextMediumEmphasis
                            )
                        }
                    }
                }

        // Folder Chips Row (FR-LIB-009, FR-LIB-018). The expanded sidebar owns this job on wide screens and keeps the
        // top edge quiet; the compact rail still relies on the chips.
        if (state.folders.isNotEmpty() && !isExpandedSidebar) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                contentPadding = PaddingValues(horizontal = pagePadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "all_videos", contentType = "folder_chip") {
                    FolderFilterChip(
                        label = "All videos",
                        selected = state.selectedFolder == null,
                        onClick = { onFolderSelected(null) }
                    )
                }

                items(state.folders, key = { it }, contentType = { "folder_chip" }) { folder ->
                    val friendlyName = remember(folder) { friendlyFolderName(folder) }
                    FolderFilterChip(
                        label = friendlyName,
                        selected = state.selectedFolder == folder,
                        onClick = { onFolderSelected(folder) }
                    )
                }

                item(key = "add_folder", contentType = "add_folder_chip") {
                    FilterChip(
                        selected = false,
                        onClick = onAddFolderClick,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = ApturaColors.AccentPrimary
                            )
                        },
                        label = { Text("Add folder", style = ApturaTypography.labelLarge.copy(fontFeatureSettings = "")) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = ApturaColors.AccentPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = ApturaColors.BorderSubtle
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(vertical = 6.dp)
                            .semantics { contentDescription = "Add custom media folder" }
                    )
                }
            }
        }

        // Post-crash prompt (spec/14 §2): quiet, once, never blocks the library.
        if (showCrashNotice) {
            CrashNoticeCard(
                onShareDetails = onShareCrashDetails,
                onDismiss = onDismissCrashNotice,
                modifier = Modifier.padding(horizontal = pagePadding, vertical = 6.dp)
            )
        }

        // Limited Storage Mode Banner (SCR-012)
        if (state.isLimitedMode) {
            NoticeBanner(
                icon = Icons.Outlined.Lock,
                title = "Limited access",
                message = "Aptura can only see videos you open or share with it. Allow access to list your whole library.",
                tint = ApturaColors.StatusWarning,
                modifier = Modifier.padding(horizontal = pagePadding, vertical = 6.dp)
            ) {
                ApturaTextButton(text = "Open a file", onClick = onOpenFilePicker)
                ApturaTextButton(text = "Allow access", onClick = onRequestPermission, tone = ApturaTone.Accent)
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
                        .padding(horizontal = pagePadding, vertical = 6.dp)
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .testTag("selection_action_bar"),
                    color = ApturaColors.SurfaceElevated2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ApturaColors.AccentPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                        if (selectedItems.isNotEmpty()) {
                            // One row: leave, count, then the actions. Selection is a mode, so it gets a visible exit.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedUris = emptySet() }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear selection",
                                        tint = ApturaColors.TextHighEmphasis
                                    )
                                }
                                Text(
                                    text = "${selectedItems.size} selected",
                                    style = ApturaTypography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    color = ApturaColors.TextHighEmphasis,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    activeModal = LibraryActiveModal.MoveToFolder(selectedItems.map { it.uri })
                                }) {
                                    Text("Move", style = ApturaTypography.labelLarge, color = ApturaColors.AccentPrimary)
                                }
                                TextButton(onClick = { shareVideos(context, selectedItems.map { it.uri }) }) {
                                    Text("Share", style = ApturaTypography.labelLarge, color = ApturaColors.AccentPrimary)
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
                                        containerColor = ApturaColors.SurfaceElevated2,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        val allSelected = selectedItems.size == state.videos.size
                                        ApturaMenuItem(
                                            text = if (allSelected) "Clear" else "Select all",
                                            icon = if (allSelected) Icons.Default.Close else Icons.Default.Check,
                                            onClick = {
                                                selectionActionsExpanded = false
                                                selectedUris = if (allSelected) {
                                                    emptySet()
                                                } else {
                                                    state.videos.mapTo(mutableSetOf()) { it.uri }
                                                }
                                            }
                                        )
                                        MenuDivider()
                                        if (selectedItems.size == 1) {
                                            ApturaMenuItem("Rename", Icons.Default.DriveFileRenameOutline, onClick = {
                                                selectionActionsExpanded = false
                                                activeModal = LibraryActiveModal.Rename(selectedItems.single())
                                            })
                                            ApturaMenuItem("Details", Icons.Outlined.Info, onClick = {
                                                selectionActionsExpanded = false
                                                val item = selectedItems.single()
                                                selectedUris = emptySet()
                                                if (onShowDetails != null) onShowDetails(item)
                                                else activeModal = LibraryActiveModal.Details(item)
                                            })
                                        }
                                        ApturaMenuItem("Copy", Icons.Outlined.ContentCopy, onClick = {
                                            selectionActionsExpanded = false
                                            clipboardUris = selectedItems.map { it.uri }
                                            clipboardIsCut = false
                                        })
                                        ApturaMenuItem("Cut", Icons.Outlined.ContentCut, onClick = {
                                            selectionActionsExpanded = false
                                            clipboardUris = selectedItems.map { it.uri }
                                            clipboardIsCut = true
                                        })
                                        ApturaMenuItem("Hide from library", Icons.Default.VisibilityOff, onClick = {
                                            selectionActionsExpanded = false
                                            activeModal = LibraryActiveModal.HideSelection(selectedItems)
                                        })
                                        MenuDivider()
                                        ApturaMenuItem("Delete videos", Icons.Default.DeleteOutline, tone = ApturaTone.Destructive, onClick = {
                                            selectionActionsExpanded = false
                                            activeModal = LibraryActiveModal.DeleteSelection(selectedItems)
                                        })
                                    }
                                }
                            }
                        }
                        if (clipboardUris.isNotEmpty()) {
                            if (selectedItems.isNotEmpty()) HorizontalDivider(color = ApturaColors.BorderSubtle)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Icon(
                                    imageVector = if (clipboardIsCut) Icons.Outlined.ContentCut else Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = ApturaColors.TextMediumEmphasis,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
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
                                    Text("Paste", style = ApturaTypography.labelLarge, color = ApturaColors.AccentPrimary)
                                }
                                TextButton(onClick = {
                                    clipboardUris = emptyList()
                                    clipboardIsCut = false
                                }) {
                                    Text("Clear", style = ApturaTypography.labelLarge, color = ApturaColors.TextMediumEmphasis)
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
            // 132 dp keeps two columns on a 320 dp phone (two 137 dp cells); 140 dp collapsed it to one huge column.
            else -> 132.dp
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
                start = pagePadding,
                end = pagePadding,
                top = 12.dp,
                bottom = bottomPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 20.dp else 14.dp)
        ) {
            if (state.continueWatching.isNotEmpty()) {
                item(
                    key = "continue_watching",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "continue_watching"
                ) {
                    ContinueWatchingRow(
                        cards = state.continueWatching,
                        onResume = { card -> onVideoClick(card.rawItem, allRawVideos) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
            // The grid gets a title of its own, so it reads as a section rather than as leftovers under the shelf.
            item(key = "grid_title", span = { GridItemSpan(maxLineSpan) }, contentType = "section_title") {
                SectionTitle(
                    title = when {
                        state.searchQuery.isNotBlank() -> "Results"
                        state.selectedFolder != null -> friendlyFolderName(state.selectedFolder)
                        else -> "Library"
                    },
                    count = state.videos.size
                )
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
                ApturaDialog(
                    onDismissRequest = { activeModal = null },
                    title = "More file actions",
                    message = MediaFormatters.cleanTitle(item.title),
                    confirmLabel = null,
                    onConfirm = {},
                    dismissLabel = "Close"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DialogOptionRow(
                            icon = Icons.Default.DriveFileRenameOutline,
                            title = "Rename",
                            subtitle = "Change the file name on your device",
                            onClick = { activeModal = LibraryActiveModal.Rename(item) }
                        )
                        DialogOptionRow(
                            icon = Icons.Outlined.ContentCopy,
                            title = "Copy",
                            subtitle = "Keep this file and paste a copy into another folder",
                            onClick = {
                                clipboardUris = listOf(item.uri)
                                clipboardIsCut = false
                                activeModal = null
                            }
                        )
                        DialogOptionRow(
                            icon = Icons.Outlined.ContentCut,
                            title = "Cut",
                            subtitle = "Move it to the folder you choose with Paste",
                            onClick = {
                                clipboardUris = listOf(item.uri)
                                clipboardIsCut = true
                                activeModal = null
                            }
                        )
                    }
                }
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
                ApturaDialog(
                    onDismissRequest = { activeModal = null },
                    icon = Icons.Default.VisibilityOff,
                    title = "Hide ${items.size} video${if (items.size == 1) "" else "s"}?",
                    message = "The files stay on your device. They will be hidden from Aptura, including after a rescan, and can be restored from Settings.",
                    confirmLabel = "Hide videos",
                    onConfirm = {
                        items.forEach { onRemoveVideo(it.uri) }
                        selectedUris = emptySet()
                        activeModal = null
                        Toast.makeText(
                            context,
                            "${items.size} ${if (items.size == 1) "video" else "videos"} hidden",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            is LibraryActiveModal.MoveToFolder -> {
                val uris = modal.uris
                ApturaDialog(
                    onDismissRequest = { activeModal = null },
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    title = "Move ${uris.size} ${if (uris.size == 1) "video" else "videos"}?",
                    message = "Aptura copies each video to the folder you choose, then asks Android to delete the original. " +
                        "If a copy fails or Android declines the deletion, that original stays where it is. " +
                        "Make sure the destination has enough free space.",
                    confirmLabel = "Choose folder",
                    onConfirm = {
                        activeModal = null
                        transferUris = uris
                        transferIsMove = true
                        transferFromClipboard = modal.fromClipboard
                        folderPicker.launch(null)
                    }
                )
            }

            null -> Unit
        }
        transferJob?.takeUnless { it.copyFinished }?.let { job ->
            val progress = transferWorkInfo?.progress?.let(MediaTransferWorker::progressFrom)
                ?: MediaTransferProgress(
                    completedFiles = job.copiedUris.size,
                    totalFiles = job.totalFiles,
                    copiedBytes = 0L,
                    totalBytes = 0L,
                    currentName = job.inProgressSource?.substringAfterLast('/').orEmpty()
                )
            TransferProgressDialog(
                progress = progress,
                isMove = job.isMove,
                isCancelling = transferCancelling,
                onCancel = {
                    transferCancelling = true
                    MediaTransferScheduler.cancel(context, job.id)
                }
            )
        }
        transferResult?.let { result ->
            ApturaDialog(
                onDismissRequest = { transferResult = null },
                title = result.title,
                message = result.message,
                confirmLabel = "OK",
                onConfirm = { transferResult = null },
                dismissLabel = null
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
    ApturaDialog(
        onDismissRequest = { if (!isCancelling) onCancel() },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = "$verb ${progress.totalFiles} $noun",
        confirmLabel = null,
        onConfirm = {},
        dismissLabel = if (isCancelling) "Cancelling…" else "Cancel",
        onDismiss = { if (!isCancelling) onCancel() }
    ) {
        Text(
            text = progress.currentName.ifBlank { "Preparing…" },
            style = ApturaTypography.bodyLarge,
            color = ApturaColors.TextHighEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ApturaColors.AccentPrimary,
            trackColor = ApturaColors.SurfaceElevated3,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildString {
                    append("${progress.completedFiles} of ${progress.totalFiles} files")
                    if (progress.totalBytes > 0L) {
                        append("  ·  ${MediaFormatters.formatFileSize(progress.copiedBytes)} of ")
                        append(MediaFormatters.formatFileSize(progress.totalBytes))
                    }
                },
                style = ApturaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = ApturaColors.TextMediumEmphasis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$percent%",
                style = ApturaTypography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = ApturaColors.TextHighEmphasis
            )
        }
    }
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
    var overflowExpanded by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(14.dp)
    // A flat card: the picture carries the shape and the text sits on the page below it. Without a container, a
    // one-line title next to a two-line title no longer leaves a blank band inside one of the cards.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(thumbShape)
                .background(ApturaColors.SurfaceElevated2)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle,
                    shape = thumbShape
                )
        ) {
            VideoThumbnail(card = card, modifier = Modifier.fillMaxSize())

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ApturaColors.AccentPrimary.copy(alpha = 0.18f))
                )
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
            } else if (card.isCompleted && !card.isMissing) {
                ThumbnailPill(
                    text = "Watched",
                    icon = Icons.Default.Check,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

            if (card.rawItem.durationMs > 0L) {
                ThumbnailPill(
                    text = card.durationFormatted,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            // Missing / Unmounted (FR-LIB-018, FR-LIB-021)
            if (card.isMissing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ApturaColors.SurfaceScrim.copy(alpha = 0.55f))
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(ApturaColors.StatusError.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = ApturaColors.TextOnScrim,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Missing", style = ApturaTypography.labelSmall, color = ApturaColors.TextOnScrim)
                }
            }

            // Watch progress: a flush 3 dp line along the bottom edge (no stop dot, P2-05).
            if (card.progressFraction > 0f && !card.isCompleted) {
                WatchProgressLine(
                    fraction = card.progressFraction,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    style = ApturaTypography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = ApturaColors.TextHighEmphasis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${card.fileSizeFormatted}  ·  ${card.folderName}",
                    style = ApturaTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ApturaColors.TextMediumEmphasis
                )
            }
            if (onMoreClick != null) {
                val hasContextActions = onPlayNextClick != null || onAddToQueueClick != null ||
                    onHideClick != null || onDeleteClick != null || onRemoveMissingClick != null ||
                    onMoreFileActionsClick != null || onShareClick != null || onMoveClick != null ||
                    onSelectClick != null
                // The touch target stays 48 dp; the offset lines the visible glyph up with the title's first line and
                // the card's right edge instead of floating in the middle of the text block.
                Box(modifier = Modifier.offset(x = 12.dp, y = (-12).dp)) {
                    IconButton(
                        onClick = {
                            if (hasContextActions) overflowExpanded = true else onMoreClick()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            // With a grid full of identical ellipses, "More actions" alone tells a
                            // screen-reader user nothing about which video will be changed.
                            contentDescription = "More actions for ${card.title}",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (hasContextActions) {
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                            containerColor = ApturaColors.SurfaceElevated2,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            fun pick(action: () -> Unit) {
                                overflowExpanded = false
                                action()
                            }
                            ApturaMenuItem("Video details", Icons.Outlined.Info, onClick = { pick(onMoreClick) })
                            if (card.isMissing) {
                                // A missing file cannot play, move, share or be deleted again. The only
                                // honest action is to remove its index row (and its saved progress).
                                if (onRemoveMissingClick != null) {
                                    MenuDivider()
                                    ApturaMenuItem(
                                        "Remove from library",
                                        Icons.Default.RemoveCircleOutline,
                                        onClick = { pick(onRemoveMissingClick) },
                                        tone = ApturaTone.Destructive
                                    )
                                }
                            } else {
                                onPlayNextClick?.let { ApturaMenuItem("Play next", Icons.AutoMirrored.Filled.PlaylistPlay, onClick = { pick(it) }) }
                                onAddToQueueClick?.let { ApturaMenuItem("Add to queue", Icons.AutoMirrored.Filled.PlaylistAdd, onClick = { pick(it) }) }
                                onSelectClick?.let { ApturaMenuItem("Select", Icons.Outlined.CheckCircle, onClick = { pick(it) }) }
                                if (onMoveClick != null || onShareClick != null || onMoreFileActionsClick != null) MenuDivider()
                                onMoveClick?.let { ApturaMenuItem("Move to folder", Icons.AutoMirrored.Filled.DriveFileMove, onClick = { pick(it) }) }
                                onShareClick?.let { ApturaMenuItem("Share", Icons.Default.Share, onClick = { pick(it) }) }
                                onMoreFileActionsClick?.let { ApturaMenuItem("More file actions", Icons.Default.DriveFileRenameOutline, onClick = { pick(it) }) }
                                if (onHideClick != null || onDeleteClick != null) MenuDivider()
                                onHideClick?.let { ApturaMenuItem("Hide from library", Icons.Default.VisibilityOff, onClick = { pick(it) }) }
                                onDeleteClick?.let {
                                    ApturaMenuItem("Delete video", Icons.Default.DeleteOutline, onClick = { pick(it) }, tone = ApturaTone.Destructive)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A folder filter. Selection is shown by fill and a check, never by colour alone. */
@Composable
private fun FolderFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
        } else null,
        label = {
            Text(
                text = label,
                style = ApturaTypography.labelLarge.copy(fontFeatureSettings = ""),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

@Composable
private fun MenuDivider() {
    HorizontalDivider(color = ApturaColors.BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
}

/** A small dark pill over a thumbnail (duration, "Watched"). Fixed light text: it always sits on a scrim. */
@Composable
internal fun ThumbnailPill(text: String, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = modifier
            .background(ApturaColors.SurfaceScrim, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ApturaColors.TextOnScrim, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text = text, style = ApturaTypography.labelSmall, color = ApturaColors.TextOnScrim)
    }
}

/** The flush watch-progress line used on every thumbnail, so the library and Continue watching read the same. */
@Composable
internal fun WatchProgressLine(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(ApturaColors.TextOnScrim.copy(alpha = 0.28f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(ApturaColors.AccentPrimary)
        )
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

/** The library's bar when there is no grid yet (loading or empty): the mark, the name, and a way to Settings. */
@Composable
private fun LibraryAppBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ApturaMark(size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Aptura",
            style = ApturaTypography.titleLarge,
            color = ApturaColors.TextHighEmphasis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = ApturaColors.TextMediumEmphasis
            )
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
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 440.dp)
                .padding(horizontal = 32.dp, vertical = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(ApturaColors.AccentSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isPermissionDenied -> Icons.Outlined.Lock
                        isFiltered -> Icons.Outlined.SearchOff
                        else -> Icons.Outlined.VideoLibrary
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = ApturaColors.AccentPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when {
                    isPermissionDenied -> "Allow access to your videos"
                    isFiltered -> "No results for “$query”"
                    else -> "No videos yet"
                },
                style = ApturaTypography.headlineMedium,
                color = ApturaColors.TextHighEmphasis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isPermissionDenied -> "Aptura needs permission to list the videos on this device. You can also add only the folders you choose."
                    isFiltered -> "Check the spelling, or try a shorter part of the title."
                    else -> "Aptura lists the videos on this device and its SD card. Scan again after adding files, or add a folder it can't see yet."
                },
                style = ApturaTypography.bodyLarge,
                color = ApturaColors.TextMediumEmphasis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            when {
                isPermissionDenied -> {
                    ApturaButton(
                        text = "Allow access",
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    ApturaOutlinedButton(
                        text = "Add a folder",
                        icon = Icons.Outlined.CreateNewFolder,
                        onClick = onAddFolderClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ApturaTextButton(text = "Open a single file", onClick = onOpenFilePicker)
                }

                isFiltered -> ApturaButton(text = "Clear search", onClick = onClearSearch)

                else -> {
                    ApturaButton(
                        text = "Scan device",
                        icon = Icons.Default.Refresh,
                        onClick = onRefresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    ApturaOutlinedButton(
                        text = "Add a folder",
                        icon = Icons.Outlined.CreateNewFolder,
                        onClick = onAddFolderClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * The first scan: the shape of the library, breathing, instead of a lone spinner. It tells the viewer what is about
 * to appear and where, so the grid does not "pop" into an empty screen.
 */
@Composable
private fun MediaLibraryLoadingView(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        val pagePadding = if (maxWidth >= 600.dp) 24.dp else 16.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pagePadding)
                .semantics(mergeDescendants = true) { contentDescription = "Looking for videos" }
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Looking for videos…",
                style = ApturaTypography.bodyMedium,
                color = ApturaColors.TextMediumEmphasis
            )
            Spacer(Modifier.height(14.dp))
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(columns) {
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
                            Spacer(Modifier.height(6.dp))
                            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }
}
