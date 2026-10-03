package com.centralia.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.centralia.app.domain.profile.LibraryStatistics
import com.centralia.app.domain.profile.NotificationPreferences
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.feature.auth.AuthenticationValidation
import com.centralia.app.ui.components.CentraliaAlert
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLabeledTextField
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.ProminentAction
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.components.SheetSectionFooter
import com.centralia.app.ui.components.SheetSectionHeader
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing

/** `private struct EditProfileSheet`. */
@Composable
fun EditProfileSheet(
    profile: UserProfile,
    isSaving: Boolean,
    failureMessage: String?,
    onDismissFailure: () -> Unit,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var email by remember { mutableStateOf(profile.email) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val submit = {
        val trimmedName = displayName.trim()
        nameError = if (trimmedName.isEmpty()) "Enter your name." else null
        emailError = AuthenticationValidation.emailError(email)

        if (nameError == null && emailError == null) {
            onSave(trimmedName, email)
        }
    }

    CentraliaSheet(
        title = "Edit Profile",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss),
        trailingAction = SheetAction(
            title = if (isSaving) "Saving…" else "Save",
            isEnabled = !isSaving,
            onClick = submit
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
                .imePadding()
        ) {
            SheetSectionHeader(title = "Personal information")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                CentraliaLabeledTextField(
                    label = "Name",
                    placeholder = "Your name",
                    value = displayName,
                    onValueChange = { displayName = it },
                    errorMessage = nameError,
                    capitalization = KeyboardCapitalization.Words
                )

                CentraliaLabeledTextField(
                    label = "Email",
                    placeholder = "you@example.com",
                    value = email,
                    onValueChange = { email = it },
                    errorMessage = emailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onSubmit = submit
                )
            }

            SheetSectionFooter(
                text = "Changing your email may require verification when the live API is " +
                    "connected."
            )
        }
    }

    failureMessage?.let { message ->
        CentraliaAlert(
            title = "Couldn’t Update Profile",
            message = message,
            onConfirm = onDismissFailure
        )
    }
}

/** `private struct ChangePasswordSheet`. */
@Composable
fun ChangePasswordSheet(
    isSaving: Boolean,
    failureMessage: String?,
    onDismissFailure: () -> Unit,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmationError by remember { mutableStateOf<String?>(null) }

    val submit = {
        currentPasswordError = if (currentPassword.isEmpty()) {
            "Enter your current password."
        } else {
            null
        }
        newPasswordError = AuthenticationValidation.passwordError(newPassword)
        confirmationError = if (newPassword == confirmation) null else "Passwords do not match."

        if (currentPasswordError == null &&
            newPasswordError == null &&
            confirmationError == null
        ) {
            onSave(currentPassword, newPassword)
        }
    }

    CentraliaSheet(
        title = "Change Password",
        onDismissRequest = onDismiss,
        skipPartiallyExpanded = true,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss),
        trailingAction = SheetAction(
            title = if (isSaving) "Saving…" else "Save",
            isEnabled = !isSaving,
            onClick = submit
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
                .imePadding()
        ) {
            SheetSectionHeader(title = "Current password")

            Box(modifier = Modifier.padding(horizontal = Spacing.medium)) {
                CentraliaLabeledTextField(
                    label = "Current password",
                    placeholder = "Enter your current password",
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    errorMessage = currentPasswordError,
                    isSecure = true
                )
            }

            SheetSectionHeader(title = "New password")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                CentraliaLabeledTextField(
                    label = "New password",
                    placeholder = "At least 8 characters",
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    errorMessage = newPasswordError,
                    isSecure = true
                )

                CentraliaLabeledTextField(
                    label = "Confirm new password",
                    placeholder = "Repeat your new password",
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    errorMessage = confirmationError,
                    isSecure = true,
                    imeAction = ImeAction.Done,
                    onSubmit = submit
                )
            }
        }
    }

    failureMessage?.let { message ->
        CentraliaAlert(
            title = "Couldn’t Change Password",
            message = message,
            onConfirm = onDismissFailure
        )
    }
}

