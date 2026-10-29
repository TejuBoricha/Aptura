package org.aptura.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.aptura.player.core.diagnostics.ApturaErrorHandler
import org.aptura.player.core.diagnostics.EventCode
import org.aptura.player.domain.model.DomainMediaItem
import org.aptura.player.domain.repository.MediaRepository

/**
 * Presentation ViewModel for the Media Library Screen.
 * Enforces Unidirectional Data Flow (UDF), explicit Dispatchers, and state immutability.
 *
 * Performance (P-03/P-04): the filter + search + sort + format pass over the whole library runs on
 * [computeDispatcher] (never Main), and non-blank search input is debounced by [searchDebounceMs]
 * so a fast typist triggers one pass instead of one per keystroke.
 */
@OptIn(FlowPreview::class)
class MediaLibraryViewModel(
    private val mediaRepository: MediaRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
    /** Emits true while a library scan is queued/running. Null keeps the legacy "enqueue only" behaviour. */
    private val scanActivity: kotlinx.coroutines.flow.Flow<Boolean>? = null,
    /** The sort the user picked in an earlier session, or null for the library's own choice by folder. */
    initialSort: LibrarySortOrder? = null,
    /** How long an empty library must stay empty, with no scan under way, before "No videos yet" is shown. */
    private val emptySettleMs: Long = EMPTY_SETTLE_MS
) : ViewModel() {

    companion object {
        const val SEARCH_DEBOUNCE_MS = 250L

        /**
         * The database answers "nothing" a moment before the scan that fills it is reported as queued, so on a first
         * install an empty library is not yet "No videos yet". It has to hold for this long first.
         */
        const val EMPTY_SETTLE_MS = 600L
    }

    /** Q-02: a failing repository call must never crash the process. */
    private val errorHandler = ApturaErrorHandler.create("library_vm", EventCode.EV_006)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder

    /** The sort the user picked; null = automatic (newest first overall, episode order inside a folder). */
    private val _chosenSort = MutableStateFlow(initialSort)

    private val _isRefreshing = MutableStateFlow(false)

    init {
        // The spinner reflects the real scan (WorkManager), not just the instant the request was enqueued.
        scanActivity?.let { activity ->
            viewModelScope.launch(dispatcher + errorHandler) {
                activity.collect { scanning -> _isRefreshing.value = scanning }
            }
        }
    }
    private val _isPermissionDenied = MutableStateFlow(false)

    private data class FilterState(
        val query: String,
        val folder: String?,
        val sort: LibrarySortOrder,
        val showContinueWatching: Boolean,
        val isRefreshing: Boolean,
        val isPermissionDenied: Boolean
    )

    // Blank input (initial value / clear) applies instantly; typed input waits for a pause.
    private val debouncedQuery = _searchQuery.debounce { query ->
        if (query.isBlank()) 0L else searchDebounceMs
    }

    private val filterState = combine(
        debouncedQuery,
        _selectedFolder,
        _chosenSort,
        _isRefreshing,
        _isPermissionDenied
    ) { query, folder, chosenSort, refreshing, permissionDenied ->
        FilterState(
            query = query,
            folder = folder,
            sort = chosenSort ?: automaticSortOrder(folder),
            // "Continue watching" belongs to the unfiltered home view only.
            showContinueWatching = query.isBlank() && folder == null,
            isRefreshing = refreshing,
            isPermissionDenied = permissionDenied
        )
    }

    private val libraryContent: kotlinx.coroutines.flow.Flow<MediaLibraryUiState> = combine(
        mediaRepository.getAllVideos(),
        mediaRepository.getAllFolders(),
        filterState
    ) { allVideos: List<DomainMediaItem>, folders: List<String>, filter: FilterState ->
        if (allVideos.isEmpty() && filter.query.isBlank() && filter.folder == null) {
            // A scan that is queued or running may be about to fill the library (the first one after an install always
            // is), so this is "still looking", not "nothing found". Without access there is no scan to wait for.
            if (filter.isRefreshing && !filter.isPermissionDenied) {
                MediaLibraryUiState.Loading
            } else {
                MediaLibraryUiState.Empty(isFiltered = false, isPermissionDenied = filter.isPermissionDenied)
            }
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
                        friendlyFolderName(item.folderPath).contains(trimmedQuery, ignoreCase = true)
                }
            }

            // Apply sort order (natural for titles: "Episode 2" before "Episode 10")
            filtered = filtered.sortedFor(filter.sort)

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
                    isRefreshing = filter.isRefreshing,
                    isLimitedMode = filter.isPermissionDenied,
                    continueWatching = if (filter.showContinueWatching) {
                        allVideos.continueWatching().map { it.toCardUiModel() }
                    } else {
                        emptyList()
                    },
                    libraryVideoCount = allVideos.size,
                    libraryDurationMs = allVideos.sumOf { it.durationMs },
                    librarySizeBytes = allVideos.sumOf { it.sizeBytes },
                    folderCounts = allVideos.groupingBy { it.folderPath }.eachCount(),
                    folderSummaries = allVideos.summarizeFolders()
                )
            }
        }
    }.flowOn(computeDispatcher)

    /**
     * The text field echoes the *raw* query so typing never lags or jumps the cursor; only this cheap
     * overlay runs per keystroke while the expensive [libraryContent] pass stays debounced.
     */
    val uiState: StateFlow<MediaLibraryUiState> = combine(
        // "No videos yet" has to hold for a moment before it is shown: on a first install the database is empty for a
        // few milliseconds before the scan is reported, and showing it then flashed an empty screen in between.
        libraryContent.debounce { state ->
            if (state is MediaLibraryUiState.Empty && !state.isFiltered && !state.isPermissionDenied) emptySettleMs else 0L
        },
        _searchQuery
    ) { content, rawQuery ->
        when (content) {
            is MediaLibraryUiState.Success -> content.copy(searchQuery = rawQuery)
            is MediaLibraryUiState.Empty ->
                if (content.isFiltered) content.copy(query = rawQuery) else content
            MediaLibraryUiState.Loading -> content
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaLibraryUiState.Loading
    )

    fun setPermissionDenied(denied: Boolean) {
        _isPermissionDenied.value = denied
    }

    /** Drops the index row of a video whose file no longer exists (after a real deletion). */
    fun removeVideoFromLibrary(uri: String) {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.removeVideoFromLibrary(uri)
        }
    }

    /** Called once Android confirmed the file was deleted from storage. */
    fun onVideoDeleted(uri: String) = removeVideoFromLibrary(uri)

    /** Hides a video from the library; the hide survives rescans until restored. */
    fun hideVideo(uri: String) {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.hideVideo(uri)
        }
    }

    fun restoreHiddenVideos() {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.restoreHiddenVideos()
        }
    }

    val hiddenVideoCount: StateFlow<Int> = mediaRepository.getHiddenVideoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val missingVideoCount: StateFlow<Int> = mediaRepository.getMissingVideos()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun clearMissingVideos() {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.clearMissingVideos()
        }
    }

    fun renameVideo(
        uri: String,
        newName: String,
        newUri: String? = null,
        newDisplayName: String? = null
    ) {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.renameVideo(uri, newName, newUri, newDisplayName)
        }
    }

    /**
     * Removes a missing item from the index. Unlike [hideVideo], this deletes the row (and its saved
     * progress/bookmarks) because the file is already gone; it does not touch any file on disk.
     */
    fun removeMissingVideo(uri: String) {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.removeVideoFromLibrary(uri)
        }
    }

    fun resetVideoProgress(uri: String) {
        viewModelScope.launch(dispatcher + errorHandler) {
            mediaRepository.savePlaybackPosition(uri, 0L, isCompleted = false)
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFolderSelected(folder: String?) {
        _selectedFolder.value = folder
    }

    fun onSortOrderChanged(newSortOrder: LibrarySortOrder) {
        _chosenSort.value = newSortOrder
    }

    fun onRefresh(onTriggerScan: (() -> Unit)? = null) {
        viewModelScope.launch(dispatcher + errorHandler) {
            _isRefreshing.value = true
            try {
                onTriggerScan?.invoke()
            } catch (t: Throwable) {
                _isRefreshing.value = false
                throw t
            }
            if (scanActivity == null) {
                _isRefreshing.value = false
            }
        }
    }

    private fun List<DomainMediaItem>.summarizeFolders(): Map<String, FolderSummary> =
        groupBy { it.folderPath }.mapValues { (_, items) ->
            // The newest addition that can still be read stands for the folder; a missing file has no frame to show.
            val cover = items.filterNot { it.isMissing }.maxByOrNull { it.dateAddedSec }
            FolderSummary(
                videoCount = items.size,
                totalBytes = items.sumOf { it.sizeBytes },
                totalDurationMs = items.sumOf { it.durationMs },
                coverUri = cover?.uri,
                coverDateModifiedSec = cover?.dateModifiedSec ?: 0L
            )
        }

    private fun DomainMediaItem.toCardUiModel(): VideoCardUiModel {
        val progress = MediaFormatters.calculateProgressFraction(playbackPositionMs, durationMs)
        val folderDisplay = friendlyFolderName(folderPath)

        return VideoCardUiModel(
            uri = uri,
            title = MediaFormatters.cleanTitle(title),
            durationFormatted = MediaFormatters.formatDuration(durationMs),
            fileSizeFormatted = MediaFormatters.formatFileSize(sizeBytes),
            folderName = folderDisplay,
            progressFraction = progress,
            isCompleted = isCompleted,
            isMissing = isMissing,
            rawItem = this
        )
    }
}
