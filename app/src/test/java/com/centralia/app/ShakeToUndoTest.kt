package com.centralia.app

import com.centralia.app.domain.library.VideoDeletion
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Shake to undo: the app shows exactly what the backend sends with a delete. */
class ShakeToUndoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes the backend's delete answer`() {
        val body = """
            {"videoID": "5f0c2a3e-1111-4222-8333-944455556666",
             "deletedAt": "2026-10-28T14:00:00Z",
             "undoExpiresAt": "2026-10-28T14:05:00Z",
             "undoMessage": "If you want to undo the delete, just shake the phone."}
        """.trimIndent()

        val deletion = json.decodeFromString(VideoDeletion.serializer(), body)

        assertEquals(UUID.fromString("5f0c2a3e-1111-4222-8333-944455556666"), deletion.videoID)
        assertEquals("If you want to undo the delete, just shake the phone.", deletion.undoMessage)
        assertEquals(Instant.parse("2026-10-28T14:05:00Z"), deletion.undoExpiresAt)
    }

    @Test
    fun `offline deletes use the same text and five minute window as the backend`() {
        val now = Instant.parse("2026-10-28T14:00:00Z")
        val deletion = VideoDeletion.local(UUID.randomUUID(), now)

        assertEquals(VideoDeletion.DEFAULT_UNDO_MESSAGE, deletion.undoMessage)
        assertEquals(now.plusSeconds(300), deletion.undoExpiresAt)
    }
}
