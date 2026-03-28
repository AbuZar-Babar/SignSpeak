package com.example.kotlinfrontend.ui.translate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.LiveCameraScreen
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.ui.common.UiFormatters
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.BannerTone
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandPrimaryLight
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(container: AppContainer) {
    val viewModel: TranslateViewModel = viewModel(
        factory = appViewModelFactory {
            TranslateViewModel(
                authRepository     = container.authRepository,
                historyRepository  = container.historyRepository,
                complaintRepository = container.complaintRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val layoutSpec = rememberResponsiveLayoutSpec()
    var isCameraOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = layoutSpec.horizontalPadding,
                vertical   = layoutSpec.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Compact header ──────────────────────────────────────────────────
        Row(
            modifier            = Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = BrandPrimaryLight,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.SignLanguage,
                        contentDescription = null,
                        tint               = BrandPrimary,
                        modifier           = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
                Text(
                    text       = "Translate",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = BrandInk
                )
            }

            if (isCameraOpen) {
                TextButton(onClick = { isCameraOpen = false }) {
                    Icon(
                        imageVector        = Icons.Rounded.HideSource,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Close")
                }
            }
        }

        // ── Inline message banner ───────────────────────────────────────────
        uiState.message?.let { message ->
            InlineBanner(
                message   = message,
                tone      = BannerTone.Warning,
                onDismiss = viewModel::dismissMessage
            )
        }

        // ── Camera area — takes all remaining space ──────────────────────
        if (!isCameraOpen) {
            // Placeholder / open-camera prompt
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement   = Arrangement.Center,
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                CameraPlaceholder(onOpen = { isCameraOpen = true })
            }
        } else {
            // Camera fills all remaining vertical space — NO conflicting height modifiers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
            ) {
                LiveCameraScreen(
                    productMode          = true,
                    isAuthenticated      = uiState.sessionState.isAuthenticated,
                    onWordCommitted      = viewModel::onWordCommitted,
                    onReportPrediction   = viewModel::openPredictionReport
                )
            }
        }

        // ── Recent words strip ───────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.recentHistory.isNotEmpty(),
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text   = "Recent words",
                    style  = MaterialTheme.typography.labelLarge,
                    color  = BrandMuted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                LazyRow(
                    contentPadding        = PaddingValues(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recentHistory, key = { it.id }) { item ->
                        RecentWordChip(item = item)
                    }
                }
            }
        }
    }

    // ── Report bottom sheet ──────────────────────────────────────────────
    val pendingReport = uiState.pendingReport
    if (pendingReport != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissReport,
            containerColor   = BrandBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text  = "Report incorrect prediction",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandInk
                )
                Text(
                    text  = "Detected ${UiFormatters.prettyWord(pendingReport.reportedWord)} at ${UiFormatters.confidencePercent(pendingReport.confidence)} confidence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted
                )
                OutlinedTextField(
                    value         = uiState.expectedWord,
                    onValueChange = viewModel::updateExpectedWord,
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Expected word") },
                    singleLine    = true
                )
                OutlinedTextField(
                    value         = uiState.reportNote,
                    onValueChange = viewModel::updateReportNote,
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Notes") },
                    minLines      = 4
                )
                Button(
                    onClick  = viewModel::submitReport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = !uiState.isSubmittingReport
                ) {
                    Text(if (uiState.isSubmittingReport) "Sending..." else "Submit report")
                }
                TextButton(
                    onClick  = viewModel::dismissReport,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// ── Camera placeholder card ──────────────────────────────────────────────────
@Composable
private fun CameraPlaceholder(onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            verticalArrangement   = Arrangement.spacedBy(20.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = BrandPrimaryLight,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint               = BrandPrimary,
                    modifier           = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                )
            }
            Text(
                text       = "Ready to translate",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = BrandInk
            )
            Text(
                text  = "Open the camera to start recognising Pakistan Sign Language in real time.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandMuted,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Videocam,
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = "Open Camera",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Recent word chip ─────────────────────────────────────────────────────────
@Composable
private fun RecentWordChip(
    item: com.example.kotlinfrontend.data.model.TranslationHistoryItem
) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text       = UiFormatters.prettyWord(item.predictedWordSlug),
                style      = MaterialTheme.typography.labelLarge,
                color      = BrandInk,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = UiFormatters.confidencePercent(item.confidence),
                style = MaterialTheme.typography.labelSmall,
                color = BrandPrimary
            )
        }
    }
}
