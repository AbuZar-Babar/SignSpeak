package com.example.kotlinfrontend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiSentenceFormerTest {
    @Test
    fun promptWithoutEmotionIncludesWordsAndLanguageOnly() {
        val prompt = GeminiSentencePromptBuilder.build(
            cleanedWords = listOf("I", "go", "home"),
            language = SentenceLanguage.ENGLISH,
            emotionContext = null
        )

        assertTrue(prompt.contains("Detected words (in order): I, go, home"))
        assertTrue(prompt.contains("Respond with ONLY a single natural English sentence."))
        assertFalse(prompt.contains("Facial expression context"))
    }

    @Test
    fun promptWithValidEmotionIncludesToneOnlyGuardrails() {
        val emotionContext = SentenceEmotionContext.fromFaceExpression(
            FaceExpressionSignal(
                label = "Smile",
                confidence = 0.72f,
                topScores = emptyList()
            )
        )

        assertNotNull(emotionContext)
        assertEquals("happy/warm", emotionContext?.toneLabel)

        val prompt = GeminiSentencePromptBuilder.build(
            cleanedWords = listOf("hello", "friend"),
            language = SentenceLanguage.BOTH,
            emotionContext = emotionContext
        )

        assertTrue(prompt.contains("Facial expression context (tone only): happy/warm tone"))
        assertTrue(prompt.contains("Use this only to choose the sentence tone."))
        assertTrue(prompt.contains("Do not state, invent, or add feelings/emotions"))
        assertTrue(prompt.contains("Do not change the meaning of the detected words"))
    }

    @Test
    fun weakNeutralAndMouthOnlyCuesAreOmitted() {
        assertNull(
            SentenceEmotionContext.fromFaceExpression(
                FaceExpressionSignal(
                    label = "Smile",
                    confidence = 0.34f,
                    topScores = emptyList()
                )
            )
        )
        assertNull(
            SentenceEmotionContext.fromFaceExpression(
                FaceExpressionSignal(
                    label = "Neutral",
                    confidence = 1f,
                    topScores = emptyList()
                )
            )
        )
        assertNull(
            SentenceEmotionContext.fromFaceExpression(
                FaceExpressionSignal(
                    label = "Mouth open",
                    confidence = 0.95f,
                    topScores = emptyList()
                )
            )
        )
        assertNull(
            SentenceEmotionContext.fromFaceExpression(
                FaceExpressionSignal(
                    label = "Jaw open",
                    confidence = 0.95f,
                    topScores = emptyList()
                )
            )
        )
    }

    @Test
    fun emotionLabelsMapToSafeToneLabels() {
        assertEquals("sad/concerned", toneFor("Sad"))
        assertEquals("surprised/emphatic", toneFor("Amazed"))
        assertEquals("serious/concerned", toneFor("Brows lowered"))
    }

    private fun toneFor(label: String): String? {
        return SentenceEmotionContext.fromFaceExpression(
            FaceExpressionSignal(
                label = label,
                confidence = 0.8f,
                topScores = emptyList()
            )
        )?.toneLabel
    }
}
