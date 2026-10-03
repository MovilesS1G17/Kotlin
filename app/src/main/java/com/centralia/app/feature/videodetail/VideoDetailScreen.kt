package com.centralia.app.feature.videodetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.PlayerSource
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.feature.library.DurationBadge
import com.centralia.app.feature.library.MoveVideoSheet
import com.centralia.app.feature.library.PlatformBadge
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CapsuleShape
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.FlowLayout
import com.centralia.app.ui.components.MenuActionItem
import com.centralia.app.ui.components.NativeShortPlayer
import com.centralia.app.ui.components.RoundedCornerMask
import com.centralia.app.ui.components.ShortPlayer
import com.centralia.app.ui.components.VideoCoverGradient
import com.centralia.app.ui.components.VideoCoverImage
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.rememberOpenUrl
import com.centralia.app.ui.components.rememberShareLink
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.semibold
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** `struct VideoDetailView` — Screen 6. */
@Composable
fun VideoDetailScreen(
    videoID: UUID,
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    onBack: () -> Unit,
    onVideoChanged: () -> Unit,
    onVideoDeleted: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    autoplay: Boolean = false
) {
    val viewModel = centraliaViewModel(key = "videoDetail-$videoID") {
        VideoDetailViewModel(videoID, videoRepository, folderRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var showsTagEditor by remember { mutableStateOf(false) }
    var showsFolderPicker by remember { mutableStateOf(false) }
    var showsNoteEditor by remember { mutableStateOf(false) }
    var showsReportForm by remember { mutableStateOf(false) }
    var showsDeleteConfirmation by remember { mutableStateOf(false) }
    var showsOpenFailure by remember { mutableStateOf(false) }
    var showsReportConfirmation by remember { mutableStateOf(false) }

    val openUrl = rememberOpenUrl()
    val shareLink = rememberShareLink()

    LaunchedEffect(Unit) { viewModel.load() }

    // Opened from a card's play button: start the short as soon as it's loaded.
    var hasAutoplayed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.video?.id) {
        if (autoplay && !hasAutoplayed && state.video != null) {
            hasAutoplayed = true
            isPlaying = true
        }
    }

    // Tapping play asks the backend for the short's stream (native player),
    // falling back to the platform's web player, or to its app if neither exists.
    LaunchedEffect(isPlaying, state.video != null) {
        val video = state.video ?: return@LaunchedEffect
        if (isPlaying) {
            viewModel.preparePlayback(onNothingToPlay = {
                isPlaying = false
                if (!openUrl(video.sourceURL)) showsOpenFailure = true
            })
        } else {
            viewModel.stopPlayback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            VideoDetailTopBar(
                onBack = onBack,
                onShare = {
                    state.video?.let { video ->
                        shareLink(
                            video.displayTitle,
                            "${video.displayTitle} — ${video.creator}",
                            video.sourceURL
                        )
                    }
                },
                onReport = { showsReportForm = true }
            )

            val video = state.video

            if (video == null) {
                if (state.isMissing) {
                    ContentUnavailable(
                        title = "Video unavailable",
                        icon = CentraliaIcons.Warning,
                        description = "This short is no longer in your library.",
                        minHeight = 320.dp
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CentraliaColors.Ink)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            VideoPreview(
                                video = video,
                                isPlaying = isPlaying,
                                source = state.playerSource,
                                onPlay = { isPlaying = true },
                                onStop = { isPlaying = false },
                                onNativeError = {
                                    if (state.webFallbackURL != null) {
                                        viewModel.fallBackToWebPlayer()
                                    } else {
                                        isPlaying = false
                                        if (!openUrl(video.sourceURL)) showsOpenFailure = true
                                    }
                                },
                                openActionTitle = state.openActionTitle,
                                onOpenSource = {
                                    isPlaying = false
                                    if (!openUrl(video.sourceURL)) showsOpenFailure = true
                                }
                            )

                            VideoMetadata(video = video, folderName = state.folderName)

                            VideoTags(
                                tags = video.tags,
                                onEditTags = { showsTagEditor = true }
                            )

                            PersonalNoteSection(
                                note = video.note,
                                onEdit = { showsNoteEditor = true }
                            )

                            VideoActions(
                                openActionTitle = state.openActionTitle,
                                folderActionTitle = state.folderActionTitle,
                                isMutating = state.isMutating,
                                onOpenSource = {
                                    if (!openUrl(video.sourceURL)) showsOpenFailure = true
                                },
                                onEditTags = { showsTagEditor = true },
                                onChooseFolder = { showsFolderPicker = true },
                                onDelete = { showsDeleteConfirmation = true }
                            )
                        }
                    }
                }
            }
        }

        // `.overlay { if viewModel.isLoading { ProgressView() … } }`
        if (state.isLoading && state.video != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        CentraliaColors.Surface.copy(alpha = 0.92f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(Spacing.medium)
            ) {
                CircularProgressIndicator(color = CentraliaColors.Ink)
            }
        }
    }

    if (showsTagEditor) {
        VideoTagEditorSheet(
            selectedTags = state.video?.tags ?: emptyList(),
            availableTags = state.availableTags,
            isMutating = state.isMutating,
            onSave = { tags ->
                viewModel.updateTags(tags) {
                    showsTagEditor = false
                    onVideoChanged()
                }
            },
            onDismiss = { showsTagEditor = false }
        )
    }

    if (showsFolderPicker) {
        MoveVideoSheet(
            folders = state.folders,
            currentFolderID = state.video?.folderID,
            onMove = { folderID -> viewModel.move(folderID, onVideoChanged) },
            onDismiss = { showsFolderPicker = false }
        )
    }

    if (showsNoteEditor) {
        VideoNoteEditorSheet(
            initialNote = state.video?.note.orEmpty(),
            hasExistingNote = state.video?.note != null,
            isMutating = state.isMutating,
            onSave = { note ->
                viewModel.updateNote(note) {
                    showsNoteEditor = false
                    onVideoChanged()
                }
            },
            onDismiss = { showsNoteEditor = false }
        )
    }

    if (showsReportForm) {
        VideoReportSheet(
            onSubmit = {
                showsReportForm = false
                showsReportConfirmation = true
            },
            onDismiss = { showsReportForm = false }
        )
    }

    if (showsDeleteConfirmation) {
        CentraliaAlert(
            title = "Delete this video?",
            message = "“${state.video?.displayTitle.orEmpty()}” will be removed from your library.",
            confirmTitle = "Delete",
            dismissTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                showsDeleteConfirmation = false
                viewModel.deleteVideo { deleted ->
                    onVideoDeleted(deleted)
                    onBack()
                }
            },
            onDismiss = { showsDeleteConfirmation = false }
        )
    }

    if (showsOpenFailure) {
        CentraliaAlert(
            title = "Couldn’t open this video",
            message = "The original link is still saved in Centralia. Try again when the " +
                "source is available.",
            onConfirm = { showsOpenFailure = false }
        )
    }

    if (showsReportConfirmation) {
        CentraliaAlert(
            title = "Report sent",
            message = "Thanks. This mock report has been recorded for the demo session.",
            onConfirm = { showsReportConfirmation = false }
        )
    }

    state.failureMessage?.let { message ->
        CentraliaAlert(
            title = "Couldn’t update video",
            message = message,
            onConfirm = viewModel::dismissFailure
        )
    }
}

