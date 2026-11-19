package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.havamania.ui.theme.*

/** Debug-only component acceptance. No Auth, Firebase, DataStore or account operations. */
class ProfileSettingsFixtureModel : ViewModel() {
    var profile by mutableStateOf(UserProfile(uid = "fixture-a", name = "Profil A", defaultCity = "Ankara", isPremium = true))
}

class ProfileSettingsTestActivity : ComponentActivity() {
    val model: ProfileSettingsFixtureModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout {
                    MaterialTheme {
                        val profile = model.profile
                        val dialogs = rememberSettingsDialogs(profile.uid)
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            PremiumSettingsCard(profile.isPremium) { }
                            Text("Şehir: ${profile.defaultCity}")
                            Button(onClick = { dialogs.delete.value = true }) { Text("Silme penceresini aç") }
                            PremiumProfileHeader(name = profile.name, bio = profile.bio, imageUri = null,
                                interests = emptySet(), aboutMe = "", stats = emptyMap(),
                                onAvatarClick = {}, onStatClick = {})
                        }
                        if (dialogs.delete.value) AccountDeleteDialog(
                            onDismiss = { dialogs.delete.value = false },
                            onConfirm = { error("Fixture cannot delete an account") })
                    }
                }
            }
        }
    }
}