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

    private data class FilterState(
        val query: String,
        val folder: String?,
        val sort: LibrarySortOrder,
        val isRefreshing: Boolean
    )

    private val filterState = combine(
        _searchQuery,
        _selectedFolder,
        _sortOrder,
        _isRefreshing
    ) { query, folder, sort, refreshing ->
        FilterState(query, folder, sort, refreshing)
    }

    val uiState: StateFlow<MediaLibraryUiState> = combine(
        mediaRepository.getAllVideos(),
        mediaRepository.getAllFolders(),
        filterState
    ) { allVideos: List<DomainMediaItem>, folders: List<String>, filter: FilterState ->
        if (allVideos.isEmpty() && filter.query.isBlank() && filter.folder == null) {
            MediaLibraryUiState.Empty(isFiltered = false)
        } else {
            var filtered = allVideos

            // Apply folder filter
            if (filter.folder != null) {
                filtered = filtered.filter { it.folderPath == filter.folder }
            }

            // Apply search query filter
            if (filter.query.isNotBlank()) {
                val trimmedQuery = filter.query.trim()
                filtered = filtered.filter { item ->
                    item.title.contains(trimmedQuery, ignoreCase = true) ||
                        item.folderPath.contains(trimmedQuery, ignoreCase = true)
                }
            }

            // Apply sort order
            filtered = when (filter.sort) {
                LibrarySortOrder.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAddedSec }
                LibrarySortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            }

            if (filtered.isEmpty()) {
                MediaLibraryUiState.Empty(isFiltered = true, query = filter.query)
            } else {
                val cards = filtered.map { item -> item.toCardUiModel() }
                MediaLibraryUiState.Success(
                    videos = cards,
                    folders = folders,
                    selectedFolder = filter.folder,
                    searchQuery = filter.query,
                    sortOrder = filter.sort,
                    totalCount = cards.size,
                    isRefreshing = filter.isRefreshing
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
        val folderDisplay = if (folderPath.contains('/')) {
            folderPath.trimEnd('/').substringAfterLast('/').ifBlank { "Internal Storage" }
        } else {
            folderPath.ifBlank { "Internal Storage" }
        }

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
