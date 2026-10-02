package com.centralia.app.domain.auth

/**
 * `protocol AuthenticationRepository`. Swift's `async throws` maps to a Kotlin
 * `suspend` function that throws, so call sites keep the same shape.
 */
interface AuthenticationRepository {
    suspend fun createAccount(email: String, password: String): AuthenticatedUser

    suspend fun logIn(email: String, password: String): AuthenticatedUser

    suspend fun authenticate(provider: AuthenticationProvider): AuthenticatedUser

    suspend fun requestPasswordReset(email: String)

    suspend fun signOut()
}
