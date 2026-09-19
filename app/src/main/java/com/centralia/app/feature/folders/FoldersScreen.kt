package com.centralia.app.feature.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.app.LibraryRevisions
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.library.LoadState
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaBorderedTextField
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.CentraliaSearchField
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.SheetErrorText
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.folderSymbolIcon
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.semibold

/** `struct FoldersView` — Screen 4. */
@Composable
fun FoldersScreen(
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    revisions: LibraryRevisions,
    onOpenFolder: (LibraryFolder) -> Unit,
    onOpenSmartOrganization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "folders") {
        FoldersViewModel(videoRepository, folderRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val foldersRevision by revisions.folders.collectAsStateWithLifecycle()
    var showsNewFolder by remember { mutableStateOf(false) }

    LaunchedEffect(foldersRevision) {
        if (foldersRevision == 0) viewModel.load() else viewModel.retry()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Spacing.medium, bottom = Spacing.xLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
    ) {
        item(key = "header") {
            CenteredContent {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium)
                ) {
                    Text(
                        text = "Folders",
                        style = CentraliaType.display,
                        color = CentraliaColors.Ink,
                        modifier = Modifier.weight(1f)
                    )

                    CentraliaTextButton(
                        title = "New Folder",
                        onClick = { showsNewFolder = true }
                    )
                }
            }
        }

        item(key = "folderSearch") {
            CenteredContent {
                CentraliaSearchField(
                    placeholder = "Search folders",
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.padding(horizontal = Spacing.medium)
                )
            }
        }

        when (val loadState = state.state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                CentraliaLoadingState(label = "Loading folders…", minHeight = 320.dp)
            }

            is LoadState.Failed -> item(key = "failed") {
                CenteredContent {
                    ContentUnavailable(
                        title = "Folders unavailable",
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
                // The Unorganized card is hidden while searching folders by name.
                if (state.query.trim().isEmpty()) {
                    item(key = "unorganized") {
                        CenteredContent {
                            UnorganizedCollectionCard(
                                unorganizedCount = state.unorganizedCount,
                                onClick = onOpenSmartOrganization,
                                modifier = Modifier.padding(horizontal = Spacing.medium)
                            )
                        }
                    }
                }

                if (state.filteredFolders.isEmpty()) {
                    item(key = "emptyFolders") {
                        CenteredContent {
                            if (state.folders.isEmpty()) {
                                ContentUnavailable(
                                    title = "Create your first folder",
                                    icon = CentraliaIcons.NewFolder,
                                    description = "Use folders to keep your saved shorts together.",
                                    minHeight = 240.dp,
                                    actions = {
                                        ProminentAction(title = "New Folder") {
                                            showsNewFolder = true
                                        }
                                    }
                                )
                            } else {
                                // `ContentUnavailableView.search(text:)`
                                ContentUnavailable(
                                    title = "No Results",
                                    icon = CentraliaIcons.Search,
                                    description = "No folders found for “${state.query}”.",
                                    minHeight = 220.dp
                                )
                            }
                        }
                    }
                } else {
                    folderGrid(
                        folders = state.filteredFolders,
                        itemCount = state::itemCount,
                        onOpenFolder = onOpenFolder
                    )
                }
            }
        }
    }

    if (showsNewFolder) {
        NewFolderSheet(
            failureMessage = state.failureMessage,
            onCreate = { name, symbol ->
                viewModel.createFolder(name, symbol) {
                    showsNewFolder = false
                    revisions.libraryChanged()
                }
            },
            onDismiss = {
                showsNewFolder = false
                viewModel.dismissFailure()
            }
        )
    }
}

/**
 * `unorganizedCollection` — the full-width ink card that opens Smart
 * Organization.
 */
@Composable
private fun UnorganizedCollectionCard(
    unorganizedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = if (unorganizedCount == 1) {
        "1 short waiting for you"
    } else {
        "$unorganizedCount shorts waiting for you"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 112.dp)
            .centraliaPressable(onClickLabel = "Unorganized, $description", onClick = onClick)
            .background(CentraliaColors.Ink, RoundedCornerShape(24.dp))
            .padding(Spacing.medium)
    ) {
        Icon(
            imageVector = CentraliaIcons.Sparkle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .semanticsHidden()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Unorganized",
                style = CentraliaType.title2.bold(),
                color = Color.White
            )

            Text(
                text = description,
                style = CentraliaType.body,
                color = Color.White.copy(alpha = 0.74f)
            )
        }

        Icon(
            imageVector = CentraliaIcons.ChevronRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .semanticsHidden()
        )
    }
}

/** `folderGrid` — a two-column grid of folder cards. */
private fun androidx.compose.foundation.lazy.LazyListScope.folderGrid(
    folders: List<LibraryFolder>,
    itemCount: (LibraryFolder) -> Int,
    onOpenFolder: (LibraryFolder) -> Unit
) {
    val rows = folders.chunked(2)

    items(
        count = rows.size,
        key = { index -> "folderRow-${rows[index].first().id}" }
    ) { index ->
        val row = rows[index]

        CenteredContent {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.Top
            ) {
                row.forEach { folder ->
                    Box(modifier = Modifier.weight(1f)) {
                        FolderGridCard(
                            folder = folder,
                            itemCount = itemCount(folder),
                            onClick = { onOpenFolder(folder) }
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

/** `private struct FolderGridCard`. */
@Composable
private fun FolderGridCard(
    folder: LibraryFolder,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countLabel = if (itemCount == 1) "1 short" else "$itemCount shorts"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 170.dp)
            .centraliaPressable(
                onClickLabel = "${folder.name}, $countLabel",
                onClick = onClick
            )
            .background(CentraliaColors.Surface, RoundedCornerShape(22.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(22.dp))
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Icon(
            imageVector = folderSymbolIcon(folder.symbolName),
            contentDescription = null,
            tint = CentraliaColors.Ink,
            modifier = Modifier
                .size(36.dp)
                .semanticsHidden()
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Text(
            text = folder.name,
            style = CentraliaType.title3.bold(),
            color = CentraliaColors.Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = countLabel,
            style = CentraliaType.subheadline,
            color = CentraliaColors.SecondaryText
        )
    }
}

/** `private struct NewFolderSheet`. */
@Composable
private fun NewFolderSheet(
    failureMessage: String?,
    onCreate: (String, FolderSymbol) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedSymbol by remember { mutableStateOf(FolderSymbol.FOLDER) }
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // `.onAppear { folderNameIsFocused = true }`
        runCatching { nameFocusRequester.requestFocus() }
    }

    val save = { onCreate(folderName, selectedSymbol) }

    CentraliaSheet(
        title = "New Folder",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss),
        trailingAction = SheetAction(
            title = "Save",
            isEnabled = folderName.trim().isNotEmpty(),
            onClick = save
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Choose a clear name for this collection of saved shorts.",
                style = CentraliaType.body,
                color = CentraliaColors.SecondaryText
            )

            CentraliaBorderedTextField(
                placeholder = "Folder name",
                value = folderName,
                onValueChange = { folderName = it },
                capitalization = KeyboardCapitalization.Words,
                focusRequester = nameFocusRequester,
                onSubmit = save
            )

            Text(
                text = "Icon",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink,
                modifier = Modifier.padding(top = Spacing.small)
            )

            FolderSymbolPicker(
                selection = selectedSymbol,
                onSelect = { selectedSymbol = it }
            )

            failureMessage?.let { SheetErrorText(message = it) }
        }
    }
}
