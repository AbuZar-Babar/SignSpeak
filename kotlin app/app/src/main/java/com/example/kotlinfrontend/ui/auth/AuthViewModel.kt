package com.example.kotlinfrontend.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    Login,
    Register
}

data class AuthUiState(
    val sessionState: SessionState = SessionState.initializing(isConfigured = false),
    val mode: AuthMode = AuthMode.Login,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showResetSheet: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collectLatest { sessionState ->
                _uiState.update { current ->
                    current.copy(sessionState = sessionState)
                }
            }
        }
    }

    fun setMode(mode: AuthMode) {
        _uiState.update { current ->
            current.copy(
                mode = mode,
                message = null,
                isError = false
            )
        }
    }

    fun updateFullName(value: String) {
        _uiState.update { current -> current.copy(fullName = value) }
    }

    fun updateEmail(value: String) {
        _uiState.update { current -> current.copy(email = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { current -> current.copy(password = value) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { current -> current.copy(confirmPassword = value) }
    }

    fun dismissMessage() {
        _uiState.update { current -> current.copy(message = null, isError = false) }
    }

    fun openResetSheet() {
        _uiState.update { current -> current.copy(showResetSheet = true, message = null, isError = false) }
    }

    fun closeResetSheet() {
        _uiState.update { current -> current.copy(showResetSheet = false) }
    }

    fun submit() {
        viewModelScope.launch {
            val state = uiState.value
            val validationError = when (state.mode) {
                AuthMode.Login -> when {
                    state.email.isBlank() -> "Enter your email address."
                    state.password.isBlank() -> "Enter your password."
                    else -> null
                }
                AuthMode.Register -> when {
                    state.fullName.isBlank() -> "Enter your full name."
                    state.email.isBlank() -> "Enter your email address."
                    state.password.length < 6 -> "Use a password with at least 6 characters."
                    state.password != state.confirmPassword -> "Passwords do not match."
                    else -> null
                }
            }

            if (validationError != null) {
                _uiState.update { current ->
                    current.copy(message = validationError, isError = true)
                }
                return@launch
            }

            _uiState.update { current -> current.copy(isWorking = true, message = null, isError = false) }
            val result = when (state.mode) {
                AuthMode.Login -> authRepository.signIn(
                    email = state.email,
                    password = state.password
                )
                AuthMode.Register -> authRepository.signUp(
                    email = state.email,
                    password = state.password,
                    fullName = state.fullName
                )
            }

            _uiState.update { current ->
                current.copy(
                    isWorking = false,
                    password = if (result.isSuccess) "" else current.password,
                    confirmPassword = if (result.isSuccess) "" else current.confirmPassword,
                    message = result.fold(
                        onSuccess = {
                            when (state.mode) {
                                AuthMode.Login -> "Welcome back."
                                AuthMode.Register -> "Your account is ready."
                            }
                        },
                        onFailure = { it.message ?: "Authentication failed." }
                    ),
                    isError = result.isFailure
                )
            }
        }
    }

    fun sendPasswordReset() {
        viewModelScope.launch {
            val email = uiState.value.email.trim()
            if (email.isBlank()) {
                _uiState.update { current ->
                    current.copy(message = "Enter your email before requesting a reset link.", isError = true)
                }
                return@launch
            }

            _uiState.update { current -> current.copy(isWorking = true, message = null, isError = false) }
            val result = authRepository.sendPasswordReset(email)
            _uiState.update { current ->
                current.copy(
                    isWorking = false,
                    showResetSheet = if (result.isSuccess) false else current.showResetSheet,
                    message = result.fold(
                        onSuccess = { "Reset email sent." },
                        onFailure = { it.message ?: "Unable to send reset email." }
                    ),
                    isError = result.isFailure
                )
            }
        }
    }
}
