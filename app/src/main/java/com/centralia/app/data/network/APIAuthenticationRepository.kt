package com.centralia.app.data.network

import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationException
import com.centralia.app.domain.auth.AuthenticationProvider
import com.centralia.app.domain.auth.AuthenticationRepository
import java.util.UUID
import org.json.JSONObject

class APIAuthenticationRepository(private val api: CentraliaApi) : AuthenticationRepository {
    override suspend fun createAccount(displayName: String, email: String, password: String): AuthenticatedUser {
        val response = JSONObject(api.request("/auth/register", "POST", JSONObject()
            .put("display_name", displayName).put("email", email).put("password", password), false))
        api.tokens.save(response.getString("access_token"))
        return try { requireNotNull(restoreSession()) } catch (error: Exception) { api.tokens.clear(); throw error }
    }
    override suspend fun logIn(email: String, password: String): AuthenticatedUser {
        val response = JSONObject(api.request("/auth/login", "POST", JSONObject()
            .put("email", email).put("password", password), false))
        api.tokens.save(response.getString("access_token"))
        return try { requireNotNull(restoreSession()) } catch (error: Exception) { api.tokens.clear(); throw error }
    }
    override suspend fun restoreSession(): AuthenticatedUser? {
        if (api.tokens.read() == null) return null
        return try { parseUser(JSONObject(api.request("/me"))) }
        catch (error: ApiException) { if (error.status == 401) null else throw error }
    }
    override suspend fun updateDisplayName(name: String): AuthenticatedUser =
        parseUser(JSONObject(api.request("/me", "PATCH", JSONObject().put("display_name", name))))
    override suspend fun authenticate(provider: AuthenticationProvider): AuthenticatedUser =
        throw AuthenticationException.ProviderUnavailable
    override suspend fun requestPasswordReset(email: String): Unit =
        throw AuthenticationException.ResetUnavailable
    override suspend fun signOut() { api.tokens.clear() }
    private fun parseUser(value: JSONObject) = AuthenticatedUser(
        UUID.fromString(value.getString("id")), value.getString("display_name"), value.getString("email"))
}
