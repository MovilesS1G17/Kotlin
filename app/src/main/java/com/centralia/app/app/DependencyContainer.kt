package com.centralia.app.app

import android.content.Context
import com.centralia.app.BuildConfig
import com.centralia.app.data.export.JsonLibraryExportService
import com.centralia.app.data.local.LibraryMemoryCache
import com.centralia.app.data.local.LocalLibraryStore
import com.centralia.app.data.local.PendingActionQueue
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
    val pendingChanges: StateFlow<Int> = MutableStateFlow(0)
) {
    companion object {

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

            backgroundScope.launch {
                outbox.refreshCount()
                // Every time the phone comes back online, send what was queued.
                connectivity.isOnline.collect { online -> if (online) syncManager.requestSync() }
            }

            // Signing out removes this user's offline copy from the phone.
            session.onSignOut = {
                memoryCache.clear()
                userRepository.clear()
                backgroundScope.launch {
                    store.clear()
                    outbox.clear()
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
                pendingChanges = outbox.count
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
