package com.centralia.app

import com.centralia.app.domain.analytics.AnalyticsEvent
import com.centralia.app.domain.analytics.EventBuffer
import com.centralia.app.domain.creators.Creators
import com.centralia.app.domain.creators.CreatorsCache
import com.centralia.app.domain.insights.Insights
import com.centralia.app.domain.library.ContentAnalysisStatus
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.rediscover.InMemoryRediscoverDismissals
import com.centralia.app.domain.rediscover.Rediscover
import com.centralia.app.domain.search.SearchFilter
import com.centralia.app.domain.search.SearchFolderFilter
import com.centralia.app.domain.search.SearchInput
import com.centralia.app.domain.trends.CachedTrendsRepository
import com.centralia.app.domain.trends.InMemoryTrendsDiskStore
import com.centralia.app.domain.trends.MapTrendsMemoryCache
import com.centralia.app.domain.trends.TrendItem
import com.centralia.app.domain.trends.Trends
import com.centralia.app.domain.trends.TrendsSource
import com.centralia.app.domain.trends.TrendsUnavailableException
import com.centralia.app.domain.watchlater.InMemoryWatchLaterRepository
import com.centralia.app.domain.watchlater.WatchLaterRules
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 4: the five new features and the micro-optimizations, on the JVM. Same cases as `Sprint4FeaturesTests.swift`. */
class Sprint4FeaturesTest {

    private val now = Instant.parse("2026-11-26T15:00:00Z") // a Thursday
    private val recipes = LibraryFolder(UUID.randomUUID(), "Recipes", "fork.knife")

    private fun video(
        daysAgo: Long,
        creator: String = "@cook",
        platform: VideoPlatform = VideoPlatform.TIKTOK,
        folder: LibraryFolder? = null,
        tags: List<String> = emptyList(),
        caption: String = "A short"
    ) = VideoItem(
        id = UUID.randomUUID(),
        sourceURL = "https://www.tiktok.com/@x/video/${UUID.randomUUID()}",
        platform = platform,
        creator = creator,
        durationSeconds = 30,
        sourceCaption = caption,
        folderID = folder?.id,
        tags = tags,
        savedAt = now.minus(Duration.ofDays(daysAgo)),
        analysisStatus = ContentAnalysisStatus.COMPLETED
    )

    // ---- Rediscover (Enrique)

    @Test
    fun `rediscover only picks old shorts not opened lately`() {
        val fresh = video(daysAgo = 2)
        val old = video(daysAgo = 20)
        val oldButSeen = video(daysAgo = 30)
        val oldSeenLongAgo = video(daysAgo = 40)
        val dismissed = video(daysAgo = 25)
        val lastViewed = mapOf(
            oldButSeen.id to now.minus(Duration.ofDays(3)),
            oldSeenLongAgo.id to now.minus(Duration.ofDays(20))
        )
        val picks = Rediscover.pick(listOf(fresh, old, oldButSeen, oldSeenLongAgo, dismissed), lastViewed, setOf(dismissed.id), now, ZoneOffset.UTC)
        assertEquals(setOf(old.id, oldSeenLongAgo.id), picks.map { it.video.id }.toSet())
        val never = picks.first { it.video.id == old.id }
        assertNull(never.daysSinceViewed)
        assertEquals("Saved 20 days ago and never opened since", never.reason)
        assertEquals("Last opened 20 days ago", picks.first { it.video.id == oldSeenLongAgo.id }.reason)
    }

    @Test
    fun `rediscover in parallel gives the same picks as sequential and is stable within a day`() = runBlocking {
        val library = (1..300).map { video(daysAgo = (it % 90).toLong()) }
        val sequential = Rediscover.pick(library, emptyMap(), emptySet(), now, ZoneOffset.UTC)
        val parallel = Rediscover.pickInParallel(library, emptyMap(), emptySet(), now, ZoneOffset.UTC)
        assertEquals(sequential.map { it.video.id }, parallel.map { it.video.id })
        assertEquals(Rediscover.LIMIT, parallel.size)
        val laterSameDay = Rediscover.pick(library, emptyMap(), emptySet(), now.plus(Duration.ofHours(5)), ZoneOffset.UTC)
        assertEquals(sequential.map { it.video.id }.toSet().size, laterSameDay.size)
    }

