package com.centralia.app.domain.auth

import java.util.UUID

/** `AuthenticatedUser` — a Swift struct with value semantics becomes a data class. */
data class AuthenticatedUser(
    val id: UUID,
    val displayName: String,
    val email: String?
)

/** `AuthenticationProvider`. */
enum class AuthenticationProvider(val rawValue: String) {
    APPLE("apple"),
    GOOGLE("google");

    /** `rawValue.capitalized`. */
    val displayName: String
        get() = rawValue.replaceFirstChar { it.uppercase() }
}

/**
 * `AuthenticationError: LocalizedError`. Swift surfaces the copy through
 * `errorDescription` and the views read `error.localizedDescription`; on Kotlin
 * the same copy travels as the exception's `message`.
 */
sealed class AuthenticationException(message: String) : Exception(message) {
    data object InvalidCredentials : AuthenticationException(
        "The email or password is incorrect. Please try again."
    )

    data object AccountAlreadyExists : AuthenticationException(
        "An account already exists for this email. Try logging in instead."
    )

    data object ProviderCancelled : AuthenticationException("Sign in was cancelled.")

    data object ProviderUnavailable : AuthenticationException(
        "This sign-in provider is unavailable right now. Please try again."
    )

    data object ResetUnavailable : AuthenticationException(
        "We could not send a reset link right now. Please try again."
    )
}
