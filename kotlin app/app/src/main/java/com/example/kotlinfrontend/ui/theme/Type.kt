package com.example.kotlinfrontend.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.kotlinfrontend.R

val EpilogueFamily = FontFamily(
    Font(R.font.epilogue_variable, FontWeight.Normal),
    Font(R.font.epilogue_variable, FontWeight.Medium),
    Font(R.font.epilogue_variable, FontWeight.SemiBold),
    Font(R.font.epilogue_variable, FontWeight.Bold),
    Font(R.font.epilogue_variable, FontWeight.ExtraBold)
)

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_variable, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_variable, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_variable, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_variable, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_variable, FontWeight.ExtraBold)
)

private val BaseTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = EpilogueFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = EpilogueFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = EpilogueFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EpilogueFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 31.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = EpilogueFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = EpilogueFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

val Typography = BaseTypography

private fun TextUnit.scaleIfSpecified(scale: Float): TextUnit =
    if (isSpecified) this * scale else this

private fun TextStyle.scaled(scale: Float): TextStyle = copy(
    fontSize = fontSize.scaleIfSpecified(scale),
    lineHeight = lineHeight.scaleIfSpecified(scale),
    letterSpacing = letterSpacing.scaleIfSpecified(scale)
)

private fun Typography.scaled(scale: Float): Typography = copy(
    displayLarge = displayLarge.scaled(scale),
    displayMedium = displayMedium.scaled(scale),
    displaySmall = displaySmall.scaled(scale),
    headlineLarge = headlineLarge.scaled(scale),
    headlineMedium = headlineMedium.scaled(scale),
    headlineSmall = headlineSmall.scaled(scale),
    titleLarge = titleLarge.scaled(scale),
    titleMedium = titleMedium.scaled(scale),
    titleSmall = titleSmall.scaled(scale),
    bodyLarge = bodyLarge.scaled(scale),
    bodyMedium = bodyMedium.scaled(scale),
    bodySmall = bodySmall.scaled(scale),
    labelLarge = labelLarge.scaled(scale),
    labelMedium = labelMedium.scaled(scale),
    labelSmall = labelSmall.scaled(scale)
)

fun responsiveTypography(screenWidthDp: Int): Typography {
    val scale = when {
        screenWidthDp >= 840 -> 1.14f
        screenWidthDp >= 600 -> 1.1f
        screenWidthDp >= 400 -> 1.04f
        else -> 1f
    }
    return BaseTypography.scaled(scale)
}

// --- Legacy aliases (for easy transition) ---
val LexendFontFamily = EpilogueFamily
val DMSansFontFamily = PlusJakartaSansFamily
val AppTypography = Typography
