package com.centralia.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.data.remote.ApiException
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationException
import com.centralia.app.domain.auth.AuthenticationRepository
import com.centralia.app.domain.auth.VerificationChallenge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LogInViewModel(
    private val repository: AuthenticationRepository
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val failureMessage: String? = null,
        val isSubmitting: Boolean = false
    ) {
        val isBusy: Boolean get() = isSubmitting
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }


    fun logIn(
        onAuthenticated: (AuthenticatedUser) -> Unit,
        onVerificationRequired: (VerificationChallenge) -> Unit
    ) {
        if (_uiState.value.isBusy || !validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, failureMessage = null) }
            try {
                val user = repository.logIn(
                    email = AuthenticationValidation.normalizedEmail(_uiState.value.email),
                    password = _uiState.value.password
                )
                onAuthenticated(user)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unverified: AuthenticationException.EmailNotVerified) {
                val email = unverified.challenge.email.ifBlank {
                    AuthenticationValidation.normalizedEmail(_uiState.value.email)
                }
                onVerificationRequired(unverified.challenge.copy(email = email))
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun validateForm(): Boolean {
        val state = _uiState.value
        val emailError = AuthenticationValidation.emailError(state.email)
        val passwordError = if (state.password.isEmpty()) "Enter your password." else null

        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        return emailError == null && passwordError == null
    }
}


class SignUpViewModel(
    private val repository: AuthenticationRepository
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val passwordConfirmation: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val confirmationError: String? = null,
        val failureMessage: String? = null,
        val isSubmitting: Boolean = false
    ) {
        val isBusy: Boolean get() = isSubmitting
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun onPasswordConfirmationChange(value: String) =
        _uiState.update { it.copy(passwordConfirmation = value) }


    fun createAccount(onVerificationRequired: (VerificationChallenge) -> Unit) {
        if (_uiState.value.isBusy || !validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, failureMessage = null) }
            try {
                val challenge = repository.createAccount(
                    email = AuthenticationValidation.normalizedEmail(_uiState.value.email),
                    password = _uiState.value.password
                )
                onVerificationRequired(challenge)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun validateForm(): Boolean {
        val state = _uiState.value
        val emailError = AuthenticationValidation.emailError(state.email)
        val passwordError = AuthenticationValidation.passwordError(state.password)
        val confirmationError = if (state.password == state.passwordConfirmation) {
            null
        } else {
            "Passwords do not match."
        }

        _uiState.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmationError = confirmationError
            )
        }
        return emailError == null && passwordError == null && confirmationError == null
    }
}


private class ResendCountdown(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onTick: (Int) -> Unit
) {
    private var job: Job? = null

    fun start(seconds: Int) {
        job?.cancel()
        onTick(seconds.coerceAtLeast(0))
        if (seconds <= 0) return
        job = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                onTick(remaining)
            }
        }
    }
}


class VerifyEmailViewModel(
    private val repository: AuthenticationRepository,
    challenge: VerificationChallenge
) : ViewModel() {

    data class UiState(
        val email: String,
        val code: String = "",
        val codeError: String? = null,
        val failureMessage: String? = null,
        val infoMessage: String? = null,
        val isVerifying: Boolean = false,
        val isResending: Boolean = false,
        val secondsUntilResend: Int = 0
    ) {
        val isBusy: Boolean get() = isVerifying || isResending
        val canResend: Boolean get() = !isBusy && secondsUntilResend == 0
    }

    private val _uiState = MutableStateFlow(UiState(email = challenge.email))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val countdown = ResendCountdown(viewModelScope) { seconds ->
        _uiState.update { it.copy(secondsUntilResend = seconds) }
    }

    init {
        countdown.start(challenge.resendAvailableInSeconds)
    }


    fun onCodeChange(value: String, onAuthenticated: (AuthenticatedUser) -> Unit) {
        val code = AuthenticationValidation.normalizedCode(value)
        _uiState.update { it.copy(code = code, codeError = null, failureMessage = null) }
        if (code.length == 6) verify(onAuthenticated)
    }

    fun verify(onAuthenticated: (AuthenticatedUser) -> Unit) {
        val state = _uiState.value
        if (state.isBusy) return
        val codeError = AuthenticationValidation.codeError(state.code)
        _uiState.update { it.copy(codeError = codeError) }
        if (codeError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, failureMessage = null, infoMessage = null) }
            try {
                onAuthenticated(repository.verifyEmail(state.email, state.code))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // A wrong code is cleared so the next attempt starts fresh.
                _uiState.update { it.copy(failureMessage = error.localizedMessage, code = "") }
            } finally {
                _uiState.update { it.copy(isVerifying = false) }
            }
        }
    }

    fun resend() {
        if (!_uiState.value.canResend) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, failureMessage = null, infoMessage = null) }
            try {
                val challenge = repository.resendVerificationCode(_uiState.value.email)
                _uiState.update { it.copy(infoMessage = "We sent a new code to ${challenge.email}.", code = "") }
                countdown.start(challenge.resendAvailableInSeconds)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
                (error as? ApiException)?.retryAfter?.let(countdown::start)
            } finally {
                _uiState.update { it.copy(isResending = false) }
            }
        }
    }
}


