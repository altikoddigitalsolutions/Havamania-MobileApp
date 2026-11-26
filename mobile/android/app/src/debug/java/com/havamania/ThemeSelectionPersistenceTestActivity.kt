package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.havamania.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal object ThemeSelectionPersistenceFixture {
    @Volatile var owner: MutableStateFlow<String>? = null
}

/** Real dialog and ThemeManager storage, synthetic owner keys, no Auth mutation. */
class ThemeSelectionPersistenceTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName == "com.havamania")
        check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null)
        val owners = checkNotNull(ThemeSelectionPersistenceFixture.owner)
        setContent {
            val owner by owners.collectAsState()
            key(owner) {
                val theme by remember(owner) { ThemeManager.getTheme(applicationContext, owner) }
                    .collectAsState(initial = AppTheme.DARK)
                val colors = ThemeFactory.createColors(resolveThemeSelection(theme, java.time.LocalDate.now()))
                val scope = rememberCoroutineScope()
                var open by rememberSaveable { mutableStateOf(false) }
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f),
                    LocalHavamaniaColors provides colors,
                    LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                    ProvideResponsiveLayout { MaterialTheme(colorScheme = ThemeManager.getColorScheme(theme)) {
                        Column {
                            Text("Seçim: ${theme.name}")
                            Button(onClick = { open = true }) { Text("Temayı seç (QA)") }
                        }
                        if (open) ThemeSelectionDialog(theme, { selected ->
                            scope.launch { ThemeManager.saveTheme(applicationContext, selected, owner) }
                            open = false
                        }, { open = false })
                    } }
                }
            }
        }
    }
}
