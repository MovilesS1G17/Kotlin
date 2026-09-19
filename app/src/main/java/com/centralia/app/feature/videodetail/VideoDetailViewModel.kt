package com.centralia.app.feature.videodetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * `@Observable final class VideoDetailViewModel` — Screen 6.
 *
 * The Swift model is constructed with the `VideoItem` the list already holds and
 * then refreshes it in `load()`. Here only the id can travel along a navigation
 * route, so `video` starts null and the screen shows its loading overlay until
 * the first load resolves — the same overlay the original shows while refreshing.
 */
class VideoDetailViewModel(
    private val videoID: UUID,
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    data class UiState(
        val video: VideoItem? = null,
        val folders: List<LibraryFolder> = emptyList(),
        val availableTags: List<String> = emptyList(),
        val isLoading: Boolean = false,
        val isMutating: Boolean = false,
        val failureMessage: String? = null,
        val isMissing: Boolean = false
    ) {
        val folderName: String?
            get() {
                val folderID = video?.folderID ?: return null
                return folders.firstOrNull { it.id == folderID }?.name
            }

        val folderActionTitle: String
            get() = if (video?.folderID == null) "Choose Folder" else "Change Folder"

        val openActionTitle: String
            get() = when (video?.platform) {
                VideoPlatform.TIKTOK -> "Open in TikTok"
                VideoPlatform.INSTAGRAM_REEL -> "Open in Instagram"
                VideoPlatform.YOUTUBE_SHORT -> "Open in YouTube"
                null -> "Open original"
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failureMessage = null) }
            try {
                coroutineScope {
                    val videosRequest = async { videoRepository.videos() }
                    val foldersRequest = async { folderRepository.folders() }
                    val videos = videosRequest.await()
                    val folders = foldersRequest.await()

                    val currentVideo = videos.firstOrNull { it.id == videoID }
                    _uiState.update {
                        it.copy(
                            folders = folders,
                            video = currentVideo ?: it.video,
                            availableTags = sortedTags(videos.flatMap { video -> video.tags }),
                            isMissing = currentVideo == null && it.video == null
                        )
                    }
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateTags(tags: List<String>, onSaved: () -> Unit) = mutate(onSaved) { video ->
        videoRepository.updateTags(video.id, tags)
        val normalized = normalizedTags(tags)
        _uiState.update {
            it.copy(
                video = it.video?.copy(tags = normalized),
                availableTags = sortedTags(it.availableTags + normalized)
            )
        }
    }

    fun move(folderID: UUID?, onSaved: () -> Unit) = mutate(onSaved) { video ->
        videoRepository.moveVideo(video.id, folderID)
        _uiState.update { it.copy(video = it.video?.copy(folderID = folderID)) }
    }

    fun updateNote(note: String?, onSaved: () -> Unit) = mutate(onSaved) { video ->
        val trimmedNote = note?.trim()?.ifEmpty { null }
        videoRepository.updateNote(video.id, trimmedNote)
        _uiState.update { it.copy(video = it.video?.copy(note = trimmedNote)) }
    }

    fun deleteVideo(onDeleted: (VideoItem) -> Unit) {
        val video = _uiState.value.video ?: return
        if (_uiState.value.isMutating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, failureMessage = null) }
            try {
                videoRepository.deleteVideo(video.id)
                onDeleted(video)
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isMutating = false) }
            }
        }
    }

    fun dismissFailure() = _uiState.update { it.copy(failureMessage = null) }

    /**
     * `mutate(_:)` — one in-flight edit at a time, clearing then reporting the
     * failure message, and only calling back on success.
     */
    private fun mutate(onSuccess: () -> Unit, operation: suspend (VideoItem) -> Unit) {
        val video = _uiState.value.video ?: return
        if (_uiState.value.isMutating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, failureMessage = null) }
            try {
                operation(video)
                onSuccess()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isMutating = false) }
            }
        }
    }

    private fun sortedTags(tags: List<String>): List<String> =
        normalizedTags(tags).sortedWith(String.CASE_INSENSITIVE_ORDER)

    /** Trims, drops blanks, removes case-insensitive duplicates, keeps order. */
    private fun normalizedTags(tags: List<String>): List<String> {
        val normalized = mutableListOf<String>()
        tags.forEach { value ->
            val tag = value.trim()
            if (tag.isEmpty()) return@forEach
            if (normalized.any { it.equals(tag, ignoreCase = true) }) return@forEach
            normalized += tag
        }
        return normalized
    }
}
