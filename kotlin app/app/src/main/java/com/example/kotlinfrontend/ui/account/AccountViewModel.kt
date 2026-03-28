package com.example.kotlinfrontend.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinfrontend.data.model.ComplaintRecord
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.repository.AuthRepository
import com.example.kotlinfrontend.data.repository.ComplaintRepository
import com.example.kotlinfrontend.data.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountAuthMode {
    LOGIN,
    REGISTER,
    RESET
}

data class AccountUiState(
    val sessionState: SessionState = SessionState.initializing(isConfigured = false),
    val authMode: AccountAuthMode = AccountAuthMode.LOGIN,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val isWorking: Boolean = false,
    val isLoadingData: Boolean = false,
    val bookmarks: List<DictionaryEntry> = emptyList(),
    val complaints: List<ComplaintRecord> = emptyList(),
    val message: String? = null
)

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val complaintRepository: ComplaintRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collectLatest { sessionState ->
                _uiState.update { current ->
                    current.copy(sessionState = sessionState)
                }
                loadUserData()
            }
        }
    }

    fun setAuthMode(value: AccountAuthMode) {
        _uiState.update { current ->
            current.copy(
                authMode = value,
                message = null
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

    fun dismissMessage() {
        _uiState.update { current -> current.copy(message = null) }
    }

    fun submitAuth() {
        viewModelScope.launch {
            val state = uiState.value
            val validationError = when (state.authMode) {
                AccountAuthMode.LOGIN -> {
                    when {
                        state.email.isBlank() -> "Email is required."
                        state.password.isBlank() -> "Password is required."
                        else -> null
                    }
                }
                AccountAuthMode.REGISTER -> {
                    when {
                        state.fullName.isBlank() -> "Full name is required."
                        state.email.isBlank() -> "Email is required."
                        state.password.length < 6 -> "Use a password with at least 6 characters."
                        else -> null
                    }
                }
                AccountAuthMode.RESET -> {
                    if (state.email.isBlank()) {
                        "Email is required."
                    } else {
                        null
                    }
                }
            }

            if (validationError != null) {
                _uiState.update { current -> current.copy(message = validationError) }
                return@launch
            }

            _uiState.update { current -> current.copy(isWorking = true) }
            val result = when (state.authMode) {
                AccountAuthMode.LOGIN -> authRepository.signIn(state.email, state.password)
                AccountAuthMode.REGISTER -> authRepository.signUp(
                    email = state.email,
                    password = state.password,
                    fullName = state.fullName
                )
                AccountAuthMode.RESET -> authRepository.sendPasswordReset(state.email)
            }

            _uiState.update { current ->
                current.copy(
                    isWorking = false,
                    password = if (result.isSuccess) "" else current.password,
                    message = result.fold(
                        onSuccess = {
                            when (state.authMode) {
                                AccountAuthMode.LOGIN -> "Signed in successfully."
                                AccountAuthMode.REGISTER -> {
                                    "Account created. If email confirmation is enabled, check your inbox."
                                }
                                AccountAuthMode.RESET -> "Password reset email sent."
                            }
                        },
                        onFailure = { it.message ?: "Authentication request failed." }
                    )
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { current -> current.copy(isWorking = true) }
            val result = authRepository.signOut()
            _uiState.update { current ->
                current.copy(
                    isWorking = false,
                    message = result.fold(
                        onSuccess = { "Signed out." },
                        onFailure = { it.message ?: "Unable to sign out." }
                    )
                )
            }
        }
    }

    fun refresh() {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val user = uiState.value.sessionState.user
            if (user == null) {
                _uiState.update { current ->
                    current.copy(
                        isLoadingData = false,
                        bookmarks = emptyList(),
                        complaints = emptyList()
                    )
                }
                return@launch
            }

            _uiState.update { current -> current.copy(isLoadingData = true) }

            val bookmarks = runCatching {
                dictionaryRepository.getBookmarkedEntries(user.id)
            }.getOrDefault(emptyList())

            val complaints = runCatching {
                complaintRepository.listMine(limit = 25, offset = 0)
            }.getOrDefault(emptyList())

            _uiState.update { current ->
                current.copy(
                    bookmarks = bookmarks,
                    complaints = complaints,
                    isLoadingData = false
                )
            }
        }
    }
}