class PasswordResetViewModel(
    private val repository: AuthenticationRepository,
    initialEmail: String
) : ViewModel() {

    enum class Step { EMAIL, CODE }

    data class UiState(
        val step: Step = Step.EMAIL,
        val email: String = "",
        val code: String = "",
        val newPassword: String = "",
        val passwordConfirmation: String = "",
        val emailError: String? = null,
        val codeError: String? = null,
        val passwordError: String? = null,
        val confirmationError: String? = null,
        val failureMessage: String? = null,
        val infoMessage: String? = null,
        val isSubmitting: Boolean = false,
        val secondsUntilResend: Int = 0
    ) {
        val canResend: Boolean get() = !isSubmitting && secondsUntilResend == 0
    }

    private val _uiState = MutableStateFlow(UiState(email = initialEmail))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val countdown = ResendCountdown(viewModelScope) { seconds ->
        _uiState.update { it.copy(secondsUntilResend = seconds) }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun onCodeChange(value: String) =
        _uiState.update { it.copy(code = AuthenticationValidation.normalizedCode(value), codeError = null) }

    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value) }

    fun onPasswordConfirmationChange(value: String) =
        _uiState.update { it.copy(passwordConfirmation = value) }


    fun sendCode() {
        if (_uiState.value.isSubmitting) return
        val emailError = AuthenticationValidation.emailError(_uiState.value.email)
        _uiState.update { it.copy(emailError = emailError) }
        if (emailError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, failureMessage = null, infoMessage = null) }
            try {
                val email = AuthenticationValidation.normalizedEmail(_uiState.value.email)
                repository.requestPasswordReset(email)
                _uiState.update { it.copy(step = Step.CODE, email = email, code = "") }
                countdown.start(60)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun resendCode() {
        if (!_uiState.value.canResend) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, failureMessage = null, infoMessage = null) }
            try {
                repository.requestPasswordReset(_uiState.value.email)
                // The backend never says whether the account exists.
                _uiState.update {
                    it.copy(infoMessage = "If an account exists for ${it.email}, a new code is on its way.", code = "")
                }
                countdown.start(60)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun useDifferentEmail() = _uiState.update {
        it.copy(step = Step.EMAIL, code = "", failureMessage = null, infoMessage = null)
    }


    fun resetPassword(onAuthenticated: (AuthenticatedUser) -> Unit) {
        val state = _uiState.value
        if (state.isSubmitting) return
        val codeError = AuthenticationValidation.codeError(state.code)
        val passwordError = AuthenticationValidation.passwordError(state.newPassword)
        val confirmationError =
            if (state.newPassword == state.passwordConfirmation) null else "Passwords do not match."
        _uiState.update {
            it.copy(codeError = codeError, passwordError = passwordError, confirmationError = confirmationError)
        }
        if (codeError != null || passwordError != null || confirmationError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, failureMessage = null, infoMessage = null) }
            try {
                onAuthenticated(repository.resetPassword(state.email, state.code, state.newPassword))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
