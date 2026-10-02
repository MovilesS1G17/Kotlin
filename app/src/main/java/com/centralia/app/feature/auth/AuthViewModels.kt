package com.centralia.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.auth.AuthenticationProvider
import com.centralia.app.domain.auth.AuthenticationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * `@Observable final class LogInViewModel`.
 *
 * The Swift model exposes each field as an independent observable property. The
 * Compose-idiomatic shape is one immutable state value in a [StateFlow], so the
 * properties are gathered into [UiState]; the fields, validation and error
 * handling are otherwise identical.
 *
 * The Swift `logIn()` returns the user and the view forwards it to
 * `completeAuthentication`. Here the work runs in [viewModelScope], so success is
 * delivered through a callback instead of a return value.
 */
class LogInViewModel(
    private val repository: AuthenticationRepository
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val failureMessage: String? = null,
        val isSubmittingEmail: Boolean = false,
        val activeProvider: AuthenticationProvider? = null
    ) {
        val isBusy: Boolean get() = isSubmittingEmail || activeProvider != null
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun logIn(onAuthenticated: (AuthenticatedUser) -> Unit) {
        if (_uiState.value.isBusy || !validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingEmail = true, failureMessage = null) }
            try {
                val user = repository.logIn(
                    email = AuthenticationValidation.normalizedEmail(_uiState.value.email),
                    password = _uiState.value.password
                )
                onAuthenticated(user)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSubmittingEmail = false) }
            }
        }
    }

    fun authenticate(
        provider: AuthenticationProvider,
        onAuthenticated: (AuthenticatedUser) -> Unit
    ) {
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(activeProvider = provider, failureMessage = null) }
            try {
                onAuthenticated(repository.authenticate(provider))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(activeProvider = null) }
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

/** `@Observable final class SignUpViewModel`. */
class SignUpViewModel(
    private val repository: AuthenticationRepository
) : ViewModel() {

    /** `SignUpViewModel.Mode`. */
    enum class Mode { OPTIONS, EMAIL }

    data class UiState(
        val mode: Mode = Mode.OPTIONS,
        val email: String = "",
        val password: String = "",
        val passwordConfirmation: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val confirmationError: String? = null,
        val failureMessage: String? = null,
        val isSubmittingEmail: Boolean = false,
        val activeProvider: AuthenticationProvider? = null
    ) {
        val isBusy: Boolean get() = isSubmittingEmail || activeProvider != null
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun onPasswordConfirmationChange(value: String) =
        _uiState.update { it.copy(passwordConfirmation = value) }

    fun showEmailForm() =
        _uiState.update { it.copy(failureMessage = null, mode = Mode.EMAIL) }

    fun showOptions() =
        _uiState.update { it.copy(failureMessage = null, mode = Mode.OPTIONS) }

    fun createAccount(onAuthenticated: (AuthenticatedUser) -> Unit) {
        if (_uiState.value.isBusy || !validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingEmail = true, failureMessage = null) }
            try {
                val user = repository.createAccount(
                    email = AuthenticationValidation.normalizedEmail(_uiState.value.email),
                    password = _uiState.value.password
                )
                onAuthenticated(user)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isSubmittingEmail = false) }
            }
        }
    }

    fun authenticate(
        provider: AuthenticationProvider,
        onAuthenticated: (AuthenticatedUser) -> Unit
    ) {
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(activeProvider = provider, failureMessage = null) }
            try {
                onAuthenticated(repository.authenticate(provider))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(failureMessage = error.localizedMessage) }
            } finally {
                _uiState.update { it.copy(activeProvider = null) }
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

/** `@Observable final class PasswordResetViewModel`. */
class PasswordResetViewModel(
    private val repository: AuthenticationRepository,
    initialEmail: String
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val emailError: String? = null,
        val failureMessage: String? = null,
        val isSubmitting: Boolean = false,
        val didSend: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState(email = initialEmail))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun sendReset() {
        if (_uiState.value.isSubmitting) return

        val emailError = AuthenticationValidation.emailError(_uiState.value.email)
        _uiState.update { it.copy(emailError = emailError) }
        if (emailError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, failureMessage = null) }
            try {
                repository.requestPasswordReset(
                    AuthenticationValidation.normalizedEmail(_uiState.value.email)
                )
                _uiState.update { it.copy(didSend = true) }
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
