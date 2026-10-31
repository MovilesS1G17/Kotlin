package com.centralia.app.data.offline

import android.content.SharedPreferences
import com.centralia.app.data.remote.ApiException
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.profile.NotificationPreferences
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.domain.profile.UserRepository
import java.util.UUID
import kotlinx.serialization.json.Json

/**
 * Eventual connectivity + local storage (Sprint 3) for the Profile tab:
 * reading the profile and notification settings is network falling back to
 * the last copy (SharedPreferences, JSON); changing them is network only.
 */
class OfflineUserRepository(
    private val remote: UserRepository,
    private val preferences: SharedPreferences
) : UserRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun profile(authenticatedUser: AuthenticatedUser): UserProfile =
        try {
            remote.profile(authenticatedUser).also {
                preferences.edit().putString(KEY_PROFILE, json.encodeToString(UserProfile.serializer(), it)).apply()
            }
        } catch (error: Exception) {
            if (!ApiException.isOffline(error)) throw error
            preferences.getString(KEY_PROFILE, null)
                ?.let { runCatching { json.decodeFromString(UserProfile.serializer(), it) }.getOrNull() }
                ?: throw OfflineException.needsConnection("Loading your profile for the first time")
        }

    override suspend fun notificationPreferences(userID: UUID): NotificationPreferences =
        try {
            remote.notificationPreferences(userID).also {
                preferences.edit()
                    .putString(KEY_NOTIFICATIONS, json.encodeToString(NotificationPreferences.serializer(), it))
                    .apply()
            }
        } catch (error: Exception) {
            if (!ApiException.isOffline(error)) throw error
            preferences.getString(KEY_NOTIFICATIONS, null)
                ?.let { runCatching { json.decodeFromString(NotificationPreferences.serializer(), it) }.getOrNull() }
                ?: NotificationPreferences.defaults
        }

    override suspend fun updateProfile(profile: UserProfile): UserProfile =
        networkOnly("Changing your profile") { remote.updateProfile(profile) }

    override suspend fun changePassword(userID: UUID, currentPassword: String, newPassword: String) =
        networkOnly("Changing your password") { remote.changePassword(userID, currentPassword, newPassword) }

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences,
        userID: UUID
    ): NotificationPreferences =
        networkOnly("Changing notifications") { remote.updateNotificationPreferences(preferences, userID) }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private suspend fun <T> networkOnly(action: String, send: suspend () -> T): T =
        try {
            send()
        } catch (error: Exception) {
            if (ApiException.isOffline(error)) throw OfflineException.needsConnection(action)
            throw error
        }

    private companion object {
        const val KEY_PROFILE = "profile"
        const val KEY_NOTIFICATIONS = "notificationPreferences"
    }
}
