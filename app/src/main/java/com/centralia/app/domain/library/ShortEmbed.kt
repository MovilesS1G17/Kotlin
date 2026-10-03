package com.centralia.app.domain.library

import java.net.URI


object ShortEmbed {
    private val youTubeID = Regex("/shorts/([A-Za-z0-9_-]{6,})")
    private val tikTokID = Regex("/(?:video|v)/(\\d{8,})")
    private val instagramCode = Regex("/(?:reels?|p)/([A-Za-z0-9_-]{5,})")


    fun mediaID(sourceURL: String, platform: VideoPlatform): String? {
        val path = runCatching { URI(sourceURL.trim()).path }.getOrNull() ?: return null
        val match = when (platform) {
            VideoPlatform.YOUTUBE_SHORT -> youTubeID.find(path)
            VideoPlatform.TIKTOK -> tikTokID.find(path)
            VideoPlatform.INSTAGRAM_REEL ->
                if ("/share/" in path) null else instagramCode.find(path)
        }
        return match?.groupValues?.get(1)
    }

    fun embedURL(sourceURL: String, platform: VideoPlatform): String? {
        val id = mediaID(sourceURL, platform) ?: return null
        return when (platform) {
            VideoPlatform.YOUTUBE_SHORT ->
                "https://www.youtube.com/embed/$id?autoplay=1&playsinline=1&rel=0" +
                    "&modestbranding=1&loop=1&playlist=$id&fs=0"
            VideoPlatform.TIKTOK ->
                "https://www.tiktok.com/player/v1/$id?autoplay=1&loop=1&rel=0" +
                    "&music_info=1&description=1&fullscreen_button=0"
            VideoPlatform.INSTAGRAM_REEL -> "https://www.instagram.com/reel/$id/embed/"
        }
    }

    fun fallbackThumbnailURL(sourceURL: String, platform: VideoPlatform): String? {
        if (platform != VideoPlatform.YOUTUBE_SHORT) return null
        val id = mediaID(sourceURL, platform) ?: return null
        return "https://i.ytimg.com/vi/$id/hqdefault.jpg"
    }
}
