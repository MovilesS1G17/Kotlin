package com.centralia.app

import com.centralia.app.domain.context.NetworkType
import com.centralia.app.domain.library.VideoPlayback
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Context-aware playback: the phone reports its network; the backend decides. */
class ContextAwarePlaybackTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `network type follows the phone's connection`() {
        assertEquals(NetworkType.OFFLINE, NetworkType.from(connected = false, wifiOrEthernet = true, cellular = true))
        assertEquals(NetworkType.WIFI, NetworkType.from(connected = true, wifiOrEthernet = true, cellular = true))
        assertEquals(NetworkType.CELLULAR, NetworkType.from(connected = true, wifiOrEthernet = false, cellular = true))
        assertEquals(NetworkType.UNKNOWN, NetworkType.from(connected = true, wifiOrEthernet = false, cellular = false))
    }

    @Test
    fun `wire values match the backend's network query`() {
        assertEquals(
            listOf("wifi", "cellular", "offline", "unknown"),
            NetworkType.entries.map { it.wireValue }
        )
    }

    @Test
    fun `decodes the data saver answer`() {
        val playback = json.decodeFromString(
            VideoPlayback.serializer(),
            """{"streamURL":null,"streamReady":false,"embedURL":"https://www.youtube.com/embed/abc",
               "dataSaver":true,"notice":"You're on mobile data, so this short plays in data saver mode."}"""
        )
        assertTrue(playback.dataSaver)
        assertNull(playback.streamURL)
        assertEquals("You're on mobile data, so this short plays in data saver mode.", playback.notice)
    }

    @Test
    fun `older answers without the field keep the normal player`() {
        val playback = json.decodeFromString(
            VideoPlayback.serializer(),
            """{"streamURL":"/v1/streams/1","streamReady":true,"embedURL":null}"""
        )
        assertFalse(playback.dataSaver)
        assertNull(playback.notice)
    }
}
