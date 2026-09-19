package com.centralia.app.data.export

import com.centralia.app.domain.InstantSerializer
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.profile.LibraryExportException
import com.centralia.app.domain.profile.LibraryExportService
import com.centralia.app.domain.profile.NotificationPreferences
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.domain.profile.UserRepository
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `actor JSONLibraryExportService`. The three loads run concurrently, mirroring
 * the Swift `async let` fan-out, and the document is written into a cache
 * subdirectory that [com.centralia.app.R.xml.file_paths] exposes through the
 * `FileProvider` so it can be shared out.
 */
class JsonLibraryExportService(
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository,
    private val userRepository: UserRepository,
    private val cacheDirectory: File
) : LibraryExportService {

    @Serializable
    private data class ExportDocument(
        val schemaVersion: Int,
        @Serializable(with = InstantSerializer::class)
        val exportedAt: Instant,
        val profile: UserProfile,
        val notificationPreferences: NotificationPreferences,
        val folders: List<LibraryFolder>,
        val videos: List<VideoItem>
    )

    override suspend fun exportLibrary(profile: UserProfile): File = coroutineScope {
        val loadedVideos = async { videoRepository.videos() }
        val loadedFolders = async { folderRepository.folders() }
        val loadedPreferences = async { userRepository.notificationPreferences(profile.id) }

        val document = ExportDocument(
            schemaVersion = 1,
            exportedAt = Instant.now(),
            profile = profile,
            notificationPreferences = loadedPreferences.await(),
            folders = loadedFolders.await(),
            videos = loadedVideos.await()
        )

        val text = try {
            EXPORT_JSON.encodeToString(ExportDocument.serializer(), document)
        } catch (error: Exception) {
            throw LibraryExportException.UnableToEncode(error)
        }

        withContext(Dispatchers.IO) {
            try {
                val exportDirectory = File(cacheDirectory, EXPORT_DIRECTORY_NAME)
                exportDirectory.mkdirs()
                val file = File(exportDirectory, "Centralia-Library-${UUID.randomUUID()}.json")
                file.writeText(text)
                file
            } catch (error: Exception) {
                throw LibraryExportException.UnableToWrite(error)
            }
        }
    }

    companion object {
        /** Must match the `cache-path` in `res/xml/file_paths.xml`. */
        const val EXPORT_DIRECTORY_NAME = "Centralia-Exports"

        private val EXPORT_JSON = Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
