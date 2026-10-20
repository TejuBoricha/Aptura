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

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(MediaLibrary)
    val context = LocalContext.current

    val database = remember { ApturaDatabase.getInstance(context) }
    val mediaRepository = remember { MediaRepositoryImpl(database.mediaItemDao()) }
    val libraryViewModel: MediaLibraryViewModel = viewModel {
        MediaLibraryViewModel(mediaRepository)
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
        }
    )
}
