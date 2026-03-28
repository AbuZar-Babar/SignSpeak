package com.example.kotlinfrontend

import kotlin.math.max

data class PredictionDisplayState(
    val label: String,
    val confidence: Float,
    val stability: Float,
    val transcript: String,
    val lastCommittedLabel: String?,
    val commitCount: Int
)

class PredictionAccumulator(
    private val smoothingWindowSize: Int = 8
) {
    private val predictionHistory = ArrayDeque<PredictionResult>(smoothingWindowSize)
    private val transcriptTokens = mutableListOf<String>()

    private var displayedLabel: String = "--"
    private var displayedConfidence = 0f
    private var displayedStability = 0f

    private var lastCommittedLabel: String? = null
    private var lastCommitTimestampMs = -1L
    private var commitCount = 0

    @Synchronized
    fun reset(clearTranscript: Boolean = false) {
        predictionHistory.clear()
        displayedLabel = "--"
        displayedConfidence = 0f
        displayedStability = 0f
        if (clearTranscript) {
            clearTranscript()
        }
    }

    @Synchronized
    fun clearTranscript() {
        transcriptTokens.clear()
        lastCommittedLabel = null
        lastCommitTimestampMs = -1L
        commitCount = 0
    }

    @Synchronized
    fun applyPrediction(prediction: PredictionResult, timestampMs: Long): PredictionDisplayState {
        if (predictionHistory.size == smoothingWindowSize) {
            predictionHistory.removeFirst()
        }
        predictionHistory.addLast(prediction)

        val aggregated = linkedMapOf<String, Pair<Int, Float>>()
        predictionHistory.forEach { item ->
            val existing = aggregated[item.label]
            if (existing == null) {
                aggregated[item.label] = 1 to item.confidence
            } else {
                aggregated[item.label] = (existing.first + 1) to (existing.second + item.confidence)
            }
        }

        if (aggregated.isEmpty()) {
            return snapshot()
        }

        val historyCount = predictionHistory.size.toFloat()
        val totalConfidence = max(
            0.0001f,
            aggregated.values.sumOf { it.second.toDouble() }.toFloat()
        )

        val best = aggregated.maxByOrNull { (_, value) ->
            val count = value.first.toFloat()
            val confidenceSum = value.second
            confidenceSum + (count * 0.15f)
        } ?: return snapshot()

        val candidateLabel = best.key
        val candidateCount = best.value.first
        val candidateConfidenceSum = best.value.second
        val candidateAverageConfidence = candidateConfidenceSum / candidateCount.toFloat()
        val candidateRelativeWeight = candidateConfidenceSum / totalConfidence
        val candidateStability = candidateCount / historyCount
        val candidateDisplayConfidence = (
            (candidateAverageConfidence * 0.7f) + (candidateRelativeWeight * 0.3f)
            ).coerceIn(0f, 1f)

        val shouldSwitch = displayedLabel == "--" ||
            displayedLabel == candidateLabel ||
            (
                candidateStability >= 0.60f &&
                    candidateDisplayConfidence >= 0.55f &&
                    candidateCount >= 3
                )

        if (shouldSwitch) {
            displayedLabel = candidateLabel
            displayedConfidence = candidateDisplayConfidence
            displayedStability = candidateStability.coerceIn(0f, 1f)
        } else {
            displayedConfidence = ((displayedConfidence * 0.8f) + (candidateDisplayConfidence * 0.2f))
                .coerceIn(0f, 1f)
        }

        maybeCommitToken(timestampMs)
        return snapshot()
    }

    @Synchronized
    fun snapshot(): PredictionDisplayState {
        return PredictionDisplayState(
            label = displayedLabel,
            confidence = displayedConfidence,
            stability = displayedStability,
            transcript = transcriptTokens.joinToString(" "),
            lastCommittedLabel = lastCommittedLabel,
            commitCount = commitCount
        )
    }

    private fun maybeCommitToken(timestampMs: Long) {
        if (displayedLabel == "--") {
            return
        }
        if (displayedStability < 0.72f || displayedConfidence < 0.58f) {
            return
        }

        val isNewToken = displayedLabel != lastCommittedLabel
        val enoughTimeElapsed = lastCommitTimestampMs < 0L || (timestampMs - lastCommitTimestampMs) >= 900L

        if (isNewToken && enoughTimeElapsed) {
            val token = displayedLabel.replace('_', ' ')
            transcriptTokens.add(token)
            if (transcriptTokens.size > 12) {
                transcriptTokens.removeAt(0)
            }
            lastCommittedLabel = displayedLabel
            lastCommitTimestampMs = timestampMs
            commitCount += 1
        }
    }
}
