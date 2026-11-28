package com.centralia.app.feature.creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.analytics.Sprint4Events
import com.centralia.app.domain.creators.CreatorSummary
import com.centralia.app.domain.creators.Creators
import com.centralia.app.domain.creators.CreatorsCache
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.feature.library.LoadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creators (Sprint 4, Alejandro's feature). The library is read on
 * Dispatchers.IO, grouped per platform in parallel on Dispatchers.Default
 * ([Creators.groupInParallel]) unless the [CreatorsCache] already has this
 * library, and the state is set back on Main.
 */
class CreatorsViewModel(
    private val videoRepository: VideoItemRepository,
    private val cache: CreatorsCache,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val creators: List<CreatorSummary> = emptyList(),
        val expandedHandle: String? = null,
        val query: String = ""
    ) {
        val visible: List<CreatorSummary>
            get() = if (query.isBlank()) creators
            else creators.filter { Creators.key(it.handle).contains(Creators.key(query)) }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (_uiState.value.creators.isEmpty()) _uiState.update { it.copy(state = LoadState.Loading) }
            try {
                val videos = withContext(Dispatchers.IO) { videoRepository.videos() }
                val key = Creators.fingerprint(videos)
                val creators = cache.get(key) ?: Creators.groupInParallel(videos).also { cache.put(key, it) }
                _uiState.update { it.copy(state = LoadState.Loaded, creators = creators) }
                analytics.track(Sprint4Events.CREATORS_VIEWED, mapOf("creators" to creators.size))
            } catch (error: Exception) {
                _uiState.update { it.copy(state = LoadState.Failed(error.message ?: "Creators couldn't load your library.")) }
            }
        }
    }

    fun toggle(creator: CreatorSummary) {
        _uiState.update { it.copy(expandedHandle = if (it.expandedHandle == creator.handle) null else creator.handle) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query.take(60)) }
    }
}
