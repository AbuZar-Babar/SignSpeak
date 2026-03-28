package com.example.kotlinfrontend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DictionaryEntry(
    val id: String,
    val slug: String,
    @SerialName("english_word")
    val englishWord: String,
    @SerialName("urdu_word")
    val urduWord: String,
    val category: String,
    @SerialName("external_url")
    val externalUrl: String? = null,
    @SerialName("review_status")
    val reviewStatus: String = "verified",
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class BookmarkRow(
    @SerialName("user_id")
    val userId: String,
    @SerialName("dictionary_entry_id")
    val dictionaryEntryId: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class TranslationHistoryItem(
    val id: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("predicted_word_slug")
    val predictedWordSlug: String,
    val confidence: Double,
    @SerialName("model_version")
    val modelVersion: String,
    val source: String = "on_device",
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class ComplaintRecord(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("source_type")
    val sourceType: String,
    @SerialName("translation_history_id")
    val translationHistoryId: String? = null,
    @SerialName("dictionary_entry_id")
    val dictionaryEntryId: String? = null,
    @SerialName("reported_word_slug")
    val reportedWordSlug: String? = null,
    @SerialName("expected_word")
    val expectedWord: String? = null,
    val note: String? = null,
    val status: String = "open",
    @SerialName("admin_note")
    val adminNote: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("resolved_at")
    val resolvedAt: String? = null
)

@Serializable
data class ProfileRecord(
    val id: String,
    @SerialName("full_name")
    val fullName: String? = null,
    val role: String = "user",
    @SerialName("created_at")
    val createdAt: String? = null
)

data class SessionUser(
    val id: String,
    val email: String?,
    val fullName: String?,
    val role: String
)

data class SessionState(
    val isInitialized: Boolean,
    val isConfigured: Boolean,
    val isAuthenticated: Boolean,
    val user: SessionUser? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun initializing(isConfigured: Boolean): SessionState = SessionState(
            isInitialized = false,
            isConfigured = isConfigured,
            isAuthenticated = false
        )

        fun guest(isConfigured: Boolean, errorMessage: String? = null): SessionState = SessionState(
            isInitialized = true,
            isConfigured = isConfigured,
            isAuthenticated = false,
            errorMessage = errorMessage
        )
    }
}
