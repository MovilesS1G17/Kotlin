package com.centralia.app.app

import com.centralia.app.domain.auth.InMemoryRememberedEmailStore
import com.centralia.app.domain.auth.RememberedEmailStore
import com.centralia.app.data.local.DataStoreRememberedEmailStore
import android.content.Context
import com.centralia.app.BuildConfig
import com.centralia.app.data.export.JsonLibraryExportService
import com.centralia.app.data.local.LibraryMemoryCache
import com.centralia.app.data.local.LocalLibraryStore
import com.centralia.app.data.local.PendingActionQueue
import com.centralia.app.data.local.room.CentraliaDatabase
import com.centralia.app.data.local.room.RoomRecentlyViewedRepository
import com.centralia.app.data.mock.MockAuthenticationRepository
import com.centralia.app.data.mock.MockDataStore
import com.centralia.app.data.mock.MockLibraryRepository
import com.centralia.app.data.mock.MockSearchHistoryRepository
import com.centralia.app.data.mock.MockUserRepository
import com.centralia.app.data.mock.MockVideoImportPipeline
import com.centralia.app.data.offline.OfflineFirstLibraryRepository
import com.centralia.app.data.offline.OfflineSearchHistoryRepository
import com.centralia.app.data.offline.OfflineUserRepository
import com.centralia.app.data.offline.SyncManager
import com.centralia.app.data.remote.ApiClient
import com.centralia.app.data.remote.RemoteAuthenticationRepository
import com.centralia.app.data.remote.RemoteLibraryRepository
import com.centralia.app.data.remote.RemoteSearchHistoryRepository
import com.centralia.app.data.remote.RemoteUserRepository
import com.centralia.app.data.remote.RemoteVideoImportPipeline
import com.centralia.app.data.remote.TokenStore
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.InMemoryRecentlyViewedRepository
import com.centralia.app.domain.library.RecentlyViewedRepository
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.profile.LibraryExportService
import com.centralia.app.domain.profile.UserRepository
import com.centralia.app.domain.search.SearchHistoryRepository
import com.centralia.app.platform.ConnectivityObserver
import com.centralia.app.platform.currentNetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.centralia.app.data.analytics.BatchingAnalyticsTracker
import com.centralia.app.data.insights.FileInsightsSnapshotStore
import com.centralia.app.data.local.SharedPreferencesRediscoverDismissals
import com.centralia.app.data.local.room.RoomWatchLaterRepository
import com.centralia.app.data.trends.FileTrendsDiskStore
import com.centralia.app.data.trends.LruTrendsMemoryCache
import com.centralia.app.data.trends.fetchTrends
import com.centralia.app.domain.analytics.AnalyticsTracker
import com.centralia.app.domain.analytics.NoOpAnalyticsTracker
import com.centralia.app.domain.creators.CreatorsCache
import com.centralia.app.domain.insights.InMemoryInsightsSnapshotStore
import com.centralia.app.domain.insights.InsightsSnapshotStore
import com.centralia.app.domain.rediscover.InMemoryRediscoverDismissals
import com.centralia.app.domain.rediscover.RediscoverDismissals
import com.centralia.app.domain.trends.CachedTrendsRepository
import com.centralia.app.domain.trends.InMemoryTrendsDiskStore
import com.centralia.app.domain.trends.MapTrendsMemoryCache
import com.centralia.app.domain.trends.TrendItem
import com.centralia.app.domain.trends.Trends
import com.centralia.app.domain.trends.TrendsRepository
import com.centralia.app.domain.watchlater.InMemoryWatchLaterRepository
import com.centralia.app.domain.watchlater.WatchLaterRepository
import java.time.Instant


