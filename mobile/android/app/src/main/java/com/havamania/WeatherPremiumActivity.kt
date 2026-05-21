package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme

import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import androidx.navigation.navDeepLink
import androidx.navigation.navArgument
import androidx.navigation.NavType

class WeatherPremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HavamaniaTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Routes.WEATHER

                var appState by remember { mutableStateOf("splash") }
                val themeColors = HavamaniaTheme.colors
                val backgroundGradient = remember(themeColors) {
                    Brush.verticalGradient(themeColors.gradientPrimary)
                }

                var pendingRecommendation by remember { mutableStateOf<HavamaniaRecommendation?>(null) }
                var activeWeatherData by remember { mutableStateOf<WeatherData?>(null) }

                if (appState == "splash") {
                    TravelInspiredSplashScreen(onNavigateToHome = {
                        appState = "main"
                    })
                } else {
                    Scaffold(
                        containerColor = themeColors.background,
                        bottomBar = {
                            val hideBottomBarRoutes = listOf(
                                Routes.SETTINGS,
                                Routes.EDIT_PROFILE,
                                Routes.CITIES,
                                "ai_history_detail",
                                Routes.NOTIFICATION_CENTER
                            )
                            val shouldShowBottomBar = currentRoute !in hideBottomBarRoutes && !currentRoute.startsWith("ai_history_detail")

                            if (shouldShowBottomBar) {
                                WeatherBottomBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo(Routes.WEATHER) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundGradient)
                            .padding(bottom = if (currentRoute !in listOf(Routes.SETTINGS, Routes.CITIES, Routes.EDIT_PROFILE, Routes.NOTIFICATION_CENTER) && !currentRoute.startsWith("ai_history_detail")) innerPadding.calculateBottomPadding() else 0.dp)
                        ) {
                            NavHost(navController = navController, startDestination = Routes.WEATHER) {
                                composable(
                                    Routes.WEATHER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/weather" })
                                ) {
                                    HomeScreen(onNavigateToAi = { rec, data ->
                                        pendingRecommendation = rec
                                        activeWeatherData = data
                                        navController.navigate(Routes.AI)
                                    }, onNavigateToNotifications = {
                                        navController.navigate(Routes.NOTIFICATION_CENTER)
                                    })
                                }
                                composable(
                                    "${Routes.CALENDAR}?focusId={focusId}",
                                    arguments = listOf(
                                        navArgument("focusId") { type = NavType.StringType; nullable = true; defaultValue = null }
                                    ),
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/calendar?focusId={focusId}" })
                                ) { backStackEntry ->
                                    val focusId = backStackEntry.arguments?.getString("focusId")
                                    TravelPlannerScreen(
                                        onBack = { navController.popBackStack() },
                                        focusId = focusId
                                    )
                                }
                                composable(Routes.AI) {
                                    AiChatScreen(
                                        initialRecommendation = pendingRecommendation,
                                        weatherData = activeWeatherData,
                                        onBack = {
                                            pendingRecommendation = null
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(Routes.PROFILE) {
                                    ProfileScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                                        onNavigateToCities = { navController.navigate(Routes.CITIES) },
                                        onNavigateToAiHistory = { navController.navigate(Routes.AI_HISTORY) },
                                        onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                                        onNavigateToTravels = { navController.navigate(Routes.CALENDAR) }
                                    )
                                }
                                composable(Routes.CITIES) {
                                    CitiesManagementScreen(onBack = { navController.popBackStack() })
                                }
                                composable(Routes.AI_HISTORY) {
                                    AiHistoryScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToDetail = { id ->
                                            navController.navigate("ai_history_detail/$id")
                                        }
                                    )
                                }
                                composable(Routes.AI_HISTORY_DETAIL) { backStackEntry ->
                                    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                                    AiHistoryDetailScreen(
                                        itemId = itemId,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(Routes.EDIT_PROFILE) {
                                    EditProfileScreen(onBack = { navController.popBackStack() })
                                }
                                composable(Routes.SETTINGS) {
                                    SettingsScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                                        onNavigateToCities = { navController.navigate(Routes.CITIES) }
                                    )
                                }
                                composable(Routes.NOTIFICATION_CENTER) {
                                    NotificationCenterScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToDetail = { screen, params ->
                                            try {
                                                navController.navigate(screen) {
                                                    launchSingleTop = true
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("Nav", "Failed to navigate to $screen", e)
                                                navController.navigate(Routes.WEATHER) {
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
