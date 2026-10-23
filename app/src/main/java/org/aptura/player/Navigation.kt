@file:OptIn(
    coil3.annotation.ExperimentalCoilApi::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class
)

package org.aptura.player

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import org.aptura.player.theme.ApturaColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.aptura.player.core.diagnostics.DiagnosticStore
import org.aptura.player.core.platform.PermissionHelper
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.data.repository.MediaRepositoryImpl
import org.aptura.player.data.scanner.MediaScanScheduler
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.ui.library.DeleteFromDeviceDialog
import org.aptura.player.ui.library.HideFromLibraryDialog
import org.aptura.player.ui.library.MediaDetailsContent
import org.aptura.player.ui.library.MediaDetailsPlaceholder
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.MediaLibraryViewModel
import org.aptura.player.ui.library.RenameVideoDialog
import org.aptura.player.ui.library.buildPlayQueueRequest
import org.aptura.player.ui.library.rememberMediaDeleteFlow
import org.aptura.player.ui.onboarding.OnboardingScreen
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerViewModel
import org.aptura.player.ui.settings.SettingsScreen

/**
 * Single back-navigation policy used by the system Back gesture/button *and* the top-bar arrow.
 * Leaving the player by any route must pause playback and persist the position (audit P0-02);
 * previously only the arrow did, so system Back left the video playing under the library.
 */
internal fun handleBack(backStack: MutableList<NavKey>, onPlayerDismissed: () -> Unit) {
    if (backStack.lastOrNull() is PlayerKey) {
        onPlayerDismissed()
    }
    backStack.removeLastOrNull()
}

/** Where the app starts: onboarding only for users who neither granted access nor finished onboarding. */
internal fun startDestinationFor(hasStorageAccess: Boolean, onboardingCompleted: Boolean): NavKey =
    if (!hasStorageAccess && !onboardingCompleted) Onboarding else MediaLibrary

/**
 * Decides the start destination only AFTER the persisted settings have loaded. It used to be computed from the
 * default `ApturaSettings()` (onboardingCompleted = false) on the very first frame, so users who had chosen
 * "Continue without access" were sent back through onboarding on every cold start (audit P1-03).
 */
@Composable
internal fun StartDestinationGate(
    settingsFlow: Flow<ApturaSettings>,
    hasStorageAccess: Boolean,
    content: @Composable (initialSettings: ApturaSettings, startKey: NavKey) -> Unit
) {
    val loaded by produceState<ApturaSettings?>(initialValue = null, settingsFlow) {
        value = settingsFlow.first()
    }
    val initialSettings = loaded
    if (initialSettings == null) {
        Box(modifier = Modifier.fillMaxSize().background(ApturaColors.SurfaceBase))
        return
    }
    val startKey = remember(initialSettings.onboardingCompleted, hasStorageAccess) {
        startDestinationFor(hasStorageAccess, initialSettings.onboardingCompleted)
    }
    content(initialSettings, startKey)
}

@Composable
fun MainNavigation(
    initialLaunchUri: String? = null,
    onLaunchUriConsumed: () -> Unit = {},
    isPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepository = remember { ApturaSettingsRepository(context) }
    val hasStorageAccess = remember { PermissionHelper.hasStoragePermission(context) }

    StartDestinationGate(settingsRepository.settingsFlow, hasStorageAccess) { initialSettings, startKey ->
        AppNavHost(
            settingsRepository = settingsRepository,
            initialSettings = initialSettings,
            startKey = startKey,
            initialLaunchUri = initialLaunchUri,
            onLaunchUriConsumed = onLaunchUriConsumed,
            isPipMode = isPipMode,
            onEnterPip = onEnterPip
        )
    }
}

