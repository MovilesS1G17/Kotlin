package com.centralia.app.feature.save

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.imports.ImportedVideoMetadata
import com.centralia.app.domain.imports.VideoImportException
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.imports.VideoImportStage
import com.centralia.app.domain.library.ContentAnalysisStatus
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `@Observable final class SaveVideoViewModel` — Screen 3. */
class SaveVideoViewModel(
    private val pipeline: VideoImportPipeline,
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    /** `SaveVideoViewModel.AnalysisState`. */
    sealed interface AnalysisState {
        data object Idle : AnalysisState
        data class Processing(val stage: VideoImportStage) : AnalysisState
        data object Ready : AnalysisState
        data class Failed(val message: String) : AnalysisState
    }

    data class UiState(
        val analysisState: AnalysisState = AnalysisState.Idle,
        val metadata: ImportedVideoMetadata? = null,
        val suggestedTags: List<String> = emptyList(),
        val suggestedFolderName: String? = null,
        val folders: List<LibraryFolder> = emptyList(),
        val isSaving: Boolean = false,
        val saveFailureMessage: String? = null,
        val folderFailureMessage: String? = null,
        val folderSelectionError: String? = null,
        val urlText: String = "",
        val selectedFolderID: UUID? = null,
        val selectedTags: List<String> = emptyList(),
        val note: String = ""
    ) {
        val canSave: Boolean
            get() = analysisState == AnalysisState.Ready && metadata != null && !isSaving

        val isDraftDirty: Boolean
            get() = urlText.trim().isNotEmpty() ||
                selectedFolderID != null ||
                selectedTags.isNotEmpty() ||
                note.trim().isNotEmpty()

        val selectedFolderName: String?
            get() {
                val id = selectedFolderID ?: return null
                return folders.firstOrNull { it.id == id }?.name
            }

        val availableTagSuggestions: List<String>
            get() = suggestedTags.filter { suggestion ->
                selectedTags.none { it.equals(suggestion, ignoreCase = true) }
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadFolders() {
        viewModelScope.launch {
            try {
                val folders = folderRepository.folders()
                _uiState.update { it.copy(folders = folders) }
            } catch (error: Exception) {
                _uiState.update { it.copy(folderFailureMessage = error.localizedMessage) }
            }
        }
    }

    fun onUrlTextChange(value: String) = _uiState.update { it.copy(urlText = value) }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun onFolderSelected(folderID: UUID?) = _uiState.update {
        // `didSet` on `selectedFolderID` clears the "pick a folder" error.
        it.copy(
            selectedFolderID = folderID,
            folderSelectionError = if (folderID != null) null else it.folderSelectionError
        )
    }

    /**
     * `analyzeURL(after:)`.
     *
     * The SwiftUI screen drives this with `.task(id: viewModel.urlText)`, which
     * cancels the previous run whenever the text changes. A `LaunchedEffect` keyed
     * on the same text does exactly that, so this stays a plain `suspend` function
     * and lets structured cancellation debounce the pipeline for free.
     */
    suspend fun analyzeURL(debounceMillis: Long = 450) {
        val source = _uiState.value.urlText.trim()

        if (source.isEmpty()) {
            resetAnalysis()
            return
        }

        try {
            if (debounceMillis > 0) delay(debounceMillis)

            // The text may have moved on while the debounce elapsed.
            if (source != _uiState.value.urlText.trim()) return

            _uiState.update {
                it.copy(
                    metadata = null,
                    suggestedTags = emptyList(),
                    suggestedFolderName = null,
                    selectedFolderID = null,
                    selectedTags = emptyList()
                )
            }

            _uiState.update {
                it.copy(analysisState = AnalysisState.Processing(VideoImportStage.DETECTING_PLATFORM))
            }
            val platform = pipeline.detectPlatform(source)
            ensureCurrent(source)

            _uiState.update {
                it.copy(
                    analysisState = AnalysisState.Processing(VideoImportStage.EXTRACTING_METADATA)
                )
            }
            val extractedMetadata = pipeline.extractMetadata(source, platform)
            ensureCurrent(source)
            _uiState.update { it.copy(metadata = extractedMetadata) }

            _uiState.update {
                it.copy(analysisState = AnalysisState.Processing(VideoImportStage.GENERATING_TAGS))
            }
            val generatedTags = pipeline.generateTags(extractedMetadata)
            ensureCurrent(source)
            _uiState.update { it.copy(suggestedTags = generatedTags) }

            _uiState.update {
                it.copy(analysisState = AnalysisState.Processing(VideoImportStage.SUGGESTING_FOLDER))
            }
            val folderName = pipeline.suggestFolder(extractedMetadata, generatedTags)
            ensureCurrent(source)

            _uiState.update { current ->
                current.copy(
                    suggestedFolderName = folderName,
                    // Preselects the suggestion only when a folder of that name exists.
                    selectedFolderID = current.folders
                        .firstOrNull { it.name.equals(folderName ?: "", ignoreCase = true) }
                        ?.id,
                    analysisState = AnalysisState.Ready
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            if (source != _uiState.value.urlText.trim()) return
            _uiState.update {
                it.copy(
                    metadata = null,
                    suggestedTags = emptyList(),
                    suggestedFolderName = null,
                    analysisState = AnalysisState.Failed(
                        error.localizedMessage
                            ?: VideoImportException.UnsupportedSource.message.orEmpty()
                    )
                )
            }
        }
    }

    fun createFolder(
        name: String,
        symbol: FolderSymbol = FolderSymbol.FOLDER,
        onCreated: (LibraryFolder) -> Unit
    ) {
        _uiState.update { it.copy(folderFailureMessage = null) }

        viewModelScope.launch {
            try {
                val folder = folderRepository.createFolder(name, symbol.rawValue)
                _uiState.update { current ->
                    current.copy(
                        folders = (current.folders + folder)
                            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                        selectedFolderID = folder.id,
                        folderSelectionError = null
                    )
                }
                onCreated(folder)
            } catch (error: Exception) {
                _uiState.update { it.copy(folderFailureMessage = error.localizedMessage) }
            }
        }
    }

    fun addTag(value: String) {
        val tag = value.trim()
        if (tag.isEmpty()) return

        _uiState.update { current ->
            if (current.selectedTags.any { it.equals(tag, ignoreCase = true) }) {
                current
            } else {
                current.copy(selectedTags = current.selectedTags + tag)
            }
        }
    }

    fun removeTag(tag: String) = _uiState.update { current ->
        current.copy(
            selectedTags = current.selectedTags.filterNot { it.equals(tag, ignoreCase = true) }
        )
    }

    /**
     * `save(organized:)` — "Save Video" requires a folder, "Save without
     * organizing" deliberately stores the short with none.
     */
    fun save(organized: Boolean, onSaved: (VideoItem) -> Unit) {
        val state = _uiState.value
        val metadata = state.metadata ?: return
        if (!state.canSave) return

        if (organized && state.selectedFolderID == null) {
            _uiState.update { it.copy(folderSelectionError = "Select a folder before saving.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(folderSelectionError = null, isSaving = true, saveFailureMessage = null)
            }

            val trimmedNote = state.note.trim()
            val video = VideoItem(
                id = UUID.randomUUID(),
                sourceURL = metadata.sourceURL,
                platform = metadata.platform,
                creator = metadata.creator,
                durationSeconds = metadata.durationSeconds,
                sourceCaption = metadata.sourceCaption,
                transcript = metadata.transcript,
                extractedOnScreenText = metadata.extractedOnScreenText,
                generatedSummary = metadata.generatedSummary,
                customTitle = null,
                folderID = if (organized) state.selectedFolderID else null,
                tags = state.selectedTags,
                note = trimmedNote.ifEmpty { null },
                savedAt = Instant.now(),
                analysisStatus = ContentAnalysisStatus.COMPLETED
            )

            try {
                videoRepository.saveVideo(video)
                onSaved(video)
            } catch (error: Exception) {
                _uiState.update { it.copy(saveFailureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun dismissSaveFailure() = _uiState.update { it.copy(saveFailureMessage = null) }

    fun dismissFolderFailure() = _uiState.update { it.copy(folderFailureMessage = null) }

    /** `ensureCurrent(_:)` — aborts if the pasted link changed mid-pipeline. */
    private fun ensureCurrent(source: String) {
        if (source != _uiState.value.urlText.trim()) throw CancellationException()
    }

    private fun resetAnalysis() = _uiState.update {
        it.copy(
            analysisState = AnalysisState.Idle,
            metadata = null,
            suggestedTags = emptyList(),
            suggestedFolderName = null,
            selectedFolderID = null,
            selectedTags = emptyList(),
            folderSelectionError = null
        )
    }
}
