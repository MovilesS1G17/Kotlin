package com.centralia.app.domain.library

import com.centralia.app.domain.InstantSerializer
import com.centralia.app.domain.UuidSerializer
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class VideoPlatform {
    @SerialName("tiktok")
    TIKTOK,

    @SerialName("instagramReel")
    INSTAGRAM_REEL,

    @SerialName("youtubeShort")
    YOUTUBE_SHORT;

    val rawValue: String
        get() = when (this) {
            TIKTOK -> "tiktok"
            INSTAGRAM_REEL -> "instagramReel"
            YOUTUBE_SHORT -> "youtubeShort"
        }

    val displayName: String
        get() = when (this) {
            TIKTOK -> "TikTok"
            INSTAGRAM_REEL -> "Instagram Reel"
            YOUTUBE_SHORT -> "YouTube Short"
        }


    val filterName: String
        get() = when (this) {
            TIKTOK -> "TikTok"
            INSTAGRAM_REEL -> "Reels"
            YOUTUBE_SHORT -> "Shorts"
        }
}


@Serializable
enum class ContentAnalysisStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("completed")
    COMPLETED,

    @SerialName("unavailable")
    UNAVAILABLE,

    @SerialName("failed")
    FAILED
}


@Serializable
data class VideoItem(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val sourceURL: String,
    val platform: VideoPlatform,
    val creator: String,
    val durationSeconds: Int,
    val sourceCaption: String? = null,
    val transcript: String? = null,
    val extractedOnScreenText: String? = null,
    val generatedSummary: String? = null,
    val customTitle: String? = null,
    @Serializable(with = UuidSerializer::class)
    val folderID: UUID? = null,
    val tags: List<String> = emptyList(),
    val note: String? = null,
    @Serializable(with = InstantSerializer::class)
    val savedAt: Instant,
    val analysisStatus: ContentAnalysisStatus,

    val thumbnailURL: String? = null,

    val embedURL: String? = null
) {

    val playerURL: String?
        get() = embedURL?.nonEmptyTrimmed() ?: ShortEmbed.embedURL(sourceURL, platform)


    val coverURL: String?
        get() = thumbnailURL?.nonEmptyTrimmed() ?: ShortEmbed.fallbackThumbnailURL(sourceURL, platform)


    val displayTitle: String
        get() = customTitle?.nonEmptyTrimmed()
            ?: generatedSummary?.nonEmptyTrimmed()
            ?: sourceCaption?.nonEmptyTrimmed()
            ?: "Short by $creator"


    val formattedDuration: String
        get() = "%d:%02d".format(durationSeconds / 60, durationSeconds % 60)


    val durationLabel: String?
        get() = if (durationSeconds > 0) formattedDuration else null
}


internal fun String.nonEmptyTrimmed(): String? = trim().ifEmpty { null }
