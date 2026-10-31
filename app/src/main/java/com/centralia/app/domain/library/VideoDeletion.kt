package com.centralia.app.domain.library

import com.centralia.app.domain.InstantSerializer
import com.centralia.app.domain.UuidSerializer
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Shake to undo: the answer to `DELETE /v1/videos/{id}`. The backend decides
 * what the undo pop-up says ([undoMessage]) and until when the short can be
 * restored ([undoExpiresAt]); the app only shows it. Mirrors
 * `VideoDeletion` in the iOS app.
 */
@Serializable
data class VideoDeletion(
    @Serializable(with = UuidSerializer::class)
    val videoID: UUID,
    val undoMessage: String = DEFAULT_UNDO_MESSAGE,
    @Serializable(with = InstantSerializer::class)
    val undoExpiresAt: Instant? = null
) {
    companion object {
        /** Same text the backend sends; used offline (mock repository). */
        const val DEFAULT_UNDO_MESSAGE = "If you want to undo the delete, just shake the phone."

        /** Same window as the backend's UNDO_WINDOW_SECONDS default. */
        const val UNDO_WINDOW_SECONDS = 300L

        fun local(videoID: UUID, now: Instant = Instant.now()) = VideoDeletion(
            videoID = videoID,
            undoMessage = DEFAULT_UNDO_MESSAGE,
            undoExpiresAt = now.plusSeconds(UNDO_WINDOW_SECONDS)
        )
    }
}

/** A short that was just deleted, with the undo pop-up the backend asked for. */
data class DeletedVideo(
    val video: VideoItem,
    val deletion: VideoDeletion
)