    @Test
    fun `not now hides a short for 30 days`() = runBlocking {
        val dismissals = InMemoryRediscoverDismissals()
        val id = UUID.randomUUID()
        dismissals.dismiss(id, now)
        assertTrue(id in dismissals.active(now.plus(Duration.ofDays(29))))
        assertFalse(id in dismissals.active(now.plus(Duration.ofDays(31))))
    }

    // ---- Watch later (Alex)

    @Test
    fun `watch later keeps order, ignores duplicates and moves entries`() = runBlocking {
        var clock = now
        val repo = InMemoryWatchLaterRepository { clock }
        val a = video(1); val b = video(2); val c = video(3)
        assertTrue(repo.add(a)); assertTrue(repo.add(b)); assertTrue(repo.add(c))
        assertFalse(repo.add(a))
        assertEquals(listOf(a.id, b.id, c.id), repo.observe().first().map { it.videoID })
        repo.move(c.id, -2)
        assertEquals(listOf(c.id, a.id, b.id), repo.observe().first().map { it.videoID })
        assertEquals(listOf(0, 1, 2), repo.observe().first().map { it.position })
        clock = now.plus(Duration.ofMinutes(90))
        val before = repo.markWatched(a.id)!!
        assertEquals(1.5, before.hoursInQueue(clock), 0.001)
        // watched entries go to the end
        assertEquals(listOf(c.id, b.id, a.id), repo.observe().first().map { it.videoID })
        repo.clearWatched()
        assertEquals(listOf(c.id, b.id), repo.observe().first().map { it.videoID })
    }

    @Test
    fun `watch later hides shorts deleted from the library`() = runBlocking {
        val repo = InMemoryWatchLaterRepository { now }
        val kept = video(1); val deleted = video(2)
        repo.add(kept); repo.add(deleted)
        val visible = WatchLaterRules.visible(repo.observe().first(), listOf(kept))
        assertEquals(listOf(kept.id), visible.map { it.first.id })
    }

    // ---- Trends cache (David)

    private fun trends(n: Int) = Trends(now, 7, 3, n, listOf(TrendItem("Recipes", n, 3)), emptyList(), mapOf("tiktok" to n))

    @Test
    fun `trends come from memory, then disk, then network, and fall back when offline`() = runBlocking {
        var clock = now
        var calls = 0
        var online = true
        val disk = InMemoryTrendsDiskStore()
        val memory = MapTrendsMemoryCache()
        val repo = CachedTrendsRepository(
            fetch = { calls++; if (!online) throw java.io.IOException("offline"); trends(calls) },
            memory = memory, disk = disk, now = { clock }
        )
        assertEquals(TrendsSource.NETWORK, repo.load(7).source)
        assertEquals(TrendsSource.MEMORY, repo.load(7).source)
        memory.clear() // the app was closed: memory is gone, the file is not
        assertEquals(TrendsSource.DISK, repo.load(7).source)
        assertEquals(1, calls)

        clock = now.plus(Duration.ofMinutes(11)) // expired
        online = false
        val stale = repo.load(7)
        assertTrue(stale.isStale)
        assertEquals(1, stale.trends.totalSaves)
        assertEquals("Updated 11 min ago", CachedTrendsRepository.updatedLabel(stale.fetchedAt, clock))

        online = true
        val refreshed = repo.load(7, forceRefresh = true)
        assertEquals(TrendsSource.NETWORK, refreshed.source)
        assertFalse(refreshed.isStale)
    }

    @Test
    fun `trends with nothing cached and no network says it needs a connection once`() = runBlocking {
        val repo = CachedTrendsRepository(
            fetch = { throw java.io.IOException("offline") },
            memory = MapTrendsMemoryCache(), disk = InMemoryTrendsDiskStore(), now = { now }
        )
        val error = runCatching { repo.load(7) }.exceptionOrNull()
        assertTrue(error is TrendsUnavailableException)
        assertEquals(CachedTrendsRepository.OFFLINE_MESSAGE, error!!.message)
    }

    @Test
    fun `the memory cache is an lru`() {
        val cache = MapTrendsMemoryCache(capacity = 2)
        val value = com.centralia.app.domain.trends.CachedTrends(trends(1), now)
        cache.put(7, value); cache.put(30, value)
        cache.get(7)
        cache.put(14, value)
        assertTrue(cache.get(7) != null)
        assertNull(cache.get(30))
    }

