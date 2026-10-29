@file:OptIn(
    coil3.annotation.ExperimentalCoilApi::class
)

package org.aptura.player

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import org.aptura.player.ui.components.StableStatusBars
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
import org.aptura.player.core.platform.AppVersion
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.data.repository.MediaRepositoryImpl
import org.aptura.player.core.media.ThumbnailRepository
import org.aptura.player.core.media.ApturaMediaFileManager
import org.aptura.player.core.media.MediaRenameResult
import org.aptura.player.data.scanner.MediaScanScheduler
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.LibraryStatistics
import org.aptura.player.ui.library.DeleteFromDeviceDialog
import org.aptura.player.ui.library.HideFromLibraryDialog
import org.aptura.player.ui.library.RemoveMissingVideoDialog
import org.aptura.player.ui.library.MediaDetailsContent
import org.aptura.player.ui.library.LibrarySortOrder
import org.aptura.player.ui.library.LibraryViewPrefs
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryUiState
import org.aptura.player.ui.library.MediaLibraryViewModel
import org.aptura.player.ui.library.RenameVideoDialog
import org.aptura.player.ui.library.buildPlayQueueRequest
import org.aptura.player.ui.library.canMoveToSystemTrash
import org.aptura.player.ui.library.rememberMediaDeleteFlow
import org.aptura.player.ui.library.rememberMediaTrashFlow
import org.aptura.player.ui.onboarding.OnboardingScreen
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerViewModel
import org.aptura.player.ui.settings.SettingsScreen
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ApturaTheme
import org.aptura.player.theme.ApturaWindowChrome
import org.aptura.player.theme.ThemeMode
import org.aptura.player.theme.themeModeForRoute

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

/**
 * Where the app starts: the welcome guide on the first launch, the library afterwards. Storage access does not matter
 * here: the guide used to be skipped whenever access was already granted (a reinstall, a device that pre-grants), and
 * since "seen" was only recorded inside the permission flow, those users never saw it at all.
 *
 * The guide is also shown once after an update: [lastSeenGuideVersion] records the build the user last finished it on,
 * so a new [currentVersionCode] means there is something new to explain. A stored 0 is a build from before versions
 * were recorded; those installs adopt the current version silently instead of being treated as an update.
 */
internal fun startDestinationFor(
    onboardingCompleted: Boolean,
    lastSeenGuideVersion: Int = 0,
    currentVersionCode: Int = 0
): NavKey = when {
    !onboardingCompleted -> Onboarding
    lastSeenGuideVersion == 0 -> MediaLibrary
    currentVersionCode > 0 && lastSeenGuideVersion != currentVersionCode -> Onboarding
    else -> MediaLibrary
}

/**
 * Decides the start destination only AFTER the persisted settings have loaded. It used to be computed from the
 * default `ApturaSettings()` (onboardingCompleted = false) on the very first frame, so users who had chosen
 * "Continue without access" were sent back through onboarding on every cold start (audit P1-03).
 */
@Composable
internal fun StartDestinationGate(
    settingsFlow: Flow<ApturaSettings>,
    currentVersionCode: Int = 0,
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
    val startKey = remember(
        initialSettings.onboardingCompleted,
        initialSettings.lastSeenGuideVersion,
        currentVersionCode
    ) {
        startDestinationFor(
            onboardingCompleted = initialSettings.onboardingCompleted,
            lastSeenGuideVersion = initialSettings.lastSeenGuideVersion,
            currentVersionCode = currentVersionCode
        )
    }
    content(initialSettings, startKey)
}

