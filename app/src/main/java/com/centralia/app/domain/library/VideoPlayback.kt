package com.centralia.app.domain.library

import kotlinx.serialization.Serializable


@Serializable
data class VideoPlayback(
    val streamURL: String? = null,
    val streamReady: Boolean = false,
    val embedURL: String? = null
)


sealed interface PlayerSource {

    data class Native(val streamURL: String) : PlayerSource


    data class Web(val embedURL: String) : PlayerSource
}
