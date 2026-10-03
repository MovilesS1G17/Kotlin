package com.centralia.app.data.remote

import com.centralia.app.data.remote.ApiClient.Method
import com.centralia.app.domain.UuidSerializer
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationException
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.auth.VerificationChallenge
import com.centralia.app.domain.imports.ImportedVideoMetadata
import com.centralia.app.domain.imports.VideoImportPipeline
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlayback
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.profile.NotificationPreferences
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.domain.profile.UserRepository
import com.centralia.app.domain.search.SearchHistoryRepository
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


class RemoteAuthenticationRepository(private val client: ApiClient) : AuthenticationRepository {

    @Serializable
    private data class SessionUser(
        @Serializable(with = UuidSerializer::class) val id: UUID,
        val displayName: String,
        val email: String? = null
    )

    @Serializable
    private data class SessionResponse(val accessToken: String, val user: SessionUser)

    @Serializable
    private data class PendingVerification(val email: String, val resendAvailableIn: Int = 60)

    override suspend fun createAccount(email: String, password: String): VerificationChallenge =
        translating {
            client.send(
                Method.POST, "v1/auth/register", PendingVerification.serializer(),
                body = buildJsonObject { put("email", email); put("password", password) },
                authenticated = false
            ).challenge()
        }

    override suspend fun logIn(email: String, password: String): AuthenticatedUser = translating {
        store(
            client.send(
                Method.POST, "v1/auth/login", SessionResponse.serializer(),
                body = buildJsonObject { put("email", email); put("password", password) },
                authenticated = false
            )
        )
    }

    override suspend fun verifyEmail(email: String, code: String): AuthenticatedUser = translating {
        store(
            client.send(
                Method.POST, "v1/auth/verify-email", SessionResponse.serializer(),
                body = buildJsonObject { put("email", email); put("code", code) },
                authenticated = false
            )
        )
    }

    override suspend fun resendVerificationCode(email: String): VerificationChallenge = translating {
        client.send(
            Method.POST, "v1/auth/verification-code", PendingVerification.serializer(),
            body = buildJsonObject { put("email", email) },
            authenticated = false
        ).challenge()
    }

    override suspend fun requestPasswordReset(email: String) = translating {
        client.execute(
            Method.POST, "v1/auth/password-reset",
            body = buildJsonObject { put("email", email) },
            authenticated = false
        )
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): AuthenticatedUser =
        translating {
            store(
                client.send(
                    Method.POST, "v1/auth/password-reset/confirm", SessionResponse.serializer(),
                    body = buildJsonObject {
                        put("email", email)
                        put("code", code)
                        put("newPassword", newPassword)
                    },
                    authenticated = false
                )
            )
        }

    override suspend fun signOut() {
        try {
            client.execute(Method.POST, "v1/auth/logout")
        } catch (_: ApiException) {
            // A failed revoke still signs this device out.
        } finally {
            client.tokenStore.token = null
        }
    }

    override suspend fun restoreSession(): AuthenticatedUser? {
        if (client.tokenStore.token == null) return null
        return try {
            client.send(Method.GET, "v1/me", UserProfile.serializer()).authenticatedUser
        } catch (_: ApiException) {
            null
        }
    }


    private suspend fun <T> translating(block: suspend () -> T): T = try {
        block()
    } catch (error: ApiException) {
        throw when (error.code) {
            "email_not_verified" -> AuthenticationException.EmailNotVerified(
                VerificationChallenge(email = error.email.orEmpty(), resendAvailableInSeconds = error.retryAfter ?: 60)
            )
            "invalid_credentials" -> AuthenticationException.InvalidCredentials
            "account_already_exists" -> AuthenticationException.AccountAlreadyExists
            else -> error
        }
    }

    private fun PendingVerification.challenge() =
        VerificationChallenge(email = email, resendAvailableInSeconds = resendAvailableIn)

    private fun store(session: SessionResponse): AuthenticatedUser {
        client.tokenStore.token = session.accessToken
        return AuthenticatedUser(
            id = session.user.id,
            displayName = session.user.displayName,
            email = session.user.email
        )
    }
}

// ------------------------------------------------------------------ library


class RemoteLibraryRepository(private val client: ApiClient) : VideoItemRepository, FolderRepository {

    override suspend fun videos(): List<VideoItem> =
        client.send(Method.GET, "v1/videos", ListSerializer(VideoItem.serializer()))

    override suspend fun saveVideo(video: VideoItem) {
        client.send(
            Method.POST, "v1/videos", VideoItem.serializer(),
            body = client.encode(VideoItem.serializer(), video)
        )
    }

    override suspend fun deleteVideo(id: UUID) {
        client.execute(Method.DELETE, "v1/videos/$id")
    }


    override suspend fun restoreVideo(video: VideoItem) {
        client.send(Method.POST, "v1/videos/${video.id}/restore", VideoItem.serializer())
    }


