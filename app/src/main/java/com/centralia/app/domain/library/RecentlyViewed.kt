package com.centralia.app.domain.library

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * "Recently viewed" (Sprint 3, Enrique · local storage with a relational
 * database). Every time a short's detail screen opens, the short is recorded
 * here; the Library shows the last ones in a row at the top. The history only
 * lives on the phone, so it also works with no connection.
 */
data class RecentlyViewedEntry(
    val videoID: UUID,
    val title: String,
    val platform: VideoPlatform?,
    val creator: String,
    val thumbnailURL: String?,
    val lastViewedAt: Instant,
    val viewCount: Int
)

interface RecentlyViewedRepository {
    /** The most recently viewed shorts, newest first; updates by itself. */
    fun observe(limit: Int = DEFAULT_LIMIT): Flow<List<RecentlyViewedEntry>>

    suspend fun record(video: VideoItem)

    suspend fun remove(videoID: UUID)

    suspend fun clear()

    companion object {
        const val DEFAULT_LIMIT = 10

        /** How many entries the table keeps; older ones are deleted. */
        const val MAX_STORED = 50
    }
}

/**
 * Same behaviour as the Room repository, kept in memory: used by the mock
 * build and the unit tests (no Android needed).
 */
class InMemoryRecentlyViewedRepository(
    private val now: () -> Instant = Instant::now
) : RecentlyViewedRepository {
    private val mutex = Mutex()
    private val entries = MutableStateFlow<List<RecentlyViewedEntry>>(emptyList())

    override fun observe(limit: Int): Flow<List<RecentlyViewedEntry>> =
        entries.map { list -> list.sortedByDescending { it.lastViewedAt }.take(limit) }

    override suspend fun record(video: VideoItem) = mutex.withLock {
        val existing = entries.value.firstOrNull { it.videoID == video.id }
        val entry = video.toRecentlyViewed(now(), (existing?.viewCount ?: 0) + 1)
        entries.value = (entries.value.filterNot { it.videoID == video.id } + entry)
            .sortedByDescending { it.lastViewedAt }
            .take(RecentlyViewedRepository.MAX_STORED)
    }

    override suspend fun remove(videoID: UUID) = mutex.withLock {
        entries.value = entries.value.filterNot { it.videoID == videoID }
    }

    override suspend fun clear() = mutex.withLock { entries.value = emptyList() }
}

fun VideoItem.toRecentlyViewed(at: Instant, viewCount: Int) = RecentlyViewedEntry(
    videoID = id,
    title = displayTitle,
    platform = platform,
    creator = creator,
    thumbnailURL = thumbnailURL,
    lastViewedAt = at,
    viewCount = viewCount
)

/**
 * The shorts to show in the "Recently viewed" row: entries whose short is
 * still in the library (a deleted short disappears from the row), in the
 * entries' order (newest first).
 */
fun recentlyViewedVideos(
    entries: List<RecentlyViewedEntry>,
    library: List<VideoItem>
): List<Pair<VideoItem, RecentlyViewedEntry>> {
    val byID = library.associateBy { it.id }
    return entries.mapNotNull { entry -> byID[entry.videoID]?.let { it to entry } }
}
