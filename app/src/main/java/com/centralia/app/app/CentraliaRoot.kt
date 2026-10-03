package com.centralia.app.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.feature.auth.LogInScreen
import com.centralia.app.feature.auth.SignUpScreen
import com.centralia.app.feature.auth.VerifyEmailScreen
import com.centralia.app.ui.theme.CentraliaColors


@Composable
fun CentraliaRoot(
    container: DependencyContainer,
    modifier: Modifier = Modifier
) {
    val phase by container.session.phase.collectAsStateWithLifecycle()
    var isRestoringSession by remember { mutableStateOf(true) }

    // A returning user with a valid stored token goes straight to the library.
    LaunchedEffect(Unit) {
        container.authenticationRepository.restoreSession()
            ?.let(container.session::completeAuthentication)
        isRestoringSession = false
    }

    if (isRestoringSession) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CentraliaColors.Ink)
        }
        return
    }

    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "centraliaPhase"
    ) { currentPhase ->
        when (currentPhase) {
            is AppSession.Phase.Unauthenticated -> when (currentPhase.destination) {
                AppSession.AuthenticationDestination.SIGN_UP -> SignUpScreen(
                    repository = container.authenticationRepository,
                    visit = currentPhase.visit,
                    showLogIn = container.session::showLogIn,
                    requireVerification = { challenge ->
                        container.session.showEmailVerification(
                            challenge, AppSession.AuthenticationDestination.SIGN_UP
                        )
                    },
                    modifier = modifier
                )

                AppSession.AuthenticationDestination.LOG_IN -> LogInScreen(
                    repository = container.authenticationRepository,
                    visit = currentPhase.visit,
                    showSignUp = container.session::showSignUp,
                    completeAuthentication = container.session::completeAuthentication,
                    requireVerification = { challenge ->
                        container.session.showEmailVerification(
                            challenge, AppSession.AuthenticationDestination.LOG_IN
                        )
                    },
                    modifier = modifier
                )
            }

            is AppSession.Phase.VerifyingEmail -> VerifyEmailScreen(
                repository = container.authenticationRepository,
                challenge = currentPhase.challenge,
                attempt = currentPhase.attempt.toString(),
                completeAuthentication = container.session::completeAuthentication,
                useDifferentEmail = {
                    if (currentPhase.returnTo == AppSession.AuthenticationDestination.SIGN_UP) {
                        container.session.showSignUp()
                    } else {
                        container.session.showLogIn()
                    }
                },
                modifier = modifier
            )

            is AppSession.Phase.Authenticated -> AuthenticatedAppScreen(
                user = currentPhase.user,
                container = container,
                modifier = modifier
            )
        }
    }
}
