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

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class WeatherPremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // We use the full path to ensure we call the @Composable function
            com.havamania.ui.theme.HavamaniaTheme {
                val navController = rememberNavController()
                var appState by remember { mutableStateOf("splash") }

                // Track current route to show/hide bottom bar
                val currentRoute = remember(navController) {
                    mutableStateOf("weather")
                }

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        currentRoute.value = entry.destination.route ?: "weather"
                    }
                }

                val themeColors = com.havamania.ui.theme.HavamaniaTheme.colors
                val backgroundGradient = Brush.verticalGradient(themeColors.gradientPrimary)

                var pendingRecommendation by remember { mutableStateOf<HavamaniaRecommendation?>(null) }

                if (appState == "splash") {
                    TravelInspiredSplashScreen(onNavigateToHome = {
                        appState = "main"
                    })
                } else {
                    Scaffold(
                        containerColor = themeColors.background,
                        bottomBar = {
                            if (currentRoute.value != "settings") {
                                WeatherBottomBar(
                                    currentRoute = currentRoute.value,
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
                            .padding(bottom = if (currentRoute.value != "settings") innerPadding.calculateBottomPadding() else 0.dp)
                        ) {
                            NavHost(navController = navController, startDestination = "weather") {
                                composable("weather") {
                                    HomeScreen(onNavigateToAi = { rec ->
                                        pendingRecommendation = rec
                                        navController.navigate("ai")
                                    })
                                }
                                composable("calendar") {
                                    TravelPlannerScreen(onBack = { navController.popBackStack() })
                                }
                                composable("ai") {
                                    AiChatScreen(
                                        initialRecommendation = pendingRecommendation,
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
                                        onNavigateToTravels = { navController.navigate("calendar") }
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
                                    SettingsScreen(onBack = { navController.popBackStack() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
