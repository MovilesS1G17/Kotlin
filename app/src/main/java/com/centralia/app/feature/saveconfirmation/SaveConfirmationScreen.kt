package com.centralia.app.feature.saveconfirmation

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.videodetail.thumbnailColor
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CapsuleShape
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaBorderedButton
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.CentraliaTextArea
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.FlowLayout
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.ContentWidth
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.medium
import com.centralia.app.ui.theme.semibold

/** `struct SaveConfirmationView` — Screen 7. */
@Composable
fun SaveConfirmationScreen(
    video: VideoItem,
    folderName: String?,
    videoRepository: VideoItemRepository,
    onVideoUpdated: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "saveConfirmation-${video.id}") {
        SaveConfirmationViewModel(video, videoRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showsNoteEditor by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CentraliaColors.Canvas)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        CenteredContent(maxWidth = ContentWidth.confirmation) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
                    .padding(top = Spacing.xxLarge, bottom = Spacing.xLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xLarge)
            ) {
                SuccessStatus(title = state.video.displayTitle)

                VideoSummaryCard(video = state.video, folderName = folderName)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    CentraliaPrimaryButton(
                        title = "View Video",
                        minHeight = 58,
                        cornerRadius = 16.dp,
                        onClick = { onOpenVideo(state.video) }
                    )

                    if (state.shouldShowAddNote) {
                        CentraliaBorderedButton(
                            title = "Add Note",
                            onClick = { showsNoteEditor = true }
                        )
                    }

                    CentraliaTextButton(
                        title = "Done",
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp),
                        onClick = onDone
                    )
                }
            }
        }

        Box(modifier = Modifier.navigationBarsPadding())
    }

    if (showsNoteEditor) {
        SaveConfirmationNoteSheet(
            isSavingNote = state.isSavingNote,
            onSave = { note ->
                viewModel.saveNote(note) {
                    showsNoteEditor = false
                    onVideoUpdated()
                }
            },
            onDismiss = { showsNoteEditor = false }
        )
    }

    state.failureMessage?.let { message ->
        CentraliaAlert(
            title = "Couldn’t save note",
            message = message,
            onConfirm = viewModel::dismissFailure
        )
    }
}

/** `successStatus` — the 108dp ink circle above the headline. */
@Composable
private fun SuccessStatus(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
    ) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .background(CentraliaColors.Ink, CircleShape)
                .semanticsHidden(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CentraliaIcons.Check,
                contentDescription = null,
                tint = CentraliaColors.Canvas,
                modifier = Modifier.size(52.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(
                text = "Saved to Centralia",
                style = CentraliaType.display,
                color = CentraliaColors.Ink,
                textAlign = TextAlign.Center
            )

            Text(
                text = title,
                style = CentraliaType.title3.semibold(),
                color = CentraliaColors.SecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** `videoSummary` — the thumbnail beside platform, creator, folder and tags. */
@Composable
private fun VideoSummaryCard(
    video: VideoItem,
    folderName: String?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(CentraliaColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(20.dp))
            .padding(Spacing.medium)
    ) {
        Box(
            modifier = Modifier
                .width(92.dp)
                .height(144.dp)
                .background(thumbnailColor(video.platform), RoundedCornerShape(14.dp))
                .semanticsHidden(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CentraliaColors.Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CentraliaIcons.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${video.platform.displayName} · ${video.formattedDuration}",
                style = CentraliaType.subheadline.semibold(),
                color = CentraliaColors.SecondaryText
            )

            Text(
                text = video.creator,
                style = CentraliaType.title3.bold(),
                color = CentraliaColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (folderName == null) {
                        CentraliaIcons.FolderUnknown
                    } else {
                        CentraliaIcons.Folder
                    },
                    contentDescription = null,
                    tint = CentraliaColors.SecondaryText,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    // The iOS copy reads "No Folder Select"; kept verbatim so the
                    // two builds show the same string.
                    text = folderName ?: "No Folder Select",
                    style = CentraliaType.subheadline.medium(),
                    color = CentraliaColors.SecondaryText
                )
            }

            if (video.tags.isNotEmpty()) {
                FlowLayout(spacing = Spacing.small, modifier = Modifier.fillMaxWidth()) {
                    video.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 30.dp)
                                .background(CentraliaColors.SoftSurface, CapsuleShape)
                                .border(1.dp, CentraliaColors.Divider, CapsuleShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tag,
                                style = CentraliaType.caption.semibold(),
                                color = CentraliaColors.Ink
                            )
                        }
                    }
                }
            }
        }
    }
}

/** `private struct SaveConfirmationNoteSheet`. */
@Composable
private fun SaveConfirmationNoteSheet(
    isSavingNote: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    val noteFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { noteFocusRequester.requestFocus() } }

    CentraliaSheet(
        title = "Add Note",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(
            title = "Cancel",
            isEnabled = !isSavingNote,
            onClick = onDismiss
        ),
        trailingAction = SheetAction(
            title = if (isSavingNote) "Saving…" else "Save",
            isEnabled = note.trim().isNotEmpty() && !isSavingNote,
            onClick = { onSave(note) }
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
                text = "Personal note",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink
            )

            CentraliaTextArea(
                placeholder = "Why is this short worth remembering?",
                value = note,
                onValueChange = { note = it },
                minHeight = 180.dp,
                focusRequester = noteFocusRequester,
                contentDescription = "Personal note"
            )
        }
    }
}
