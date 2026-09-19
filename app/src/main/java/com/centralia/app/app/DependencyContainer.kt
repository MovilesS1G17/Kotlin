package com.centralia.app.app

import android.content.Context
import com.centralia.app.data.export.JsonLibraryExportService
import com.centralia.app.data.mock.MockAuthenticationRepository
import com.centralia.app.data.mock.MockDataStore
import com.centralia.app.data.mock.MockLibraryRepository
import com.centralia.app.data.mock.MockSearchHistoryRepository
import com.centralia.app.data.mock.MockUserRepository
import com.centralia.app.data.mock.MockVideoImportPipeline
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.profile.LibraryExportService
import com.centralia.app.domain.profile.UserRepository
import com.centralia.app.domain.search.SearchHistoryRepository

/**
 * `final class DependencyContainer`. Constructor injection is unchanged; only the
 * `mock()` factory has to differ, because the Android stores need a directory
 * from the [Context] where the Swift ones could reach Application Support on
 * their own.
 */
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
        /** `DependencyContainer.mock()`. */
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
