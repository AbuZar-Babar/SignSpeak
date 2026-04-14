package com.example.kotlinfrontend.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.CloudSyncCard
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.components.ScreenTopBar
import com.example.kotlinfrontend.ui.components.TonalActionCard
import com.example.kotlinfrontend.ui.theme.BrandError
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.SurfaceContainerLow
import com.example.kotlinfrontend.ui.theme.SurfaceContainerLowest
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec

@Composable
fun ProfileScreen(
    container: AppContainer,
    onAvatarClick: () -> Unit = {}
) {
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
    val displayName = user?.fullName?.trim().orEmpty().ifBlank { "Friend" }
    val avatarSeed = user?.fullName ?: user?.email ?: "SignSpeak"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 32.dp,
            top = 18.dp,
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenTopBar(
                avatarSeed = avatarSeed,
                onAvatarClick = onAvatarClick
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Assalam-o-Alaikum,",
                    style = MaterialTheme.typography.displayMedium,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrandInk,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = user?.email.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted
                )
            }
        }

        uiState.message?.let { message ->
            item {
                InlineBanner(message = message, onDismiss = viewModel::dismissMessage)
            }
        }

        item {
            TonalActionCard(
                title = "Saved Words",
                subtitle = "${uiState.bookmarks.size} bookmarked PSL signs",
                icon = Icons.Rounded.Bookmark
            )
        }

        item {
            TonalActionCard(
                title = "My Reports",
                subtitle = "${uiState.complaints.size} incorrect predictions submitted",
                icon = Icons.Rounded.Description,
                accent = BrandPrimary
            )
        }

        item {
            CloudSyncCard(checked = uiState.sessionState.isConfigured)
        }

        item {
            Text(
                text = "ACCOUNT MANAGEMENT",
                style = MaterialTheme.typography.labelLarge,
                color = BrandMuted,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest)
            ) {
                Column {
                    AccountRow(
                        title = "Log out",
                        tint = BrandInk,
                        icon = Icons.Rounded.Logout,
                        onClick = viewModel::signOut
                    )
                    BoxDivider()
                    AccountRow(
                        title = "Delete Account",
                        tint = BrandError,
                        icon = Icons.Rounded.DeleteOutline,
                        enabled = false,
                        onClick = {}
                    )
                }
            }
        }

        item {
            Text(
                text = "SignSpeak v1.0 • Made with love in Pakistan",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun AccountRow(
    title: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = tint,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = tint.copy(alpha = 0.55f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(imageVector = icon, contentDescription = null)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = if (enabled) ">" else "",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun BoxDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(SurfaceContainerLow)
    )
}
