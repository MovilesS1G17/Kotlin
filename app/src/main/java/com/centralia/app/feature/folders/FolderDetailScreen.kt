package com.centralia.app.feature.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.library.DurationBadge
import com.centralia.app.feature.library.LibrarySourceFilter
import com.centralia.app.feature.library.LoadState
import com.centralia.app.feature.library.MoveVideoSheet
import com.centralia.app.feature.library.PlatformBadge
import com.centralia.app.feature.library.SourceFilterRow
import com.centralia.app.feature.library.VideoOverflowMenu
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaBorderedTextField
import com.centralia.app.ui.components.CentraliaChip
import com.centralia.app.ui.components.CentraliaFilterMenu
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.CentraliaSearchField
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.LibraryUndoToast
import com.centralia.app.ui.components.MenuActionItem
import com.centralia.app.ui.components.MenuSelectionItem
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.SheetErrorText
import com.centralia.app.ui.components.SheetListRow
import com.centralia.app.ui.components.SheetSectionHeader
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.medium
import com.centralia.app.ui.theme.semibold
import java.util.UUID

/** `struct FolderDetailView` — Screen 5. */
@Composable
fun FolderDetailScreen(
    folderID: UUID,
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    onBack: () -> Unit,
    onFolderChanged: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "folderDetail-$folderID") {
        FolderDetailViewModel(folderID, videoRepository, folderRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var movingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var pendingDeletion by remember { mutableStateOf<VideoItem?>(null) }
    var showsRenameFolder by remember { mutableStateOf(false) }
    var showsTagFilter by remember { mutableStateOf(false) }
    var showsDeleteFolderConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    val folderName = state.folder?.name ?: ""

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Spacing.small, bottom = Spacing.xLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            item(key = "header") {
                CenteredContent {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.medium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .centraliaPressable(onClickLabel = "Back", onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CentraliaIcons.Back,
                                contentDescription = null,
                                tint = CentraliaColors.Ink
                            )
                        }

                        Text(
                            text = folderName,
                            style = CentraliaType.display,
                            color = CentraliaColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        FolderActionsMenu(
                            onRename = { showsRenameFolder = true },
                            onDelete = { showsDeleteFolderConfirmation = true }
                        )
                    }
                }
            }

            item(key = "search") {
                CenteredContent {
                    CentraliaSearchField(
                        placeholder = "Search in $folderName",
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        minHeight = 52.dp,
                        cornerRadius = 16.dp,
                        modifier = Modifier.padding(horizontal = Spacing.medium)
                    )
                }
            }

            item(key = "filters") {
                SourceFilterRow(
                    selectedFilter = state.selectedSourceFilter,
                    onSelect = viewModel::onSourceFilterChange,
                    trailingContent = {
                        val tagCount = state.selectedTags.size
                        CentraliaChip(
                            title = if (tagCount == 0) "Tags" else "Tags ($tagCount)",
                            isSelected = tagCount > 0,
                            contentDescription = if (tagCount == 0) {
                                "Filter by tags"
                            } else {
                                "Filter by tags, $tagCount selected"
                            },
                            trailingIcon = CentraliaIcons.ChevronDown,
                            onClick = { showsTagFilter = true }
                        )
                    }
                )
            }

            when (val loadState = state.state) {
                LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                    CentraliaLoadingState(label = "Loading folder…", minHeight = 320.dp)
                }

                is LoadState.Failed -> item(key = "failed") {
                    CenteredContent {
                        ContentUnavailable(
                            title = "Folder unavailable",
                            icon = CentraliaIcons.Warning,
                            description = loadState.message,
                            minHeight = 320.dp,
                            actions = {
                                ProminentAction(title = "Try Again", onClick = viewModel::retry)
                            }
                        )
                    }
                }

                LoadState.Loaded -> {
                    item(key = "resultControls") {
                        CenteredContent {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.medium)
                            ) {
                                val count = state.filteredVideos.size
                                Text(
                                    text = if (count == 1) {
                                        "1 saved short"
                                    } else {
                                        "$count saved shorts"
                                    },
                                    style = CentraliaType.headline,
                                    color = CentraliaColors.SecondaryText,
                                    modifier = Modifier.weight(1f)
                                )

                                CentraliaFilterMenu(
                                    title = "Sort",
                                    isActive = false,
                                    contentDescription = "Sort"
                                ) { dismiss ->
                                    FolderSort.entries.forEach { option ->
                                        MenuSelectionItem(
                                            title = option.title,
                                            isSelected = state.sort == option
                                        ) {
                                            viewModel.onSortChange(option)
                                            dismiss()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.filteredVideos.isEmpty()) {
                        item(key = "empty") {
                            CenteredContent {
                                ContentUnavailable(
                                    title = "No matching shorts",
                                    icon = CentraliaIcons.VideoStack,
                                    description = if (state.videos.isEmpty()) {
                                        "Move a saved short here to build this folder."
                                    } else {
                                        "Try another search or filter."
                                    },
                                    minHeight = 260.dp,
                                    actions = if (state.hasActiveFilters) {
                                        {
                                            ProminentAction(title = "Clear Filters") {
                                                viewModel.clearFilters()
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    } else {
                        folderVideoGrid(
                            videos = state.filteredVideos,
                            onOpen = onOpenVideo,
                            onRequestMove = { movingVideo = it },
                            onRequestDelete = { pendingDeletion = it }
                        )
                    }
                }
            }
        }

        if (state.recentlyDeletedVideo != null) {
            LibraryUndoToast(
                modifier = Modifier.align(Alignment.BottomCenter),
                onUndo = { viewModel.undoDelete(onFolderChanged) },
                onDismiss = viewModel::dismissUndo
            )
        }
    }

    movingVideo?.let { video ->
        MoveVideoSheet(
            folders = state.folders,
            currentFolderID = video.folderID,
            onMove = { destination -> viewModel.move(video, destination, onFolderChanged) },
            onDismiss = { movingVideo = null }
        )
    }

    if (showsRenameFolder) {
        FolderRenameSheet(
            initialName = folderName,
            failureMessage = state.failureMessage,
            onSave = { name ->
                viewModel.renameFolder(name) {
                    showsRenameFolder = false
                    onFolderChanged()
                }
            },
            onDismiss = {
                showsRenameFolder = false
                viewModel.dismissFailure()
            }
        )
    }

    if (showsTagFilter) {
        FolderTagsSheet(
            folderName = folderName,
            availableTags = state.availableTags,
            selectedTags = state.selectedTags,
            onToggleTag = viewModel::toggleTag,
            onReset = viewModel::resetTags,
            onDismiss = { showsTagFilter = false }
        )
    }

    pendingDeletion?.let { video ->
        CentraliaAlert(
            title = "Remove this short from your library?",
            message = "“${video.displayTitle}” will be removed from your library.",
            confirmTitle = "Remove",
            dismissTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.delete(video, onFolderChanged)
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }

    if (showsDeleteFolderConfirmation) {
        CentraliaAlert(
            title = "Delete $folderName?",
            message = "Its shorts will remain in Unorganized.",
            confirmTitle = "Delete Folder",
            dismissTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                showsDeleteFolderConfirmation = false
                viewModel.deleteFolder {
                    onFolderChanged()
                    onBack()
                }
            },
            onDismiss = { showsDeleteFolderConfirmation = false }
        )
    }

    // `.alert("Couldn't update folder", isPresented:)` — only while the folder
    // itself loaded; a load failure is shown inline instead.
    if (state.failureMessage != null && !showsRenameFolder && state.folder != null) {
        CentraliaAlert(
            title = "Couldn’t update folder",
            message = state.failureMessage,
            onConfirm = viewModel::dismissFailure
        )
    }
}

/** The folder's overflow menu: rename or delete. */
@Composable
private fun FolderActionsMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(44.dp)
                .centraliaPressable(onClickLabel = "Folder actions") { isExpanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CentraliaIcons.More,
                contentDescription = null,
                tint = CentraliaColors.Ink
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            MenuActionItem(title = "Rename Folder", icon = CentraliaIcons.Pencil) {
                isExpanded = false
                onRename()
            }

            MenuActionItem(
                title = "Delete Folder",
                icon = CentraliaIcons.Delete,
                isDestructive = true
            ) {
                isExpanded = false
                onDelete()
            }
        }
    }
}

/** `videoGrid` — a two-column grid of 270dp-tall folder cards. */
private fun androidx.compose.foundation.lazy.LazyListScope.folderVideoGrid(
    videos: List<VideoItem>,
    onOpen: (VideoItem) -> Unit,
    onRequestMove: (VideoItem) -> Unit,
    onRequestDelete: (VideoItem) -> Unit
) {
    val rows = videos.chunked(2)

    items(
        count = rows.size,
        key = { index -> "folderVideoRow-${rows[index].first().id}" }
    ) { index ->
        val row = rows[index]

        CenteredContent {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                row.forEachIndexed { offset, video ->
                    Box(modifier = Modifier.weight(1f)) {
                        FolderDetailVideoCard(
                            video = video,
                            styleIndex = index * 2 + offset,
                            onOpen = { onOpen(video) },
                            onRequestMove = { onRequestMove(video) },
                            onRequestDelete = { onRequestDelete(video) }
                        )
                    }
                }

                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * `private struct FolderDetailVideoCard` — a single 270dp tile with the platform
 * badge, a centred open control, the duration, the title block, and the menu.
 */
@Composable
private fun FolderDetailVideoCard(
    video: VideoItem,
    styleIndex: Int,
    onOpen: () -> Unit,
    onRequestMove: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = CentraliaColors.folderCardPalette[styleIndex.mod(4)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardColor)
            .centraliaPressable(
                onClickLabel = "Open ${video.displayTitle}",
                onClick = onOpen
            )
    ) {
        PlatformBadge(
            text = video.platform.displayName,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .semanticsHidden()
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp)
                .centraliaPressable(
                    onClickLabel = "Open ${video.displayTitle}",
                    onClick = onOpen
                )
                .background(CentraliaColors.Ink.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CentraliaIcons.Play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        DurationBadge(
            text = video.formattedDuration,
            backgroundAlpha = 0.84f,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .padding(bottom = 61.dp)
                .semanticsHidden()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .padding(end = 28.dp)
        ) {
            Text(
                text = video.displayTitle,
                style = CentraliaType.subheadline.bold(),
                color = CentraliaColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = video.creator,
                style = CentraliaType.caption.medium(),
                color = CentraliaColors.SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        VideoOverflowMenu(
            video = video,
            deleteTitle = "Remove from Library",
            onRequestMove = onRequestMove,
            onRequestDelete = onRequestDelete,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
        )
    }
}

/** `private struct FolderRenameSheet`. */
@Composable
private fun FolderRenameSheet(
    initialName: String,
    failureMessage: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { nameFocusRequester.requestFocus() } }

    CentraliaSheet(
        title = "Rename Folder",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss),
        trailingAction = SheetAction(
            title = "Save",
            isEnabled = name.trim().isNotEmpty(),
            onClick = { onSave(name) }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Rename this collection without changing its saved shorts.",
                style = CentraliaType.body,
                color = CentraliaColors.SecondaryText
            )

            CentraliaBorderedTextField(
                placeholder = "Folder name",
                value = name,
                onValueChange = { name = it },
                capitalization = KeyboardCapitalization.Words,
                focusRequester = nameFocusRequester,
                onSubmit = { onSave(name) }
            )

            failureMessage?.let { SheetErrorText(message = it) }
        }
    }
}

/** `private struct FolderTagsSheet`. */
@Composable
private fun FolderTagsSheet(
    folderName: String,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    CentraliaSheet(
        title = "Filter by Tags",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Reset", onClick = onReset),
        trailingAction = SheetAction(title = "Done", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
        ) {
            SheetSectionHeader(title = "Tags in $folderName")

            if (availableTags.isEmpty()) {
                Text(
                    text = "No tags in this folder yet.",
                    style = CentraliaType.body,
                    color = CentraliaColors.SecondaryText,
                    modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)
                )
            } else {
                availableTags.forEach { tag ->
                    SheetListRow(
                        title = tag,
                        isSelected = selectedTags.contains(tag),
                        onClick = { onToggleTag(tag) }
                    )
                }
            }
        }
    }
}
