package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.havamania.ui.theme.*
import kotlinx.coroutines.flow.StateFlow

internal object AccountEntryTestSession {
    data class Inputs(val owner: StateFlow<String?>, val profile: StateFlow<ProfileState>,
        val retry: () -> Unit, val exit: () -> Unit, val complete: () -> Unit, val tabControls: Boolean = false)
    @Volatile var inputs: Inputs? = null
}

/** Debug-only Activity. Actual production redirect/loading components; destination bodies are test markers. */
class AccountEntryTestActivity : ComponentActivity() {
    lateinit var nav: NavHostController
        private set
    var route: String? = null
        private set
    val visited = mutableListOf<String>()
    private var visitedOwner: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inputs = checkNotNull(AccountEntryTestSession.inputs)
        setContent {
            val owner by inputs.owner.collectAsStateWithLifecycle()
            val profile by inputs.profile.collectAsStateWithLifecycle()
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme(colorScheme = ThemeManager.getColorScheme(AppTheme.LIGHT)) {
                    key(owner) {
                        val controller = rememberNavController()
                        val entry by controller.currentBackStackEntryAsState()
                        val activeRoute = entry?.destination?.route
                        SideEffect {
                            // Observations are account-local, like the keyed NavController.
                            if (visitedOwner != owner) { visited.clear(); visitedOwner = owner }
                            nav = controller; route = activeRoute
                            route?.let { if (visited.lastOrNull() != it) visited.add(it) }
                        }
                        AccountEntryRedirect(controller, owner, profile, enabled = true)
                        Scaffold(bottomBar = {
                            if (inputs.tabControls && activeRoute?.substringBefore('?') in setOf(Routes.WEATHER_ROOT, Routes.AI_ROOT, Routes.CALENDAR_ROOT, Routes.PROFILE_ROOT)) {
                                WeatherBottomBar(activeRoute, { target -> controller.navigate(target) { accountTabOptions(target) } })
                            }
                        }) { padding ->
                        NavHost(controller, startDestination = if (owner == null) Routes.AUTH_WELCOME else Routes.ACCOUNT_LOADING,
                            modifier = Modifier.padding(padding)) {
                            composable(Routes.ACCOUNT_LOADING) {
                                AccountProfileLoadingScreen(owner, profile, inputs.retry, inputs.exit)
                            }
                            composable(Routes.AUTH_WELCOME) { Text("Giriş başlangıcı") }
                            composable(Routes.PERSONALIZATION) { source ->
                                androidx.compose.foundation.layout.Column {
                                TextButton(onClick = {
                                    inputs.complete()
                                    controller.navigate(Routes.WEATHER_ROOT) { popUpTo(0) { inclusive = true } }
                                }) { Text("Kişiselleştirmeyi tamamla") }
                                TextButton(onClick = { controller.leavePersonalization(source, inputs.exit) }) { Text("Kişiselleştirmeden geri dön") }
                                }
                            }
                            composable(Routes.CALENDAR_ROOT) { Text("Takvim hedefi") }
                            composable(Routes.PROFILE_ROOT) { Text("Profil hedefi") }
                            composable(Routes.WEATHER_ROOT) { source ->
                                androidx.compose.foundation.layout.Column {
                                    Text("Hava hedefi")
                                    if (inputs.tabControls) TextButton(onClick = {
                                        controller.navigateFromActiveScreen(source, Routes.AI_ROOT)
                                    }) { Text("Soru sor (yerel test)") }
                                }
                            }
                            composable(Routes.AI_ROOT + "?conversationId={conversationId}", arguments = listOf(
                                androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }
                            )) {
                                var draft by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
                                androidx.compose.foundation.layout.Column {
                                    Text("Asistan hedefi")
                                    TextField(draft, { draft = it }, label = { Text("Test mesajı") })
                                    TextButton(onClick = { draft = "Taslak korunmalı" }) { Text("Taslağı hazırla") }
                                }
                            }
                        }
                        }
                    }
                } }
            }
        }
    }
}