package com.centralia.app.data.mock

import com.centralia.app.domain.library.ContentAnalysisStatus
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.FolderRepositoryException
import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoItemRepositoryException
import com.centralia.app.domain.library.VideoPlatform
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * `actor MockLibraryRepository: VideoItemRepository, FolderRepository`.
 *
 * Every mutation goes through [MockDataStore.mutate] so the load-edit-save
 * sequence stays atomic, which is what the Swift actor guaranteed implicitly.
 */
class MockLibraryRepository(
    private val store: MockDataStore,
    private val filename: String = "library-v1.json"
) : VideoItemRepository, FolderRepository {

    @Serializable
    internal data class Snapshot(
        val schemaVersion: Int,
        val videos: List<VideoItem>,
        val folders: List<LibraryFolder>
    )

    override suspend fun videos(): List<VideoItem> =
        snapshot().videos.sortedByDescending { it.savedAt }

    override suspend fun folders(): List<LibraryFolder> =
        snapshot().folders.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    override suspend fun saveVideo(video: VideoItem) {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.videos.any { it.sourceURL == video.sourceURL }) {
                throw VideoItemRepositoryException.DuplicateVideo
            }
            current.copy(videos = current.videos + video)
        }
    }

    override suspend fun createFolder(name: String, symbolName: String): LibraryFolder {
        var created: LibraryFolder? = null

        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            val trimmedName = validatedFolderName(name, excludedID = null, folders = current.folders)
            val folder = LibraryFolder(
                id = UUID.randomUUID(),
                name = trimmedName,
                symbolName = FolderSymbol.fromRawValue(symbolName).rawValue
            )
            created = folder
            current.copy(folders = current.folders + folder)
        }

        return requireNotNull(created)
    }

    override suspend fun renameFolder(id: UUID, name: String): LibraryFolder {
        var renamed: LibraryFolder? = null

        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.folders.none { it.id == id }) {
                throw FolderRepositoryException.FolderNotFound
            }
            val trimmedName = validatedFolderName(name, excludedID = id, folders = current.folders)
            val updatedFolders = current.folders.map { folder ->
                if (folder.id == id) folder.copy(name = trimmedName).also { renamed = it } else folder
            }
            current.copy(folders = updatedFolders)
        }

        return requireNotNull(renamed)
    }

    override suspend fun deleteFolder(id: UUID) {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.folders.none { it.id == id }) {
                throw FolderRepositoryException.FolderNotFound
            }
            current.copy(
                folders = current.folders.filterNot { it.id == id },
                // Shorts in a deleted folder fall back to Unorganized.
                videos = current.videos.map { video ->
                    if (video.folderID == id) video.copy(folderID = null) else video
                }
            )
        }
    }

    override suspend fun deleteVideo(id: UUID) {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            current.copy(videos = current.videos.filterNot { it.id == id })
        }
    }

    override suspend fun restoreVideo(video: VideoItem) {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            current.copy(videos = current.videos.filterNot { it.id == video.id } + video)
        }
    }

    override suspend fun moveVideo(id: UUID, folderID: UUID?) {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.videos.none { it.id == id }) return@mutate current
            current.copy(
                videos = current.videos.map { video ->
                    if (video.id == id) video.copy(folderID = folderID) else video
                }
            )
        }
    }

    override suspend fun updateNote(id: UUID, note: String?) {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.videos.none { it.id == id }) return@mutate current
            val trimmedNote = note?.trim()?.ifEmpty { null }
            current.copy(
                videos = current.videos.map { video ->
                    if (video.id == id) video.copy(note = trimmedNote) else video
                }
            )
        }
    }

    override suspend fun updateTags(id: UUID, tags: List<String>) {
        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.videos.none { it.id == id }) return@mutate current
            current.copy(
                videos = current.videos.map { video ->
                    if (video.id == id) video.copy(tags = normalizedTags(tags)) else video
                }
            )
        }
    }

    private suspend fun snapshot(): Snapshot =
        store.load(Snapshot.serializer(), filename, { seed() })

    /** `validatedFolderName(_:excluding:from:)`. */
    private fun validatedFolderName(
        name: String,
        excludedID: UUID?,
        folders: List<LibraryFolder>
    ): String {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) throw FolderRepositoryException.EmptyName

        val isDuplicate = folders.any { folder ->
            folder.id != excludedID && folder.name.equals(trimmedName, ignoreCase = true)
        }
        if (isDuplicate) throw FolderRepositoryException.DuplicateName

        return trimmedName
    }

    /** Trims, drops blanks, and removes case-insensitive duplicates, order kept. */
    private fun normalizedTags(tags: List<String>): List<String> {
        val normalized = mutableListOf<String>()
        tags.forEach { value ->
            val tag = value.trim()
            if (tag.isEmpty()) return@forEach
            if (normalized.any { it.equals(tag, ignoreCase = true) }) return@forEach
            normalized += tag
        }
        return normalized
    }

    private companion object {
        // Fixed UUIDs, matching the Swift seed so an existing library-v1.json
        // written by either platform keeps its folder links intact.
        val SPACES_ID: UUID = UUID.fromString("20000000-0000-4000-8000-000000000001")
        val DESIGN_ID: UUID = UUID.fromString("20000000-0000-4000-8000-000000000002")
        val RECIPES_ID: UUID = UUID.fromString("20000000-0000-4000-8000-000000000003")
        val WELLBEING_ID: UUID = UUID.fromString("20000000-0000-4000-8000-000000000004")

        /** `Date(timeIntervalSince1970: 1_789_166_400)`. */
        val BASE_DATE: Instant = Instant.ofEpochSecond(1_789_166_400L)

        fun seed(): Snapshot {
            val spaces = LibraryFolder(SPACES_ID, "Spaces", "house")
            val design = LibraryFolder(DESIGN_ID, "Design", "paintpalette")
            val recipes = LibraryFolder(RECIPES_ID, "Recipes", "fork.knife")
            val wellbeing = LibraryFolder(WELLBEING_ID, "Wellbeing", "sun.max")

            val videos = listOf(
                VideoItem(
                    id = UUID.fromString("10000000-0000-4000-8000-000000000001"),
                    sourceURL = "https://www.tiktok.com/@roomreset/video/1",
                    platform = VideoPlatform.TIKTOK,
                    creator = "@roomreset",
                    durationSeconds = 28,
                    sourceCaption = "Wait for the final result.",
                    transcript = "Use vertical storage and keep the desk close to natural light in a small studio.",
                    extractedOnScreenText = "small studio reset",
                    generatedSummary = "Tiny studio ideas",
                    customTitle = null,
                    folderID = spaces.id,
                    tags = listOf("studio", "organization", "interiors"),
                    note = null,
                    savedAt = BASE_DATE,
                    analysisStatus = ContentAnalysisStatus.COMPLETED
                ),
                VideoItem(
                    id = UUID.fromString("10000000-0000-4000-8000-000000000002"),
                    sourceURL = "https://www.instagram.com/reel/centralia-colour",
                    platform = VideoPlatform.INSTAGRAM_REEL,
                    creator = "@studiomarta",
                    durationSeconds = 41,
                    sourceCaption = "The third one changed everything.",
                    transcript = "Choose one dominant color, one supporting color, and one small accent color.",
                    extractedOnScreenText = "three colour rule",
                    generatedSummary = "Three color rules",
                    customTitle = null,
                    folderID = design.id,
                    tags = listOf("color", "design", "visual identity"),
                    note = null,
                    savedAt = BASE_DATE.minusSeconds(3_600),
                    analysisStatus = ContentAnalysisStatus.COMPLETED
                ),
                VideoItem(
                    id = UUID.fromString("10000000-0000-4000-8000-000000000003"),
                    sourceURL = "https://www.youtube.com/shorts/centralia-pasta",
                    platform = VideoPlatform.YOUTUBE_SHORT,
                    creator = "@quickplate",
                    durationSeconds = 54,
                    sourceCaption = "Dinner sorted.",
                    transcript = "Boil the pasta while the tomatoes, garlic, and olive oil cook in the same pan.",
                    extractedOnScreenText = "20 minute pasta",
                    generatedSummary = "Pasta in 20 minutes",
                    customTitle = null,
                    folderID = recipes.id,
                    tags = listOf("pasta", "dinner", "quick recipes"),
                    note = "Try this on Thursday.",
                    savedAt = BASE_DATE.minusSeconds(7_200),
                    analysisStatus = ContentAnalysisStatus.COMPLETED
                ),
                VideoItem(
                    id = UUID.fromString("10000000-0000-4000-8000-000000000004"),
                    sourceURL = "https://www.tiktok.com/@slowmornings/video/4",
                    platform = VideoPlatform.TIKTOK,
                    creator = "@slowmornings",
                    durationSeconds = 22,
                    sourceCaption = "Needed this today.",
                    transcript = null,
                    extractedOnScreenText = "a slower morning routine",
                    generatedSummary = "A calmer morning",
                    customTitle = null,
                    folderID = wellbeing.id,
                    tags = listOf("morning", "routine", "wellbeing"),
                    note = null,
                    savedAt = BASE_DATE.minusSeconds(10_800),
                    analysisStatus = ContentAnalysisStatus.COMPLETED
                ),
                VideoItem(
                    id = UUID.fromString("10000000-0000-4000-8000-000000000005"),
                    sourceURL = "https://www.instagram.com/reel/centralia-ceramics",
                    platform = VideoPlatform.INSTAGRAM_REEL,
                    creator = "@softforms",
                    durationSeconds = 36,
                    sourceCaption = "One more for the shelf.",
                    transcript = "Center the clay before opening the form and keep both elbows anchored.",
                    extractedOnScreenText = "wheel throwing basics",
                    generatedSummary = "Centering clay on the wheel",
                    customTitle = null,
                    folderID = design.id,
                    tags = listOf("ceramics", "craft", "tutorial"),
                    note = null,
                    savedAt = BASE_DATE.minusSeconds(14_400),
                    analysisStatus = ContentAnalysisStatus.COMPLETED
                ),
                VideoItem(
                    id = UUID.fromString("10000000-0000-4000-8000-000000000006"),
                    sourceURL = "https://www.youtube.com/shorts/centralia-swiftui",
                    platform = VideoPlatform.YOUTUBE_SHORT,
                    creator = "@swiftminute",
                    durationSeconds = 48,
                    sourceCaption = "This transition feels so much better.",
                    transcript = "Use a matched geometry effect to preserve spatial continuity between both SwiftUI states.",
                    extractedOnScreenText = "matchedGeometryEffect",
                    generatedSummary = "Smoother SwiftUI transitions",
                    customTitle = null,
                    folderID = null,
                    tags = listOf("SwiftUI", "animation", "iOS"),
                    note = null,
                    savedAt = BASE_DATE.minusSeconds(18_000),
                    analysisStatus = ContentAnalysisStatus.COMPLETED
                )
            )

            return Snapshot(
                schemaVersion = 1,
                videos = videos,
                folders = listOf(spaces, design, recipes, wellbeing)
            )
        }
    }
}
