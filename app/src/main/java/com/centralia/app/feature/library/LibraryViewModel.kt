package com.centralia.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * `enum LibrarySourceFilter`. Swift's enum with an associated value becomes a
 * sealed interface; `allCases` is spelled out because the Swift version
 * overrides the synthesised one.
 */
sealed interface LibrarySourceFilter {
    data object All : LibrarySourceFilter
    data class OnPlatform(val platform: VideoPlatform) : LibrarySourceFilter

    val id: String
        get() = when (this) {
            All -> "all"
            is OnPlatform -> platform.rawValue
        }

    val displayName: String
        get() = when (this) {
            All -> "All"
            is OnPlatform -> platform.filterName
        }

    fun includes(video: VideoItem): Boolean = when (this) {
        All -> true
        is OnPlatform -> video.platform == platform
    }

    companion object {
        val allCases: List<LibrarySourceFilter> = listOf(
            All,
            OnPlatform(VideoPlatform.TIKTOK),
            OnPlatform(VideoPlatform.INSTAGRAM_REEL),
            OnPlatform(VideoPlatform.YOUTUBE_SHORT)
        )
    }
}

/** The four-case `LoadState` shared by every list screen in the app. */
sealed interface LoadState {
    data object Idle : LoadState
    data object Loading : LoadState
    data object Loaded : LoadState
    data class Failed(val message: String) : LoadState

    val isFailure: Boolean get() = this is Failed
}

/** `@Observable final class LibraryViewModel`. */
class LibraryViewModel(
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val videos: List<VideoItem> = emptyList(),
        val folders: List<LibraryFolder> = emptyList(),
        val recentlyDeletedVideo: VideoItem? = null,
        val selectedFilter: LibrarySourceFilter = LibrarySourceFilter.All
    ) {
        val filteredVideos: List<VideoItem>
            get() = videos.filter { selectedFilter.includes(it) }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** `.task { await viewModel.load() }` — only loads once unless it failed. */
    fun load() {
        val current = _uiState.value.state
        if (current != LoadState.Idle && !current.isFailure) return

        viewModelScope.launch {
            _uiState.update { it.copy(state = LoadState.Loading) }
            try {
                val videos = videoRepository.videos()
                val folders = folderRepository.folders()
                _uiState.update {
                    it.copy(videos = videos, folders = folders, state = LoadState.Loaded)
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(state = LoadState.Failed(error.localizedMessage ?: "Please try again."))
                }
            }
        }
    }

    /** `retry()` — also the reload path when another tab changed the library. */
    fun retry() {
        _uiState.update { it.copy(state = LoadState.Idle) }
        load()
    }

    fun onFilterChange(filter: LibrarySourceFilter) =
        _uiState.update { it.copy(selectedFilter = filter) }

    fun delete(video: VideoItem) {
        viewModelScope.launch {
            try {
                videoRepository.deleteVideo(video.id)
                _uiState.update {
                    it.copy(
                        videos = it.videos.filterNot { item -> item.id == video.id },
                        recentlyDeletedVideo = video
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.localizedMessage ?: "")) }
            }
        }
    }

    fun undoDelete() {
        val video = _uiState.value.recentlyDeletedVideo ?: return

        viewModelScope.launch {
            try {
                videoRepository.restoreVideo(video)
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

    fun dismissUndo() = _uiState.update { it.copy(recentlyDeletedVideo = null) }

    /** `apply(_:)` — folds an edit made on the detail screen back into the list. */
    fun apply(video: VideoItem) {
        _uiState.update { current ->
            if (current.videos.none { it.id == video.id }) {
                current
            } else {
                current.copy(
                    videos = current.videos.map { if (it.id == video.id) video else it }
                )
            }
        }
    }

    fun registerDeleted(video: VideoItem) {
        _uiState.update {
            it.copy(
                videos = it.videos.filterNot { item -> item.id == video.id },
                recentlyDeletedVideo = video
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

    /** `viewModel.videos.count { $0.folderID == folder.id }`. */
    fun itemCount(folder: LibraryFolder): Int =
        _uiState.value.videos.count { it.folderID == folder.id }
}
