package com.centralia.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.auth.VerificationChallenge
import com.centralia.app.ui.centraliaViewModel
import com.centralia.app.ui.components.CentraliaLabeledTextField
import com.centralia.app.ui.components.CentraliaPrimaryButton
import com.centralia.app.ui.components.CentraliaTextButton
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold


@Composable
fun VerifyEmailScreen(
    repository: AuthenticationRepository,
    challenge: VerificationChallenge,
    completeAuthentication: (AuthenticatedUser) -> Unit,
    useDifferentEmail: () -> Unit,
    modifier: Modifier = Modifier,
    attempt: String = challenge.email
) {
    val viewModel = centraliaViewModel(key = "verifyEmail-$attempt") {
        VerifyEmailViewModel(repository, challenge)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AuthenticationScaffold(
        modifier = modifier,
        footer = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Can't find it? Check your spam folder.",
                    style = CentraliaType.footnote,
                    color = CentraliaColors.SecondaryText,
                    textAlign = TextAlign.Center
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
                text = "Check your email.",
                style = CentraliaType.display,
                color = CentraliaColors.Ink,
                textAlign = TextAlign.Center
            )

            Text(
                text = "We sent a 6-digit code to ${state.email}. Enter it to confirm your account.",
                style = CentraliaType.body,
                color = CentraliaColors.SecondaryText,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            CentraliaLabeledTextField(
                label = "Verification code",
                placeholder = "123456",
                value = state.code,
                onValueChange = { viewModel.onCodeChange(it, completeAuthentication) },
                errorMessage = state.codeError,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onSubmit = { viewModel.verify(completeAuthentication) }
            )

            state.failureMessage?.let { InlineAuthenticationError(message = it) }
            state.infoMessage?.let { InlineAuthenticationInfo(message = it) }

            CentraliaPrimaryButton(
                title = "Verify email",
                isLoading = state.isVerifying,
                onClick = { viewModel.verify(completeAuthentication) }
            )

            ResendCodeButton(
                secondsUntilResend = state.secondsUntilResend,
                isEnabled = state.canResend,
                onResend = viewModel::resend
            )

            CentraliaTextButton(
                title = "Use a different email",
                style = CentraliaType.subheadline.semibold(),
                color = CentraliaColors.SecondaryText,
                isDisabled = state.isBusy,
                onClick = useDifferentEmail
            )
        }
    }
}
