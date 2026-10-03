package com.centralia.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.library.FolderRepository
import com.centralia.app.domain.library.VideoItemRepository
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.profile.LibraryExportService
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.domain.profile.UserRepository
import com.centralia.app.feature.library.LoadState
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CapsuleShape
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaDivider
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLoadingState
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.rememberShareFile
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.bold
import com.centralia.app.ui.theme.semibold

/** The four sheets `ProfileView` can present. */
private enum class ProfileSheet {
    EDIT_PROFILE,
    CHANGE_PASSWORD,
    NOTIFICATION_PREFERENCES,
    EXPORT_LIBRARY
}

/** `struct ProfileView` — Screen 9. */
@Composable
fun ProfileScreen(
    authenticatedUser: AuthenticatedUser,
    userRepository: UserRepository,
    videoRepository: VideoItemRepository,
    folderRepository: FolderRepository,
    exportService: LibraryExportService,
    authenticationRepository: AuthenticationRepository,
    onUserChanged: (AuthenticatedUser) -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "profile-${authenticatedUser.id}") {
        ProfileViewModel(
            authenticatedUser,
            userRepository,
            videoRepository,
            folderRepository,
            exportService,
            authenticationRepository
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var activeSheet by remember { mutableStateOf<ProfileSheet?>(null) }
    var showsSignOutConfirmation by remember { mutableStateOf(false) }
    var showsPasswordConfirmation by remember { mutableStateOf(false) }
    var signOutFailure by remember { mutableStateOf<String?>(null) }
    var editProfileFailure by remember { mutableStateOf<String?>(null) }
    var changePasswordFailure by remember { mutableStateOf<String?>(null) }
    var exportFailure by remember { mutableStateOf<String?>(null) }

    val shareFile = rememberShareFile()

    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Spacing.medium, bottom = Spacing.xLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
    ) {
        item(key = "title") {
            CenteredContent {
                Text(
                    text = "Profile",
                    style = CentraliaType.display,
                    color = CentraliaColors.Ink,
                    modifier = Modifier.padding(horizontal = Spacing.medium)
                )
            }
        }

        when (val loadState = state.state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") {
                CentraliaLoadingState(label = "Loading profile…", minHeight = 520.dp)
            }

            is LoadState.Failed -> item(key = "failed") {
                CenteredContent {
                    ContentUnavailable(
                        title = "Profile unavailable",
                        icon = CentraliaIcons.Warning,
                        description = loadState.message,
                        minHeight = 520.dp,
                        actions = {
                            ProminentAction(title = "Try Again", onClick = viewModel::retry)
                        }
                    )
                }
            }

            LoadState.Loaded -> {
                val profile = state.profile

                if (profile != null) {
                    item(key = "identity") {
                        CenteredContent {
                            IdentityCard(
                                profile = profile,
                                modifier = Modifier.padding(horizontal = Spacing.medium)
                            )
                        }
                    }

                    item(key = "accountActions") {
                        CenteredContent {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = Spacing.medium)
                                    .fillMaxWidth()
                                    .background(
                                        CentraliaColors.Surface,
                                        RoundedCornerShape(22.dp)
                                    )
                                    .border(
                                        1.dp,
                                        CentraliaColors.Divider,
                                        RoundedCornerShape(22.dp)
                                    )
                            ) {
                                ProfileActionRow(title = "Edit Profile") {
                                    activeSheet = ProfileSheet.EDIT_PROFILE
                                }
                                CentraliaDivider(
                                    modifier = Modifier.padding(horizontal = Spacing.medium)
                                )
                                ProfileActionRow(title = "Change Password") {
                                    activeSheet = ProfileSheet.CHANGE_PASSWORD
                                }
                                CentraliaDivider(
                                    modifier = Modifier.padding(horizontal = Spacing.medium)
                                )
                                ProfileActionRow(title = "Notification Preferences") {
                                    activeSheet = ProfileSheet.NOTIFICATION_PREFERENCES
                                }
                            }
                        }
                    }

                    item(key = "storage") {
                        CenteredContent {
                            StorageCard(
                                summary = state.storageUsage.summary,
                                fractionUsed = state.storageUsage.fractionUsed.toFloat(),
                                onExport = { activeSheet = ProfileSheet.EXPORT_LIBRARY },
                                modifier = Modifier.padding(horizontal = Spacing.medium)
                            )
                        }
                    }

                    item(key = "librarySummary") {
                        CenteredContent {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = Spacing.medium)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Your library",
                                    style = CentraliaType.sectionTitle,
                                    color = CentraliaColors.Ink
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            CentraliaColors.Ink,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(vertical = Spacing.medium),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileStat(
                                        value = state.statistics.savedCount,
                                        label = "Saved",
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileStatDivider()
                                    ProfileStat(
                                        value = state.statistics.folderCount,
                                        label = "Folders",
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProfileStatDivider()
                                    ProfileStat(
                                        value = state.statistics.unorganizedCount,
                                        label = "To organize",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "platformSummary") {
                        CenteredContent {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = Spacing.medium)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Saved by platform",
                                    style = CentraliaType.sectionTitle,
                                    color = CentraliaColors.Ink
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            CentraliaColors.Surface,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            1.dp,
                                            CentraliaColors.Divider,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(Spacing.medium),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                                ) {
                                    PlatformStat(
                                        color = CentraliaColors.Ink,
                                        title = "TikTok",
                                        value = state.statistics.count(VideoPlatform.TIKTOK),
                                        modifier = Modifier.weight(1f)
                                    )
                                    PlatformStat(
                                        color = CentraliaColors.VideoLavender,
                                        title = "Reels",
                                        value = state.statistics
                                            .count(VideoPlatform.INSTAGRAM_REEL),
                                        modifier = Modifier.weight(1f)
                                    )
                                    PlatformStat(
                                        color = CentraliaColors.VideoClay,
                                        title = "Shorts",
                                        value = state.statistics
                                            .count(VideoPlatform.YOUTUBE_SHORT),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "signOut") {
                        CenteredContent {
                            CentraliaPrimaryButton(
                                title = "Sign Out",
                                isLoading = state.isSigningOut,
                                modifier = Modifier.padding(horizontal = Spacing.medium),
                                onClick = { showsSignOutConfirmation = true }
                            )
                        }
                    }
                }
            }
        }
    }

    when (activeSheet) {
        ProfileSheet.EDIT_PROFILE -> state.profile?.let { profile ->
            EditProfileSheet(
                profile = profile,
                isSaving = state.isUpdatingProfile,
                failureMessage = editProfileFailure,
                onDismissFailure = { editProfileFailure = null },
                onSave = { name, email ->
                    viewModel.updateProfile(
                        displayName = name,
                        email = email,
                        onUpdated = { user ->
                            onUserChanged(user)
                            activeSheet = null
                        },
                        onFailed = {
                            editProfileFailure =
                                "Your changes could not be saved. Please try again."
                        }
                    )
                },
                onDismiss = { activeSheet = null }
            )
        }

        ProfileSheet.CHANGE_PASSWORD -> ChangePasswordSheet(
            isSaving = state.isChangingPassword,
            failureMessage = changePasswordFailure,
            onDismissFailure = { changePasswordFailure = null },
            onSave = { currentPassword, newPassword ->
                viewModel.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    onChanged = {
                        activeSheet = null
                        showsPasswordConfirmation = true
                    },
                    onFailed = {
                        changePasswordFailure = "Your password could not be changed. " +
                            "Check your current password and try again."
                    }
                )
            },
            onDismiss = { activeSheet = null }
        )

        ProfileSheet.NOTIFICATION_PREFERENCES -> NotificationPreferencesSheet(
            preferences = state.notificationPreferences,
            isSaving = state.isSavingPreferences,
            onPreferencesChange = viewModel::updateNotificationPreferences,
            onDismiss = { activeSheet = null }
        )

        ProfileSheet.EXPORT_LIBRARY -> ExportLibrarySheet(
            statistics = state.statistics,
            isExporting = state.isExporting,
            failureMessage = exportFailure,
            onDismissFailure = { exportFailure = null },
            onExport = {
                viewModel.prepareExport(
                    onPrepared = { file ->
                        if (!shareFile(file)) {
                            exportFailure = "Your library export could not be shared."
                        }
                    },
                    onFailed = {
                        exportFailure = "Your library export could not be prepared."
                    }
                )
            },
            onDismiss = { activeSheet = null }
        )

        null -> Unit
    }

    if (showsSignOutConfirmation) {
        CentraliaAlert(
            title = "Sign out of Centralia?",
            message = "You’ll need to sign in again to access your library.",
            confirmTitle = "Sign Out",
            dismissTitle = "Cancel",
            isDestructive = true,
            onConfirm = {
                showsSignOutConfirmation = false
                viewModel.signOut(
                    onSignedOut = onSignedOut,
                    onFailed = { message -> signOutFailure = message }
                )
            },
            onDismiss = { showsSignOutConfirmation = false }
        )
    }

    if (showsPasswordConfirmation) {
        CentraliaAlert(
            title = "Password Updated",
            message = "Your Centralia password has been changed.",
            onConfirm = { showsPasswordConfirmation = false }
        )
    }

    signOutFailure?.let { message ->
        CentraliaAlert(
            title = "Couldn’t Sign Out",
            message = message,
            onConfirm = { signOutFailure = null }
        )
    }
}

