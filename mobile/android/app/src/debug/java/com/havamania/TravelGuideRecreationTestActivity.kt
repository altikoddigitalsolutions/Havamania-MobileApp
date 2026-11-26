package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*

internal object TravelGuideRecreationFixture {
    data class Inputs(val model: TravelViewModel, val focus: String, val phone: Boolean)
    @Volatile var inputs: Inputs? = null
}

/** Debug only: real screen, test-owned retained ViewModel and in-memory database. */
class TravelGuideRecreationTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName == "com.havamania")
        check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null)
        val inputs = checkNotNull(TravelGuideRecreationFixture.inputs)
        setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalTravelGuideRefresher provides { _, _, _ -> }) {
                ProvideResponsiveLayout {
                    val size = LocalWindowSize.current
                    CompositionLocalProvider(LocalWindowSize provides if (inputs.phone)
                        HavamaniaWindowSize(390, size.heightDp, false, true, false, false) else size) {
                        MaterialTheme {
                            Box(Modifier.width(if (inputs.phone) 390.dp else 800.dp).fillMaxHeight()) {
                                TravelPlannerScreen(inputs.model, focusId = inputs.focus, onBack = {})
                            }
                        }
                    }
                }
            }
        }
    }
}
