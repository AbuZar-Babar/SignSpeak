package com.example.kotlinfrontend

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.backendSettingsDataStore by preferencesDataStore(name = "backend_settings")

class BackendSettingsRepository(private val context: Context) {
    private object Keys {
        val backendBaseUrl = stringPreferencesKey("backend_base_url")
        val sentenceModeEnabled = booleanPreferencesKey("sentence_mode_enabled")
        val sentenceLanguage = stringPreferencesKey("sentence_language")
    }

    val backendBaseUrlFlow: Flow<String> = context.backendSettingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[Keys.backendBaseUrl].orEmpty()
        }

    val sentenceModeEnabledFlow: Flow<Boolean> = context.backendSettingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[Keys.sentenceModeEnabled] ?: false
        }

    val sentenceLanguageFlow: Flow<SentenceLanguage> = context.backendSettingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            val stored = preferences[Keys.sentenceLanguage] ?: SentenceLanguage.BOTH.name
            try {
                SentenceLanguage.valueOf(stored)
            } catch (_: Exception) {
                SentenceLanguage.BOTH
            }
        }

    suspend fun saveBackendBaseUrl(baseUrl: String) {
        context.backendSettingsDataStore.edit { preferences ->
            if (baseUrl.isBlank()) {
                preferences.remove(Keys.backendBaseUrl)
            } else {
                preferences[Keys.backendBaseUrl] = baseUrl
            }
        }
    }

    suspend fun saveSentenceModeEnabled(enabled: Boolean) {
        context.backendSettingsDataStore.edit { preferences ->
            preferences[Keys.sentenceModeEnabled] = enabled
        }
    }

    suspend fun saveSentenceLanguage(language: SentenceLanguage) {
        context.backendSettingsDataStore.edit { preferences ->
            preferences[Keys.sentenceLanguage] = language.name
        }
    }
}
