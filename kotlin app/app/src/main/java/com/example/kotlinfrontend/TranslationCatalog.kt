package com.example.kotlinfrontend

import com.example.kotlinfrontend.data.model.DictionaryEntry
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object TranslationCatalog {
    private val translations = ConcurrentHashMap<String, String>()

    @Volatile
    private var isReady = false

    fun initialize(entries: List<DictionaryEntry>) {
        if (entries.isEmpty()) {
            return
        }

        entries.forEach { entry ->
            register(entry.slug, entry.urduWord)
            register(entry.englishWord, entry.urduWord)
        }

        isReady = true
    }

    fun lookup(label: String): String? {
        if (label.isBlank() || label == "--") {
            return label
        }

        val candidates = buildList {
            add(normalize(label))
            add(label.lowercase(Locale.getDefault()).trim())
            add(label.lowercase(Locale.getDefault()).replace('_', ' ').replace('-', ' ').trim())
        }.distinct()

        candidates.forEach { candidate ->
            translations[candidate]?.let { return it }
        }

        return null
    }

    fun resetForTests() {
        translations.clear()
        isReady = false
    }

    fun isReady(): Boolean = isReady

    private fun register(source: String, urduWord: String) {
        val normalized = normalize(source)
        if (normalized.isNotBlank()) {
            translations[normalized] = urduWord
        }

        val compactVariant = source.lowercase(Locale.getDefault()).trim()
        if (compactVariant.isNotBlank()) {
            translations[compactVariant] = urduWord
        }

        val spacedVariant = source
            .lowercase(Locale.getDefault())
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        if (spacedVariant.isNotBlank()) {
            translations[spacedVariant] = urduWord
        }
    }

    private fun normalize(raw: String): String {
        return raw
            .lowercase(Locale.getDefault())
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}