/** `identityCard(_:)` — initials avatar, name, email, membership pill. */
@Composable
private fun IdentityCard(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .background(CentraliaColors.Surface, RoundedCornerShape(22.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(22.dp))
            .padding(Spacing.medium)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(CentraliaColors.Ink, CircleShape)
                .semanticsHidden(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.initials,
                style = CentraliaType.title2.bold(),
                color = CentraliaColors.Surface
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = profile.displayName,
                style = CentraliaType.sectionTitle,
                color = CentraliaColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = profile.email,
                style = CentraliaType.subheadline,
                color = CentraliaColors.SecondaryText
            )

            Box(
                modifier = Modifier
                    .background(CentraliaColors.SoftSurface, CapsuleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = profile.membershipStatus.displayName,
                    style = CentraliaType.caption.semibold(),
                    color = CentraliaColors.Ink
                )
            }
        }
    }
}

/** `storageCard` — the usage meter above the export action. */
@Composable
private fun StorageCard(
    summary: String,
    fractionUsed: Float,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CentraliaColors.Surface, RoundedCornerShape(22.dp))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(22.dp))
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Text(
            text = "STORAGE & DATA",
            style = CentraliaType.caption.bold(),
            color = CentraliaColors.SecondaryText
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Storage Used",
                style = CentraliaType.headline,
                color = CentraliaColors.Ink,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = summary,
                style = CentraliaType.subheadline,
                color = CentraliaColors.SecondaryText
            )
        }

        LinearProgressIndicator(
            progress = { fractionUsed },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = CentraliaColors.Ink,
            trackColor = CentraliaColors.SoftSurface
        )

        CentraliaDivider()

        ProfileActionRow(
            title = "Export Library Data",
            hasHorizontalPadding = false,
            onClick = onExport
        )
    }
}

