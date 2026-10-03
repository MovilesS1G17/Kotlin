package com.centralia.app.domain.profile

import java.io.File


interface LibraryExportService {
    suspend fun exportLibrary(profile: UserProfile): File
}


sealed class LibraryExportException(message: String) : Exception(message) {
    class UnableToEncode(cause: Throwable) : LibraryExportException(
        "Centralia could not prepare your export: ${cause.localizedMessage ?: cause}"
    )

    class UnableToWrite(cause: Throwable) : LibraryExportException(
        "Centralia could not save your export: ${cause.localizedMessage ?: cause}"
    )
}
