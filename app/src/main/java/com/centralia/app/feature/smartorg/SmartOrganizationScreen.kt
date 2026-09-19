package com.centralia.app.feature.smartorg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.library.DurationBadge
import com.centralia.app.feature.library.LoadState
import com.centralia.app.feature.library.PlatformBadge
import com.centralia.app.feature.videodetail.thumbnailColor
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaBorderedButton
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.SheetListRow
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.folderSymbolIcon
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.semibold

/** `struct SmartOrganizationView` — Screen 8. */
@Composable
fun SmartOrganizationScreen(
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    suggestionPipeline: VideoImportPipeline,
    onBack: () -> Unit,
    onLibraryChanged: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "smartOrganization") {
        SmartOrganizationViewModel(videoRepository, folderRepository, suggestionPipeline)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var changingSuggestion by remember { mutableStateOf<FolderSuggestion?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CenteredContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
                    .padding(top = Spacing.small, bottom = Spacing.xLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                // header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.fillMaxWidth()
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
                        text = "Organize",
                        style = CentraliaType.display,
                        color = CentraliaColors.Ink
                    )
                }

                // unorganizedCount
                Text(
                    text = if (state.unorganizedCount == 1) {
                        "1 unorganized short"
                    } else {
                        "${state.unorganizedCount} unorganized shorts"
                    },
                    style = CentraliaType.headline,
                    color = CentraliaColors.SecondaryText
                )

                when (val loadState = state.state) {
                    LoadState.Idle, LoadState.Loading -> CentraliaLoadingState(
                        label = "Preparing suggestions…",
                        minHeight = 440.dp
                    )

                    is LoadState.Failed -> ContentUnavailable(
                        title = "Suggestions unavailable",
                        icon = CentraliaIcons.Warning,
                        description = loadState.message,
                        minHeight = 440.dp,
                        actions = {
                            ProminentAction(title = "Try Again", onClick = viewModel::retry)
                        }
                    )

                    LoadState.Loaded -> if (state.suggestions.isEmpty()) {
                        CompletionState(
                            unorganizedCount = state.unorganizedCount,
                            onBackToFolders = onBack
                        )
                    } else {
                        SuggestionPager(
                            suggestions = state.suggestions,
                            selectedSuggestionID = state.selectedSuggestionID,
                            isMutating = state.isMutating,
                            onSelectedChange = viewModel::onSelectedSuggestionChange,
                            onOpenVideo = onOpenVideo,
                            onChangeFolder = { changingSuggestion = it },
                            onAccept = { viewModel.accept(it, onLibraryChanged) },
                            onSkip = viewModel::skip
                        )

                        ControlNotice()
                    }
                }
            }
        }

        Box(modifier = Modifier.navigationBarsPadding())
    }

    changingSuggestion?.let { suggestion ->
        SuggestionFolderPickerSheet(
            folders = state.folders,
            suggestedFolderID = suggestion.folder.id,
            onChoose = { folder -> viewModel.move(suggestion, folder, onLibraryChanged) },
            onDismiss = { changingSuggestion = null }
        )
    }

    state.failureMessage?.let { message ->
        CentraliaAlert(
            title = "Couldn’t organize this short",
            message = message,
            onConfirm = viewModel::dismissFailure
        )
    }
}

/**
 * `suggestionPager` plus `pageIndicator`.
 *
 * SwiftUI uses a `TabView` in `.page` style with the indicator hidden and its own
 * tappable dots below. [HorizontalPager] is the Compose equivalent; the pager
 * state and the view model's selected id are kept in step in both directions.
 */
