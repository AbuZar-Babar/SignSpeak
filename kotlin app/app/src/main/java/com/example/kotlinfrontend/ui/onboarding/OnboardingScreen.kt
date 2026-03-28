package com.example.kotlinfrontend.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kotlinfrontend.ui.components.AppBackground
import com.example.kotlinfrontend.ui.motion.MotionTokens
import com.example.kotlinfrontend.ui.theme.BrandAccent
import com.example.kotlinfrontend.ui.theme.BrandGlass
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandPrimaryDark
import com.example.kotlinfrontend.ui.theme.SoftOlive
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = defaultOnboardingPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val layoutSpec = rememberResponsiveLayoutSpec()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == pages.lastIndex

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 32.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SignSpeak",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onComplete) {
                    Text("Skip")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                val isActive = pageIndex == currentPage
                val scale = animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.96f,
                    animationSpec = MotionTokens.standardTween(),
                    label = "onboardingScale"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp)
                        .padding(end = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(layoutSpec.illustrationHeight + 12.dp),
                        color = SoftOlive,
                        shape = RoundedCornerShape(32.dp),
                        shadowElevation = 10.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                SoftOlive,
                                                page.accentColor.copy(alpha = 0.18f)
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(112.dp * scale.value)
                                    .background(
                                        page.accentColor.copy(alpha = 0.24f),
                                        CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(top = 34.dp, end = 28.dp)
                                    .background(BrandGlass.copy(alpha = 0.3f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(layoutSpec.illustrationHeight - 48.dp)
                                    .padding(horizontal = 28.dp, vertical = 34.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                page.accentColor.copy(alpha = 0.18f),
                                                Color.White.copy(alpha = 0.08f)
                                            )
                                        ),
                                        RoundedCornerShape(30.dp)
                                    )
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 18.dp, vertical = 18.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = BrandGlass
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = page.previewLabel.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrandPrimaryDark
                                    )
                                    Text(
                                        text = page.previewCaption,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrandInk
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = page.eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = page.accentColor
                        )
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.displayMedium,
                            color = BrandInk
                        )
                        Text(
                            text = page.subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrandMuted
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        val indicatorWidth = animateFloatAsState(
                            targetValue = if (index == currentPage) 22f else 8f,
                            animationSpec = MotionTokens.standardTween(),
                            label = "indicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = indicatorWidth.value.dp, height = 8.dp)
                                .background(
                                    color = if (index == currentPage) BrandPrimary else BrandAccent.copy(alpha = 0.32f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isLastPage) "Get Started" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
