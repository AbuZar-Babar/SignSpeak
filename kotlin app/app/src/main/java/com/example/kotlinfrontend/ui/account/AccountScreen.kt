package com.example.kotlinfrontend.ui.account

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.ui.common.UiFormatters
import com.example.kotlinfrontend.ui.common.appViewModelFactory

@Composable
fun AccountScreen(container: AppContainer) {
    val context = LocalContext.current
    val viewModel: AccountViewModel = viewModel(
        factory = appViewModelFactory {
            AccountViewModel(
                authRepository = container.authRepository,
                dictionaryRepository = container.dictionaryRepository,
                complaintRepository = container.complaintRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun openExternal(entry: DictionaryEntry) {
        val url = entry.externalUrl ?: return
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2EA)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF402218), Color(0xFF865439), Color(0xFFD1B894))
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (uiState.sessionState.isAuthenticated) {
                            "Your Account"
                        } else {
                            "Sign In for Sync"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (uiState.sessionState.isAuthenticated) {
                            "Manage bookmarks, review your reports, and keep translation history synced."
                        } else {
                            "Guest access keeps translation and dictionary browsing open. Sign in to sync history, bookmark words, and submit reports."
                        },
                        color = Color(0xFFFFF2E5)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (uiState.sessionState.isConfigured) {
                                        "Supabase Connected"
                                    } else {
                                        "Supabase Missing"
                                    }
                                )
                            }
                        )
                        if (uiState.sessionState.isAuthenticated) {
                            AssistChip(onClick = viewModel::refresh, label = { Text("Refresh") })
                        }
                    }
                }
            }
        }

        if (!uiState.message.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4DB)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.message.orEmpty(),
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF6B4F00)
                        )
                        TextButton(onClick = viewModel::dismissMessage) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        if (!uiState.sessionState.isConfigured) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E4)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        text = "Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in Gradle properties or environment variables to enable login, reports, bookmarks, and sync.",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF7A2E1F)
                    )
                }
            }
        }

        if (!uiState.sessionState.isAuthenticated) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { viewModel.setAuthMode(AccountAuthMode.LOGIN) },
                                label = { Text("Login") }
                            )
                            AssistChip(
                                onClick = { viewModel.setAuthMode(AccountAuthMode.REGISTER) },
                                label = { Text("Register") }
                            )
                            AssistChip(
                                onClick = { viewModel.setAuthMode(AccountAuthMode.RESET) },
                                label = { Text("Reset") }
                            )
                        }

                        if (uiState.authMode == AccountAuthMode.REGISTER) {
                            OutlinedTextField(
                                value = uiState.fullName,
                                onValueChange = viewModel::updateFullName,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Full name") },
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = viewModel::updateEmail,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            singleLine = true
                        )

                        if (uiState.authMode != AccountAuthMode.RESET) {
                            OutlinedTextField(
                                value = uiState.password,
                                onValueChange = viewModel::updatePassword,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }

                        TextButton(
                            onClick = viewModel::submitAuth,
                            enabled = !uiState.isWorking
                        ) {
                            Text(
                                text = when {
                                    uiState.isWorking -> "Please wait..."
                                    uiState.authMode == AccountAuthMode.LOGIN -> "Sign In"
                                    uiState.authMode == AccountAuthMode.REGISTER -> "Create Account"
                                    else -> "Send Reset Email"
                                }
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = uiState.sessionState.user?.fullName
                                ?: uiState.sessionState.user?.email
                                ?: "Signed in",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3B2B20)
                        )
                        Text(
                            text = uiState.sessionState.user?.email.orEmpty(),
                            color = Color(0xFF756154)
                        )
                        Text(
                            text = "Role: ${UiFormatters.prettyWord(uiState.sessionState.user?.role.orEmpty())}",
                            color = Color(0xFF756154)
                        )
                        TextButton(
                            onClick = viewModel::signOut,
                            enabled = !uiState.isWorking
                        ) {
                            Text(if (uiState.isWorking) "Signing out..." else "Sign Out")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Bookmarks",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF402218)
                )
            }

            if (uiState.bookmarks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(
                            text = if (uiState.isLoadingData) {
                                "Loading bookmarks..."
                            } else {
                                "No bookmarks yet."
                            },
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF756154)
                        )
                    }
                }
            } else {
                items(uiState.bookmarks, key = { item -> item.id }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.englishWord,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF3B2B20)
                                )
                                Text(
                                    text = entry.urduWord,
                                    color = Color(0xFF756154)
                                )
                            }
                            if (!entry.externalUrl.isNullOrBlank()) {
                                TextButton(onClick = { openExternal(entry) }) {
                                    Text("Open PSL")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "My Reports",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF402218)
                )
            }

            if (uiState.complaints.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(
                            text = if (uiState.isLoadingData) {
                                "Loading reports..."
                            } else {
                                "No reports yet."
                            },
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF756154)
                        )
                    }
                }
            } else {
                items(uiState.complaints, key = { item -> item.id }) { complaint ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = UiFormatters.prettyWord(
                                    complaint.reportedWordSlug ?: complaint.sourceType
                                ),
                                color = Color(0xFF3B2B20),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Status: ${UiFormatters.complaintStatus(complaint.status)}",
                                color = Color(0xFF756154)
                            )
                            if (!complaint.note.isNullOrBlank()) {
                                Text(
                                    text = complaint.note,
                                    color = Color(0xFF5E4B3F)
                                )
                            }
                            Text(
                                text = UiFormatters.timestamp(complaint.createdAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8A776B)
                            )
                        }
                    }
                }
            }
        }
    }
}
