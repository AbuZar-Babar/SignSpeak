package com.example.kotlinfrontend.ui.account

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.ui.common.UiFormatters
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.EditorialTextField
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandPrimaryDark
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.SurfaceContainerHigh
import com.example.kotlinfrontend.ui.theme.SurfaceContainerLow

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
            .background(BrandBackground),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                AccountAuthCard(
                    uiState = uiState,
                    onModeChange = viewModel::setAuthMode,
                    onFullNameChange = viewModel::updateFullName,
                    onEmailChange = viewModel::updateEmail,
                    onPasswordChange = viewModel::updatePassword,
                    onInviteCodeChange = viewModel::updateInviteCode,
                    onSubmit = viewModel::submitAuth
                )
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
                        Text(
                            text = if (uiState.sessionState.user?.organizationId.isNullOrBlank()) {
                                "Institute: Not linked"
                            } else {
                                "Institute: Linked"
                            },
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
                            text = "Join an institute",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3B2B20)
                        )
                        Text(
                            text = "Enter the invite code shared by your school or organization.",
                            color = Color(0xFF756154)
                        )
                        EditorialTextField(
                            value = uiState.inviteCode,
                            onValueChange = viewModel::updateInviteCode,
                            label = "Institute invite code",
                            leadingIcon = Icons.Rounded.School
                        )
                        TextButton(
                            onClick = viewModel::joinOrganization,
                            enabled = !uiState.isWorking && uiState.sessionState.isConfigured
                        ) {
                            Text(if (uiState.isWorking) "Joining..." else "Join institute")
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

@Composable
private fun AccountAuthCard(
    uiState: AccountUiState,
    onModeChange: (AccountAuthMode) -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val isLogin = uiState.authMode == AccountAuthMode.LOGIN
    val isRegister = uiState.authMode == AccountAuthMode.REGISTER
    val isReset = uiState.authMode == AccountAuthMode.RESET

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountAuthHero(authMode = uiState.authMode)
            AccountAuthModeTabs(selectedMode = uiState.authMode, onModeChange = onModeChange)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when {
                        isLogin -> "Sign in to sync"
                        isRegister -> "Create your account"
                        else -> "Reset your password"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandInk,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = when {
                        isLogin -> "Keep translation history, reports, and bookmarks available across devices."
                        isRegister -> "Add an institute code now or join your school after signing in."
                        else -> "Enter your account email and we will send a secure reset link."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            if (isRegister) {
                EditorialTextField(
                    value = uiState.fullName,
                    onValueChange = onFullNameChange,
                    label = "Full name",
                    leadingIcon = Icons.Rounded.Person
                )
            }

            EditorialTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = "Email address",
                leadingIcon = Icons.Rounded.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            if (isRegister) {
                EditorialTextField(
                    value = uiState.inviteCode,
                    onValueChange = onInviteCodeChange,
                    label = "Institute code (optional)",
                    leadingIcon = Icons.Rounded.School
                )
            }

            if (!isReset) {
                EditorialTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    leadingIcon = Icons.Rounded.Lock,
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !uiState.isWorking && uiState.sessionState.isConfigured,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = when {
                        uiState.isWorking -> "Please wait..."
                        isLogin -> "Sign In"
                        isRegister -> "Create Account"
                        else -> "Send Reset Email"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AccountAuthHero(authMode: AccountAuthMode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(BrandPrimaryDark, Color(0xFF2F7E37), BrandPrimary)
                )
            )
            .padding(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.TopEnd)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SignLanguage,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "SignSpeak Account",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun AccountAuthModeTabs(
    selectedMode: AccountAuthMode,
    onModeChange: (AccountAuthMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SurfaceContainerLow,
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AccountAuthModeTab(
                label = "Login",
                selected = selectedMode == AccountAuthMode.LOGIN,
                onClick = { onModeChange(AccountAuthMode.LOGIN) },
                modifier = Modifier.weight(1f)
            )
            AccountAuthModeTab(
                label = "Register",
                selected = selectedMode == AccountAuthMode.REGISTER,
                onClick = { onModeChange(AccountAuthMode.REGISTER) },
                modifier = Modifier.weight(1f)
            )
            AccountAuthModeTab(
                label = "Reset",
                selected = selectedMode == AccountAuthMode.RESET,
                onClick = { onModeChange(AccountAuthMode.RESET) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AccountAuthModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) BrandSurface else Color.Transparent,
        shadowElevation = if (selected) 3.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) BrandPrimaryDark else BrandMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
