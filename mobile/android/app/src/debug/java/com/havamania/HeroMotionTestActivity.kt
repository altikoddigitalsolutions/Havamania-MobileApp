package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*

/** Isolated real-window fixture: no Compose test clock and no Firebase dependencies. */
class HeroMotionTestActivity : ComponentActivity() {
    @Volatile var observedScale = -1f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
            SideEffect { observedScale = scale }
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                MaterialTheme {
                    Box(Modifier.width(360.dp)) {
                        WeatherHeroCard(cityName="İstanbul", temperature="18°", conditionLabel="Güneşli",
                            weatherCode=0, isDay=true, high="22°", low="12°", feelsLike="17°",
                            humidity="%50", windSpeed="12 km/sa", uvIndex="3", onCityClick={},
                            parallaxOffset=20f, liveEffectsOverride=true)
                    }
                }
            }
        }
    }
}
