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
import com.centralia.app.domain.search.SearchHistoryRepository
import com.centralia.app.feature.library.LoadState
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `enum SearchFolderFilter`. */
sealed interface SearchFolderFilter {
    data object All : SearchFolderFilter
    data object Unorganized : SearchFolderFilter
    data class InFolder(val folderID: UUID) : SearchFolderFilter
}

/** `@Observable final class SearchViewModel` — Screen 2, Global Search. */
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
        val selectedTags: Set<String> = emptySet()
    ) {
        /**
         * `filteredVideos` — every active facet must pass, then each whitespace
         * separated term must appear in the title, creator, folder name or a tag.
         */
        val filteredVideos: List<VideoItem>
            get() {
                val terms = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

                return videos.filter { video ->
                    if (selectedPlatform != null && video.platform != selectedPlatform) {
                        return@filter false
                    }
                    if (selectedCreator != null && video.creator != selectedCreator) {
                        return@filter false
                    }

                    when (val folderFilter = selectedFolder) {
                        SearchFolderFilter.All -> Unit
                        SearchFolderFilter.Unorganized ->
                            if (video.folderID != null) return@filter false
                        is SearchFolderFilter.InFolder ->
                            if (video.folderID != folderFilter.folderID) return@filter false
                    }

                    if (!TextMatch.hasAllTags(video.tags, selectedTags)) return@filter false

                    if (terms.isEmpty()) return@filter true

                    val searchableValues = listOf(
                        video.displayTitle,
                        video.creator,
                        folderName(video.folderID) ?: ""
                    ) + video.tags

                    // Case- and accent-insensitive, like iOS (`TextMatch`).
                    terms.all { term ->
                        searchableValues.any { TextMatch.contains(it, term) }
                    }
                }
            }

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
}
