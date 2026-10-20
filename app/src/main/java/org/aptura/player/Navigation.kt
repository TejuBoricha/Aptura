package org.aptura.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.repository.MediaRepositoryImpl
import org.aptura.player.data.scanner.MediaScanScheduler
import org.aptura.player.ui.library.MediaLibraryScreen
import org.aptura.player.ui.library.MediaLibraryViewModel

import androidx.compose.runtime.LaunchedEffect
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.ui.player.PlayerScreen
import org.aptura.player.ui.player.PlayerViewModel

import org.aptura.player.data.playback.AudioEnhancementController
import org.aptura.player.domain.model.DomainMediaItem

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(MediaLibrary)
    val context = LocalContext.current

    val database = remember { ApturaDatabase.getInstance(context) }
    val mediaRepository = remember { MediaRepositoryImpl(database.mediaItemDao()) }
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

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MediaLibrary> {
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
                    onSearchQueryChanged = libraryViewModel::onSearchQueryChanged,
                    onFolderSelected = libraryViewModel::onFolderSelected,
                    onSortOrderChanged = libraryViewModel::onSortOrderChanged,
                    onRefresh = {
                        libraryViewModel.onRefresh {
                            MediaScanScheduler.scheduleImmediateScan(context)
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
                }
                val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
                PlayerScreen(
                    uiState = uiState,
                    player = playbackController.player,
                    onBackClick = {
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
                    onStepFrame = playerViewModel::stepFrame
                )
            }
        }
    )
}
