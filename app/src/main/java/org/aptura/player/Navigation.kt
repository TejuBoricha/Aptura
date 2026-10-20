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
    val playerViewModel: PlayerViewModel = viewModel {
        PlayerViewModel(playbackController, mediaRepository)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MediaLibrary> {
                val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
                MediaLibraryScreen(
                    uiState = uiState,
                    onVideoClick = { item, _ ->
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
                    playerViewModel.loadAndPlay(playerKey.videoUri)
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
                    onPlayModeToggle = playerViewModel::togglePlayMode
                )
            }
        }
    )
}
