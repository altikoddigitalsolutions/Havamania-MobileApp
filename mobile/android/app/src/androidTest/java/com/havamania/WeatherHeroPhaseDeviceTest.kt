package com.havamania
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Rule
import org.junit.Test
class WeatherHeroPhaseDeviceTest {
 @get:Rule val compose=createComposeRule()
 @Test fun neutralNightAndDayRecomposeAndRemainReadableAcrossThemes() {
  var phase by mutableStateOf<Boolean?>(null)
  var theme by mutableStateOf(AppTheme.LIGHT)
  compose.setContent {
   val colors=ThemeFactory.createColors(theme)
   CompositionLocalProvider(LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    MaterialTheme { Column(Modifier.width(360.dp).background(colors.background)) {
     WeatherHeroCard(cityName="İstanbul",temperature="18°",conditionLabel="Güneşli",weatherCode=0,isDay=phase,
      high="22°",low="12°",feelsLike="17°",humidity="%50",windSpeed="12 km/sa",uvIndex="3",
      onCityClick={},onNotificationsClick={},liveEffectsOverride=true)
    } }
   }
  }
  compose.onNodeWithText("Açık").assertIsDisplayed()
  compose.onNodeWithText("Güneşli").assertDoesNotExist()
  capture("hero-phase-neutral-light","Gündüz/gece bilgisi bilinmiyor")
  compose.runOnIdle { theme=AppTheme.DARK }
  compose.onNodeWithText("Açık").assertIsDisplayed()
  capture("hero-phase-neutral-dark","Gündüz/gece bilgisi bilinmiyor")
  compose.runOnIdle { phase=false }
  compose.onNodeWithText("Açık gece").assertIsDisplayed()
  capture("hero-phase-night","Gece görünümü")
  compose.runOnIdle { phase=true;theme=AppTheme.LIGHT }
  compose.onNodeWithText("Güneşli").assertIsDisplayed()
  compose.onNodeWithText("Açık gece").assertDoesNotExist()
  capture("hero-phase-day","Gündüz görünümü")
  compose.runOnIdle { phase=null }
  compose.onNodeWithText("Açık").assertIsDisplayed()
  compose.onNodeWithText("Güneşli").assertDoesNotExist()
 }
 @Test fun disabledLiveBackgroundStaysStaticAfterPreferenceChange() {
  var enabled by mutableStateOf(true)
  val spec=WeatherHeroStyleManager.resolveWeatherHeroTheme(0,"",null,41.0,29.0,null,null,true)
  compose.setContent { Box(Modifier.size(300.dp)) { LiveBackgroundLayer(spec,enabled) } }
  compose.mainClock.autoAdvance=false
  compose.mainClock.advanceTimeBy(1000)
  compose.runOnIdle { enabled=false }
  compose.mainClock.advanceTimeBy(100)
  compose.waitForIdle()
  val before=compose.onRoot().captureToImage().asAndroidBitmap()
  compose.mainClock.advanceTimeBy(10000)
  compose.waitForIdle()
  val after=compose.onRoot().captureToImage().asAndroidBitmap()
  org.junit.Assert.assertTrue("Disabled background must not change across animation frames",before.sameAs(after))
 }
 private fun capture(name:String,status:String) {
  compose.mainClock.advanceTimeBy(1500)
  compose.waitForIdle()
  val node=compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,status))
  node.assertIsDisplayed()
  val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
  val values=android.content.ContentValues().apply {
   put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
   put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
   put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Havamania-QA")
  }
  val uri=checkNotNull(context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
  context.contentResolver.openOutputStream(uri)!!.use {
   check(node.captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))
  }
 }
}
