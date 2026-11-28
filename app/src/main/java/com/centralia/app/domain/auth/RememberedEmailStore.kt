package com.centralia.app.domain.auth

/**
 * "Remember my email" on the sign-in screen (Sprint 3 · local storage).
 * Android keeps it in Jetpack DataStore (Alex); iOS keeps it in the Keychain.
 * Only the email is stored, never the password, and it survives sign-out on
 * purpose: that is when the user needs it.
 */
interface RememberedEmailStore {
    suspend fun email(): String?

    suspend fun remember(email: String)

    suspend fun forget()
}

/** Used by the mock build and the unit tests. */
class InMemoryRememberedEmailStore(private var saved: String? = null) : RememberedEmailStore {
    override suspend fun email(): String? = saved

    override suspend fun remember(email: String) {
        saved = email
    }

    override suspend fun forget() {
        saved = null
    }
}
