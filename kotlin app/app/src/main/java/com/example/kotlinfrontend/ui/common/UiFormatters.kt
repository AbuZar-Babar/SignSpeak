package com.example.kotlinfrontend.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object UiFormatters {
    private val timestampFormatter = DateTimeFormatter.ofPattern(
        "dd MMM, hh:mm a",
        Locale.getDefault()
    )
    private val dayFormatter = DateTimeFormatter.ofPattern(
        "dd MMMM",
        Locale.getDefault()
    )

    fun prettyWord(raw: String): String {
        if (raw.isBlank()) {
            return "Unknown"
        }

        return raw
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { part ->
                when (part.lowercase(Locale.getDefault())) {
                    "atm" -> "ATM"
                    else -> part.replaceFirstChar { char ->
                        if (char.isLowerCase()) {
                            char.titlecase(Locale.getDefault())
                        } else {
                            char.toString()
                        }
                    }
                }
            }
    }

    fun confidencePercent(confidence: Float): String = "${(confidence * 100f).roundToInt()}%"

    fun confidencePercent(confidence: Double): String = "${(confidence * 100).roundToInt()}%"

    fun timestamp(isoValue: String): String {
        return runCatching {
            val instant = Instant.parse(isoValue)
            timestampFormatter.format(instant.atZone(ZoneId.systemDefault()))
        }.getOrDefault(isoValue)
    }

    fun timelineDayLabel(isoValue: String): String {
        return runCatching {
            val date = Instant.parse(isoValue)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            when (date) {
                LocalDate.now() -> "Today"
                LocalDate.now().minusDays(1) -> "Yesterday"
                else -> dayFormatter.format(date)
            }
        }.getOrDefault(isoValue)
    }

    fun reviewStatus(status: String): String {
        return when (status.lowercase(Locale.getDefault())) {
            "verified" -> "Verified"
            "needs_review" -> "Needs Review"
            else -> prettyWord(status)
        }
    }

    fun complaintStatus(status: String): String {
        return when (status.lowercase(Locale.getDefault())) {
            "open" -> "Open"
            "reviewing" -> "Reviewing"
            "resolved" -> "Resolved"
            "rejected" -> "Rejected"
            else -> prettyWord(status)
        }
    }

    fun modelVersion(modelVersion: String): String {
        return when (modelVersion.lowercase(Locale.getDefault())) {
            "augmented" -> "Augmented"
            "baseline" -> "Baseline"
            else -> prettyWord(modelVersion)
        }
    }
}