class DependencyContainer(
    val authenticationRepository: AuthenticationRepository,
    val videoItemRepository: VideoItemRepository,
    val folderRepository: FolderRepository,
    val searchHistoryRepository: SearchHistoryRepository,
    val userRepository: UserRepository,
    val libraryExportService: LibraryExportService,
    val videoImportPipeline: VideoImportPipeline,
    val session: AppSession = AppSession(),
    val revisions: LibraryRevisions = LibraryRevisions(),
    /** Eventual connectivity: live online/offline state for the banner. */
    val isOnline: StateFlow<Boolean> = MutableStateFlow(true),
    /** Changes made offline that are waiting in the outbox. */
    val pendingChanges: StateFlow<Int> = MutableStateFlow(0),
    /** "Recently viewed" history (Room database on the phone). */
    val recentlyViewed: RecentlyViewedRepository = InMemoryRecentlyViewedRepository(),
    /** "Remember my email" on the sign-in screen (DataStore). */
    val rememberedEmail: RememberedEmailStore = InMemoryRememberedEmailStore(),
    // ---- Sprint 4
    /** Feature events for BQ11-15, batched and sent when online. */
    val analytics: AnalyticsTracker = NoOpAnalyticsTracker,
    /** Rediscover's "Not now" (SharedPreferences). */
    val rediscoverDismissals: RediscoverDismissals = InMemoryRediscoverDismissals(),
    /** Watch later queue (Room table `watch_later`). */
    val watchLater: WatchLaterRepository = InMemoryWatchLaterRepository(),
    /** Trends with a memory LRU + disk cache in front of the API. */
    val trends: TrendsRepository = CachedTrendsRepository(
        fetch = { days -> sampleTrends(days) },
        memory = MapTrendsMemoryCache(),
        disk = InMemoryTrendsDiskStore()
    ),
    /** Grouped creators by library fingerprint (LRU). */
    val creatorsCache: CreatorsCache = CreatorsCache(),
    /** Last Insights snapshot (JSON file). */
    val insightsStore: InsightsSnapshotStore = InMemoryInsightsSnapshotStore()
) {
    companion object {

        /** Trends for the mock build (no server). */
        fun sampleTrends(days: Int) = Trends(
            generatedAt = Instant.now(),
            windowDays = days,
            minUsers = 3,
            totalSaves = 128,
            categories = listOf(TrendItem("Recipes", 34, 12), TrendItem("Fitness", 21, 9), TrendItem("Travel", 17, 8)),
            tags = listOf(TrendItem("Pasta", 15, 7), TrendItem("Workout", 11, 6), TrendItem("Beach", 8, 4)),
            platformCounts = mapOf("tiktok" to 61, "instagramReel" to 33, "youtubeShort" to 34)
        )

        fun make(context: Context): DependencyContainer {
            check(BuildConfig.API_BASE_URL.isNotBlank()) {
                "Set centraliaApiBaseUrl in gradle.properties to the Centralia API address."
            }
            return live(context, BuildConfig.API_BASE_URL)
        }


        fun live(context: Context, baseUrl: String): DependencyContainer {
            val client = ApiClient(baseUrl, TokenStore(context))
            val remoteLibrary = RemoteLibraryRepository(client) { currentNetworkType(context) }
            val session = AppSession()
            val revisions = LibraryRevisions()

            // Sprint 3: offline-first data layer (eventual connectivity, local
            // storage, caching and background work). Background work runs on
            // this scope: Dispatchers.IO, and a SupervisorJob so one failed
            // write doesn't cancel the others.
            val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val connectivity = ConnectivityObserver(context)
            val store = LocalLibraryStore(context.filesDir)
            val outbox = PendingActionQueue(context.filesDir)
            val memoryCache = LibraryMemoryCache()
            // Local storage, relational (Enrique): Room/SQLite for the
            // "Recently viewed" history.
            val database = CentraliaDatabase.build(context)
            val recentlyViewed = RoomRecentlyViewedRepository(database.recentlyViewedDao())
            // ---- Sprint 4
            // Local storage (Alex): the Watch later queue, second table of the same database.
            val watchLater = RoomWatchLaterRepository(database.watchLaterDao())
            // Key/value (Enrique): Rediscover's "Not now".
            val rediscoverDismissals = SharedPreferencesRediscoverDismissals(
                context.getSharedPreferences("centralia_rediscover", Context.MODE_PRIVATE)
            )
            // Caching (David's feature): LruCache in memory + JSON file on disk in front of GET /v1/trends.
            val trends = CachedTrendsRepository(
                fetch = { days -> fetchTrends(client, days) },
                memory = LruTrendsMemoryCache(),
                disk = FileTrendsDiskStore(java.io.File(context.cacheDir, "trends"))
            )
            val creatorsCache = CreatorsCache()
            // Files (Gustavo's feature): the last Insights snapshot.
            val insightsStore = FileInsightsSnapshotStore(java.io.File(context.filesDir, "insights"))
            val libraryRepository = OfflineFirstLibraryRepository(
                remote = remoteLibrary,
                store = store,
                queue = outbox,
                memory = memoryCache,
                ioScope = backgroundScope,
                isOnline = { connectivity.isOnline.value },
                networkType = { currentNetworkType(context) }
            )
            val syncManager = SyncManager(remoteLibrary, outbox, backgroundScope) {
                revisions.refreshAll()
            }
            val userRepository = OfflineUserRepository(
                RemoteUserRepository(client),
                context.getSharedPreferences("centralia_offline_profile", Context.MODE_PRIVATE)
            )
            val searchHistoryRepository = OfflineSearchHistoryRepository(
                RemoteSearchHistoryRepository(client),
                context.getSharedPreferences("centralia_offline_search", Context.MODE_PRIVATE)
            )

            val analytics = BatchingAnalyticsTracker(client, backgroundScope, connectivity.isOnline)

            backgroundScope.launch {
                outbox.refreshCount()
                // Every time the phone comes back online, send what was queued.
                connectivity.isOnline.collect { online -> if (online) syncManager.requestSync() }
            }

            // Signing out removes this user's offline copy from the phone.
            session.onSignOut = {
                memoryCache.clear()
                userRepository.clear()
                creatorsCache.clear()
                backgroundScope.launch {
                    store.clear()
                    outbox.clear()
                    recentlyViewed.clear()
                    // Sprint 4: everything the new features keep on the phone.
                    watchLater.clear()
                    rediscoverDismissals.clear()
                    trends.clear()
                    insightsStore.clear()
                    analytics.clear()
                }
            }

            // An expired or revoked token sends the user back to sign in.
            client.onUnauthorized = { session.signOut() }

            return DependencyContainer(
                authenticationRepository = RemoteAuthenticationRepository(client),
                videoItemRepository = libraryRepository,
                folderRepository = libraryRepository,
                searchHistoryRepository = searchHistoryRepository,
                userRepository = userRepository,
                libraryExportService = JsonLibraryExportService(
                    videoRepository = libraryRepository,
                    folderRepository = libraryRepository,
                    userRepository = userRepository,
                    cacheDirectory = context.cacheDir
                ),
                videoImportPipeline = RemoteVideoImportPipeline(client),
                session = session,
                revisions = revisions,
                isOnline = connectivity.isOnline,
                pendingChanges = outbox.count,
                recentlyViewed = recentlyViewed,
                rememberedEmail = DataStoreRememberedEmailStore(context.applicationContext),
                analytics = analytics,
                rediscoverDismissals = rediscoverDismissals,
                watchLater = watchLater,
                trends = trends,
                creatorsCache = creatorsCache,
                insightsStore = insightsStore
            )
        }


        fun mock(context: Context): DependencyContainer {
            val store = MockDataStore(context.filesDir)
            val libraryRepository = MockLibraryRepository(store)
            val userRepository = MockUserRepository(store)

            return DependencyContainer(
                authenticationRepository = MockAuthenticationRepository(),
                videoItemRepository = libraryRepository,
                folderRepository = libraryRepository,
                searchHistoryRepository = MockSearchHistoryRepository(store),
                userRepository = userRepository,
                libraryExportService = JsonLibraryExportService(
                    videoRepository = libraryRepository,
                    folderRepository = libraryRepository,
                    userRepository = userRepository,
                    cacheDirectory = context.cacheDir
                ),
                videoImportPipeline = MockVideoImportPipeline()
            )
        }
    }
}
