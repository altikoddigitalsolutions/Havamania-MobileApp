package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.havamania.ui.theme.*
import kotlinx.coroutines.flow.StateFlow

internal object AccountEntryTestSession {
    data class Inputs(val owner: StateFlow<String?>, val profile: StateFlow<ProfileState>,
        val retry: () -> Unit, val exit: () -> Unit, val complete: () -> Unit)
    @Volatile var inputs: Inputs? = null
}

/** Debug-only Activity. Actual production redirect/loading components; destination bodies are test markers. */
class AccountEntryTestActivity : ComponentActivity() {
    lateinit var nav: NavHostController
        private set
    var route: String? = null
        private set
    val visited = mutableListOf<String>()
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
                            nav = controller; route = activeRoute
                            route?.let { if (visited.lastOrNull() != it) visited.add(it) }
                        }
                        AccountEntryRedirect(controller, owner, profile, enabled = true)
                        NavHost(controller, startDestination = if (owner == null) Routes.AUTH_WELCOME else Routes.ACCOUNT_LOADING) {
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
                            composable(Routes.WEATHER_ROOT) { Text("Hava hedefi") }
                        }
                    }
                } }
            }
        }
    }
}