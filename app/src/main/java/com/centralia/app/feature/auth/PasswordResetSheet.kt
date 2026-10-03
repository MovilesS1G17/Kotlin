package com.centralia.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaLabeledTextField
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold


@Composable
fun PasswordResetSheet(
    repository: AuthenticationRepository,
    initialEmail: String,
    onDismiss: () -> Unit,
    completeAuthentication: (AuthenticatedUser) -> Unit,
    attempt: Int = 0
) {
    val viewModel = centraliaViewModel(key = "passwordReset-$attempt") {
        PasswordResetViewModel(repository, initialEmail)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CentraliaSheet(
        title = "Password reset",
        onDismissRequest = onDismiss,
        trailingAction = SheetAction(title = "Cancel", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.large)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            when (state.step) {
                PasswordResetViewModel.Step.EMAIL -> {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        Text(
                            text = "Reset your password",
                            style = CentraliaType.display,
                            color = CentraliaColors.Ink
                        )
                        Text(
                            text = "Enter the email of your Centralia account and we'll send you a 6-digit code.",
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
                        onSubmit = viewModel::sendCode
                    )

                    state.failureMessage?.let { InlineAuthenticationError(message = it) }

                    CentraliaPrimaryButton(
                        title = "Send code",
                        isLoading = state.isSubmitting,
                        onClick = viewModel::sendCode
                    )
                }

                PasswordResetViewModel.Step.CODE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        Text(
                            text = "Check your email",
                            style = CentraliaType.display,
                            color = CentraliaColors.Ink
                        )
                        Text(
                            text = "If an account exists for ${state.email}, we sent it a 6-digit code. " +
                                "Enter it with your new password.",
                            style = CentraliaType.body,
                            color = CentraliaColors.SecondaryText
                        )
                    }

                    CentraliaLabeledTextField(
                        label = "Code",
                        placeholder = "123456",
                        value = state.code,
                        onValueChange = viewModel::onCodeChange,
                        errorMessage = state.codeError,
                        keyboardType = KeyboardType.Number
                    )

                    CentraliaLabeledTextField(
                        label = "New password",
                        placeholder = AuthenticationValidation.PASSWORD_HINT,
                        value = state.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        errorMessage = state.passwordError,
                        isSecure = true
                    )

                    CentraliaLabeledTextField(
                        label = "Confirm new password",
                        placeholder = "Repeat your new password",
                        value = state.passwordConfirmation,
                        onValueChange = viewModel::onPasswordConfirmationChange,
                        errorMessage = state.confirmationError,
                        isSecure = true,
                        imeAction = ImeAction.Done,
                        onSubmit = { viewModel.resetPassword(completeAuthentication) }
                    )

                    state.failureMessage?.let { InlineAuthenticationError(message = it) }
                    state.infoMessage?.let { InlineAuthenticationInfo(message = it) }

                    CentraliaPrimaryButton(
                        title = "Reset password",
                        isLoading = state.isSubmitting,
                        onClick = { viewModel.resetPassword(completeAuthentication) }
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)
                    ) {
                        ResendCodeButton(
                            secondsUntilResend = state.secondsUntilResend,
                            isEnabled = state.canResend,
                            onResend = viewModel::resendCode
                        )
                        CentraliaTextButton(
                            title = "Use a different email",
                            style = CentraliaType.subheadline.semibold(),
                            color = CentraliaColors.SecondaryText,
                            isDisabled = state.isSubmitting,
                            onClick = viewModel::useDifferentEmail
                        )
                    }
                }
            }
        }
    }
}
