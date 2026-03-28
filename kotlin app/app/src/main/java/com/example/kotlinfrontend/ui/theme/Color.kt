package com.example.kotlinfrontend.ui.theme

import androidx.compose.ui.graphics.Color

// --- Primary brand — Duolingo green ---
val BrandPrimary     = Color(0xFF58CC02) // Duolingo signature green
val BrandPrimaryDark = Color(0xFF3D9100) // pressed / dark variant
val BrandPrimaryLight= Color(0xFFD7F5B1) // light tint for backgrounds

// --- Accent ---
val BrandAccent      = Color(0xFFFFE066) // warm gold accent

// --- Surfaces ---
val BrandBackground  = Color(0xFFF8FAF5) // very light green-white
val BrandSurface     = Color(0xFFFFFFFF) // pure white cards
val BrandSurfaceAlt  = Color(0xFFF0F0F0) // chip / secondary surface

// --- Typography ---
val BrandInk         = Color(0xFF1A1A1A) // near-black text
val BrandMuted       = Color(0xFF777777) // secondary text
val BrandMutedLight  = Color(0xFFB0B0B0) // placeholders / disabled

// --- Semantic ---
val BrandError       = Color(0xFFFF4B4B) // red error
val BrandSuccess     = Color(0xFF58CC02) // reuse primary

// --- Stroke ---
val BrandStroke      = Color(0xFFE5E5E5) // dividers / borders

// --- Duolingo card tints ---
val BrandCream       = Color(0xFFFFF9E6)
val BrandMint        = Color(0xFFE8F9E0)
val BrandSky         = Color(0xFFE8F4FD)
val BrandCardOlive   = Color(0xFFDDEEDB)
val BrandCardGold    = Color(0xFFFFE9A9)
val BrandCardPeach   = Color(0xFFFFE3D8)

// --- Legacy aliases (keep compile compat) ---
val PrimaryGreen     = BrandPrimary
val BackgroundLight  = BrandBackground
val SurfaceLight     = BrandSurface
val TextPrimary      = BrandInk
val TextSecondary    = BrandMuted
