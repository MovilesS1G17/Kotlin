package com.centralia.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaLabeledTextField
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import androidx.compose.ui.unit.dp

/** `struct PasswordResetSheet`. */
@Composable
fun PasswordResetSheet(
    repository: AuthenticationRepository,
    initialEmail: String,
    onDismiss: () -> Unit
) {
    val viewModel = centraliaViewModel(key = "passwordReset") {
        PasswordResetViewModel(repository, initialEmail)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CentraliaSheet(
        title = "Password reset",
        onDismissRequest = onDismiss,
        trailingAction = SheetAction(
            title = if (state.didSend) "Done" else "Cancel",
            onClick = onDismiss
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            if (state.didSend) {
                ContentUnavailable(
                    title = "Check your email",
                    icon = CentraliaIcons.Envelope,
                    description = "A mock reset link was sent to " +
                        "${AuthenticationValidation.normalizedEmail(state.email)}.",
                    minHeight = 220.dp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Reset your password",
                        style = CentraliaType.display,
                        color = CentraliaColors.Ink
                    )

                    Text(
                        text = "Enter the email associated with your Centralia account.",
                        style = CentraliaType.body,
                        color = CentraliaColors.SecondaryText
                    )
                }

                CentraliaLabeledTextField(
                    label = "Email",
                    placeholder = "you@example.com",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    errorMessage = state.emailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onSubmit = viewModel::sendReset
                )

                state.failureMessage?.let { InlineAuthenticationError(message = it) }

                CentraliaPrimaryButton(
                    title = "Send reset link",
                    isLoading = state.isSubmitting,
                    onClick = viewModel::sendReset
                )
            }
        }
    }
}