    override suspend fun moveVideo(id: UUID, folderID: UUID?) {
        patchVideo(id, buildJsonObject {
            put("folderID", folderID?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
        })
    }

    override suspend fun updateNote(id: UUID, note: String?) {
        patchVideo(id, buildJsonObject { put("note", note?.let { JsonPrimitive(it) } ?: JsonNull) })
    }

    override suspend fun updateTags(id: UUID, tags: List<String>) {
        patchVideo(id, buildJsonObject { put("tags", JsonArray(tags.map { JsonPrimitive(it) })) })
    }

    override suspend fun playback(id: UUID): VideoPlayback {
        val playback = client.send(Method.GET, "v1/videos/$id/playback", VideoPlayback.serializer())
        return playback.copy(streamURL = playback.streamURL?.let(client::absoluteUrl))
    }

    private suspend fun patchVideo(id: UUID, body: kotlinx.serialization.json.JsonObject) {
        client.send(Method.PATCH, "v1/videos/$id", VideoItem.serializer(), body = body)
    }

    override suspend fun folders(): List<LibraryFolder> =
        client.send(Method.GET, "v1/folders", ListSerializer(LibraryFolder.serializer()))

    override suspend fun createFolder(name: String, symbolName: String): LibraryFolder =
        client.send(
            Method.POST, "v1/folders", LibraryFolder.serializer(),
            body = buildJsonObject { put("name", name); put("symbolName", symbolName) }
        )

    override suspend fun renameFolder(id: UUID, name: String): LibraryFolder =
        client.send(
            Method.PATCH, "v1/folders/$id", LibraryFolder.serializer(),
            body = buildJsonObject { put("name", name) }
        )

    override suspend fun deleteFolder(id: UUID) {
        client.execute(Method.DELETE, "v1/folders/$id")
    }
}

// ------------------------------------------------------------------ perfil


class RemoteUserRepository(private val client: ApiClient) : UserRepository {

    override suspend fun profile(authenticatedUser: AuthenticatedUser): UserProfile =
        client.send(Method.GET, "v1/me", UserProfile.serializer())

    override suspend fun updateProfile(profile: UserProfile): UserProfile =
        client.send(
            Method.PATCH, "v1/me", UserProfile.serializer(),
            body = buildJsonObject {
                put("displayName", profile.displayName)
                put("email", profile.email)
            }
        )

    override suspend fun changePassword(userID: UUID, currentPassword: String, newPassword: String) {
        client.execute(
            Method.POST, "v1/me/password",
            body = buildJsonObject {
                put("currentPassword", currentPassword)
                put("newPassword", newPassword)
            }
        )
    }

    override suspend fun notificationPreferences(userID: UUID): NotificationPreferences =
        client.send(Method.GET, "v1/me/notification-preferences", NotificationPreferences.serializer())

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferences,
        userID: UUID
    ): NotificationPreferences =
        client.send(
            Method.PUT, "v1/me/notification-preferences", NotificationPreferences.serializer(),
            body = client.encode(NotificationPreferences.serializer(), preferences)
        )
}

// ------------------------------------------------------------------ historial de busqueda


class RemoteSearchHistoryRepository(private val client: ApiClient) : SearchHistoryRepository {

    override suspend fun recentSearches(): List<String> =
        client.send(Method.GET, "v1/search-history", ListSerializer(String.serializer()))

    override suspend fun recordSearch(query: String) {
        client.execute(Method.POST, "v1/search-history", body = buildJsonObject { put("query", query) })
    }

    override suspend fun clearSearchHistory() {
        client.execute(Method.DELETE, "v1/search-history")
    }
}

// ----------------------- importacion del pipeline


class RemoteVideoImportPipeline(private val client: ApiClient) : VideoImportPipeline {

    @Serializable
    private data class PlatformResponse(val platform: VideoPlatform)

    @Serializable
    private data class TagsResponse(val tags: List<String>)

    @Serializable
    private data class FolderSuggestionResponse(val folderName: String? = null)

    override suspend fun detectPlatform(sourceURL: String): VideoPlatform =
        client.send(
            Method.POST, "v1/imports/detect", PlatformResponse.serializer(),
            body = buildJsonObject { put("sourceURL", sourceURL) }
        ).platform

    override suspend fun extractMetadata(sourceURL: String, platform: VideoPlatform): ImportedVideoMetadata =
        client.send(
            Method.POST, "v1/imports/metadata", ImportedVideoMetadata.serializer(),
            body = buildJsonObject {
                put("sourceURL", sourceURL)
                put("platform", platform.rawValue)
            }
        )

    override suspend fun generateTags(metadata: ImportedVideoMetadata): List<String> =
        client.send(
            Method.POST, "v1/imports/tags", TagsResponse.serializer(),
            body = buildJsonObject {
                put("metadata", client.encode(ImportedVideoMetadata.serializer(), metadata))
            }
        ).tags

    override suspend fun suggestFolder(metadata: ImportedVideoMetadata, tags: List<String>): String? =
        client.send(
            Method.POST, "v1/imports/folder-suggestion", FolderSuggestionResponse.serializer(),
            body = buildJsonObject {
                put("metadata", client.encode(ImportedVideoMetadata.serializer(), metadata))
                put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
            }
        ).folderName
}
