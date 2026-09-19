package com.centralia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Radius

/**
 * `CentraliaPrimaryButton` — ink fill, white label, 52dp minimum height, and a
 * spinner that replaces the label in place while loading.
 */
@Composable
fun CentraliaPrimaryButton(
    title: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    minHeight: Int = 52,
    cornerRadius: androidx.compose.ui.unit.Dp = Radius.control,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .alpha(if (isDisabled && !isLoading) 0.48f else 1f)
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight.dp)
            .centraliaPressable(enabled = !isDisabled && !isLoading, onClick = onClick)
            .background(CentraliaColors.Ink, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = title,
                style = CentraliaType.headline,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * `CentraliaSecondaryButton` — white fill with a hairline divider border and a
 * leading 22dp icon slot, used by the social sign-in rows.
 */
@Composable
fun CentraliaSecondaryButton(
    title: String,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .alpha(if (isDisabled) 0.48f else 1f)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .centraliaPressable(enabled = !isDisabled, onClick = onClick)
            .background(CentraliaColors.Surface, RoundedCornerShape(Radius.control))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(Radius.control)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Text(
                text = title,
                style = CentraliaType.headline,
                color = CentraliaColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The bordered surface button used for "Add Note" and "Skip": white fill, divider
 * border, ink label.
 */
@Composable
fun CentraliaBorderedButton(
    title: String,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
    minHeight: Int = 58,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .alpha(if (isDisabled) 0.48f else 1f)
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight.dp)
            .centraliaPressable(enabled = !isDisabled, onClick = onClick)
            .background(CentraliaColors.Surface, RoundedCornerShape(cornerRadius))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = CentraliaType.headline,
            color = CentraliaColors.Ink,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * A borderless text action, standing in for a plain SwiftUI `Button("…")`. The
 * 44dp minimum height preserves the iOS touch target the originals set with
 * `.frame(minHeight: 44)`.
 */
@Composable
fun CentraliaTextButton(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = CentraliaColors.Ink,
    style: androidx.compose.ui.text.TextStyle = CentraliaType.headline,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .alpha(if (isDisabled) 0.48f else 1f)
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .centraliaPressable(enabled = !isDisabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, style = style, color = color)
    }
}

/**
 * `Button(action:) { Image(systemName: "plus") … .background(Color.centraliaInk, in: Circle()) }`
 * — the circular ink action on the library header.
 */
@Composable
fun CentraliaCircularIconButton(
    contentDescription: String,
    diameter: Int,
    modifier: Modifier = Modifier,
    background: Color = CentraliaColors.Ink,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(diameter.dp)
            .centraliaPressable(onClickLabel = contentDescription, onClick = onClick)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