    // ---- Creators (Alejandro)

    @Test
    fun `creators group the same handle across platforms, most saved first`() = runBlocking {
        val library = listOf(
            video(1, "@Cook"), video(5, "cook", VideoPlatform.INSTAGRAM_REEL), video(3, "@cook"),
            video(2, "@gymrat"), video(9, "  ")
        )
        val groups = Creators.groupInParallel(library)
        assertEquals(listOf(3, 1), groups.map { it.count })
        assertEquals("@Cook", groups[0].handle)
        assertEquals(listOf(VideoPlatform.TIKTOK, VideoPlatform.INSTAGRAM_REEL), groups[0].platforms)
        assertEquals("C", groups[0].initial)
        assertEquals(Creators.group(library).map { it.handle }, groups.map { it.handle })
    }

    @Test
    fun `creators cache is reused for the same library and not for a changed one`() {
        val library = listOf(video(1), video(2, "@other"))
        val cache = CreatorsCache()
        val key = Creators.fingerprint(library)
        cache.put(key, Creators.group(library))
        assertTrue(cache.get(Creators.fingerprint(library.toList())) != null)
        assertNull(cache.get(Creators.fingerprint(library + video(3))))
        assertEquals(1, cache.hits)
    }

    // ---- Insights (Gustavo)

    @Test
    fun `insights count saves per week, streak, folders and tags`() {
        val library = listOf(
            video(0, folder = recipes, tags = listOf("Pasta")), video(1, tags = listOf("pasta", "dinner")), // this week (Mon 23 - Thu 26)
            video(4), // last week (Sun 22)
            video(12, platform = VideoPlatform.YOUTUBE_SHORT), // two weeks ago
            video(60) // outside 8 weeks
        )
        val snapshot = Insights.compute(library, now, ZoneOffset.UTC)
        assertEquals(8, snapshot.weeks.size)
        assertEquals("2026-11-23", snapshot.weeks.last().weekStart)
        assertEquals(2, snapshot.thisWeek)
        assertEquals(1, snapshot.lastWeek)
        assertEquals(1, snapshot.weekChange)
        assertEquals(3, snapshot.streakWeeks)
        assertEquals(20, snapshot.inFolderPct)
        assertEquals(listOf("pasta", "dinner"), snapshot.topTags)
        assertEquals(mapOf("tiktok" to 4, "youtubeShort" to 1), snapshot.platformCounts)
        assertEquals("Nov 23", Insights.shortLabel("2026-11-23"))
        assertNull(Insights.sinceLastVisit(null, snapshot))
        assertEquals(2, Insights.sinceLastVisit(snapshot.copy(totalSaves = 3), snapshot))
    }

    // ---- Analytics buffer (eventual connectivity for the BQ events)

    @Test
    fun `failed batches go back to the front and the buffer is capped`() {
        val buffer = EventBuffer(capacity = 3)
        (1..4).forEach { buffer.add(AnalyticsEvent("e$it", now)) }
        assertEquals(3, buffer.size) // e1 dropped
        val batch = buffer.takeBatch(2)
        assertEquals(listOf("e2", "e3"), batch.map { it.name })
        buffer.restore(batch)
        assertEquals(listOf("e2", "e3", "e4"), buffer.takeBatch().map { it.name })
    }

    // ---- Micro-optimizations keep the same results

    @Test
    fun `indexed search matches accents, tags and folders like before`() {
        val a = video(1, caption = "Crème brûlée in 5 minutes", folder = recipes, tags = listOf("Dessert"))
        val b = video(1, creator = "@gym", caption = "Leg day")
        val base = SearchInput(listOf(a, b), listOf(recipes), "", null, null, SearchFolderFilter.All, emptySet())
        assertEquals(listOf(a), SearchFilter.apply(base.copy(query = "creme BRULEE")))
        assertEquals(listOf(a), SearchFilter.apply(base.copy(query = "recipes dessert")))
        assertEquals(listOf(a), SearchFilter.apply(base.copy(tags = setOf("dessert"))))
        assertEquals(listOf(b), SearchFilter.apply(base.copy(query = "gym")))
        assertEquals(emptyList<VideoItem>(), SearchFilter.apply(base.copy(query = "gym dessert")))
        assertEquals(listOf(a, b), SearchFilter.apply(base))
    }
}
