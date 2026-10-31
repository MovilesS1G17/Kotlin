package com.centralia.app.data.offline

import com.centralia.app.data.local.LibraryMemoryCache
import com.centralia.app.data.local.LocalLibraryStore
import com.centralia.app.data.local.PendingAction
import com.centralia.app.data.local.PendingActionQueue
import com.centralia.app.data.remote.ApiException
import com.centralia.app.data.remote.RemoteLibraryRepository
import com.centralia.app.domain.context.NetworkType
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.UndoMethod
import com.centralia.app.domain.library.VideoDeletion
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlayback
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Eventual connectivity + caching + local storage (Sprint 3) for the library.
 * It wraps the API repository, so the screens and ViewModels don't change:
 *
 * | Data | Strategy |
 * |---|---|
 * | Library and folder lists | Network falling back to cache: memory, then the JSON file on disk |
 * | Delete, undo, move, note, tags | Offline: applied to the local copy at once and queued in the outbox; synced when the network is back |
 * | Playback | Cache (LRU, 10 min) falling back to network; offline it needs a connection |
 * | Saving a short, creating/renaming/deleting folders | Network only, with a clear "you're offline" message |
 *
 * Disk writes are launched on [ioScope] so the screen gets its answer without
 * waiting for the file to be written.
 */
class OfflineFirstLibraryRepository(
    private val remote: RemoteLibraryRepository,
    private val store: LocalLibraryStore,
    private val queue: PendingActionQueue,
    private val memory: LibraryMemoryCache,
    private val ioScope: CoroutineScope,
    private val isOnline: () -> Boolean,
    private val networkType: () -> NetworkType,
    private val now: () -> Instant = Instant::now
) : VideoItemRepository, FolderRepository {

    // ------------------------------------------------------------- reads

    override suspend fun videos(): List<VideoItem> {
        if (isOnline()) {
            try {
                val fresh = remote.videos()
                memory.videos = fresh
                ioScope.launch {
                    store.update { it.copy(videos = fresh, savedAt = now()) }
                }
                return withPendingChanges(fresh)
            } catch (error: Exception) {
                if (!ApiException.isOffline(error)) throw error
            }
        }
        val cached = memory.videos ?: store.read().videos.also { memory.videos = it }
        return withPendingChanges(cached)
    }

    override suspend fun folders(): List<LibraryFolder> {
        if (isOnline()) {
            try {
                val fresh = remote.folders()
                memory.folders = fresh
                ioScope.launch { store.update { it.copy(folders = fresh, savedAt = now()) } }
                return fresh
            } catch (error: Exception) {
                if (!ApiException.isOffline(error)) throw error
            }
        }
        return memory.folders ?: store.read().folders.also { memory.folders = it }
    }

    override suspend fun playback(id: UUID): VideoPlayback? {
        val network = networkType().wireValue
        memory.playback(id, network)?.let { return it }
        if (!isOnline()) throw OfflineException.needsConnection("Playing a short")
        val fresh = remote.playback(id)
        memory.storePlayback(id, network, fresh)
        return fresh
    }

    // ------------------------------------------------------------- network only

    override suspend fun saveVideo(video: VideoItem) {
        networkOnly("Saving a short") { remote.saveVideo(video) }
        memory.videos = null // the next read goes to the network
    }

    override suspend fun createFolder(name: String, symbolName: String): LibraryFolder =
        networkOnly("Creating a folder") { remote.createFolder(name, symbolName) }

    override suspend fun renameFolder(id: UUID, name: String): LibraryFolder =
        networkOnly("Renaming a folder") { remote.renameFolder(id, name) }

    override suspend fun deleteFolder(id: UUID) =
        networkOnly("Deleting a folder") { remote.deleteFolder(id) }

    // ------------------------------------------------------------- queued when offline

    override suspend fun deleteVideo(id: UUID): VideoDeletion =
        orQueue(PendingAction.Delete(id, now())) { remote.deleteVideo(id) }
            ?: VideoDeletion.local(id, now())

    override suspend fun restoreVideo(video: VideoItem, method: UndoMethod) {
        val restoredLocally = orQueue(PendingAction.Restore(video.id, now(), method)) {
            remote.restoreVideo(video, method)
        }
        if (restoredLocally == null) {
            // Offline: put the short back in the local copy right away.
            updateLocal { videos -> videos.filterNot { it.id == video.id } + video }
        }
    }

    override suspend fun moveVideo(id: UUID, folderID: UUID?) {
        orQueue(PendingAction.Move(id, now(), folderID)) { remote.moveVideo(id, folderID) }
    }

    override suspend fun updateNote(id: UUID, note: String?) {
        orQueue(PendingAction.Note(id, now(), note)) { remote.updateNote(id, note) }
    }

    override suspend fun updateTags(id: UUID, tags: List<String>) {
        orQueue(PendingAction.Tags(id, now(), tags)) { remote.updateTags(id, tags) }
    }

    // ------------------------------------------------------------- helpers

    /**
     * Tries the server; if the phone is offline, applies [action] to the local
     * copy, queues it and returns null so the caller can answer locally.
     */
    private suspend fun <T> orQueue(action: PendingAction, send: suspend () -> T): T? {
        if (isOnline()) {
            try {
                val result = send()
                memory.videos?.let { memory.videos = action.applyTo(it) }
                return result
            } catch (error: Exception) {
                if (!ApiException.isOffline(error)) throw error
            }
        }
        queue.enqueue(action)
        updateLocal { action.applyTo(it) }
        return null
    }

    private suspend fun updateLocal(transform: (List<VideoItem>) -> List<VideoItem>) {
        val updated = store.update { it.copy(videos = transform(it.videos)) }
        memory.videos = updated.videos
    }

    private suspend fun <T> networkOnly(action: String, send: suspend () -> T): T {
        if (!isOnline()) throw OfflineException.needsConnection(action)
        return try {
            send()
        } catch (error: Exception) {
            if (ApiException.isOffline(error)) throw OfflineException.needsConnection(action)
            throw error
        }
    }

    /** Changes still in the outbox are shown even when the list came from the server. */
    private suspend fun withPendingChanges(videos: List<VideoItem>): List<VideoItem> =
        queue.all().fold(videos) { list, action -> action.applyTo(list) }
}
