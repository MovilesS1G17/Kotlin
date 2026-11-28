package com.centralia.app.domain.trends

import com.centralia.app.domain.InstantSerializer
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** `GET /v1/trends`: what the community saved, only things 3+ people saved. */
@Serializable
data class TrendItem(val name: String, val saves: Int, val users: Int)

@Serializable
data class Trends(
    @Serializable(with = InstantSerializer::class)
    val generatedAt: Instant,
    val windowDays: Int,
    val minUsers: Int,
    val totalSaves: Int,
    val categories: List<TrendItem>,
    val tags: List<TrendItem>,
    val platformCounts: Map<String, Int>
)

/** Where a Trends answer came from (sent with `trends_loaded` for BQ13). */
enum class TrendsSource(val rawValue: String) { MEMORY("memory"), DISK("disk"), NETWORK("network") }

data class TrendsResult(
    val trends: Trends,
    val source: TrendsSource,
    val fetchedAt: Instant,
    /** Older than the cache lifetime: shown because the network failed. */
    val isStale: Boolean
)

@Serializable
data class CachedTrends(
    val trends: Trends,
    @Serializable(with = InstantSerializer::class)
    val fetchedAt: Instant
)

class TrendsUnavailableException(message: String) : Exception(message)

/** Memory level of the cache (Android: `LruCache`; tests: a map). */
interface TrendsMemoryCache {
    fun get(windowDays: Int): CachedTrends?
    fun put(windowDays: Int, value: CachedTrends)
    fun clear()
}

/** Disk level of the cache (Android: a JSON file per window in `cacheDir`). */
interface TrendsDiskStore {
    suspend fun read(windowDays: Int): CachedTrends?
    suspend fun write(windowDays: Int, value: CachedTrends)
    suspend fun clear()
}

interface TrendsRepository {
    suspend fun load(windowDays: Int = 7, forceRefresh: Boolean = false): TrendsResult
    suspend fun clear()
}

/**
 * Caching (Sprint 4, David's feature on both apps): a two-level cache in front
 * of `GET /v1/trends`, the only screen whose data is the same for everyone.
 *
 * 1. Memory (LRU, one entry per window): instant, while the app is open.
 * 2. Disk (JSON file): survives restarts and works offline.
 * 3. Network, only when both are older than [TTL] or the user pulls to refresh.
 *
 * Eventual connectivity: if the network fails, the newest cached copy is shown
 * marked as stale ("Updated 3 h ago"); with nothing cached the screen says it
 * needs a connection once.
 */
class CachedTrendsRepository(
    private val fetch: suspend (windowDays: Int) -> Trends,
    private val memory: TrendsMemoryCache,
    private val disk: TrendsDiskStore,
    private val now: () -> Instant = Instant::now
) : TrendsRepository {
    private val mutex = Mutex()

    override suspend fun load(windowDays: Int, forceRefresh: Boolean): TrendsResult = mutex.withLock {
        val at = now()
        if (!forceRefresh) {
            memory.get(windowDays)?.takeIf { isFresh(it, at) }?.let {
                return@withLock TrendsResult(it.trends, TrendsSource.MEMORY, it.fetchedAt, false)
            }
            disk.read(windowDays)?.takeIf { isFresh(it, at) }?.let {
                memory.put(windowDays, it)
                return@withLock TrendsResult(it.trends, TrendsSource.DISK, it.fetchedAt, false)
            }
        }
        try {
            val fresh = CachedTrends(fetch(windowDays), at)
            memory.put(windowDays, fresh)
            disk.write(windowDays, fresh)
            TrendsResult(fresh.trends, TrendsSource.NETWORK, at, false)
        } catch (error: Exception) {
            val fallback = memory.get(windowDays) ?: disk.read(windowDays)?.also { memory.put(windowDays, it) }
            if (fallback != null) {
                val source = if (memory.get(windowDays) === fallback) TrendsSource.MEMORY else TrendsSource.DISK
                TrendsResult(fallback.trends, source, fallback.fetchedAt, isStale = !isFresh(fallback, at))
            } else {
                throw TrendsUnavailableException(OFFLINE_MESSAGE)
            }
        }
    }

    override suspend fun clear() = mutex.withLock {
        memory.clear()
        disk.clear()
    }

    private fun isFresh(cached: CachedTrends, at: Instant): Boolean =
        Duration.between(cached.fetchedAt, at) < TTL

    companion object {
        val TTL: Duration = Duration.ofMinutes(10)
        const val OFFLINE_MESSAGE =
            "Trends needs a connection the first time. Connect to the internet and we'll keep a copy on your phone for next time."

        /** "Updated just now" / "Updated 5 min ago" / "Updated 3 h ago" / "Updated 2 days ago". */
        fun updatedLabel(fetchedAt: Instant, at: Instant): String {
            val minutes = Duration.between(fetchedAt, at).toMinutes().coerceAtLeast(0)
            return when {
                minutes < 1 -> "Updated just now"
                minutes < 60 -> "Updated $minutes min ago"
                minutes < 60 * 24 -> "Updated ${minutes / 60} h ago"
                else -> "Updated ${minutes / (60 * 24)} days ago"
            }
        }
    }
}

/** A small LRU map, used by the tests and the in-memory build. */
class MapTrendsMemoryCache(private val capacity: Int = 4) : TrendsMemoryCache {
    private val map = object : LinkedHashMap<Int, CachedTrends>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, CachedTrends>?) = size > capacity
    }

    @Synchronized override fun get(windowDays: Int): CachedTrends? = map[windowDays]
    @Synchronized override fun put(windowDays: Int, value: CachedTrends) { map[windowDays] = value }
    @Synchronized override fun clear() = map.clear()
}

class InMemoryTrendsDiskStore : TrendsDiskStore {
    private val mutex = Mutex()
    private val files = mutableMapOf<Int, CachedTrends>()
    override suspend fun read(windowDays: Int) = mutex.withLock { files[windowDays] }
    override suspend fun write(windowDays: Int, value: CachedTrends) = mutex.withLock { files[windowDays] = value }
    override suspend fun clear() = mutex.withLock { files.clear() }
}
