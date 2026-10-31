package com.centralia.app

import com.centralia.app.data.local.PendingAction
import com.centralia.app.domain.library.UndoMethod
import com.centralia.app.ui.components.offlineBannerText
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 3: the outbox rules and the offline banner, without Android. */
class OfflineFirstTest {

    private val video = UUID.fromString("11111111-1111-4111-8111-111111111111")
    private val at = Instant.parse("2026-10-29T15:00:00Z")

    @Test
    fun `undoing a delete that never reached the server cancels both`() {
        val queued = PendingAction.enqueue(emptyList(), PendingAction.Delete(video, at))
        val afterUndo = PendingAction.enqueue(queued, PendingAction.Restore(video, at, UndoMethod.SHAKE))
        assertTrue(afterUndo.isEmpty())
    }

    @Test
    fun `a newer edit of the same kind replaces the older one`() {
        var queue = PendingAction.enqueue(emptyList(), PendingAction.Note(video, at, "first"))
        queue = PendingAction.enqueue(queue, PendingAction.Tags(video, at, listOf("pasta")))
        queue = PendingAction.enqueue(queue, PendingAction.Note(video, at.plusSeconds(5), "second"))
        assertEquals(
            listOf(PendingAction.Tags(video, at, listOf("pasta")), PendingAction.Note(video, at.plusSeconds(5), "second")),
            queue
        )
    }

    @Test
    fun `the outbox survives a round trip through its JSON file`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val actions = listOf(
            PendingAction.Delete(video, at),
            PendingAction.Move(video, at, null),
            PendingAction.Restore(video, at, UndoMethod.SHAKE)
        )
        val serializer = ListSerializer(PendingAction.serializer())
        assertEquals(actions, json.decodeFromString(serializer, json.encodeToString(serializer, actions)))
    }

    @Test
    fun `banner tells the user what is happening`() {
        assertEquals("You're offline. Showing your saved library.", offlineBannerText(false, 0))
        assertEquals(
            "You're offline. Showing your saved library · 1 change will sync when you're back online.",
            offlineBannerText(false, 1)
        )
        assertEquals("Back online · syncing 3 changes…", offlineBannerText(true, 3))
    }
}
