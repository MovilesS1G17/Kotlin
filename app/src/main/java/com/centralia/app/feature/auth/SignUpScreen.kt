package com.centralia.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationProvider
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaLabeledTextField
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaSecondaryButton
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold

/** `struct SignUpView`. */
@Composable
fun SignUpScreen(
    repository: AuthenticationRepository,
    showLogIn: () -> Unit,
    completeAuthentication: (AuthenticatedUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "signUp") { SignUpViewModel(repository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var presentedLegalDocument by remember { mutableStateOf<LegalDocument?>(null) }

    AuthenticationScaffold(
        modifier = modifier,
        footer = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Already have an account?",
                        style = CentraliaType.subheadline,
                        color = CentraliaColors.SecondaryText
                    )

                    CentraliaTextButton(
                        title = "Log In",
                        style = CentraliaType.subheadline.semibold(),
                        isDisabled = state.isBusy,
                        modifier = Modifier.padding(start = 5.dp),
                        onClick = showLogIn
                    )
                }

                LegalAgreementText(
                    openDocument = { presentedLegalDocument = it }
                )
            }
        }
    ) {
        CentraliaAuthHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(
                text = "Save every short worth keeping.",
                style = CentraliaType.display,
                color = CentraliaColors.Ink,
                textAlign = TextAlign.Center
            )

            Text(
                text = "TikToks, Reels, and Shorts together.",
                style = CentraliaType.body,
                color = CentraliaColors.SecondaryText,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xLarge)
        ) {
            when (state.mode) {
                SignUpViewModel.Mode.OPTIONS -> OptionControls(
                    state = state,
                    onAuthenticate = { provider ->
                        viewModel.authenticate(provider, completeAuthentication)
                    },
                    onContinueWithEmail = viewModel::showEmailForm
                )

                SignUpViewModel.Mode.EMAIL -> EmailControls(
                    state = state,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onConfirmationChange = viewModel::onPasswordConfirmationChange,
                    onCreateAccount = { viewModel.createAccount(completeAuthentication) },
                    onUseAnotherMethod = viewModel::showOptions
                )
            }
        }
    }

    presentedLegalDocument?.let { document ->
        LegalDocumentSheet(
            document = document,
            onDismiss = { presentedLegalDocument = null }
        )
    }
}

/** `optionControls` — the two provider buttons above the email path. */
@Composable
private fun OptionControls(
    state: SignUpViewModel.UiState,
    onAuthenticate: (AuthenticationProvider) -> Unit,
    onContinueWithEmail: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        CentraliaSecondaryButton(
            title = "Continue with Apple",
            isDisabled = state.isBusy,
            icon = {
                ProviderMark(
                    provider = AuthenticationProvider.APPLE,
                    isLoading = state.activeProvider == AuthenticationProvider.APPLE
                )
            },
            onClick = { onAuthenticate(AuthenticationProvider.APPLE) }
        )

        CentraliaSecondaryButton(
            title = "Continue with Google",
            isDisabled = state.isBusy,
            icon = {
                ProviderMark(
                    provider = AuthenticationProvider.GOOGLE,
                    isLoading = state.activeProvider == AuthenticationProvider.GOOGLE
                )
            },
            onClick = { onAuthenticate(AuthenticationProvider.GOOGLE) }
        )

        AuthenticationDivider(
            text = "or",
            modifier = Modifier.padding(vertical = Spacing.xSmall)
        )

        CentraliaPrimaryButton(
            title = "Continue with email",
            isDisabled = state.isBusy,
            onClick = onContinueWithEmail
        )

        state.failureMessage?.let { InlineAuthenticationError(message = it) }
    }
}

/** `emailControls` — email, password and confirmation. */
@Composable
private fun EmailControls(
    state: SignUpViewModel.UiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
    onUseAnotherMethod: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        CentraliaLabeledTextField(
            label = "Email",
            placeholder = "you@example.com",
            value = state.email,
            onValueChange = onEmailChange,
            errorMessage = state.emailError,
            keyboardType = KeyboardType.Email
        )

        CentraliaLabeledTextField(
            label = "Password",
            placeholder = "At least 8 characters",
            value = state.password,
            onValueChange = onPasswordChange,
            errorMessage = state.passwordError,
            isSecure = true
        )

        CentraliaLabeledTextField(
            label = "Confirm password",
            placeholder = "Repeat your password",
            value = state.passwordConfirmation,
            onValueChange = onConfirmationChange,
            errorMessage = state.confirmationError,
            isSecure = true,
            imeAction = ImeAction.Done,
            onSubmit = onCreateAccount
        )

        state.failureMessage?.let { InlineAuthenticationError(message = it) }

        CentraliaPrimaryButton(
            title = "Create account",
            isLoading = state.isSubmittingEmail,
            onClick = onCreateAccount
        )

        CentraliaTextButton(
            title = "Use another method",
            style = CentraliaType.subheadline.semibold(),
            isDisabled = state.isBusy,
            onClick = onUseAnotherMethod
        )
    }
}
