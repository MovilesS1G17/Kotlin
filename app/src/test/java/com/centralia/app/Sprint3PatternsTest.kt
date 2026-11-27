package com.centralia.app

import com.centralia.app.data.cache.FolderCountCache
import com.centralia.app.domain.auth.InMemoryRememberedEmailStore
import com.centralia.app.domain.imports.LinkProblem
import com.centralia.app.domain.library.ContentAnalysisStatus
import com.centralia.app.domain.library.InMemoryRecentlyViewedRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.RecentlyViewedRepository
import com.centralia.app.domain.library.TagRules
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.library.recentlyViewedVideos
import com.centralia.app.domain.search.SearchFilter
import com.centralia.app.domain.search.SearchFolderFilter
import com.centralia.app.domain.search.SearchInput
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The TA's three app-side corrections and the new Sprint 3 patterns, tested on
 * the JVM (no Android needed). Same cases as `TAFeedbackTests.swift` on iOS.
 */
class Sprint3PatternsTest {

    private val start = Instant.parse("2026-11-20T10:00:00Z")
    private val recipes = LibraryFolder(UUID.randomUUID(), "Recipes", "fork.knife")
    private val gym = LibraryFolder(UUID.randomUUID(), "Gym", "dumbbell")

    private fun video(
        creator: String,
        caption: String,
        folder: LibraryFolder? = null,
        tags: List<String> = emptyList(),
        platform: VideoPlatform = VideoPlatform.TIKTOK
    ) = VideoItem(
        id = UUID.randomUUID(),
        sourceURL = "https://www.tiktok.com/@x/video/${UUID.randomUUID()}",
        platform = platform,
        creator = creator,
        durationSeconds = 30,
        sourceCaption = caption,
        folderID = folder?.id,
        tags = tags,
        savedAt = start,
        analysisStatus = ContentAnalysisStatus.COMPLETED
    )

    // ---- TA: "when the tag is very long the data leaves the screen"

    @Test
    fun `tag field never holds more than 30 characters`() {
        assertEquals(30, TagRules.limitInput("a".repeat(45)).length)
        assertEquals("short", TagRules.limitInput("short"))
        assertEquals("9/30", TagRules.counter("  two   words "))
        assertTrue(TagRules.isValid("x".repeat(30)))
        assertFalse(TagRules.isValid("x".repeat(31)))
        assertFalse(TagRules.isValid("   "))
    }

    // ---- TA: "Done should save what you just did"

    @Test
    fun `a typed tag is added once on Done or Save, ignoring case`() {
        assertEquals(listOf("gym", "Meal prep"), TagRules.addPending(listOf("gym"), "  Meal  prep "))
        assertEquals(listOf("gym"), TagRules.addPending(listOf("gym"), "GYM"))
        assertEquals(listOf("gym"), TagRules.addPending(listOf("gym"), ""))
    }

    // ---- TA: "when saving an inadmissible link, make it more telling"

    @Test
    fun `a rejected link shows the server's title and how to fix it`() {
        val problem = LinkProblem.from(
            code = "unsupported_source",
            title = "This TikTok link isn't a video",
            detail = "It points to a profile, not to one video. Open the video, tap Share → Copy link, and paste it here."
        )
        assertEquals("This TikTok link isn't a video", problem.title)
        assertTrue(problem.message.contains("Copy link"))
    }

    @Test
    fun `a rejection without title still gets a specific one`() {
        assertEquals("This is a regular YouTube video", LinkProblem.from("unsupported_youtube_video", null, "x").title)
        assertEquals(LinkProblem.DEFAULT_TITLE, LinkProblem.from("something_else", null, null).title)
        assertEquals(LinkProblem.OFFLINE_TITLE, LinkProblem.from("unreachable", null, "x").title)
    }

    // ---- Alex · multithreading: the search the Flow pipeline runs on Dispatchers.Default