/**
 * The inline navigation bar: a back affordance, the "Video" title in the section
 * serif, and the overflow menu carrying share and report.
 */
@Composable
private fun VideoDetailTopBar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.small, vertical = Spacing.small)
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
            text = "Video",
            style = CentraliaType.sectionTitle,
            color = CentraliaColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .centraliaPressable(
                        onClickLabel = "More video actions"
                    ) { isExpanded = true },
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
                MenuActionItem(title = "Share Link", icon = CentraliaIcons.Share) {
                    isExpanded = false
                    onShare()
                }

                MenuActionItem(title = "Report", icon = CentraliaIcons.ReportIssue) {
                    isExpanded = false
                    onReport()
                }
            }
        }
    }
}

/**
 * `preview` — the short's cover (0.72 aspect ratio, capped at 410dp wide) with
 * a play button. Playing swaps it for the platform's player at the short's own
 * 9:16 shape; Instagram's embed adds a header and caption, so it gets more height.
 */
@Composable
private fun VideoPreview(
    video: VideoItem,
    isPlaying: Boolean,
    source: PlayerSource?,
    openActionTitle: String,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onNativeError: () -> Unit,
    onOpenSource: () -> Unit
) {
    val showsPlayer = isPlaying
    val aspectRatio = when {
        !showsPlayer -> 0.72f
        // Instagram's web embed adds a header and caption around the video.
        source is PlayerSource.Web && video.platform == VideoPlatform.INSTAGRAM_REEL -> 0.46f
        else -> 9f / 16f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        // The player must NOT sit inside a rounded `clip`: Android can't draw a
        // WebView through a non-rectangular clip, so the video stayed black while
        // its audio played. The rounded corners are painted on top instead.
        Box(
            modifier = Modifier
                .widthIn(max = 410.dp)
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .then(
                    if (showsPlayer) Modifier
                    else Modifier.clip(RoundedCornerShape(24.dp)).background(thumbnailColor(video.platform))
                )
        ) {
            if (showsPlayer) {
                when (source) {
                    // The backend's MP4 in the phone's own video player (first choice).
                    is PlayerSource.Native -> NativeShortPlayer(
                        streamURL = source.streamURL,
                        onError = onNativeError,
                        modifier = Modifier.fillMaxSize()
                    )
                    // The platform's web player (no backend stream, or it failed).
                    is PlayerSource.Web -> ShortPlayer(
                        embedURL = source.embedURL,
                        modifier = Modifier.fillMaxSize()
                    )
                    null -> Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                RoundedCornerMask(
                    radius = 24.dp,
                    color = CentraliaColors.Canvas,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.small)
                        .size(36.dp)
                        .centraliaPressable(onClickLabel = "Stop playing", onClick = onStop)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CentraliaIcons.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                val coverURL = video.coverURL
                VideoCoverImage(url = coverURL)
                if (coverURL != null) VideoCoverGradient()

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(76.dp)
                        .centraliaPressable(onClickLabel = "Play short", onClick = onPlay)
                        .background(CentraliaColors.Ink.copy(alpha = 0.95f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CentraliaIcons.Play,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                PlatformBadge(
                    text = video.platform.displayName,
                    style = CentraliaType.subheadline.semibold(),
                    minHeight = 38.dp,
                    horizontalPadding = Spacing.medium,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.medium)
                )

                video.durationLabel?.let { duration ->
                    DurationBadge(
                        text = duration,
                        style = CentraliaType.subheadline.semibold(),
                        minHeight = 32.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Spacing.medium)
                    )
                }
            }
        }

        // Some devices and emulators can't decode a platform's video inside a
        // WebView (sound, no picture). The platform's own app always can.
        if (showsPlayer) {
            CentraliaTextButton(
                title = "Video not showing? $openActionTitle",
                color = CentraliaColors.SecondaryText,
                style = CentraliaType.subheadline.semibold(),
                onClick = onOpenSource
            )
        }
    }
}

/** `metadata` — title, creator with the saved date, and the folder label. */
@Composable
private fun VideoMetadata(
    video: VideoItem,
    folderName: String?
) {
    val savedLabel = remember(video.savedAt) {
        // `.formatted(.dateTime.month(.abbreviated).day())`
        DateTimeFormatter
            .ofPattern("MMM d", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(video.savedAt)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = video.displayTitle,
            style = CentraliaType.largeTitle.bold(),
            color = CentraliaColors.Ink
        )

        Text(
            text = "${video.creator} · Saved $savedLabel",
            style = CentraliaType.headline,
            color = CentraliaColors.SecondaryText
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                imageVector = if (video.folderID == null) {
                    CentraliaIcons.FolderUnknown
                } else {
                    CentraliaIcons.Folder
                },
                contentDescription = null,
                tint = CentraliaColors.SecondaryText,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = folderName ?: "No Folder Selected",
                style = CentraliaType.subheadline.semibold(),
                color = CentraliaColors.SecondaryText
            )
        }
    }
}

