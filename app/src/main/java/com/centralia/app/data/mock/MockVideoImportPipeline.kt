package com.centralia.app.data.mock

import android.net.Uri
import com.centralia.app.domain.imports.ImportedVideoMetadata
import com.centralia.app.domain.imports.VideoImportException
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.VideoPlatform
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * `struct MockVideoImportPipeline`. URL validation, the per-platform fixtures,
 * and the DJB2 hash that picks a stable fixture for a given link are all carried
 * over, so pasting the same link produces the same metadata as on iOS.
 */
class MockVideoImportPipeline(
    private val delayMillis: Long = 420
) : VideoImportPipeline {

    private data class Fixture(
        val platform: VideoPlatform,
        val creator: String,
        val durationSeconds: Int,
        val caption: String,
        val transcript: String?,
        val onScreenText: String?,
        val summary: String,
        val tags: List<String>,
        val suggestedFolder: String
    )

    override suspend fun detectPlatform(sourceURL: String): VideoPlatform {
        pause()

        val uri = runCatching { Uri.parse(sourceURL) }.getOrNull()
            ?: throw VideoImportException.InvalidURL

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT)
        if (scheme != "https" || host.isNullOrEmpty()) throw VideoImportException.InvalidURL

        val path = (uri.path ?: "").lowercase(Locale.ROOT)

        if (host == "tiktok.com" || host.endsWith(".tiktok.com")) {
            if (!path.contains("/video/") && host != "vm.tiktok.com") {
                throw VideoImportException.UnsupportedSource
            }
            return VideoPlatform.TIKTOK
        }

        if (host == "instagram.com" || host.endsWith(".instagram.com")) {
            if (!path.contains("/reel/") && !path.contains("/reels/")) {
                throw VideoImportException.UnsupportedInstagramPost
            }
            return VideoPlatform.INSTAGRAM_REEL
        }

        if (host == "youtube.com" || host.endsWith(".youtube.com")) {
            if (!path.contains("/shorts/")) {
                throw VideoImportException.UnsupportedYouTubeVideo
            }
            return VideoPlatform.YOUTUBE_SHORT
        }

        throw VideoImportException.UnsupportedSource
    }

    override suspend fun extractMetadata(
        sourceURL: String,
        platform: VideoPlatform
    ): ImportedVideoMetadata {
        pause()
        val fixture = fixture(sourceURL, platform)

        return ImportedVideoMetadata(
            sourceURL = sourceURL,
            platform = fixture.platform,
            creator = fixture.creator,
            durationSeconds = fixture.durationSeconds,
            sourceCaption = fixture.caption,
            transcript = fixture.transcript,
            extractedOnScreenText = fixture.onScreenText,
            generatedSummary = fixture.summary
        )
    }

    override suspend fun generateTags(metadata: ImportedVideoMetadata): List<String> {
        pause()
        return fixture(metadata.sourceURL, metadata.platform).tags
    }

    override suspend fun suggestFolder(
        metadata: ImportedVideoMetadata,
        tags: List<String>
    ): String? {
        pause()
        return fixture(metadata.sourceURL, metadata.platform).suggestedFolder
    }

    private suspend fun pause() {
        if (delayMillis > 0) delay(delayMillis)
    }

    private fun fixture(sourceURL: String, platform: VideoPlatform): Fixture {
        val matches = FIXTURES.filter { it.platform == platform }
        return matches[stableIndex(sourceURL, matches.size)]
    }

    /**
     * DJB2 over the UTF-8 bytes, modulo the fixture count. Swift's `&+` and `<<`
     * wrap on overflow; Kotlin's `Long` arithmetic wraps the same way, and the
     * unsigned modulo is taken with [ULong] to match `UInt64`.
     */
    private fun stableIndex(value: String, upperBound: Int): Int {
        if (upperBound <= 0) return 0
        var hash = 5_381L
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = (hash shl 5) + hash + (byte.toInt() and 0xFF).toLong()
        }
        return (hash.toULong() % upperBound.toULong()).toInt()
    }

    private companion object {
        val FIXTURES = listOf(
            Fixture(
                platform = VideoPlatform.TIKTOK,
                creator = "@roomreset",
                durationSeconds = 28,
                caption = "Wait for the final result.",
                transcript = "Use vertical storage and keep the desk close to natural light in a small studio.",
                onScreenText = "small studio reset",
                summary = "Tiny studio ideas",
                tags = listOf("studio", "organization", "interiors"),
                suggestedFolder = "Spaces"
            ),
            Fixture(
                platform = VideoPlatform.TIKTOK,
                creator = "@slowmornings",
                durationSeconds = 22,
                caption = "Needed this today.",
                transcript = null,
                onScreenText = "a slower morning routine",
                summary = "A calmer morning",
                tags = listOf("morning", "routine", "wellbeing"),
                suggestedFolder = "Wellbeing"
            ),
            Fixture(
                platform = VideoPlatform.TIKTOK,
                creator = "@typefoundry",
                durationSeconds = 34,
                caption = "Save this for your next layout.",
                transcript = "Start with contrast, then use spacing and scale to make the hierarchy obvious.",
                onScreenText = "visual hierarchy in three steps",
                summary = "Build clearer visual hierarchy",
                tags = listOf("design", "typography", "layout"),
                suggestedFolder = "Design"
            ),
            Fixture(
                platform = VideoPlatform.INSTAGRAM_REEL,
                creator = "@studiomarta",
                durationSeconds = 41,
                caption = "The third one changed everything.",
                transcript = "Choose one dominant color, one supporting color, and one small accent color.",
                onScreenText = "three colour rule",
                summary = "Three color rules",
                tags = listOf("color", "design", "visual identity"),
                suggestedFolder = "Design"
            ),
            Fixture(
                platform = VideoPlatform.INSTAGRAM_REEL,
                creator = "@softforms",
                durationSeconds = 36,
                caption = "One more for the shelf.",
                transcript = "Center the clay before opening the form and keep both elbows anchored.",
                onScreenText = "wheel throwing basics",
                summary = "Centering clay on the wheel",
                tags = listOf("ceramics", "craft", "tutorial"),
                suggestedFolder = "Design"
            ),
            Fixture(
                platform = VideoPlatform.INSTAGRAM_REEL,
                creator = "@tinyspaces",
                durationSeconds = 33,
                caption = "A corner that finally works.",
                transcript = "Use one floating shelf and a compact task light to separate the workspace.",
                onScreenText = "home workspace",
                summary = "A workspace for small rooms",
                tags = listOf("workspace", "interiors", "organization"),
                suggestedFolder = "Spaces"
            ),
            Fixture(
                platform = VideoPlatform.YOUTUBE_SHORT,
                creator = "@swiftminute",
                durationSeconds = 48,
                caption = "This transition feels so much better.",
                transcript = "Use a matched geometry effect to preserve spatial continuity between SwiftUI states.",
                onScreenText = "matchedGeometryEffect",
                summary = "Smoother SwiftUI transitions",
                tags = listOf("SwiftUI", "animation", "iOS"),
                suggestedFolder = "Design"
            ),
            Fixture(
                platform = VideoPlatform.YOUTUBE_SHORT,
                creator = "@quickplate",
                durationSeconds = 54,
                caption = "Dinner sorted.",
                transcript = "Boil the pasta while the tomatoes, garlic, and olive oil cook in the same pan.",
                onScreenText = "20 minute pasta",
                summary = "Pasta in 20 minutes",
                tags = listOf("pasta", "dinner", "quick recipes"),
                suggestedFolder = "Recipes"
            ),
            Fixture(
                platform = VideoPlatform.YOUTUBE_SHORT,
                creator = "@dailyreset",
                durationSeconds = 39,
                caption = "A simple way to end the day.",
                transcript = "Put tomorrow's three priorities on paper and move your phone away from the bed.",
                onScreenText = "five minute evening reset",
                summary = "A five-minute evening reset",
                tags = listOf("routine", "focus", "wellbeing"),
                suggestedFolder = "Wellbeing"
            )
        )
    }
}
