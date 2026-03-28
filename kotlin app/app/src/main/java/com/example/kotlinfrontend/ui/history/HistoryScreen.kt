package com.example.kotlinfrontend.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.ui.common.UiFormatters
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.BannerTone
import com.example.kotlinfrontend.ui.components.EmptyStateCard
import com.example.kotlinfrontend.ui.components.GradientHeroCard
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.components.MetricChip
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec

@Composable
fun HistoryScreen(container: AppContainer) {
    val viewModel: HistoryViewModel = viewModel(
        factory = appViewModelFactory {
            HistoryViewModel(
                authRepository = container.authRepository,
                historyRepository = container.historyRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val layoutSpec = rememberResponsiveLayoutSpec()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = layoutSpec.horizontalPadding,
            vertical = layoutSpec.verticalPadding
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GradientHeroCard(
                eyebrow = "History",
                title = "A cleaner timeline of what the recognizer confirmed.",
                subtitle = "Track recent predictions, confidence levels, and model output without digging through a raw log."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChip(label = "Entries", value = uiState.items.size.toString(), modifier = Modifier.weight(1f))
                    MetricChip(label = "Sync", value = if (uiState.sessionState.isConfigured) "Cloud" else "Local", modifier = Modifier.weight(1f))
                }
            }
        }

        uiState.message?.let { message ->
            item {
                InlineBanner(
                    message = message,
                    tone = BannerTone.Warning,
                    onDismiss = viewModel::dismissMessage
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent activity",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandInk
                )
                TextButton(onClick = viewModel::refresh) {
                    Text("Refresh")
                }
            }
        }

        if (uiState.items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (uiState.isLoading) "Loading history..." else "No timeline yet",
                    subtitle = "Once the translator commits words, they will appear here in a grouped timeline."
                )
            }
        } else {
            itemsIndexed(uiState.items, key = { _, item -> item.id }) { index, item ->
                val currentDay = UiFormatters.timelineDayLabel(item.createdAt)
                val previousDay = uiState.items.getOrNull(index - 1)?.createdAt?.let(UiFormatters::timelineDayLabel)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currentDay != previousDay) {
                        Text(
                            text = currentDay,
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandMuted,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(12.dp)
                                    .background(BrandPrimary, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(72.dp)
                                    .background(Color(0x22000000), RoundedCornerShape(100))
                            )
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = UiFormatters.prettyWord(item.predictedWordSlug),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = BrandInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = BrandBackground)
                                    ) {
                                        Text(
                                            text = UiFormatters.confidencePercent(item.confidence),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = BrandPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = UiFormatters.timestamp(item.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandMuted
                                )
                                Text(
                                    text = "Model ${UiFormatters.modelVersion(item.modelVersion)} • ${if (uiState.sessionState.isConfigured) "Synced history" else "Device history"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
