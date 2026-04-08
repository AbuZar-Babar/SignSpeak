package com.example.kotlinfrontend

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SentenceLanguage(val displayName: String) {
    URDU("اردو"),
    ENGLISH("English"),
    BOTH("Both / دونوں")
}

data class SentenceResult(
    val rawWords: List<String>,
    val formedSentence: String,
    val language: SentenceLanguage,
    val isError: Boolean = false
)

class GeminiSentenceFormer(apiKey: String) {

    private val normalizedApiKey = apiKey.trim()
    private val model: GenerativeModel? by lazy(LazyThreadSafetyMode.NONE) {
        if (normalizedApiKey.isBlank()) {
            null
        } else {
            try {
                GenerativeModel(
                    modelName = "gemini-2.0-flash",
                    apiKey = normalizedApiKey
                )
            } catch (error: Throwable) {
                Log.e("GeminiSentenceFormer", "Failed to initialize Gemini model", error)
                null
            }
        }
    }

    suspend fun formSentence(
        words: List<String>,
        language: SentenceLanguage
    ): SentenceResult {
        if (words.isEmpty()) {
            return SentenceResult(
                rawWords = words,
                formedSentence = "",
                language = language
            )
        }

        val cleanedWords = words.map { word ->
            word.replace('_', ' ').trim()
        }.filter { it.isNotBlank() }

        if (cleanedWords.isEmpty()) {
            return SentenceResult(
                rawWords = words,
                formedSentence = "",
                language = language
            )
        }

        val languageInstruction = when (language) {
            SentenceLanguage.URDU -> """
                Respond with ONLY a single natural Urdu sentence written in Urdu script.
                Do not include any English text, transliteration, or explanation.
            """.trimIndent()

            SentenceLanguage.ENGLISH -> """
                Respond with ONLY a single natural English sentence.
                Do not include any explanation or extra text.
            """.trimIndent()

            SentenceLanguage.BOTH -> """
                Respond with exactly two lines:
                Line 1: A natural Urdu sentence written in Urdu script.
                Line 2: The same sentence in English.
                Do not include any labels, explanation, or extra text.
            """.trimIndent()
        }

        val prompt = """
            You are a sign language interpreter assistant for Pakistani Sign Language (PSL).
            You will receive a sequence of isolated words detected from sign language gestures.

            Your task: Form a single natural, grammatically correct sentence using these words.
            - You may add small connector words (prepositions, articles, conjunctions, verbs) to make the sentence natural.
            - Do NOT invent completely new concepts or add words that change the meaning.
            - Keep the sentence concise and conversational.
            - If the words are greetings or phrases, combine them naturally.

            Detected words (in order): ${cleanedWords.joinToString(", ")}

            $languageInstruction
        """.trimIndent()

        val activeModel = model
        if (activeModel == null) {
            return SentenceResult(
                rawWords = cleanedWords,
                formedSentence = cleanedWords.joinToString(" "),
                language = language,
                isError = true
            )
        }

        return try {
            val response = withContext(Dispatchers.IO) {
                activeModel.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()

            if (text.isBlank()) {
                SentenceResult(
                    rawWords = cleanedWords,
                    formedSentence = cleanedWords.joinToString(" "),
                    language = language,
                    isError = true
                )
            } else {
                SentenceResult(
                    rawWords = cleanedWords,
                    formedSentence = text,
                    language = language
                )
            }
        } catch (e: Throwable) {
            Log.e("GeminiSentenceFormer", "Failed to form sentence", e)
            SentenceResult(
                rawWords = cleanedWords,
                formedSentence = cleanedWords.joinToString(" "),
                language = language,
                isError = true
            )
        }
    }
}
