package com.centralia.app.feature.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.trends.CachedTrendsRepository
import com.centralia.app.domain.trends.TrendItem
import com.centralia.app.domain.trends.TrendsRepository
import com.centralia.app.domain.trends.TrendsSource
import com.centralia.app.feature.discover.FeatureCard
import com.centralia.app.feature.discover.FeatureHeader
import com.centralia.app.feature.discover.LabeledBar
import com.centralia.app.feature.discover.RowTextAction
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.CentraliaChip
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold
import java.time.Instant

/** Sprint 4 view (David): what the Centralia community is saving. */
@Composable
fun TrendsScreen(
    repository: TrendsRepository,
    analytics: AnalyticsTracker,
    isOnline: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "trends") { TrendsViewModel(repository, analytics) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (state.result == null) viewModel.load() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CenteredContent {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
                    .padding(top = Spacing.small, bottom = Spacing.xLarge)
            ) {
                FeatureHeader(
                    title = "Trends",
                    subtitle = "What people on Centralia are saving. Only shows what at least 3 people saved, never anyone's library.",
                    onBack = onBack
                ) {
                    RowTextAction(title = "Refresh", onClick = { viewModel.load(forceRefresh = true) })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    CentraliaChip(title = "This week", isSelected = state.windowDays == 7, onClick = { viewModel.selectWindow(7) })
                    CentraliaChip(title = "This month", isSelected = state.windowDays == 30, onClick = { viewModel.selectWindow(30) })
                }

                val result = state.result
                if (result != null) {
                    Text(
                        text = buildString {
                            append(CachedTrendsRepository.updatedLabel(result.fetchedAt, Instant.now()))
                            append(
                                when (result.source) {
                                    TrendsSource.MEMORY, TrendsSource.DISK -> " · saved on this phone"
                                    TrendsSource.NETWORK -> ""
                                }
                            )
                            if (result.isStale) {
                                append(if (isOnline) " · couldn't refresh" else " · you're offline")
                            }
                        },
                        style = CentraliaType.footnote,
                        color = CentraliaColors.SecondaryText
                    )
                    TrendsContent(result.trends.totalSaves, result.trends.categories, result.trends.tags, result.trends.platformCounts)
                } else if (state.errorMessage != null) {
                    ContentUnavailable(
                        title = "Trends unavailable",
                        icon = Icons.Outlined.CloudOff,
                        description = state.errorMessage,
                        actions = { ProminentAction(title = "Try Again", onClick = { viewModel.load(forceRefresh = true) }) }
                    )
                } else {
                    CentraliaLoadingState(label = "Loading trends…", minHeight = 320.dp)
                }
            }
        }
        androidx.compose.foundation.layout.Box(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TrendsContent(
    totalSaves: Int,
    categories: List<TrendItem>,
    tags: List<TrendItem>,
    platformCounts: Map<String, Int>
) {
    FeatureCard {
        Text("$totalSaves shorts saved", style = CentraliaType.title3.semibold(), color = CentraliaColors.Ink)
        val total = platformCounts.values.sum().coerceAtLeast(1)
        for ((key, label) in listOf("tiktok" to "TikTok", "instagramReel" to "Instagram", "youtubeShort" to "YouTube")) {
            val count = platformCounts[key] ?: 0
            LabeledBar(label = label, value = "${Math.round(100.0 * count / total)}%", fraction = count.toFloat() / total)
        }
    }
    TrendList(title = "Top categories", items = categories, empty = "Not enough people saved into the same folder yet.")
    TrendList(title = "Top tags", items = tags, empty = "Not enough people used the same tag yet.")
}

@Composable
private fun TrendList(title: String, items: List<TrendItem>, empty: String) {
    FeatureCard {
        Text(title, style = CentraliaType.headline, color = CentraliaColors.Ink)
        if (items.isEmpty()) {
            Text(empty, style = CentraliaType.footnote, color = CentraliaColors.SecondaryText)
        } else {
            val max = items.maxOf { it.saves }.coerceAtLeast(1)
            items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("${index + 1}", style = CentraliaType.footnote.semibold(), color = CentraliaColors.SecondaryText, modifier = Modifier.padding(end = Spacing.small))
                    LabeledBar(label = item.name, value = "${item.saves}", fraction = item.saves.toFloat() / max, modifier = Modifier.weight(1f))
                }
                Text(
                    "${item.users} people",
                    style = CentraliaType.caption,
                    color = CentraliaColors.SecondaryText,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
        }
    }
}
