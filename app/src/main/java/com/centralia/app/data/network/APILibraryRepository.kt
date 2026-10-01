package com.centralia.app.data.network

import com.centralia.app.domain.library.ContentAnalysisStatus
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

class APILibraryRepository(private val api: CentraliaApi) : VideoItemRepository, FolderRepository {
    private val events = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun videos(): List<VideoItem> =
        JSONArray(api.request("/videos")).objects().map(::parseVideo)

    override suspend fun saveVideo(video: VideoItem) {
        api.request("/videos", "POST", JSONObject()
            .put("id", video.id.toString()).put("source_url", video.sourceURL)
            .putNullable("folder_id", video.folderID?.toString())
            .put("tags", JSONArray(video.tags)).putNullable("note", video.note)
            .put("assignment_source", if (video.folderID == null) "none" else "manual"))
        event("video_saved", video.id, JSONObject().put("platform", video.platform.rawValue)
            .put("tag_count", video.tags.size)
            .put("assignment_source", if (video.folderID == null) "none" else "manual"))
        event("organization_decision", video.id, JSONObject()
            .put("assignment_source", if (video.folderID == null) "none" else "manual"))
    }

    override suspend fun deleteVideo(id: UUID) { api.request("/videos/$id", "DELETE") }
    override suspend fun restoreVideo(video: VideoItem) { api.request("/videos/${video.id}/restore", "POST") }
    override suspend fun moveVideo(id: UUID, folderID: UUID?) {
        api.request("/videos/$id/folder", "PATCH", JSONObject().putNullable("folder_id", folderID?.toString()))
        event("organization_decision", id, JSONObject().put("assignment_source",
            if (folderID == null) "none" else "manual"))
    }
    override suspend fun updateNote(id: UUID, note: String?) {
        api.request("/videos/$id/note", "PATCH", JSONObject().putNullable("note", note))
    }
    override suspend fun updateTags(id: UUID, tags: List<String>) {
        api.request("/videos/$id/tags", "PATCH", JSONObject().put("tags", JSONArray(tags)))
    }
    override fun recordSourceOpened(id: UUID) { event("source_opened", id) }

    override suspend fun folders(): List<LibraryFolder> =
        JSONArray(api.request("/folders")).objects().map(::parseFolder)
    override suspend fun createFolder(name: String, symbolName: String): LibraryFolder =
        parseFolder(JSONObject(api.request("/folders", "POST", JSONObject()
            .put("name", name).put("symbol_name", symbolName))))
    override suspend fun renameFolder(id: UUID, name: String): LibraryFolder =
        parseFolder(JSONObject(api.request("/folders/$id", "PATCH", JSONObject().put("name", name))))
    override suspend fun deleteFolder(id: UUID) { api.request("/folders/$id", "DELETE") }

    private fun event(name: String, id: UUID, properties: JSONObject = JSONObject()) {
        events.launch {
            repeat(2) { attempt ->
                try {
                    api.request("/events", "POST", JSONObject()
                        .put("event_name", name).put("video_id", id.toString())
                        .put("client_platform", "android").put("properties", properties))
                    return@launch
                } catch (_: Exception) {
                    if (attempt == 0) delay(2_000)
                }
            }
        }
    }
}

internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
internal fun parseFolder(json: JSONObject): LibraryFolder = LibraryFolder(
    id = UUID.fromString(json.getString("id")), name = json.getString("name"),
    symbolName = json.getString("symbol_name"))
internal fun parseVideo(json: JSONObject): VideoItem = VideoItem(
    id = UUID.fromString(json.getString("id")), sourceURL = json.getString("source_url"),
    platform = VideoPlatform.entries.first { it.rawValue == json.getString("platform") },
    creator = json.optString("creator"), durationSeconds = json.optInt("duration_seconds"),
    sourceCaption = json.stringOrNull("source_caption"), transcript = json.stringOrNull("transcript"),
    extractedOnScreenText = json.stringOrNull("extracted_on_screen_text"),
    generatedSummary = json.stringOrNull("generated_summary"), customTitle = json.stringOrNull("custom_title"),
    folderID = json.stringOrNull("folder_id")?.let(UUID::fromString),
    tags = json.getJSONArray("tags").let { array -> (0 until array.length()).map(array::getString) },
    note = json.stringOrNull("note"), savedAt = parseInstant(json.getString("saved_at")),
    analysisStatus = ContentAnalysisStatus.entries.first { it.name.equals(json.getString("analysis_status"), true) }
)

private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }
    .getOrElse { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
