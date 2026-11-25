package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.havamania.ui.theme.*

internal object ProfileEditSdkSession {
    @Volatile var factory: ViewModelProvider.Factory? = null
    var validateOnForeground = false
    var validationNetworkAllowed = true
    val emulatorNetworkAvailable: () -> Boolean = { validationNetworkAllowed }
}

/** Production profile form and ViewModels; no default SDK factory and no deletion route. */
class ProfileEditSdkActivity : ComponentActivity() {
    lateinit var authModel: AuthViewModel
    lateinit var profileModel: ProfileViewModel
    var backRequests = 0
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = ViewModelProvider(this, checkNotNull(ProfileEditSdkSession.factory))
        authModel = provider[AuthViewModel::class.java]
        profileModel = provider[ProfileViewModel::class.java]
        setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme(colorScheme = ThemeManager.getColorScheme(AppTheme.LIGHT)) {
                    if (ProfileEditSdkSession.validateOnForeground)
                        AuthSessionForegroundCheck(authModel, ProfileEditSdkSession.emulatorNetworkAvailable, profileModel)
                    val owner by profileModel.activeUid.collectAsStateWithLifecycle()
                    key(owner) {
                        if (owner == null) Text("SDK oturumu kapalı")
                        else EditProfileScreen(onBack = { backRequests++ }, profileViewModel = profileModel)
                    }
                } }
            }
        }
    }
}
