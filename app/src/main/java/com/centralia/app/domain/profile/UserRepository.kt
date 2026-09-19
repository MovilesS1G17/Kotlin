package com.centralia.app.domain.profile

import com.centralia.app.domain.auth.AuthenticatedUser
import java.util.UUID

/** `protocol UserRepository`. */
interface UserRepository {
    suspend fun profile(authenticatedUser: AuthenticatedUser): UserProfile

    suspend fun updateProfile(profile: UserProfile): UserProfile

    suspend fun changePassword(userID: UUID, currentPassword: String, newPassword: String)

    suspend fun notificationPreferences(userID: UUID): NotificationPreferences

    suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences,
        userID: UUID
    ): NotificationPreferences
}

/** `UserRepositoryError`. */
sealed class UserRepositoryException(message: String) : Exception(message) {
    data object ProfileNotFound : UserRepositoryException(
        "Your profile could not be found. Please sign in again."
    )

    data object DisplayNameRequired : UserRepositoryException("Enter your name.")

    data object InvalidEmail : UserRepositoryException("Enter a valid email address.")

    data object IncorrectCurrentPassword : UserRepositoryException(
        "The current password is incorrect."
    )

    data object PasswordUnchanged : UserRepositoryException(
        "Your new password must be different from the current password."
    )
}
