package com.example.kotlinfrontend.ui.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.LiveCameraScreen
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.ui.common.UiFormatters
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.EditorialTextField
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.components.ScreenTopBar
import com.example.kotlinfrontend.ui.theme.BrandAccent
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(container: AppContainer) {
    val viewModel: TranslateViewModel = viewModel(
        factory = appViewModelFactory {
            TranslateViewModel(
                authRepository = container.authRepository,
                historyRepository = container.historyRepository,
                complaintRepository = container.complaintRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarSeed = uiState.sessionState.user?.fullName ?: uiState.sessionState.user?.email ?: "SignSpeak"
    var showCamera by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 32.dp, top = 18.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenTopBar(avatarSeed = avatarSeed)

        uiState.message?.let { message ->
            InlineBanner(
                message = message,
                onDismiss = viewModel::dismissMessage
            )
        }

        if (showCamera) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LiveCameraScreen(
                    productMode = true,
                    isAuthenticated = uiState.sessionState.isAuthenticated,
                    onRequireAuth = { showCamera = false },
                    onWordCommitted = viewModel::onWordCommitted,
                    onReportPrediction = viewModel::openPredictionReport
                )
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authentication needed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BrandInk
                    )
                    Text(
                        text = "Sign in to report incorrect predictions and keep your history synced.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandMuted,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    TextButton(onClick = { showCamera = true }) {
                        Text("Back to camera")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Words",
                style = MaterialTheme.typography.titleMedium,
                color = BrandInk,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "View History",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandPrimary
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.recentHistory.take(8), key = { it.id }) { item ->
                RecentWordPill(item = item)
            }
        }
    }

    uiState.pendingReport?.let { pendingReport ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissReport,
            containerColor = BrandBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .height(5.dp)
                            .fillMaxWidth(0.16f),
                        shape = RoundedCornerShape(100),
                        color = BrandMuted.copy(alpha = 0.2f)
                    ) {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "What went wrong?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = BrandInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your feedback helps our AI bridge the gap between PSL and speech more accurately.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMuted
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = BrandAccent
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = BrandInk
                            )
                        }
                    }
                }

                PredictionReportReason.entries.forEach { reason ->
                    ReportReasonCard(
                        title = reason.title,
                        selected = uiState.reportReason == reason,
                        onClick = { viewModel.updateReportReason(reason) }
                    )
                }

                if (uiState.reportReason != PredictionReportReason.Other) {
                    EditorialTextField(
                        value = uiState.expectedWord,
                        onValueChange = viewModel::updateExpectedWord,
                        label = if (uiState.reportReason == PredictionReportReason.WrongSign) {
                            "Correct sign"
                        } else {
                            "Correct translation"
                        }
                    )
                }

                EditorialTextField(
                    value = uiState.reportNote,
                    onValueChange = viewModel::updateReportNote,
                    label = "Additional notes",
                    singleLine = false,
                    minLines = 4
                )

                androidx.compose.material3.Button(
                    onClick = viewModel::submitReport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSubmittingReport,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(if (uiState.isSubmittingReport) "Submitting..." else "Submit Report")
                }

                Text(
                    text = "Detected ${UiFormatters.prettyWord(pendingReport.reportedWord)} at ${UiFormatters.confidencePercent(pendingReport.confidence)} confidence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentWordPill(item: com.example.kotlinfrontend.data.model.TranslationHistoryItem) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = BrandBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = UiFormatters.prettyWord(item.predictedWordSlug),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandInk,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = UiFormatters.confidencePercent(item.confidence),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandMuted
                )
            }
        }
    }
}

@Composable
private fun ReportReasonCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BrandBackground else Color.White.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = if (selected) BrandPrimary else Color.Transparent,
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, CircleShape)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandInk
            )
        }
    }
}
