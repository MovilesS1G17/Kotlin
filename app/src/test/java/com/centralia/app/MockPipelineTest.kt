package com.centralia.app

import com.centralia.app.data.mock.MockAuthenticationRepository
import com.centralia.app.domain.imports.VideoImportException
import com.centralia.app.domain.library.VideoPlatform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test


class MockPipelineTest {


    private suspend fun MockAuthenticationRepository.signUp(email: String, password: String = "shorts2026") =
        createAccount(email, password).let { challenge ->
            verifyEmail(challenge.email, lastSentCode.getValue(challenge.email))
        }

    @Test
    fun `deterministic ids are stable and RFC 4122 shaped`() = runBlocking {
        val repository = MockAuthenticationRepository(delayMillis = 0)

        val first = repository.signUp("you@example.com")
        val second = repository.logIn("you@example.com", "shorts2026")

        // Same email always yields the same account id.
        assertEquals(first.id, second.id)

        // Version 4 and the RFC 4122 variant bits are forced.
        assertEquals(4, first.id.version())
        assertEquals(2, first.id.variant())
    }

    @Test
    fun `display names are derived from the email local part`() = runBlocking {
        val repository = MockAuthenticationRepository(delayMillis = 0)

        assertEquals("David Caro", repository.signUp("david.caro@example.com").displayName)
        assertEquals("David Caro", repository.signUp("david_caro@example.com").displayName)
        assertEquals("Roomreset", repository.signUp("ROOMRESET@example.com").displayName)
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
