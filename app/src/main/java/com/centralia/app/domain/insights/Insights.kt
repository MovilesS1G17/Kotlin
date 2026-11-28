package com.centralia.app.domain.insights

import com.centralia.app.domain.InstantSerializer
import com.centralia.app.domain.library.VideoItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class WeekBar(
    /** Monday of the week, ISO date (2026-11-23). */
    val weekStart: String,
    val saves: Int
)

/**
 * Insights (Sprint 4, Gustavo): the user's own saving habits, computed on the
 * phone from the library (so it works offline). The last snapshot is kept on
 * the phone (local storage): the screen shows it instantly on open, and the
 * difference with the new one is "Since your last visit".
 */
@Serializable
data class InsightsSnapshot(
    @Serializable(with = InstantSerializer::class)
    val computedAt: Instant,
    val totalSaves: Int,
    val weeks: List<WeekBar>,
    val platformCounts: Map<String, Int>,
    /** % of shorts in a folder. */
    val inFolderPct: Int,
    val topTags: List<String>,
    /** Weeks in a row, up to this one, with at least one save. */
    val streakWeeks: Int
) {
    val thisWeek: Int get() = weeks.lastOrNull()?.saves ?: 0
    val lastWeek: Int get() = weeks.getOrNull(weeks.size - 2)?.saves ?: 0
    val weekChange: Int get() = thisWeek - lastWeek
    val averagePerWeek: Double get() = if (weeks.isEmpty()) 0.0 else weeks.sumOf { it.saves }.toDouble() / weeks.size
}

object Insights {
    const val WEEKS = 8

    fun compute(
        videos: List<VideoItem>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
        weeks: Int = WEEKS
    ): InsightsSnapshot {
        val thisMonday = now.atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val starts = (weeks - 1 downTo 0).map { thisMonday.minusWeeks(it.toLong()) }
        val counts = IntArray(weeks)
        val first = starts.first()
        for (video in videos) {
            val day = video.savedAt.atZone(zone).toLocalDate()
            if (day.isBefore(first)) continue
            val index = ((day.toEpochDay() - first.toEpochDay()) / 7).toInt()
            if (index in 0 until weeks) counts[index]++
        }
        val allMondays = videos.map {
            it.savedAt.atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }.toHashSet()
        var streak = 0
        var monday: LocalDate = thisMonday
        while (monday in allMondays) {
            streak++
            monday = monday.minusWeeks(1)
        }
        val tagCounts = HashMap<String, Int>()
        for (video in videos) for (tag in video.tags.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.distinct()) {
            tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
        }
        return InsightsSnapshot(
            computedAt = now,
            totalSaves = videos.size,
            weeks = starts.mapIndexed { i, start -> WeekBar(start.toString(), counts[i]) },
            platformCounts = videos.groupingBy { it.platform.rawValue }.eachCount(),
            inFolderPct = if (videos.isEmpty()) 0 else Math.round(100.0 * videos.count { it.folderID != null } / videos.size).toInt(),
            topTags = tagCounts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(5).map { it.key },
            streakWeeks = streak
        )
    }

    /** New saves since the stored snapshot, or null on the first visit. */
    fun sinceLastVisit(previous: InsightsSnapshot?, current: InsightsSnapshot): Int? =
        previous?.let { current.totalSaves - it.totalSaves }

    /** "Nov 23" for a bar label. */
    fun shortLabel(weekStart: String): String {
        val date = LocalDate.parse(weekStart)
        val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
        return "$month ${date.dayOfMonth}"
    }
}

/** Where the last snapshot is kept (Android: a JSON file; tests: memory). */
interface InsightsSnapshotStore {
    suspend fun load(): InsightsSnapshot?
    suspend fun save(snapshot: InsightsSnapshot)
    suspend fun clear()
}

class InMemoryInsightsSnapshotStore : InsightsSnapshotStore {
    @Volatile private var snapshot: InsightsSnapshot? = null
    override suspend fun load() = snapshot
    override suspend fun save(snapshot: InsightsSnapshot) { this.snapshot = snapshot }
    override suspend fun clear() { snapshot = null }
}
