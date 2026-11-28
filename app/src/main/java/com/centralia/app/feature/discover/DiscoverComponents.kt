package com.centralia.app.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.feature.videodetail.thumbnailColor
import com.centralia.app.ui.components.CapsuleShape
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.VideoCoverImage
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold

/** The five Sprint 4 views, reachable from the "Discover" row on the Library. */
enum class DiscoverDestination(val title: String, val route: String, val icon: ImageVector) {
    WATCH_LATER("Watch later", "watchLater", Icons.Outlined.BookmarkBorder),
    REDISCOVER("Rediscover", "rediscover", Icons.Outlined.Restore),
    CREATORS("Creators", "creators", Icons.Outlined.Groups),
    TRENDS("Trends", "trends", Icons.Outlined.Whatshot),
    INSIGHTS("Insights", "insights", Icons.Outlined.BarChart)
}

/** A row of capsules on the Library, one per Sprint 4 view. */
@Composable
fun DiscoverShortcutsRow(
    onOpen: (DiscoverDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        for (destination in DiscoverDestination.entries) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .centraliaPressable(onClickLabel = "Opens ${destination.title}") { onOpen(destination) }
                    .background(CentraliaColors.Surface, CapsuleShape)
                    .border(1.dp, CentraliaColors.Divider, CapsuleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(destination.icon, contentDescription = null, tint = CentraliaColors.Ink, modifier = Modifier.size(18.dp))
                Text(destination.title, style = CentraliaType.subheadline.semibold(), color = CentraliaColors.Ink)
            }
        }
    }
}

/** Back button, large title and a one-line description: the top of every Sprint 4 view. */
@Composable
fun FeatureHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .centraliaPressable(onClickLabel = "Back", onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(CentraliaIcons.Back, contentDescription = "Back", tint = CentraliaColors.Ink)
            }
            Text(
                text = title,
                style = CentraliaType.display,
                color = CentraliaColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke(this)
        }
        Text(text = subtitle, style = CentraliaType.subheadline, color = CentraliaColors.SecondaryText)
    }
}

/** A short as a list row: small cover, title (one or two lines), a detail line and trailing actions. */
@Composable
fun ShortRow(
    video: VideoItem,
    detail: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(CentraliaColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(thumbnailColor(video.platform))
                .centraliaPressable(onClickLabel = "Opens ${video.displayTitle}", onClick = onOpen)
        ) {
            VideoCoverImage(url = video.coverURL, modifier = Modifier.fillMaxSize())
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .centraliaPressable(onClickLabel = "Opens ${video.displayTitle}", onClick = onOpen)
        ) {
            Text(
                text = video.displayTitle,
                style = CentraliaType.subheadline.semibold(),
                color = CentraliaColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${video.creator} · ${video.platform.displayName}",
                style = CentraliaType.caption,
                color = CentraliaColors.SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = CentraliaType.caption,
                color = CentraliaColors.SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke(this)
    }
}

/** A small text action used on the rows ("Not now", "Remove"). */
@Composable
fun RowTextAction(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .centraliaPressable(onClickLabel = title, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, style = CentraliaType.footnote.semibold(), color = CentraliaColors.Ink)
    }
}

/** A white rounded card for a block of content (Trends, Insights). */
@Composable
fun FeatureCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .background(CentraliaColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(20.dp))
            .padding(Spacing.medium)
    ) { content() }
}

/** A labelled horizontal bar: label, bar proportional to [fraction], value. */
@Composable
fun LabeledBar(label: String, value: String, fraction: Float, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = CentraliaType.subheadline,
            color = CentraliaColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.38f)
        )
        Box(
            modifier = Modifier
                .weight(0.47f)
                .height(10.dp)
                .clip(CapsuleShape)
                .background(CentraliaColors.SoftSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(CentraliaColors.Ink, CapsuleShape)
            )
        }
        Text(value, style = CentraliaType.footnote.semibold(), color = CentraliaColors.Ink, modifier = Modifier.weight(0.15f))
    }
}
