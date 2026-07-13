package com.havamania.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class HavamaniaWindowSize(
    val widthDp: Int,
    val heightDp: Int,
    val isCompact: Boolean,
    val isPhone: Boolean,
    val isTablet: Boolean,
    val isLargeTablet: Boolean
)

@Immutable
data class HavamaniaResponsiveValues(
    val pagePadding: Dp,
    val cardRadius: Dp,
    val headerFontSize: TextUnit,
    val bodyFontSize: TextUnit,
    val iconSizeLarge: Dp,
    val iconSizeMedium: Dp,
    val maxContentWidth: Dp
)

val LocalWindowSize = staticCompositionLocalOf<HavamaniaWindowSize> {
    error("No WindowSize provided")
}

val LocalResponsiveValues = staticCompositionLocalOf<HavamaniaResponsiveValues> {
    error("No ResponsiveValues provided")
}

@Composable
fun ProvideResponsiveLayout(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp

    val windowSize = HavamaniaWindowSize(
        widthDp = widthDp,
        heightDp = heightDp,
        isCompact = widthDp < 360,
        isPhone = widthDp in 360..599,
        isTablet = widthDp in 600..899,
        isLargeTablet = widthDp >= 900
    )

    val responsiveValues = HavamaniaResponsiveValues(
        pagePadding = if (windowSize.isTablet || windowSize.isLargeTablet) 40.dp else if (windowSize.isCompact) 12.dp else 20.dp,
        cardRadius = if (windowSize.isCompact) 16.dp else 24.dp,
        headerFontSize = if (windowSize.isTablet) 28.sp else if (windowSize.isCompact) 18.sp else 22.sp,
        bodyFontSize = if (windowSize.isTablet) 18.sp else if (windowSize.isCompact) 14.sp else 16.sp,
        iconSizeLarge = if (windowSize.isTablet) 48.dp else 40.dp,
        iconSizeMedium = if (windowSize.isTablet) 32.dp else 24.dp,
        maxContentWidth = if (windowSize.isTablet || windowSize.isLargeTablet) 800.dp else Dp.Unspecified
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalWindowSize provides windowSize,
        LocalResponsiveValues provides responsiveValues
    ) {
        content()
    }
}
