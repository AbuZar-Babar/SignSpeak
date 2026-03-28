package com.example.kotlinfrontend.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinfrontend.SignModel
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.model.TranslationHistoryItem
import com.example.kotlinfrontend.data.repository.AuthRepository
import com.example.kotlinfrontend.data.repository.ComplaintRepository
import com.example.kotlinfrontend.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PredictionReportDraft(
    val historyId: String,
    val reportedWord: String,
    val confidence: Float,
    val modelName: String
)

data class TranslateUiState(
    val sessionState: SessionState = SessionState.initializing(isConfigured = false),
    val recentHistory: List<TranslationHistoryItem> = emptyList(),
    val pendingReport: PredictionReportDraft? = null,
    val expectedWord: String = "",
    val reportNote: String = "",
    val isSubmittingReport: Boolean = false,
    val isLoadingHistory: Boolean = true,
    val message: String? = null
)

class TranslateViewModel(
    private val authRepository: AuthRepository,
    private val historyRepository: HistoryRepository,
    private val complaintRepository: ComplaintRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collectLatest { sessionState ->
                _uiState.update { current ->
                    current.copy(sessionState = sessionState)
                }
                refreshHistory()
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _uiState.update { current -> current.copy(isLoadingHistory = true) }
            val history = runCatching {
                historyRepository.listRecent(limit = 8, offset = 0)
            }.getOrElse { error ->
                _uiState.update { current ->
                    current.copy(
                        isLoadingHistory = false,
                        message = error.message ?: "Unable to load recent words."
                    )
                }
                return@launch
            }

            _uiState.update { current ->
                current.copy(
                    recentHistory = history,
                    isLoadingHistory = false
                )
            }
        }
    }

    fun onWordCommitted(word: String, confidence: Float, model: SignModel, createdAt: String) {
        if (word.isBlank() || word == "--") {
            return
        }

        viewModelScope.launch {
            historyRepository.savePrediction(
                predictedWord = word,
                confidence = confidence.toDouble(),
                modelVersion = model.backendKey,
                createdAt = createdAt
            )
            refreshHistory()
        }
    }

    fun openPredictionReport(word: String, confidence: Float, model: SignModel) {
        val matchingHistory = uiState.value.recentHistory.firstOrNull { history ->
            history.predictedWordSlug.equals(word, ignoreCase = true)
        } ?: uiState.value.recentHistory.firstOrNull()

        if (matchingHistory == null) {
            _uiState.update { current ->
                current.copy(message = "Wait for a confirmed word before sending a report.")
            }
            return
        }

        _uiState.update { current ->
            current.copy(
                pendingReport = PredictionReportDraft(
                    historyId = matchingHistory.id,
                    reportedWord = matchingHistory.predictedWordSlug,
                    confidence = confidence,
                    modelName = model.displayName
                ),
                expectedWord = "",
                reportNote = "",
                message = null
            )
        }
    }

    fun updateExpectedWord(value: String) {
        _uiState.update { current -> current.copy(expectedWord = value) }
    }

    fun updateReportNote(value: String) {
        _uiState.update { current -> current.copy(reportNote = value) }
    }

    fun dismissReport() {
        _uiState.update { current ->
            current.copy(
                pendingReport = null,
                expectedWord = "",
                reportNote = "",
                isSubmittingReport = false
            )
        }
    }

    fun dismissMessage() {
        _uiState.update { current -> current.copy(message = null) }
    }

    fun submitReport() {
        val draft = uiState.value.pendingReport ?: return

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isSubmittingReport = true) }
            val result = complaintRepository.submitFromPrediction(
                historyId = draft.historyId,
                expectedWord = uiState.value.expectedWord,
                note = uiState.value.reportNote
            )

            _uiState.update { current ->
                current.copy(
                    isSubmittingReport = false,
                    pendingReport = if (result.isSuccess) null else current.pendingReport,
                    expectedWord = if (result.isSuccess) "" else current.expectedWord,
                    reportNote = if (result.isSuccess) "" else current.reportNote,
                    message = result.fold(
                        onSuccess = { "Prediction report submitted." },
                        onFailure = { it.message ?: "Unable to submit the prediction report." }
                    )
                )
            }
        }
    }
}
