package com.example.kotlinfrontend.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = layoutSpec.contentMaxWidth)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(
                    modifier = Modifier.padding(top = 18.dp),
                    shape = CircleShape,
                    color = BrandPrimary.copy(alpha = 0.18f)
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SignLanguage,
                            contentDescription = null,
                            tint = BrandPrimaryDark
                        )
                    }
                }

                Text(
                    text = "SignSpeak",
                    style = MaterialTheme.typography.displayMedium,
                    color = BrandPrimaryDark,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Bridge the gap. Connect through signs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted
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
                            Text("Forgot Password?")
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
                    shape = RoundedCornerShape(24.dp),
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
                            .background(BrandMuted.copy(alpha = 0.15f))
                    )
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandMuted,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(BrandMuted.copy(alpha = 0.15f))
                    )
                }

                Button(
                    onClick = { viewModel.setMode(if (isLogin) AuthMode.Register else AuthMode.Login) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerLow,
                        contentColor = BrandInk
                    )
                ) {
                    Text(
                        text = if (isLogin) "Register New Account" else "Already Have An Account",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
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
                    modifier = Modifier.fillMaxWidth(),
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
