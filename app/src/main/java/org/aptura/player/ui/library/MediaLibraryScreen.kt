package org.aptura.player.ui.library

import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.aptura.player.domain.model.LibraryLayout
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
import org.aptura.player.ui.components.SHORT_WINDOW_HEIGHT
import org.aptura.player.ui.components.SectionTitle
import org.aptura.player.ui.components.SkeletonBlock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
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
import org.aptura.player.core.media.MediaMover
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
import kotlinx.coroutines.delay
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
        val fromClipboard: Boolean = false,
        val isMove: Boolean = true
    ) : LibraryActiveModal
}

/** The collection index needs at least this much height: below it the rail is used, however wide the window is. */
private val SIDEBAR_MIN_HEIGHT = 600.dp

/**
 * Media Library Grid Screen (SCR-001, FR-LIB-001..015).
 * Built with Jetpack Compose following anti-slop taste guidelines and adaptive breakpoints. In a short window
 * ([SHORT_WINDOW_HEIGHT]) the rail drops its text labels, the shelf's cards shrink and the spacing tightens.
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
    viewPrefs: LibraryViewPrefs = LibraryViewPrefs(),
    onViewPrefsChanged: (LibraryViewPrefs) -> Unit = {},
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
                        Box(modifier = Modifier.weight(1f)) {
                            // Loading that is over in a moment (every normal start) shows nothing, not a flash of skeleton.
                            AfterDelay { MediaLibraryLoadingView() }
                        }
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
                        viewPrefs = viewPrefs,
                        onViewPrefsChanged = onViewPrefsChanged,
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
    viewPrefs: LibraryViewPrefs = LibraryViewPrefs(),
    onViewPrefsChanged: (LibraryViewPrefs) -> Unit = {},
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
    var viewSheetVisible by remember { mutableStateOf(false) }
    var folderBrowserVisible by remember { mutableStateOf(false) }
    var swipeHint by remember { mutableStateOf<FolderSwipeHint?>(null) }
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
    val mover = remember(context) { MediaMover(context) }
    val moveFlow = rememberMediaMoveFlow(onMoved = { uri ->
        ThumbnailRepository.deleteThumbnail(context, uri)
        clipboardUris = clipboardUris - uri
        if (clipboardUris.isEmpty()) clipboardIsCut = false
    })
    var pendingCrossVolumeMoveJob by remember { mutableStateOf<MediaTransferJob?>(null) }
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
            // For cross-volume moves (e.g. into SAF SD card), copy succeeded. Show an educational heads-up
            // dialog before invoking Android's delete confirmation so the user understands why Android asks to delete.
            pendingCrossVolumeMoveJob = job
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

    val launchTransfer: (sources: List<String>, destination: String, shouldMove: Boolean, fromClipboard: Boolean) -> Unit =
        { sources, destination, shouldMove, fromClipboard ->
            if (sources.isNotEmpty()) {
                if (destination.startsWith("content://")) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            destination.toUri(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                    }
                }
                val job = MediaTransferJob(
                    id = UUID.randomUUID().toString(),
                    sources = sources,
                    destination = destination,
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
        } else {
            launchTransfer(sources, destination.toString(), shouldMove, fromClipboard)
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
                        uri = uri.toUri(),
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
        // The index's fixed parts (brand, summary, tools, the offline promise) take about 450 dp before a single folder
        // row, so a phone held sideways (about 400 dp tall) has no room left for the folders: it gets the rail, with the
        // folder bar in the content, whatever its width.
        val tallEnoughForSidebar = maxHeight >= SIDEBAR_MIN_HEIGHT
        val canExpandSidebar = isWideScreen && containerWidth >= 700.dp && tallEnoughForSidebar && LocalDensity.current.fontScale < 1.5f
        val isExpandedSidebar = canExpandSidebar && sidebarExpanded
        val shortWindow = maxHeight < SHORT_WINDOW_HEIGHT

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
                            shortWindow = shortWindow,
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

                val labels = remember(state.folders) { folderLabels(state.folders) }

                LibraryHeader(
                    query = state.searchQuery,
                    scopeName = state.selectedFolder?.let { labels[it] ?: friendlyFolderName(it) },
                    isRefreshing = state.isRefreshing,
                    showRefreshAndSettings = !isWideScreen,
                    pagePadding = pagePadding,
                    onQueryChange = onSearchQueryChanged,
                    onSearchDone = { focusManager.clearFocus() },
                    onViewOptions = { viewSheetVisible = true },
                    onRefresh = onRefresh,
                    onSettingsClick = onSettingsClick
                )

                // The folder strip (FR-LIB-009, FR-LIB-018). The expanded sidebar owns this job on wide screens and
                // keeps the top edge quiet; the compact rail still relies on the strip.
                if (state.folders.isNotEmpty() && !isExpandedSidebar) {
                    FolderStrip(
                        folders = state.folders,
                        labels = labels,
                        counts = state.folderCounts,
                        totalCount = state.libraryVideoCount,
                        selectedFolder = state.selectedFolder,
                        onFolderSelected = onFolderSelected,
                        onBrowseFolders = { folderBrowserVisible = true },
                        onAddFolder = onAddFolderClick,
                        horizontalPadding = pagePadding
                    )
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
                            val clipboardSourceFolders = remember(clipboardUris, state.videos) {
                                clipboardUris.mapNotNull { uri -> state.videos.firstOrNull { it.uri == uri }?.rawItem?.folderPath }.toSet()
                            }
                            val activeViewFolder = state.selectedFolder
                            val canPasteDirectlyHere = activeViewFolder != null && (!clipboardIsCut || activeViewFolder !in clipboardSourceFolders)
                            val activeFolderLabel = activeViewFolder?.let { labels[it] ?: friendlyFolderName(it) }

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
                                    text = buildString {
                                        append("${clipboardUris.size} ${if (clipboardUris.size == 1) "video" else "videos"} ")
                                        append(if (clipboardIsCut) "ready to move" else "copied")
                                        if (canPasteDirectlyHere && activeFolderLabel != null) {
                                            append(" · $activeFolderLabel")
                                        }
                                    },
                                    style = ApturaTypography.bodyMedium,
                                    color = ApturaColors.TextMediumEmphasis,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (canPasteDirectlyHere) {
                                    val dest = activeViewFolder
                                    val destLabel = activeFolderLabel ?: friendlyFolderName(dest)
                                    TextButton(onClick = {
                                        if (clipboardIsCut && mover.canMoveAllInPlace(clipboardUris, dest)) {
                                            moveFlow.moveAll(clipboardUris, dest) { batch ->
                                                forgetTransferredClipboard(batch.movedUris, fromClipboard = true)
                                                onRefresh()
                                                if (batch.movedUris.isNotEmpty()) {
                                                    val count = batch.movedCount
                                                    val noun = if (count == 1) "video" else "videos"
                                                    transferResult = TransferResultMessage(
                                                        title = "Move complete",
                                                        message = "$count of ${clipboardUris.size} $noun moved to $destLabel."
                                                    )
                                                }
                                            }
                                        } else {
                                            launchTransfer(clipboardUris, dest, clipboardIsCut, true)
                                        }
                                    }) {
                                        Text(
                                            text = if (clipboardIsCut) "Move here" else "Paste here",
                                            style = ApturaTypography.labelLarge,
                                            color = ApturaColors.AccentPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            activeModal = LibraryActiveModal.MoveToFolder(
                                                uris = clipboardUris,
                                                fromClipboard = true,
                                                isMove = clipboardIsCut
                                            )
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                            contentDescription = "Choose another folder",
                                            tint = ApturaColors.TextMediumEmphasis,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    TextButton(onClick = {
                                        activeModal = LibraryActiveModal.MoveToFolder(
                                            uris = clipboardUris,
                                            fromClipboard = true,
                                            isMove = clipboardIsCut
                                        )
                                    }) {
                                        Text(
                                            text = if (clipboardIsCut) "Move" else "Paste",
                                            style = ApturaTypography.labelLarge,
                                            color = ApturaColors.AccentPrimary
                                        )
                                    }
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

        val allRawVideos = remember(state.videos) { state.videos.map { it.rawItem } }
        val isSelecting = selectedUris.isNotEmpty()
        val haptic = LocalHapticFeedback.current

        // A swipe moves one folder along and says where it landed. At either end it stays put (no wrap-around) and
        // the pill still names the folder, so the viewer sees it is the end rather than wondering if anything happened.
        val swipeToFolder: (Int) -> Unit = { delta ->
            val order = folderOrder(state.folders)
            val target = neighbouringFolder(state.folders, state.selectedFolder, delta)
            // "All videos" is the folder null, so a null folder from `target` is a destination, not "no target".
            val landed = if (target != null) target.second else state.selectedFolder
            val position = (target?.first ?: order.indexOf(state.selectedFolder).coerceAtLeast(0)) + 1
            if (target != null) onFolderSelected(landed)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            swipeHint = FolderSwipeHint(
                label = landed?.let { labels[it] ?: friendlyFolderName(it) } ?: "All videos",
                position = position,
                total = order.size,
                token = (swipeHint?.token ?: 0L) + 1L
            )
        }
        LaunchedEffect(swipeHint?.token) {
            if (swipeHint != null) {
                kotlinx.coroutines.delay(1400)
                swipeHint = null
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LibraryVideoPaneHost(
                state = state,
                swipeEnabled = !isSelecting && state.folders.isNotEmpty(),
                onSwipe = swipeToFolder,
                modifier = Modifier.fillMaxSize()
            ) { paneState, paneModifier ->
                LibraryVideoPane(
                    state = paneState,
                    prefs = viewPrefs,
                    scopeTitle = when {
                        paneState.searchQuery.isNotBlank() -> "Results"
                        paneState.selectedFolder != null -> labels[paneState.selectedFolder] ?: friendlyFolderName(paneState.selectedFolder)
                        else -> "Library"
                    },
                    isWideScreen = isWideScreen,
                    containerWidth = containerWidth,
                    pagePadding = pagePadding,
                    bottomPadding = bottomPadding,
                    onResumeContinueWatching = { card -> onVideoClick(card.rawItem, allRawVideos) },
                    onToggleLayout = {
                        onViewPrefsChanged(
                            viewPrefs.copy(
                                layout = if (viewPrefs.layout == LibraryLayout.LIST) LibraryLayout.GRID else LibraryLayout.LIST
                            )
                        )
                    },
                    isSelected = { card -> card.uri in selectedUris },
                    callbacksFor = { card ->
                        VideoCardCallbacks(
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
                            onMore = {
                                if (onShowDetails != null) {
                                    onShowDetails(card.rawItem)
                                } else {
                                    activeModal = LibraryActiveModal.Details(card.rawItem)
                                }
                            },
                            onPlayNext = { onPlayNext(card.rawItem) },
                            onAddToQueue = { onAddToQueue(card.rawItem) },
                            onHide = { activeModal = LibraryActiveModal.Hide(card.rawItem) },
                            onDelete = { activeModal = LibraryActiveModal.Delete(card.rawItem) },
                            onRemoveMissing = { activeModal = LibraryActiveModal.RemoveMissing(card.rawItem) },
                            onMoreFileActions = { activeModal = LibraryActiveModal.MoreFileActions(card.rawItem) },
                            onShare = { shareVideos(context, listOf(card.rawItem.uri)) },
                            onMove = { activeModal = LibraryActiveModal.MoveToFolder(listOf(card.rawItem.uri)) },
                            onSelect = { selectedUris = selectedUris + card.uri }
                        )
                    },
                    modifier = paneModifier,
                    shortWindow = shortWindow
                )
            }
            // At the bottom, where the thumb is and nothing is being read: it must not cover the folder's title.
            FolderSwipeHintPill(
                swipeHint,
                Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding + 24.dp)
            )
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
                            setDataAndType(item.uri.toUri(), item.mimeType ?: "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(viewIntent, "Open Video With..."))
                        } catch (_: Exception) {
                            Toast.makeText(context, "No app available to open this video", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShareClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = item.mimeType ?: "video/*"
                            putExtra(Intent.EXTRA_STREAM, item.uri.toUri())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "No app available to share this video", Toast.LENGTH_SHORT).show()
                        }
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
                                uri = item.uri.toUri(),
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
                val isMove = modal.isMove
                val sourceFolders = remember(uris, state.videos) {
                    uris.mapNotNull { uri -> state.videos.firstOrNull { it.uri == uri }?.rawItem?.folderPath }.toSet()
                }
                FolderDestinationSheet(
                    title = if (isMove) {
                        "Move ${uris.size} ${if (uris.size == 1) "video" else "videos"} to..."
                    } else {
                        "Copy ${uris.size} ${if (uris.size == 1) "video" else "videos"} to..."
                    },
                    folders = state.folders,
                    labels = labels,
                    summaries = state.folderSummaries,
                    currentFolders = sourceFolders,
                    isMove = isMove,
                    onSelectDestination = { destinationFolder ->
                        activeModal = null
                        val destLabel = labels[destinationFolder] ?: friendlyFolderName(destinationFolder)
                        if (isMove && mover.canMoveAllInPlace(uris, destinationFolder)) {
                            moveFlow.moveAll(uris, destinationFolder) { batch ->
                                selectedUris = emptySet()
                                forgetTransferredClipboard(batch.movedUris, modal.fromClipboard)
                                onRefresh()
                                if (batch.movedUris.isNotEmpty()) {
                                    val count = batch.movedCount
                                    val noun = if (count == 1) "video" else "videos"
                                    transferResult = TransferResultMessage(
                                        title = "Move complete",
                                        message = "$count of ${uris.size} $noun moved to $destLabel."
                                    )
                                }
                            }
                        } else {
                            launchTransfer(uris, destinationFolder, isMove, modal.fromClipboard)
                        }
                    },
                    onBrowseStorage = {
                        activeModal = null
                        transferUris = uris
                        transferIsMove = isMove
                        transferFromClipboard = modal.fromClipboard
                        folderPicker.launch(null)
                    },
                    onDismiss = { activeModal = null }
                )
            }

            null -> Unit
        }
        if (viewSheetVisible) {
            LibraryViewSheet(
                prefs = viewPrefs,
                sortOrder = state.sortOrder,
                onPrefsChange = onViewPrefsChanged,
                onSortChange = onSortOrderChanged,
                onDismiss = { viewSheetVisible = false }
            )
        }
        if (folderBrowserVisible) {
            FolderBrowserSheet(
                folders = state.folders,
                labels = labels,
                summaries = state.folderSummaries,
                libraryVideoCount = state.libraryVideoCount,
                libraryDurationMs = state.libraryDurationMs,
                librarySizeBytes = state.librarySizeBytes,
                selectedFolder = state.selectedFolder,
                layout = viewPrefs.folderLayout,
                onLayoutChange = { onViewPrefsChanged(viewPrefs.copy(folderLayout = it)) },
                onSelect = onFolderSelected,
                onAddFolder = onAddFolderClick,
                onDismiss = { folderBrowserVisible = false }
            )
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
        pendingCrossVolumeMoveJob?.let { job ->
            val count = job.copiedUris.size
            val noun = if (count == 1) "video" else "videos"
            val titles = state.videos.associate { it.uri to it.title }
            ApturaDialog(
                onDismissRequest = {
                    pendingCrossVolumeMoveJob = null
                    selectedUris = emptySet()
                    forgetTransferredClipboard(job.copiedUris, job.fromClipboard)
                    onRefresh()
                    transferResult = TransferResultMessage(
                        title = "Originals kept",
                        message = "Every $noun was copied to the destination folder. The originals were kept in place."
                    )
                    clearTransferJournal(job.id)
                },
                title = "Remove original from device?",
                message = "The $noun ${if (count == 1) "was" else "were"} copied to the selected folder. Android will now ask for permission to remove the original from your device to finish the move.",
                confirmLabel = "Continue",
                onConfirm = {
                    pendingCrossVolumeMoveJob = null
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
                },
                dismissLabel = "Keep original",
                onDismiss = {
                    pendingCrossVolumeMoveJob = null
                    selectedUris = emptySet()
                    forgetTransferredClipboard(job.copiedUris, job.fromClipboard)
                    onRefresh()
                    transferResult = TransferResultMessage(
                        title = "Originals kept",
                        message = "Every $noun was copied to the destination folder. The originals were kept in place."
                    )
                    clearTransferJournal(job.id)
                }
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
internal fun MenuDivider() {
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
    // Measured outside the scrolling box, which is given unlimited height and would never read as short.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // A phone held sideways has about 360 dp of height: the same spacing would push the main button to the edge of
        // the screen, so everything is tighter there and the button is in view without scrolling.
        val short = maxHeight < SHORT_WINDOW_HEIGHT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 32.dp, vertical = if (short) 12.dp else 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (short) 56.dp else 88.dp)
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
                        modifier = Modifier.size(if (short) 28.dp else 40.dp),
                        tint = ApturaColors.AccentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(if (short) 10.dp else 24.dp))

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

                Spacer(modifier = Modifier.height(if (short) 4.dp else 8.dp))

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

                Spacer(modifier = Modifier.height(if (short) 12.dp else 28.dp))

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
}

/** How long loading has to last before the skeleton is worth showing. */
internal const val SKELETON_DELAY_MS = 300L

/**
 * Shows [content] only if it is still wanted after [delayMs], fading in. Anything that loads faster never flashes a
 * placeholder for a few milliseconds, which reads as a flicker rather than as progress.
 */
@Composable
private fun AfterDelay(delayMs: Long = SKELETON_DELAY_MS, content: @Composable () -> Unit) {
    var due by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        due = true
    }
    AnimatedVisibility(visible = due, enter = fadeIn(tween(220)), exit = ExitTransition.None) { content() }
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
                .semantics(mergeDescendants = true) { contentDescription = "Looking for videos. The first scan can take a little while." }
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
            // The scan saves everything in one go, so nothing appears until it ends: say so, once, calmly.
            Text(
                text = "The first scan can take a little while.",
                style = ApturaTypography.bodySmall,
                color = ApturaColors.TextLowEmphasis
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
