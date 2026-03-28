package com.example.kotlinfrontend.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kotlinfrontend.app.AppContainer
import com.example.kotlinfrontend.ui.components.AppBackground
import com.example.kotlinfrontend.ui.dictionary.DictionaryScreen
import com.example.kotlinfrontend.ui.history.HistoryScreen
import com.example.kotlinfrontend.ui.profile.ProfileScreen
import com.example.kotlinfrontend.ui.theme.BrandAccent
import com.example.kotlinfrontend.ui.theme.BrandGlass
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimaryDark
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec
import com.example.kotlinfrontend.ui.translate.TranslateScreen

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Translate("translate", "Translate", Icons.Rounded.SignLanguage),
    Dictionary("dictionary", "Dictionary", Icons.Rounded.AutoStories),
    History("history", "History", Icons.Rounded.History),
    Profile("profile", "Profile", Icons.Rounded.Person)
}

@Composable
fun MainScaffold(container: AppContainer) {
    val navController = rememberNavController()
    val layoutSpec = rememberResponsiveLayoutSpec()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = MainTab.entries.firstOrNull { it.route == currentRoute } ?: MainTab.Translate

    AppBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                LivingBridgeBottomBar(
                    currentTab = currentTab,
                    maxWidth = layoutSpec.contentMaxWidth,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MainTab.Translate.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(MainTab.Translate.route) { TranslateScreen(container = container) }
                composable(MainTab.Dictionary.route) { DictionaryScreen(container = container) }
                composable(MainTab.History.route) { HistoryScreen(container = container) }
                composable(MainTab.Profile.route) { ProfileScreen(container = container) }
            }
        }
    }
}

@Composable
private fun LivingBridgeBottomBar(
    currentTab: MainTab,
    maxWidth: androidx.compose.ui.unit.Dp,
    onTabSelected: (MainTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxWidth),
            color = BrandGlass,
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    LivingBridgeNavItem(
                        tab = tab,
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LivingBridgeNavItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint by animateColorAsState(
        targetValue = if (selected) BrandPrimaryDark else BrandMuted,
        label = "navIcon"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) BrandPrimaryDark else BrandMuted,
        label = "navLabel"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            color = if (selected) BrandAccent else Color.Transparent,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
