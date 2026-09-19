package com.centralia.app.data.mock

import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationException
import com.centralia.app.domain.auth.AuthenticationProvider
import com.centralia.app.domain.auth.AuthenticationRepository
import java.nio.ByteBuffer
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * `struct MockAuthenticationRepository`. The simulated latency and the demo
 * accounts that force each failure path are preserved so the same manual test
 * cases work: `existing@example.com`, `fail@example.com`,
 * `reset-fail@example.com`.
 */
class MockAuthenticationRepository(
    private val delayMillis: Long = 650,
    private val providerFailures: Map<AuthenticationProvider, AuthenticationException> = emptyMap()
) : AuthenticationRepository {

    override suspend fun createAccount(email: String, password: String): AuthenticatedUser {
        simulateWork()

        if (email.equals("existing@example.com", ignoreCase = true)) {
            throw AuthenticationException.AccountAlreadyExists
        }

        return AuthenticatedUser(
            id = deterministicID(email),
            displayName = displayName(email),
            email = email
        )
    }

    override suspend fun logIn(email: String, password: String): AuthenticatedUser {
        simulateWork()

        if (email.equals("fail@example.com", ignoreCase = true)) {
            throw AuthenticationException.InvalidCredentials
        }

        return AuthenticatedUser(
            id = deterministicID(email),
            displayName = displayName(email),
            email = email
        )
    }

    override suspend fun authenticate(provider: AuthenticationProvider): AuthenticatedUser {
        simulateWork()

        providerFailures[provider]?.let { throw it }

        return AuthenticatedUser(
            id = deterministicID(provider.rawValue),
            displayName = "Centralia User",
            email = "demo@${provider.rawValue}.example"
        )
    }

    override suspend fun requestPasswordReset(email: String) {
        simulateWork()

        if (email.equals("reset-fail@example.com", ignoreCase = true)) {
            throw AuthenticationException.ResetUnavailable
        }
    }

    override suspend fun signOut() {
        simulateWork()
    }

    private suspend fun simulateWork() {
        if (delayMillis > 0) delay(delayMillis)
    }

    /**
     * `deterministicID(for:)` — the first 16 UTF-8 bytes of the value, zero
     * padded, with the RFC 4122 version 4 and variant bits forced. Reproduced
     * byte for byte so the same email yields the same profile id as on iOS.
     */
    private fun deterministicID(value: String): UUID {
        val bytes = ByteArray(16)
        val source = value.toByteArray(Charsets.UTF_8)
        source.copyInto(bytes, endIndex = minOf(source.size, 16))

        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long)
    }

    /**
     * `displayName(from:)` — the local part with dots and underscores turned
     * into spaces, then capitalized the way Swift's `.capitalized` does
     * (every word's first letter upper, the rest lower).
     */
    private fun displayName(email: String): String {
        val localPart = email.substringBefore('@', missingDelimiterValue = "")
        if (localPart.isEmpty()) return "Centralia User"

        return localPart
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                if (word.isEmpty()) {
                    word
                } else {
                    word.substring(0, 1).uppercase(Locale.getDefault()) +
                        word.substring(1).lowercase(Locale.getDefault())
                }
            }
    }
}
