package com.centralia.app.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.analytics.Sprint4Events
import com.centralia.app.domain.trends.TrendsRepository
import com.centralia.app.domain.trends.TrendsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Trends (Sprint 4, David's feature · caching). The repository answers from
 * memory, disk or the network; the time it took and where it came from are
 * sent as `trends_loaded` (BQ13: how much the cache saves).
 */
class TrendsViewModel(
    private val repository: TrendsRepository,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    data class UiState(
        val windowDays: Int = 7,
        val isLoading: Boolean = false,
        val result: TrendsResult? = null,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = false) {
        val days = _uiState.value.windowDays
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val start = System.nanoTime()
            try {
                val result = repository.load(days, forceRefresh)
                val ms = (System.nanoTime() - start) / 100_000 / 10.0
                _uiState.update { it.copy(isLoading = false, result = result) }
                analytics.track(
                    Sprint4Events.TRENDS_LOADED,
                    mapOf("source" to result.source.rawValue, "duration_ms" to ms, "stale" to result.isStale, "window_days" to days)
                )
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Trends couldn't load.") }
            }
        }
    }

    fun selectWindow(days: Int) {
        if (days == _uiState.value.windowDays) return
        _uiState.update { it.copy(windowDays = days, result = null) }
        load()
    }
}
