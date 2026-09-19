package com.centralia.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.app.LibraryRevisions
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaChip
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.LibraryUndoToast
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import java.util.UUID

/**
 * `struct LibraryView` — Screen 1. The SwiftUI body is a `ScrollView` of a
 * `LazyVStack`; here the whole page is a [LazyColumn] so the masonry feed and the
 * header scroll together while rows stay lazily composed.
 */
@Composable
fun LibraryScreen(
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    revisions: LibraryRevisions,
    onOpenSearchTab: () -> Unit,
    onPresentSave: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenFolder: (LibraryFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "library") {
        LibraryViewModel(videoRepository, folderRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val libraryRevision by revisions.library.collectAsStateWithLifecycle()
    val deletedElsewhere by revisions.deletedVideo.collectAsStateWithLifecycle()

    var playingVideoID by remember { mutableStateOf<UUID?>(null) }
    var pendingDeletion by remember { mutableStateOf<VideoItem?>(null) }
    var movingVideo by remember { mutableStateOf<VideoItem?>(null) }

    // `.task { await viewModel.load() }`, re-run whenever another tab reports a
    // change — the Compose stand-in for `.id(libraryRevision)`.
    LaunchedEffect(libraryRevision) {
        if (libraryRevision == 0) viewModel.load() else viewModel.retry()
    }

    // A delete performed on the pushed detail screen surfaces its undo toast here.
    LaunchedEffect(deletedElsewhere) {
        deletedElsewhere?.let { video ->
            viewModel.registerDeleted(video)
            revisions.consumeDeletedVideo()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = Spacing.small,
                bottom = Spacing.xLarge
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            item(key = "header") {
                CenteredContent {
                    LibraryHeader(
                        onPresentSave = onPresentSave,
                        modifier = Modifier.padding(horizontal = Spacing.medium)
                    )
                }
            }

            item(key = "searchEntry") {
                CenteredContent {
                    LibrarySearchEntry(
                        onClick = onOpenSearchTab,
                        modifier = Modifier.padding(horizontal = Spacing.medium)
                    )
                }
            }

            item(key = "sourceFilters") {
                SourceFilterRow(
                    selectedFilter = state.selectedFilter,
                    onSelect = viewModel::onFilterChange
                )
            }

            when (val loadState = state.state) {
                LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                    CentraliaLoadingState(label = "Loading your library…", minHeight = 280.dp)
                }

                is LoadState.Failed -> item(key = "failed") {
                    CenteredContent {
                        ContentUnavailable(
                            title = "Library unavailable",
                            icon = CentraliaIcons.Warning,
                            description = loadState.message,
                            minHeight = 320.dp,
                            actions = {
                                ProminentAction(title = "Try Again", onClick = viewModel::retry)
                            }
                        )
                    }
                }

                LoadState.Loaded -> if (state.videos.isEmpty()) {
                    item(key = "emptyLibrary") {
                        CenteredContent {
                            ContentUnavailable(
                                title = "Your library is ready",
                                icon = CentraliaIcons.VideoStack,
                                description = "Save a TikTok, Instagram Reel, or YouTube " +
                                    "Short to see it here.",
                                minHeight = 340.dp,
                                actions = {
                                    ProminentAction(
                                        title = "Save a Short",
                                        onClick = onPresentSave
                                    )
                                }
                            )
                        }
                    }
                } else {
                    item(key = "folderShortcuts") {
                        FolderShortcutsSection(
                            folders = state.folders,
                            itemCount = viewModel::itemCount,
                            onOpenFolder = onOpenFolder
                        )
                    }

                    if (state.filteredVideos.isEmpty()) {
                        item(key = "noneForFilter") {
                            CenteredContent {
                                ContentUnavailable(
                                    title = "No saved ${state.selectedFilter.displayName}",
                                    icon = CentraliaIcons.VideoStack,
                                    description = "Choose another platform or save a new short.",
                                    minHeight = 260.dp
                                )
                            }
                        }
                    } else {
                        masonryFeed(
                            videos = state.filteredVideos,
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

/** `sourceFilters` — the horizontally scrolling platform chips. */
@Composable
fun SourceFilterRow(
    selectedFilter: LibrarySourceFilter,
    onSelect: (LibrarySourceFilter) -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Spacing.medium
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        items(
            items = LibrarySourceFilter.allCases,
            key = { it.id }
        ) { filter ->
            CentraliaChip(
                title = filter.displayName,
                isSelected = selectedFilter == filter,
                onClick = { onSelect(filter) }
            )
        }

        if (trailingContent != null) {
            item(key = "trailing") { trailingContent() }
        }
    }
}

/** `folderShortcuts` — the "Folders" heading plus its horizontal row. */
@Composable
private fun FolderShortcutsSection(
    folders: List<LibraryFolder>,
    itemCount: (LibraryFolder) -> Int,
    onOpenFolder: (LibraryFolder) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        CenteredContent {
            Text(
                text = "Folders",
                style = CentraliaType.sectionTitle,
                color = CentraliaColors.Ink,
                modifier = Modifier.padding(horizontal = Spacing.medium)
            )
        }

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Spacing.medium
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            items(items = folders, key = { it.id.toString() }) { folder ->
                FolderShortcut(
                    folder = folder,
                    itemCount = itemCount(folder),
                    onClick = { onOpenFolder(folder) }
                )
            }
        }
    }
}

/**
 * `masonryFeed` — two columns fed by alternating index, so each column keeps its
 * own intrinsic heights rather than being forced into a uniform grid row.
 *
 * The SwiftUI version nests two `LazyVStack`s in an `HStack`. A Compose
 * [LazyColumn] cannot nest another lazy column vertically, so the pairs are
 * emitted as rows of the outer list: row *n* holds items 2n and 2n+1, which lands
 * items on the same left/right sides as the original.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.masonryFeed(
    videos: List<VideoItem>,
    playingVideoID: UUID?,
    onOpenDetail: (VideoItem) -> Unit,
    onTogglePlayback: (VideoItem) -> Unit,
    onRequestMove: (VideoItem) -> Unit,
    onRequestDelete: (VideoItem) -> Unit
) {
    val pairs = videos.chunked(2)

    items(
        count = pairs.size,
        key = { index -> "masonry-${pairs[index].first().id}" }
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
                    Box(modifier = Modifier.weight(1f)) {
                        LibraryVideoCard(
                            video = video,
                            styleIndex = index * 2 + offset,
                            isPlaying = playingVideoID == video.id,
                            onOpenDetail = { onOpenDetail(video) },
                            onTogglePlayback = { onTogglePlayback(video) },
                            onRequestMove = { onRequestMove(video) },
                            onRequestDelete = { onRequestDelete(video) }
                        )
                    }
                }

                // Keeps a lone trailing card at half width.
                if (pair.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
