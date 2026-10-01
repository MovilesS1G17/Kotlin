package com.centralia.app.data.network

import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.profile.MembershipStatus
import com.centralia.app.domain.profile.NotificationPreferences
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.domain.profile.UserRepository
import java.util.UUID
import org.json.JSONObject

class APIUserRepository(private val api: CentraliaApi) : UserRepository {
    override suspend fun profile(authenticatedUser: AuthenticatedUser): UserProfile =
        parse(JSONObject(api.request("/me")))
    override suspend fun updateProfile(profile: UserProfile): UserProfile {
        val current = JSONObject(api.request("/me"))
        if (profile.email != current.getString("email")) throw ApiException(422, "Email cannot be updated.")
        return parse(JSONObject(api.request("/me", "PATCH", JSONObject().put("display_name", profile.displayName))))
    }
    override suspend fun changePassword(userID: UUID, currentPassword: String, newPassword: String): Unit =
        throw ApiException(501, "Password changes are not available yet.")
    override suspend fun notificationPreferences(userID: UUID): NotificationPreferences = NotificationPreferences.defaults
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences, userID: UUID): NotificationPreferences =
        throw ApiException(501, "Notification preferences are not available yet.")
    private fun parse(value: JSONObject) = UserProfile(
        UUID.fromString(value.getString("id")), value.getString("display_name"),
        value.getString("email"), MembershipStatus.CENTRALIA_MEMBER)
}
