package com.example.kotlinfrontend.ui.profile

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.ui.common.UiFormatters
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.BannerTone
import com.example.kotlinfrontend.ui.components.EmptyStateCard
import com.example.kotlinfrontend.ui.components.GradientHeroCard
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.components.MetricChip
import com.example.kotlinfrontend.ui.components.SectionHeading
import com.example.kotlinfrontend.ui.components.StatusAssistChip
import com.example.kotlinfrontend.ui.components.WrappingChipRow
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(container: AppContainer) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = appViewModelFactory {
            ProfileViewModel(
                authRepository = container.authRepository,
                dictionaryRepository = container.dictionaryRepository,
                complaintRepository = container.complaintRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val layoutSpec = rememberResponsiveLayoutSpec()
    val user = uiState.sessionState.user

    fun openExternal(entry: DictionaryEntry) {
        val url = entry.externalUrl ?: return
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = layoutSpec.horizontalPadding,
            vertical = layoutSpec.verticalPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GradientHeroCard(
                eyebrow = "Profile",
                title = user?.fullName ?: user?.email ?: "Your account",
                subtitle = "Manage saved words, review submitted reports, and control your SignSpeak session."
            ) {
                WrappingChipRow {
                    StatusAssistChip(label = "Role ${UiFormatters.prettyWord(user?.role.orEmpty())}")
                    StatusAssistChip(label = if (uiState.sessionState.isConfigured) "Supabase connected" else "Config missing")
                }
            }
        }

        uiState.message?.let { message ->
            item {
                InlineBanner(
                    message = message,
                    tone = BannerTone.Neutral,
                    onDismiss = viewModel::dismissMessage
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricChip(
                    label = "Bookmarks",
                    value = uiState.bookmarks.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Reports",
                    value = uiState.complaints.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Sync",
                    value = if (uiState.sessionState.isConfigured) "On" else "Off",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeading(
                title = "Saved words",
                subtitle = "Words you marked for fast recall."
            )
        }

        if (uiState.bookmarks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (uiState.isLoading) "Loading bookmarks..." else "No saved words yet",
                    subtitle = "Bookmark words from the dictionary to build your quick reference set."
                )
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.bookmarks, key = { it.id }) { entry ->
                        Card(
                            modifier = Modifier.fillParentMaxWidth(0.82f),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandSurface)
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.englishWord,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = BrandInk
                                )
                                Text(
                                    text = entry.urduWord,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BrandPrimary
                                )
                                WrappingChipRow {
                                    StatusAssistChip(label = entry.category)
                                }
                                if (!entry.externalUrl.isNullOrBlank()) {
                                    TextButton(onClick = { openExternal(entry) }) {
                                        Text("Open PSL reference")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeading(
                title = "My reports",
                subtitle = "Complaints you submitted from translation or dictionary screens."
            )
        }

        if (uiState.complaints.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (uiState.isLoading) "Loading reports..." else "No reports yet",
                    subtitle = "When you report an incorrect prediction or dictionary issue, it will appear here."
                )
            }
        } else {
            items(uiState.complaints, key = { it.id }) { complaint ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = UiFormatters.prettyWord(
                                    complaint.reportedWordSlug ?: complaint.sourceType
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandInk,
                                fontWeight = FontWeight.Bold
                            )
                            StatusAssistChip(label = UiFormatters.complaintStatus(complaint.status))
                        }
                        complaint.note?.takeIf { it.isNotBlank() }?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandInk
                            )
                        }
                        Text(
                            text = UiFormatters.timestamp(complaint.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandPrimary
                        )
                    }
                }
            }
        }

        item {
            SectionHeading(
                title = "Account controls",
                subtitle = "Session and app status"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = BrandBackground)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = BrandPrimary.copy(alpha = 0.12f)) {
                            Text(
                                text = (user?.fullName?.take(1) ?: user?.email?.take(1) ?: "S").uppercase(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = BrandPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        androidx.compose.foundation.layout.Column {
                            Text(
                                text = user?.fullName ?: "Signed in user",
                                style = MaterialTheme.typography.titleLarge,
                                color = BrandInk
                            )
                            Text(
                                text = user?.email.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandInk
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::signOut,
                        enabled = !uiState.isWorking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isWorking) "Signing out..." else "Sign out")
                    }
                }
            }
        }
    }
}