@Composable
fun MainNavigation(
    // Called once the saved theme is the one on screen (see FirstFrameGate).
    onThemeApplied: () -> Unit = {},
    initialLaunchUri: String? = null,
    onLaunchUriConsumed: () -> Unit = {},
    isPipMode: Boolean = false,
    // Devices without the PiP feature (some TVs and low-end tablets) must not show a control that can never work.
    pipSupported: Boolean = true,
    onEnterPip: () -> Unit = {},
    onPlayerRouteActiveChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepository = remember { ApturaSettingsRepository(context) }
    val currentVersionCode = remember(context) { AppVersion.versionCode(context) }

    // The app theme is applied here, above the start-destination gate, so the loading frame and every screen share it.
    // A null flow value means "settings not read yet": follow the system until the real choice arrives. The player
    // is always dark, but it wraps itself in its own theme (see the PlayerKey entry), so a light library and the dark
    // player can be on screen together during a transition and each keeps its own colours.
    val themeSettings by settingsRepository.settingsFlow
        .collectAsStateWithLifecycle<ApturaSettings?>(initialValue = null)

    // Runs after the composition that has the saved theme is applied, so the frame that follows is drawn with it.
    if (themeSettings != null) SideEffect(onThemeApplied)

    ApturaTheme(
        themeMode = themeSettings?.themeMode ?: ThemeMode.SYSTEM,
        accentColor = themeSettings?.accentColor ?: AccentColor.CYAN
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            StartDestinationGate(
                settingsFlow = settingsRepository.settingsFlow,
                currentVersionCode = currentVersionCode
            ) { initialSettings, startKey ->
                AppNavHost(
                    settingsRepository = settingsRepository,
                    initialSettings = initialSettings,
                    startKey = startKey,
                    currentVersionCode = currentVersionCode,
                    initialLaunchUri = initialLaunchUri,
                    onLaunchUriConsumed = onLaunchUriConsumed,
                    isPipMode = isPipMode,
                    pipSupported = pipSupported,
                    onEnterPip = onEnterPip,
                    onPlayerRouteActiveChanged = onPlayerRouteActiveChanged
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(
    settingsRepository: ApturaSettingsRepository,
    initialSettings: ApturaSettings,
    startKey: NavKey,
    currentVersionCode: Int,
    initialLaunchUri: String?,
    onLaunchUriConsumed: () -> Unit,
    isPipMode: Boolean,
    pipSupported: Boolean,
    onEnterPip: () -> Unit,
    onPlayerRouteActiveChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = initialSettings)

    // Installs from before the guide recorded a version adopt the current build silently. Without this, the first
    // launch after updating to this build would look like an update and replay the guide for everyone.
    LaunchedEffect(settings.onboardingCompleted, settings.lastSeenGuideVersion, currentVersionCode) {
        if (settings.onboardingCompleted && settings.lastSeenGuideVersion == 0 && currentVersionCode > 0) {
            settingsRepository.setLastSeenGuideVersion(currentVersionCode)
        }
    }

    val database = remember { ApturaDatabase.getInstance(context) }
    val mediaRepository = remember { MediaRepositoryImpl(database.mediaItemDao(), database.bookmarkDao()) }
    val libraryViewModel: MediaLibraryViewModel = viewModel {
        MediaLibraryViewModel(
            mediaRepository,
            scanActivity = MediaScanScheduler.isScanning(context.applicationContext),
            // The sort the user last picked, so the library opens the way it was left.
            initialSort = LibrarySortOrder.entries.firstOrNull { it.name == initialSettings.librarySortName }
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
            coroutineScope.launch {
                settingsRepository.setOnboardingCompleted(true)
                settingsRepository.setLastSeenGuideVersion(currentVersionCode)
            }
            MediaScanScheduler.scheduleImmediateScan(context)
            if (backStack.lastOrNull() == Onboarding) {
                backStack.removeLastOrNull()
                if (backStack.isEmpty()) backStack.add(MediaLibrary)
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

    // Synchronize active route with MainActivity so PiP only triggers while Player is active (F-04)
    val isPlayerRouteActive = backStack.lastOrNull() is PlayerKey
    // The bars' icons and the window background follow the screen on top. They are read here, in the same pass that
    // reads the back stack, so they change with the route and not a frame later. This scope sits outside the
    // player's own theme, so these colours are the app's.
    val appBackground = ApturaColors.SurfaceBase
    ApturaWindowChrome(
        darkScreen = isPlayerRouteActive || ApturaColors.IsDark,
        background = if (isPlayerRouteActive) Color.Black else appBackground
    )
    LaunchedEffect(isPlayerRouteActive) {
        onPlayerRouteActiveChanged(isPlayerRouteActive)
    }

    // Handle initial ACTION_VIEW launch intent (F-17: replace existing PlayerKey if already on player route)
    LaunchedEffect(initialLaunchUri) {
        if (initialLaunchUri != null) {
            playerViewModel.loadAndPlay(initialLaunchUri)
            if (backStack.lastOrNull() is PlayerKey) {
                backStack.removeLastOrNull()
            }
            backStack.add(PlayerKey(initialLaunchUri))
            onLaunchUriConsumed()
        }
    }

    val activity = context as? Activity
    val dismissPlayerAndResetOrientation: () -> Unit = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        playerViewModel.onPlayerDismissed()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { handleBack(backStack, dismissPlayerAndResetOrientation) },
        transitionSpec = { appTransition() },
        popTransitionSpec = { appPopTransition() },
        predictivePopTransitionSpec = { swipeEdge -> appPredictivePopTransition(swipeEdge) },
        entryProvider = entryProvider {
            entry<Onboarding> {
                // Leaves the guide: back to Settings when it was replayed from there, otherwise on to the library.
                // Finishing (by the button or by skipping) counts as seen, so the guide does not return next launch.
                val finishGuide: () -> Unit = {
                    coroutineScope.launch {
                        settingsRepository.setOnboardingCompleted(true)
                        settingsRepository.setLastSeenGuideVersion(currentVersionCode)
                    }
                    backStack.removeLastOrNull()
                    if (backStack.isEmpty()) backStack.add(MediaLibrary)
                }
                // The library page of the guide shows the person's own videos when there are enough of them (the replay
                // from Settings), and drawn samples otherwise (the first run, before anything is scanned).
                val libraryForGuide by libraryViewModel.uiState.collectAsStateWithLifecycle()
                OnboardingScreen(
                    libraryState = libraryForGuide as? MediaLibraryUiState.Success,
                    hasStorageAccess = PermissionHelper.hasStoragePermission(context),
                    onRequestPermissionClick = {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
                    },
                    onContinueWithoutPermissionClick = {
                        libraryViewModel.setPermissionDenied(true)
                        finishGuide()
                    },
                    onFinishClick = finishGuide,
                    // Reaching the last page counts as having seen the guide, even if the app is closed right there.
                    onReachedLastPage = {
                        coroutineScope.launch {
                            settingsRepository.setOnboardingCompleted(true)
                            settingsRepository.setLastSeenGuideVersion(currentVersionCode)
                        }
                    }
                )
            }

            entry<MediaLibrary> {
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
                    onPlayAudioOnly = { item, items ->
                        val request = buildPlayQueueRequest(item, items)
                        playerViewModel.playQueue(request.items, request.startIndex, audioOnly = true)
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
                    onSortOrderChanged = { order ->
                        libraryViewModel.onSortOrderChanged(order)
                        coroutineScope.launch { settingsRepository.updateLibrarySortName(order.name) }
                    },
                    viewPrefs = LibraryViewPrefs(
                        layout = settings.libraryLayout,
                        density = settings.libraryDensity,
                        detail = settings.libraryDetail,
                        folderLayout = settings.folderLayout
                    ),
                    onViewPrefsChanged = { new ->
                        // Only what changed is written, so a tap on a layout button is one small edit.
                        coroutineScope.launch {
                            if (new.layout != settings.libraryLayout) settingsRepository.updateLibraryLayout(new.layout)
                            if (new.density != settings.libraryDensity) settingsRepository.updateLibraryDensity(new.density)
                            if (new.detail != settings.libraryDetail) settingsRepository.updateLibraryDetail(new.detail)
                            if (new.folderLayout != settings.folderLayout) settingsRepository.updateFolderLayout(new.folderLayout)
                        }
                    },
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
                    onRemoveMissingVideo = libraryViewModel::removeMissingVideo,
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

            entry<MediaDetailsKey> { detailsKey ->
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
                var itemToRemoveMissing by remember { mutableStateOf<DomainMediaItem?>(null) }
                var pendingRenameUri by remember { mutableStateOf<String?>(null) }
                var pendingRenameTitle by remember { mutableStateOf<String?>(null) }
                val renameLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    val uri = pendingRenameUri
                    val title = pendingRenameTitle
                    if (result.resultCode == Activity.RESULT_OK && uri != null && title != null) {
                        when (
                            val outcome = ApturaMediaFileManager.renameMediaFile(
                                context = context,
                                uri = Uri.parse(uri),
                                newNameWithExt = title
                            )
                        ) {
                            is MediaRenameResult.Renamed -> {
                                libraryViewModel.renameVideo(
                                    uri = uri,
                                    newName = title,
                                    newUri = outcome.uri.toString(),
                                    newDisplayName = outcome.displayName
                                )
                                Toast.makeText(context, "Renamed to \"$title\"", Toast.LENGTH_SHORT).show()
                            }

                            is MediaRenameResult.PermissionRequired,
                            MediaRenameResult.Failed -> {
                                Toast.makeText(context, "Android did not allow the rename.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else if (result.resultCode != Activity.RESULT_OK) {
                        Toast.makeText(context, "Rename cancelled. The file name was not changed.", Toast.LENGTH_SHORT).show()
                    }
                    pendingRenameUri = null
                    pendingRenameTitle = null
                }
                val deleteFlow = rememberMediaDeleteFlow(onDeleted = { uri ->
                    ThumbnailRepository.deleteThumbnail(context, uri)
                    libraryViewModel.onVideoDeleted(uri)
                    backStack.removeLastOrNull()
                })
                val trashFlow = rememberMediaTrashFlow { uris ->
                    uris.forEach { uri ->
                        libraryViewModel.hideVideo(uri)
                    }
                    backStack.removeLastOrNull()
                }

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
                    onRemoveMissingClick = {
                        itemToRemoveMissing = item
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
                        // Stable while the player hides and restores the bars (see the library Scaffold).
                        .windowInsetsPadding(StableStatusBars)
                        .padding(top = 8.dp)
                )

                itemToRename?.let { target ->
                    RenameVideoDialog(
                        itemTitle = target.title,
                        onConfirm = { newName ->
                            when (
                                val outcome = ApturaMediaFileManager.renameMediaFile(
                                    context = context,
                                    uri = Uri.parse(target.uri),
                                    newNameWithExt = newName
                                )
                            ) {
                                is MediaRenameResult.Renamed -> {
                                    libraryViewModel.renameVideo(
                                        uri = target.uri,
                                        newName = newName,
                                        newUri = outcome.uri.toString(),
                                        newDisplayName = outcome.displayName
                                    )
                                    Toast.makeText(context, "Renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                                }

                                is MediaRenameResult.PermissionRequired -> {
                                    pendingRenameUri = target.uri
                                    pendingRenameTitle = newName
                                    renameLauncher.launch(
                                        IntentSenderRequest.Builder(outcome.intentSender).build()
                                    )
                                }

                                MediaRenameResult.Failed -> {
                                    Toast.makeText(context, "Could not rename this video file.", Toast.LENGTH_LONG).show()
                                }
                            }
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

                itemToRemoveMissing?.let { target ->
                    RemoveMissingVideoDialog(
                        itemTitle = target.title,
                        onConfirm = {
                            libraryViewModel.removeMissingVideo(target.uri)
                            itemToRemoveMissing = null
                            backStack.removeLastOrNull()
                        },
                        onDismiss = { itemToRemoveMissing = null }
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
                        allowTrash = canMoveToSystemTrash(listOf(target.uri)),
                        onTrashClick = {
                            if (trashFlow.trash(listOf(target.uri))) itemToDelete = null
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
                val missingVideoCount by libraryViewModel.missingVideoCount.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings = settings,
                    libraryStats = libraryStats,
                    hiddenVideoCount = hiddenVideoCount,
                    onRestoreHiddenVideos = libraryViewModel::restoreHiddenVideos,
                    missingVideoCount = missingVideoCount,
                    onClearMissingVideos = libraryViewModel::clearMissingVideos,
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
                    onResetPlaybackDefaults = {
                        coroutineScope.launch { settingsRepository.resetPlaybackDefaults() }
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
                    onUpdateThemeMode = { mode ->
                        coroutineScope.launch { settingsRepository.updateThemeMode(mode) }
                    },
                    onUpdateAccentColor = { accent ->
                        coroutineScope.launch { settingsRepository.updateAccentColor(accent) }
                    },
                    onClearThumbnailCache = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                ThumbnailRepository.clearCache(context)
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
                            mediaRepository.removeVideosByUriPrefix(folderUriStr)
                            MediaScanScheduler.scheduleImmediateScan(context)
                        }
                    }
                )
            }

            entry<PlayerKey>(metadata = PlayerRouteMetadata) { playerKey ->
                // Playback is a dark room whatever the app theme is. The theme is scoped to this entry, so the
                // library beneath it (a transition, a predictive back swipe) keeps the app's own colours.
                ApturaTheme(
                    themeMode = themeModeForRoute(isPlayerRouteActive = true, appThemeMode = settings.themeMode),
                    accentColor = settings.accentColor
                ) {
                    PlayerRouteFrame(closingColor = appBackground) {
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
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val recent = org.aptura.player.data.repository.SidecarSubtitleDetector.findRecentDownloads(
                                                context = context,
                                                searchQuery = uiState.cleanSearchQuery
                                            )
                                            if (recent.isNotEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    playerViewModel.setRecentDownloadedSubtitle(recent.first())
                                                }
                                            }
                                        } catch (_: Throwable) {}
                                    }
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
                            pipSupported = pipSupported,
                            onEnterPip = onEnterPip,
                            onBackClick = { handleBack(backStack, dismissPlayerAndResetOrientation) },
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
                            onStartHoldSpeed = playerViewModel::startHoldSpeedBoost,
                            onEndHoldSpeed = playerViewModel::endHoldSpeedBoost,
                            onResizeModeToggle = playerViewModel::toggleResizeMode,
                            onResizeModeSelected = playerViewModel::setResizeMode,
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
                            onExtendSleepTimer = playerViewModel::extendSleepTimer,
                            onSleepAtEndOfVideo = playerViewModel::startSleepAtEndOfVideo,
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
                            onRemoveExternalSubtitle = playerViewModel::removeExternalSubtitle,
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
                            onUpdateBookmarkNote = playerViewModel::updateBookmarkNote,
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
            }
        }
    )
}
