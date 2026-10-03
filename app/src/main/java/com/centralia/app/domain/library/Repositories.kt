package com.centralia.app.domain.library

import java.util.UUID


interface VideoItemRepository {
    suspend fun videos(): List<VideoItem>

    suspend fun saveVideo(video: VideoItem)

    suspend fun deleteVideo(id: UUID)

    suspend fun restoreVideo(video: VideoItem)

    suspend fun moveVideo(id: UUID, folderID: UUID?)

    suspend fun updateNote(id: UUID, note: String?)

    suspend fun updateTags(id: UUID, tags: List<String>)


    suspend fun playback(id: UUID): VideoPlayback? = null
}


interface FolderRepository {
    suspend fun folders(): List<LibraryFolder>

    suspend fun createFolder(
        name: String,
        symbolName: String = FolderSymbol.FOLDER.rawValue
    ): LibraryFolder

    suspend fun renameFolder(id: UUID, name: String): LibraryFolder

    suspend fun deleteFolder(id: UUID)
}


sealed class VideoItemRepositoryException(message: String) : Exception(message) {
    data object DuplicateVideo : VideoItemRepositoryException(
        "This short is already in your Centralia library."
    )
}


sealed class FolderRepositoryException(message: String) : Exception(message) {
    data object EmptyName : FolderRepositoryException("Enter a folder name.")

    data object DuplicateName : FolderRepositoryException(
        "A folder with that name already exists."
    )

    data object FolderNotFound : FolderRepositoryException(
        "This folder is no longer available."
    )
}
