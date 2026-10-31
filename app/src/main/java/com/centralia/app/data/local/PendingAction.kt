package com.centralia.app.data.local

import com.centralia.app.domain.InstantSerializer
import com.centralia.app.domain.UuidSerializer
import com.centralia.app.domain.library.UndoMethod
import com.centralia.app.domain.library.VideoItem
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Eventual connectivity (Sprint 3): a change the user made while offline,
 * waiting in the outbox to be sent to the backend when the phone is back
 * online. [queuedAt] travels with it as `X-Queued-At`, so the backend knows how
 * long it waited. Mirrors `PendingAction` in the iOS app.
 */
@Serializable
sealed class PendingAction {
    abstract val videoID: UUID
    abstract val queuedAt: Instant

    @Serializable
    @SerialName("delete")
    data class Delete(
        @Serializable(with = UuidSerializer::class) override val videoID: UUID,
        @Serializable(with = InstantSerializer::class) override val queuedAt: Instant
    ) : PendingAction()

    @Serializable
    @SerialName("restore")
    data class Restore(
        @Serializable(with = UuidSerializer::class) override val videoID: UUID,
        @Serializable(with = InstantSerializer::class) override val queuedAt: Instant,
        val method: UndoMethod = UndoMethod.BUTTON
    ) : PendingAction()

    @Serializable
    @SerialName("move")
    data class Move(
        @Serializable(with = UuidSerializer::class) override val videoID: UUID,
        @Serializable(with = InstantSerializer::class) override val queuedAt: Instant,
        @Serializable(with = UuidSerializer::class) val folderID: UUID?
    ) : PendingAction()

    @Serializable
    @SerialName("note")
    data class Note(
        @Serializable(with = UuidSerializer::class) override val videoID: UUID,
        @Serializable(with = InstantSerializer::class) override val queuedAt: Instant,
        val note: String?
    ) : PendingAction()

    @Serializable
    @SerialName("tags")
    data class Tags(
        @Serializable(with = UuidSerializer::class) override val videoID: UUID,
        @Serializable(with = InstantSerializer::class) override val queuedAt: Instant,
        val tags: List<String>
    ) : PendingAction()

    /** The same change applied to a local copy of the library. */
    fun applyTo(videos: List<VideoItem>): List<VideoItem> = when (this) {
        is Delete -> videos.filterNot { it.id == videoID }
        is Restore -> videos
        is Move -> videos.map { if (it.id == videoID) it.copy(folderID = folderID) else it }
        is Note -> videos.map { if (it.id == videoID) it.copy(note = note) else it }
        is Tags -> videos.map { if (it.id == videoID) it.copy(tags = tags) else it }
    }

    /** True when [other] makes this one pointless (a newer edit of the same kind). */
    fun isReplacedBy(other: PendingAction): Boolean =
        videoID == other.videoID && this::class == other::class && this !is Delete && this !is Restore

    companion object {
        /**
         * Pure outbox rule, shared by the queue and its tests: an undo of a delete
         * that never reached the server cancels both; a newer edit of the same
         * kind replaces the older one.
         */
        fun enqueue(current: List<PendingAction>, action: PendingAction): List<PendingAction> {
            if (action is Restore && current.any { it is Delete && it.videoID == action.videoID }) {
                return current.filterNot { it is Delete && it.videoID == action.videoID }
            }
            return current.filterNot { it.isReplacedBy(action) } + action
        }
    }
}
