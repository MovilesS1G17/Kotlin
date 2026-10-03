package com.centralia.app.feature.smartorg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.imports.ImportedVideoMetadata
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.library.LoadState
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** `struct FolderSuggestion` — identified by the video it is about. */
data class FolderSuggestion(
    val video: VideoItem,
    val folder: LibraryFolder
) {
    val id: UUID get() = video.id
}

/** `@Observable final class SmartOrganizationViewModel` — Screen 8. */
class SmartOrganizationViewModel(
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository,
    private val suggestionPipeline: VideoImportPipeline
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val suggestions: List<FolderSuggestion> = emptyList(),
        val folders: List<LibraryFolder> = emptyList(),
        val unorganizedCount: Int = 0,
        val isMutating: Boolean = false,
        val failureMessage: String? = null,
        val selectedSuggestionID: UUID? = null
    ) {
        val selectedSuggestion: FolderSuggestion?
            get() = suggestions.firstOrNull { it.id == selectedSuggestionID }
                ?: suggestions.firstOrNull()
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
                    val videos = loadedVideos.await()
                    val availableFolders = loadedFolders.await()
                    val unorganizedVideos = videos.filter { it.folderID == null }

                    val suggestions = makeSuggestions(unorganizedVideos, availableFolders)

                    _uiState.update {
                        it.copy(
                            folders = availableFolders,
                            unorganizedCount = unorganizedVideos.size,
                            suggestions = suggestions,
                            selectedSuggestionID = suggestions.firstOrNull()?.id,
                            state = LoadState.Loaded
                        )
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

    fun onSelectedSuggestionChange(id: UUID?) =
        _uiState.update { it.copy(selectedSuggestionID = id) }

    /** `accept(_:)` — files the short into the suggested folder. */
    fun accept(suggestion: FolderSuggestion, onChanged: () -> Unit) =
        move(suggestion, suggestion.folder, onChanged)

    fun move(suggestion: FolderSuggestion, folder: LibraryFolder, onChanged: () -> Unit) {
        if (_uiState.value.isMutating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, failureMessage = null) }
            try {
                videoRepository.moveVideo(suggestion.video.id, folder.id)
                _uiState.update { current ->
                    current
                        .copy(unorganizedCount = (current.unorganizedCount - 1).coerceAtLeast(0))
                        .removingSuggestion(suggestion.id)
                }
                onChanged()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isMutating = false) }
            }
        }
    }

    /** `skip(_:)` — drops the card without moving anything. */
    fun skip(suggestion: FolderSuggestion) {
        if (_uiState.value.isMutating) return
        _uiState.update { it.removingSuggestion(suggestion.id) }
    }

    fun apply(updatedVideo: VideoItem) {
        _uiState.update { current ->
            val index = current.suggestions.indexOfFirst { it.id == updatedVideo.id }
            if (index < 0) return@update current

            if (updatedVideo.folderID != null) {
                current
                    .copy(unorganizedCount = (current.unorganizedCount - 1).coerceAtLeast(0))
                    .removingSuggestion(updatedVideo.id)
            } else {
                current.copy(
                    suggestions = current.suggestions.mapIndexed { itemIndex, suggestion ->
                        if (itemIndex == index) {
                            suggestion.copy(video = updatedVideo)
                        } else {
                            suggestion
                        }
                    }
                )
            }
        }
    }

    fun registerDeleted(video: VideoItem) {
        if (video.folderID != null) return
        _uiState.update {
            it.copy(unorganizedCount = (it.unorganizedCount - 1).coerceAtLeast(0))
                .removingSuggestion(video.id)
        }
    }

    fun dismissFailure() = _uiState.update { it.copy(failureMessage = null) }

    /**
     * `makeSuggestions(for:folders:)` — asks the pipeline for a folder name per
     * unorganized short and keeps the ones that match an existing folder. The
     * loop is sequential, as in Swift, and bails out if the scope is cancelled.
     */
    private suspend fun makeSuggestions(
        videos: List<VideoItem>,
        folders: List<LibraryFolder>
    ): List<FolderSuggestion> {
        val values = mutableListOf<FolderSuggestion>()

        for (video in videos) {
            if (!currentCoroutineContext().isActive) break

            val metadata = ImportedVideoMetadata(
                sourceURL = video.sourceURL,
                platform = video.platform,
                creator = video.creator,
                durationSeconds = video.durationSeconds,
                sourceCaption = video.sourceCaption,
                transcript = video.transcript,
                extractedOnScreenText = video.extractedOnScreenText,
                generatedSummary = video.generatedSummary
            )

            val folderName = runCatching {
                suggestionPipeline.suggestFolder(metadata, video.tags)
            }.getOrNull() ?: continue

            val folder = folders.firstOrNull { it.name.equals(folderName, ignoreCase = true) }
                ?: continue

            values += FolderSuggestion(video = video, folder = folder)
        }

        return values
    }

    /**
     * `removeSuggestion(id:)` — after removing a card the selection moves to the
     * one that slid into its place, or to the new last card.
     */
    private fun UiState.removingSuggestion(id: UUID): UiState {
        val removedIndex = suggestions.indexOfFirst { it.id == id }
        if (removedIndex < 0) return this

        val remaining = suggestions.filterNot { it.id == id }
        if (remaining.isEmpty()) {
            return copy(suggestions = remaining, selectedSuggestionID = null)
        }

        val nextIndex = minOf(removedIndex, remaining.size - 1)
        return copy(suggestions = remaining, selectedSuggestionID = remaining[nextIndex].id)
    }
}
