package com.centralia.app.domain.search


interface SearchHistoryRepository {
    suspend fun recentSearches(): List<String>

    suspend fun recordSearch(query: String)

    suspend fun clearSearchHistory()
}
