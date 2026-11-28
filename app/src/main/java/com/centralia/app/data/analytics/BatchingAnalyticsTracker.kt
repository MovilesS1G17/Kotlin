package com.centralia.app.data.analytics

import com.centralia.app.data.remote.ApiClient
import com.centralia.app.domain.analytics.AnalyticsEvent
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.analytics.EventBuffer
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Sends the Sprint 4 feature events (BQ11-15) to `POST /v1/analytics/events`.
 *
 * `track` only adds to a buffer and returns (it never waits on the network).
 * A batch goes out [flushDelayMs] after the first event or as soon as
 * [batchSize] are waiting, on [scope] (Dispatchers.IO).
 *
 * Eventual connectivity: offline, nothing is sent; a failed batch goes back
 * to the front of the buffer and is sent when the phone is online again
 * ([isOnline] turning true triggers a flush). At most 200 events wait.
 */
class BatchingAnalyticsTracker(
    private val client: ApiClient,
    private val scope: CoroutineScope,
    private val isOnline: StateFlow<Boolean>,
    private val batchSize: Int = 20,
    private val flushDelayMs: Long = 10_000
) : AnalyticsTracker {
    private val mutex = Mutex()
    private val buffer = EventBuffer()
    private var scheduled: Job? = null

    init {
        scope.launch { isOnline.collect { online -> if (online) flush() } }
    }

    override fun track(name: String, properties: Map<String, Any?>) {
        val event = AnalyticsEvent(name, Instant.now(), properties)
        scope.launch {
            val sendNow = mutex.withLock {
                buffer.add(event)
                buffer.size >= batchSize
            }
            if (sendNow) flush() else schedule()
        }
    }

    private fun schedule() {
        if (scheduled?.isActive == true) return
        scheduled = scope.launch {
            delay(flushDelayMs)
            flush()
        }
    }

    suspend fun flush() {
        if (!isOnline.value) return
        while (true) {
            val batch = mutex.withLock { buffer.takeBatch() }
            if (batch.isEmpty()) return
            try {
                client.execute(ApiClient.Method.POST, "v1/analytics/events", body = encode(batch))
            } catch (error: Exception) {
                mutex.withLock { buffer.restore(batch) }
                return // tried again when the connection comes back or on the next event
            }
        }
    }

    /** Drops what is waiting (sign-out: the next user must not send them). */
    suspend fun clear() = mutex.withLock { buffer.takeBatch(Int.MAX_VALUE); Unit }

    private fun encode(batch: List<AnalyticsEvent>) = buildJsonObject {
        putJsonArray("events") {
            for (event in batch) addJsonObject {
                put("name", event.name)
                put("occurredAt", event.occurredAt.toString())
                putJsonObject("properties") {
                    for ((key, value) in event.properties) {
                        put(
                            key,
                            when (value) {
                                null -> JsonNull
                                is Boolean -> JsonPrimitive(value)
                                is Number -> JsonPrimitive(value)
                                else -> JsonPrimitive(value.toString())
                            }
                        )
                    }
                }
            }
        }
    }
}
