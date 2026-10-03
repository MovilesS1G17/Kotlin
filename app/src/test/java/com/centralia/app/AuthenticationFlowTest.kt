package com.centralia.app

import com.centralia.app.data.mock.MockAuthenticationRepository
import com.centralia.app.domain.auth.AuthenticationException
import com.centralia.app.feature.auth.AuthenticationValidation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test


class AuthenticationFlowTest {

    @Test
    fun `password rules match the backend`() {
        assertEquals(AuthenticationValidation.WEAK_PASSWORD, AuthenticationValidation.passwordError("short1"))
        assertEquals(AuthenticationValidation.WEAK_PASSWORD, AuthenticationValidation.passwordError("onlyletters"))
        assertEquals(AuthenticationValidation.WEAK_PASSWORD, AuthenticationValidation.passwordError("123456789"))
        assertEquals(AuthenticationValidation.COMMON_PASSWORD, AuthenticationValidation.passwordError("Password123"))
        assertNull(AuthenticationValidation.passwordError("shorts2026"))
    }

    @Test
    fun `codes keep digits only`() {
        assertEquals("482913", AuthenticationValidation.normalizedCode(" 482 - 913 "))
        assertEquals("123456", AuthenticationValidation.normalizedCode("1234567"))
        assertNull(AuthenticationValidation.codeError("482913"))
        assertTrue(AuthenticationValidation.codeError("4829") != null)
    }

    @Test
    fun `sign-up needs the emailed code before any session`() = runBlocking {
        val repository = MockAuthenticationRepository(delayMillis = 0)
        val challenge = repository.createAccount("ana@example.com", "shorts2026")
        assertEquals("ana@example.com", challenge.email)

        // Right password, unconfirmed email: the code screen, not a session.
        val unverified = runCatching { repository.logIn("ana@example.com", "shorts2026") }.exceptionOrNull()
        assertTrue(unverified is AuthenticationException.EmailNotVerified)

        assertThrows(AuthenticationException.InvalidCode::class.java) {
            runBlocking { repository.verifyEmail("ana@example.com", "000000x") }
        }
        val code = repository.lastSentCode.getValue("ana@example.com")
        assertEquals("ana@example.com", repository.verifyEmail("ana@example.com", code).email)
        assertEquals("ana@example.com", repository.logIn("ana@example.com", "shorts2026").email)
    }

    @Test
    fun `password reset with the emailed code signs in with the new password`() = runBlocking {
        val repository = MockAuthenticationRepository(delayMillis = 0)
        repository.createAccount("ana@example.com", "shorts2026")
        repository.verifyEmail("ana@example.com", repository.lastSentCode.getValue("ana@example.com"))

        repository.requestPasswordReset("ana@example.com")
        val code = repository.lastSentCode.getValue("ana@example.com")
        repository.resetPassword("ana@example.com", code, "brandnew123")

        assertThrows(AuthenticationException.InvalidCredentials::class.java) {
            runBlocking { repository.logIn("ana@example.com", "shorts2026") }
        }
        assertEquals("ana@example.com", repository.logIn("ana@example.com", "brandnew123").email)
    }
}
