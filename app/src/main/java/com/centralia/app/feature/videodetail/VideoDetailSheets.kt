package com.centralia.app.feature.videodetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.centralia.app.ui.components.CentraliaBorderedTextField
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.CentraliaTextArea
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.SheetListRow
import com.centralia.app.ui.components.SheetSectionFooter
import com.centralia.app.ui.components.SheetSectionHeader
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing

/**
 * `private struct VideoTagEditorSheet` — the selected tags, a field to add one,
 * and the library's other tags. Edits are local until Save, matching the Swift
 * sheet's `@State private var selectedTags`.
 */
@Composable
fun VideoTagEditorSheet(
    selectedTags: List<String>,
    availableTags: List<String>,
    isMutating: Boolean,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val workingTags = remember { selectedTags.toMutableStateList() }
    var newTag by remember { mutableStateOf("") }
    val newTagFocusRequester = remember { FocusRequester() }

    val addNewTag = {
        val tag = newTag.trim()
        if (tag.isNotEmpty()) {
            if (workingTags.none { it.equals(tag, ignoreCase = true) }) {
                workingTags.add(tag)
            }
            newTag = ""
        }
    }

    val unselectedAvailableTags = availableTags.filter { availableTag ->
        workingTags.none { it.equals(availableTag, ignoreCase = true) }
    }

    CentraliaSheet(
        title = "Edit Tags",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(
            title = "Cancel",
            isEnabled = !isMutating,
            onClick = onDismiss
        ),
        trailingAction = SheetAction(
            title = if (isMutating) "Saving…" else "Save",
            isEnabled = !isMutating,
            onClick = { onSave(workingTags.toList()) }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
                .imePadding()
        ) {
            SheetSectionHeader(title = "Selected tags")

            if (workingTags.isEmpty()) {
                Text(
                    text = "No tags selected",
                    style = CentraliaType.body,
                    color = CentraliaColors.SecondaryText,
                    modifier = Modifier.padding(
                        horizontal = Spacing.medium,
                        vertical = Spacing.small
                    )
                )
            } else {
                workingTags.toList().forEach { tag ->
                    SheetListRow(
                        title = tag,
                        leadingIcon = CentraliaIcons.CheckCircle,
                        trailingIcon = CentraliaIcons.Close,
                        trailingIconDescription = "Removes this tag",
                        onClick = {
                            workingTags.removeAll { it.equals(tag, ignoreCase = true) }
                        }
                    )
                }
            }

            SheetSectionHeader(title = "Add a tag")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
            ) {
                CentraliaBorderedTextField(
                    placeholder = "New tag",
                    value = newTag,
                    onValueChange = { newTag = it },
                    minHeight = 52.dp,
                    focusRequester = newTagFocusRequester,
                    onSubmit = addNewTag,
                    modifier = Modifier.weight(1f)
                )

                CentraliaTextButton(
                    title = "Add",
                    isDisabled = newTag.trim().isEmpty(),
                    onClick = addNewTag
                )
            }

            if (unselectedAvailableTags.isNotEmpty()) {
                SheetSectionHeader(title = "Available tags")

                unselectedAvailableTags.forEach { tag ->
                    SheetListRow(
                        title = tag,
                        leadingIcon = CentraliaIcons.AddCircle,
                        trailingIconDescription = "Adds this tag",
                        onClick = { workingTags.add(tag) }
                    )
                }
            }
        }
    }
}

/** `private struct VideoNoteEditorSheet`. */
@Composable
fun VideoNoteEditorSheet(
    initialNote: String,
    hasExistingNote: Boolean,
    isMutating: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }
    val noteFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { noteFocusRequester.requestFocus() } }

    CentraliaSheet(
        title = if (hasExistingNote) "Edit Note" else "Add Note",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(
            title = "Cancel",
            isEnabled = !isMutating,
            onClick = onDismiss
        ),
        trailingAction = SheetAction(
            title = if (isMutating) "Saving…" else "Save",
            isEnabled = !isMutating,
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
                placeholder = "",
                value = note,
                onValueChange = { note = it },
                minHeight = 180.dp,
                focusRequester = noteFocusRequester,
                contentDescription = "Personal note"
            )
        }
    }
}

/** `private struct VideoReportSheet`. */
@Composable
fun VideoReportSheet(
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val issues = remember {
        listOf(
            "Video is unavailable",
            "Metadata is incorrect",
            "Something else"
        )
    }
    var selectedIssue by remember { mutableStateOf(issues.first()) }
    var details by remember { mutableStateOf("") }

    CentraliaSheet(
        title = "Report Video",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss),
        trailingAction = SheetAction(title = "Submit", onClick = onSubmit)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
                .imePadding()
        ) {
            SheetSectionHeader(title = "What went wrong?")

            // `.pickerStyle(.inline)` — every option visible, one selected.
            issues.forEach { issue ->
                SheetListRow(
                    title = issue,
                    isSelected = selectedIssue == issue,
                    onClick = { selectedIssue = issue }
                )
            }

            SheetSectionHeader(title = "Details (optional)")

            CentraliaTextArea(
                placeholder = "Tell us what happened",
                value = details,
                onValueChange = { details = it },
                minHeight = 110.dp,
                modifier = Modifier.padding(horizontal = Spacing.medium)
            )

            SheetSectionFooter(
                text = "Reports are recorded locally for this demo session."
            )
        }
    }
}