/**
 * `organization` — either an "Add Tags" action or the tag capsules, where the
 * first tag is filled with ink and the rest are outlined.
 */
@Composable
private fun VideoTags(
    tags: List<String>,
    onEditTags: () -> Unit
) {
    if (tags.isEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xSmall),
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .centraliaPressable(onClick = onEditTags)
        ) {
            Icon(
                imageVector = CentraliaIcons.Add,
                contentDescription = null,
                tint = CentraliaColors.Ink,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "Add Tags",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink
            )
        }
        return
    }

    FlowLayout(spacing = Spacing.small, modifier = Modifier.fillMaxWidth()) {
        tags.forEachIndexed { index, tag ->
            val isPrimary = index == 0

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .centraliaPressable(
                        onClickLabel = "Opens the tag editor",
                        onClick = onEditTags
                    )
                    .background(
                        color = if (isPrimary) {
                            CentraliaColors.Ink
                        } else {
                            CentraliaColors.SoftSurface
                        },
                        shape = CapsuleShape
                    )
                    .then(
                        if (isPrimary) {
                            Modifier
                        } else {
                            Modifier.border(1.dp, CentraliaColors.Divider, CapsuleShape)
                        }
                    )
                    .padding(horizontal = Spacing.medium, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tag,
                    style = CentraliaType.headline,
                    color = if (isPrimary) Color.White else CentraliaColors.Ink
                )
            }
        }
    }
}

