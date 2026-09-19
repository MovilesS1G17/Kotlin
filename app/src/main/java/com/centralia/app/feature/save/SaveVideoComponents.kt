package com.centralia.app.feature.save

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.centralia.app.domain.imports.ImportedVideoMetadata
import com.centralia.app.domain.imports.VideoImportStage
import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.feature.folders.FolderSymbolPicker
import com.centralia.app.ui.components.CapsuleShape
import com.centralia.app.ui.components.CentraliaBorderedTextField
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.SheetErrorText
import com.centralia.app.ui.components.SheetListRow
import com.centralia.app.ui.components.SheetSectionFooter
import com.centralia.app.ui.components.SheetSectionHeader
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.folderSymbolIcon
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold
import java.util.UUID

/**
 * `SaveImportStatusCard` — the thumbnail, the detected platform or the pipeline
 * stage, and a trailing checkmark or spinner.
 */
@Composable
fun SaveImportStatusCard(
    metadata: ImportedVideoMetadata?,
    stage: VideoImportStage?,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .background(CentraliaColors.SoftSurface, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        ImportThumbnail(platform = metadata?.platform, isError = errorMessage != null)

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            when {
                errorMessage != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = CentraliaIcons.Warning,
                            contentDescription = null,
                            tint = CentraliaColors.Error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Link not supported",
                            style = CentraliaType.headline,
                            color = CentraliaColors.Error
                        )
                    }

                    Text(
                        text = errorMessage,
                        style = CentraliaType.subheadline,
                        color = CentraliaColors.SecondaryText
                    )
                }

                metadata != null -> {
                    Text(
                        text = "${metadata.platform.displayName} detected",
                        style = CentraliaType.headline,
                        color = CentraliaColors.Ink
                    )

                    Text(
                        text = "${metadata.creator} · ${metadata.formattedDuration}",
                        style = CentraliaType.subheadline.semibold(),
                        color = CentraliaColors.SecondaryText
                    )

                    if (stage != null) {
                        Text(
                            text = stage.displayName,
                            style = CentraliaType.subheadline,
                            color = CentraliaColors.SecondaryText
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = CentraliaIcons.Check,
                                contentDescription = null,
                                tint = CentraliaColors.SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Metadata ready",
                                style = CentraliaType.subheadline,
                                color = CentraliaColors.SecondaryText
                            )
                        }
                    }
                }

                stage != null -> {
                    Text(
                        text = stage.displayName,
                        style = CentraliaType.headline,
                        color = CentraliaColors.Ink
                    )
                    StageProgress(stage = stage)
                }
            }
        }

        if (errorMessage == null && stage == null) {
            Icon(
                imageVector = CentraliaIcons.Check,
                contentDescription = null,
                tint = CentraliaColors.Ink,
                modifier = Modifier
                    .size(24.dp)
                    .semanticsHidden()
            )
        } else if (stage != null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(22.dp)
                    .semanticsHidden(),
                color = CentraliaColors.Ink,
                strokeWidth = 2.dp
            )
        }
    }
}

/** `thumbnail` — a 76x102 platform-tinted tile with a circular glyph. */
@Composable
private fun ImportThumbnail(
    platform: VideoPlatform?,
    isError: Boolean
) {
    val thumbnailColor = when (platform) {
        VideoPlatform.TIKTOK -> CentraliaColors.VideoSand
        VideoPlatform.INSTAGRAM_REEL -> CentraliaColors.VideoMint
        VideoPlatform.YOUTUBE_SHORT -> CentraliaColors.VideoClay
        null -> CentraliaColors.Divider
    }

    Box(
        modifier = Modifier
            .width(76.dp)
            .height(102.dp)
            .background(thumbnailColor, RoundedCornerShape(12.dp))
            .semanticsHidden(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(CentraliaColors.Ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isError) CentraliaIcons.Link else CentraliaIcons.Play,
                contentDescription = null,
                tint = CentraliaColors.Surface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** `stageProgress` — four 22x4 bars, filled up to the current stage. */
@Composable
private fun StageProgress(stage: VideoImportStage) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        VideoImportStage.entries.forEach { item ->
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(4.dp)
                    .background(
                        color = if (item.ordinal <= stage.ordinal) {
                            CentraliaColors.Ink
                        } else {
                            CentraliaColors.Divider
                        },
                        shape = CapsuleShape
                    )
            )
        }
    }
}

