package com.example.kotlinfrontend.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ResponsiveLayoutSpec(
    val screenWidthDp: Int,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val bottomBarHorizontalPadding: Dp,
    val bottomBarVerticalPadding: Dp,
    val illustrationHeight: Dp,
    val contentMaxWidth: Dp
) {
    val isCompact: Boolean
        get() = screenWidthDp < 360
}

@Composable
fun rememberResponsiveLayoutSpec(): ResponsiveLayoutSpec {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth >= 840 -> ResponsiveLayoutSpec(
            screenWidthDp = screenWidth,
            horizontalPadding = 32.dp,
            verticalPadding = 28.dp,
            bottomBarHorizontalPadding = 32.dp,
            bottomBarVerticalPadding = 18.dp,
            illustrationHeight = 340.dp,
            contentMaxWidth = 760.dp
        )

        screenWidth >= 600 -> ResponsiveLayoutSpec(
            screenWidthDp = screenWidth,
            horizontalPadding = 28.dp,
            verticalPadding = 26.dp,
            bottomBarHorizontalPadding = 24.dp,
            bottomBarVerticalPadding = 16.dp,
            illustrationHeight = 320.dp,
            contentMaxWidth = 680.dp
        )

        screenWidth >= 400 -> ResponsiveLayoutSpec(
            screenWidthDp = screenWidth,
            horizontalPadding = 22.dp,
            verticalPadding = 24.dp,
            bottomBarHorizontalPadding = 18.dp,
            bottomBarVerticalPadding = 14.dp,
            illustrationHeight = 280.dp,
            contentMaxWidth = 560.dp
        )

        else -> ResponsiveLayoutSpec(
            screenWidthDp = screenWidth,
            horizontalPadding = 18.dp,
            verticalPadding = 20.dp,
            bottomBarHorizontalPadding = 14.dp,
            bottomBarVerticalPadding = 12.dp,
            illustrationHeight = 240.dp,
            contentMaxWidth = 520.dp
        )
    }
}
