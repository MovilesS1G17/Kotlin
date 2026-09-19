package com.centralia.app

import com.centralia.app.data.mock.MockAuthenticationRepository
import com.centralia.app.domain.imports.VideoImportException
import com.centralia.app.domain.library.VideoPlatform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The import pipeline's URL rules and the deterministic id derivation are the two
 * places where a translation slip would be invisible in the UI but wrong.
 *
 * [com.centralia.app.data.mock.MockVideoImportPipeline] parses with `android.net.Uri`, which needs
 * Robolectric or an instrumented device, so platform detection is covered by the
 * instrumented suite instead; these are the parts that run on the JVM.
 */
class MockPipelineTest {

    @Test
    fun `deterministic ids are stable and RFC 4122 shaped`() = runBlocking {
        val repository = MockAuthenticationRepository(delayMillis = 0)

        val first = repository.logIn("you@example.com", "password")
        val second = repository.logIn("you@example.com", "different-password")

        // Same email always yields the same account id.
        assertEquals(first.id, second.id)

        // Version 4 and the RFC 4122 variant bits are forced.
        assertEquals(4, first.id.version())
        assertEquals(2, first.id.variant())
    }

    @Test
    fun `display names are derived from the email local part`() = runBlocking {
        val repository = MockAuthenticationRepository(delayMillis = 0)

        assertEquals("David Caro", repository.logIn("david.caro@example.com", "p").displayName)
        assertEquals("David Caro", repository.logIn("david_caro@example.com", "p").displayName)
        assertEquals("Roomreset", repository.logIn("ROOMRESET@example.com", "p").displayName)
    }

    @Test
    fun `the demo accounts still trigger their failure paths`() {
        val repository = MockAuthenticationRepository(delayMillis = 0)

        assertThrows(Exception::class.java) {
            runBlocking { repository.logIn("fail@example.com", "p") }
        }
        assertThrows(Exception::class.java) {
            runBlocking { repository.createAccount("existing@example.com", "password") }
        }
        assertThrows(Exception::class.java) {
            runBlocking { repository.requestPasswordReset("reset-fail@example.com") }
        }
    }

    @Test
    fun `import errors carry the user-facing copy`() {
        assertTrue(
            VideoImportException.InvalidURL.message
                .orEmpty()
                .startsWith("Enter a complete link")
        )
        assertTrue(
            VideoImportException.UnsupportedYouTubeVideo.message
                .orEmpty()
                .contains("YouTube Shorts")
        )
        assertTrue(
            VideoImportException.UnsupportedInstagramPost.message
                .orEmpty()
                .contains("Instagram Reel")
        )
    }

    @Test
    fun `every platform has a distinct display and filter name`() {
        val displayNames = VideoPlatform.entries.map { it.displayName }
        val filterNames = VideoPlatform.entries.map { it.filterName }
        assertEquals(displayNames.size, displayNames.distinct().size)
        assertEquals(filterNames.size, filterNames.distinct().size)
    }
}
