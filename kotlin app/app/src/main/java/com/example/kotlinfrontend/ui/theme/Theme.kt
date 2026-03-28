package com.example.kotlinfrontend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

private val SignSpeakColorScheme = lightColorScheme(
    primary              = BrandPrimary,
    onPrimary            = BrandSurface,
    primaryContainer     = BrandMint,
    onPrimaryContainer   = BrandPrimaryDark,
    secondary            = BrandAccent,
    onSecondary          = BrandInk,
    secondaryContainer   = BrandCream,
    onSecondaryContainer = BrandInk,
    tertiary             = BrandPrimaryDark,
    onTertiary           = BrandSurface,
    background           = BrandBackground,
    onBackground         = BrandInk,
    surface              = BrandSurface,
    onSurface            = BrandInk,
    surfaceVariant       = BrandSky,
    onSurfaceVariant     = BrandMuted,
    outline              = BrandStroke,
    error                = BrandError,
    onError              = BrandSurface
)

@Composable
fun KotlinFrontendTheme(
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    MaterialTheme(
        colorScheme = SignSpeakColorScheme,
        typography  = responsiveTypography(configuration.screenWidthDp),
        content     = content
    )
}
