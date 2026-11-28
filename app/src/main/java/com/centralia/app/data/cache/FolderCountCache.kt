package com.centralia.app.data.cache

import androidx.collection.ArrayMap
import com.centralia.app.domain.library.VideoItem
import java.util.UUID

/**
 * Caching (Sprint 3, Alex): how many shorts each folder holds, kept in an
 * [ArrayMap].
 *
 * The Library and Folders screens ask "how many shorts in this folder?" once per
 * folder card on every recomposition. Counting means walking the whole library
 * each time (folders × shorts). This cache walks the library once, stores one
 * count per folder, and reuses the counts until the library list changes.
 *
 * Why ArrayMap and not HashMap or LruCache:
 * - A user has a handful of folders (tens at most). ArrayMap keeps keys and
 *   values in two compact arrays and finds a key by binary search on its hash:
 *   for fewer than ~1000 entries that is as fast as a HashMap and uses much
 *   less memory (no Entry object per item, no mostly empty bucket array).
 * - Nothing needs evicting: the whole map is rebuilt when the library changes,
 *   so an LRU would add bookkeeping for nothing.
 * - SparseArray would need Int keys; folder ids are UUIDs.
 *
 * The cache is "invalidated by identity": it remembers which list it was built
 * from, and any new list (a reload, a delete, a move) makes it rebuild.
 */
class FolderCountCache {
    private val counts = ArrayMap<UUID, Int>()
    private var unorganized = 0
    private var builtFrom: List<VideoItem>? = null

    /** Rebuilds the counts when [videos] is a different list than last time. */
    @Synchronized
    fun count(videos: List<VideoItem>, folderID: UUID?): Int {
        if (builtFrom !== videos) rebuild(videos)
        return if (folderID == null) unorganized else counts[folderID] ?: 0
    }

    @Synchronized
    fun clear() {
        counts.clear()
        unorganized = 0
        builtFrom = null
    }

    /** For tests and the viva demo: how many folders are cached right now. */
    @get:Synchronized
    val size: Int
        get() = counts.size

    private fun rebuild(videos: List<VideoItem>) {
        counts.clear()
        unorganized = 0
        for (video in videos) {
            val folder = video.folderID
            if (folder == null) unorganized++ else counts[folder] = (counts[folder] ?: 0) + 1
        }
        builtFrom = videos
    }
}
