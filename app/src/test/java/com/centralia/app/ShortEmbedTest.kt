package com.centralia.app

import com.centralia.app.domain.library.ShortEmbed
import com.centralia.app.domain.library.VideoPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class ShortEmbedTest {
    @Test
    fun youtubeShortPlaysInTheEmbedPlayer() {
        val url = ShortEmbed.embedURL("https://youtube.com/shorts/aqz-KE-bpKQ?si=abc", VideoPlatform.YOUTUBE_SHORT)
        assertEquals(
            "https://www.youtube.com/embed/aqz-KE-bpKQ?autoplay=1&playsinline=1&rel=0" +
                "&modestbranding=1&loop=1&playlist=aqz-KE-bpKQ&fs=0",
            url
        )
        assertEquals(
            "https://i.ytimg.com/vi/aqz-KE-bpKQ/hqdefault.jpg",
            ShortEmbed.fallbackThumbnailURL("https://www.youtube.com/shorts/aqz-KE-bpKQ", VideoPlatform.YOUTUBE_SHORT)
        )
    }

    @Test
    fun tiktokUsesTheOfficialPlayer() {
        assertEquals(
            "https://www.tiktok.com/player/v1/7311987654321098765?autoplay=1&loop=1&rel=0" +
                "&music_info=1&description=1&fullscreen_button=0",
            ShortEmbed.embedURL("https://www.tiktok.com/@a/video/7311987654321098765?_r=1", VideoPlatform.TIKTOK)
        )
        // Share links need the backend to follow the redirect.
        assertNull(ShortEmbed.embedURL("https://vm.tiktok.com/ZMabc/", VideoPlatform.TIKTOK))
    }

    @Test
    fun instagramReelUsesTheEmbedPage() {
        assertEquals(
            "https://www.instagram.com/reel/C5n2x1Rr3Qe/embed/",
            ShortEmbed.embedURL("https://www.instagram.com/reel/C5n2x1Rr3Qe/?igsh=x", VideoPlatform.INSTAGRAM_REEL)
        )
        assertNull(ShortEmbed.embedURL("https://www.instagram.com/share/reel/BAabc/", VideoPlatform.INSTAGRAM_REEL))
        assertNull(ShortEmbed.fallbackThumbnailURL("https://www.instagram.com/reel/C5n2x1Rr3Qe/", VideoPlatform.INSTAGRAM_REEL))
    }
}
