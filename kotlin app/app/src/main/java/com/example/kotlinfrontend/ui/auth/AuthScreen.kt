package com.example.kotlinfrontend.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.AppBackground
import com.example.kotlinfrontend.ui.components.BannerTone
import com.example.kotlinfrontend.ui.components.HandIllustration
import com.example.kotlinfrontend.ui.components.IllustrationVariant
import com.example.kotlinfrontend.ui.components.InlineBanner
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandSurface
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

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = layoutSpec.horizontalPadding,
                    vertical = layoutSpec.verticalPadding
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = layoutSpec.contentMaxWidth)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "SignSpeak",
                    style = MaterialTheme.typography.headlineLarge,
                    color = BrandInk,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Learn faster, translate live, and keep your PSL journey organized.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandMuted
                )

                HandIllustration(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layoutSpec.illustrationHeight),
                    variant = IllustrationVariant.Auth
                )

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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AuthModeSwitch(
                            selectedMode = uiState.mode,
                            onModeSelected = viewModel::setMode
                        )
                        AnimatedContent(targetState = uiState.mode, label = "authMode") { mode ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (mode == AuthMode.Register) {
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
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                )

                                OutlinedTextField(
                                    value = uiState.password,
                                    onValueChange = viewModel::updatePassword,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation()
                                )

                                if (mode == AuthMode.Register) {
                                    OutlinedTextField(
                                        value = uiState.confirmPassword,
                                        onValueChange = viewModel::updateConfirmPassword,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Confirm password") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation()
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = viewModel::submit,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isWorking && uiState.sessionState.isConfigured
                        ) {
                            Text(
                                text = when {
                                    uiState.isWorking -> "Please wait..."
                                    uiState.mode == AuthMode.Login -> "Log In"
                                    else -> "Create Account"
                                }
                            )
                        }

                        if (uiState.mode == AuthMode.Login) {
                            TextButton(onClick = viewModel::openResetSheet) {
                                Text("Forgot password?")
                            }
                        } else {
                            Text(
                                text = "Account creation is instant in the app flow. You will enter the app as soon as Supabase accepts the signup.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandMuted
                            )
                        }
                    }
                }
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
                    text = "We’ll email a reset link to the address on your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Button(
                    onClick = viewModel::sendPasswordReset,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isWorking
                ) {
                    Text(if (uiState.isWorking) "Sending..." else "Send reset email")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AuthModeSwitch(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandBackground, RoundedCornerShape(22.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AuthMode.entries.forEach { mode ->
            val selected = mode == selectedMode
            Text(
                text = if (mode == AuthMode.Login) "Login" else "Register",
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selected) BrandPrimary else BrandSurface,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 12.dp),
                color = if (selected) BrandSurface else BrandInk,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
