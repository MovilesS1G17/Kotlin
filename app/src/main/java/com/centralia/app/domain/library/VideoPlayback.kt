package com.centralia.app.domain.library

import kotlinx.serialization.Serializable


@Serializable
data class VideoPlayback(
    val streamURL: String? = null,
    val streamReady: Boolean = false,
    val embedURL: String? = null,
    /** Context-aware playback: the backend turned on data saver (mobile data). */
    val dataSaver: Boolean = false,
    /** What to tell the user about it ("You're on mobile data, so…"). */
    val notice: String? = null
)


sealed interface PlayerSource {

    data class Native(val streamURL: String) : PlayerSource


    data class Web(val embedURL: String) : PlayerSource
}
