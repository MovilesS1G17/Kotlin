package com.centralia.app.data.mock

import com.centralia.app.domain.search.SearchHistoryRepository
import kotlinx.serialization.Serializable

/** `actor MockSearchHistoryRepository`. */
class MockSearchHistoryRepository(
    private val store: MockDataStore,
    private val filename: String = "search-history-v1.json",
    private val maximumQueryCount: Int = 8
) : SearchHistoryRepository {

    @Serializable
    internal data class Snapshot(
        val schemaVersion: Int,
        val queries: List<String>
    )

    override suspend fun recentSearches(): List<String> = snapshot().queries

    override suspend fun recordSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return

        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            // Most recent first, case-insensitively de-duplicated, capped.
            val withoutQuery = current.queries.filterNot { it.equals(trimmedQuery, ignoreCase = true) }
            current.copy(queries = (listOf(trimmedQuery) + withoutQuery).take(maximumQueryCount))
        }
    }

    override suspend fun clearSearchHistory() {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            current.copy(queries = emptyList())
        }
    }

    private suspend fun snapshot(): Snapshot =
        store.load(Snapshot.serializer(), filename, { seed() })

    private companion object {
        fun seed() = Snapshot(
            schemaVersion = 1,
            queries = listOf("SwiftUI animation", "pasta", "small studio")
        )
    }
}
