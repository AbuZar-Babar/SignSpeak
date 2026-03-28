package com.example.kotlinfrontend.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kotlinfrontend.data.AppJson
import com.example.kotlinfrontend.data.model.TranslationHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException

private val Context.signSpeakDataStore by preferencesDataStore(name = "signspeak_v1")

class AppLocalStore(private val context: Context) {
    private object Keys {
        val guestHistory = stringPreferencesKey("guest_history")
        val hasSeenOnboarding = booleanPreferencesKey("has_seen_onboarding")
    }

    private val historySerializer = ListSerializer(TranslationHistoryItem.serializer())

    val guestHistoryFlow: Flow<List<TranslationHistoryItem>> = context.signSpeakDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            decodeHistory(preferences[Keys.guestHistory])
        }

    val hasSeenOnboardingFlow: Flow<Boolean> = context.signSpeakDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[Keys.hasSeenOnboarding] ?: false
        }

    suspend fun readGuestHistory(): List<TranslationHistoryItem> = guestHistoryFlow.first()

    suspend fun hasSeenOnboarding(): Boolean = hasSeenOnboardingFlow.first()

    suspend fun appendGuestHistory(item: TranslationHistoryItem, maxItems: Int = 100) {
        val updatedItems = buildList {
            add(item)
            addAll(readGuestHistory().filterNot { existing -> existing.id == item.id })
        }.sortedByDescending { history -> history.createdAt }
            .take(maxItems)

        context.signSpeakDataStore.edit { preferences ->
            preferences[Keys.guestHistory] = AppJson.instance.encodeToString(historySerializer, updatedItems)
        }
    }

    suspend fun findHistoryById(id: String): TranslationHistoryItem? {
        return readGuestHistory().firstOrNull { item -> item.id == id }
    }

    suspend fun markOnboardingSeen() {
        context.signSpeakDataStore.edit { preferences ->
            preferences[Keys.hasSeenOnboarding] = true
        }
    }

    private fun decodeHistory(serialized: String?): List<TranslationHistoryItem> {
        if (serialized.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            AppJson.instance.decodeFromString(historySerializer, serialized)
        }.getOrDefault(emptyList())
    }
}
