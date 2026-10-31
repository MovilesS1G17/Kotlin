package com.centralia.app.data.local

import com.centralia.app.domain.InstantSerializer
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Local storage (Sprint 3): the last copy of the user's library and folders,
 * kept as a JSON file in the app's private storage (`files/offline/library.json`).
 *
 * Why a file and not a database: the library is read and written as a whole
 * (one user, a few hundred shorts), the app already serialises these models
 * to JSON for the API, and a file needs no extra dependency or migrations.
 *
 * Threading: every read and write runs on Dispatchers.IO and a Mutex makes
 * them one at a time, so the UI thread never touches the disk and two writes
 * can't interleave. Writes go to a temp file first and are then renamed, so a
 * crash mid-write never leaves a half-written library.
 */
class LocalLibraryStore(directory: File) {

    @Serializable
    data class Snapshot(
        val videos: List<VideoItem> = emptyList(),
        val folders: List<LibraryFolder> = emptyList(),
        @Serializable(with = InstantSerializer::class)
        val savedAt: Instant? = null
    )

    private val file = File(directory, "offline/library.json")
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun read(): Snapshot = mutex.withLock { withContext(Dispatchers.IO) { readUnlocked() } }

    /** Applies [transform] to the stored copy and saves the result. */
    suspend fun update(transform: (Snapshot) -> Snapshot): Snapshot = mutex.withLock {
        withContext(Dispatchers.IO) {
            val next = transform(readUnlocked())
            writeUnlocked(next)
            next
        }
    }

    suspend fun clear() = mutex.withLock { withContext(Dispatchers.IO) { file.delete() } }

    private fun readUnlocked(): Snapshot =
        if (!file.exists()) {
            Snapshot()
        } else {
            runCatching { json.decodeFromString(Snapshot.serializer(), file.readText()) }
                .getOrDefault(Snapshot())
        }

    private fun writeUnlocked(snapshot: Snapshot) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.encodeToString(Snapshot.serializer(), snapshot))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }
}
