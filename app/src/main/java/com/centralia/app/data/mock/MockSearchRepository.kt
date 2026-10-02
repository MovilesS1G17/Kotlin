package com.centralia.app.data.mock

import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.search.SearchRepository
import com.centralia.app.domain.search.SearchResult
import java.util.UUID

/**
 * Local stand-in for [com.centralia.app.data.network.APISearchRepository],
 * used by `DependencyContainer.mock()`. Mirrors the backend's Specification
 * semantics (single substring `query` across title/caption/creator, exact
 * case-insensitive `creator`, exact `platform`/`folderID`, exact
 * case-insensitive `tag`) over whatever the injected repository holds.
 */
class MockSearchRepository(private val videoRepository: VideoItemRepository) : SearchRepository {
    override suspend fun search(
        query: String?,
        platform: VideoPlatform?,
        creator: String?,
        folderID: UUID?,
        tag: String?,
        limit: Int?,
        offset: Int
    ): SearchResult {
        val matched = videoRepository.videos().filter { video ->
            if (!query.isNullOrEmpty()) {
                val haystacks = listOf(video.customTitle, video.sourceCaption, video.creator)
                if (haystacks.none { it?.contains(query, ignoreCase = true) == true }) return@filter false
            }
            if (platform != null && video.platform != platform) return@filter false
            if (creator != null && !video.creator.equals(creator, ignoreCase = true)) return@filter false
            if (folderID != null && video.folderID != folderID) return@filter false
            if (tag != null && video.tags.none { it.equals(tag, ignoreCase = true) }) return@filter false
            true
        }

        val total = matched.size
        if (limit == null) return SearchResult(matched, total)
        val page = matched.drop(offset).take(limit)
        return SearchResult(page, total)
    }

    override fun reportSearchCompleted(durationMs: Int, resultCount: Int, filterCount: Int) {
        // No backend in mock mode — nothing to report.
    }
}
