package com.centralia.app.data.offline

import android.content.SharedPreferences
import com.centralia.app.data.remote.ApiException
import com.centralia.app.domain.search.SearchHistoryRepository

/**
 * Eventual connectivity + local storage (Sprint 3) for recent searches:
 * network falling back to a copy kept in SharedPreferences. Offline, searches
 * are still recorded locally (the server's history only lists the last 8, so
 * missing a few offline ones is acceptable).
 */
class OfflineSearchHistoryRepository(
    private val remote: SearchHistoryRepository,
    private val preferences: SharedPreferences
) : SearchHistoryRepository {

    override suspend fun recentSearches(): List<String> =
        try {
            remote.recentSearches().also(::save)
        } catch (error: Exception) {
            if (!ApiException.isOffline(error)) throw error
            load()
        }

    override suspend fun recordSearch(query: String) {
        save((listOf(query) + load().filterNot { it.equals(query, ignoreCase = true) }).take(LIMIT))
        try {
            remote.recordSearch(query)
        } catch (error: Exception) {
            if (!ApiException.isOffline(error)) throw error
        }
    }

    override suspend fun clearSearchHistory() {
        save(emptyList())
        try {
            remote.clearSearchHistory()
        } catch (error: Exception) {
            if (!ApiException.isOffline(error)) throw error
        }
    }

    private fun save(searches: List<String>) {
        preferences.edit().putString(KEY, searches.joinToString(SEPARATOR)).apply()
    }

    private fun load(): List<String> =
        preferences.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()

    private companion object {
        const val KEY = "recentSearches"
        const val SEPARATOR = "\u001F"
        const val LIMIT = 8
    }
}
