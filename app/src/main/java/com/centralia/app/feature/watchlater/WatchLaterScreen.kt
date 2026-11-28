package com.centralia.app.feature.watchlater

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.watchlater.WatchLaterEntry
import com.centralia.app.domain.watchlater.WatchLaterRepository
import com.centralia.app.feature.discover.FeatureHeader
import com.centralia.app.feature.discover.RowTextAction
import com.centralia.app.feature.discover.ShortRow
import com.centralia.app.feature.library.LoadState
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold
import java.time.Duration
import java.time.Instant

/** Sprint 4 view (Alex): the Watch later queue. */
@Composable
fun WatchLaterScreen(
    watchLater: WatchLaterRepository,
    videoRepository: VideoItemRepository,
    analytics: AnalyticsTracker,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "watchLater") {
        WatchLaterViewModel(watchLater, videoRepository, analytics)
    }
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
                    title = "Watch later",
                    subtitle = "Your queue lives on this phone, so it works offline too.",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.medium)
                )
            }
        }
        when (val load = state.state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                CentraliaLoadingState(label = "Loading your queue…", minHeight = 360.dp)
            }
            is LoadState.Failed -> item(key = "failed") {
                ContentUnavailable(
                    title = "Watch later unavailable",
                    icon = CentraliaIcons.Warning,
                    description = load.message,
                    actions = { ProminentAction(title = "Try Again", onClick = viewModel::load) }
                )
            }
            LoadState.Loaded -> if (state.queue.isEmpty()) {
                item(key = "empty") {
                    ContentUnavailable(
                        title = "Your queue is empty",
                        icon = Icons.Outlined.BookmarkBorder,
                        description = "Open a short and tap \"Watch later\" to line it up here."
                    )
                }
            } else {
                val unwatched = state.unwatched
                item(key = "count") {
                    CenteredContent {
                        Text(
                            text = if (unwatched.size == 1) "1 short to watch" else "${unwatched.size} shorts to watch",
                            style = CentraliaType.headline,
                            color = CentraliaColors.SecondaryText,
                            modifier = Modifier.padding(horizontal = Spacing.medium)
                        )
                    }
                }
                itemsIndexed(
                    items = unwatched,
                    key = { _, pair -> "queue-${pair.first.id}" },
                    contentType = { _, _ -> "queueRow" }
                ) { index, (video, entry) ->
                    CenteredContent {
                        ShortRow(
                            video = video,
                            detail = "#${index + 1} · ${addedLabel(entry)}",
                            onOpen = {
                                viewModel.open(video)
                                onOpenVideo(video)
                            },
                            modifier = Modifier.padding(horizontal = Spacing.medium)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ArrowButton(Icons.Outlined.KeyboardArrowUp, "Move up", enabled = index > 0) { viewModel.moveUp(video) }
                                ArrowButton(Icons.Outlined.KeyboardArrowDown, "Move down", enabled = index < unwatched.lastIndex) { viewModel.moveDown(video) }
                            }
                            RowTextAction(title = "Remove", onClick = { viewModel.remove(video) })
                        }
                    }
                }
                val watched = state.watched
                if (watched.isNotEmpty()) {
                    item(key = "watchedHeader") {
                        CenteredContent {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.medium, vertical = Spacing.small)
                            ) {
                                Text(
                                    "Watched",
                                    style = CentraliaType.sectionTitle,
                                    color = CentraliaColors.Ink,
                                    modifier = Modifier.weight(1f)
                                )
                                RowTextAction(title = "Clear watched", onClick = { viewModel.clearWatched() })
                            }
                        }
                    }
                    itemsIndexed(
                        items = watched,
                        key = { _, pair -> "watched-${pair.first.id}" },
                        contentType = { _, _ -> "queueRow" }
                    ) { _, (video, entry) ->
                        CenteredContent {
                            ShortRow(
                                video = video,
                                detail = "Watched · ${addedLabel(entry)}",
                                onOpen = { onOpenVideo(video) },
                                modifier = Modifier.padding(horizontal = Spacing.medium)
                            ) {
                                RowTextAction(title = "Remove", onClick = { viewModel.remove(video) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .centraliaPressable(enabled = enabled, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) CentraliaColors.Ink else CentraliaColors.Divider
        )
    }
}

internal fun addedLabel(entry: WatchLaterEntry, at: Instant = Instant.now()): String {
    val days = Duration.between(entry.addedAt, at).toDays()
    return when {
        days <= 0 -> "Added today"
        days == 1L -> "Added yesterday"
        else -> "Added $days days ago"
    }
}
