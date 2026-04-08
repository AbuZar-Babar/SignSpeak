package com.example.kotlinfrontend.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.kotlinfrontend.data.model.TranslationHistoryItem
import com.example.kotlinfrontend.ui.common.UiFormatters
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.EmptyStateCard
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.components.ScreenTopBar
import com.example.kotlinfrontend.ui.theme.BrandAccent
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun HistoryScreen(
    container: AppContainer,
    onAvatarClick: () -> Unit = {}
) {
    val viewModel: HistoryViewModel = viewModel(
        factory = appViewModelFactory {
            HistoryViewModel(
                authRepository = container.authRepository,
                dictionaryRepository = container.dictionaryRepository,
                historyRepository = container.historyRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarSeed = uiState.sessionState.user?.fullName ?: uiState.sessionState.user?.email ?: "SignSpeak"
    val weeklyCount = uiState.items.count { item ->
        runCatching {
            Instant.parse(item.createdAt).isAfter(Instant.now().minus(7, ChronoUnit.DAYS))
        }.getOrDefault(false)
    }
    val accuracy = if (uiState.items.isEmpty()) 0 else {
        ((uiState.items.sumOf { it.confidence } / uiState.items.size) * 100).toInt()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 32.dp,
            top = 18.dp,
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenTopBar(
                avatarSeed = avatarSeed,
                onAvatarClick = onAvatarClick,
                trailingContent = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = BrandMuted
                        )
                    }
                }
            )
        }

        item {
            Text(
                text = "Translation History",
                style = MaterialTheme.typography.displayMedium,
                color = BrandInk,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = "Your journey through Pakistan Sign Language.",
                style = MaterialTheme.typography.bodyLarge,
                color = BrandMuted
            )
        }

        uiState.message?.let { message ->
            item {
                InlineBanner(message = message, onDismiss = viewModel::dismissMessage)
            }
        }

        if (uiState.items.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (uiState.isLoading) "Loading history..." else "No timeline yet",
                    subtitle = "Once the translator confirms words, they will appear here."
                )
            }
        } else {
            items(uiState.items, key = { it.id }) { item ->
                HistoryTranslationCard(
                    item = item,
                    urduTranslation = uiState.translationLookup[item.predictedWordSlug]?.urduWord
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        value = weeklyCount.toString(),
                        label = "Translations this week",
                        accentText = BrandPrimary
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        value = "$accuracy%",
                        label = "Average accuracy",
                        accentText = BrandAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTranslationCard(
    item: TranslationHistoryItem,
    urduTranslation: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ENGLISH WORD",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandMuted
                )
                ConfidencePill(UiFormatters.confidencePercent(item.confidence))
            }

            Text(
                text = UiFormatters.prettyWord(item.predictedWordSlug),
                style = MaterialTheme.typography.headlineSmall,
                color = BrandInk,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = BrandBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "URDU TRANSLATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandMuted
                        )
                        Text(
                            text = urduTranslation ?: "Translation unavailable",
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrandInk
                        )
                    }
                    Text(
                        text = "Model ${UiFormatters.modelVersion(item.modelVersion)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandMuted
                    )
                }
            }

            Text(
                text = UiFormatters.timelineDayLabel(item.createdAt).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = BrandMuted
            )
        }
    }
}

@Composable
private fun ConfidencePill(text: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandAccent)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = BrandInk,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryCard(
    value: String,
    label: String,
    accentText: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = BrandBackground)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = accentText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = BrandInk
            )
        }
    }
}
