package com.example.kotlinfrontend.ui.onboarding

import androidx.compose.ui.graphics.Color
import com.example.kotlinfrontend.ui.components.IllustrationVariant
import com.example.kotlinfrontend.ui.theme.BrandAccent
import com.example.kotlinfrontend.ui.theme.BrandCardPeach
import com.example.kotlinfrontend.ui.theme.BrandPrimary

data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val variant: IllustrationVariant
)

val defaultOnboardingPages = listOf(
    OnboardingPage(
        eyebrow = "Translate live",
        title = "Turn sign language into readable words in real time.",
        subtitle = "Use the camera to capture PSL gestures and keep recent confirmed predictions in one place.",
        accentColor = BrandPrimary,
        variant = IllustrationVariant.Translate
    ),
    OnboardingPage(
        eyebrow = "Study smarter",
        title = "Browse a polished PSL dictionary whenever you need to double-check a sign.",
        subtitle = "Search English or Urdu, save important words, and jump into PSL references fast.",
        accentColor = BrandAccent,
        variant = IllustrationVariant.Dictionary
    ),
    OnboardingPage(
        eyebrow = "Improve quality",
        title = "Flag wrong predictions and dictionary issues so the system keeps getting better.",
        subtitle = "Your reports help reviewers clean up entries, retrain weak words, and fix mistakes faster.",
        accentColor = BrandCardPeach,
        variant = IllustrationVariant.Feedback
    )
)