/** `SaveTagChip` — an ink capsule with a remove affordance. */
@Composable
fun SaveTagChip(
    tag: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(CentraliaColors.Ink, CapsuleShape)
            .padding(start = 14.dp, end = 10.dp)
    ) {
        Text(
            text = tag,
            style = CentraliaType.subheadline.semibold(),
            color = CentraliaColors.Canvas
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .centraliaPressable(onClickLabel = "Remove $tag", onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CentraliaIcons.Close,
                contentDescription = null,
                tint = CentraliaColors.Canvas,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** `struct SaveFolderPickerSheet` — pick a destination or create one inline. */
@Composable
fun SaveFolderPickerSheet(
    folders: List<LibraryFolder>,
    selectedFolderID: UUID?,
    folderFailureMessage: String?,
    onSelectFolder: (UUID?) -> Unit,
    onCreateFolder: (String, FolderSymbol) -> Unit,
    onDismiss: () -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    var selectedSymbol by remember { mutableStateOf(FolderSymbol.FOLDER) }

    CentraliaSheet(
        title = "Choose a folder",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
                .imePadding()
        ) {
            SheetSectionHeader(title = "Destination")

            SheetListRow(
                title = "Unorganized",
                leadingIcon = CentraliaIcons.Tray,
                isSelected = selectedFolderID == null,
                onClick = {
                    onSelectFolder(null)
                    onDismiss()
                }
            )

            folders.forEach { folder ->
                SheetListRow(
                    title = folder.name,
                    leadingIcon = folderSymbolIcon(folder.symbolName),
                    isSelected = selectedFolderID == folder.id,
                    onClick = {
                        onSelectFolder(folder.id)
                        onDismiss()
                    }
                )
            }

            SheetSectionHeader(title = "New Folder")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                CentraliaBorderedTextField(
                    placeholder = "Folder name",
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    capitalization = KeyboardCapitalization.Words,
                    onSubmit = { onCreateFolder(newFolderName, selectedSymbol) }
                )

                FolderSymbolPicker(
                    selection = selectedSymbol,
                    onSelect = { selectedSymbol = it }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xSmall)
                ) {
                    Icon(
                        imageVector = CentraliaIcons.NewFolder,
                        contentDescription = null,
                        tint = if (newFolderName.trim().isEmpty()) {
                            CentraliaColors.SecondaryText
                        } else {
                            CentraliaColors.Ink
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    CentraliaTextButton(
                        title = "Create and Select",
                        isDisabled = newFolderName.trim().isEmpty(),
                        onClick = { onCreateFolder(newFolderName, selectedSymbol) }
                    )
                }

                folderFailureMessage?.let { SheetErrorText(message = it) }
            }
        }
    }
}

/** `struct SaveTagEditorSheet` — add a tag by hand or accept a suggestion. */
@Composable
fun SaveTagEditorSheet(
    availableTagSuggestions: List<String>,
    onAddTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tag by remember { mutableStateOf("") }
    val tagFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { tagFocusRequester.requestFocus() } }

    val addManualTag = {
        val trimmedTag = tag.trim()
        if (trimmedTag.isNotEmpty()) {
            onAddTag(trimmedTag)
            tag = ""
        }
    }

    CentraliaSheet(
        title = "Add tag",
        onDismissRequest = onDismiss,
        trailingAction = SheetAction(title = "Done", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
                .imePadding()
        ) {
            SheetSectionHeader(title = "Add your own tag")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
            ) {
                CentraliaBorderedTextField(
                    placeholder = "Tag",
                    value = tag,
                    onValueChange = { tag = it },
                    minHeight = 52.dp,
                    focusRequester = tagFocusRequester,
                    onSubmit = addManualTag,
                    modifier = Modifier.weight(1f)
                )

                CentraliaTextButton(
                    title = "Add Tag",
                    isDisabled = tag.trim().isEmpty(),
                    onClick = addManualTag
                )
            }

            if (availableTagSuggestions.isNotEmpty()) {
                SheetSectionHeader(title = "Suggestions")

                availableTagSuggestions.forEach { suggestion ->
                    SheetListRow(
                        title = suggestion,
                        leadingIcon = CentraliaIcons.AddCircle,
                        onClick = { onAddTag(suggestion) }
                    )
                }

                SheetSectionFooter(text = "Suggestions are never added automatically.")
            }
        }
    }
}
