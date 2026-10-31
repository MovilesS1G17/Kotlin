package com.centralia.app.data.offline

import com.centralia.app.data.local.PendingActionQueue
import com.centralia.app.data.remote.ApiException
import com.centralia.app.data.remote.RemoteLibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Eventual connectivity + multithreading (Sprint 3): sends the outbox to the
 * backend when the phone gets a connection back.
 *
 * - Runs on a background coroutine scope (Dispatchers.IO, SupervisorJob), never
 *   on the main thread, and outlives any single screen.
 * - A Mutex makes sure only one sync runs at a time, even if the network flaps.
 * - Changes are sent in the order the user made them. If the network drops
 *   again it stops and keeps the rest for next time; a change the server
 *   rejects for good (e.g. the short was deleted on the other phone, 404) is
 *   dropped so it doesn't block the queue.
 * - When something was synced, [onSynced] runs on the main thread so the lists
 *   reload with the server's version.
 */
class SyncManager(
    private val remote: RemoteLibraryRepository,
    private val queue: PendingActionQueue,
    private val scope: CoroutineScope,
    private val onSynced: () -> Unit
) {
    private val mutex = Mutex()

    fun requestSync() {
        scope.launch { sync() }
    }

    suspend fun sync(): Int = mutex.withLock {
        var synced = 0
        for (action in queue.all()) {
            try {
                remote.replay(action)
                queue.remove(action)
                synced += 1
            } catch (error: Exception) {
                if (ApiException.isOffline(error)) break
                val permanent = error is ApiException && error.statusCode in 400..499 &&
                    error.statusCode != 408 && error.statusCode != 429
                if (!permanent) break
                queue.remove(action)
            }
        }
        if (synced > 0) withContext(Dispatchers.Main) { onSynced() }
        synced
    }
}
