package com.example.kotlinfrontend.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import com.example.kotlinfrontend.ui.components.AppBackground
import com.example.kotlinfrontend.ui.components.HandIllustration
import com.example.kotlinfrontend.ui.motion.MotionTokens
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
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
                .padding(
                    horizontal = layoutSpec.horizontalPadding,
                    vertical = layoutSpec.verticalPadding
                )
                .widthIn(max = layoutSpec.contentMaxWidth),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SignSpeak",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandInk
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
                val pageOffset = (
                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                    ).absoluteValue
                val cardScale = animateFloatAsState(
                    targetValue = 1f - (pageOffset.coerceAtMost(1f) * 0.08f),
                    animationSpec = MotionTokens.standardTween(),
                    label = "onboardingScale"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                        .graphicsLayer {
                            scaleX = cardScale.value
                            scaleY = cardScale.value
                            alpha = 1f - pageOffset.coerceAtMost(1f) * 0.18f
                            translationX = pageOffset * 42f
                        },
                    color = BrandBackground.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(34.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(layoutSpec.illustrationHeight + 40.dp)
                                    .background(
                                        color = page.accentColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(30.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            HandIllustration(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                variant = page.variant
                            )
                        }
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
                            color = BrandMuted,
                            modifier = Modifier.alpha(0.95f)
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
                        val width = animateFloatAsState(
                            targetValue = if (index == currentPage) 28f else 10f,
                            animationSpec = MotionTokens.standardTween(),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(10.dp)
                                .size(width = width.value.dp, height = 10.dp)
                                .background(
                                    color = if (index == currentPage) pages[index].accentColor else BrandMuted.copy(alpha = 0.22f),
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
                    shape    = RoundedCornerShape(18.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor   = BrandBackground
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    )
                ) {
                    Text(
                        text       = if (isLastPage) "Get Started" else "Continue",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
