package com.centralia.app.domain.creators

import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Everything saved from one creator. */
data class CreatorSummary(
    val handle: String,
    val platforms: List<VideoPlatform>,
    val videos: List<VideoItem>,
    val lastSavedAt: Instant
) {
    val count: Int get() = videos.size
    val initial: String get() = handle.trimStart('@').firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}

/**
 * Creators (Sprint 4, Alejandro). Saved shorts grouped by creator, most saved
 * first. The same handle on TikTok and Instagram is one creator.
 *
 * Multithreading: [groupInParallel] groups each platform's shorts in its own
 * coroutine on [Dispatchers.Default] and merges the three partial maps.
 * Caching: [CreatorsCache] keeps the last results by library fingerprint, so
 * coming back to the screen doesn't regroup an unchanged library.
 */
object Creators {
    fun key(handle: String): String = handle.trim().trimStart('@').lowercase(Locale.ROOT)

    /** Cheap identity of a library (one pass, no allocation): the cache key. */
    fun fingerprint(videos: List<VideoItem>): Long {
        var hash = videos.size.toLong()
        for (video in videos) {
            hash = hash * 31 + (video.id.hashCode().toLong() xor video.creator.hashCode().toLong())
        }
        return hash
    }

    fun group(videos: List<VideoItem>): List<CreatorSummary> = merge(listOf(partial(videos)))

    suspend fun groupInParallel(
        videos: List<VideoItem>,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): List<CreatorSummary> = coroutineScope {
        val partials = videos.groupBy { it.platform }.values
            .map { slice -> async(dispatcher) { partial(slice) } }
            .awaitAll()
        merge(partials)
    }

    private fun partial(videos: List<VideoItem>): Map<String, List<VideoItem>> =
        videos.filter { key(it.creator).isNotEmpty() }.groupBy { key(it.creator) }

    private fun merge(partials: List<Map<String, List<VideoItem>>>): List<CreatorSummary> {
        val all = HashMap<String, MutableList<VideoItem>>()
        for (partial in partials) for ((key, list) in partial) all.getOrPut(key) { mutableListOf() }.addAll(list)
        return all.values.map { list ->
            val newestFirst = list.sortedByDescending { it.savedAt }
            CreatorSummary(
                handle = newestFirst.first().creator.trim(),
                platforms = newestFirst.map { it.platform }.distinct().sortedBy { it.ordinal },
                videos = newestFirst,
                lastSavedAt = newestFirst.first().savedAt
            )
        }.sortedWith(
            compareByDescending<CreatorSummary> { it.count }
                .thenByDescending { it.lastSavedAt }
                .thenBy { key(it.handle) }
        )
    }
}

/** LRU of grouped results by library fingerprint ([Creators.fingerprint]). */
class CreatorsCache(private val capacity: Int = 4) {
    private val map = object : LinkedHashMap<Long, List<CreatorSummary>>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, List<CreatorSummary>>?) = size > capacity
    }
    var hits = 0
        private set

    @Synchronized
    fun get(key: Long): List<CreatorSummary>? = map[key]?.also { hits++ }

    @Synchronized
    fun put(key: Long, value: List<CreatorSummary>) {
        map[key] = value
    }

    @Synchronized
    fun clear() = map.clear()
}
