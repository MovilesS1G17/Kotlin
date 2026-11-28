package com.centralia.app.feature.watchlater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.analytics.Sprint4Events
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.watchlater.WatchLaterEntry
import com.centralia.app.domain.watchlater.WatchLaterRepository
import com.centralia.app.domain.watchlater.WatchLaterRules
import com.centralia.app.feature.library.LoadState
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Watch later (Sprint 4, Alex · local storage). The queue comes from the Room
 * table as a Flow, combined with the library (offline-first copy), so a short
 * deleted from the library drops out and changes made anywhere show at once.
 */
class WatchLaterViewModel(
    private val watchLater: WatchLaterRepository,
    private val videoRepository: VideoItemRepository,
    private val analytics: AnalyticsTracker,
    private val now: () -> Instant = Instant::now
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val queue: List<Pair<VideoItem, WatchLaterEntry>> = emptyList()
    ) {
        val unwatched get() = queue.filterNot { it.second.isWatched }
        val watched get() = queue.filter { it.second.isWatched }
    }

    private val library = MutableStateFlow<List<VideoItem>?>(null)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(watchLater.observe(), library) { entries, videos ->
                if (videos == null) null else WatchLaterRules.visible(entries, videos)
            }.collect { visible ->
                if (visible != null) _uiState.update { it.copy(state = LoadState.Loaded, queue = visible) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            if (library.value == null) _uiState.update { it.copy(state = LoadState.Loading) }
            try {
                library.value = videoRepository.videos()
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.message ?: "Watch later couldn't load your library.")) }
            }
        }
    }

    /** Opening from the queue marks it watched (BQ12: watched within 7 days?). */
    fun open(video: VideoItem) {
        viewModelScope.launch {
            val before = watchLater.markWatched(video.id) ?: return@launch
            if (!before.isWatched) {
                analytics.track(
                    Sprint4Events.WATCH_LATER_OPENED,
                    mapOf("video_id" to video.id.toString(), "hours_in_queue" to before.hoursInQueue(now()))
                )
            }
        }
    }

    fun moveUp(video: VideoItem) = viewModelScope.launch { watchLater.move(video.id, -1) }

    fun moveDown(video: VideoItem) = viewModelScope.launch { watchLater.move(video.id, 1) }

    fun remove(video: VideoItem) = viewModelScope.launch { watchLater.remove(video.id) }

    fun clearWatched() = viewModelScope.launch { watchLater.clearWatched() }
}
