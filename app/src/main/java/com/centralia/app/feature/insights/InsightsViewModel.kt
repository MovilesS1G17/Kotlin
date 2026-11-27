package com.centralia.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.analytics.Sprint4Events
import com.centralia.app.domain.insights.Insights
import com.centralia.app.domain.insights.InsightsSnapshot
import com.centralia.app.domain.insights.InsightsSnapshotStore
import com.centralia.app.domain.library.VideoItemRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Insights (Sprint 4, Gustavo's feature · local storage). The last snapshot
 * is read from its JSON file and shown at once; the library is then read
 * (IO), the new snapshot computed (Default) and saved to the file again.
 * "Since your last visit" compares the two.
 */
class InsightsViewModel(
    private val videoRepository: VideoItemRepository,
    private val store: InsightsSnapshotStore,
    private val analytics: AnalyticsTracker,
    private val now: () -> Instant = Instant::now
) : ViewModel() {

    data class UiState(
        val snapshot: InsightsSnapshot? = null,
        val sinceLastVisit: Int? = null,
        val isUpdating: Boolean = true,
        /** The snapshot on screen is the stored one: the library couldn't be read. */
        val isFromLastVisit: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val previous = store.load()
            if (previous != null) _uiState.update { it.copy(snapshot = previous, isFromLastVisit = true) }
            try {
                val videos = withContext(Dispatchers.IO) { videoRepository.videos() }
                val current = withContext(Dispatchers.Default) { Insights.compute(videos, now()) }
                _uiState.update {
                    UiState(
                        snapshot = current,
                        sinceLastVisit = Insights.sinceLastVisit(previous, current),
                        isUpdating = false
                    )
                }
                store.save(current)
                analytics.track(Sprint4Events.INSIGHTS_VIEWED, mapOf("weeks" to current.weeks.size))
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isUpdating = false, errorMessage = error.message ?: "Insights couldn't read your library.")
                }
            }
        }
    }
}
