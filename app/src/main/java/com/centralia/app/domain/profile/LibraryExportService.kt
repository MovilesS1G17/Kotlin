package com.centralia.app.domain.profile

import java.io.File

/**
 * `protocol LibraryExportService`. The Swift version hands back a temporary file
 * `URL` for `UIActivityViewController`; the Android version returns a [File] in
 * the app cache that is shared through a `FileProvider` content URI.
 */
interface LibraryExportService {
    suspend fun exportLibrary(profile: UserProfile): File
}

/** `LibraryExportError`. */
sealed class LibraryExportException(message: String) : Exception(message) {
    class UnableToEncode(cause: Throwable) : LibraryExportException(
        "Centralia could not prepare your export: ${cause.localizedMessage ?: cause}"
    )

    class UnableToWrite(cause: Throwable) : LibraryExportException(
        "Centralia could not save your export: ${cause.localizedMessage ?: cause}"
    )
}
