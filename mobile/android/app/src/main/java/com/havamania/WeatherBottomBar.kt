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
        color = themeColors.surfaceGlass,
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, themeColors.border.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )

            NavigationBar(
                modifier = Modifier.height(72.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val iconSize by animateDpAsState(targetValue = if (isSelected) 26.dp else 22.dp)
                    val labelAlpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.6f)

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onNavigate(item.route) },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.graphicsLayer { alpha = labelAlpha },
                                color = if (isSelected) themeColors.accent else themeColors.textSecondary
                            )
                        },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .offset(y = (-18).dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(themeColors.accent)
                                    )
                                }

                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(iconSize),
                                    tint = if (isSelected) themeColors.accent else themeColors.textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
