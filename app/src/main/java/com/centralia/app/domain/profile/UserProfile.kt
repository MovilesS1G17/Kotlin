package com.centralia.app.domain.profile

import com.centralia.app.domain.UuidSerializer
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.library.VideoPlatform
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `MembershipStatus`. */
@Serializable
enum class MembershipStatus {
    @SerialName("centraliaMember")
    CENTRALIA_MEMBER;

    val displayName: String
        get() = when (this) {
            CENTRALIA_MEMBER -> "Centralia member"
        }
}

/** `UserProfile`. */
@Serializable
data class UserProfile(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val displayName: String,
    val email: String,
    val membershipStatus: MembershipStatus
) {
    /** `initials` — first letters of up to two whitespace-separated words. */
    val initials: String
        get() {
            val value = displayName
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .take(2)
                .mapNotNull { it.firstOrNull() }
                .joinToString("")
                .uppercase()
            return value.ifEmpty { "C" }
        }

    val authenticatedUser: AuthenticatedUser
        get() = AuthenticatedUser(id = id, displayName = displayName, email = email)
}

/** `NotificationPreferences`. */
@Serializable
data class NotificationPreferences(
    val organizationReminders: Boolean,
    val weeklyLibrarySummary: Boolean,
    val productUpdates: Boolean
) {
    companion object {
        val defaults = NotificationPreferences(
            organizationReminders = true,
            weeklyLibrarySummary = true,
            productUpdates = false
        )
    }
}

/** `LibraryStatistics`. */
data class LibraryStatistics(
    val savedCount: Int,
    val folderCount: Int,
    val unorganizedCount: Int,
    val platformCounts: Map<VideoPlatform, Int>
) {
    fun count(platform: VideoPlatform): Int = platformCounts[platform] ?: 0

    companion object {
        val empty = LibraryStatistics(
            savedCount = 0,
            folderCount = 0,
            unorganizedCount = 0,
            platformCounts = emptyMap()
        )
    }
}

/** `ProfileStorageUsage`. */
data class ProfileStorageUsage(
    val usedGigabytes: Double,
    val capacityGigabytes: Double
) {
    val fractionUsed: Double
        get() = if (capacityGigabytes > 0) {
            (usedGigabytes / capacityGigabytes).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

    val percentage: Int
        get() = Math.round(fractionUsed * 100).toInt()

    /**
     * `summary` — Swift formats used with one fraction digit and capacity with
     * none: "2.4 GB of 5 GB (48%)".
     */
    val summary: String
        get() = "%.1f GB of %.0f GB (%d%%)".format(
            usedGigabytes,
            capacityGigabytes,
            percentage
        )

    companion object {
        val mock = ProfileStorageUsage(usedGigabytes = 2.4, capacityGigabytes = 5.0)
    }
}
