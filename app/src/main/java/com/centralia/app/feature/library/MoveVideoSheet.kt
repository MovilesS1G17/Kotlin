package com.centralia.app.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.SheetListRow
import com.centralia.app.ui.components.folderSymbolIcon
import com.centralia.app.ui.theme.Spacing
import java.util.UUID

/**
 * `struct MoveVideoSheet` — Unorganized followed by every folder.
 *
 * The iOS sheet is presented with an item binding (`sheet(item:)`), so the caller
 * here shows it only while a video is pending and passes that video's folder in.
 */
@Composable
fun MoveVideoSheet(
    folders: List<LibraryFolder>,
    onMove: (UUID?) -> Unit,
    onDismiss: () -> Unit,
    currentFolderID: UUID? = null
) {
    CentraliaSheet(
        title = "Move Video",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.small, horizontal = 0.dp)
                .navigationBarsPadding()
        ) {
            SheetListRow(
                title = "Unorganized",
                leadingIcon = CentraliaIcons.Tray,
                isSelected = currentFolderID == null,
                onClick = {
                    onMove(null)
                    onDismiss()
                }
            )

            folders.forEach { folder ->
                SheetListRow(
                    title = folder.name,
                    leadingIcon = folderSymbolIcon(folder.symbolName),
                    isSelected = currentFolderID == folder.id,
                    onClick = {
                        onMove(folder.id)
                        onDismiss()
                    }
                )
            }
        }
    }
}
