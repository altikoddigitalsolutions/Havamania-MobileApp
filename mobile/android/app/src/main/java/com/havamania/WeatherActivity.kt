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
import com.havamania.*

import androidx.navigation.NavGraph.Companion.findStartDestination
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

            com.havamania.ui.theme.HavamaniaTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                // currentRoute tracks the route pattern (not actual URL)
                val currentRoute = navBackStackEntry?.destination?.route ?: Routes.WEATHER_ROOT

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
                            val hideBottomBarRoutes = listOf(Routes.SETTINGS, Routes.EDIT_PROFILE, Routes.CITIES, Routes.NOTIFICATION_CENTER, Routes.AI_HISTORY)
                            val isDetailRoute = currentRoute.startsWith("sub_ai_history_detail")
                            val shouldShowBottomBar = currentRoute !in hideBottomBarRoutes && !isDetailRoute

                            if (shouldShowBottomBar) {
                                WeatherBottomBar(
                                    currentRoute = currentRoute,
                                onNavigate = { route ->
                                    android.util.Log.d("Navigation", "BottomBar Action: $route | From: $currentRoute")

                                    val startDestId = navController.graph.findStartDestination().id
                                    // Hava ve Profil daima root'a dönmeli.
                                    val shouldResetState = route == Routes.WEATHER_ROOT || route == Routes.PROFILE_ROOT

                                    navController.navigate(route) {
                                        popUpTo(startDestId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = !shouldResetState
                                    }
                                }
                                )
                            }
                        }
                    ) { innerPadding ->
                        val hideBottomBarRoutes = listOf(Routes.SETTINGS, Routes.EDIT_PROFILE, Routes.CITIES, Routes.NOTIFICATION_CENTER, Routes.AI_HISTORY)
                        val isDetailRoute = currentRoute.startsWith("sub_ai_history_detail")
                        val isBottomBarHidden = currentRoute in hideBottomBarRoutes || isDetailRoute

                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundGradient)
                            .padding(bottom = if (!isBottomBarHidden) innerPadding.calculateBottomPadding() else 0.dp)
                        ) {
                            NavHost(navController = navController, startDestination = Routes.WEATHER_ROOT) {
                                // 1. WEATHER TAB
                                composable(
                                    Routes.WEATHER_ROOT,
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/weather" })
                                ) {
                                    HomeScreen(
                                        onNavigateToAi = { rec, data ->
                                            pendingRecommendation = rec
                                            activeWeatherData = data
                                            val startDestId = navController.graph.findStartDestination().id
                                            navController.navigate(Routes.AI_ROOT) {
                                                popUpTo(startDestId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onNavigateToNotifications = {
                                            navController.navigate(Routes.NOTIFICATION_CENTER)
                                        }
                                    )
                                }

                                // 2. CALENDAR TAB (TRAVEL PLANNER)
                                composable(
                                    Routes.CALENDAR_ROOT + "?focusId={focusId}&highlight={highlight}&city={city}&start={start}",
                                    arguments = listOf(
                                        navArgument("focusId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("highlight") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("city") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("start") { type = NavType.StringType; nullable = true; defaultValue = null }
                                    ),
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/calendar?focusId={focusId}&highlight={highlight}&city={city}&start={start}" })
                                ) { backStackEntry ->
                                    val focusId = backStackEntry.arguments?.getString("focusId")
                                    val highlight = backStackEntry.arguments?.getString("highlight")
                                    val city = backStackEntry.arguments?.getString("city")
                                    val start = backStackEntry.arguments?.getString("start")

                                    TravelPlannerScreen(
                                        onBack = { navController.popBackStack() },
                                        focusId = focusId,
                                        highlight = highlight,
                                        initialCity = city,
                                        initialStartDate = start
                                    )
                                }

                                // 3. AI TAB
                                composable(
                                    Routes.AI_ROOT,
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/AIChat" })
                                ) {
                                    AiChatScreen(
                                        initialRecommendation = pendingRecommendation,
                                        onBack = {
                                            pendingRecommendation = null
                                            navController.popBackStack()
                                        },
                                        onNavigateToTravelCreate = { city, start ->
                                            navController.navigate(Routes.CALENDAR_ROOT + "?city=$city&start=$start") {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }

                                // 4. PROFILE TAB
                                composable(Routes.PROFILE_ROOT) {
                                    ProfileScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                                        onNavigateToCities = { navController.navigate(Routes.CITIES) },
                                        onNavigateToAiHistory = { navController.navigate(Routes.AI_HISTORY) },
                                        onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                                        onNavigateToTravels = {
                                            val startDestId = navController.graph.findStartDestination().id
                                            navController.navigate(Routes.CALENDAR_ROOT) {
                                                popUpTo(startDestId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATION_CENTER) }
                                    )
                                }

                                // --- SUB SCREENS ---
                                composable(Routes.CITIES) {
                                    CitiesManagementScreen(onBack = { navController.popBackStack() })
                                }
                                composable(Routes.AI_HISTORY) {
                                    AiHistoryScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToChat = { id ->
                                            navController.navigate(Routes.AI_ROOT.replace("{conversationId}", id))
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
                                        onNavigateToCities = { navController.navigate(Routes.CITIES) },
                                        onNavigateToLegal = { title, url ->
                                            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                            navController.navigate(
                                                Routes.LEGAL_WEBVIEW
                                                    .replace("{title}", title)
                                                    .replace("{url}", encodedUrl)
                                            )
                                        },
                                        onNavigateToSmartAlerts = { navController.navigate(Routes.SMART_ALERTS) }
                                    )
                                }
                                composable(Routes.NOTIFICATION_CENTER) {
                                    NotificationCenterScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToDetail = { screen, _ -> navController.navigate(screen) }
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
