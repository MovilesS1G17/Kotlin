package com.centralia.app.domain.library

import com.centralia.app.domain.InstantSerializer
import com.centralia.app.domain.UuidSerializer
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `VideoPlatform`. `rawValue` is persisted, so the strings must not change. */
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

    /** The shorter label used on the library filter chips. */
    val filterName: String
        get() = when (this) {
            TIKTOK -> "TikTok"
            INSTAGRAM_REEL -> "Reels"
            YOUTUBE_SHORT -> "Shorts"
        }
}

/** `ContentAnalysisStatus`. */
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

/**
 * `VideoItem`. The Swift type stores `sourceURL` as a `URL`; it is kept as a
 * `String` here because every use is either display, equality, or handing the
 * value to an `Intent`, and `java.net.URI` would reject some shortener forms
 * that `URL(string:)` accepts.
 */
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
    val analysisStatus: ContentAnalysisStatus
) {
    /**
     * `displayTitle` — the first non-blank of custom title, generated summary,
     * or source caption, falling back to the creator.
     */
    val displayTitle: String
        get() = customTitle?.nonEmptyTrimmed()
            ?: generatedSummary?.nonEmptyTrimmed()
            ?: sourceCaption?.nonEmptyTrimmed()
            ?: "Short by $creator"

    /** `formattedDuration` — `String(format: "%d:%02d", minutes, seconds)`. */
    val formattedDuration: String
        get() = "%d:%02d".format(durationSeconds / 60, durationSeconds % 60)
}

/** Swift's `private extension String { var nonEmptyTrimmed: String? }`. */
internal fun String.nonEmptyTrimmed(): String? = trim().ifEmpty { null }
