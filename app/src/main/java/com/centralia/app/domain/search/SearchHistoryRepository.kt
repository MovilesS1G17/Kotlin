package com.centralia.app.domain.search

/** `protocol SearchHistoryRepository`. */
interface SearchHistoryRepository {
    suspend fun recentSearches(): List<String>

    suspend fun recordSearch(query: String)

    suspend fun clearSearchHistory()
}
