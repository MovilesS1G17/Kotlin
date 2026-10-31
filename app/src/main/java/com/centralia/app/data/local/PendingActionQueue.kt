package com.centralia.app.data.local

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Local storage + eventual connectivity (Sprint 3): the outbox of changes made
 * offline, saved as `files/offline/outbox.json` so they survive closing the
 * app or restarting the phone. [SyncManager] empties it when the network is
 * back. [count] feeds the "changes waiting to sync" banner.
 */
class PendingActionQueue(directory: File) {

    private val file = File(directory, "offline/outbox.json")
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val serializer = ListSerializer(PendingAction.serializer())

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    suspend fun all(): List<PendingAction> = mutex.withLock { load() }

    suspend fun enqueue(action: PendingAction) = mutate { PendingAction.enqueue(it, action) }

    suspend fun remove(action: PendingAction) = mutate { current -> current.filterNot { it == action } }

    suspend fun clear() = mutate { emptyList() }

    /** Loads the count once at start-up so the banner is right after a restart. */
    suspend fun refreshCount() {
        val size = all().size
        _count.value = size
    }

    private suspend fun mutate(transform: (List<PendingAction>) -> List<PendingAction>) =
        mutex.withLock {
            val next = transform(load())
            withContext(Dispatchers.IO) {
                file.parentFile?.mkdirs()
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(json.encodeToString(serializer, next))
                if (!temp.renameTo(file)) {
                    file.delete()
                    temp.renameTo(file)
                }
            }
            _count.value = next.size
        }

    private suspend fun load(): List<PendingAction> = withContext(Dispatchers.IO) {
        if (!file.exists()) emptyList()
        else runCatching { json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
    }
}
