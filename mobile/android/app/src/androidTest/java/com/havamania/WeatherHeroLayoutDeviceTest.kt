package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WeatherHeroLayoutDeviceTest {
 @get:Rule val compose=createComposeRule()
 @Test fun lightNarrowLargeTextPreservesControlsAndMetrics() = verify(AppTheme.LIGHT,2f)
 @Test fun darkNarrowNormalTextPreservesControlsAndMetrics() = verify(AppTheme.DARK,1f)
 private fun verify(theme:AppTheme,fontScale:Float) {
  var cities=0
  var notifications=0
  val city="AFYONKARAHİSAR MERKEZ"
  compose.setContent {
   val density=LocalDensity.current
   val colors=ThemeFactory.createColors(theme)
   CompositionLocalProvider(LocalDensity provides Density(density.density,fontScale),
    LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    MaterialTheme { Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
     WeatherHeroCard(cityName=city,temperature="-18°",conditionLabel="Güneşli",weatherCode=0,isDay=true,
      high="22°",low="-22°",feelsLike="-24°",humidity="%100",windSpeed="123 km/sa",uvIndex="11",
      onCityClick={cities++},onNotificationsClick={notifications++},liveEffectsOverride=true)
    } }
   }
  }
  val location=compose.onNode(hasClickAction() and hasText(city,substring=true))
  val bell=compose.onNodeWithContentDescription("Bildirimler")
  location.assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
  bell.assertIsDisplayed().performClick()
  compose.runOnIdle { assertEquals(1,cities);assertEquals(1,notifications) }
  val locationBounds=location.getUnclippedBoundsInRoot()
  val bellBounds=bell.getUnclippedBoundsInRoot()
  assertTrue("Location must leave room for notifications",locationBounds.right<=bellBounds.left)
  for(value in listOf("%100","123 km/sa","UV 11","Hissedilen -24°")) {
   val node=compose.onNodeWithText(value).performScrollTo().assertIsDisplayed()
   val bounds=node.getUnclippedBoundsInRoot()
   assertTrue("$value must stay inside 320dp card",bounds.left>=0.dp && bounds.right<=320.dp)
  }
  val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
  val values=android.content.ContentValues().apply {
   put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"hero-layout-${theme.name}-$fontScale.png")
   put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
   put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Havamania-QA")
  }
  val uri=checkNotNull(context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
  context.contentResolver.openOutputStream(uri)!!.use {
   check(compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))
  }
 }
}
