package com.centralia.app.domain.analytics

import java.time.Instant

/**
 * A UI event for the business questions (Sprint 4). The apps send them to
 * `POST /v1/analytics/events`; names the server records itself
 * (`video_saved`, ...) are refused by the API, so they are never built here.
 * Property values are strings, numbers, booleans or null.
 */
data class AnalyticsEvent(
    val name: String,
    val occurredAt: Instant,
    val properties: Map<String, Any?> = emptyMap()
)

/** What the screens depend on. Fire-and-forget: never blocks, never throws. */
interface AnalyticsTracker {
    fun track(name: String, properties: Map<String, Any?> = emptyMap())
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(name: String, properties: Map<String, Any?>) = Unit
}

/** The Sprint 4 events, one family per new feature (BQ11-15). */
object Sprint4Events {
    const val REDISCOVER_VIEWED = "rediscover_viewed"   // shown
    const val REDISCOVER_OPENED = "rediscover_opened"   // video_id, days_since_save
    const val WATCH_LATER_ADDED = "watch_later_added"   // video_id
    const val WATCH_LATER_OPENED = "watch_later_opened" // video_id, hours_in_queue
    const val TRENDS_LOADED = "trends_loaded"           // source, duration_ms, stale
    const val CREATORS_VIEWED = "creators_viewed"       // creators
    const val INSIGHTS_VIEWED = "insights_viewed"       // weeks
}

/**
 * The events waiting to be sent (eventual connectivity): kept in order, at
 * most [capacity] (the oldest are dropped), handed out in batches and put back
 * at the front when a send fails, so nothing is lost while the phone is
 * offline. Not thread-safe by itself: the tracker guards it with a Mutex.
 */
class EventBuffer(private val capacity: Int = 200) {
    private val events = ArrayDeque<AnalyticsEvent>()

    val size: Int get() = events.size

    fun add(event: AnalyticsEvent) {
        events.addLast(event)
        while (events.size > capacity) events.removeFirst()
    }

    fun takeBatch(max: Int = MAX_BATCH): List<AnalyticsEvent> {
        val count = minOf(max, events.size)
        return List(count) { events.removeFirst() }
    }

    fun restore(batch: List<AnalyticsEvent>) {
        for (event in batch.asReversed()) events.addFirst(event)
        while (events.size > capacity) events.removeLast()
    }

    companion object {
        /** `POST /v1/analytics/events` accepts at most 100 events per call. */
        const val MAX_BATCH = 100
    }
}
