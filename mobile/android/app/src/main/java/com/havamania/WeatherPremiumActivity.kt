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
import com.havamania.ui.theme.HavamaniaTheme

class WeatherPremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HavamaniaTheme {
                var appState by remember { mutableStateOf("splash") }
                var currentRoute by remember { mutableStateOf("weather") }

                val atmosphereGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                )

                if (appState == "splash") {
                    TravelInspiredSplashScreen(onNavigateToHome = {
                        appState = "main"
                    })
                } else {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (currentRoute != "settings") {
                                WeatherBottomBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { currentRoute = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(atmosphereGradient)
                            .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            Crossfade(targetState = currentRoute, label = "main_nav") { target ->
                                when (target) {
                                    "weather" -> HomeScreen(
                                        onNavigateToAi = { currentRoute = "ai" }
                                    )
                                    "calendar" -> TravelPlannerScreen(onBack = { currentRoute = "weather" })
                                    "ai" -> AiChatScreen(onBack = { currentRoute = "weather" })
                                    "profile" -> ProfileScreen(onBack = { currentRoute = "weather" }, onNavigateToSettings = { currentRoute = "settings" })
                                    "settings" -> SettingsScreen(onBack = { currentRoute = "profile" })
                                    else -> HomeScreen(
                                        onNavigateToAi = { currentRoute = "ai" }
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
