package com.centralia.app.feature.creators

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.creators.CreatorSummary
import com.centralia.app.domain.creators.CreatorsCache
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.discover.FeatureHeader
import com.centralia.app.feature.discover.ShortRow
import com.centralia.app.feature.library.LoadState
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.CentraliaSearchField
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold
import java.time.Duration
import java.time.Instant

/** Sprint 4 view (Alejandro): saved shorts grouped by creator. */
@Composable
fun CreatorsScreen(
    videoRepository: VideoItemRepository,
    cache: CreatorsCache,
    analytics: AnalyticsTracker,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "creators") { CreatorsViewModel(videoRepository, cache, analytics) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Spacing.small, bottom = Spacing.xLarge),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            CenteredContent {
                FeatureHeader(
                    title = "Creators",
                    subtitle = "Everyone you've saved shorts from, most saved first.",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.medium)
                )
            }
        }
        when (val load = state.state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                CentraliaLoadingState(label = "Grouping your shorts by creator…", minHeight = 360.dp)
            }
            is LoadState.Failed -> item(key = "failed") {
                ContentUnavailable(
                    title = "Creators unavailable",
                    icon = CentraliaIcons.Warning,
                    description = load.message,
                    actions = { ProminentAction(title = "Try Again", onClick = viewModel::load) }
                )
            }
            LoadState.Loaded -> if (state.creators.isEmpty()) {
                item(key = "empty") {
                    ContentUnavailable(
                        title = "No creators yet",
                        icon = Icons.Outlined.Groups,
                        description = "Save a short and its creator will show up here."
                    )
                }
            } else {
                item(key = "search") {
                    CenteredContent {
                        CentraliaSearchField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = "Search creators",
                            modifier = Modifier.padding(horizontal = Spacing.medium)
                        )
                    }
                }
                items(items = state.visible, key = { "creator-${it.handle.lowercase()}" }, contentType = { "creator" }) { creator ->
                    CenteredContent {
                        CreatorCard(
                            creator = creator,
                            isExpanded = state.expandedHandle == creator.handle,
                            onToggle = { viewModel.toggle(creator) },
                            onOpenVideo = onOpenVideo,
                            modifier = Modifier.padding(horizontal = Spacing.medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorCard(
    creator: CreatorSummary,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small), modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .centraliaPressable(onClickLabel = if (isExpanded) "Hides ${creator.handle}'s shorts" else "Shows ${creator.handle}'s shorts", onClick = onToggle)
                .background(CentraliaColors.Surface, RoundedCornerShape(18.dp))
                .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CentraliaColors.SoftSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(creator.initial, style = CentraliaType.title3.semibold(), color = CentraliaColors.Ink)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(creator.handle, style = CentraliaType.headline, color = CentraliaColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (creator.count == 1) "1 short" else "${creator.count} shorts"} · " +
                        creator.platforms.joinToString(" · ") { it.filterName } + " · " + lastSavedLabel(creator.lastSavedAt),
                    style = CentraliaType.caption,
                    color = CentraliaColors.SecondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (isExpanded) CentraliaIcons.ChevronDown else CentraliaIcons.ChevronRight,
                contentDescription = null,
                tint = CentraliaColors.SecondaryText
            )
        }
        if (isExpanded) {
            for (video in creator.videos) {
                ShortRow(video = video, detail = lastSavedLabel(video.savedAt), onOpen = { onOpenVideo(video) })
            }
        }
    }
}

internal fun lastSavedLabel(at: Instant, now: Instant = Instant.now()): String {
    val days = Duration.between(at, now).toDays()
    return when {
        days <= 0 -> "saved today"
        days == 1L -> "saved yesterday"
        else -> "saved $days days ago"
    }
}
