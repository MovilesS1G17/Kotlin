package com.centralia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.centralia.app.domain.library.UndoMethod
import com.centralia.app.platform.OnShake
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.semibold
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/** SwiftUI's `Capsule()`. */
val CapsuleShape: Shape = RoundedCornerShape(percent = 50)

/**
 * `ContentUnavailableView` — a centred icon, a bold title, an optional
 * description and an optional action. The SwiftUI call sites pin a minimum
 * height, which [minHeight] carries over so the empty state occupies the same
 * space as the content it replaces.
 */
@Composable
fun ContentUnavailable(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    minHeight: Dp = 280.dp,
    actions: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(Spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CentraliaColors.SecondaryText,
                modifier = Modifier.size(52.dp)
            )

            Text(
                text = title,
                style = CentraliaType.title3.bold(),
                color = CentraliaColors.Ink,
                textAlign = TextAlign.Center
            )

            if (description != null) {
                Text(
                    text = description,
                    style = CentraliaType.subheadline,
                    color = CentraliaColors.SecondaryText,
                    textAlign = TextAlign.Center
                )
            }

            if (actions != null) {
                Box(modifier = Modifier.padding(top = Spacing.small)) {
                    actions()
                }
            }
        }
    }
}

/**
 * `Button("Try Again").buttonStyle(.borderedProminent).tint(Color.centraliaInk)`
 * — the prominent recovery action inside an empty or failure state.
 */
@Composable
fun ProminentAction(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .centraliaPressable(onClick = onClick)
            .background(CentraliaColors.Ink, CapsuleShape)
            .padding(horizontal = Spacing.large, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, style = CentraliaType.headline, color = CentraliaColors.Surface)
    }
}

/**
 * The selectable capsule shared by `LibraryFilterChip`, the search filter menus
 * and the folder-detail tag button: ink fill with a white label when selected,
 * soft surface with a hairline border when not.
 */
@Composable
fun CentraliaChip(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .centraliaPressable(onClickLabel = contentDescription, onClick = onClick)
            .background(
                color = if (isSelected) CentraliaColors.Ink else CentraliaColors.SoftSurface,
                shape = CapsuleShape
            )
            .then(
                if (isSelected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, CentraliaColors.Divider, CapsuleShape)
                }
            )
            .padding(horizontal = Spacing.medium, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = CentraliaType.subheadline.semibold(),
            color = if (isSelected) Color.White else CentraliaColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = if (isSelected) Color.White else CentraliaColors.Ink,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** `ProgressView("Loading your library…")`. */
@Composable
fun CentraliaLoadingState(
    label: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 280.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            CircularProgressIndicator(color = CentraliaColors.Ink)
            Text(
                text = label,
                style = CentraliaType.subheadline,
                color = CentraliaColors.SecondaryText
            )
        }
    }
}

/**
 * `.alert(title, isPresented:) { Button("OK") … } message: { … }`. Android has no
 * separate action sheet, so `.confirmationDialog` maps here too, with
 * [isDestructive] tinting the confirm action.
 */
@Composable
fun CentraliaAlert(
    title: String,
    message: String?,
    confirmTitle: String = "OK",
    dismissTitle: String? = null,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onConfirm
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CentraliaColors.Surface,
        titleContentColor = CentraliaColors.Ink,
        textContentColor = CentraliaColors.SecondaryText,
        title = {
            Text(text = title, style = CentraliaType.headline, color = CentraliaColors.Ink)
        },
        text = if (message == null) {
            null
        } else {
            {
                Text(
                    text = message,
                    style = CentraliaType.subheadline,
                    color = CentraliaColors.SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmTitle,
                    style = CentraliaType.headline,
                    color = if (isDestructive) CentraliaColors.Error else CentraliaColors.Ink
                )
            }
        },
        dismissButton = if (dismissTitle == null) {
            null
        } else {
            {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = dismissTitle,
                        style = CentraliaType.body,
                        color = CentraliaColors.SecondaryText
                    )
                }
            }
        }
    )
}

/**
 * Shake to undo: the pop-up shown after any short is deleted (Library, Search,
 * a folder or the detail screen). The text comes from the backend's answer to
 * the delete ("If you want to undo the delete, just shake the phone."). While
 * it is up the accelerometer listens: shaking undoes the delete, like the Undo
 * button. It closes by itself when the backend's undo window ends. Mirrors
 * `ShakeToUndoPopup` in the iOS app.
 */
@Composable
fun ShakeToUndoPopup(
    message: String,
    expiresAt: Instant?,
    onUndo: (UndoMethod) -> Unit,
    onDismiss: () -> Unit
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(expiresAt) {
        val until = expiresAt ?: return@LaunchedEffect
        delay(Duration.between(Instant.now(), until).toMillis().coerceAtLeast(0))
        currentOnDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CentraliaColors.Surface,
        icon = {
            Icon(
                imageVector = CentraliaIcons.Delete,
                contentDescription = null,
                tint = CentraliaColors.Ink
            )
        },
        title = {
            Text(
                text = "Video deleted",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink,
                textAlign = TextAlign.Center
            )
        },
        text = {
            // Sensor feature: shaking the phone while the pop-up is up undoes the delete.
            OnShake(onShake = { onUndo(UndoMethod.SHAKE) })
            Text(
                text = message,
                style = CentraliaType.subheadline,
                color = CentraliaColors.SecondaryText,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = { onUndo(UndoMethod.BUTTON) }) {
                Text(text = "Undo", style = CentraliaType.headline, color = CentraliaColors.Ink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss", style = CentraliaType.body, color = CentraliaColors.SecondaryText)
            }
        }
    )
}

/** A hairline rule, matching SwiftUI's `Divider()` against the warm palette. */
@Composable
fun CentraliaDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CentraliaColors.Divider)
    )
}