/** `private struct ProfileActionRow`. */
@Composable
private fun ProfileActionRow(
    title: String,
    modifier: Modifier = Modifier,
    hasHorizontalPadding: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .centraliaPressable(onClick = onClick)
            .padding(horizontal = if (hasHorizontalPadding) Spacing.medium else 0.dp)
    ) {
        Text(
            text = title,
            style = CentraliaType.headline,
            color = CentraliaColors.Ink,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = CentraliaIcons.ChevronRight,
            contentDescription = null,
            tint = CentraliaColors.SecondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** `private struct ProfileStat`. */
@Composable
private fun ProfileStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value.toString(),
            style = CentraliaType.title.bold(),
            color = CentraliaColors.Surface
        )

        Text(
            text = label,
            style = CentraliaType.caption.semibold(),
            color = CentraliaColors.Surface.copy(alpha = 0.82f)
        )
    }
}

/** `private struct ProfileStatDivider`. */
@Composable
private fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 46.dp)
            .background(CentraliaColors.Surface.copy(alpha = 0.24f))
            .semanticsHidden()
    )
}

/** `private struct PlatformStat`. */
@Composable
private fun PlatformStat(
    color: Color,
    title: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
                .semanticsHidden()
        )

        Text(
            text = title,
            style = CentraliaType.subheadline.semibold(),
            color = CentraliaColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value.toString(),
            style = CentraliaType.subheadline.bold(),
            color = CentraliaColors.Ink
        )
    }
}
