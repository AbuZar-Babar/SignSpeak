package com.example.kotlinfrontend.ui.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.ui.MainScaffold
import com.example.kotlinfrontend.ui.auth.AuthScreen
import com.example.kotlinfrontend.ui.common.appViewModelFactory
import com.example.kotlinfrontend.ui.components.AppBackground
import com.example.kotlinfrontend.ui.components.HandIllustration
import com.example.kotlinfrontend.ui.components.IllustrationVariant
import com.example.kotlinfrontend.ui.onboarding.OnboardingScreen
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted

private const val BootRoute = "root_boot"

@Composable
fun SignSpeakRoot(container: AppContainer) {
    val navController = rememberNavController()
    val viewModel: RootViewModel = viewModel(
        factory = appViewModelFactory {
            RootViewModel(
                authRepository = container.authRepository,
                localStore = container.localStore
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val targetDestination = uiState.destination

    LaunchedEffect(targetDestination, currentRoute) {
        val targetRoute = targetDestination?.route ?: return@LaunchedEffect
        if (currentRoute == targetRoute) {
            return@LaunchedEffect
        }

        navController.navigate(targetRoute) {
            launchSingleTop = true
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = BootRoute
    ) {
        composable(BootRoute) {
            BrandedLoadingScreen()
        }
        composable(RootDestination.Onboarding.route) {
            OnboardingScreen(
                onComplete = viewModel::completeOnboarding
            )
        }
        composable(RootDestination.Auth.route) {
            AuthScreen(container = container)
        }
        composable(RootDestination.Main.route) {
            MainScaffold(container = container)
        }
    }
}

@Composable
private fun BrandedLoadingScreen() {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(targetState = true, label = "boot") {
                HandIllustration(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    variant = IllustrationVariant.Auth
                )
            }
            Text(
                text = "SignSpeak",
                style = MaterialTheme.typography.displayLarge,
                color = BrandInk,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preparing your translation studio...",
                style = MaterialTheme.typography.bodyLarge,
                color = BrandMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
