package com.centralia.app.domain.auth

import java.util.UUID


data class AuthenticatedUser(
    val id: UUID,
    val displayName: String,
    val email: String?
)


data class VerificationChallenge(
    val email: String,

    val resendAvailableInSeconds: Int
)


sealed class AuthenticationException(message: String) : Exception(message) {
    data object InvalidCredentials : AuthenticationException(
        "The email or password is incorrect. Please try again."
    )

    data object AccountAlreadyExists : AuthenticationException(
        "An account already exists for this email. Try logging in instead."
    )

    data object InvalidCode : AuthenticationException(
        "That code isn't right. Check the email and try again."
    )

    /** The password was right but the email isn't confirmed; a code was emailed. */
    class EmailNotVerified(val challenge: VerificationChallenge) : AuthenticationException(
        "Confirm your email to continue. We sent a code to ${challenge.email}."
    )
}
