package com.example.kotlinfrontend.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinfrontend.data.local.AppLocalStore
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RootUiState(
    val sessionState: SessionState = SessionState.initializing(isConfigured = false),
    val hasSeenOnboarding: Boolean? = null
) {
    val isReady: Boolean
        get() = hasSeenOnboarding != null && sessionState.isInitialized

    val destination: RootDestination?
        get() = when {
            !isReady -> null
            sessionState.isAuthenticated -> RootDestination.Main
            hasSeenOnboarding == true -> RootDestination.Auth
            else -> RootDestination.Onboarding
        }
}

class RootViewModel(
    private val authRepository: AuthRepository,
    private val localStore: AppLocalStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collectLatest { sessionState ->
                _uiState.update { current ->
                    current.copy(sessionState = sessionState)
                }
            }
        }

        viewModelScope.launch {
            localStore.hasSeenOnboardingFlow.collectLatest { hasSeen ->
                _uiState.update { current ->
                    current.copy(hasSeenOnboarding = hasSeen)
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            localStore.markOnboardingSeen()
        }
    }
}
