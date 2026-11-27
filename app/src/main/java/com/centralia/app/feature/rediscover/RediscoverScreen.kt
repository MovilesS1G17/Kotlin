package com.centralia.app.feature.rediscover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.library.RecentlyViewedRepository
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.rediscover.RediscoverDismissals
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
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing

/** Sprint 4 view (Enrique): shorts saved a while ago and not opened since. */
@Composable
fun RediscoverScreen(
    videoRepository: VideoItemRepository,
    recentlyViewed: RecentlyViewedRepository,
    dismissals: RediscoverDismissals,
    analytics: AnalyticsTracker,
    isOnline: Boolean,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "rediscover") {
        RediscoverViewModel(videoRepository, recentlyViewed, dismissals, analytics)
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
                    title = "Rediscover",
                    subtitle = "Shorts you saved a while ago and haven't opened since. A new pick every day.",
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.medium)
                )
            }
        }
        if (!isOnline) {
            item(key = "offline") {
                CenteredContent {
                    Text(
                        "You're offline. These picks come from the copy of your library on this phone.",
                        style = CentraliaType.footnote,
                        color = CentraliaColors.SecondaryText,
                        modifier = Modifier.padding(horizontal = Spacing.medium)
                    )
                }
            }
        }
        when (val load = state.state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                CentraliaLoadingState(label = "Looking for forgotten shorts…", minHeight = 360.dp)
            }
            is LoadState.Failed -> item(key = "failed") {
                ContentUnavailable(
                    title = "Rediscover unavailable",
                    icon = CentraliaIcons.Warning,
                    description = load.message,
                    actions = { ProminentAction(title = "Try Again", onClick = viewModel::load) }
                )
            }
            LoadState.Loaded -> if (state.picks.isEmpty()) {
                item(key = "empty") {
                    ContentUnavailable(
                        title = "Nothing to rediscover yet",
                        icon = Icons.Outlined.Restore,
                        description = if (state.librarySize == 0) {
                            "Save a few shorts first. In a week, the ones you haven't opened will show up here."
                        } else {
                            "You've opened everything you saved more than a week ago. Nice!"
                        }
                    )
                }
            } else {
                items(items = state.picks, key = { it.video.id.toString() }, contentType = { "pick" }) { pick ->
                    CenteredContent {
                        ShortRow(
                            video = pick.video,
                            detail = pick.reason,
                            onOpen = {
                                viewModel.opened(pick)
                                onOpenVideo(pick.video)
                            },
                            modifier = Modifier.padding(horizontal = Spacing.medium)
                        ) {
                            RowTextAction(title = "Not now", onClick = { viewModel.notNow(pick) })
                        }
                    }
                }
                item(key = "footnote") {
                    CenteredContent {
                        Box(Modifier.padding(horizontal = Spacing.medium)) {
                            Text(
                                "\"Not now\" hides a short from Rediscover for 30 days.",
                                style = CentraliaType.caption,
                                color = CentraliaColors.SecondaryText
                            )
                        }
                    }
                }
            }
        }
    }
}
