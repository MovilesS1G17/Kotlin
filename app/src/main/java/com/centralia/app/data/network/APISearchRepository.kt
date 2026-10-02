package com.centralia.app.data.network

import android.os.Build
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.search.SearchRepository
import com.centralia.app.domain.search.SearchResult
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** The device-model identifier the backend expects in `X-Device-Model`. */
fun currentDeviceModelIdentifier(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

class APISearchRepository(private val api: CentraliaApi) : SearchRepository {
    private val events = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun search(
        query: String?,
        platform: VideoPlatform?,
        creator: String?,
        folderID: UUID?,
        tag: String?,
        limit: Int?,
        offset: Int
    ): SearchResult {
        val params = buildMap {
            query?.takeIf { it.isNotEmpty() }?.let { put("query", it) }
            platform?.let { put("platform", it.rawValue) }
            creator?.takeIf { it.isNotEmpty() }?.let { put("creator", it) }
            folderID?.let { put("folder_id", it.toString()) }
            tag?.takeIf { it.isNotEmpty() }?.let { put("tag", it) }
            limit?.let { put("limit", it.toString()) }
            if (offset > 0) put("offset", offset.toString())
        }
        val headers = mapOf(
            "X-Device-Model" to currentDeviceModelIdentifier(),
            "X-Client-Platform" to "android"
        )
        val (body, responseHeaders) = api.requestWithHeaders("/videos", params, headers)
        val videos = JSONArray(body).objects().map(::parseVideo)
        val totalCount = responseHeaders["X-Total-Count"]?.toIntOrNull() ?: videos.size
        return SearchResult(videos, totalCount)
    }

    override fun reportSearchCompleted(durationMs: Int, resultCount: Int, filterCount: Int) {
        events.launch {
            repeat(2) { attempt ->
                try {
                    api.request(
                        "/events", "POST",
                        JSONObject()
                            .put("event_name", "search_completed")
                            .put("client_platform", "android")
                            .put(
                                "properties",
                                JSONObject()
                                    .put("duration_ms", durationMs)
                                    .put("result_count", resultCount)
                                    .put("device_model", currentDeviceModelIdentifier())
                                    .put("filter_count", filterCount)
                            )
                    )
                    return@launch
                } catch (_: Exception) {
                    if (attempt == 0) delay(2_000)
                }
            }
        }
    }
}
