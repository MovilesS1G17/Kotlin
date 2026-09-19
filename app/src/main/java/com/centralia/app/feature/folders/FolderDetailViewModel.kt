package com.centralia.app.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.FolderRepositoryException
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.library.LibrarySourceFilter
import com.centralia.app.feature.library.LoadState
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `enum FolderSort`. */
enum class FolderSort(val title: String) {
    DATE_SAVED("Date Saved"),
    ALPHABETICAL("Alphabetical")
}

/**
 * `@Observable final class FolderDetailViewModel` — Screen 5.
 *
 * The SwiftUI screen is handed the whole `LibraryFolder` because it is pushed
 * from a list that already has it. A Compose Navigation route can only carry the
 * id, so the folder is resolved during [load]; that also means the screen shows
 * live data after the process is restored from the back stack.
 */
class FolderDetailViewModel(
    private val folderID: UUID,
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val folder: LibraryFolder? = null,
        val videos: List<VideoItem> = emptyList(),
        val folders: List<LibraryFolder> = emptyList(),
        val recentlyDeletedVideo: VideoItem? = null,
        val failureMessage: String? = null,
        val query: String = "",
        val selectedSourceFilter: LibrarySourceFilter = LibrarySourceFilter.All,
        val selectedTags: Set<String> = emptySet(),
        val sort: FolderSort = FolderSort.DATE_SAVED
    ) {
        val availableTags: List<String>
            get() = videos.flatMap { it.tags }
                .distinct()
                .sortedWith(String.CASE_INSENSITIVE_ORDER)

        val filteredVideos: List<VideoItem>
            get() {
                val trimmedQuery = query.trim()

                val filtered = videos.filter { video ->
                    selectedSourceFilter.includes(video) &&
                        selectedTags.all { selectedTag ->
                            video.tags.any { it.equals(selectedTag, ignoreCase = true) }
                        } &&
                        (trimmedQuery.isEmpty() || matches(video, trimmedQuery))
                }

                return when (sort) {
                    FolderSort.DATE_SAVED -> filtered.sortedByDescending { it.savedAt }
                    FolderSort.ALPHABETICAL -> filtered.sortedWith(
                        compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle }
                    )
                }
            }

        val hasActiveFilters: Boolean
            get() = query.isNotEmpty() ||
                selectedSourceFilter != LibrarySourceFilter.All ||
                selectedTags.isNotEmpty()

        private fun matches(video: VideoItem, query: String): Boolean =
            video.displayTitle.contains(query, ignoreCase = true) ||
                video.creator.contains(query, ignoreCase = true) ||
                video.tags.any { it.contains(query, ignoreCase = true) }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        val current = _uiState.value.state
        if (current != LoadState.Idle && !current.isFailure) return

        viewModelScope.launch {
            _uiState.update { it.copy(state = LoadState.Loading) }
            try {
                coroutineScope {
                    val loadedVideos = async { videoRepository.videos() }
                    val loadedFolders = async { folderRepository.folders() }
                    val allVideos = loadedVideos.await()
                    val folders = loadedFolders.await()
                    val folder = folders.firstOrNull { it.id == folderID }

                    if (folder == null) {
                        _uiState.update {
                            it.copy(
                                state = LoadState.Failed(
                                    FolderRepositoryException.FolderNotFound.message
                                        ?: "This folder is no longer available."
                                )
                            )
                        }
                        return@coroutineScope
                    }

                    _uiState.update {
                        it.copy(
                            folder = folder,
                            videos = allVideos.filter { video -> video.folderID == folderID },
                            folders = folders,
                            state = LoadState.Loaded
                        ).reconcileSelectedTags()
                    }
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(state = LoadState.Failed(error.localizedMessage ?: "Please try again."))
                }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(state = LoadState.Idle) }
        load()
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun onSourceFilterChange(filter: LibrarySourceFilter) =
        _uiState.update { it.copy(selectedSourceFilter = filter) }

    fun onSortChange(sort: FolderSort) = _uiState.update { it.copy(sort = sort) }

    fun toggleTag(tag: String) = _uiState.update { current ->
        current.copy(
            selectedTags = if (current.selectedTags.contains(tag)) {
                current.selectedTags - tag
            } else {
                current.selectedTags + tag
            }
        )
    }

    fun resetTags() = _uiState.update { it.copy(selectedTags = emptySet()) }

    fun clearFilters() = _uiState.update {
        it.copy(
            query = "",
            selectedSourceFilter = LibrarySourceFilter.All,
            selectedTags = emptySet()
        )
    }

    /** Moving a video out of this folder removes it from the list. */
    fun move(video: VideoItem, destinationFolderID: UUID?, onChanged: () -> Unit) {
        viewModelScope.launch {
            try {
                videoRepository.moveVideo(video.id, destinationFolderID)
                _uiState.update {
                    it.copy(videos = it.videos.filterNot { item -> item.id == video.id })
                        .reconcileSelectedTags()
                }
                onChanged()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            }
        }
    }

    fun delete(video: VideoItem, onChanged: () -> Unit) {
        viewModelScope.launch {
            try {
                videoRepository.deleteVideo(video.id)
                _uiState.update {
                    it.copy(
                        videos = it.videos.filterNot { item -> item.id == video.id },
                        recentlyDeletedVideo = video
                    ).reconcileSelectedTags()
                }
                onChanged()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            }
        }
    }

    fun undoDelete(onChanged: () -> Unit) {
        val video = _uiState.value.recentlyDeletedVideo ?: return

        viewModelScope.launch {
            try {
                videoRepository.restoreVideo(video)
                _uiState.update {
                    it.copy(
                        videos = it.videos + video,
                        recentlyDeletedVideo = null
                    ).reconcileSelectedTags()
                }
                onChanged()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            }
        }
    }

    fun dismissUndo() = _uiState.update { it.copy(recentlyDeletedVideo = null) }

    fun registerDeleted(video: VideoItem) {
        _uiState.update {
            it.copy(
                videos = it.videos.filterNot { item -> item.id == video.id },
                recentlyDeletedVideo = video
            ).reconcileSelectedTags()
        }
    }

    fun renameFolder(name: String, onRenamed: () -> Unit) {
        viewModelScope.launch {
            try {
                val folder = folderRepository.renameFolder(folderID, name)
                _uiState.update { it.copy(folder = folder, failureMessage = null) }
                onRenamed()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            }
        }
    }

    fun deleteFolder(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                folderRepository.deleteFolder(folderID)
                onDeleted()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            }
        }
    }

    fun dismissFailure() = _uiState.update { it.copy(failureMessage = null) }

    /** `reconcileSelectedTags()` — drops selections no video in the folder carries. */
    private fun UiState.reconcileSelectedTags(): UiState {
        val available = availableTags.toSet()
        return copy(selectedTags = selectedTags.intersect(available))
    }
}
