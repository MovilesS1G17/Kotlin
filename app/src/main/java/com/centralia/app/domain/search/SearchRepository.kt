package com.centralia.app.domain.search

import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import java.util.UUID

data class SearchResult(val videos: List<VideoItem>, val totalCount: Int)

/** `protocol SearchRepository`. */
interface SearchRepository {
    /**
     * Calls the backend's Specification-based `GET /videos` search. [query]
     * is expected to already be a single term — multi-term AND matching
     * across fields stays a client-side concern (`filteredVideos`), since
     * the backend's `query` param only does a single substring match.
     */
    suspend fun search(
        query: String?,
        platform: VideoPlatform?,
        creator: String?,
        folderID: UUID?,
        tag: String?,
        limit: Int?,
        offset: Int
    ): SearchResult

    /**
     * Fire-and-forget: reports the client-measured latency of a search that
     * had at least one active filter/query. Never throws, never blocks the
     * caller — failures are swallowed internally.
     */
    fun reportSearchCompleted(durationMs: Int, resultCount: Int, filterCount: Int)
}
