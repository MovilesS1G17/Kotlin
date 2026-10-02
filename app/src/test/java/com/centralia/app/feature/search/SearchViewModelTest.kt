package com.centralia.app.feature.search

import com.centralia.app.data.mock.MockDataStore
import com.centralia.app.data.mock.MockLibraryRepository
import com.centralia.app.data.mock.MockSearchHistoryRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.search.SearchRepository
import com.centralia.app.domain.search.SearchResult
import com.centralia.app.feature.library.LoadState
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the backend-backed search path added for Global Search
 * (BQ1): a filter change should call [SearchRepository.search], measure
 * latency, and report `search_completed` telemetry only when at least one
 * filter is active. [APISearchRepository] itself can't be unit-tested on
 * the JVM — it goes through `CentraliaApi`/`SecureTokenStore`, which needs
 * real `AndroidKeyStore` and only runs on-device — so this uses a fake
 * [SearchRepository], the same seam `APISearchRepository` implements.
 */
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeSearchRepository : SearchRepository {
        var result: Result<SearchResult> = Result.success(SearchResult(emptyList(), 0))
        var searchCallCount = 0
        val reportedEvents = mutableListOf<Triple<Int, Int, Int>>()

        override suspend fun search(
            query: String?,
            platform: VideoPlatform?,
            creator: String?,
            folderID: UUID?,
            tag: String?,
            limit: Int?,
            offset: Int
        ): SearchResult {
            searchCallCount++
            return result.getOrThrow()
        }

        override fun reportSearchCompleted(durationMs: Int, resultCount: Int, filterCount: Int) {
            reportedEvents.add(Triple(durationMs, resultCount, filterCount))
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun tempStore(): MockDataStore {
        val directory = File.createTempFile("search-vm-test", "").apply {
            delete()
            mkdirs()
        }
        return MockDataStore(directory)
    }

    private fun makeViewModel(
        store: MockDataStore,
        fake: FakeSearchRepository
    ): SearchViewModel {
        val libraryRepository = MockLibraryRepository(store, "library.json")
        return SearchViewModel(
            videoRepository = libraryRepository,
            folderRepository = libraryRepository,
            searchRepository = fake,
            searchHistoryRepository = MockSearchHistoryRepository(store, "history.json")
        )
    }

    @Test
    fun `selecting a filter runs a search and reports latency`() = runTest(dispatcher) {
        val fake = FakeSearchRepository()
        fake.result = Result.success(SearchResult(emptyList(), 3))
        val viewModel = makeViewModel(tempStore(), fake)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPlatformChange(VideoPlatform.TIKTOK)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LoadState.Loaded, viewModel.uiState.value.state)
        assertEquals(1, fake.searchCallCount)
        assertEquals(1, fake.reportedEvents.size)
        assertEquals(3, fake.reportedEvents[0].second)
        assertEquals(1, fake.reportedEvents[0].third)
    }

    @Test
    fun `search with no active filters does not report telemetry`() = runTest(dispatcher) {
        val fake = FakeSearchRepository()
        val viewModel = makeViewModel(tempStore(), fake)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.performSearch()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(fake.reportedEvents.isEmpty())
    }

    @Test
    fun `failed search shows a retryable error and retry repeats the search`() = runTest(dispatcher) {
        val fake = FakeSearchRepository()
        fake.result = Result.failure(RuntimeException("offline"))
        val viewModel = makeViewModel(tempStore(), fake)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCreatorChange("chef.maria")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.state is LoadState.Failed)
        assertEquals(1, fake.searchCallCount)

        fake.result = Result.success(SearchResult(emptyList(), 0))
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fake.searchCallCount)
        assertEquals(LoadState.Loaded, viewModel.uiState.value.state)
    }
}
