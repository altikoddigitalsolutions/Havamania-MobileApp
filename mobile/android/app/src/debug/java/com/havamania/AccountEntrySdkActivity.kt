package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.havamania.ui.theme.*

internal object AccountEntrySdkSession {
    @Volatile var factory: ViewModelProvider.Factory? = null
    val lifecycleEvents = java.util.concurrent.CopyOnWriteArrayList<String>()
}

/** Debug-only real ViewModels/personalization/nav; weather and welcome are markers. No default factory. */
class AccountEntrySdkActivity : ComponentActivity() {
    lateinit var authModel: AuthViewModel
        private set
    lateinit var profileModel: ProfileViewModel
        private set
    lateinit var nav: NavHostController
        private set
    var route: String? = null
        private set
    val visited = mutableListOf<String>()
    private fun trace(event: String) {
        val message = "${android.os.SystemClock.elapsedRealtime()} instance=${System.identityHashCode(this)} $event changing=$isChangingConfigurations finishing=$isFinishing focus=${hasWindowFocus()}"
        AccountEntrySdkSession.lifecycleEvents.add(message)
        android.util.Log.i("EntryLifecycle", message)
    }
    override fun onStart() { super.onStart(); trace("start") }
    override fun onResume() { super.onResume(); trace("resume") }
    override fun onPause() { trace("pause"); super.onPause() }
    override fun onStop() { trace("stop"); super.onStop() }
    override fun onDestroy() { trace("destroy"); super.onDestroy() }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus); trace("windowFocus=$hasFocus")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trace("create restored=${savedInstanceState != null}")
        val provider = ViewModelProvider(this, checkNotNull(AccountEntrySdkSession.factory))
        authModel = provider[AuthViewModel::class.java]
        profileModel = provider[ProfileViewModel::class.java]
        setContent {
            val authState by authModel.authState.collectAsStateWithLifecycle()
            val profile by profileModel.profileState.collectAsStateWithLifecycle()
            val owner = (authState as? AuthState.Authenticated)?.user?.uid?.takeIf { authModel.currentUser?.uid == it }
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
                            activeRoute?.let { if (visited.lastOrNull() != it) visited.add(it) }
                        }
                        AccountEntryRedirect(controller, owner, profile, true)
                        NavHost(controller, startDestination = if (owner == null) Routes.AUTH_WELCOME else Routes.ACCOUNT_LOADING) {
                            composable(Routes.AUTH_WELCOME) { Text("SDK giriş başlangıcı") }
                            composable(Routes.ACCOUNT_LOADING) {
                                AccountProfileLoadingScreen(owner, profile, profileModel::refresh, authModel::signOut)
                            }
                            composable(Routes.PERSONALIZATION) { source ->
                                PersonalizationScreen(profileModel, onComplete = {
                                    controller.navigate(Routes.WEATHER_ROOT) {
                                        popUpTo(Routes.PERSONALIZATION) { inclusive = true }
                                    }
                                }, onBack = { controller.leavePersonalization(source, authModel::signOut) })
                            }
                            composable(Routes.WEATHER_ROOT) { Text("SDK hava hedefi") }
                        }
                    }
                } }
            }
        }
    }
}