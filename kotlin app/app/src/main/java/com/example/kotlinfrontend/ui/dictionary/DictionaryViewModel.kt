package com.example.kotlinfrontend.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.repository.AuthRepository
import com.example.kotlinfrontend.data.repository.BookmarkRepository
import com.example.kotlinfrontend.data.repository.ComplaintRepository
import com.example.kotlinfrontend.data.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictionaryUiState(
    val sessionState: SessionState = SessionState.initializing(isConfigured = false),
    val query: String = "",
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val entries: List<DictionaryEntry> = emptyList(),
    val bookmarkedIds: Set<String> = emptySet(),
    val selectedEntry: DictionaryEntry? = null,
    val reportEntry: DictionaryEntry? = null,
    val reportNote: String = "",
    val isLoading: Boolean = true,
    val isSubmittingReport: Boolean = false,
    val message: String? = null
)

class DictionaryViewModel(
    private val authRepository: AuthRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val complaintRepository: ComplaintRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadCategories()
            refreshEntries()
        }

        viewModelScope.launch {
            authRepository.sessionState.collectLatest { sessionState ->
                _uiState.update { current ->
                    current.copy(sessionState = sessionState)
                }
                refreshBookmarks()
            }
        }
    }

    fun updateQuery(value: String) {
        _uiState.update { current -> current.copy(query = value) }
        refreshEntries()
    }

    fun selectCategory(value: String) {
        _uiState.update { current -> current.copy(selectedCategory = value) }
        refreshEntries()
    }

    fun refreshEntries() {
        viewModelScope.launch {
            _uiState.update { current -> current.copy(isLoading = true) }
            val entries = runCatching {
                dictionaryRepository.search(
                    query = uiState.value.query,
                    category = uiState.value.selectedCategory,
                    limit = 250,
                    offset = 0
                )
            }.getOrElse { error ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        message = error.message ?: "Unable to load the dictionary."
                    )
                }
                return@launch
            }

            _uiState.update { current ->
                current.copy(
                    entries = entries,
                    isLoading = false
                )
            }
        }
    }

    fun openEntry(entry: DictionaryEntry) {
        _uiState.update { current -> current.copy(selectedEntry = entry) }
    }

    fun dismissEntry() {
        _uiState.update { current -> current.copy(selectedEntry = null) }
    }

    fun openReport(entry: DictionaryEntry) {
        _uiState.update { current ->
            current.copy(
                reportEntry = entry,
                reportNote = "",
                message = null
            )
        }
    }

    fun dismissReport() {
        _uiState.update { current ->
            current.copy(
                reportEntry = null,
                reportNote = "",
                isSubmittingReport = false
            )
        }
    }

    fun updateReportNote(value: String) {
        _uiState.update { current -> current.copy(reportNote = value) }
    }

    fun toggleBookmark(entryId: String) {
        val userId = uiState.value.sessionState.user?.id
        if (userId == null) {
            _uiState.update { current -> current.copy(message = "Sign in to bookmark words.") }
            return
        }

        viewModelScope.launch {
            val result = bookmarkRepository.toggleBookmark(userId = userId, entryId = entryId)
            refreshBookmarks()
            _uiState.update { current ->
                current.copy(
                    message = result.fold(
                        onSuccess = { bookmarked ->
                            if (bookmarked) {
                                "Word saved to bookmarks."
                            } else {
                                "Bookmark removed."
                            }
                        },
                        onFailure = { it.message ?: "Unable to update bookmark." }
                    )
                )
            }
        }
    }

    fun submitReport() {
        val entry = uiState.value.reportEntry ?: return

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isSubmittingReport = true) }
            val result = complaintRepository.submitFromDictionary(
                entryId = entry.id,
                note = uiState.value.reportNote
            )

            _uiState.update { current ->
                current.copy(
                    isSubmittingReport = false,
                    reportEntry = if (result.isSuccess) null else current.reportEntry,
                    reportNote = if (result.isSuccess) "" else current.reportNote,
                    message = result.fold(
                        onSuccess = { "Dictionary report submitted." },
                        onFailure = { it.message ?: "Unable to submit the dictionary report." }
                    )
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { current -> current.copy(message = null) }
    }

    private suspend fun loadCategories() {
        val categories = runCatching {
            dictionaryRepository.getCategories()
        }.getOrElse {
            listOf("All")
        }
        _uiState.update { current -> current.copy(categories = categories) }
    }

    private fun refreshBookmarks() {
        viewModelScope.launch {
            val userId = uiState.value.sessionState.user?.id
            if (userId == null) {
                _uiState.update { current -> current.copy(bookmarkedIds = emptySet()) }
                return@launch
            }

            val bookmarkedIds = runCatching {
                bookmarkRepository.getBookmarkedIds(userId)
            }.getOrDefault(emptySet())

            _uiState.update { current -> current.copy(bookmarkedIds = bookmarkedIds) }
        }
    }
}
