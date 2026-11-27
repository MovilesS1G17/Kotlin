package com.centralia.app.feature.rediscover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.analytics.Sprint4Events
import com.centralia.app.domain.library.RecentlyViewedRepository
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.rediscover.Rediscover
import com.centralia.app.domain.rediscover.RediscoverDismissals
import com.centralia.app.domain.rediscover.RediscoverPick
import com.centralia.app.feature.library.LoadState
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Rediscover (Sprint 4, Enrique · multithreading).
 *
 * `load()` runs on Main (viewModelScope) and nests coroutines:
 *  1. `withContext(Dispatchers.IO)` + three `async`: the library, the
 *     "Recently viewed" table (Room) and the "Not now" list (SharedPreferences)
 *     are read at the same time, off the main thread.
 *  2. `Rediscover.pickInParallel` scores the candidates in chunks, each in its
 *     own `async` on `Dispatchers.Default` (CPU work).
 *  3. Back on Main, the state is updated and the screen redraws.
 */
class RediscoverViewModel(
    private val videoRepository: VideoItemRepository,
    private val recentlyViewed: RecentlyViewedRepository,
    private val dismissals: RediscoverDismissals,
    private val analytics: AnalyticsTracker,
    private val now: () -> Instant = Instant::now
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val picks: List<RediscoverPick> = emptyList(),
        val librarySize: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (_uiState.value.picks.isEmpty()) _uiState.update { it.copy(state = LoadState.Loading) }
            try {
                val at = now()
                val (videos, lastViewed, dismissed) = withContext(Dispatchers.IO) {
                    coroutineScope {
                        val videos = async { videoRepository.videos() }
                        val history = async {
                            recentlyViewed.observe(RecentlyViewedRepository.MAX_STORED).first()
                                .associate { it.videoID to it.lastViewedAt }
                        }
                        val dismissed = async { dismissals.active(at) }
                        Triple(videos.await(), history.await(), dismissed.await())
                    }
                }
                val picks = Rediscover.pickInParallel(videos, lastViewed, dismissed, at)
                _uiState.update { UiState(LoadState.Loaded, picks, videos.size) }
                analytics.track(Sprint4Events.REDISCOVER_VIEWED, mapOf("shown" to picks.size))
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(state = LoadState.Failed(error.message ?: "Rediscover couldn't load your library."))
                }
            }
        }
    }

    fun opened(pick: RediscoverPick) {
        analytics.track(
            Sprint4Events.REDISCOVER_OPENED,
            mapOf("video_id" to pick.video.id.toString(), "days_since_save" to pick.daysSinceSaved)
        )
    }

    /** "Not now": hidden for 30 days, and the list closes the gap right away. */
    fun notNow(pick: RediscoverPick) {
        _uiState.update { state -> state.copy(picks = state.picks.filterNot { it.video.id == pick.video.id }) }
        viewModelScope.launch { dismissals.dismiss(pick.video.id, now()) }
    }
}
