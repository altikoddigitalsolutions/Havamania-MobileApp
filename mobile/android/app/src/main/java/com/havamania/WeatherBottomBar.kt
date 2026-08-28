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
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                items.forEach { item ->
                    // Logic: Match base route precisely to avoid cross-tab activation
                    val isSelected = when (item.route) {
                        Routes.WEATHER_ROOT -> currentRoute == Routes.WEATHER_ROOT
                        Routes.CALENDAR_ROOT -> currentRoute?.startsWith(Routes.CALENDAR_ROOT) == true
                        Routes.AI_ROOT -> currentRoute?.startsWith(Routes.AI_ROOT) == true || currentRoute?.startsWith(Routes.AI_HISTORY) == true
                        Routes.PROFILE_ROOT -> {
                            currentRoute == Routes.PROFILE_ROOT ||
                            currentRoute == Routes.EDIT_PROFILE ||
                            currentRoute == Routes.CITIES ||
                            currentRoute == Routes.SETTINGS
                        }
                        else -> currentRoute == item.route
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            android.util.Log.d("Navigation", "Tab Click: ${item.route}")
                            onNavigate(item.route)
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = HavamaniaTheme.typography.label.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isSelected) themeColors.accent else themeColors.textMuted,
                                maxLines = 1
                            )
                        },
                        icon = {
                            val iconSize by animateDpAsState(targetValue = if (isSelected) 24.dp else 22.dp, label = "iconSize")

                            Box(contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    // Subtle Glow behind icon
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .blur(10.dp)
                                            .background(themeColors.accent.copy(alpha = 0.15f), CircleShape)
                                    )
                                }

                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = if (isSelected) themeColors.accent else themeColors.textMuted.copy(alpha = 0.6f)
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