@Composable
private fun SuggestionPager(
    suggestions: List<FolderSuggestion>,
    selectedSuggestionID: java.util.UUID?,
    isMutating: Boolean,
    onSelectedChange: (java.util.UUID?) -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onChangeFolder: (FolderSuggestion) -> Unit,
    onAccept: (FolderSuggestion) -> Unit,
    onSkip: (FolderSuggestion) -> Unit
) {
    val selectedIndex = suggestions
        .indexOfFirst { it.id == selectedSuggestionID }
        .coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { suggestions.size }
    )

    // Swiping the pager updates the selection.
    LaunchedEffect(pagerState, suggestions) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            suggestions.getOrNull(page)?.let { onSelectedChange(it.id) }
        }
    }

    // Tapping a dot, or a card being removed, scrolls the pager.
    LaunchedEffect(selectedSuggestionID, suggestions.size) {
        val target = suggestions.indexOfFirst { it.id == selectedSuggestionID }
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.large)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp),
            pageSpacing = Spacing.small
        ) { page ->
            suggestions.getOrNull(page)?.let { suggestion ->
                SmartSuggestionCard(
                    suggestion = suggestion,
                    isMutating = isMutating,
                    onOpenVideo = { onOpenVideo(suggestion.video) },
                    onChangeFolder = { onChangeFolder(suggestion) },
                    onAccept = { onAccept(suggestion) },
                    onSkip = { onSkip(suggestion) },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }

        // pageIndicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f))

            suggestions.forEachIndexed { index, suggestion ->
                val isSelected = selectedSuggestionID == suggestion.id

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .centraliaPressable(
                            onClickLabel = "Suggestion ${index + 1} of ${suggestions.size}"
                        ) { onSelectedChange(suggestion.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isSelected) {
                                    CentraliaColors.Ink
                                } else {
                                    CentraliaColors.Divider
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            Box(modifier = Modifier.weight(1f))
        }
    }
}

/** `private struct SmartSuggestionCard`. */
@Composable
private fun SmartSuggestionCard(
    suggestion: FolderSuggestion,
    isMutating: Boolean,
    onOpenVideo: () -> Unit,
    onChangeFolder: () -> Unit,
    onAccept: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember(suggestion.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CentraliaColors.Surface, RoundedCornerShape(24.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(24.dp))
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // preview
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 390.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(thumbnailColor(suggestion.video.platform))
                    .centraliaPressable(onClick = onOpenVideo)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .centraliaPressable(
                            onClickLabel = if (isPlaying) "Pause preview" else "Play preview"
                        ) { isPlaying = !isPlaying }
                        .background(CentraliaColors.Ink.copy(alpha = 0.94f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) CentraliaIcons.Pause else CentraliaIcons.Play,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                PlatformBadge(
                    text = suggestion.video.platform.displayName,
                    style = CentraliaType.subheadline.semibold(),
                    minHeight = 36.dp,
                    horizontalPadding = Spacing.medium,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .semanticsHidden()
                )

                DurationBadge(
                    text = suggestion.video.formattedDuration,
                    style = CentraliaType.subheadline.semibold(),
                    minHeight = 30.dp,
                    backgroundAlpha = 0.84f,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .semanticsHidden()
                )
            }
        }

        // title block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .centraliaPressable(
                    onClickLabel = "Opens video details",
                    onClick = onOpenVideo
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = suggestion.video.displayTitle,
                style = CentraliaType.title2.bold(),
                color = CentraliaColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = suggestion.video.creator,
                style = CentraliaType.headline,
                color = CentraliaColors.SecondaryText
            )
        }

        Text(
            text = "Suggested folder",
            style = CentraliaType.subheadline.semibold(),
            color = CentraliaColors.SecondaryText
        )

        // the suggested folder row, tappable to choose another
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp)
                .centraliaPressable(
                    enabled = !isMutating,
                    onClickLabel = "Suggested folder ${suggestion.folder.name}. Change folder",
                    onClick = onChangeFolder
                )
                .background(CentraliaColors.SoftSurface, RoundedCornerShape(16.dp))
                .padding(horizontal = Spacing.medium, vertical = Spacing.small)
        ) {
            Icon(
                imageVector = folderSymbolIcon(suggestion.folder.symbolName),
                contentDescription = null,
                tint = CentraliaColors.Ink,
                modifier = Modifier
                    .size(28.dp)
                    .semanticsHidden()
            )

            Text(
                text = suggestion.folder.name,
                style = CentraliaType.headline,
                color = CentraliaColors.Ink,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Change",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink
            )
        }

        // accept / skip
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CentraliaPrimaryButton(
                title = "Accept",
                isDisabled = isMutating,
                minHeight = 54,
                cornerRadius = 16.dp,
                modifier = Modifier.weight(1f),
                onClick = onAccept
            )

            CentraliaBorderedButton(
                title = "Skip",
                isDisabled = isMutating,
                minHeight = 54,
                modifier = Modifier.weight(1f),
                onClick = onSkip
            )
        }
    }
}

/** `controlNotice` — the reassurance strip under the pager. */
@Composable
private fun ControlNotice() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxWidth()
            .background(CentraliaColors.SoftSurface, RoundedCornerShape(20.dp))
            .padding(Spacing.medium)
    ) {
        Icon(
            imageVector = CentraliaIcons.Sparkle,
            contentDescription = null,
            tint = CentraliaColors.Ink,
            modifier = Modifier
                .size(40.dp)
                .semanticsHidden()
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Suggestions stay under your control",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink
            )

            Text(
                text = "Nothing moves without your choice.",
                style = CentraliaType.subheadline,
                color = CentraliaColors.SecondaryText
            )
        }
    }
}

/** `completionState` — nothing left to suggest, or nothing left unorganized. */
@Composable
private fun CompletionState(
    unorganizedCount: Int,
    onBackToFolders: () -> Unit
) {
    ContentUnavailable(
        title = if (unorganizedCount == 0) {
            "Everything is organized"
        } else {
            "No suggestions left"
        },
        icon = if (unorganizedCount == 0) {
            CentraliaIcons.CheckCircle
        } else {
            CentraliaIcons.Sparkle
        },
        description = if (unorganizedCount == 0) {
            "Your Unorganized collection is empty."
        } else {
            "Skipped shorts are still safe in Unorganized."
        },
        minHeight = 440.dp,
        actions = {
            ProminentAction(title = "Back to Folders", onClick = onBackToFolders)
        }
    )
}

/** `private struct SuggestionFolderPickerSheet`. */
@Composable
private fun SuggestionFolderPickerSheet(
    folders: List<LibraryFolder>,
    suggestedFolderID: java.util.UUID,
    onChoose: (LibraryFolder) -> Unit,
    onDismiss: () -> Unit
) {
    CentraliaSheet(
        title = "Choose Folder",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Spacing.small)
                .navigationBarsPadding()
        ) {
            folders.forEach { folder ->
                SheetListRow(
                    title = folder.name,
                    leadingIcon = folderSymbolIcon(folder.symbolName),
                    trailingIcon = if (folder.id == suggestedFolderID) {
                        CentraliaIcons.Sparkle
                    } else {
                        null
                    },
                    trailingIconDescription = if (folder.id == suggestedFolderID) {
                        "Suggested"
                    } else {
                        null
                    },
                    onClick = {
                        onChoose(folder)
                        onDismiss()
                    }
                )
            }
        }
    }
}
