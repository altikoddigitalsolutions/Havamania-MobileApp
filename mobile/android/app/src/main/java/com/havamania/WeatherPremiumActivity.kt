package com.havamania

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.HavamaniaTheme
import com.havamania.ui.theme.ThemeViewModel
import com.havamania.*

import androidx.navigation.NavDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
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

internal fun NavController.replaceSavedAssistantChat(source: NavBackStackEntry): Boolean {
    if (currentBackStackEntry != source) return false
    navigate(Routes.AI_ROOT) {
        popUpTo(source.destination.id) { inclusive = true }
    }
    return true
}

internal fun NavController.navigateFromActiveScreen(
    source: NavBackStackEntry,
    route: String,
    options: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    if (currentBackStackEntry != source) return false
    navigate(route, options)
    return true
}

internal fun NavController.popActiveScreen(source: NavBackStackEntry): Boolean =
    currentBackStackEntry == source && popBackStack()

private val mainTabRoutes = setOf(Routes.WEATHER_ROOT, Routes.CALENDAR_ROOT, Routes.AI_ROOT, Routes.PROFILE_ROOT)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isMainTabSwitch(): Boolean =
    initialState.destination.route?.substringBefore('?') in mainTabRoutes &&
        targetState.destination.route?.substringBefore('?') in mainTabRoutes

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.appScreenEnterTransition(): EnterTransition =
    if (isMainTabSwitch()) EnterTransition.None else fadeIn(tween(700))

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.appScreenExitTransition(): ExitTransition =
    if (isMainTabSwitch()) ExitTransition.None else fadeOut(tween(700))

class WeatherPremiumActivity : ComponentActivity() {
    private val notificationTap by viewModels<NotificationTapViewModel>()

