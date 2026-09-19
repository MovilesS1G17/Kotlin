package com.centralia.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.app.LibraryRevisions
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.search.SearchHistoryRepository
import com.centralia.app.feature.library.LibraryVideoCard
import com.centralia.app.feature.library.LoadState
import com.centralia.app.feature.library.MoveVideoSheet
import com.centralia.app.feature.library.shortsCountLabel
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaFilterMenu
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.CentraliaSearchField
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.LibraryUndoToast
import com.centralia.app.ui.components.MenuActionItem
import com.centralia.app.ui.components.MenuSelectionItem
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold
import java.util.UUID

/** `struct SearchView` — Screen 2, Global Search. */
@Composable
fun SearchScreen(
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    searchHistoryRepository: SearchHistoryRepository,
    revisions: LibraryRevisions,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "search") {
        SearchViewModel(videoRepository, folderRepository, searchHistoryRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val libraryRevision by revisions.library.collectAsStateWithLifecycle()
    val deletedElsewhere by revisions.deletedVideo.collectAsStateWithLifecycle()

    var playingVideoID by remember { mutableStateOf<UUID?>(null) }
    var pendingDeletion by remember { mutableStateOf<VideoItem?>(null) }
    var movingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var searchIsFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(libraryRevision) {
        if (libraryRevision == 0) viewModel.load() else viewModel.retry()
    }

    LaunchedEffect(deletedElsewhere) {
        deletedElsewhere?.let { video ->
            viewModel.registerDeleted(video)
            revisions.consumeDeletedVideo()
        }
    }

    val showsRecentSearches = searchIsFocused &&
        state.query.isEmpty() &&
        state.recentSearches.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Spacing.medium, bottom = Spacing.xLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            item(key = "title") {
                CenteredContent {
                    Text(
                        text = "Search",
                        style = CentraliaType.display,
                        color = CentraliaColors.Ink,
                        modifier = Modifier.padding(horizontal = Spacing.medium)
                    )
                }
            }

            item(key = "searchArea") {
                CenteredContent {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        CentraliaSearchField(
                            placeholder = "Search titles, creators, or tags",
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            focusRequester = searchFocusRequester,
                            onFocusChange = { searchIsFocused = it },
                            onSubmit = viewModel::submitSearch
                        )

                        if (showsRecentSearches) {
                            RecentSearchMenu(
                                recentSearches = state.recentSearches,
                                onSelect = viewModel::selectRecentSearch,
                                onClearHistory = viewModel::clearRecentSearches
                            )
                        }
                    }
                }
            }

            item(key = "filters") {
                FilterMenuRow(
                    state = state,
                    onPlatformChange = viewModel::onPlatformChange,
                    onCreatorChange = viewModel::onCreatorChange,
                    onFolderFilterChange = viewModel::onFolderFilterChange,
                    onToggleTag = viewModel::toggleTag,
                    onClearTags = viewModel::clearTags,
                    onClearFilters = viewModel::clearFilters
                )
            }

            when (val loadState = state.state) {
                LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                    CentraliaLoadingState(label = "Searching your library…", minHeight = 280.dp)
                }

                is LoadState.Failed -> item(key = "failed") {
                    CenteredContent {
                        ContentUnavailable(
                            title = "Search unavailable",
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
                    item(key = "resultsHeader") {
                        CenteredContent {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shorts in your library",
                                    style = CentraliaType.sectionTitle,
                                    color = CentraliaColors.Ink,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = shortsCountLabel(state.filteredVideos.size),
                                    style = CentraliaType.subheadline,
                                    color = CentraliaColors.SecondaryText
                                )
                            }
                        }
                    }

                    if (state.videos.isEmpty()) {
                        item(key = "noSaved") {
                            CenteredContent {
                                ContentUnavailable(
                                    title = "No saved shorts",
                                    icon = CentraliaIcons.VideoStack,
                                    description = "Save a short before searching your library.",
                                    minHeight = 280.dp
                                )
                            }
                        }
                    } else if (state.filteredVideos.isEmpty()) {
                        item(key = "noResults") {
                            CenteredContent {
                                ContentUnavailable(
                                    title = "No shorts found",
                                    icon = CentraliaIcons.Search,
                                    description = "Try another title, creator, folder, or tag.",
                                    minHeight = 280.dp,
                                    actions = {
                                        ProminentAction(title = "Clear Search and Filters") {
                                            viewModel.clearSearch()
                                            searchFocusRequester.requestFocus()
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        resultsGrid(
                            videos = state.filteredVideos,
                            allVideos = state.videos,
                            playingVideoID = playingVideoID,
                            onOpenDetail = onOpenVideo,
                            onTogglePlayback = { video ->
                                playingVideoID = if (playingVideoID == video.id) null else video.id
                            },
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
                onUndo = viewModel::undoDelete,
                onDismiss = viewModel::dismissUndo
            )
        }
    }

    movingVideo?.let { video ->
        MoveVideoSheet(
            folders = state.folders,
            currentFolderID = video.folderID,
            onMove = { folderID -> viewModel.move(video, folderID) },
            onDismiss = { movingVideo = null }
        )
    }

    pendingDeletion?.let { video ->
        CentraliaAlert(
            title = "Delete this video?",
            message = "“${video.displayTitle}” will be removed from your library.",
            confirmTitle = "Delete",
            dismissTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.delete(video)
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }
}

/** `recentSearchMenu` — the history panel shown under a focused, empty field. */
@Composable
private fun RecentSearchMenu(
    recentSearches: List<String>,
    onSelect: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CentraliaColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(18.dp))
            .padding(vertical = Spacing.small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small)
        ) {
            Text(
                text = "Recent searches",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink,
                modifier = Modifier.weight(1f)
            )

            CentraliaTextButton(
                title = "Clear History",
                style = CentraliaType.subheadline.semibold(),
                onClick = onClearHistory
            )
        }

        recentSearches.forEach { search ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp)
                    .centraliaPressable { onSelect(search) }
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small)
            ) {
                Icon(
                    imageVector = CentraliaIcons.RecentSearch,
                    contentDescription = null,
                    tint = CentraliaColors.Ink,
                    modifier = Modifier.size(20.dp)
                )

                Text(text = search, style = CentraliaType.body, color = CentraliaColors.Ink)
            }
        }
    }
}

