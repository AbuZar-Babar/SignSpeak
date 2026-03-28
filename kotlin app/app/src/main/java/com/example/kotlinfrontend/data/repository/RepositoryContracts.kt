package com.example.kotlinfrontend.data.repository

import com.example.kotlinfrontend.data.model.ComplaintRecord
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.model.TranslationHistoryItem
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun signOut(): Result<Unit>
}

interface DictionaryRepository {
    suspend fun search(query: String, category: String?, limit: Int, offset: Int): List<DictionaryEntry>

    suspend fun getEntry(slug: String): DictionaryEntry?

    suspend fun getEntryById(id: String): DictionaryEntry?

    suspend fun getCategories(): List<String>

    suspend fun getBookmarkedEntries(userId: String): List<DictionaryEntry>
}

interface BookmarkRepository {
    suspend fun getBookmarkedIds(userId: String): Set<String>

    suspend fun toggleBookmark(userId: String, entryId: String): Result<Boolean>
}

interface HistoryRepository {
    suspend fun savePrediction(
        predictedWord: String,
        confidence: Double,
        modelVersion: String,
        createdAt: String
    ): TranslationHistoryItem

    suspend fun listRecent(limit: Int, offset: Int): List<TranslationHistoryItem>
}

interface ComplaintRepository {
    suspend fun submitFromPrediction(
        historyId: String,
        expectedWord: String,
        note: String
    ): Result<Unit>

    suspend fun submitFromDictionary(entryId: String, note: String): Result<Unit>

    suspend fun listMine(limit: Int, offset: Int): List<ComplaintRecord>
}
