package com.centralia.app.domain.rediscover

import com.centralia.app.domain.library.VideoItem
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One short Rediscover brings back, with why it was picked. */
data class RediscoverPick(
    val video: VideoItem,
    val daysSinceSaved: Long,
    /** Days since it was last opened on this phone, or null if never. */
    val daysSinceViewed: Long?,
    val score: Double
) {
    val reason: String
        get() = when (daysSinceViewed) {
            null -> "Saved $daysSinceSaved days ago and never opened since"
            else -> "Last opened $daysSinceViewed days ago"
        }
}

/**
 * Rediscover (Sprint 4, Enrique). Shorts saved at least [MIN_AGE_DAYS] days
 * ago that haven't been opened in the last [QUIET_DAYS] days. The older and
 * the longer forgotten, the higher the score; a small jitter that changes
 * once a day makes the list feel new each day while staying stable within it.
 *
 * Multithreading: scoring is CPU work over the whole library, so
 * [pickInParallel] splits the candidates into chunks and scores each chunk in
 * its own coroutine on [Dispatchers.Default] (nested `async` inside
 * `coroutineScope`), then merges the results.
 */
object Rediscover {
    const val MIN_AGE_DAYS = 7L
    const val QUIET_DAYS = 14L
    const val LIMIT = 6
    const val DISMISS_DAYS = 30L

    fun candidates(
        videos: List<VideoItem>,
        lastViewed: Map<UUID, Instant>,
        dismissed: Set<UUID>,
        now: Instant
    ): List<VideoItem> = videos.filter { video ->
        val age = Duration.between(video.savedAt, now).toDays()
        val viewed = lastViewed[video.id]
        age >= MIN_AGE_DAYS &&
            video.id !in dismissed &&
            (viewed == null || Duration.between(viewed, now).toDays() >= QUIET_DAYS)
    }

    fun score(video: VideoItem, lastViewed: Instant?, now: Instant, daySeed: Long): RediscoverPick {
        val age = Duration.between(video.savedAt, now).toDays()
        val quiet = lastViewed?.let { Duration.between(it, now).toDays() }
        var score = minOf(age, 120L).toDouble()
        score += if (quiet == null) 30.0 else minOf(quiet, 90L) * 0.5
        if (video.folderID != null || video.tags.isNotEmpty()) score += 5.0
        score += jitter(video.id, daySeed) * 20.0
        return RediscoverPick(video, age, quiet, score)
    }

    /** Sequential version (also what each parallel chunk runs). */
    fun pick(
        videos: List<VideoItem>,
        lastViewed: Map<UUID, Instant>,
        dismissed: Set<UUID>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
        limit: Int = LIMIT
    ): List<RediscoverPick> {
        val seed = now.atZone(zone).toLocalDate().toEpochDay()
        return candidates(videos, lastViewed, dismissed, now)
            .map { score(it, lastViewed[it.id], now, seed) }
            .sortedWith(BY_SCORE)
            .take(limit)
    }

    suspend fun pickInParallel(
        videos: List<VideoItem>,
        lastViewed: Map<UUID, Instant>,
        dismissed: Set<UUID>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
        limit: Int = LIMIT,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        chunks: Int = 4
    ): List<RediscoverPick> = coroutineScope {
        val seed = now.atZone(zone).toLocalDate().toEpochDay()
        val candidates = candidates(videos, lastViewed, dismissed, now)
        if (candidates.isEmpty()) return@coroutineScope emptyList()
        val size = (candidates.size + chunks - 1) / chunks
        candidates.chunked(size)
            .map { chunk ->
                async(dispatcher) {
                    chunk.map { score(it, lastViewed[it.id], now, seed) }
                        .sortedWith(BY_SCORE)
                        .take(limit)
                }
            }
            .awaitAll()
            .flatten()
            .sortedWith(BY_SCORE)
            .take(limit)
    }

    private val BY_SCORE = compareByDescending<RediscoverPick> { it.score }.thenBy { it.video.id }

    /** 0 ≤ jitter < 1, the same for one short on one day. */
    private fun jitter(id: UUID, daySeed: Long): Double {
        var x = id.mostSignificantBits xor id.leastSignificantBits xor (daySeed * -0x61c8864680b583ebL)
        x = (x xor (x ushr 33)) * -0xae502812aa7333L
        x = x xor (x ushr 33)
        return (x ushr 11).toDouble() / (1L shl 53).toDouble()
    }
}

/** "Not now" on a Rediscover card hides that short for [Rediscover.DISMISS_DAYS] days. */
interface RediscoverDismissals {
    suspend fun active(now: Instant): Set<UUID>
    suspend fun dismiss(videoID: UUID, at: Instant)
    suspend fun clear()
}

class InMemoryRediscoverDismissals : RediscoverDismissals {
    private val mutex = Mutex()
    private val dismissedAt = mutableMapOf<UUID, Instant>()

    override suspend fun active(now: Instant): Set<UUID> = mutex.withLock {
        dismissedAt.filterValues { Duration.between(it, now).toDays() < Rediscover.DISMISS_DAYS }.keys.toSet()
    }

    override suspend fun dismiss(videoID: UUID, at: Instant) = mutex.withLock {
        dismissedAt[videoID] = at
    }

    override suspend fun clear() = mutex.withLock { dismissedAt.clear() }
}
