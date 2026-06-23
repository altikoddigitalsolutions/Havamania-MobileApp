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
import com.havamania.ui.theme.HavamaniaTheme

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isSpecial: Boolean = false
) {
    object Weather : BottomNavItem("weather", "Hava", Icons.Rounded.Cloud)
    object Calendar : BottomNavItem("calendar", "Takvim", Icons.Rounded.CalendarMonth)
    object AI : BottomNavItem("ai", "Asistan", Icons.Rounded.AutoAwesome, isSpecial = true)
    object Profile : BottomNavItem("profile", "Profil", Icons.Rounded.Person)
}

@Composable
fun WeatherBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors
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
        Column {
            // Premium Divider Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(themeColors.border.copy(alpha = 0.1f))
            )

            NavigationBar(
                modifier = Modifier.height(80.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                items.forEach { item ->
                    val isSelected = when (item.route) {
                        Routes.WEATHER -> currentRoute == Routes.WEATHER
                        Routes.CALENDAR -> currentRoute?.startsWith(Routes.CALENDAR) == true
                        Routes.AI -> currentRoute == Routes.AI || currentRoute?.startsWith("ai_history") == true
                        Routes.PROFILE -> currentRoute == Routes.PROFILE
                        else -> currentRoute == item.route
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onNavigate(item.route) },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = if (isSelected) themeColors.accent else themeColors.textSecondary.copy(alpha = 0.7f)
                            )
                        },
                        icon = {
                            val iconSize by animateDpAsState(targetValue = if (isSelected) 24.dp else 22.dp)

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) themeColors.accent else Color.Transparent)
                            ) {
                                if (isSelected) {
                                    // Glow effect for extra premium feel
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(8.dp)
                                            .background(themeColors.accent.copy(alpha = 0.3f), CircleShape)
                                    )
                                }

                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(iconSize),
                                    tint = if (isSelected) Color.White else themeColors.textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = Color.White,
                            unselectedIconColor = themeColors.textSecondary.copy(alpha = 0.6f),
                            selectedTextColor = themeColors.accent,
                            unselectedTextColor = themeColors.textSecondary.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}
