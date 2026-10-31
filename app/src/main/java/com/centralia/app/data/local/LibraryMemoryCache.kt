package com.centralia.app.data.local

import android.util.LruCache
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlayback
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Caching (Sprint 3): the in-memory layer in front of the disk copy and the
 * network.
 *
 * - The library and folder lists stay in memory while the app runs, so moving
 *   between Library, Search and Folders while offline doesn't re-read the file.
 * - Playback answers live in an [LruCache] of the 32 most recently played
 *   shorts for 10 minutes. Re-opening a short you just watched skips the
 *   `/playback` request; the signed MP4 link is valid for 6 hours, so 10 minutes
 *   is well inside it. The key includes the network, because on mobile data the
 *   backend answers differently (data saver).
 *
 * Images (covers) have their own memory + disk cache in Coil, configured in
 * [com.centralia.app.CentraliaApplication].
 */
class LibraryMemoryCache(
    private val playbackTtl: Duration = Duration.ofMinutes(10),
    private val now: () -> Instant = Instant::now
) {
    @Volatile var videos: List<VideoItem>? = null
    @Volatile var folders: List<LibraryFolder>? = null

    private data class Entry(val playback: VideoPlayback, val storedAt: Instant)

    private val playback = LruCache<String, Entry>(32)

    fun playback(id: UUID, network: String): VideoPlayback? {
        val entry = playback.get(key(id, network)) ?: return null
        if (Duration.between(entry.storedAt, now()) > playbackTtl) {
            playback.remove(key(id, network))
            return null
        }
        return entry.playback
    }

    fun storePlayback(id: UUID, network: String, value: VideoPlayback) {
        playback.put(key(id, network), Entry(value, now()))
    }

    fun clear() {
        videos = null
        folders = null
        playback.evictAll()
    }

    private fun key(id: UUID, network: String) = "$id|$network"
}
