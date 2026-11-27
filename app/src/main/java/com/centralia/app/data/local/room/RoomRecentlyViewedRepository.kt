package com.centralia.app.data.local.room

import com.centralia.app.domain.library.RecentlyViewedEntry
import com.centralia.app.domain.library.RecentlyViewedRepository
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [RecentlyViewedRepository] backed by the Room table `recently_viewed`. */
class RoomRecentlyViewedRepository(
    private val dao: RecentlyViewedDao,
    private val now: () -> Instant = Instant::now
) : RecentlyViewedRepository {

    override fun observe(limit: Int): Flow<List<RecentlyViewedEntry>> =
        dao.observeRecent(limit).map { rows -> rows.mapNotNull { it.toEntry() } }

    override suspend fun record(video: VideoItem) {
        dao.recordView(
            RecentlyViewedEntity(
                videoID = video.id.toString(),
                title = video.displayTitle,
                platform = video.platform.rawValue,
                creator = video.creator,
                thumbnailURL = video.thumbnailURL,
                lastViewedAt = now().toEpochMilli(),
                viewCount = 0 // the DAO adds this view to the stored count
            ),
            keep = RecentlyViewedRepository.MAX_STORED
        )
    }

    override suspend fun remove(videoID: UUID) = dao.delete(videoID.toString())

    override suspend fun clear() = dao.clear()

    private fun RecentlyViewedEntity.toEntry(): RecentlyViewedEntry? {
        val id = runCatching { UUID.fromString(videoID) }.getOrNull() ?: return null
        return RecentlyViewedEntry(
            videoID = id,
            title = title,
            platform = VideoPlatform.entries.firstOrNull { it.rawValue == platform },
            creator = creator,
            thumbnailURL = thumbnailURL,
            lastViewedAt = Instant.ofEpochMilli(lastViewedAt),
            viewCount = viewCount
        )
    }
}
