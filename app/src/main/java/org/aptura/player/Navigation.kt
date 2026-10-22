@file:OptIn(
    coil.annotation.ExperimentalCoilApi::class,
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
import org.aptura.player.core.platform.PermissionHelper
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.playback.AudioEnhancementController
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
import org.aptura.player.ui.onboarding.OnboardingScreen
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerViewModel
import org.aptura.player.ui.settings.SettingsScreen

@Composable
fun MainNavigation(
    initialLaunchUri: String? = null,
    isPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val settingsRepository = remember { ApturaSettingsRepository(context) }
    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = ApturaSettings())

    val database = remember { ApturaDatabase.getInstance(context) }
    val mediaRepository = remember { MediaRepositoryImpl(database.mediaItemDao(), database.bookmarkDao()) }
    val libraryViewModel: MediaLibraryViewModel = viewModel {
        MediaLibraryViewModel(mediaRepository)
    }

    val playbackController = remember { PlayerManager.getOrCreate(context) }
    val audioEnhancementController = remember {
        AudioEnhancementController().apply {
            val sessionId = playbackController.audioSessionId
            if (sessionId > 0) {
                attachToSession(sessionId)
            }
        }
    }
    val playerViewModel: PlayerViewModel = viewModel {
        PlayerViewModel(playbackController, mediaRepository, audioEnhancementController)
    }

    val hasStorageAccess = remember { PermissionHelper.hasStoragePermission(context) }
    val startKey = remember {
        if (!hasStorageAccess && !settings.onboardingCompleted) Onboarding else MediaLibrary
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

    // Storage access state synchronization
    LaunchedEffect(Unit) {
        libraryViewModel.setPermissionDenied(!PermissionHelper.hasStoragePermission(context))
    }

    // Handle initial ACTION_VIEW launch intent
    LaunchedEffect(initialLaunchUri) {
        if (initialLaunchUri != null) {
            playerViewModel.loadAndPlay(initialLaunchUri)
            backStack.add(PlayerKey(initialLaunchUri))
        }
    }

    // Apply persisted default playback speed and decoder strategy
    LaunchedEffect(settings.defaultPlaybackSpeed) {
        playerViewModel.setPlaybackSpeed(settings.defaultPlaybackSpeed)
    }
    LaunchedEffect(settings.defaultDecoderStrategy) {
        playerViewModel.setDecoderStrategy(settings.defaultDecoderStrategy)
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
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
                MediaLibraryScreen(
                    uiState = uiState,
                    onVideoClick = { item, items ->
                        val domainItems = items.map { uiItem ->
                            DomainMediaItem(
                                uri = uiItem.uri,
                                title = uiItem.title,
                                durationMs = uiItem.durationMs,
                                sizeBytes = uiItem.sizeBytes,
                                mimeType = "video/*"
                            )
                        }
                        val clickedIndex = items.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                        playerViewModel.playQueue(domainItems, clickedIndex)
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
                    onRemoveVideo = libraryViewModel::removeVideoFromLibrary,
                    onResetProgress = libraryViewModel::resetVideoProgress,
                    onRenameVideo = libraryViewModel::renameVideo
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

                MediaDetailsContent(
                    item = item,
                    onClose = { backStack.removeLastOrNull() },
                    onPlayClick = {
                        val clickedIndex = domainItems.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                        if (domainItems.isNotEmpty()) {
                            playerViewModel.playQueue(domainItems, clickedIndex)
                        } else {
                            playerViewModel.loadAndPlay(item.uri)
                        }
                        backStack.add(PlayerKey(item.uri))
                    },
                    onPlayAudioOnlyClick = {
                        playerViewModel.loadAndPlay(item.uri)
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
                            libraryViewModel.removeVideoFromLibrary(target.uri)
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
                            libraryViewModel.removeVideoFromLibrary(target.uri)
                            itemToDelete = null
                            backStack.removeLastOrNull()
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

                SettingsScreen(
                    settings = settings,
                    libraryStats = libraryStats,
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
                        coroutineScope.launch { settingsRepository.updateDefaultDecoderStrategy(strategy) }
                        playerViewModel.setDecoderStrategy(strategy)
                    },
                    onClearThumbnailCache = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                context.cacheDir.deleteRecursively()
                                context.cacheDir.mkdirs()
                                coil.Coil.imageLoader(context).diskCache?.clear()
                                coil.Coil.imageLoader(context).memoryCache?.clear()
                            } catch (_: Throwable) {}
                        }
                    }
                )
            }

            entry<PlayerKey> { playerKey ->
                LaunchedEffect(playerKey.videoUri) {
                    val sessionId = playbackController.audioSessionId
                    if (sessionId > 0) {
                        audioEnhancementController.attachToSession(sessionId)
                    }
                    if (playerViewModel.uiState.value.videoUri != playerKey.videoUri) {
                        playerViewModel.loadAndPlay(playerKey.videoUri)
                    }
                    playerViewModel.setSubtitleFontSize(settings.subtitleFontSizeSp)
                    playerViewModel.setSubtitleTextColor(settings.subtitleTextColorHex)
                    playerViewModel.setSubtitleBackgroundEnabled(settings.subtitleBackgroundEnabled)

                    // Auto-discover and pair sidecar subtitles (.srt, .vtt, etc. in video directory or /subs/)
                    try {
                        val videoUri = android.net.Uri.parse(playerKey.videoUri)
                        val sidecars = org.aptura.player.data.repository.SidecarSubtitleDetector.detectSidecars(
                            context = context,
                            videoUri = videoUri,
                            videoDisplayName = playerViewModel.uiState.value.videoTitle
                        )
                        if (sidecars.isNotEmpty()) {
                            sidecars.forEach { sidecar ->
                                playerViewModel.addExternalSubtitle(
                                    uri = sidecar.uri.toString(),
                                    mimeType = sidecar.mimeType,
                                    label = "${sidecar.languageLabel} (${sidecar.displayName})"
                                )
                            }
                            val defaultSidecar = sidecars.firstOrNull { it.isDefault } ?: sidecars.first()
                            playerViewModel.setAutoPairedSubtitleToast("Auto-paired: ${defaultSidecar.languageLabel}")
                        }
                    } catch (_: Throwable) {}
                }

                val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

                // Check for recently downloaded subtitles when returning to the app from external search
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner, uiState.cleanSearchQuery) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
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
                    onBackClick = {
                        playerViewModel.onPlayerDismissed()
                        backStack.removeLastOrNull()
                    },
                    onPlayPauseClick = playerViewModel::togglePlayPause,
                    onSeekRelative = playerViewModel::seekRelative,
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
                    onDecoderStrategyChanged = { strategy ->
                        playerViewModel.setDecoderStrategy(strategy)
                        coroutineScope.launch { settingsRepository.updateDefaultDecoderStrategy(strategy) }
                    },
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
                    onDismissAutoPairToast = playerViewModel::dismissAutoPairToast
                )
            }
        }
    )
}
