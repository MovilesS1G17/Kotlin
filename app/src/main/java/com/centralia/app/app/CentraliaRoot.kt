package com.centralia.app.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.centralia.app.feature.auth.LogInScreen
import com.centralia.app.feature.auth.SignUpScreen

/**
 * `struct ContentView` — the root switch between the two unauthenticated screens
 * and the authenticated shell.
 *
 * `.animation(.easeInOut(duration: 0.2), value: container.session.phase)` becomes
 * an [AnimatedContent] cross-fade over the same 200ms.
 */
@Composable
fun CentraliaRoot(
    container: DependencyContainer,
    modifier: Modifier = Modifier
) {
    val phase by container.session.phase.collectAsStateWithLifecycle()
    var restoring by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        try {
            container.authenticationRepository.restoreSession()?.let(container.session::completeAuthentication)
        } catch (_: Exception) {
            container.session.showLogIn()
        } finally {
            restoring = false
        }
    }
    if (restoring) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
                    showLogIn = container.session::showLogIn,
                    completeAuthentication = container.session::completeAuthentication,
                    modifier = modifier
                )

                AppSession.AuthenticationDestination.LOG_IN -> LogInScreen(
                    repository = container.authenticationRepository,
                    showSignUp = container.session::showSignUp,
                    completeAuthentication = container.session::completeAuthentication,
                    modifier = modifier
                )
            }

            is AppSession.Phase.Authenticated -> AuthenticatedAppScreen(
                user = currentPhase.user,
                container = container,
                modifier = modifier
            )
        }
    }
}
