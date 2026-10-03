package com.centralia.app.data.mock

import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationException
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.auth.VerificationChallenge
import java.nio.ByteBuffer
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.delay


class MockAuthenticationRepository(
    private val delayMillis: Long = 650
) : AuthenticationRepository {

    private data class Account(var password: String, var verified: Boolean)

    private val accounts = mutableMapOf<String, Account>()
    private val codes = mutableMapOf<String, String>()


    val lastSentCode: Map<String, String> get() = codes

    override suspend fun createAccount(email: String, password: String): VerificationChallenge {
        simulateWork()
        val key = email.lowercase(Locale.ROOT)
        val existing = accounts[key]
        if (existing?.verified == true) throw AuthenticationException.AccountAlreadyExists
        accounts[key] = Account(password, verified = false)
        return sendCode(key)
    }

    override suspend fun logIn(email: String, password: String): AuthenticatedUser {
        simulateWork()
        val key = email.lowercase(Locale.ROOT)
        val account = accounts[key]
        if (account == null || account.password != password) throw AuthenticationException.InvalidCredentials
        if (!account.verified) throw AuthenticationException.EmailNotVerified(sendCode(key))
        return user(key)
    }

    override suspend fun verifyEmail(email: String, code: String): AuthenticatedUser {
        simulateWork()
        val key = email.lowercase(Locale.ROOT)
        val account = accounts[key] ?: throw AuthenticationException.InvalidCode
        if (codes[key] == null || codes[key] != code.filter { it.isDigit() }) {
            throw AuthenticationException.InvalidCode
        }
        codes.remove(key)
        account.verified = true
        return user(key)
    }

    override suspend fun resendVerificationCode(email: String): VerificationChallenge {
        simulateWork()
        return sendCode(email.lowercase(Locale.ROOT))
    }

    override suspend fun requestPasswordReset(email: String) {
        simulateWork()
        val key = email.lowercase(Locale.ROOT)
        if (accounts.containsKey(key)) sendCode(key)
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): AuthenticatedUser {
        simulateWork()
        val key = email.lowercase(Locale.ROOT)
        val account = accounts[key] ?: throw AuthenticationException.InvalidCode
        if (codes[key] == null || codes[key] != code.filter { it.isDigit() }) {
            throw AuthenticationException.InvalidCode
        }
        codes.remove(key)
        account.password = newPassword
        account.verified = true
        return user(key)
    }

    override suspend fun signOut() {
        simulateWork()
    }

    private fun sendCode(key: String): VerificationChallenge {
        val code = (0 until 6).joinToString("") { Random.nextInt(10).toString() }
        codes[key] = code
        return VerificationChallenge(email = key, resendAvailableInSeconds = 60)
    }

    private fun user(email: String) = AuthenticatedUser(
        id = deterministicID(email),
        displayName = displayName(email),
        email = email
    )

    private suspend fun simulateWork() {
        if (delayMillis > 0) delay(delayMillis)
    }


    private fun deterministicID(value: String): UUID {
        val bytes = ByteArray(16)
        val source = value.toByteArray(Charsets.UTF_8)
        source.copyInto(bytes, endIndex = minOf(source.size, 16))

        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long)
    }


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
