package com.centralia.app.feature.saveconfirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `@Observable final class SaveConfirmationViewModel` — Screen 7. */
class SaveConfirmationViewModel(
    initialVideo: VideoItem,
    private val videoRepository: VideoItemRepository
) : ViewModel() {

    data class UiState(
        val video: VideoItem,
        val isSavingNote: Boolean = false,
        val failureMessage: String? = null
    ) {
        /** Offers "Add Note" only when the saved short has none yet. */
        val shouldShowAddNote: Boolean
            get() = video.note?.trim().isNullOrEmpty()
    }

    private val _uiState = MutableStateFlow(UiState(video = initialVideo))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun saveNote(note: String, onSaved: () -> Unit) {
        val trimmedNote = note.trim()
        if (trimmedNote.isEmpty() || _uiState.value.isSavingNote) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingNote = true, failureMessage = null) }
            try {
                videoRepository.updateNote(_uiState.value.video.id, trimmedNote)
                _uiState.update { it.copy(video = it.video.copy(note = trimmedNote)) }
                onSaved()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSavingNote = false) }
            }
        }
    }

    fun dismissFailure() = _uiState.update { it.copy(failureMessage = null) }
}
