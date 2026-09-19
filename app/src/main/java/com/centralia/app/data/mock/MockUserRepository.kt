package com.centralia.app.data.mock

import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.profile.MembershipStatus
import com.centralia.app.domain.profile.NotificationPreferences
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.domain.profile.UserRepository
import com.centralia.app.domain.profile.UserRepositoryException
import com.centralia.app.feature.auth.AuthenticationValidation
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

/** `actor MockUserRepository`. */
class MockUserRepository(
    private val store: MockDataStore,
    private val filename: String = "users-v1.json",
    private val delayMillis: Long = 280
) : UserRepository {

    @Serializable
    internal data class UserRecord(
        val profile: UserProfile,
        val notificationPreferences: NotificationPreferences
    )

    @Serializable
    internal data class Snapshot(
        val schemaVersion: Int,
        val records: List<UserRecord>
    )

    override suspend fun profile(authenticatedUser: AuthenticatedUser): UserProfile {
        simulateWork()

        var resolved: UserProfile? = null

        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            val existing = current.records.firstOrNull { it.profile.id == authenticatedUser.id }
            if (existing != null) {
                resolved = existing.profile
                return@mutate current
            }

            // First sign-in for this identity: materialise a profile from it.
            val profile = UserProfile(
                id = authenticatedUser.id,
                displayName = authenticatedUser.displayName,
                email = authenticatedUser.email ?: "",
                membershipStatus = MembershipStatus.CENTRALIA_MEMBER
            )
            resolved = profile
            current.copy(
                records = current.records + UserRecord(profile, NotificationPreferences.defaults)
            )
        }

        return requireNotNull(resolved)
    }

    override suspend fun updateProfile(profile: UserProfile): UserProfile {
        simulateWork()

        val normalizedName = profile.displayName.trim()
        val normalizedEmail = AuthenticationValidation.normalizedEmail(profile.email)

        if (normalizedName.isEmpty()) throw UserRepositoryException.DisplayNameRequired
        if (AuthenticationValidation.emailError(normalizedEmail) != null) {
            throw UserRepositoryException.InvalidEmail
        }

        var updated: UserProfile? = null

        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.records.none { it.profile.id == profile.id }) {
                throw UserRepositoryException.ProfileNotFound
            }

            current.copy(
                records = current.records.map { record ->
                    if (record.profile.id != profile.id) {
                        record
                    } else {
                        val newProfile = record.profile.copy(
                            displayName = normalizedName,
                            email = normalizedEmail
                        )
                        updated = newProfile
                        record.copy(profile = newProfile)
                    }
                }
            )
        }

        return requireNotNull(updated)
    }

    override suspend fun changePassword(
        userID: UUID,
        currentPassword: String,
        newPassword: String
    ) {
        simulateWork()
        val current = snapshot()

        if (current.records.none { it.profile.id == userID }) {
            throw UserRepositoryException.ProfileNotFound
        }
        // The mock rejects this one sentinel so the failure path is testable.
        if (currentPassword == "wrong-password") {
            throw UserRepositoryException.IncorrectCurrentPassword
        }
        if (currentPassword == newPassword) {
            throw UserRepositoryException.PasswordUnchanged
        }
    }

    override suspend fun notificationPreferences(userID: UUID): NotificationPreferences {
        simulateWork()
        val record = snapshot().records.firstOrNull { it.profile.id == userID }
            ?: throw UserRepositoryException.ProfileNotFound
        return record.notificationPreferences
    }

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences,
        userID: UUID
    ): NotificationPreferences {
        simulateWork()

        store.mutate(Snapshot.serializer(), filename, { seed() }) { current ->
            if (current.records.none { it.profile.id == userID }) {
                throw UserRepositoryException.ProfileNotFound
            }
            current.copy(
                records = current.records.map { record ->
                    if (record.profile.id == userID) {
                        record.copy(notificationPreferences = preferences)
                    } else {
                        record
                    }
                }
            )
        }

        return preferences
    }

    private suspend fun snapshot(): Snapshot =
        store.load(Snapshot.serializer(), filename, { seed() })

    private suspend fun simulateWork() {
        if (delayMillis > 0) delay(delayMillis)
    }

    private companion object {
        fun seed() = Snapshot(schemaVersion = 1, records = emptyList())
    }
}
