package com.centralia.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.profile.LibraryExportService
import com.centralia.app.domain.profile.LibraryStatistics
import com.centralia.app.domain.profile.NotificationPreferences
import com.centralia.app.domain.profile.ProfileStorageUsage
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.domain.profile.UserRepository
import com.centralia.app.feature.library.LoadState
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `@Observable final class ProfileViewModel` — Screen 9. */
class ProfileViewModel(
    private val authenticatedUser: AuthenticatedUser,
    private val userRepository: UserRepository,
    private val videoRepository: VideoItemRepository,
    private val folderRepository: FolderRepository,
    private val exportService: LibraryExportService,
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    data class UiState(
        val state: LoadState = LoadState.Idle,
        val profile: UserProfile? = null,
        val statistics: LibraryStatistics = LibraryStatistics.empty,
        val notificationPreferences: NotificationPreferences = NotificationPreferences.defaults,
        val storageUsage: ProfileStorageUsage = ProfileStorageUsage.mock,
        val isUpdatingProfile: Boolean = false,
        val isChangingPassword: Boolean = false,
        val isSavingPreferences: Boolean = false,
        val isExporting: Boolean = false,
        val isSigningOut: Boolean = false,
        val failureMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        val current = _uiState.value.state
        if (current != LoadState.Idle && !current.isFailure) return

        viewModelScope.launch {
            _uiState.update { it.copy(state = LoadState.Loading, failureMessage = null) }
            try {
                // The profile has to resolve first: the other three loads need its id.
                val loadedProfile = userRepository.profile(authenticatedUser)

                coroutineScope {
                    val loadedVideos = async { videoRepository.videos() }
                    val loadedFolders = async { folderRepository.folders() }
                    val loadedPreferences = async {
                        userRepository.notificationPreferences(loadedProfile.id)
                    }

                    val videos = loadedVideos.await()
                    val folders = loadedFolders.await()
                    val preferences = loadedPreferences.await()

                    _uiState.update {
                        it.copy(
                            profile = loadedProfile,
                            notificationPreferences = preferences,
                            statistics = statistics(videos, folders),
                            state = LoadState.Loaded
                        )
                    }
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(state = LoadState.Failed(error.localizedMessage ?: "Please try again."))
                }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(state = LoadState.Idle) }
        load()
    }

    fun updateProfile(
        displayName: String,
        email: String,
        onUpdated: (AuthenticatedUser) -> Unit,
        onFailed: () -> Unit
    ) {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isUpdatingProfile) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingProfile = true, failureMessage = null) }
            try {
                val updatedProfile = userRepository.updateProfile(
                    UserProfile(
                        id = profile.id,
                        displayName = displayName,
                        email = email,
                        membershipStatus = profile.membershipStatus
                    )
                )
                _uiState.update { it.copy(profile = updatedProfile) }
                onUpdated(updatedProfile.authenticatedUser)
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
                onFailed()
            } finally {
                _uiState.update { it.copy(isUpdatingProfile = false) }
            }
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onChanged: () -> Unit,
        onFailed: () -> Unit
    ) {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isChangingPassword) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, failureMessage = null) }
            try {
                userRepository.changePassword(profile.id, currentPassword, newPassword)
                onChanged()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
                onFailed()
            } finally {
                _uiState.update { it.copy(isChangingPassword = false) }
            }
        }
    }

    /** Optimistic: applies the toggle, then rolls it back if the save fails. */
    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isSavingPreferences) return

        val previousPreferences = _uiState.value.notificationPreferences

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    notificationPreferences = preferences,
                    isSavingPreferences = true,
                    failureMessage = null
                )
            }
            try {
                val saved = userRepository.updateNotificationPreferences(preferences, profile.id)
                _uiState.update { it.copy(notificationPreferences = saved) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        notificationPreferences = previousPreferences,
                        failureMessage = error.localizedMessage
                    )
                }
            } finally {
                _uiState.update { it.copy(isSavingPreferences = false) }
            }
        }
    }

    fun prepareExport(onPrepared: (File) -> Unit, onFailed: () -> Unit) {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, failureMessage = null) }
            try {
                onPrepared(exportService.exportLibrary(profile))
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
                onFailed()
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit, onFailed: (String?) -> Unit) {
        if (_uiState.value.isSigningOut) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true, failureMessage = null) }
            try {
                authenticationRepository.signOut()
                onSignedOut()
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
                onFailed(error.localizedMessage)
            } finally {
                _uiState.update { it.copy(isSigningOut = false) }
            }
        }
    }

    fun dismissFailure() = _uiState.update { it.copy(failureMessage = null) }

    /** `statistics(videos:folders:)`. */
    private fun statistics(
        videos: List<VideoItem>,
        folders: List<LibraryFolder>
    ): LibraryStatistics {
        val platformCounts = mutableMapOf<VideoPlatform, Int>()
        videos.forEach { video ->
            platformCounts[video.platform] = (platformCounts[video.platform] ?: 0) + 1
        }

        return LibraryStatistics(
            savedCount = videos.size,
            folderCount = folders.size,
            unorganizedCount = videos.count { it.folderID == null },
            platformCounts = platformCounts
        )
    }
}