/** `filterMenus` — platform, creator, folder, tags, and the clear action. */
@Composable
private fun FilterMenuRow(
    state: SearchViewModel.UiState,
    onPlatformChange: (VideoPlatform?) -> Unit,
    onCreatorChange: (String?) -> Unit,
    onFolderFilterChange: (SearchFolderFilter) -> Unit,
    onToggleTag: (String) -> Unit,
    onClearTags: () -> Unit,
    onClearFilters: () -> Unit
) {
    val selectedFolderTitle = when (val filter = state.selectedFolder) {
        SearchFolderFilter.All -> "Folder"
        SearchFolderFilter.Unorganized -> "Unorganized"
        is SearchFolderFilter.InFolder -> state.folderName(filter.folderID) ?: "Folder"
    }

    val selectedTagsTitle = when (state.selectedTags.size) {
        0 -> "Tags"
        1 -> state.selectedTags.first()
        else -> "${state.selectedTags.size} Tags"
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        item(key = "platform") {
            CentraliaFilterMenu(
                title = state.selectedPlatform?.filterName ?: "Platform",
                isActive = state.selectedPlatform != null,
                contentDescription = "Platform filter"
            ) { dismiss ->
                MenuSelectionItem(
                    title = "All Platforms",
                    isSelected = state.selectedPlatform == null
                ) {
                    onPlatformChange(null)
                    dismiss()
                }

                VideoPlatform.entries.forEach { platform ->
                    MenuSelectionItem(
                        title = platform.displayName,
                        isSelected = state.selectedPlatform == platform
                    ) {
                        onPlatformChange(platform)
                        dismiss()
                    }
                }
            }
        }

        item(key = "creator") {
            CentraliaFilterMenu(
                title = state.selectedCreator ?: "Creator",
                isActive = state.selectedCreator != null,
                contentDescription = "Creator filter"
            ) { dismiss ->
                MenuSelectionItem(
                    title = "All Creators",
                    isSelected = state.selectedCreator == null
                ) {
                    onCreatorChange(null)
                    dismiss()
                }

                state.availableCreators.forEach { creator ->
                    MenuSelectionItem(
                        title = creator,
                        isSelected = state.selectedCreator == creator
                    ) {
                        onCreatorChange(creator)
                        dismiss()
                    }
                }
            }
        }

        item(key = "folder") {
            CentraliaFilterMenu(
                title = selectedFolderTitle,
                isActive = state.selectedFolder != SearchFolderFilter.All,
                contentDescription = "Folder filter"
            ) { dismiss ->
                MenuSelectionItem(
                    title = "All Folders",
                    isSelected = state.selectedFolder == SearchFolderFilter.All
                ) {
                    onFolderFilterChange(SearchFolderFilter.All)
                    dismiss()
                }

                MenuSelectionItem(
                    title = "Unorganized",
                    isSelected = state.selectedFolder == SearchFolderFilter.Unorganized
                ) {
                    onFolderFilterChange(SearchFolderFilter.Unorganized)
                    dismiss()
                }

                state.folders.forEach { folder ->
                    MenuSelectionItem(
                        title = folder.name,
                        isSelected = state.selectedFolder ==
                            SearchFolderFilter.InFolder(folder.id)
                    ) {
                        onFolderFilterChange(SearchFolderFilter.InFolder(folder.id))
                        dismiss()
                    }
                }
            }
        }

        item(key = "tags") {
            CentraliaFilterMenu(
                title = selectedTagsTitle,
                isActive = state.selectedTags.isNotEmpty(),
                contentDescription = "Tags filter"
            ) { dismiss ->
                if (state.selectedTags.isNotEmpty()) {
                    MenuActionItem(
                        title = "Clear Tags",
                        icon = CentraliaIcons.Close
                    ) {
                        onClearTags()
                        dismiss()
                    }
                }

                // Tags are multi-select, so the menu stays open between taps.
                state.availableTags.forEach { tag ->
                    MenuSelectionItem(
                        title = tag,
                        isSelected = state.selectedTags.contains(tag)
                    ) {
                        onToggleTag(tag)
                    }
                }
            }
        }

        if (state.hasActiveFilters) {
            item(key = "clearFilters") {
                CentraliaTextButton(
                    title = "Clear filters",
                    style = CentraliaType.subheadline.semibold(),
                    onClick = onClearFilters
                )
            }
        }
    }
}