    private fun captureNotificationLaunch(incoming: Intent, restoring: Boolean = false): Boolean {
        if (incoming.action?.startsWith(NOTIFICATION_ACTION_PREFIX) != true &&
            !incoming.hasExtra(NOTIFICATION_OWNER) && !incoming.hasExtra(NOTIFICATION_ID)) return false
        notificationTapRequest(incoming,
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
            AccountDeletionManager.getInstance(application).state.value.let { it.pending || it.busy },
            restoring = restoring
        )?.let { notificationTap.pending = it }
        // NavHost must not consume this automatically before the owner check.
        setIntent(Intent(incoming).apply {
            data = null
            action = Intent.ACTION_MAIN
            removeExtra(NOTIFICATION_OWNER)
            removeExtra(NOTIFICATION_ID)
        })
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!captureNotificationLaunch(intent)) setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Initialize MapLibre before any MapView is created
if (BuildConfig.DEBUG) {
            Log.i("MapInit", "Initializing MapLibre getInstance")
}
        try {
            MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
if (BuildConfig.DEBUG) {
                Log.i("MapInit", "MapLibre initialized successfully")
}
        } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                Log.e("MapInit", "MapLibre initialization FAILED", e)
}
        }
        var isReady by mutableStateOf(false)
        var splashMinimumTimedOut by mutableStateOf(false)

        // Keep system splash on screen until basic data is ready OR min timeout
        splashScreen.setKeepOnScreenCondition {
            !isReady && !splashMinimumTimedOut
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        captureNotificationLaunch(intent, restoring = savedInstanceState != null)

        setContent {
            HavamaniaTheme {
                val deletionManager = remember { AccountDeletionManager.getInstance(application) }
                val deletionState by deletionManager.state.collectAsState()
                if (deletionState.pending) {
                    LaunchedEffect(Unit) {
                        isReady = true
                        splashMinimumTimedOut = true
                        // Cancel old profile/sync jobs before local cleanup.
                        viewModelStore.clear()
                        UserProfileRepository.getInstance().stopObserving()
                        deletionManager.retry()
                    }
                    AccountDeletionRecoveryScreen(deletionState, deletionManager::retry) {
                        viewModelStore.clear()
                        deletionManager.acknowledgeCompletion()
                        recreate()
                    }
                    return@HavamaniaTheme
                }
                val authViewModel: AuthViewModel = viewModel()
                val profileViewModel: ProfileViewModel = viewModel()
                val themeViewModel: ThemeViewModel = viewModel()

                val authState by authViewModel.authState.collectAsState()
                val profileState by profileViewModel.profileState.collectAsState()

                val navigationOwner = authViewModel.currentUser?.uid
                val navController = key(navigationOwner) { rememberNavController() }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val activeRoute = navBackStackEntry?.destination?.route
                val currentRoute = activeRoute ?: Routes.WEATHER_ROOT

                var appState by remember { mutableStateOf("splash") }
                LaunchedEffect(notificationTap.pending, navigationOwner, appState, isReady, activeRoute, deletionState.busy) {
                    val launch = notificationTap.pending ?: return@LaunchedEffect
                    if (!launch.isCurrent(navigationOwner, deletionManager.state.value.let { it.pending || it.busy })) {
                        notificationTap.pending = null
                        return@LaunchedEffect
                    }
                    if (appState != "main" || !isReady || !canNavigateFromNotification(activeRoute)) return@LaunchedEffect
                    val destination = try {
                        val dao = NotificationDatabase.getDatabase(application).notificationDao()
                        NotificationInboxGate.withLock {
                            resolveNotificationTap(launch,
                                { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid },
                                { deletionManager.state.value.let { it.pending || it.busy } },
                                markRead = { owner, id ->
                                    try { dao.markAsRead(owner, listOf(id)) }
                                    catch (_: android.database.sqlite.SQLiteException) {
                                        Log.w("NotificationTap", "Could not mark notification as read")
                                    }
                                },
                                load = dao::getVisibleNotification)
                        }
                    } catch (error: kotlinx.coroutines.CancellationException) { throw error }
                    catch (_: Exception) { null }
                    if (notificationTap.pending !== launch) return@LaunchedEffect
                    notificationTap.pending = null
                    if (destination != null && launch.isCurrent(
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
                            deletionManager.state.value.let { it.pending || it.busy }
                        )) {
                        navController.navigate(destination) { launchSingleTop = true }
                    }
                }

                // Splash Screen Minimum Duration Timer
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200) // Lowered for faster cold start, custom splash handles the rest
                    splashMinimumTimedOut = true
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
if (BuildConfig.DEBUG) {
                                        Log.e("Nav", "Initial redirection failed", e)
}
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
if (BuildConfig.DEBUG) {
                                        Log.e("Nav", "Main redirection failed", e)
}
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

                var pendingRecommendation by remember(navigationOwner) { mutableStateOf<HavamaniaRecommendation?>(null) }

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
                    val composingAssistantMessage = currentRoute == Routes.AI_ROOT &&
                        WindowInsets.ime.getBottom(LocalDensity.current) > 0
                    val shouldShowBottomBar = !composingAssistantMessage &&
                        currentRoute !in hideBottomBarRoutes &&
                        !currentRoute.startsWith("sub_ai_history_detail") &&
                        !currentRoute.startsWith("sub_route_weather")

                    Scaffold(
                        containerColor = Color.Transparent, // Managed by HavamaniaScreen
                        bottomBar = {
                            if (shouldShowBottomBar) {
                                WeatherBottomBar(
                                    currentRoute = currentRoute,
                                    onNavigate = navigate@ { route ->
                                        val entry = navController.currentBackStackEntry
                                        if (entry?.destination?.route?.substringBefore('?') == route &&
                                            listOf("focusId", "city", "date", "conversationId")
                                                .none { entry.arguments?.getString(it) != null }
                                        ) {
                                            return@navigate
                                        }
                                        try {
                                            val startDestId = navController.graph.findStartDestination().id

                                            navController.navigate(route) {
                                                popUpTo(startDestId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                                                Log.e("Nav", "Navigation failed to $route", e)
}
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        val tabBarPadding = PaddingValues(
                            bottom = if (shouldShowBottomBar) innerPadding.calculateBottomPadding() else 0.dp
                        )
                        // The background gradient is now handled globally in HavamaniaScreen
                        // for each page to allow specific overrides if needed.
                        // We use a root Box here for common layout values.
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(tabBarPadding)
                            .then(
                                if (currentRoute == Routes.AI_ROOT) Modifier.consumeWindowInsets(tabBarPadding)
                                else Modifier
                            )
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = if (authViewModel.currentUser != null) Routes.WEATHER_ROOT else Routes.AUTH_WELCOME,
                                enterTransition = { appScreenEnterTransition() },
                                exitTransition = { appScreenExitTransition() }
                            ) {
                                // --- AUTH ROUTES ---
                                composable(Routes.AUTH_WELCOME) {
                                    AuthWelcomeScreen(
                                        onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                                        onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                                    )
                                }
                                composable(Routes.LOGIN) { backStackEntry ->
                                    LoginScreen(
                                        viewModel = authViewModel,
                                        onBack = { navController.popActiveScreen(backStackEntry) },
                                        onNavigateToRegister = {
                                            navController.navigate(Routes.REGISTER) {
                                                popUpTo(Routes.AUTH_WELCOME)
                                            }
                                        },
                                        onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                                    )
                                }
                                composable(Routes.REGISTER) { backStackEntry ->
                                    RegisterScreen(
                                        viewModel = authViewModel,
                                        onBack = { navController.popActiveScreen(backStackEntry) },
                                        onNavigateToLogin = {
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(Routes.AUTH_WELCOME)
                                            }
                                        }
                                    )
                                }
                                composable(Routes.FORGOT_PASSWORD) { backStackEntry ->
                                    ForgotPasswordScreen(
                                        viewModel = authViewModel,
                                        onBack = { navController.popActiveScreen(backStackEntry) }
                                    )
                                }

                                // --- APP ROUTES ---
                                composable(
                                    Routes.WEATHER_ROOT,
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/weather" })
                                ) { backStackEntry ->
                                    HomeScreen(profileViewModel = profileViewModel, themeViewModel = themeViewModel,
                                        onNavigateToAi = { rec: HavamaniaRecommendation, _: WeatherData? ->
                                        if (navController.navigateFromActiveScreen(backStackEntry, Routes.AI_ROOT)) {
                                            pendingRecommendation = rec
                                        }
                                    }, onNavigateToNotifications = {
                                        navController.navigateFromActiveScreen(backStackEntry, Routes.NOTIFICATION_CENTER)
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
                                        onBack = { navController.popActiveScreen(backStackEntry) },
                                        focusId = focusId,
                                        initialCity = city,
                                        initialStartDate = date,
                                        onViewRoute = { tripId ->
                                            navController.navigateFromActiveScreen(backStackEntry, Routes.routeWeather(tripId))
                                        }
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
                                        onStartNewChat = {
                                            if (navController.replaceSavedAssistantChat(backStackEntry)) {
                                                pendingRecommendation = null
                                            }
                                        },
                                        onBack = {
                                            if (navController.popActiveScreen(backStackEntry)) {
                                                pendingRecommendation = null
                                            }
                                        },
                                        onNavigateToHistory = {
                                            navController.navigateFromActiveScreen(backStackEntry, Routes.AI_HISTORY)
                                        },
                                        onNavigateToTravelCreate = { city: String, date: String? ->
                                            val encodedCity = android.net.Uri.encode(city)
                                            navController.navigateFromActiveScreen(
                                                backStackEntry, "${Routes.CALENDAR_ROOT}?focusId=NEW&city=$encodedCity&date=$date"
                                            )
                                        }
                                    )
                                }
                                composable(Routes.PROFILE_ROOT) { backStackEntry ->
                                    ProfileScreen(
                                        profileViewModel = profileViewModel,
                                        onBack = { navController.popActiveScreen(backStackEntry) },
                                        onNavigateToSettings = { navController.navigateFromActiveScreen(backStackEntry, Routes.SETTINGS) },
                                        onNavigateToCities = { navController.navigateFromActiveScreen(backStackEntry, Routes.CITIES) },
                                        onNavigateToAiHistory = { navController.navigateFromActiveScreen(backStackEntry, Routes.AI_HISTORY) },
                                        onNavigateToEditProfile = { navController.navigateFromActiveScreen(backStackEntry, Routes.EDIT_PROFILE) },
                                        onNavigateToPersonalization = { navController.navigateFromActiveScreen(backStackEntry, Routes.PERSONALIZATION) },
                                        onNavigateToTravels = {
                                            val startDestId = navController.graph.findStartDestination().id
                                            navController.navigateFromActiveScreen(backStackEntry, Routes.CALENDAR_ROOT) {
                                                popUpTo(startDestId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                                composable(Routes.CITIES) { backStackEntry ->
                                    CitiesManagementScreen(onBack = { navController.popActiveScreen(backStackEntry) })
                                }
                                composable(Routes.AI_HISTORY) { backStackEntry ->
                                    AiHistoryScreen(
                                        onBack = { navController.popActiveScreen(backStackEntry) },
                                        onNavigateToChat = { id ->
                                            navController.navigateFromActiveScreen(
                                                backStackEntry, "sub_ai_history_detail/${android.net.Uri.encode(id)}"
                                            )
                                        }
                                    )
                                }
                                composable(Routes.AI_HISTORY_DETAIL) { backStackEntry ->
                                    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                                    AiHistoryDetailScreen(
                                        itemId = itemId,
                                        onBack = { navController.popActiveScreen(backStackEntry) }
                                    )
                                }
                                composable(Routes.EDIT_PROFILE) { backStackEntry ->
                                    EditProfileScreen(onBack = { navController.popActiveScreen(backStackEntry) })
                                }
                                composable(Routes.SETTINGS) { backStackEntry ->
                                    SettingsScreen(
                                        onBack = { navController.popActiveScreen(backStackEntry) },
                                        onNavigateToEditProfile = { navController.navigateFromActiveScreen(backStackEntry, Routes.EDIT_PROFILE) },
                                        onNavigateToCities = { navController.navigateFromActiveScreen(backStackEntry, Routes.CITIES) },
                                        onNavigateToSmartAlerts = { navController.navigateFromActiveScreen(backStackEntry, Routes.SMART_ALERTS) }
                                    )
                                }
                                composable(Routes.SMART_ALERTS) { backStackEntry ->
                                    SmartAlertsScreen(onBack = { navController.popActiveScreen(backStackEntry) })
                                }
                                composable(Routes.PERSONALIZATION) { backStackEntry ->
                                    PersonalizationScreen(
                                        profileViewModel = profileViewModel,
                                        onComplete = {
                                            navController.navigate(Routes.WEATHER_ROOT) {
                                                popUpTo(Routes.PERSONALIZATION) { inclusive = true }
                                            }
                                        },
                                        onBack = { navController.popActiveScreen(backStackEntry) }
                                    )
                                }
                                composable(
                                    Routes.NOTIFICATION_CENTER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "havamania://app/notifications" })
                                ) { backStackEntry ->
                                    NotificationCenterScreen(
                                        onBack = { navController.popActiveScreen(backStackEntry) },
                                        onNavigateToDetail = { screen, params ->
                                            try {
                                                navController.navigate(screen) {
                                                    launchSingleTop = true
                                                }
                                            } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                                                    Log.e("Nav", "Failed to navigate to $screen", e)
}
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
                                        onBack = { navController.popActiveScreen(backStackEntry) }
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
