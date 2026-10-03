package com.centralia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold

/** One toolbar action on a sheet header (`Cancel`, `Save`, `Done`, `Reset`). */
data class SheetAction(
    val title: String,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * The SwiftUI sheets are a `NavigationStack` with an inline navigation title and
 * cancel/confirm toolbar items, presented at `.medium`/`.large` detents.
 *
 * A Material [ModalBottomSheet] is the equivalent presentation on Android: it
 * starts partially expanded and can be dragged to full height, and dismisses by
 * swipe or scrim tap. The navigation bar becomes the header row below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentraliaSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    leadingAction: SheetAction? = null,
    trailingAction: SheetAction? = null,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CentraliaColors.Canvas,
        contentColor = CentraliaColors.Ink,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(
                title = title,
                leadingAction = leadingAction,
                trailingAction = trailingAction
            )
            content()
        }
    }
}

/**
 * `.presentationDragIndicator(.visible)` plus the inline navigation title and its
 * two toolbar placements.
 */
@Composable
private fun SheetHeader(
    title: String,
    leadingAction: SheetAction?,
    trailingAction: SheetAction?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Drag indicator.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.small),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(CentraliaColors.Divider, CapsuleShape)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.small, vertical = Spacing.small)
        ) {
            Box(modifier = Modifier.defaultMinSize(minWidth = 72.dp)) {
                if (leadingAction != null) {
                    CentraliaTextButton(
                        title = leadingAction.title,
                        style = CentraliaType.body,
                        isDisabled = !leadingAction.isEnabled,
                        onClick = leadingAction.onClick
                    )
                }
            }

            Text(
                text = title,
                style = CentraliaType.headline,
                color = CentraliaColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier.defaultMinSize(minWidth = 72.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (trailingAction != null) {
                    CentraliaTextButton(
                        title = trailingAction.title,
                        style = CentraliaType.headline,
                        isDisabled = !trailingAction.isEnabled,
                        onClick = trailingAction.onClick
                    )
                }
            }
        }

        CentraliaDivider()
    }
}

/**
 * A row inside a sheet's `List`, with an optional leading icon and a trailing
 * checkmark for the selected entry.
 */
@Composable
fun SheetListRow(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isSelected: Boolean = false,
    trailingIcon: ImageVector? = null,
    trailingIconDescription: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .centraliaPressable(onClick = onClick)
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = CentraliaColors.Ink,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            style = CentraliaType.body,
            color = CentraliaColors.Ink,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = CentraliaIcons.Check,
                contentDescription = "Selected",
                tint = CentraliaColors.Ink,
                modifier = Modifier.size(20.dp)
            )
        } else if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingIconDescription,
                tint = CentraliaColors.SecondaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** `Section("…")` header inside a sheet list. */
@Composable
fun SheetSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = CentraliaType.caption.semibold(),
        color = CentraliaColors.SecondaryText,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.medium,
                end = Spacing.medium,
                top = Spacing.medium,
                bottom = Spacing.xSmall
            )
    )
}

/** `Section { … } footer: { Text("…") }`. */
@Composable
fun SheetSectionFooter(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = CentraliaType.footnote,
        color = CentraliaColors.SecondaryText,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
    )
}

/** Inline red validation copy inside a sheet. */
@Composable
fun SheetErrorText(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        style = CentraliaType.footnote,
        color = CentraliaColors.Error,
        modifier = modifier.semanticsLabel("Error: $message")
    )
}
