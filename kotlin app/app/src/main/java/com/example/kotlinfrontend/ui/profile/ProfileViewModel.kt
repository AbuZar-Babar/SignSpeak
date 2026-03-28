package com.example.kotlinfrontend.ui.profile

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

data class ProfileUiState(
    val sessionState: SessionState = SessionState.initializing(isConfigured = false),
    val bookmarks: List<DictionaryEntry> = emptyList(),
    val complaints: List<ComplaintRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val complaintRepository: ComplaintRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collectLatest { sessionState ->
                _uiState.update { current -> current.copy(sessionState = sessionState) }
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val user = uiState.value.sessionState.user ?: return@launch
            _uiState.update { current -> current.copy(isLoading = true) }

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
                    isLoading = false
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { current -> current.copy(message = null) }
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
}
