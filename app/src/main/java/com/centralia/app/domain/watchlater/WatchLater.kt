package com.centralia.app.domain.watchlater

import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Watch later (Sprint 4, Alex · local storage). An ordered queue of shorts the
 * user wants to watch later. It lives only on the phone, in the Room table
 * `watch_later`, so it works the same with or without a connection.
 */
data class WatchLaterEntry(
    val videoID: UUID,
    val title: String,
    val platform: VideoPlatform?,
    val creator: String,
    val thumbnailURL: String?,
    val addedAt: Instant,
    /** 0 is the top of the queue. */
    val position: Int,
    /** Set when the short was opened from the queue. */
    val watchedAt: Instant? = null
) {
    val isWatched: Boolean get() = watchedAt != null

    fun hoursInQueue(at: Instant): Double =
        Math.round(Duration.between(addedAt, at).toMinutes() / 6.0) / 10.0
}

interface WatchLaterRepository {
    /** The queue: unwatched first in queue order, then watched ones. */
    fun observe(): Flow<List<WatchLaterEntry>>

    suspend fun contains(videoID: UUID): Boolean

    /** Adds at the end of the queue; does nothing if it's already there. Returns true if added. */
    suspend fun add(video: VideoItem): Boolean

    suspend fun remove(videoID: UUID)

    /** Moves an entry [by] places (negative = up). */
    suspend fun move(videoID: UUID, by: Int)

    /** Marks it watched and returns the entry as it was before (for hours in queue). */
    suspend fun markWatched(videoID: UUID): WatchLaterEntry?

    suspend fun clearWatched()

    suspend fun clear()

    companion object {
        const val MAX_ENTRIES = 100
    }
}

/** The queue rules, shared by the Room repository and the in-memory one. */
object WatchLaterRules {
    fun ordered(entries: List<WatchLaterEntry>): List<WatchLaterEntry> =
        entries.sortedWith(compareBy<WatchLaterEntry> { it.isWatched }.thenBy { it.position }.thenBy { it.addedAt })

    /** The queue after moving [videoID] [by] places among the unwatched entries, renumbered 0..n. */
    fun moved(entries: List<WatchLaterEntry>, videoID: UUID, by: Int): List<WatchLaterEntry> {
        val queue = ordered(entries).filterNot { it.isWatched }.toMutableList()
        val from = queue.indexOfFirst { it.videoID == videoID }
        if (from < 0 || by == 0) return entries
        val to = (from + by).coerceIn(0, queue.lastIndex)
        queue.add(to, queue.removeAt(from))
        val renumbered = queue.mapIndexed { index, entry -> entry.copy(position = index) }
        return renumbered + entries.filter { it.isWatched }
    }

    /** Entries whose short is still in the library, with the short. */
    fun visible(entries: List<WatchLaterEntry>, library: List<VideoItem>): List<Pair<VideoItem, WatchLaterEntry>> {
        val byID = library.associateBy { it.id }
        return ordered(entries).mapNotNull { entry -> byID[entry.videoID]?.let { it to entry } }
    }
}

fun VideoItem.toWatchLater(at: Instant, position: Int) = WatchLaterEntry(
    videoID = id,
    title = displayTitle,
    platform = platform,
    creator = creator,
    thumbnailURL = thumbnailURL,
    addedAt = at,
    position = position
)

/** Same behaviour as the Room repository, in memory (mock build and unit tests). */
class InMemoryWatchLaterRepository(
    private val now: () -> Instant = Instant::now
) : WatchLaterRepository {
    private val mutex = Mutex()
    private val entries = MutableStateFlow<List<WatchLaterEntry>>(emptyList())

    override fun observe(): Flow<List<WatchLaterEntry>> = entries.map(WatchLaterRules::ordered)

    override suspend fun contains(videoID: UUID): Boolean = entries.value.any { it.videoID == videoID }

    override suspend fun add(video: VideoItem): Boolean = mutex.withLock {
        if (entries.value.any { it.videoID == video.id }) return@withLock false
        if (entries.value.size >= WatchLaterRepository.MAX_ENTRIES) return@withLock false
        val next = (entries.value.maxOfOrNull { it.position } ?: -1) + 1
        entries.value = entries.value + video.toWatchLater(now(), next)
        true
    }

    override suspend fun remove(videoID: UUID) = mutex.withLock {
        entries.value = entries.value.filterNot { it.videoID == videoID }
    }

    override suspend fun move(videoID: UUID, by: Int) = mutex.withLock {
        entries.value = WatchLaterRules.moved(entries.value, videoID, by)
    }

    override suspend fun markWatched(videoID: UUID): WatchLaterEntry? = mutex.withLock {
        val before = entries.value.firstOrNull { it.videoID == videoID } ?: return@withLock null
        entries.value = entries.value.map { if (it.videoID == videoID) it.copy(watchedAt = now()) else it }
        before
    }

    override suspend fun clearWatched() = mutex.withLock {
        entries.value = entries.value.filterNot { it.isWatched }
    }

    override suspend fun clear() = mutex.withLock { entries.value = emptyList() }
}
