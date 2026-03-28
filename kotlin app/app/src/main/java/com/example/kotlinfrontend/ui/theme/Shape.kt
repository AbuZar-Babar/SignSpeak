package com.example.kotlinfrontend.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(24.dp), // md is 1.5rem (approx 24dp)
    large = RoundedCornerShape(32.dp),  // lg is 2rem (approx 32dp)
    extraLarge = RoundedCornerShape(48.dp) // xl is 3rem
)
