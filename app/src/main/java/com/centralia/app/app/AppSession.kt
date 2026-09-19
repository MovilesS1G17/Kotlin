package com.centralia.app.app

import com.centralia.app.domain.auth.AuthenticatedUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * `@Observable final class AppSession`. The observable `phase` property becomes a
 * [StateFlow], which is how Compose observes state held outside the composition.
 */
class AppSession {

    /** `AppSession.Phase`. */
    sealed interface Phase {
        data class Unauthenticated(val destination: AuthenticationDestination) : Phase
        data class Authenticated(val user: AuthenticatedUser) : Phase
    }

    /** `AppSession.AuthenticationDestination`. */
    enum class AuthenticationDestination { SIGN_UP, LOG_IN }

    private val _phase = MutableStateFlow<Phase>(
        Phase.Unauthenticated(AuthenticationDestination.SIGN_UP)
    )
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    fun showSignUp() {
        _phase.value = Phase.Unauthenticated(AuthenticationDestination.SIGN_UP)
    }

    fun showLogIn() {
        _phase.value = Phase.Unauthenticated(AuthenticationDestination.LOG_IN)
    }

    fun completeAuthentication(user: AuthenticatedUser) {
        _phase.value = Phase.Authenticated(user)
    }

    /** Ignored unless already authenticated, as the `guard case` in Swift does. */
    fun updateAuthenticatedUser(user: AuthenticatedUser) {
        if (_phase.value !is Phase.Authenticated) return
        _phase.value = Phase.Authenticated(user)
    }

    fun signOut() {
        _phase.value = Phase.Unauthenticated(AuthenticationDestination.LOG_IN)
    }
}