/**
 * `resultsGrid` — the same two-column feed as the library.
 *
 * The card palette is keyed off the video's position in the unfiltered library
 * (`stableStyleIndex`), so a card keeps its colour as filters narrow the results.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.resultsGrid(
    videos: List<VideoItem>,
    allVideos: List<VideoItem>,
    playingVideoID: UUID?,
    onOpenDetail: (VideoItem) -> Unit,
    onTogglePlayback: (VideoItem) -> Unit,
    onRequestMove: (VideoItem) -> Unit,
    onRequestDelete: (VideoItem) -> Unit
) {
    val pairs = videos.chunked(2)

    items(
        count = pairs.size,
        key = { index -> "searchResult-${pairs[index].first().id}" }
    ) { index ->
        val pair = pairs[index]

        CenteredContent {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                pair.forEachIndexed { offset, video ->
                    val fallbackIndex = index * 2 + offset
                    val stableStyleIndex = allVideos
                        .indexOfFirst { it.id == video.id }
                        .takeIf { it >= 0 } ?: fallbackIndex

                    Box(modifier = Modifier.weight(1f)) {
                        LibraryVideoCard(
                            video = video,
                            styleIndex = stableStyleIndex,
                            isPlaying = playingVideoID == video.id,
                            onOpenDetail = { onOpenDetail(video) },
                            onTogglePlayback = { onTogglePlayback(video) },
                            onRequestMove = { onRequestMove(video) },
                            onRequestDelete = { onRequestDelete(video) }
                        )
                    }
                }

                if (pair.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