@Composable
private fun AppNavHost(
    settingsRepository: ApturaSettingsRepository,
    initialSettings: ApturaSettings,
    startKey: NavKey,
    initialLaunchUri: String?,
    onLaunchUriConsumed: () -> Unit,
    isPipMode: Boolean,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = initialSettings)

    val database = remember { ApturaDatabase.getInstance(context) }
    val mediaRepository = remember { MediaRepositoryImpl(database.mediaItemDao(), database.bookmarkDao()) }
    val libraryViewModel: MediaLibraryViewModel = viewModel {
        MediaLibraryViewModel(
            mediaRepository,
            scanActivity = MediaScanScheduler.isScanning(context.applicationContext)
        )
    }

    val playbackController = remember { PlayerManager.getOrCreate(context) }
    // The ViewModel creates, binds (to the player's audio session) and releases its own effects controller. It used to
    // be built here with `remember`, so every Activity recreation leaked another set of system audio effects.
    val playerViewModel: PlayerViewModel = viewModel {
        PlayerViewModel(playbackController, mediaRepository, settingsRepository = settingsRepository)
    }

    val backStack = rememberNavBackStack(startKey)

    // Permission Request Launcher (SCR-001)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        libraryViewModel.setPermissionDenied(!allGranted)
        if (allGranted) {
            coroutineScope.launch { settingsRepository.setOnboardingCompleted(true) }
            MediaScanScheduler.scheduleImmediateScan(context)
            if (backStack.lastOrNull() == Onboarding) {
                backStack.removeLastOrNull()
                backStack.add(MediaLibrary)
            }
        }
    }

    // SAF Document File Picker Launcher (Limited Storage Mode, SCR-012)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val uriString = uri.toString()
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}
            playerViewModel.loadAndPlay(uriString)
            backStack.add(PlayerKey(uriString))
        }
    }

    // SAF Directory Tree Picker Launcher (Custom Storage Folders & SD Card, FR-LIB-005)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}
            coroutineScope.launch {
                settingsRepository.addSafFolder(treeUri.toString())
                MediaScanScheduler.scheduleImmediateScan(context)
            }
        }
    }

    // Storage access state synchronization
    LaunchedEffect(Unit) {
        libraryViewModel.setPermissionDenied(!PermissionHelper.hasStoragePermission(context))
    }

    // Handle initial ACTION_VIEW launch intent
    LaunchedEffect(initialLaunchUri) {
        if (initialLaunchUri != null) {
            playerViewModel.loadAndPlay(initialLaunchUri)
            backStack.add(PlayerKey(initialLaunchUri))
            onLaunchUriConsumed()
        }
    }

    // Apply persisted default playback speed and decoder strategy
    LaunchedEffect(settings.defaultPlaybackSpeed) {
        playerViewModel.setPlaybackSpeed(settings.defaultPlaybackSpeed)
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        backStack = backStack,
        onBack = { handleBack(backStack, playerViewModel::onPlayerDismissed) },
        sceneStrategy = listDetailStrategy,
        entryProvider = entryProvider {
            entry<Onboarding> {
                OnboardingScreen(
                    onRequestPermissionClick = {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    },
                    onContinueWithoutPermissionClick = {
                        coroutineScope.launch { settingsRepository.setOnboardingCompleted(true) }
                        libraryViewModel.setPermissionDenied(true)
                        backStack.removeLastOrNull()
                        backStack.add(MediaLibrary)
                    }
                )
            }

            entry<MediaLibrary>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        MediaDetailsPlaceholder(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                        )
                    }
                )
            ) {
                val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
                // Read off the main thread: the crash log is a file.
                var pendingCrash by remember { mutableStateOf<DiagnosticStore.StoredCrash?>(null) }
                LaunchedEffect(Unit) {
                    pendingCrash = withContext(Dispatchers.IO) { ApturaDiagnostics.pendingCrashNotice() }
                }
                MediaLibraryScreen(
                    uiState = uiState,
                    showCrashNotice = pendingCrash != null,
                    onShareCrashDetails = {
                        pendingCrash?.let { crash ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Aptura Redacted Diagnostic Report")
                                putExtra(Intent.EXTRA_TEXT, ApturaDiagnostics.generateDiagnosticMarkdown(context))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share crash details"))
                            ApturaDiagnostics.acknowledgeCrash(crash.name)
                        }
                        pendingCrash = null
                    },
                    onDismissCrashNotice = {
                        pendingCrash?.let { ApturaDiagnostics.acknowledgeCrash(it.name) }
                        pendingCrash = null
                    },
                    onVideoClick = { item, items ->
                        val request = buildPlayQueueRequest(item, items)
                        playerViewModel.playQueue(request.items, request.startIndex)
                        backStack.add(PlayerKey(item.uri))
                    },
                    onShowDetails = { item ->
                        if (backStack.lastOrNull() is MediaDetailsKey) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(MediaDetailsKey(item.uri))
                    },
                    onSearchQueryChanged = libraryViewModel::onSearchQueryChanged,
                    onFolderSelected = libraryViewModel::onFolderSelected,
                    onSortOrderChanged = libraryViewModel::onSortOrderChanged,
                    onRefresh = {
                        libraryViewModel.onRefresh {
                            MediaScanScheduler.scheduleImmediateScan(context)
                        }
                    },
                    onSettingsClick = {
                        backStack.add(Settings)
                    },
                    onOpenFilePicker = {
                        filePickerLauncher.launch(arrayOf("video/*"))
                    },
                    onRequestPermission = {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    },
                    onRemoveVideo = libraryViewModel::hideVideo,
                    onVideoDeleted = libraryViewModel::onVideoDeleted,
                    onResetProgress = libraryViewModel::resetVideoProgress,
                    onRenameVideo = libraryViewModel::renameVideo,
                    onAddFolderClick = { folderPickerLauncher.launch(null) },
                    onPlayNext = { videoItem ->
                        playerViewModel.playNext(videoItem)
                        android.widget.Toast.makeText(context, "Will play next: ${videoItem.title}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onAddToQueue = { videoItem ->
                        playerViewModel.addToQueue(videoItem)
                        android.widget.Toast.makeText(context, "Added to queue: ${videoItem.title}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            entry<MediaDetailsKey>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { detailsKey ->
                val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
                val domainItems = (uiState as? MediaLibraryUiState.Success)?.videos?.map { it.rawItem } ?: emptyList()
                val item = domainItems.find { it.uri == detailsKey.videoUri }
                    ?: remember(detailsKey.videoUri) {
                        DomainMediaItem(
                            uri = detailsKey.videoUri,
                            title = detailsKey.videoUri.substringAfterLast('/'),
                            durationMs = 0L,
                            sizeBytes = 0L,
                            mimeType = "video/*"
                        )
                    }

                var itemToRename by remember { mutableStateOf<DomainMediaItem?>(null) }
                var itemToDelete by remember { mutableStateOf<DomainMediaItem?>(null) }
                var itemToHide by remember { mutableStateOf<DomainMediaItem?>(null) }
                val deleteFlow = rememberMediaDeleteFlow(onDeleted = { uri ->
                    libraryViewModel.onVideoDeleted(uri)
                    backStack.removeLastOrNull()
                })

                MediaDetailsContent(
                    item = item,
                    onClose = { backStack.removeLastOrNull() },
                    onPlayClick = {
                        val request = buildPlayQueueRequest(item, domainItems)
                        playerViewModel.playQueue(request.items, request.startIndex)
                        backStack.add(PlayerKey(item.uri))
                    },
                    onPlayAudioOnlyClick = {
                        val request = buildPlayQueueRequest(item, domainItems)
                        playerViewModel.playQueue(request.items, request.startIndex, audioOnly = true)
                        backStack.add(PlayerKey(item.uri))
                    },
                    onOpenWithClick = {
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(item.uri), item.mimeType ?: "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(viewIntent, "Open Video With..."))
                        } catch (_: Exception) {}
                    },
                    onShareClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = item.mimeType ?: "video/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        } catch (_: Exception) {}
                    },
                    onResetProgressClick = {
                        libraryViewModel.resetVideoProgress(item.uri)
                    },
                    onRenameClick = {
                        itemToRename = item
                    },
                    onHideClick = {
                        itemToHide = item
                    },
                    onDeleteClick = {
                        itemToDelete = item
                    },
                    onPlayNextClick = {
                        playerViewModel.playNext(item)
                        android.widget.Toast.makeText(context, "Will play next: ${item.title}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onAddToQueueClick = {
                        playerViewModel.addToQueue(item)
                        android.widget.Toast.makeText(context, "Added to queue: ${item.title}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    showCloseButton = true,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                )

                itemToRename?.let { target ->
                    RenameVideoDialog(
                        itemTitle = target.title,
                        onConfirm = { newName ->
                            libraryViewModel.renameVideo(target.uri, newName)
                            itemToRename = null
                        },
                        onDismiss = { itemToRename = null }
                    )
                }

                itemToHide?.let { target ->
                    HideFromLibraryDialog(
                        itemTitle = target.title,
                        onConfirm = {
                            libraryViewModel.hideVideo(target.uri)
                            itemToHide = null
                            backStack.removeLastOrNull()
                        },
                        onDismiss = { itemToHide = null }
                    )
                }

                itemToDelete?.let { target ->
                    DeleteFromDeviceDialog(
                        itemTitle = target.title,
                        onConfirm = {
                            // Real deletion (system consent when required); the screen closes only on success.
                            deleteFlow.delete(target.uri)
                            itemToDelete = null
                        },
                        onDismiss = { itemToDelete = null }
                    )
                }
            }

            entry<Settings> {
                var libraryStats by remember { mutableStateOf(LibraryStatistics()) }
                LaunchedEffect(Unit) {
                    libraryStats = mediaRepository.getLibraryStatistics()
                }

                val hiddenVideoCount by libraryViewModel.hiddenVideoCount.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings = settings,
                    libraryStats = libraryStats,
                    hiddenVideoCount = hiddenVideoCount,
                    onRestoreHiddenVideos = libraryViewModel::restoreHiddenVideos,
                    onBackClick = { backStack.removeLastOrNull() },
                    onUpdateSpeed = { speed ->
                        coroutineScope.launch { settingsRepository.updateDefaultPlaybackSpeed(speed) }
                    },
                    onUpdateResizeMode = { mode ->
                        coroutineScope.launch { settingsRepository.updateDefaultResizeMode(mode) }
                    },
                    onUpdateSeekDelta = { delta ->
                        coroutineScope.launch { settingsRepository.updateSeekDeltaSeconds(delta) }
                    },
                    onUpdateBackgroundAudio = { bg ->
                        coroutineScope.launch { settingsRepository.updateBackgroundAudioEnabled(bg) }
                    },
                    onUpdateAutoAdvanceCountdown = { cd ->
                        coroutineScope.launch { settingsRepository.updateAutoAdvanceCountdown(cd) }
                    },
                    onRescanClick = {
                        MediaScanScheduler.scheduleImmediateScan(context)
                    },
                    onReplayOnboardingClick = {
                        backStack.add(Onboarding)
                    },
                    onUpdateDecoderStrategy = { strategy ->
                        // The Settings screen edits the DEFAULT; the player picks it up from the settings flow.
                        coroutineScope.launch { settingsRepository.updateDefaultDecoderStrategy(strategy) }
                    },
                    onClearThumbnailCache = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                context.cacheDir.deleteRecursively()
                                context.cacheDir.mkdirs()
                                coil3.SingletonImageLoader.get(context).diskCache?.clear()
                                coil3.SingletonImageLoader.get(context).memoryCache?.clear()
                            } catch (_: Throwable) {}
                        }
                    },
                    onAddFolderClick = { folderPickerLauncher.launch(null) },
                    onRemoveSafFolder = { folderUriStr ->
                        try {
                            context.contentResolver.releasePersistableUriPermission(
                                android.net.Uri.parse(folderUriStr),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Throwable) {}
                        coroutineScope.launch {
                            settingsRepository.removeSafFolder(folderUriStr)
                            MediaScanScheduler.scheduleImmediateScan(context)
                        }
                    }
                )
            }

            entry<PlayerKey> { playerKey ->
                LaunchedEffect(playerKey.videoUri) {
                    if (playerViewModel.uiState.value.videoUri != playerKey.videoUri) {
                        playerViewModel.loadAndPlay(playerKey.videoUri)
                    }
                    playerViewModel.setSubtitleFontSize(settings.subtitleFontSizeSp)
                    playerViewModel.setSubtitleTextColor(settings.subtitleTextColorHex)
                    playerViewModel.setSubtitleBackgroundEnabled(settings.subtitleBackgroundEnabled)
                }

                val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

                // Check for recently downloaded subtitles when returning to the app from external search
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner, uiState.cleanSearchQuery) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                            // The process can be killed any time after this; make sure the position is on disk.
                            playerViewModel.persistProgress()
                        }
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && uiState.cleanSearchQuery.isNotBlank()) {
                            try {
                                val recent = org.aptura.player.data.repository.SidecarSubtitleDetector.findRecentDownloads(
                                    context = context,
                                    searchQuery = uiState.cleanSearchQuery
                                )
                                if (recent.isNotEmpty()) {
                                    playerViewModel.setRecentDownloadedSubtitle(recent.first())
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                PlayerScreen(
                    uiState = uiState,
                    player = playbackController.player,
                    isPipMode = isPipMode,
                    onEnterPip = onEnterPip,
                    onBackClick = { handleBack(backStack, playerViewModel::onPlayerDismissed) },
                    onPlayPauseClick = playerViewModel::togglePlayPause,
                    onSeekRelative = { delta -> playerViewModel.seekRelative(delta) },
                    onSeekRelativeWithSide = playerViewModel::seekRelative,
                    onHorizontalSwipeSeekStart = playerViewModel::onHorizontalSwipeSeekStart,
                    onHorizontalSwipeSeek = playerViewModel::onHorizontalSwipeSeek,
                    onHorizontalSwipeSeekCommit = playerViewModel::onHorizontalSwipeSeekCommit,
                    onHorizontalSwipeSeekCancel = playerViewModel::onHorizontalSwipeSeekCancel,
                    onTriggerLockHint = playerViewModel::triggerLockHint,
                    onTwoFingerTap = playerViewModel::togglePlayPause,
                    onScrubStart = playerViewModel::onScrubStart,
                    onScrubMove = playerViewModel::onScrubMove,
                    onScrubEnd = playerViewModel::onScrubEnd,
                    onControlsVisibilityToggle = playerViewModel::toggleControlsVisibility,
                    onLockToggle = playerViewModel::toggleLock,
                    onSpeedSelected = playerViewModel::setPlaybackSpeed,
                    onResizeModeToggle = playerViewModel::toggleResizeMode,
                    onSpeedSheetVisible = playerViewModel::setSpeedSheetVisible,
                    onAudioSheetVisible = playerViewModel::setAudioSheetVisible,
                    onSubtitleSheetVisible = playerViewModel::setSubtitleSheetVisible,
                    onAudioTrackSelected = playerViewModel::selectAudioTrack,
                    onSubtitleTrackSelected = playerViewModel::selectSubtitleTrack,
                    onVolumePercentChanged = playerViewModel::setVolumePercent,
                    onBrightnessPercentChanged = playerViewModel::setBrightnessPercent,
                    onRetryPlayback = playerViewModel::retryPlayback,
                    onPlayModeToggle = playerViewModel::togglePlayMode,
                    onSkipNext = playerViewModel::skipToNext,
                    onSkipPrevious = playerViewModel::skipToPrevious,
                    onAdvancedSheetVisible = playerViewModel::setAdvancedSheetVisible,
                    onSubtitleDelayChanged = playerViewModel::setSubtitleDelay,
                    onAudioDelayChanged = playerViewModel::setAudioDelay,
                    onStartSleepTimer = playerViewModel::startSleepTimer,
                    onCancelSleepTimer = playerViewModel::cancelSleepTimer,
                    onDelayStepChanged = playerViewModel::setDelayStep,
                    onEqualizerEnabledChanged = playerViewModel::setEqualizerEnabled,
                    onEqualizerPresetSelected = playerViewModel::setEqualizerPreset,
                    onEqualizerBandChanged = playerViewModel::setEqualizerBandLevel,
                    onBassBoostChanged = playerViewModel::setBassBoost,
                    onVirtualizerChanged = playerViewModel::setVirtualizer,
                    onLoudnessBoostChanged = playerViewModel::setLoudnessBoost,
                    onContrastChanged = playerViewModel::setContrast,
                    onSaturationChanged = playerViewModel::setSaturation,
                    onNightModeToggle = { enabled -> playerViewModel.setNightMode(enabled) },
                    onNightModeWarmthChanged = { warmth -> playerViewModel.setNightMode(true, warmth) },
                    onResetPictureAdjustment = playerViewModel::resetPictureAdjustments,
                    onSetAbPointA = playerViewModel::setAbPointA,
                    onSetAbPointB = playerViewModel::setAbPointB,
                    onClearAbRepeat = playerViewModel::clearAbRepeat,
                    onStepFrame = playerViewModel::stepFrame,
                    onCancelAutoAdvance = playerViewModel::cancelAutoAdvance,
                    onPlayNextImmediately = playerViewModel::playNextImmediately,
                    onExternalSubtitleSelected = { uri, mimeType, label ->
                        playerViewModel.addExternalSubtitle(uri.toString(), mimeType, label)
                    },
                    onRemoveExternalSubtitle = playerViewModel::removeExternalSubtitles,
                    onSubtitleFontSizeChanged = { sizeSp ->
                        playerViewModel.setSubtitleFontSize(sizeSp)
                        coroutineScope.launch { settingsRepository.updateSubtitleFontSize(sizeSp) }
                    },
                    onSubtitleTextColorChanged = { colorHex ->
                        playerViewModel.setSubtitleTextColor(colorHex)
                        coroutineScope.launch { settingsRepository.updateSubtitleTextColor(colorHex) }
                    },
                    onSubtitleBackgroundChanged = { enabled ->
                        playerViewModel.setSubtitleBackgroundEnabled(enabled)
                        coroutineScope.launch { settingsRepository.updateSubtitleBackground(enabled) }
                    },
                    onAddBookmark = playerViewModel::addBookmark,
                    onDeleteBookmark = playerViewModel::deleteBookmark,
                    // In the player the choice belongs to this video only (it used to also overwrite the global default).
                    onDecoderStrategyChanged = playerViewModel::setDecoderStrategy,
                    onDismissCompatibilityNotice = playerViewModel::dismissCompatibilityNotice,
                    onSearchWebClick = { playerViewModel.showWebSearchDialog(true) },
                    onDismissWebSearchDialog = { playerViewModel.showWebSearchDialog(false) },
                    onLaunchWebSearch = { query, provider ->
                        playerViewModel.checkpointPlayback()
                        playerViewModel.showWebSearchDialog(false)
                        val targetUrl = when (provider) {
                            0 -> org.aptura.player.core.media.SubtitleQuerySanitizer.buildSubdlUrl(query)
                            1 -> org.aptura.player.core.media.SubtitleQuerySanitizer.buildOpenSubtitlesUrl(query)
                            else -> org.aptura.player.core.media.SubtitleQuerySanitizer.buildGoogleSearchUrl(query)
                        }
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                        } catch (_: Throwable) {
                            android.widget.Toast.makeText(context, "No web browser found to open link", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAttachRecentSubtitle = { sidecar ->
                        playerViewModel.addExternalSubtitle(sidecar.uri.toString(), sidecar.mimeType, sidecar.displayName)
                        playerViewModel.setRecentDownloadedSubtitle(null)
                        android.widget.Toast.makeText(context, "Attached: ${sidecar.displayName}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onDismissRecentSubtitle = {
                        playerViewModel.setRecentDownloadedSubtitle(null)
                    },
                    onDismissAutoPairToast = playerViewModel::dismissAutoPairToast,
                    onQueueSheetVisible = playerViewModel::showQueueSheet,
                    onJumpToQueueItem = playerViewModel::jumpToQueueItem,
                    onMoveQueueItem = playerViewModel::moveQueueItem,
                    onRemoveQueueItem = playerViewModel::removeFromQueue,
                    onClearQueue = playerViewModel::clearQueue,
                    onReplayCurrentItem = playerViewModel::replayCurrentItem,
                    onToggleStatsOverlay = playerViewModel::toggleStatsOverlay,
                    onWhyCompatibilityClick = { playerViewModel.setWhyCompatibilitySheetVisible(true) },
                    onDismissWhyCompatibilitySheet = { playerViewModel.setWhyCompatibilitySheetVisible(false) },
                    onResetVideoPreferences = playerViewModel::resetVideoPreferences,
                    onAudioOnlyChanged = playerViewModel::setAudioOnly
                )
            }
        }
    )
}
