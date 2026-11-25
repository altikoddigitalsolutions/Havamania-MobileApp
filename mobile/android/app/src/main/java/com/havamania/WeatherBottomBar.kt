package com.havamania

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isSpecial: Boolean = false
) {
    object Weather : BottomNavItem(Routes.WEATHER_ROOT, "Hava", Icons.Rounded.Cloud)
    object Calendar : BottomNavItem(Routes.CALENDAR_ROOT, "Takvim", Icons.Rounded.CalendarMonth)
    object AI : BottomNavItem(Routes.AI_ROOT, "Asistan", Icons.Rounded.AutoAwesome, isSpecial = true)
    object Profile : BottomNavItem(Routes.PROFILE_ROOT, "Profil", Icons.Rounded.Person)
}

@Composable
fun WeatherBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors
    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current

    val items = listOf(
        BottomNavItem.Weather,
        BottomNavItem.Calendar,
        BottomNavItem.AI,
        BottomNavItem.Profile
    )

    Surface(
        color = themeColors.surfaceGlass.copy(alpha = 0.94f),
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 0.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val labelWidth = (minOf(maxWidth, responsive.maxContentWidth) / 4 - 4.dp).coerceAtLeast(1.dp)
            Column(
                modifier = Modifier
                    .then(
                        if (windowSize.isTablet || windowSize.isLargeTablet)
                            Modifier.widthIn(max = responsive.maxContentWidth)
                        else Modifier.fillMaxWidth()
                    )
            ) {
                // Premium Divider Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(themeColors.border.copy(alpha = 0.1f))
            )

            NavigationBar(
                modifier = Modifier.heightIn(min = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 112.dp else 80.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                items.forEach { item ->
                    // Logic: Match base route precisely to avoid cross-tab activation
                    val isSelected = bottomNavigationOwner(currentRoute) == item.route

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
if (BuildConfig.DEBUG) {
                                android.util.Log.d("Navigation", "Tab Click: ${item.route}")
}
                            if (currentRoute?.substringBefore("?") != item.route) onNavigate(item.route)
                        },
                        label = {
                            Text(
                                text = item.title,
                                modifier = Modifier.requiredWidth(labelWidth),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = HavamaniaTheme.typography.label.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.sp
                                ),
                                color = if (isSelected) themeColors.accent else themeColors.textSecondary,
                                maxLines = 2
                            )
                        },
                        icon = {
                            val iconSize by animateDpAsState(targetValue = if (isSelected) 24.dp else 22.dp, label = "iconSize")

                            Box(contentAlignment = Alignment.Center) {


                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = if (isSelected) themeColors.accent else themeColors.textSecondary
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
}
