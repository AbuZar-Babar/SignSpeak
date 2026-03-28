package com.example.kotlinfrontend.ui.dictionary

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.example.kotlinfrontend.ui.components.SectionHeading
import com.example.kotlinfrontend.ui.components.StatusAssistChip
import com.example.kotlinfrontend.ui.components.WrappingChipRow
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(container: AppContainer) {
    val context = LocalContext.current
    val viewModel: DictionaryViewModel = viewModel(
        factory = appViewModelFactory {
            DictionaryViewModel(
                authRepository = container.authRepository,
                dictionaryRepository = container.dictionaryRepository,
                bookmarkRepository = container.bookmarkRepository,
                complaintRepository = container.complaintRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val layoutSpec = rememberResponsiveLayoutSpec()

    fun openExternalLink(url: String?) {
        if (url.isNullOrBlank()) return
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = layoutSpec.horizontalPadding,
            vertical = layoutSpec.verticalPadding
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GradientHeroCard(
                eyebrow = "Dictionary",
                title = "Search PSL words with more clarity and less clutter.",
                subtitle = "Browse English or Urdu entries, save what matters, and open PSL references from a cleaner catalog."
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search English or Urdu") },
                    singleLine = true
                )
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
            SectionHeading(
                title = "Categories",
                subtitle = "Filter the dictionary by topic."
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.categories) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category) }
                    )
                }
            }
        }

        if (uiState.entries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (uiState.isLoading) "Loading dictionary..." else "No matching words",
                    subtitle = "Try a different search query or switch back to All categories."
                )
            }
        } else {
            items(uiState.entries, key = { it.id }) { entry ->
                val isBookmarked = entry.id in uiState.bookmarkedIds
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openEntry(entry) },
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                            }
                            IconButton(onClick = { viewModel.toggleBookmark(entry.id) }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isBookmarked) BrandPrimary else BrandMuted
                                )
                            }
                        }

                        WrappingChipRow {
                            StatusAssistChip(label = entry.category)
                            StatusAssistChip(label = UiFormatters.reviewStatus(entry.reviewStatus))
                        }

                        if (!entry.externalUrl.isNullOrBlank()) {
                            TextButton(onClick = { openExternalLink(entry.externalUrl) }) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                                Text("Open PSL reference")
                            }
                        }
                    }
                }
            }
        }
    }

    val selectedEntry = uiState.selectedEntry
    if (selectedEntry != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissEntry,
            containerColor = BrandBackground
        ) {
            DictionaryEntrySheet(
                entry = selectedEntry,
                isBookmarked = selectedEntry.id in uiState.bookmarkedIds,
                onBookmark = { viewModel.toggleBookmark(selectedEntry.id) },
                onOpenReference = { openExternalLink(selectedEntry.externalUrl) },
                onReport = {
                    viewModel.dismissEntry()
                    viewModel.openReport(selectedEntry)
                }
            )
        }
    }

    val reportEntry = uiState.reportEntry
    if (reportEntry != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissReport,
            containerColor = BrandBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Report dictionary issue",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandInk
                )
                Text(
                    text = "${reportEntry.englishWord} • ${reportEntry.urduWord}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandMuted
                )
                OutlinedTextField(
                    value = uiState.reportNote,
                    onValueChange = viewModel::updateReportNote,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What looks incorrect?") },
                    minLines = 4
                )
                androidx.compose.material3.Button(
                    onClick = viewModel::submitReport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSubmittingReport
                ) {
                    Text(if (uiState.isSubmittingReport) "Sending..." else "Submit report")
                }
                TextButton(
                    onClick = viewModel::dismissReport,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryEntrySheet(
    entry: DictionaryEntry,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onOpenReference: () -> Unit,
    onReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = entry.englishWord,
            style = MaterialTheme.typography.headlineSmall,
            color = BrandInk
        )
        Text(
            text = entry.urduWord,
            style = MaterialTheme.typography.bodyLarge,
            color = BrandPrimary
        )
        WrappingChipRow {
            StatusAssistChip(label = entry.category)
            StatusAssistChip(label = UiFormatters.reviewStatus(entry.reviewStatus))
        }
        Text(
            text = "Use the actions below to open the PSL source, save this entry, or report a mismatch.",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandMuted
        )
        if (!entry.externalUrl.isNullOrBlank()) {
            TextButton(onClick = onOpenReference) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                Text("Open PSL reference")
            }
        }
        TextButton(onClick = onBookmark) {
            Icon(
                imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = null
            )
            Text(if (isBookmarked) "Remove bookmark" else "Save bookmark")
        }
        TextButton(onClick = onReport) {
            Icon(Icons.Rounded.ReportProblem, contentDescription = null)
            Text("Report this entry")
        }
    }
}
