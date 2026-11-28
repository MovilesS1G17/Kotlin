package com.centralia.app.data.local.room

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.watchlater.WatchLaterEntry
import com.centralia.app.domain.watchlater.WatchLaterRepository
import com.centralia.app.domain.watchlater.WatchLaterRules
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local storage (Sprint 4, Alex): the Watch later queue is a second table in
 * the same Room database as "Recently viewed".
 *
 * Why Room: the queue has an order the user changes (move up/down), which is
 * an UPDATE of a few rows in one transaction; "is this short queued?" is an
 * indexed lookup; and the screen observes the table as a Flow, so adding a
 * short from the detail screen shows up in the queue without reloading.
 */
@Entity(tableName = "watch_later", indices = [Index(value = ["position"])])
data class WatchLaterEntity(
    @PrimaryKey val videoID: String,
    val title: String,
    val platform: String,
    val creator: String,
    val thumbnailURL: String?,
    /** Epoch milliseconds. */
    val addedAt: Long,
    val position: Int,
    /** Epoch milliseconds, null until opened from the queue. */
    val watchedAt: Long?
)

@Dao
abstract class WatchLaterDao {
    @Query("SELECT * FROM watch_later ORDER BY (watchedAt IS NOT NULL), position, addedAt")
    abstract fun observeAll(): Flow<List<WatchLaterEntity>>

    @Query("SELECT * FROM watch_later ORDER BY (watchedAt IS NOT NULL), position, addedAt")
    abstract suspend fun all(): List<WatchLaterEntity>

    @Query("SELECT * FROM watch_later WHERE videoID = :videoID")
    abstract suspend fun find(videoID: String): WatchLaterEntity?

    @Query("SELECT COUNT(*) FROM watch_later")
    abstract suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM watch_later")
    abstract suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entry: WatchLaterEntity): Long

    @Query("UPDATE watch_later SET position = :position WHERE videoID = :videoID")
    abstract suspend fun setPosition(videoID: String, position: Int)

    @Query("UPDATE watch_later SET watchedAt = :at WHERE videoID = :videoID")
    abstract suspend fun setWatched(videoID: String, at: Long)

    @Query("DELETE FROM watch_later WHERE videoID = :videoID")
    abstract suspend fun delete(videoID: String)

    @Query("DELETE FROM watch_later WHERE watchedAt IS NOT NULL")
    abstract suspend fun deleteWatched()

    @Query("DELETE FROM watch_later")
    abstract suspend fun clear()

    /** Adds at the end of the queue, all or nothing. Returns true if it was added. */
    @Transaction
    open suspend fun append(entry: WatchLaterEntity, max: Int): Boolean {
        if (find(entry.videoID) != null || count() >= max) return false
        return insert(entry.copy(position = maxPosition() + 1)) != -1L
    }

    /** Writes the new order of every unwatched row in one transaction. */
    @Transaction
    open suspend fun reorder(positions: List<Pair<String, Int>>) {
        for ((id, position) in positions) setPosition(id, position)
    }
}

/** [WatchLaterRepository] backed by the Room table `watch_later`. */
class RoomWatchLaterRepository(
    private val dao: WatchLaterDao,
    private val now: () -> Instant = Instant::now
) : WatchLaterRepository {

    override fun observe(): Flow<List<WatchLaterEntry>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toEntry() } }

    override suspend fun contains(videoID: UUID): Boolean = dao.find(videoID.toString()) != null

    override suspend fun add(video: VideoItem): Boolean = dao.append(
        WatchLaterEntity(
            videoID = video.id.toString(),
            title = video.displayTitle,
            platform = video.platform.rawValue,
            creator = video.creator,
            thumbnailURL = video.thumbnailURL,
            addedAt = now().toEpochMilli(),
            position = 0,
            watchedAt = null
        ),
        max = WatchLaterRepository.MAX_ENTRIES
    )

    override suspend fun remove(videoID: UUID) = dao.delete(videoID.toString())

    override suspend fun move(videoID: UUID, by: Int) {
        val entries = dao.all().mapNotNull { it.toEntry() }
        val moved = WatchLaterRules.moved(entries, videoID, by).filterNot { it.isWatched }
        dao.reorder(moved.map { it.videoID.toString() to it.position })
    }

    override suspend fun markWatched(videoID: UUID): WatchLaterEntry? {
        val before = dao.find(videoID.toString())?.toEntry() ?: return null
        dao.setWatched(videoID.toString(), now().toEpochMilli())
        return before
    }

    override suspend fun clearWatched() = dao.deleteWatched()

    override suspend fun clear() = dao.clear()

    private fun WatchLaterEntity.toEntry(): WatchLaterEntry? {
        val id = runCatching { UUID.fromString(videoID) }.getOrNull() ?: return null
        return WatchLaterEntry(
            videoID = id,
            title = title,
            platform = VideoPlatform.entries.firstOrNull { it.rawValue == platform },
            creator = creator,
            thumbnailURL = thumbnailURL,
            addedAt = Instant.ofEpochMilli(addedAt),
            position = position,
            watchedAt = watchedAt?.let(Instant::ofEpochMilli)
        )
    }
}
