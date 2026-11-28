package com.centralia.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.TextMatch
import com.centralia.app.domain.library.UndoMethod
import com.centralia.app.domain.library.VideoDeletion
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.search.SearchFilter
import com.centralia.app.domain.search.SearchFolderFilter
import com.centralia.app.domain.search.SearchHistoryRepository
import com.centralia.app.domain.search.SearchInput
import com.centralia.app.feature.library.LoadState
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import android.os.Trace
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `@Observable final class SearchViewModel` — Screen 2, Global Search. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val videos: List<VideoItem> = emptyList(),
        val folders: List<LibraryFolder> = emptyList(),
        val recentSearches: List<String> = emptyList(),
        val recentlyDeletedVideo: VideoItem? = null,
        /** Shake to undo: the pop-up the backend sent for [recentlyDeletedVideo]. */
        val undoDeletion: VideoDeletion? = null,
        val query: String = "",
        val selectedPlatform: VideoPlatform? = null,
        val selectedCreator: String? = null,
        val selectedFolder: SearchFolderFilter = SearchFolderFilter.All,
        val selectedTags: Set<String> = emptySet(),
        /**
         * The search results. Computed by `SearchFilter` on a background thread
         * (see the pipeline in `init`), never in this getter on the main thread.
         */
        val results: List<VideoItem> = emptyList()
    ) {
        val filteredVideos: List<VideoItem>
            get() = results

        /** What the results depend on; the pipeline recomputes when it changes. */
        val searchInput: SearchInput
            get() = SearchInput(
                videos = videos,
                folders = folders,
                query = query,
                platform = selectedPlatform,
                creator = selectedCreator,
                folder = selectedFolder,
                tags = selectedTags
            )

        val availableCreators: List<String>
            get() = videos.map { it.creator }
                .distinct()
                .sortedWith(String.CASE_INSENSITIVE_ORDER)

        val availableTags: List<String>
            get() = TextMatch.uniqueTags(videos.flatMap { it.tags })

        val hasActiveFilters: Boolean
            get() = selectedPlatform != null ||
                selectedCreator != null ||
                selectedFolder != SearchFolderFilter.All ||
                selectedTags.isNotEmpty()

        fun folderName(folderID: UUID?): String? {
            if (folderID == null) return null
            return folders.firstOrNull { it.id == folderID }?.name
        }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Multithreading (Sprint 3, Alex): a Flow pipeline for search.
        //  - map + distinctUntilChanged: only a change to the query, a filter
        //    or the library starts a new search (not unrelated state changes);
        //  - debounce: while typing, wait 200 ms for the user to pause;
        //  - mapLatest: a new keystroke cancels the search still running;
        //  - flowOn(Dispatchers.Default): the filtering runs on the CPU pool,
        //    off the main thread;
        //  - collect runs in viewModelScope, on Main, where the UI state is set.
        viewModelScope.launch {
            _uiState
                .map { it.searchInput }
                .distinctUntilChanged()
                .debounce { input -> if (input.query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
                .mapLatest { input ->
                    // Sprint 4: a named section, so the Android Studio Profiler
                    // (System Trace) shows how long each search takes, before
                    // and after the SearchIndex micro-optimization.
                    Trace.beginSection("search_filter")
                    try {
                        SearchFilter.apply(input)
                    } finally {
                        Trace.endSection()
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect { results -> _uiState.update { it.copy(results = results) } }
        }
    }

    fun load() {
        val current = _uiState.value.state
        if (current != LoadState.Idle && !current.isFailure) return

        viewModelScope.launch {
            _uiState.update { it.copy(state = LoadState.Loading) }
            try {
                // Multithreading (Sprint 3): the three requests run at the same time.
                val (videos, folders, recentSearches) = coroutineScope {
                    val videos = async { videoRepository.videos() }
                    val folders = async { folderRepository.folders() }
                    val recentSearches = async { searchHistoryRepository.recentSearches() }
                    Triple(videos.await(), folders.await(), recentSearches.await())
                }
                _uiState.update {
                    it.copy(
                        videos = videos,
                        folders = folders,
                        recentSearches = recentSearches,
                        state = LoadState.Loaded
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(state = LoadState.Failed(error.localizedMessage ?: "Please try again."))
                }
            }
        }
    }

    /**
     * Reloads in the background without replacing the content with a spinner.
     * Used when another tab, another screen or another of the user's devices may
     * have changed the library. Falls back to a normal load if nothing is shown yet.
     */
    fun refresh() {
        if (_uiState.value.state != LoadState.Loaded) return retry()

        viewModelScope.launch {
            try {
                // Multithreading (Sprint 3): the three requests run at the same time.
                val (videos, folders, recentSearches) = coroutineScope {
                    val videos = async { videoRepository.videos() }
                    val folders = async { folderRepository.folders() }
                    val recentSearches = async { searchHistoryRepository.recentSearches() }
                    Triple(videos.await(), folders.await(), recentSearches.await())
                }
                _uiState.update {
                    it.copy(videos = videos, folders = folders, recentSearches = recentSearches)
                }
            } catch (_: Exception) {
                // Keep showing what is already on screen.
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(state = LoadState.Idle) }
        load()
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    /** `submitSearch()` — records the query in history on submit, not per keystroke. */
    fun submitSearch() {
        val trimmedQuery = _uiState.value.query.trim()
        if (trimmedQuery.isEmpty()) return

        viewModelScope.launch {
            try {
                searchHistoryRepository.recordSearch(trimmedQuery)
                val recentSearches = searchHistoryRepository.recentSearches()
                _uiState.update { it.copy(recentSearches = recentSearches) }
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.localizedMessage ?: "")) }
            }
        }
    }

    fun selectRecentSearch(search: String) {
        _uiState.update { it.copy(query = search) }
        submitSearch()
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            try {
                searchHistoryRepository.clearSearchHistory()
                _uiState.update { it.copy(recentSearches = emptyList()) }
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.localizedMessage ?: "")) }
            }
        }
    }

    fun onPlatformChange(platform: VideoPlatform?) =
        _uiState.update { it.copy(selectedPlatform = platform) }

    fun onCreatorChange(creator: String?) =
        _uiState.update { it.copy(selectedCreator = creator) }

    fun onFolderFilterChange(filter: SearchFolderFilter) =
        _uiState.update { it.copy(selectedFolder = filter) }

    fun toggleTag(tag: String) = _uiState.update { current ->
        current.copy(
            selectedTags = if (current.selectedTags.contains(tag)) {
                current.selectedTags - tag
            } else {
                current.selectedTags + tag
            }
        )
    }

    fun clearTags() = _uiState.update { it.copy(selectedTags = emptySet()) }

    fun clearFilters() = _uiState.update {
        it.copy(
            selectedPlatform = null,
            selectedCreator = null,
            selectedFolder = SearchFolderFilter.All,
            selectedTags = emptySet()
        )
    }

    fun clearSearch() = _uiState.update {
        it.copy(
            query = "",
            selectedPlatform = null,
            selectedCreator = null,
            selectedFolder = SearchFolderFilter.All,
            selectedTags = emptySet()
        )
    }

    fun delete(video: VideoItem) {
        viewModelScope.launch {
            try {
                val deletion = videoRepository.deleteVideo(video.id)
                _uiState.update {
                    it.copy(
                        videos = it.videos.filterNot { item -> item.id == video.id },
                        recentlyDeletedVideo = video,
                        undoDeletion = deletion
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.localizedMessage ?: "")) }
            }
        }
    }

    fun undoDelete(method: UndoMethod = UndoMethod.BUTTON) {
        val video = _uiState.value.recentlyDeletedVideo ?: return
        // Close the pop-up right away so a second shake can't restore twice.
        _uiState.update { it.copy(recentlyDeletedVideo = null, undoDeletion = null) }

        viewModelScope.launch {
            try {
                videoRepository.restoreVideo(video, method)
                _uiState.update {
                    it.copy(
                        videos = (it.videos + video).sortedByDescending { item -> item.savedAt },
                        recentlyDeletedVideo = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.localizedMessage ?: "")) }
            }
        }
    }

    fun dismissUndo() = _uiState.update { it.copy(recentlyDeletedVideo = null, undoDeletion = null) }

    fun registerDeleted(video: VideoItem, deletion: VideoDeletion = VideoDeletion.local(video.id)) {
        _uiState.update {
            it.copy(
                videos = it.videos.filterNot { item -> item.id == video.id },
                recentlyDeletedVideo = video,
                undoDeletion = deletion
            )
        }
    }

    fun move(video: VideoItem, folderID: UUID?) {
        viewModelScope.launch {
            try {
                videoRepository.moveVideo(video.id, folderID)
                _uiState.update { current ->
                    current.copy(
                        videos = current.videos.map {
                            if (it.id == video.id) it.copy(folderID = folderID) else it
                        }
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.localizedMessage ?: "")) }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
