package com.example.kotlinfrontend.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.kotlinfrontend.ui.theme.BrandAccent
import com.example.kotlinfrontend.ui.theme.BrandCardGold
import com.example.kotlinfrontend.ui.theme.BrandCardOlive
import com.example.kotlinfrontend.ui.theme.BrandCardPeach
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandPrimaryDark
import com.example.kotlinfrontend.ui.theme.BrandSurface

enum class IllustrationVariant {
    Auth,
    Translate,
    Dictionary,
    Feedback
}

@Composable
fun HandIllustration(
    modifier: Modifier = Modifier,
    variant: IllustrationVariant
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BrandSurface.copy(alpha = 0.88f),
                        Color(0xFFF6F4EA),
                        Color(0xFFE8F5E9)
                    )
                )
            )
    ) {
        FloatingBadge(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            text = when (variant) {
                IllustrationVariant.Auth -> "Sign in"
                IllustrationVariant.Translate -> "Live"
                IllustrationVariant.Dictionary -> "Words"
                IllustrationVariant.Feedback -> "Reports"
            }
        )
        FloatingBadge(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            text = when (variant) {
                IllustrationVariant.Auth -> "Secure"
                IllustrationVariant.Translate -> "Camera"
                IllustrationVariant.Dictionary -> "PSL"
                IllustrationVariant.Feedback -> "Review"
            },
            tone = when (variant) {
                IllustrationVariant.Dictionary -> BrandCardGold
                IllustrationVariant.Feedback -> BrandCardPeach
                else -> BrandCardOlive
            }
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val palmColor = when (variant) {
                IllustrationVariant.Feedback -> Color(0xFFF7C1AE)
                else -> Color(0xFFF3BE9D)
            }
            val outline = BrandPrimaryDark
            val palmWidth = size.width * 0.28f
            val palmHeight = size.height * 0.34f
            val palmLeft = size.width * 0.36f
            val palmTop = size.height * 0.36f

            drawRoundRect(
                color = BrandAccent.copy(alpha = 0.18f),
                topLeft = Offset(size.width * 0.12f, size.height * 0.12f),
                size = Size(size.width * 0.76f, size.height * 0.76f),
                cornerRadius = CornerRadius(48f, 48f)
            )

            drawRoundRect(
                color = palmColor,
                topLeft = Offset(palmLeft, palmTop),
                size = Size(palmWidth, palmHeight),
                cornerRadius = CornerRadius(48f, 48f)
            )

            val fingerWidth = palmWidth * 0.18f
            val fingerHeight = size.height * 0.28f
            val fingerSpacing = fingerWidth * 0.3f
            repeat(4) { index ->
                val left = palmLeft + (fingerWidth + fingerSpacing) * index + fingerWidth * 0.15f
                drawRoundRect(
                    color = palmColor,
                    topLeft = Offset(left, palmTop - fingerHeight * 0.72f),
                    size = Size(fingerWidth, fingerHeight),
                    cornerRadius = CornerRadius(32f, 32f)
                )
            }

            rotate(-35f, pivot = Offset(palmLeft + 10f, palmTop + palmHeight * 0.75f)) {
                drawRoundRect(
                    color = palmColor,
                    topLeft = Offset(palmLeft - palmWidth * 0.18f, palmTop + palmHeight * 0.32f),
                    size = Size(palmWidth * 0.38f, fingerHeight * 0.72f),
                    cornerRadius = CornerRadius(28f, 28f)
                )
            }

            drawLine(
                color = outline,
                start = Offset(palmLeft + palmWidth * 0.42f, palmTop + palmHeight * 0.15f),
                end = Offset(palmLeft + palmWidth * 0.18f, palmTop + palmHeight * 0.42f),
                strokeWidth = 5f
            )
            drawLine(
                color = outline,
                start = Offset(palmLeft + palmWidth * 0.7f, palmTop + palmHeight * 0.22f),
                end = Offset(palmLeft + palmWidth * 0.32f, palmTop + palmHeight * 0.56f),
                strokeWidth = 5f
            )

            val bubbleColor = when (variant) {
                IllustrationVariant.Translate -> BrandPrimary.copy(alpha = 0.16f)
                IllustrationVariant.Dictionary -> BrandAccent.copy(alpha = 0.22f)
                IllustrationVariant.Feedback -> Color(0xFFF5B4A6).copy(alpha = 0.22f)
                IllustrationVariant.Auth -> BrandCardOlive.copy(alpha = 0.55f)
            }

            drawCircle(
                color = bubbleColor,
                radius = size.minDimension * 0.1f,
                center = Offset(size.width * 0.24f, size.height * 0.32f)
            )
            drawCircle(
                color = BrandPrimary.copy(alpha = 0.12f),
                radius = size.minDimension * 0.08f,
                center = Offset(size.width * 0.78f, size.height * 0.26f)
            )
        }
    }
}

@Composable
private fun BoxScope.FloatingBadge(
    modifier: Modifier,
    text: String,
    tone: Color = BrandCardOlive
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tone)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = BrandInk
        )
    }
}
