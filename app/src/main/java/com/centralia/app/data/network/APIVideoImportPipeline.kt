package com.centralia.app.data.network

import android.net.Uri
import com.centralia.app.domain.imports.ImportedVideoMetadata
import com.centralia.app.domain.imports.VideoImportException
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.VideoPlatform
import org.json.JSONObject

class APIVideoImportPipeline(private val api: CentraliaApi) : VideoImportPipeline {
    override suspend fun detectPlatform(sourceURL: String): VideoPlatform {
        val uri = Uri.parse(sourceURL)
        if (uri.scheme != "https" || uri.host.isNullOrEmpty()) throw VideoImportException.InvalidURL
        val host = uri.host!!.lowercase()
        val path = uri.path.orEmpty().lowercase()
        return when {
            host in setOf("tiktok.com", "www.tiktok.com", "m.tiktok.com", "vm.tiktok.com") &&
                (path.contains("/video/") || host == "vm.tiktok.com") -> VideoPlatform.TIKTOK
            host in setOf("instagram.com", "www.instagram.com") &&
                (path.contains("/reel/") || path.contains("/reels/")) -> VideoPlatform.INSTAGRAM_REEL
            host in setOf("youtube.com", "www.youtube.com", "m.youtube.com") &&
                path.contains("/shorts/") -> VideoPlatform.YOUTUBE_SHORT
            else -> throw VideoImportException.UnsupportedSource
        }
    }
    override suspend fun extractMetadata(sourceURL: String, platform: VideoPlatform): ImportedVideoMetadata {
        val json = JSONObject(api.request("/videos/preview", "POST", JSONObject().put("source_url", sourceURL)))
        return ImportedVideoMetadata(sourceURL = json.getString("source_url"),
            platform = VideoPlatform.entries.first { it.rawValue == json.getString("platform") },
            creator = json.optString("creator"), durationSeconds = json.optInt("duration_seconds"),
            sourceCaption = json.stringOrNull("source_caption"), transcript = json.stringOrNull("transcript"),
            extractedOnScreenText = json.stringOrNull("extracted_on_screen_text"),
            generatedSummary = json.stringOrNull("generated_summary"))
    }
    override suspend fun generateTags(metadata: ImportedVideoMetadata): List<String> = emptyList()
    override suspend fun suggestFolder(metadata: ImportedVideoMetadata, tags: List<String>): String? = null
}
