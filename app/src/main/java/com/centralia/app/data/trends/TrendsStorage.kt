package com.centralia.app.data.trends

import android.util.LruCache
import com.centralia.app.data.remote.ApiClient
import com.centralia.app.domain.trends.CachedTrends
import com.centralia.app.domain.trends.Trends
import com.centralia.app.domain.trends.TrendsDiskStore
import com.centralia.app.domain.trends.TrendsMemoryCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** `GET /v1/trends?days=…` (Sprint 4, David's feature). */
suspend fun fetchTrends(client: ApiClient, windowDays: Int): Trends =
    client.send(
        ApiClient.Method.GET,
        "v1/trends",
        Trends.serializer(),
        query = mapOf("days" to windowDays.toString())
    )

/** Memory level: `android.util.LruCache`, one entry per window (7 / 30 days). */
class LruTrendsMemoryCache(maxEntries: Int = 4) : TrendsMemoryCache {
    private val cache = LruCache<Int, CachedTrends>(maxEntries)
    override fun get(windowDays: Int): CachedTrends? = cache.get(windowDays)
    override fun put(windowDays: Int, value: CachedTrends) { cache.put(windowDays, value) }
    override fun clear() = cache.evictAll()
}

/**
 * Disk level: one JSON file per window in the app's cache directory. Android
 * may delete cache files when space is low, which is fine for a cache. Written
 * to a temporary file and renamed, so a crash never leaves half a file.
 */
class FileTrendsDiskStore(private val directory: File) : TrendsDiskStore {
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(windowDays: Int) = File(directory, "trends-$windowDays.json")

    override suspend fun read(windowDays: Int): CachedTrends? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString(CachedTrends.serializer(), file(windowDays).readText()) }.getOrNull()
    }

    override suspend fun write(windowDays: Int, value: CachedTrends) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val temporary = File(directory, "trends-$windowDays.json.tmp")
        temporary.writeText(json.encodeToString(CachedTrends.serializer(), value))
        if (!temporary.renameTo(file(windowDays))) {
            file(windowDays).delete()
            temporary.renameTo(file(windowDays))
        }
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        directory.listFiles { f -> f.name.startsWith("trends-") }?.forEach { it.delete() }
        Unit
    }
}