/** `note` — the tappable personal-note block. */
@Composable
private fun PersonalNoteSection(
    note: String?,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .centraliaPressable(onClickLabel = "Opens the note editor", onClick = onEdit),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Personal note",
                style = CentraliaType.sectionTitle,
                color = CentraliaColors.Ink,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = CentraliaIcons.Pencil,
                contentDescription = null,
                tint = CentraliaColors.SecondaryText,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = note ?: "Add a personal note.",
            style = CentraliaType.body,
            color = if (note == null) CentraliaColors.SecondaryText else CentraliaColors.Ink
        )
    }
}

/** `actions` — the primary open button above the three inline actions. */
@Composable
private fun VideoActions(
    openActionTitle: String,
    folderActionTitle: String,
    isMutating: Boolean,
    onOpenSource: () -> Unit,
    onEditTags: () -> Unit,
    onChooseFolder: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        CentraliaPrimaryButton(
            title = openActionTitle,
            isDisabled = isMutating,
            minHeight = 58,
            cornerRadius = 16.dp,
            onClick = onOpenSource
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CentraliaTextButton(
                title = "Edit Tags",
                isDisabled = isMutating,
                onClick = onEditTags
            )

            Box(modifier = Modifier.weight(1f))

            CentraliaTextButton(
                title = folderActionTitle,
                isDisabled = isMutating,
                onClick = onChooseFolder
            )

            Box(modifier = Modifier.weight(1f))

            CentraliaTextButton(
                title = "Delete",
                color = CentraliaColors.Error,
                isDisabled = isMutating,
                onClick = onDelete
            )
        }
    }
}

/** `thumbnailColor` — one palette colour per platform. */
internal fun thumbnailColor(platform: VideoPlatform): Color = when (platform) {
    VideoPlatform.TIKTOK -> CentraliaColors.VideoSand
    VideoPlatform.INSTAGRAM_REEL -> CentraliaColors.VideoMint
    VideoPlatform.YOUTUBE_SHORT -> CentraliaColors.VideoClay
}
