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

/** `struct LogInView`. */
@Composable
fun LogInScreen(
    repository: AuthenticationRepository,
    showSignUp: () -> Unit,
    completeAuthentication: (AuthenticatedUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = centraliaViewModel(key = "logIn") { LogInViewModel(repository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showsPasswordReset by remember { mutableStateOf(false) }

    AuthenticationScaffold(
        modifier = modifier,
        footer = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "New to Centralia?",
                    style = CentraliaType.subheadline,
                    color = CentraliaColors.SecondaryText
                )

                CentraliaTextButton(
                    title = "Create account",
                    style = CentraliaType.subheadline.semibold(),
                    isDisabled = state.isBusy,
                    modifier = Modifier.padding(start = 5.dp),
                    onClick = showSignUp
                )
            }
        }
    ) {
        CentraliaAuthHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(
                text = "Welcome back.",
                style = CentraliaType.display,
                color = CentraliaColors.Ink,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your short-video library is ready.",
                style = CentraliaType.body,
                color = CentraliaColors.SecondaryText,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            CentraliaLabeledTextField(
                label = "Email",
                placeholder = "you@example.com",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                errorMessage = state.emailError,
                keyboardType = KeyboardType.Email
            )

            CentraliaLabeledTextField(
                label = "Password",
                placeholder = "Enter your password",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                errorMessage = state.passwordError,
                isSecure = true,
                imeAction = ImeAction.Done,
                onSubmit = { viewModel.logIn(completeAuthentication) }
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                CentraliaTextButton(
                    title = "Forgot password?",
                    style = CentraliaType.subheadline.semibold(),
                    isDisabled = state.isBusy,
                    onClick = { showsPasswordReset = true }
                )
            }

            state.failureMessage?.let { InlineAuthenticationError(message = it) }

            CentraliaPrimaryButton(
                title = "Log In",
                isLoading = state.isSubmittingEmail,
                onClick = { viewModel.logIn(completeAuthentication) }
            )

            AuthenticationDivider(
                text = "or continue with",
                modifier = Modifier.padding(vertical = Spacing.xSmall)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.fillMaxWidth()
            ) {
                AuthenticationProvider.entries.forEach { provider ->
                    CentraliaSecondaryButton(
                        title = provider.displayName,
                        isDisabled = state.isBusy,
                        modifier = Modifier.weight(1f),
                        icon = {
                            ProviderMark(
                                provider = provider,
                                isLoading = state.activeProvider == provider
                            )
                        },
                        onClick = { viewModel.authenticate(provider, completeAuthentication) }
                    )
                }
            }
        }
    }

    if (showsPasswordReset) {
        PasswordResetSheet(
            repository = repository,
            initialEmail = state.email,
            onDismiss = { showsPasswordReset = false }
        )
    }
}
