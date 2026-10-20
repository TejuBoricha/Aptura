package org.aptura.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.MediaRepository

/**
 * Presentation ViewModel for the Media Library Screen.
 * Enforces Unidirectional Data Flow (UDF), explicit Dispatchers, and state immutability.
 */
class MediaLibraryViewModel(
    private val mediaRepository: MediaRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder

    private val _sortOrder = MutableStateFlow(LibrarySortOrder.DATE_ADDED_DESC)
    val sortOrder: StateFlow<LibrarySortOrder> = _sortOrder

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<MediaLibraryUiState> = combine(
        mediaRepository.getAllVideos(),
        mediaRepository.getAllFolders(),
        _searchQuery,
        _selectedFolder,
        _sortOrder
    ) { allVideos: List<DomainMediaItem>, folders: List<String>, query: String, folderFilter: String?, sort: LibrarySortOrder ->
        if (allVideos.isEmpty() && query.isBlank() && folderFilter == null) {
            MediaLibraryUiState.Empty(isFiltered = false)
        } else {
            var filtered = allVideos

            // Apply folder filter
            if (folderFilter != null) {
                filtered = filtered.filter { it.folderPath == folderFilter }
            }

            // Apply search query filter
            if (query.isNotBlank()) {
                val trimmedQuery = query.trim()
                filtered = filtered.filter { item ->
                    item.title.contains(trimmedQuery, ignoreCase = true) ||
                        item.folderPath.contains(trimmedQuery, ignoreCase = true)
                }
            }

            // Apply sort order
            filtered = when (sort) {
                LibrarySortOrder.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAddedSec }
                LibrarySortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            }

            if (filtered.isEmpty()) {
                MediaLibraryUiState.Empty(isFiltered = true, query = query)
            } else {
                val cards = filtered.map { item -> item.toCardUiModel() }
                MediaLibraryUiState.Success(
                    videos = cards,
                    folders = folders,
                    selectedFolder = folderFilter,
                    searchQuery = query,
                    sortOrder = sort,
                    totalCount = cards.size,
                    isRefreshing = _isRefreshing.value
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaLibraryUiState.Loading
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFolderSelected(folder: String?) {
        _selectedFolder.value = folder
    }

    fun onSortOrderChanged(newSortOrder: LibrarySortOrder) {
        _sortOrder.value = newSortOrder
    }

    fun onRefresh(onTriggerScan: (() -> Unit)? = null) {
        viewModelScope.launch(dispatcher) {
            _isRefreshing.value = true
            onTriggerScan?.invoke()
            _isRefreshing.value = false
        }
    }

    private fun DomainMediaItem.toCardUiModel(): VideoCardUiModel {
        val progress = MediaFormatters.calculateProgressFraction(playbackPositionMs, durationMs)
        val folderDisplay = folderPath.ifBlank { "Internal Storage" }

        return VideoCardUiModel(
            uri = uri,
            title = title,
            durationFormatted = MediaFormatters.formatDuration(durationMs),
            fileSizeFormatted = MediaFormatters.formatFileSize(sizeBytes),
            folderName = folderDisplay,
            progressFraction = progress,
            isCompleted = isCompleted,
            rawItem = this
        )
    }
}
