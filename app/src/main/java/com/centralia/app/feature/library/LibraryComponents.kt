package com.centralia.app.feature.library

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.centralia.app.R
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.ui.components.CapsuleShape
import com.centralia.app.ui.components.CentraliaCircularIconButton
import com.centralia.app.ui.components.CentraliaDivider
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.folderSymbolIcon
import com.centralia.app.ui.components.rememberShareLink
import com.centralia.app.ui.components.scaledDp
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.medium
import com.centralia.app.ui.theme.semibold

/** `LibraryHeader` — mark, wordmark, and the circular save action. */
@Composable
fun LibraryHeader(
    onPresentSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.centralia_mark),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .semanticsHidden()
        )

        Text(
            text = "Centralia",
            style = CentraliaType.brand,
            color = CentraliaColors.Ink,
            modifier = Modifier.weight(1f)
        )

        CentraliaCircularIconButton(
            contentDescription = "Save a short",
            diameter = 54,
            onClick = onPresentSave
        ) {
            Icon(
                imageVector = CentraliaIcons.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/** `LibrarySearchEntry` — the tappable field that switches to the Search tab. */
@Composable
fun LibrarySearchEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .centraliaPressable(onClickLabel = "Search your library", onClick = onClick)
            .background(CentraliaColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(18.dp))
            .padding(horizontal = Spacing.medium)
    ) {
        Icon(
            imageVector = CentraliaIcons.Search,
            contentDescription = null,
            tint = CentraliaColors.SecondaryText,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = "Search your library",
            style = CentraliaType.body,
            color = CentraliaColors.SecondaryText
        )
    }
}

/** `FolderShortcut` — the horizontal folder pill on the library. */
@Composable
fun FolderShortcut(
    folder: LibraryFolder,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier
            .defaultMinSize(minHeight = 58.dp)
            .centraliaPressable(onClick = onClick)
            .background(CentraliaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(16.dp))
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        Icon(
            imageVector = folderSymbolIcon(folder.symbolName),
            contentDescription = null,
            tint = CentraliaColors.Ink,
            modifier = Modifier.size(24.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = folder.name,
                style = CentraliaType.subheadline.semibold(),
                color = CentraliaColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = shortsCountLabel(itemCount),
                style = CentraliaType.caption,
                color = CentraliaColors.SecondaryText
            )
        }
    }
}

/**
 * `LibraryVideoCard` — a 150dp colour preview with platform and duration
 * capsules, then a 76dp metadata strip with the overflow menu.
 */
@Composable
fun LibraryVideoCard(
    video: VideoItem,
    styleIndex: Int,
    isPlaying: Boolean,
    onOpenDetail: () -> Unit,
    onTogglePlayback: () -> Unit,
    onRequestMove: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = CentraliaColors.videoCardPalette[styleIndex.mod(4)]
    val previewHeight = scaledDp(150.dp)
    val metadataHeight = scaledDp(76.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .centraliaPressable(
                    onClickLabel = "Open ${video.displayTitle}",
                    onClick = onOpenDetail
                )
        ) {
            // Centred play / pause control.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(58.dp)
                    .centraliaPressable(
                        onClickLabel = if (isPlaying) "Pause preview" else "Play preview",
                        onClick = onTogglePlayback
                    )
                    .background(CentraliaColors.Ink.copy(alpha = 0.92f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) CentraliaIcons.Pause else CentraliaIcons.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            PlatformBadge(
                text = video.platform.displayName,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )

            DurationBadge(
                text = video.formattedDuration,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(metadataHeight)
                .padding(end = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .centraliaPressable(onClick = onOpenDetail)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = video.displayTitle,
                    style = CentraliaType.subheadline.bold(),
                    color = CentraliaColors.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = video.creator,
                    style = CentraliaType.caption.medium(),
                    color = CentraliaColors.SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 36.dp)
                )
            }

            VideoOverflowMenu(
                video = video,
                deleteTitle = "Delete",
                onRequestMove = onRequestMove,
                onRequestDelete = onRequestDelete,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * The `Menu` on a card: move to folder, share the source link, delete.
 * `ShareLink` becomes an `ACTION_SEND` chooser.
 */
@Composable
fun VideoOverflowMenu(
    video: VideoItem,
    deleteTitle: String,
    onRequestMove: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val shareLink = rememberShareLink()

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .centraliaPressable(
                    onClickLabel = "More actions for ${video.displayTitle}"
                ) { isExpanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CentraliaIcons.More,
                contentDescription = null,
                tint = CentraliaColors.SecondaryText
            )
        }

        // The menu picks up CentraliaColors.Surface from the theme's colour scheme.
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Move to Folder", style = CentraliaType.body) },
                leadingIcon = {
                    Icon(CentraliaIcons.Folder, contentDescription = null)
                },
                onClick = {
                    isExpanded = false
                    onRequestMove()
                }
            )

            DropdownMenuItem(
                text = { Text("Share Link", style = CentraliaType.body) },
                leadingIcon = {
                    Icon(CentraliaIcons.Share, contentDescription = null)
                },
                onClick = {
                    isExpanded = false
                    shareLink(
                        video.displayTitle,
                        "${video.displayTitle} — ${video.creator}",
                        video.sourceURL
                    )
                }
            )

            CentraliaDivider()

            DropdownMenuItem(
                text = {
                    Text(deleteTitle, style = CentraliaType.body, color = CentraliaColors.Error)
                },
                leadingIcon = {
                    Icon(
                        CentraliaIcons.Delete,
                        contentDescription = null,
                        tint = CentraliaColors.Error
                    )
                },
                onClick = {
                    isExpanded = false
                    onRequestDelete()
                }
            )
        }
    }
}

/** The light capsule naming the platform, top-left of a preview. */
@Composable
fun PlatformBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = CentraliaType.caption.semibold(),
    minHeight: androidx.compose.ui.unit.Dp = 30.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 10.dp
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .background(CentraliaColors.Surface.copy(alpha = 0.94f), CapsuleShape)
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = style, color = CentraliaColors.Ink)
    }
}

/** The dark capsule showing the running time, bottom-right of a preview. */
@Composable
fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = CentraliaType.caption.semibold(),
    minHeight: androidx.compose.ui.unit.Dp = 28.dp,
    backgroundAlpha: Float = 0.88f
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .background(CentraliaColors.Ink.copy(alpha = backgroundAlpha), CapsuleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = style, color = Color.White)
    }
}

/** `"\(count) \(count == 1 ? "short" : "shorts")"`. */
fun shortsCountLabel(count: Int): String =
    if (count == 1) "1 short" else "$count shorts"
