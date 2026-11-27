package com.centralia.app.data.local.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Local storage (Sprint 3, Enrique): a relational database with Room (SQLite).
 *
 * Why a database here and not a JSON file like the library copy: the history
 * is queried, not read whole. "The last 10 viewed" is an ORDER BY … LIMIT, a
 * second view of the same short is an UPDATE of one row, and trimming the table
 * to 50 rows is one DELETE with a subquery. Room checks those SQL statements
 * when the app is compiled, runs suspend DAO calls off the main thread, and
 * returns a Flow that emits again whenever the table changes.
 */
@Entity(
    tableName = "recently_viewed",
    indices = [Index(value = ["lastViewedAt"])]
)
data class RecentlyViewedEntity(
    @PrimaryKey val videoID: String,
    val title: String,
    val platform: String,
    val creator: String,
    val thumbnailURL: String?,
    /** Epoch milliseconds. */
    val lastViewedAt: Long,
    val viewCount: Int
)

@Dao
abstract class RecentlyViewedDao {
    @Query("SELECT * FROM recently_viewed ORDER BY lastViewedAt DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<RecentlyViewedEntity>>

    @Query("SELECT * FROM recently_viewed WHERE videoID = :videoID")
    abstract suspend fun find(videoID: String): RecentlyViewedEntity?

    @Upsert
    abstract suspend fun upsert(entry: RecentlyViewedEntity)

    @Query(
        "DELETE FROM recently_viewed WHERE videoID NOT IN " +
            "(SELECT videoID FROM recently_viewed ORDER BY lastViewedAt DESC LIMIT :keep)"
    )
    abstract suspend fun trimTo(keep: Int)

    @Query("DELETE FROM recently_viewed WHERE videoID = :videoID")
    abstract suspend fun delete(videoID: String)

    @Query("DELETE FROM recently_viewed")
    abstract suspend fun clear()

    /** One view: read the count, write the row, trim the table — all or nothing. */
    @Transaction
    open suspend fun recordView(entry: RecentlyViewedEntity, keep: Int) {
        val previous = find(entry.videoID)
        upsert(entry.copy(viewCount = (previous?.viewCount ?: 0) + 1))
        trimTo(keep)
    }
}

@Database(entities = [RecentlyViewedEntity::class], version = 1, exportSchema = false)
abstract class CentraliaDatabase : RoomDatabase() {
    abstract fun recentlyViewedDao(): RecentlyViewedDao

    companion object {
        fun build(context: Context): CentraliaDatabase =
            Room.databaseBuilder(context, CentraliaDatabase::class.java, "centralia.db")
                // Only a local history lives here, so a schema change may rebuild it.
                .fallbackToDestructiveMigration()
                .build()
    }
}
