package com.centralia.app.app

import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.VerificationChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class AppSession {


    sealed interface Phase {

        data class Unauthenticated(val destination: AuthenticationDestination, val visit: Int = 0) : Phase


        data class VerifyingEmail(
            val challenge: VerificationChallenge,

            val returnTo: AuthenticationDestination,

            val attempt: java.util.UUID = java.util.UUID.randomUUID()
        ) : Phase

        data class Authenticated(val user: AuthenticatedUser) : Phase
    }


    enum class AuthenticationDestination { SIGN_UP, LOG_IN }

    private val _phase = MutableStateFlow<Phase>(
        Phase.Unauthenticated(AuthenticationDestination.SIGN_UP)
    )
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    private var visit = 0

    fun showSignUp() {
        _phase.value = Phase.Unauthenticated(AuthenticationDestination.SIGN_UP, visit)
    }

    fun showLogIn() {
        _phase.value = Phase.Unauthenticated(AuthenticationDestination.LOG_IN, visit)
    }

    fun showEmailVerification(challenge: VerificationChallenge, returnTo: AuthenticationDestination) {
        _phase.value = Phase.VerifyingEmail(challenge, returnTo)
    }

    fun completeAuthentication(user: AuthenticatedUser) {
        _phase.value = Phase.Authenticated(user)
    }


    fun updateAuthenticatedUser(user: AuthenticatedUser) {
        if (_phase.value !is Phase.Authenticated) return
        _phase.value = Phase.Authenticated(user)
    }

    fun signOut() {
        visit += 1
        _phase.value = Phase.Unauthenticated(AuthenticationDestination.LOG_IN, visit)
    }
}