    @Test
    fun `search matches every term in title, creator, folder or tag`() {
        val pasta = video("@chef", "Creamy pasta in 10 minutes", recipes, listOf("dinner"))
        val squat = video("@coach", "Squat form", gym, listOf("legs"), VideoPlatform.YOUTUBE_SHORT)
        val loose = video("@chef", "Morning routine")
        val input = SearchInput(
            videos = listOf(pasta, squat, loose),
            folders = listOf(recipes, gym),
            query = "",
            platform = null,
            creator = null,
            folder = SearchFolderFilter.All,
            tags = emptySet()
        )
        assertEquals(listOf(pasta), SearchFilter.apply(input.copy(query = "recipes dinner")))
        assertEquals(listOf(pasta, loose), SearchFilter.apply(input.copy(creator = "@chef")))
        assertEquals(listOf(loose), SearchFilter.apply(input.copy(folder = SearchFolderFilter.Unorganized)))
        assertEquals(listOf(squat), SearchFilter.apply(input.copy(platform = VideoPlatform.YOUTUBE_SHORT)))
        assertEquals(listOf(squat), SearchFilter.apply(input.copy(tags = setOf("legs"))))
        assertEquals(3, SearchFilter.apply(input).size)
    }

    // ---- Alex · caching: ArrayMap folder counts

    @Test
    fun `folder counts are built once and rebuilt when the list changes`() {
        val cache = FolderCountCache()
        val first = listOf(video("@a", "1", recipes), video("@b", "2", recipes), video("@c", "3", gym), video("@d", "4"))
        assertEquals(2, cache.count(first, recipes.id))
        assertEquals(1, cache.count(first, gym.id))
        assertEquals(1, cache.count(first, null))
        assertEquals(2, cache.size)

        // A new list (e.g. after a move) rebuilds; the same list does not.
        val moved = first.map { if (it.folderID == gym.id) it.copy(folderID = recipes.id) else it }
        assertEquals(3, cache.count(moved, recipes.id))
        assertEquals(0, cache.count(moved, gym.id))
        assertEquals(1, cache.size)
    }

    // ---- Enrique · local storage: Recently viewed (same behaviour as the Room DAO)

    @Test
    fun `recently viewed keeps the newest first and counts repeat visits`() = runBlocking {
        var now = start
        val repository = InMemoryRecentlyViewedRepository { now }
        val a = video("@a", "A")
        val b = video("@b", "B")

        repository.record(a)
        now = now.plusSeconds(60)
        repository.record(b)
        now = now.plusSeconds(60)
        repository.record(a)

        val entries = repository.observe().first()
        assertEquals(listOf(a.id, b.id), entries.map { it.videoID })
        assertEquals(2, entries.first().viewCount)

        // A deleted short drops out of the row even if it's still in the table.
        val shown = recentlyViewedVideos(entries, listOf(b))
        assertEquals(listOf(b.id), shown.map { it.first.id })

        repository.remove(b.id)
        assertEquals(listOf(a.id), repository.observe().first().map { it.videoID })
        repository.clear()
        assertTrue(repository.observe().first().isEmpty())
    }

    @Test
    fun `recently viewed keeps at most 50 shorts and shows 10`() = runBlocking {
        var now = start
        val repository = InMemoryRecentlyViewedRepository { now }
        repeat(60) {
            now = now.plusSeconds(1)
            repository.record(video("@c$it", "Short $it"))
        }
        assertEquals(10, repository.observe().first().size)
        assertEquals(RecentlyViewedRepository.MAX_STORED, repository.observe(limit = 100).first().size)
        assertEquals("Short 59", repository.observe().first().first().title)
    }

    // ---- Alex · local storage: remembered email (DataStore on the phone)

    @Test
    fun `remembered email can be saved and forgotten`() = runBlocking {
        val store = InMemoryRememberedEmailStore()
        assertNull(store.email())
        store.remember("ana@example.com")
        assertEquals("ana@example.com", store.email())
        store.forget()
        assertNull(store.email())
    }
}
