package com.example.kotlinfrontend.ui.theme

import androidx.compose.ui.graphics.Color

// Living Bridge palette from the Stitch design system.
val Primary = Color(0xFF2B6C00)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF58CC02)
val OnPrimaryContainer = Color(0xFF1E5000)
val PrimaryFixed = Color(0xFF87FE45)
val OnPrimaryFixed = Color(0xFF082100)
val PrimaryFixedDim = Color(0xFF6BE026)

val Secondary = Color(0xFF705D00)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFFCD400)
val OnSecondaryContainer = Color(0xFF6E5C00)
val SecondaryFixed = Color(0xFFFFE16D)
val OnSecondaryFixed = Color(0xFF221B00)
val SecondaryFixedDim = Color(0xFFE9C400)

val Tertiary = Color(0xFF5F5E5E)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFB5B3B3)
val OnTertiaryContainer = Color(0xFF464545)

// Errors
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

val Background = Color(0xFFF6FBF1)
val OnBackground = Color(0xFF181D17)

val Surface = Color(0xFFF6FBF1)
val OnSurface = Color(0xFF181D17)
val SurfaceBright = Color(0xFFF6FBF1)
val SurfaceDim = Color(0xFFD6DCD2)
val SurfaceTint = Primary

val SurfaceVariant = Color(0xFFDFE4DA)
val OnSurfaceVariant = Color(0xFF3F4A36)

val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF0F5EB)
val SurfaceContainer = Color(0xFFEAEFE6)
val SurfaceContainerHigh = Color(0xFFE5EAE0)
val SurfaceContainerHighest = Color(0xFFDFE4DA)

val Outline = Color(0xFF6F7B64)
val OutlineVariant = Color(0xFFBECBB1)
val InverseSurface = Color(0xFF2C322B)
val InverseOnSurface = Color(0xFFEDF2E8)
val InversePrimary = Color(0xFF6BE026)

val GlassSurface = Color(0xEDFFFFFF)
val SoftGold = Color(0xFFFFF0A8)
val SoftGreen = Color(0xFFEAF6D7)
val SoftOlive = Color(0xFF263314)
val SoftOliveBright = Color(0xFF38481D)
val WarmPaper = Color(0xFFFBFCF8)
val DangerSoft = Color(0xFFFFF2EF)
val SuccessSoft = Color(0xFFEAF6D7)
val WarningSoft = Color(0xFFFFF3CC)

// --- Legacy aliases (for easy transition) ---
val BrandPrimary     = PrimaryContainer
val BrandPrimaryDark = Primary
val BrandAccent      = SecondaryContainer
val BrandBackground  = Background
val BrandSurface     = SurfaceContainerLowest
val BrandInk         = OnBackground
val BrandMuted       = OnSurfaceVariant
val BrandError       = Error
val BrandCream       = SurfaceContainerLow
val BrandSky         = SurfaceVariant
val BrandMint        = SurfaceContainer
val BrandPrimaryLight= SurfaceContainerHigh
val BrandCardOlive   = SurfaceContainer
val BrandCardGold    = SecondaryContainer
val BrandCardPeach   = ErrorContainer
val BrandGlass       = GlassSurface
val BrandPaper       = WarmPaper
val PrimaryGreen     = PrimaryContainer
val BackgroundLight  = Background
val SurfaceLight     = SurfaceContainerLowest
val TextPrimary      = OnBackground
val TextSecondary    = OnSurfaceVariant
val BrandStroke      = OutlineVariant
