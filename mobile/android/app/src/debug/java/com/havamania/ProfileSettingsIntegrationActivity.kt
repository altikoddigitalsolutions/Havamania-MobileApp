package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.havamania.ui.theme.*

/** Test injection only; no default Firebase fallback. Absent from release. */
internal object ProfileSettingsIntegrationSession {
    @Volatile var factory: ViewModelProvider.Factory? = null
}

class ProfileSettingsIntegrationActivity : ComponentActivity() {
    lateinit var profileModel: ProfileViewModel
        private set
    lateinit var themeModel: ThemeViewModel
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = ViewModelProvider(this, checkNotNull(ProfileSettingsIntegrationSession.factory))
        profileModel = provider[ProfileViewModel::class.java]
        themeModel = provider[ThemeViewModel::class.java]
        setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout {
                    MaterialTheme {
                        val state by profileModel.profileState.collectAsStateWithLifecycle()
                        val owner by profileModel.activeUid.collectAsStateWithLifecycle()
                        val name by themeModel.userName.collectAsStateWithLifecycle()
                        val city by themeModel.defaultCity.collectAsStateWithLifecycle()
                        val premium by themeModel.isPremium.collectAsStateWithLifecycle()
                        val profile = (state as? ProfileState.Success)?.profile?.takeIf { it.uid == owner }
                        val dialogs = rememberSettingsDialogs(owner)
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            PremiumSettingsCard(premium) {}
                            Text("Yerel ad: $name")
                            Text("Yerel şehir: ${city?.name.orEmpty()}")
                            Button(onClick = { dialogs.delete.value = true }) { Text("Silme penceresini aç") }
                            if (profile != null) PremiumProfileHeader(profile.name, profile.bio, null,
                                interests = emptySet(), aboutMe = profile.aboutMe, stats = emptyMap(),
                                onAvatarClick = {}, onStatClick = {})
                        }
                        if (dialogs.delete.value) AccountDeleteDialog(
                            onDismiss = { dialogs.delete.value = false },
                            onConfirm = { error("Integration fixture cannot delete accounts") })
                    }
                }
            }
        }
    }
}