package com.centralia.app.domain.imports

import com.centralia.app.domain.library.VideoPlatform

/** `VideoImportStage`. `ordinal` stands in for the Swift `rawValue: Int`. */
enum class VideoImportStage {
    DETECTING_PLATFORM,
    EXTRACTING_METADATA,
    GENERATING_TAGS,
    SUGGESTING_FOLDER;

    val displayName: String
        get() = when (this) {
            DETECTING_PLATFORM -> "Detecting platform…"
            EXTRACTING_METADATA -> "Extracting metadata…"
            GENERATING_TAGS -> "Generating tag suggestions…"
            SUGGESTING_FOLDER -> "Suggesting a folder…"
        }
}

/** `ImportedVideoMetadata`. */
data class ImportedVideoMetadata(
    val sourceURL: String,
    val platform: VideoPlatform,
    val creator: String,
    val durationSeconds: Int,
    val sourceCaption: String?,
    val transcript: String?,
    val extractedOnScreenText: String?,
    val generatedSummary: String?
) {
    val formattedDuration: String
        get() = "%d:%02d".format(durationSeconds / 60, durationSeconds % 60)
}

/** `VideoImportError`. */
sealed class VideoImportException(message: String) : Exception(message) {
    data object InvalidURL : VideoImportException(
        "Enter a complete link beginning with https://."
    )

    data object UnsupportedSource : VideoImportException(
        "Centralia currently supports TikTok videos, Instagram Reels, and YouTube Shorts only."
    )

    data object UnsupportedYouTubeVideo : VideoImportException(
        "This looks like a regular YouTube video. Paste a YouTube Shorts link instead."
    )

    data object UnsupportedInstagramPost : VideoImportException(
        "This looks like an Instagram post. Paste an Instagram Reel link instead."
    )
}

/** `protocol VideoImportPipeline`. */
interface VideoImportPipeline {
    suspend fun detectPlatform(sourceURL: String): VideoPlatform

    suspend fun extractMetadata(sourceURL: String, platform: VideoPlatform): ImportedVideoMetadata

    suspend fun generateTags(metadata: ImportedVideoMetadata): List<String>

    suspend fun suggestFolder(metadata: ImportedVideoMetadata, tags: List<String>): String?
}
