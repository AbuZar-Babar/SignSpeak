package com.example.kotlinfrontend.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.model.TranslationHistoryItem
import com.example.kotlinfrontend.data.repository.AuthRepository
import com.example.kotlinfrontend.data.repository.DictionaryRepository
import com.example.kotlinfrontend.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessionState: SessionState = SessionState.initializing(isConfigured = false),
    val items: List<TranslationHistoryItem> = emptyList(),
    val translationLookup: Map<String, DictionaryEntry> = emptyMap(),
    val isLoading: Boolean = true,
    val message: String? = null
)

class HistoryViewModel(
    private val authRepository: AuthRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

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
            _uiState.update { current -> current.copy(isLoading = true) }
            val items = runCatching {
                historyRepository.listRecent(limit = 50, offset = 0)
            }.getOrElse { error ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        message = error.message ?: "Unable to load translation history."
                    )
                }
                return@launch
            }

            val translations = items
                .map { it.predictedWordSlug }
                .distinct()
                .associateWith { slug ->
                    runCatching { dictionaryRepository.getEntry(slug) }.getOrNull()
                }
                .mapNotNull { (slug, entry) -> entry?.let { slug to it } }
                .toMap()

            _uiState.update { current ->
                current.copy(
                    items = items,
                    translationLookup = translations,
                    isLoading = false
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { current -> current.copy(message = null) }
    }
}
