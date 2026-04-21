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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
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
    History("history", "History", Icons.Rounded.History)
}

private const val ProfileRoute = "profile"

@Composable
fun MainScaffold(container: AppContainer) {
    val navController = rememberNavController()
    val layoutSpec = rememberResponsiveLayoutSpec()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = MainTab.entries.firstOrNull { it.route == currentRoute }
    var drawerModelOptionsContent by remember {
        mutableStateOf<(@Composable () -> Unit)?>(null)
    }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
    }

    AppBackground {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = BrandPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )

                        if (drawerModelOptionsContent != null) {
                            Text(
                                text = "Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandInk,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            drawerModelOptionsContent?.invoke()
                        } else {
                            Text(
                                text = "No adjustable translation settings are available right now.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    LivingBridgeBottomBar(
                        currentTab = currentTab,
                        maxWidth = layoutSpec.contentMaxWidth,
                        onTabSelected = { tab ->
                            navigateTo(tab.route)
                        }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = MainTab.Translate.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                ) {
                    composable(MainTab.Translate.route) {
                        TranslateScreen(
                            container = container,
                            onMenuClick = ::openDrawer,
                            onAvatarClick = { navigateTo(ProfileRoute) },
                            onModelOptionsContentChange = { content ->
                                drawerModelOptionsContent = content
                            }
                        )
                    }
                    composable(MainTab.Dictionary.route) {
                        DictionaryScreen(
                            container = container,
                            onMenuClick = ::openDrawer,
                            onAvatarClick = { navigateTo(ProfileRoute) }
                        )
                    }
                    composable(MainTab.History.route) {
                        HistoryScreen(
                            container = container,
                            onMenuClick = ::openDrawer,
                            onAvatarClick = { navigateTo(ProfileRoute) }
                        )
                    }
                    composable(ProfileRoute) {
                        ProfileScreen(
                            container = container,
                            onMenuClick = ::openDrawer,
                            onAvatarClick = { navigateTo(ProfileRoute) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LivingBridgeBottomBar(
    currentTab: MainTab?,
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
