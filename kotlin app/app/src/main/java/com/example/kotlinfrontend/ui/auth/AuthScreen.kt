package com.example.kotlinfrontend.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.AppBackground
import com.example.kotlinfrontend.ui.components.BannerTone
import com.example.kotlinfrontend.ui.components.EditorialTextField
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandPrimaryDark
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.SurfaceContainerLow
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(container: AppContainer) {
    val viewModel: AuthViewModel = viewModel(
        factory = appViewModelFactory {
            AuthViewModel(authRepository = container.authRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val layoutSpec = rememberResponsiveLayoutSpec()
    val isLogin = uiState.mode == AuthMode.Login

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = layoutSpec.horizontalPadding, vertical = layoutSpec.verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(34.dp),
                    color = BrandSurface,
                    tonalElevation = 2.dp,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AuthHeroPanel(isLogin = isLogin)

                        AuthModePill(isLogin = isLogin)

                        if (!uiState.sessionState.isConfigured) {
                            InlineBanner(
                                message = "Supabase is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY before using authentication.",
                                tone = BannerTone.Error
                            )
                        }

                        uiState.message?.let { message ->
                            InlineBanner(
                                message = message,
                                tone = if (uiState.isError) BannerTone.Error else BannerTone.Success,
                                onDismiss = viewModel::dismissMessage
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!isLogin) {
                                EditorialTextField(
                                    value = uiState.fullName,
                                    onValueChange = viewModel::updateFullName,
                                    label = "Full Name",
                                    leadingIcon = Icons.Rounded.Person
                                )
                            }

                            EditorialTextField(
                                value = uiState.email,
                                onValueChange = viewModel::updateEmail,
                                label = "Email Address",
                                leadingIcon = Icons.Rounded.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            EditorialTextField(
                                value = uiState.password,
                                onValueChange = viewModel::updatePassword,
                                label = "Password",
                                leadingIcon = Icons.Rounded.Lock,
                                visualTransformation = PasswordVisualTransformation()
                            )

                            if (isLogin) {
                                TextButton(
                                    onClick = viewModel::openResetSheet,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Forgot password?")
                                }
                            } else {
                                EditorialTextField(
                                    value = uiState.confirmPassword,
                                    onValueChange = viewModel::updateConfirmPassword,
                                    label = "Confirm Password",
                                    leadingIcon = Icons.Rounded.Lock,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            }
                        }

                        Button(
                            onClick = viewModel::submit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !uiState.isWorking && uiState.sessionState.isConfigured,
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
                                    else -> "Create Account"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(BrandMuted.copy(alpha = 0.12f))
                            )
                            Text(
                                text = if (isLogin) "NEW HERE?" else "HAVE AN ACCOUNT?",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandMuted,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(BrandMuted.copy(alpha = 0.12f))
                            )
                        }

                        Button(
                            onClick = { viewModel.setMode(if (isLogin) AuthMode.Register else AuthMode.Login) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceContainerLow,
                                contentColor = BrandInk
                            )
                        ) {
                            Text(
                                text = if (isLogin) "Create New Account" else "Back To Sign In",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "Bridge communication through signs.",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandMuted
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (uiState.showResetSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeResetSheet,
            containerColor = BrandBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Reset your password",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandInk
                )
                Text(
                    text = "We'll email a reset link to the address on your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted
                )
                EditorialTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = "Email Address",
                    leadingIcon = Icons.Rounded.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Button(
                    onClick = viewModel::sendPasswordReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isWorking,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(if (uiState.isWorking) "Sending..." else "Send reset email")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AuthHeroPanel(isLogin: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BrandPrimaryDark,
                        Color(0xFF2F7E37),
                        Color(0xFF73D942)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(118.dp)
                .align(Alignment.TopEnd)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
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
                        text = "SignSpeak",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isLogin) "Welcome back" else "Create your account",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.86f)
                    )
                }
            }

            Text(
                text = if (isLogin) {
                    "Continue translating signs, tracking progress, and learning with your institute."
                } else {
                    "Start your SignSpeak profile and connect with your institute using an invite code."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.94f),
                fontWeight = FontWeight.Medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthFeatureChip(label = "Translate")
                AuthFeatureChip(label = "Learn")
                AuthFeatureChip(label = "Sync")
            }
        }
    }
}

@Composable
private fun AuthFeatureChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AuthModePill(isLogin: Boolean) {
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
            AuthModePillItem(
                label = "Sign in",
                selected = isLogin,
                modifier = Modifier.weight(1f)
            )
            AuthModePillItem(
                label = "Create account",
                selected = !isLogin,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthModePillItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
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
