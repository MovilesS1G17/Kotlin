package com.centralia.app.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.insights.Insights
import com.centralia.app.domain.insights.InsightsSnapshot
import com.centralia.app.domain.insights.InsightsSnapshotStore
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.discover.FeatureCard
import com.centralia.app.feature.discover.FeatureHeader
import com.centralia.app.feature.discover.LabeledBar
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold

/** Sprint 4 view (Gustavo): the user's own saving habits. */
@Composable
fun InsightsScreen(
    videoRepository: VideoItemRepository,
    store: InsightsSnapshotStore,
    analytics: AnalyticsTracker,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "insights") { InsightsViewModel(videoRepository, store, analytics) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CenteredContent {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
                    .padding(top = Spacing.small, bottom = Spacing.xLarge)
            ) {
                FeatureHeader(
                    title = "Insights",
                    subtitle = "How you save and organize, worked out on your phone.",
                    onBack = onBack
                )
                val snapshot = state.snapshot
                when {
                    snapshot != null -> InsightsContent(
                        snapshot = snapshot,
                        sinceLastVisit = state.sinceLastVisit,
                        note = when {
                            state.isUpdating -> "Showing your last visit while we update…"
                            state.isFromLastVisit && state.errorMessage != null -> "From your last visit. ${state.errorMessage}"
                            else -> null
                        }
                    )
                    state.errorMessage != null -> ContentUnavailable(
                        title = "Insights unavailable",
                        icon = Icons.Outlined.BarChart,
                        description = state.errorMessage
                    )
                    else -> CentraliaLoadingState(label = "Working out your insights…", minHeight = 320.dp)
                }
            }
        }
        Box(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun InsightsContent(snapshot: InsightsSnapshot, sinceLastVisit: Int?, note: String?) {
    if (note != null) Text(note, style = CentraliaType.footnote, color = CentraliaColors.SecondaryText)

    FeatureCard {
        Text("This week", style = CentraliaType.headline, color = CentraliaColors.SecondaryText)
        Text(
            if (snapshot.thisWeek == 1) "1 short saved" else "${snapshot.thisWeek} shorts saved",
            style = CentraliaType.title.semibold(),
            color = CentraliaColors.Ink
        )
        val change = snapshot.weekChange
        Text(
            when {
                change > 0 -> "$change more than last week"
                change < 0 -> "${-change} fewer than last week"
                else -> "Same as last week"
            } + " · ${"%.1f".format(java.util.Locale.ENGLISH, snapshot.averagePerWeek)} a week on average",
            style = CentraliaType.subheadline,
            color = CentraliaColors.SecondaryText
        )
        if (sinceLastVisit != null && sinceLastVisit > 0) {
            Text(
                "Since your last visit: +$sinceLastVisit saved",
                style = CentraliaType.subheadline.semibold(),
                color = CentraliaColors.Ink
            )
        }
        if (snapshot.streakWeeks >= 2) {
            Text("${snapshot.streakWeeks}-week saving streak", style = CentraliaType.subheadline, color = CentraliaColors.Ink)
        }
    }

    FeatureCard {
        Text("Last ${snapshot.weeks.size} weeks", style = CentraliaType.headline, color = CentraliaColors.Ink)
        WeekBars(snapshot)
    }

    FeatureCard {
        Text("Where you save from", style = CentraliaType.headline, color = CentraliaColors.Ink)
        val total = snapshot.totalSaves.coerceAtLeast(1)
        for ((key, label) in listOf("tiktok" to "TikTok", "instagramReel" to "Instagram", "youtubeShort" to "YouTube")) {
            val count = snapshot.platformCounts[key] ?: 0
            LabeledBar(label = label, value = "$count", fraction = count.toFloat() / total)
        }
    }

    FeatureCard {
        Text("Organized", style = CentraliaType.headline, color = CentraliaColors.Ink)
        LabeledBar(label = "In a folder", value = "${snapshot.inFolderPct}%", fraction = snapshot.inFolderPct / 100f)
        Text(
            if (snapshot.topTags.isEmpty()) "No tags yet." else "Your top tags: " + snapshot.topTags.joinToString(", "),
            style = CentraliaType.footnote,
            color = CentraliaColors.SecondaryText
        )
    }
}

@Composable
private fun WeekBars(snapshot: InsightsSnapshot) {
    val max = snapshot.weeks.maxOfOrNull { it.saves }?.coerceAtLeast(1) ?: 1
    val description = snapshot.weeks.joinToString("; ") { "${Insights.shortLabel(it.weekStart)}: ${it.saves}" }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { contentDescription = "Saves per week. $description" }
    ) {
        snapshot.weeks.forEachIndexed { index, week ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("${week.saves}", style = CentraliaType.caption2, color = CentraliaColors.SecondaryText)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((96f * week.saves / max).coerceAtLeast(3f).dp)
                        .background(
                            if (index == snapshot.weeks.lastIndex) CentraliaColors.Ink else CentraliaColors.VideoSand,
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                Text(
                    Insights.shortLabel(week.weekStart),
                    style = CentraliaType.caption2,
                    color = CentraliaColors.SecondaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
