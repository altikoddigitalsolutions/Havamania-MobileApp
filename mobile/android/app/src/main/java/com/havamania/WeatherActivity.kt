package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

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

class WeatherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            // We use the full path to ensure we call the @Composable function
            com.havamania.ui.theme.HavamaniaTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "weather"

                var appState by remember { mutableStateOf("splash") }
                val themeColors = com.havamania.ui.theme.HavamaniaTheme.colors
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
                            val hideBottomBarRoutes = listOf("settings", "edit_profile", "cities", "ai_history_detail", "notifications")
                            val shouldShowBottomBar = currentRoute !in hideBottomBarRoutes && !currentRoute.startsWith("ai_history_detail")

                            if (shouldShowBottomBar) {
                                WeatherBottomBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo("weather") { saveState = true }
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
                            .padding(bottom = if (currentRoute !in listOf("settings", "cities", "edit_profile", "notifications") && !currentRoute.startsWith("ai_history_detail")) innerPadding.calculateBottomPadding() else 0.dp)
                        ) {
                            NavHost(navController = navController, startDestination = "weather") {
                                composable(
                                    "weather",
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/weather" })
                                ) {
                                    HomeScreen(
                                        onNavigateToAi = { rec, data ->
                                            pendingRecommendation = rec
                                            activeWeatherData = data
                                            navController.navigate("ai")
                                        },
                                        onNavigateToNotifications = {
                                            navController.navigate("notifications")
                                        }
                                    )
                                }
                                composable(
                                    "notifications",
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/Notifications" })
                                ) {
                                    NotificationCenterScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToDetail = { screen: String, params: Map<String, String>? ->
                                            try {
                                                if (screen == "calendar") {
                                                    val focusId = params?.get("focusId") ?: ""
                                                    val highlight = params?.get("highlight") ?: ""
                                                    navController.navigate("calendar?focusId=$focusId&highlight=$highlight") {
                                                        launchSingleTop = true
                                                    }
                                                } else {
                                                    navController.navigate(screen) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("Nav", "Failed to navigate to $screen", e)
                                                navController.navigate("weather") {
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }
                                composable(
                                    "calendar?focusId={focusId}&highlight={highlight}",
                                    arguments = listOf(
                                        navArgument("focusId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("highlight") { type = NavType.StringType; nullable = true; defaultValue = null }
                                    ),
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/calendar?focusId={focusId}&highlight={highlight}" })
                                ) { backStackEntry ->
                                    val focusId = backStackEntry.arguments?.getString("focusId")
                                    val highlight = backStackEntry.arguments?.getString("highlight")
                                    TravelPlannerScreen(
                                        onBack = { navController.popBackStack() },
                                        focusId = focusId,
                                        highlight = highlight
                                    )
                                }
                                composable(
                                    "ai",
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/AIChat" })
                                ) {
                                    AiChatScreen(
                                        initialRecommendation = pendingRecommendation,
                                        weatherData = activeWeatherData,
                                        onBack = {
                                            pendingRecommendation = null
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable("profile") {
                                    ProfileScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToSettings = { navController.navigate("settings") },
                                        onNavigateToCities = { navController.navigate("cities") },
                                        onNavigateToAiHistory = { navController.navigate("ai_history") },
                                        onNavigateToEditProfile = { navController.navigate("edit_profile") },
                                        onNavigateToTravels = { navController.navigate("calendar") },
                                        onNavigateToNotifications = { navController.navigate("notifications") }
                                    )
                                }
                                composable("cities") {
                                    CitiesManagementScreen(onBack = { navController.popBackStack() })
                                }
                                composable("ai_history") {
                                    AiHistoryScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToDetail = { id ->
                                            navController.navigate("ai_history_detail/$id")
                                        }
                                    )
                                }
                                composable("ai_history_detail/{itemId}") { backStackEntry ->
                                    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                                    AiHistoryDetailScreen(
                                        itemId = itemId,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("edit_profile") {
                                    EditProfileScreen(onBack = { navController.popBackStack() })
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToEditProfile = { navController.navigate("edit_profile") },
                                        onNavigateToCities = { navController.navigate("cities") }
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
