package com.centralia.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType

/**
 * A `Menu` whose label is a filter capsule, used by the four Global Search facets
 * and the folder-detail sort control. SwiftUI's `Menu` becomes a chip that opens
 * a [DropdownMenu]; [menuContent] receives a dismiss callback so each item can
 * close the menu as `Button` inside a `Menu` does automatically.
 */
@Composable
fun CentraliaFilterMenu(
    title: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        CentraliaChip(
            title = title,
            isSelected = isActive,
            contentDescription = contentDescription,
            trailingIcon = CentraliaIcons.ChevronDown,
            onClick = { isExpanded = true }
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            menuContent { isExpanded = false }
        }
    }
}

/**
 * `selectionButton(_:isSelected:action:)` — a menu row that shows a leading
 * checkmark when it is the current choice.
 */
@Composable
fun MenuSelectionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text = title, style = CentraliaType.body, color = CentraliaColors.Ink)
        },
        leadingIcon = {
            Box(modifier = Modifier.size(24.dp)) {
                if (isSelected) {
                    Icon(
                        imageVector = CentraliaIcons.Check,
                        contentDescription = null,
                        tint = CentraliaColors.Ink
                    )
                }
            }
        },
        onClick = onClick
    )
}

/**
 * A plain menu row with an optional icon, for actions rather than selections.
 *
 * [onClick] is deliberately the last parameter so call sites can pass it as a
 * trailing lambda.
 */
@Composable
fun MenuActionItem(
    title: String,
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (isDestructive) CentraliaColors.Error else CentraliaColors.Ink

    DropdownMenuItem(
        text = { Text(text = title, style = CentraliaType.body, color = tint) },
        leadingIcon = if (icon == null) {
            null
        } else {
            { Icon(imageVector = icon, contentDescription = null, tint = tint) }
        },
        onClick = onClick
    )
}
