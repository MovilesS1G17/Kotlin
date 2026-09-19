package com.centralia.app.feature.save

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.saveconfirmation.SaveConfirmationScreen
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaBorderedTextField
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaTextArea
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.CapsuleShape
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.FlowLayout
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.semibold

/**
 * `struct SaveVideoView` — Screen 3.
 *
 * iOS presents this as a sheet from the tab bar's plus button and swaps its body
 * for the confirmation once a short is saved. Here it is a full-screen
 * destination, which is how Android presents a multi-field form, and the same
 * swap to [SaveConfirmationScreen] happens in place.
 */
@Composable
fun SaveVideoScreen(
    pipeline: VideoImportPipeline,
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    onVideoSaved: () -> Unit,
    onClose: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "saveVideo") {
        SaveVideoViewModel(pipeline, videoRepository, folderRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showsFolderPicker by remember { mutableStateOf(false) }
    var showsTagEditor by remember { mutableStateOf(false) }
    var showsDiscardConfirmation by remember { mutableStateOf(false) }
    var savedVideo by remember { mutableStateOf<VideoItem?>(null) }

    LaunchedEffect(Unit) { viewModel.loadFolders() }

    // `.task(id: viewModel.urlText) { await viewModel.analyzeURL() }` — the key
    // change cancels the in-flight pipeline, which is the debounce.
    LaunchedEffect(state.urlText) { viewModel.analyzeURL() }

    val close = {
        if (state.isDraftDirty && savedVideo == null) {
            showsDiscardConfirmation = true
        } else {
            onClose()
        }
    }

    // `.interactiveDismissDisabled(viewModel.isDraftDirty && savedVideo == nil)`
    BackHandler(enabled = true) { close() }

    val currentSavedVideo = savedVideo

    if (currentSavedVideo != null) {
        SaveConfirmationScreen(
            video = currentSavedVideo,
            folderName = state.folders
                .firstOrNull { it.id == currentSavedVideo.folderID }
                ?.name,
            videoRepository = videoRepository,
            onVideoUpdated = onVideoSaved,
            onOpenVideo = onOpenVideo,
            onDone = onClose,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CentraliaColors.Canvas)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        CenteredContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
                    .padding(top = Spacing.medium, bottom = Spacing.xLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                // header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Save Short Video",
                        style = CentraliaType.display,
                        color = CentraliaColors.Ink,
                        modifier = Modifier.weight(1f)
                    )

                    CentraliaTextButton(title = "Close", onClick = close)
                }

                // urlSection
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Paste a TikTok, Reel, or Short link",
                        style = CentraliaType.title3.bold(),
                        color = CentraliaColors.Ink
                    )

                    CentraliaBorderedTextField(
                        placeholder = "https://www.youtube.com/shorts/…",
                        value = state.urlText,
                        onValueChange = viewModel::onUrlTextChange,
                        minHeight = 66.dp,
                        cornerRadius = 18.dp,
                        borderWidth = 2.dp,
                        borderColor = CentraliaColors.Ink,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                }

                // importStatus
                if (state.urlText.trim().isNotEmpty()) {
                    when (val analysis = state.analysisState) {
                        SaveVideoViewModel.AnalysisState.Idle -> Unit

                        is SaveVideoViewModel.AnalysisState.Processing -> SaveImportStatusCard(
                            metadata = state.metadata,
                            stage = analysis.stage,
                            errorMessage = null
                        )

                        SaveVideoViewModel.AnalysisState.Ready -> SaveImportStatusCard(
                            metadata = state.metadata,
                            stage = null,
                            errorMessage = null
                        )

                        is SaveVideoViewModel.AnalysisState.Failed -> SaveImportStatusCard(
                            metadata = null,
                            stage = null,
                            errorMessage = analysis.message
                        )
                    }
                }

                // folderSection
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Folder",
                        style = CentraliaType.headline,
                        color = CentraliaColors.Ink
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 58.dp)
                            .centraliaPressable { showsFolderPicker = true }
                            .background(CentraliaColors.Surface, RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = if (state.folderSelectionError == null) {
                                    CentraliaColors.Divider
                                } else {
                                    CentraliaColors.Error
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = state.selectedFolderName ?: "Choose a folder",
                                style = CentraliaType.body,
                                color = if (state.selectedFolderName == null) {
                                    CentraliaColors.SecondaryText
                                } else {
                                    CentraliaColors.Ink
                                }
                            )

                            val suggestion = state.suggestedFolderName
                            if (suggestion != null && state.selectedFolderName == suggestion) {
                                Text(
                                    text = "Suggested",
                                    style = CentraliaType.caption,
                                    color = CentraliaColors.SecondaryText
                                )
                            }
                        }

                        Icon(
                            imageVector = CentraliaIcons.ChevronRight,
                            contentDescription = null,
                            tint = CentraliaColors.SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    state.folderSelectionError?.let { message ->
                        Text(
                            text = message,
                            style = CentraliaType.caption,
                            color = CentraliaColors.Error
                        )
                    }
                }

                // tagsSection
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Tags",
                        style = CentraliaType.headline,
                        color = CentraliaColors.Ink
                    )

                    FlowLayout(spacing = Spacing.small, modifier = Modifier.fillMaxWidth()) {
                        state.selectedTags.forEach { tag ->
                            SaveTagChip(tag = tag, onRemove = { viewModel.removeTag(tag) })
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .centraliaPressable { showsTagEditor = true }
                                .background(CentraliaColors.SoftSurface, CapsuleShape)
                                .border(1.dp, CentraliaColors.Divider, CapsuleShape)
                                .padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = CentraliaIcons.Add,
                                contentDescription = null,
                                tint = CentraliaColors.Ink,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                text = "Add tag",
                                style = CentraliaType.subheadline.semibold(),
                                color = CentraliaColors.Ink
                            )
                        }
                    }
                }

                // noteSection
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Personal note",
                        style = CentraliaType.headline,
                        color = CentraliaColors.Ink
                    )

                    CentraliaTextArea(
                        placeholder = "Why is this short worth remembering?",
                        value = state.note,
                        onValueChange = viewModel::onNoteChange,
                        minHeight = 124.dp,
                        contentDescription = "Personal note"
                    )
                }

                // saveActions
                Column(
                    modifier = Modifier.padding(top = Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    // `.opacity(viewModel.canSave ? 1 : 0.45)`
                    CentraliaPrimaryButton(
                        title = "Save Video",
                        isLoading = state.isSaving,
                        isDisabled = !state.canSave,
                        minHeight = 58,
                        cornerRadius = 16.dp,
                        modifier = if (state.canSave) Modifier else Modifier.alpha(0.45f),
                        onClick = {
                            viewModel.save(organized = true) { video ->
                                onVideoSaved()
                                savedVideo = video
                            }
                        }
                    )

                    CentraliaTextButton(
                        title = "Save without organizing",
                        isDisabled = !state.canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp),
                        onClick = {
                            viewModel.save(organized = false) { video ->
                                onVideoSaved()
                                savedVideo = video
                            }
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.navigationBarsPadding())
    }

    if (showsFolderPicker) {
        SaveFolderPickerSheet(
            folders = state.folders,
            selectedFolderID = state.selectedFolderID,
            folderFailureMessage = state.folderFailureMessage,
            onSelectFolder = viewModel::onFolderSelected,
            onCreateFolder = { name, symbol ->
                viewModel.createFolder(name, symbol) { showsFolderPicker = false }
            },
            onDismiss = {
                showsFolderPicker = false
                viewModel.dismissFolderFailure()
            }
        )
    }

    if (showsTagEditor) {
        SaveTagEditorSheet(
            availableTagSuggestions = state.availableTagSuggestions,
            onAddTag = viewModel::addTag,
            onDismiss = { showsTagEditor = false }
        )
    }

    if (showsDiscardConfirmation) {
        CentraliaAlert(
            title = "Discard this draft?",
            message = "The pasted link and your organization choices will be lost.",
            confirmTitle = "Discard Draft",
            dismissTitle = "Keep Editing",
            isDestructive = true,
            onConfirm = {
                showsDiscardConfirmation = false
                onClose()
            },
            onDismiss = { showsDiscardConfirmation = false }
        )
    }

    state.saveFailureMessage?.let { message ->
        CentraliaAlert(
            title = "Couldn’t save video",
            message = message,
            confirmTitle = "Try Again",
            dismissTitle = "Cancel",
            onConfirm = {
                viewModel.dismissSaveFailure()
                viewModel.save(organized = true) { video ->
                    onVideoSaved()
                    savedVideo = video
                }
            },
            onDismiss = viewModel::dismissSaveFailure
        )
    }
}
