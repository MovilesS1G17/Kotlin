package com.centralia.app.app

import android.content.Context
import com.centralia.app.BuildConfig
import com.centralia.app.data.export.JsonLibraryExportService
import com.centralia.app.data.mock.MockAuthenticationRepository
import com.centralia.app.data.mock.MockDataStore
import com.centralia.app.data.mock.MockLibraryRepository
import com.centralia.app.data.mock.MockSearchHistoryRepository
import com.centralia.app.data.mock.MockUserRepository
import com.centralia.app.data.mock.MockVideoImportPipeline
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


class DependencyContainer(
    val authenticationRepository: AuthenticationRepository,
    val videoItemRepository: VideoItemRepository,
    val folderRepository: FolderRepository,
    val searchHistoryRepository: SearchHistoryRepository,
    val userRepository: UserRepository,
    val libraryExportService: LibraryExportService,
    val videoImportPipeline: VideoImportPipeline,
    val session: AppSession = AppSession(),
    val revisions: LibraryRevisions = LibraryRevisions()
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
            val libraryRepository = RemoteLibraryRepository(client)
            val userRepository = RemoteUserRepository(client)
            val session = AppSession()

            // An expired or revoked token sends the user back to sign in.
            client.onUnauthorized = { session.signOut() }

            return DependencyContainer(
                authenticationRepository = RemoteAuthenticationRepository(client),
                videoItemRepository = libraryRepository,
                folderRepository = libraryRepository,
                searchHistoryRepository = RemoteSearchHistoryRepository(client),
                userRepository = userRepository,
                libraryExportService = JsonLibraryExportService(
                    videoRepository = libraryRepository,
                    folderRepository = libraryRepository,
                    userRepository = userRepository,
                    cacheDirectory = context.cacheDir
                ),
                videoImportPipeline = RemoteVideoImportPipeline(client),
                session = session
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
