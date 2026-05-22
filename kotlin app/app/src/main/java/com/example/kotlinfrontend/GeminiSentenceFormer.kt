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
import java.util.Locale

enum class SentenceLanguage(val displayName: String) {
    URDU("اردو"),
    ENGLISH("English"),
    BOTH("Both / دونوں")
}

data class SentenceEmotionContext(
    val toneLabel: String,
    val sourceLabel: String,
    val confidence: Float
) {
    companion object {
        private const val MIN_CONFIDENCE = 0.35f
        private val HAPPY_LABELS = setOf("smile", "smile left", "smile right")
        private val SAD_LABELS = setOf("sad")
        private val SURPRISED_LABELS = setOf(
            "amazed",
            "brows raised",
            "inner brow raise",
            "outer brows raised",
            "eyes wide"
        )
        private val SERIOUS_LABELS = setOf(
            "frown",
            "brows lowered",
            "left brow lowered",
            "right brow lowered",
            "eyes squint",
            "nose sneer"
        )

        fun fromFaceExpression(signal: FaceExpressionSignal?): SentenceEmotionContext? {
            val faceSignal = signal ?: return null
            val sourceLabel = faceSignal.label.trim()
            if (sourceLabel.isBlank() || sourceLabel.equals("Neutral", ignoreCase = true)) {
                return null
            }

            val confidence = faceSignal.confidence.coerceIn(0f, 1f)
            if (confidence < MIN_CONFIDENCE) {
                return null
            }

            val normalizedLabel = sourceLabel.lowercase(Locale.US)
            val toneLabel = when (normalizedLabel) {
                in HAPPY_LABELS -> "happy/warm"
                in SAD_LABELS -> "sad/concerned"
                in SURPRISED_LABELS -> "surprised/emphatic"
                in SERIOUS_LABELS -> "serious/concerned"
                else -> null
            } ?: return null

            return SentenceEmotionContext(
                toneLabel = toneLabel,
                sourceLabel = sourceLabel,
                confidence = confidence
            )
        }
    }
}

data class SentenceResult(
    val rawWords: List<String>,
    val formedSentence: String,
    val language: SentenceLanguage,
    val emotionContext: SentenceEmotionContext? = null,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

internal object GeminiSentencePromptBuilder {
    fun cleanWords(words: List<String>): List<String> {
        return words.map { word ->
            word.replace('_', ' ').trim()
        }.filter { it.isNotBlank() }
    }

    fun build(
        cleanedWords: List<String>,
        language: SentenceLanguage,
        emotionContext: SentenceEmotionContext?
    ): String {
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

        val emotionSection = emotionContext?.let { context ->
            """

            Facial expression context (tone only): ${context.toneLabel} tone, detected from ${context.sourceLabel} at ${(context.confidence * 100f).toInt()}% confidence.
            - Use this only to choose the sentence tone.
            - Do not state, invent, or add feelings/emotions unless the signed words already imply them.
            - Do not change the meaning of the detected words because of the facial expression.
            """.trimIndent()
        }.orEmpty()

        return """
            You are a sign language interpreter assistant for Pakistani Sign Language (PSL).
            You will receive a sequence of isolated words detected from sign language gestures.

            Your task: Form a single natural, grammatically correct sentence using these words.
            - You may add small connector words (prepositions, articles, conjunctions, verbs) to make the sentence natural.
            - Do NOT invent completely new concepts or add words that change the meaning.
            - Keep the sentence concise and conversational.
            - If the words are greetings or phrases, combine them naturally.

            Detected words (in order): ${cleanedWords.joinToString(", ")}
            $emotionSection

            $languageInstruction
        """.trimIndent()
    }
}

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
        language: SentenceLanguage,
        emotionContext: SentenceEmotionContext? = null
    ): SentenceResult {
        if (words.isEmpty()) {
            return SentenceResult(
                rawWords = words,
                formedSentence = "",
                language = language
            )
        }

        val cleanedWords = GeminiSentencePromptBuilder.cleanWords(words)

        if (cleanedWords.isEmpty()) {
            return SentenceResult(
                rawWords = words,
                formedSentence = "",
                language = language
            )
        }

        val prompt = GeminiSentencePromptBuilder.build(
            cleanedWords = cleanedWords,
            language = language,
            emotionContext = emotionContext
        )

        if (normalizedApiKey.isBlank()) {
            return SentenceResult(
                rawWords = cleanedWords,
                formedSentence = cleanedWords.joinToString(" "),
                language = language,
                emotionContext = emotionContext,
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
                    emotionContext = emotionContext,
                    isError = true,
                    errorMessage = "Gemini returned an empty response."
                )
            } else {
                SentenceResult(
                    rawWords = cleanedWords,
                    formedSentence = text,
                    language = language,
                    emotionContext = emotionContext
                )
            }
        } catch (e: Throwable) {
            Log.e("GeminiSentenceFormer", "Failed to form sentence", e)
            SentenceResult(
                rawWords = cleanedWords,
                formedSentence = cleanedWords.joinToString(" "),
                language = language,
                emotionContext = emotionContext,
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
