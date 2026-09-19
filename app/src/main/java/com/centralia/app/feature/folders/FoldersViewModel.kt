package com.centralia.app.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.FolderRepositoryException
import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.library.LoadState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `@Observable final class FoldersViewModel` — Screen 4. */
class FoldersViewModel(
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val folders: List<LibraryFolder> = emptyList(),
        val videos: List<VideoItem> = emptyList(),
        val failureMessage: String? = null,
        val query: String = ""
    ) {
        val filteredFolders: List<LibraryFolder>
            get() {
                val trimmedQuery = query.trim()
                if (trimmedQuery.isEmpty()) return folders
                return folders.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
            }

        val unorganizedCount: Int
            get() = videos.count { it.folderID == null }

        fun itemCount(folder: LibraryFolder): Int = videos.count { it.folderID == folder.id }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        val current = _uiState.value.state
        if (current != LoadState.Idle && !current.isFailure) return

        viewModelScope.launch {
            _uiState.update { it.copy(state = LoadState.Loading) }
            try {
                // `async let` — both loads run concurrently.
                coroutineScope {
                    val loadedVideos = async { videoRepository.videos() }
                    val loadedFolders = async { folderRepository.folders() }
                    val videos = loadedVideos.await()
                    val folders = loadedFolders.await()
                    _uiState.update {
                        it.copy(videos = videos, folders = folders, state = LoadState.Loaded)
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

    /** `applyFolderChange()` — a full reload after a child screen edited a folder. */
    fun applyFolderChange() = retry()

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    /**
     * `createFolder(named:symbol:)` — returns the folder so the sheet only closes
     * on success, exactly as the Swift `guard await … != nil else { return }` does.
     */
    fun createFolder(
        name: String,
        symbol: FolderSymbol,
        onCreated: (LibraryFolder) -> Unit
    ) {
        if (name.trim().isEmpty()) {
            _uiState.update {
                it.copy(failureMessage = FolderRepositoryException.EmptyName.message)
            }
            return
        }

        viewModelScope.launch {
            try {
                val folder = folderRepository.createFolder(name, symbol.rawValue)
                _uiState.update { current ->
                    current.copy(
                        folders = (current.folders + folder)
                            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                        failureMessage = null
                    )
                }
                onCreated(folder)
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            }
        }
    }

    fun dismissFailure() = _uiState.update { it.copy(failureMessage = null) }
}