/**
 * `private struct NotificationPreferencesSheet` — each toggle saves immediately,
 * as the Swift binding's setter does.
 */
@Composable
fun NotificationPreferencesSheet(
    preferences: NotificationPreferences,
    isSaving: Boolean,
    onPreferencesChange: (NotificationPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    CentraliaSheet(
        title = "Notifications",
        onDismissRequest = onDismiss,
        trailingAction = SheetAction(title = "Done", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
        ) {
            PreferenceToggle(
                title = "Organization reminders",
                detail = "Remind me when saved shorts still need a folder.",
                isChecked = preferences.organizationReminders,
                isEnabled = !isSaving,
                onCheckedChange = {
                    onPreferencesChange(preferences.copy(organizationReminders = it))
                }
            )

            PreferenceToggle(
                title = "Weekly library summary",
                detail = "Receive a weekly overview of saved content.",
                isChecked = preferences.weeklyLibrarySummary,
                isEnabled = !isSaving,
                onCheckedChange = {
                    onPreferencesChange(preferences.copy(weeklyLibrarySummary = it))
                }
            )

            PreferenceToggle(
                title = "Product updates",
                detail = "Hear about new Centralia features.",
                isChecked = preferences.productUpdates,
                isEnabled = !isSaving,
                onCheckedChange = {
                    onPreferencesChange(preferences.copy(productUpdates = it))
                }
            )

            SheetSectionFooter(
                text = "Changes are saved automatically and will sync with your account."
            )
        }
    }
}

/** `preferenceToggle(_:detail:keyPath:)`. */
@Composable
private fun PreferenceToggle(
    title: String,
    detail: String,
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = CentraliaType.body,
                color = CentraliaColors.Ink
            )

            Text(
                text = detail,
                style = CentraliaType.caption,
                color = CentraliaColors.SecondaryText
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = isEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CentraliaColors.Surface,
                checkedTrackColor = CentraliaColors.Ink,
                uncheckedThumbColor = CentraliaColors.Surface,
                uncheckedTrackColor = CentraliaColors.Divider,
                uncheckedBorderColor = CentraliaColors.Divider
            )
        )
    }
}

/** `private struct ExportLibrarySheet`. */
@Composable
fun ExportLibrarySheet(
    statistics: LibraryStatistics,
    isExporting: Boolean,
    failureMessage: String?,
    onDismissFailure: () -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    CentraliaSheet(
        title = "Export Library Data",
        onDismissRequest = onDismiss,
        leadingAction = SheetAction(title = "Cancel", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.medium)
                .navigationBarsPadding()
        ) {
            SheetSectionHeader(title = "Included in your export")

            ExportDetailRow(
                icon = CentraliaIcons.PlayRectangle,
                text = "${statistics.savedCount} saved shorts"
            )
            ExportDetailRow(
                icon = CentraliaIcons.Folder,
                text = "${statistics.folderCount} folders"
            )
            ExportDetailRow(
                icon = CentraliaIcons.Tag,
                text = "Tags, notes, and source links"
            )
            ExportDetailRow(
                icon = CentraliaIcons.ProfileCard,
                text = "Profile and notification preferences"
            )

            SheetSectionHeader(title = "Format")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small)
            ) {
                Text(
                    text = "File type",
                    style = CentraliaType.body,
                    color = CentraliaColors.Ink,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "JSON",
                    style = CentraliaType.body,
                    color = CentraliaColors.SecondaryText
                )
            }

            SheetSectionFooter(
                text = "The export is human-readable and can also be imported by another " +
                    "service."
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                if (isExporting) {
                    CircularProgressIndicator(color = CentraliaColors.Ink)
                } else {
                    ProminentAction(title = "Export", onClick = onExport)
                }
            }
        }
    }

    failureMessage?.let { message ->
        CentraliaAlert(
            title = "Export Unavailable",
            message = message,
            onConfirm = onDismissFailure
        )
    }
}

/** One `Label(_:systemImage:)` row in the export summary. */
@Composable
private fun ExportDetailRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CentraliaColors.Ink,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = text,
            style = CentraliaType.body,
            color = CentraliaColors.Ink
        )
    }
}
