package com.example.kotlinfrontend.ui.root

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.kotlinfrontend.ui.onboarding.OnboardingScreen
import com.example.kotlinfrontend.ui.theme.BrandAccent
import com.example.kotlinfrontend.ui.theme.BrandGlass
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandPrimaryDark
import com.example.kotlinfrontend.ui.theme.SurfaceContainerLow

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
            OnboardingScreen(onComplete = viewModel::completeOnboarding)
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
                .padding(horizontal = 28.dp, vertical = 52.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(132.dp),
                    shape = RoundedCornerShape(34.dp),
                    color = BrandGlass,
                    shadowElevation = 10.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = CircleShape,
                            color = SurfaceContainerLow
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.SignLanguage,
                                    contentDescription = null,
                                    tint = BrandPrimaryDark,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(52.dp)
                        .background(BrandAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SignLanguage,
                        contentDescription = null,
                        tint = BrandPrimaryDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = "SignSpeak",
                style = MaterialTheme.typography.displayLarge,
                color = BrandPrimaryDark,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "THE LIVING BRIDGE",
                style = MaterialTheme.typography.labelLarge,
                color = BrandMuted,
                modifier = Modifier.padding(top = 6.dp)
            )

            Row(
                modifier = Modifier.padding(top = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(BrandAccent, RoundedCornerShape(100))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(BrandPrimary.copy(alpha = 0.35f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(BrandPrimary.copy(alpha = 0.18f), CircleShape)
                )
            }

            Text(
                text = "Connecting Pakistan through PSL",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandMuted,
                modifier = Modifier.padding(top = 28.dp)
            )
        }
    }
}
