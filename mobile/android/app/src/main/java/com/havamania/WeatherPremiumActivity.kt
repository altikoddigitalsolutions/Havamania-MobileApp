package com.havamania

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.HavamaniaTheme
import com.havamania.ui.theme.ThemeViewModel
import com.havamania.*

import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import androidx.navigation.navDeepLink
import androidx.navigation.navArgument
import androidx.navigation.NavType
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class WeatherPremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Initialize MapLibre before any MapView is created
        Log.i("MapInit", "Initializing MapLibre getInstance")
        try {
            MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
            Log.i("MapInit", "MapLibre initialized successfully")
        } catch (e: Exception) {
            Log.e("MapInit", "MapLibre initialization FAILED", e)
        }
        var isReady by mutableStateOf(false)
        var splashMinimumTimedOut by mutableStateOf(false)

        // Keep system splash on screen until basic data is ready OR min timeout
        splashScreen.setKeepOnScreenCondition {
            !isReady && !splashMinimumTimedOut
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            HavamaniaTheme {
                val authViewModel: AuthViewModel = viewModel()
                val profileViewModel: ProfileViewModel = viewModel()
                val themeViewModel: ThemeViewModel = viewModel()

                val authState by authViewModel.authState.collectAsState()
                val profileState by profileViewModel.profileState.collectAsState()

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Routes.WEATHER_ROOT

                // Profile Sync Logic
                LaunchedEffect(profileState) {
                    val state = profileState
                    if (state is ProfileState.Success) {
                        themeViewModel.syncWithFirebase(state.profile)
                    }
                }

                var appState by remember { mutableStateOf("splash") }

                // Splash Screen Minimum Duration Timer
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200) // Lowered for faster cold start, custom splash handles the rest
                    splashMinimumTimedOut = true
                }
                val themeColors = HavamaniaTheme.colors
                val backgroundGradient = remember(themeColors) {
                    Brush.verticalGradient(themeColors.gradientPrimary)
                }

                // Startup Logic: Execute critical path before showing anything
                LaunchedEffect(authState) {
                    val currentUser = authViewModel.currentUser
                    if (currentUser == null) {
                        themeViewModel.clearLocalUserData()
                        isReady = true // Show login/welcome
                    } else {
                        // Giriş yapılmışsa profili bekle
                        profileViewModel.fetchProfile()
                        themeViewModel.checkInitialLocationMode()
                    }
                }

                LaunchedEffect(profileState) {
                    val state = profileState
                    if (state is ProfileState.Success || state is ProfileState.Error) {
                        isReady = true // Veri hazır veya hata olsa bile artık içeri al
                    }
                }

                // Auth Redirection Logic
                LaunchedEffect(appState, authState, isReady, profileState) {
                    if (isReady && appState == "main") {
                        val currentUser = authViewModel.currentUser
                        val state = profileState

                        if (currentUser == null) {
                            if (currentRoute !in listOf(Routes.AUTH_WELCOME, Routes.LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD)) {
                                try {
                                    navController.navigate(Routes.AUTH_WELCOME) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Nav", "Initial redirection failed", e)
                                }
                            }
                        } else if (state is ProfileState.Success) {
                            // If in Auth root, move to Main root
                            if (currentRoute in listOf(Routes.AUTH_WELCOME, Routes.LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD)) {
                                val profile = state.profile
                                val target = if (!profile.onboardingCompleted) Routes.PERSONALIZATION else Routes.WEATHER_ROOT

                                try {
                                    navController.navigate(target) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Nav", "Main redirection failed", e)
                                }
                            }
                        }
                    }
                }

                // Havamania Splash (Sloganlı) süresini ayarla
                LaunchedEffect(isReady, splashMinimumTimedOut) {
                    if (isReady && splashMinimumTimedOut) {
                        // Uygulama verisi hazır VE minimum süre dolduysa ana ekrana geçiş yapıyoruz.
                        appState = "main"
                    }
                }

                var pendingRecommendation by remember { mutableStateOf<HavamaniaRecommendation?>(null) }
                var activeWeatherData by remember { mutableStateOf<WeatherData?>(null) }

                // Splash Screen Logic
                if (appState == "splash") {
                    TravelInspiredSplashScreen(
                        onNavigate = {
                            appState = "main"
                        },
                        isReady = isReady
                    )
                } else {
                    val hideBottomBarRoutes = listOf(
                        Routes.AUTH_WELCOME,
                        Routes.LOGIN,
                        Routes.REGISTER,
                        Routes.FORGOT_PASSWORD,
                        Routes.SETTINGS,
                        Routes.EDIT_PROFILE,
                        Routes.CITIES,
                        Routes.AI_HISTORY,
                        Routes.NOTIFICATION_CENTER
                    )
                    val shouldShowBottomBar = currentRoute !in hideBottomBarRoutes && !currentRoute.startsWith("sub_ai_history_detail") && !currentRoute.startsWith("sub_route_weather")

                    Scaffold(
                        containerColor = Color.Transparent, // Managed by HavamaniaScreen
                        bottomBar = {
                            if (shouldShowBottomBar) {
                                WeatherBottomBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        try {
                                            val startDestId = navController.graph.findStartDestination().id
                                            val shouldResetState = route == Routes.WEATHER_ROOT || route == Routes.PROFILE_ROOT || route == Routes.AI_ROOT

                                            navController.navigate(route) {
                                                popUpTo(startDestId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = !shouldResetState
                                            }
                                        } catch (e: Exception) {
                                            Log.e("Nav", "Navigation failed to $route", e)
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        // The background gradient is now handled globally in HavamaniaScreen
                        // for each page to allow specific overrides if needed.
                        // We use a root Box here for common layout values.
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (shouldShowBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = if (authViewModel.currentUser != null) Routes.WEATHER_ROOT else Routes.AUTH_WELCOME
                            ) {
                                // --- AUTH ROUTES ---
                                composable(Routes.AUTH_WELCOME) {
                                    AuthWelcomeScreen(
                                        onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                                        onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                                    )
                                }
                                composable(Routes.LOGIN) {
                                    LoginScreen(
                                        viewModel = authViewModel,
                                        onBack = { navController.popBackStack() },
                                        onNavigateToRegister = {
                                            navController.navigate(Routes.REGISTER) {
                                                popUpTo(Routes.AUTH_WELCOME)
                                            }
                                        },
                                        onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                                    )
                                }
                                composable(Routes.REGISTER) {
                                    RegisterScreen(
                                        viewModel = authViewModel,
                                        onBack = { navController.popBackStack() },
                                        onNavigateToLogin = {
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(Routes.AUTH_WELCOME)
                                            }
                                        }
                                    )
                                }
                                composable(Routes.FORGOT_PASSWORD) {
                                    ForgotPasswordScreen(
                                        viewModel = authViewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                // --- APP ROUTES ---
                                composable(
                                    Routes.WEATHER_ROOT,
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/weather" })
                                ) {
                                    HomeScreen(onNavigateToAi = { rec: HavamaniaRecommendation, data: WeatherData? ->
                                        pendingRecommendation = rec
                                        activeWeatherData = data
                                        navController.navigate(Routes.AI_ROOT)
                                    }, onNavigateToNotifications = {
                                        navController.navigate(Routes.NOTIFICATION_CENTER)
                                    })
                                }
                                composable(
                                    "${Routes.CALENDAR_ROOT}?focusId={focusId}&city={city}&date={date}",
                                    arguments = listOf(
                                        navArgument("focusId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("city") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null }
                                    ),
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/calendar?focusId={focusId}&city={city}&date={date}" })
                                ) { backStackEntry ->
                                    val focusId = backStackEntry.arguments?.getString("focusId")
                                    val city = backStackEntry.arguments?.getString("city")
                                    val date = backStackEntry.arguments?.getString("date")
                                    TravelPlannerScreen(
                                        onBack = { navController.popBackStack() },
                                        focusId = focusId,
                                        initialCity = city,
                                        initialStartDate = date,
                                        onViewRoute = { tripId -> navController.navigate(Routes.routeWeather(tripId)) }
                                    )
                                }
                                composable(
                                    Routes.AI_ROOT + "?conversationId={conversationId}",
                                    arguments = listOf(
                                        navArgument("conversationId") { type = NavType.StringType; nullable = true; defaultValue = null }
                                    )
                                ) { backStackEntry ->
                                    val conversationId = backStackEntry.arguments?.getString("conversationId")
                                    AiChatScreen(
                                        initialRecommendation = pendingRecommendation,
                                        conversationId = conversationId,
                                        onRecommendationHandled = { pendingRecommendation = null },
                                        onBack = {
                                            pendingRecommendation = null
                                            navController.popBackStack()
                                        },
                                        onNavigateToHistory = {
                                            navController.navigate(Routes.AI_HISTORY)
                                        },
                                        onNavigateToTravelCreate = { city: String, date: String? ->
                                            navController.navigate("${Routes.CALENDAR_ROOT}?focusId=NEW&city=$city&date=$date")
                                        }
                                    )
                                }
                                composable(Routes.PROFILE_ROOT) {
                                    ProfileScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                                        onNavigateToCities = { navController.navigate(Routes.CITIES) },
                                        onNavigateToAiHistory = { navController.navigate(Routes.AI_HISTORY) },
                                        onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                                        onNavigateToPersonalization = { navController.navigate(Routes.PERSONALIZATION) },
                                        onNavigateToTravels = {
                                            val startDestId = navController.graph.findStartDestination().id
                                            navController.navigate(Routes.CALENDAR_ROOT) {
                                                popUpTo(startDestId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                                composable(Routes.CITIES) {
                                    CitiesManagementScreen(onBack = { navController.popBackStack() })
                                }
                                composable(Routes.AI_HISTORY) {
                                    AiHistoryScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToChat = { id ->
                                            navController.navigate(Routes.AI_ROOT + "?conversationId=$id")
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
                                        onNavigateToSmartAlerts = { navController.navigate(Routes.SMART_ALERTS) }
                                    )
                                }
                                composable(Routes.SMART_ALERTS) {
                                    SmartAlertsScreen(onBack = { navController.popBackStack() })
                                }
                                composable(Routes.PERSONALIZATION) {
                                    PersonalizationScreen(
                                        profileViewModel = profileViewModel,
                                        onComplete = {
                                            navController.navigate(Routes.WEATHER_ROOT) {
                                                popUpTo(Routes.PERSONALIZATION) { inclusive = true }
                                            }
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    Routes.NOTIFICATION_CENTER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/notifications" })
                                ) {
                                    NotificationCenterScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToDetail = { screen, params ->
                                            try {
                                                navController.navigate(screen) {
                                                    launchSingleTop = true
                                                }
                                            } catch (e: Exception) {
                                                Log.e("Nav", "Failed to navigate to $screen", e)
                                                navController.navigate(Routes.WEATHER_ROOT) {
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }
                                // --- AKILLI GÜZERGÂH HAVA DURUMU ---
                                composable(
                                    Routes.ROUTE_WEATHER,
                                    arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/route/{tripId}" })
                                ) { backStackEntry ->
                                    val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                                    TravelRouteWeatherScreen(
                                        tripId = tripId,
                                        onBack = { navController.popBackStack() }
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
