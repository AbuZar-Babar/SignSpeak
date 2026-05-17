package com.example.kotlinfrontend

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

enum class SentenceLanguage(val displayName: String) {
    URDU("اردو"),
    ENGLISH("English"),
    BOTH("Both / دونوں")
}

data class SentenceResult(
    val rawWords: List<String>,
    val formedSentence: String,
    val language: SentenceLanguage,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

class GeminiSentenceFormer(apiKey: String) {

    private val normalizedApiKey = apiKey.trim()
    private val apiKeyDiagnostic = if (normalizedApiKey.isBlank()) {
        "length=0"
    } else {
        "length=${normalizedApiKey.length}, prefix=${normalizedApiKey.take(6)}"
    }
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val modelName = "gemini-2.5-flash-lite"

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

        if (normalizedApiKey.isBlank()) {
            return SentenceResult(
                rawWords = cleanedWords,
                formedSentence = cleanedWords.joinToString(" "),
                language = language,
                isError = true,
                errorMessage = "Gemini API key is missing in this build ($apiKeyDiagnostic)."
            )
        }

        return try {
            val text = withContext(Dispatchers.IO) {
                generateContent(prompt)
            }.trim()

            if (text.isBlank()) {
                SentenceResult(
                    rawWords = cleanedWords,
                    formedSentence = cleanedWords.joinToString(" "),
                    language = language,
                    isError = true,
                    errorMessage = "Gemini returned an empty response."
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
                isError = true,
                errorMessage = "${e::class.java.simpleName}: ${e.message.orEmpty()}"
            )
        }
    }

    private fun generateContent(prompt: String): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "$modelName:generateContent?key=$normalizedApiKey"
        val payload = JSONObject()
            .put(
                "contents",
                org.json.JSONArray()
                    .put(
                        JSONObject()
                            .put(
                                "parts",
                                org.json.JSONArray()
                                    .put(JSONObject().put("text", prompt))
                            )
                    )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    .put("maxOutputTokens", 120)
            )
            .toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Gemini HTTP ${response.code}: ${extractErrorMessage(body)}")
            }

            return JSONObject(body)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
        }
    }

    private fun extractErrorMessage(body: String): String {
        return runCatching {
            JSONObject(body)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: body.take(240)
        }.getOrDefault(body.take(240))
    }
}
