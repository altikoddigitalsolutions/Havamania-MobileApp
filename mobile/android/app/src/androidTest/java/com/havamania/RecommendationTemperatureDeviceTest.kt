package com.havamania
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Rule
import org.junit.Test
class RecommendationTemperatureDeviceTest {
 @get:Rule val compose=createComposeRule()
 @Test fun shortInsightUpdatesWithoutInventedHeatOrFreshness() {
  val good=WeatherData(cityName="Bursa",temperature="18.5°",windSpeed=5.0,uvIndex=1,precipitationProbability=10,condition="Bulutlu",weatherCode=3)
  var weather by mutableStateOf(good)
  compose.setContent {
   val colors=ThemeFactory.createColors(AppTheme.LIGHT)
   CompositionLocalProvider(LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    MaterialTheme { Column(Modifier.width(320.dp).height(700.dp)) { DailyInsightCard(weather,colors) } }
   }
  }
  compose.onNodeWithText("bulutlu",substring=true).assertIsDisplayed()
  compose.onNodeWithText("oldukça sıcak",substring=true).assertDoesNotExist()
  compose.runOnIdle { weather=good.copy(temperature="--") }
  compose.onNodeWithText("eksik",substring=true).assertIsDisplayed()
  compose.runOnIdle { weather=good.copy(isStale=true) }
  compose.onNodeWithText("güncel değil",substring=true).assertIsDisplayed()
  compose.runOnIdle { weather=good.copy(temperature="20°",weatherCode=95,condition="Fırtınalı") }
  compose.onNodeWithText("fırtına",substring=true,ignoreCase=true).assertIsDisplayed()
 }
}
