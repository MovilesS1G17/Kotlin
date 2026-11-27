package com.centralia.app.data.insights

import com.centralia.app.domain.insights.InsightsSnapshot
import com.centralia.app.domain.insights.InsightsSnapshotStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Local storage (Sprint 4, Gustavo's feature): the last Insights snapshot as a
 * JSON file in `filesDir/insights/`. One document read and written whole, so
 * a file is the simplest fit. Disk work runs on Dispatchers.IO; the write goes
 * to a temporary file first and is renamed.
 */
class FileInsightsSnapshotStore(private val directory: File) : InsightsSnapshotStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val file get() = File(directory, "last_snapshot.json")

    override suspend fun load(): InsightsSnapshot? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString(InsightsSnapshot.serializer(), file.readText()) }.getOrNull()
    }

    override suspend fun save(snapshot: InsightsSnapshot) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val temporary = File(directory, "last_snapshot.json.tmp")
        temporary.writeText(json.encodeToString(InsightsSnapshot.serializer(), snapshot))
        if (!temporary.renameTo(file)) {
            file.delete()
            temporary.renameTo(file)
        }
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        Unit
    }
}
