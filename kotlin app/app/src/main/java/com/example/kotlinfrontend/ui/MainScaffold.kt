package com.example.kotlinfrontend.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SignLanguage
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.example.kotlinfrontend.ui.motion.MotionTokens
import com.example.kotlinfrontend.ui.profile.ProfileScreen
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.BrandPrimaryLight
import com.example.kotlinfrontend.ui.theme.BrandStroke
import com.example.kotlinfrontend.ui.theme.BrandSurface
import com.example.kotlinfrontend.ui.theme.ResponsiveLayoutSpec
import com.example.kotlinfrontend.ui.theme.rememberResponsiveLayoutSpec
import com.example.kotlinfrontend.ui.translate.TranslateScreen

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Translate ("translate",  "Translate",  Icons.Rounded.SignLanguage),
    Dictionary("dictionary", "Dictionary", Icons.Rounded.AutoStories),
    History   ("history",    "History",    Icons.Rounded.History),
    Profile   ("profile",    "Profile",    Icons.Rounded.Person)
}

@Composable
fun MainScaffold(container: AppContainer) {
    val navController = rememberNavController()
    val layoutSpec    = rememberResponsiveLayoutSpec()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab   = MainTab.entries.firstOrNull { it.route == currentRoute } ?: MainTab.Translate

    AppBackground {
        Scaffold(
            modifier       = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                DuolingoBottomBar(
                    currentTab      = currentTab,
                    layoutSpec      = layoutSpec,
                    onTabSelected   = { tab ->
                        navController.navigate(tab.route) {
                            launchSingleTop = true
                            restoreState    = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = MainTab.Translate.route,
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(MainTab.Translate.route)  { TranslateScreen(container = container) }
                composable(MainTab.Dictionary.route) { DictionaryScreen(container = container) }
                composable(MainTab.History.route)    { HistoryScreen(container = container) }
                composable(MainTab.Profile.route)    { ProfileScreen(container = container) }
            }
        }
    }
}

@Composable
private fun DuolingoBottomBar(
    currentTab: MainTab,
    layoutSpec: ResponsiveLayoutSpec,
    onTabSelected: (MainTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, spotColor = BrandInk.copy(alpha = 0.08f))
            .background(BrandSurface)
    ) {
        HorizontalDivider(color = BrandStroke, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = layoutSpec.bottomBarHorizontalPadding, vertical = 8.dp)
                .widthIn(max = layoutSpec.contentMaxWidth),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.entries.forEach { tab ->
                DuolingoNavItem(
                    tab      = tab,
                    selected = currentTab == tab,
                    onClick  = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DuolingoNavItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue   = if (selected) 1f else 0.93f,
        animationSpec = MotionTokens.bouncySpring(),
        label         = "tabScale"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (selected) BrandPrimary else BrandMuted,
        animationSpec = MotionTokens.standardTween(),
        label         = "tabTint"
    )
    val labelColor by animateColorAsState(
        targetValue   = if (selected) BrandPrimary else BrandMuted,
        animationSpec = MotionTokens.standardTween(),
        label         = "labelColor"
    )
    val pillHeight by animateDpAsState(
        targetValue   = if (selected) 40.dp else 36.dp,
        animationSpec = MotionTokens.standardTween(),
        label         = "pillHeight"
    )

    Column(
        modifier = modifier
            .scale(animatedScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon pill — green filled background when selected
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(pillHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) BrandPrimaryLight else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = tab.icon,
                contentDescription = tab.label,
                tint               = iconTint,
                modifier           = Modifier.size(24.dp)
            )
        }

        // Label
        Text(
            text       = tab.label,
            style      = MaterialTheme.typography.labelSmall,
            color      = labelColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
