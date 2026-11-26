package com.havamania
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Thermostat
import org.junit.Rule
import org.junit.Test
class PersonalizationSurfaceDeviceTest {
 @get:Rule val compose=createComposeRule()
 @Test fun autumnCollapsedExpandedAndSelectionRemainUsable() {
  var expanded by mutableStateOf(false)
  var selected by mutableStateOf(emptySet<String>())
  val category=InterestsData.categories.first()
  compose.setContent {
   val colors=ThemeFactory.createColors(AppTheme.AUTUMN)
   CompositionLocalProvider(LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    ProvideResponsiveLayout { MaterialTheme { Column(Modifier.width(360.dp).background(colors.background).padding(16.dp)) {
     PersonalizationAccordionItem(category,expanded,{expanded=!expanded},selected,{id->selected=if(id in selected)selected-id else selected+id})
    } } }
   }
  }
  capture("personalization-autumn-collapsed")
  compose.onNodeWithText(category.title).performClick()
  compose.onNodeWithText(category.interests.first().label).assertIsDisplayed().performClick()
  compose.runOnIdle { org.junit.Assert.assertTrue(category.interests.first().id in selected) }
  capture("personalization-autumn-expanded")
 }
 @Test fun lightLargeTextKeepsEveryCategoryAndSelectionReachable() = verifyCategories(AppTheme.LIGHT,2f)
 @Test fun darkNormalTextKeepsEveryCategoryAndSelectionReachable() = verifyCategories(AppTheme.DARK,1f)
 private fun verifyCategories(theme:AppTheme,fontScale:Float) {
  var expanded by mutableStateOf<String?>(null)
  var selected by mutableStateOf(emptySet<String>())
  compose.setContent {
   val colors=ThemeFactory.createColors(theme)
   val density=LocalDensity.current
   CompositionLocalProvider(LocalDensity provides Density(density.density,fontScale),LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    ProvideResponsiveLayout { MaterialTheme {
     Column(Modifier.width(320.dp).height(600.dp).background(colors.background).verticalScroll(rememberScrollState()).padding(16.dp)) {
      InterestsData.categories.forEach { category ->
       PersonalizationAccordionItem(category,expanded==category.title,{expanded=if(expanded==category.title)null else category.title},selected,{id->selected=if(id in selected)selected-id else selected+id})
       Spacer(Modifier.height(12.dp))
      }
     }
    } }
   }
  }
  InterestsData.categories.forEach { category ->
   compose.onNodeWithText(category.title).performScrollTo().performClick()
   val interest=category.interests.first()
   val chip=compose.onNodeWithText(interest.label).performScrollTo().assertIsDisplayed()
   chip.assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
   val bounds=chip.getUnclippedBoundsInRoot()
   org.junit.Assert.assertTrue(bounds.left>=0.dp && bounds.right<=320.dp)
   chip.performClick().assertIsNotSelected()
   compose.runOnIdle { org.junit.Assert.assertFalse(interest.id in selected) }
   compose.onNodeWithText(category.title).performScrollTo().performClick()
  }
  capture("personalization-${theme.name.lowercase()}-$fontScale-all-categories")
 }
 @Test fun preferencesRemainReachableAtLargeTextAndDisabledSwitchCannotChange() {
  var travel by mutableStateOf(false)
  var weather by mutableStateOf(false)
  var chosen by mutableStateOf(emptySet<String>())
  var rain by mutableStateOf(false)
  compose.setContent {
   val colors=ThemeFactory.createColors(AppTheme.LIGHT)
   val density=LocalDensity.current
   CompositionLocalProvider(LocalDensity provides Density(density.density,2f),LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    ProvideResponsiveLayout { MaterialTheme {
     Column(Modifier.width(320.dp).height(600.dp).verticalScroll(rememberScrollState())) {
      PersonalizationAccordionItemSimple("Seyahat Tarzın",androidx.compose.material.icons.Icons.Rounded.FlightTakeoff,travel,{travel=!travel}) {
       TravelStyleChoices(chosen,true) {style->chosen=if(style in chosen)chosen-style else chosen+style}
      }
      PersonalizationAccordionItemSimple("Hava Hassasiyetlerin",androidx.compose.material.icons.Icons.Rounded.Thermostat,weather,{weather=!weather}) {
       PreferenceSwitch("Yağmura Hassasım",rain) {rain=it}
       PreferenceSwitch("Rüzgâra Hassasım",false,enabled=false) {error("Disabled switch must not write")}
      }
     }
    } }
   }
  }
  compose.onNodeWithText("Seyahat Tarzın").performScrollTo().performClick()
  PersonalizationDefaults.TRAVEL_STYLES.forEach { style ->
   val chip=compose.onNodeWithText(style).performScrollTo().assertIsDisplayed()
   chip.performClick().assertIsSelected()
   val bounds=chip.getUnclippedBoundsInRoot()
   org.junit.Assert.assertTrue("Travel style must stay in card",bounds.left>=0.dp && bounds.right<=320.dp)
   chip.performClick().assertIsNotSelected()
  }
  compose.onNodeWithText("Hava Hassasiyetlerin").performScrollTo().performClick()
  val toggle=compose.onNodeWithContentDescription("Yağmura Hassasım").performScrollTo()
  toggle.assertIsOff().performClick().assertIsOn()
  val disabled=compose.onNodeWithContentDescription("Rüzgâra Hassasım").performScrollTo()
  disabled.assertIsNotEnabled().performClick().assertIsOff()
  compose.runOnIdle {org.junit.Assert.assertTrue(rain)}
 }
 @Test fun busyPreferencesExposeDisabledStateAndRejectClicksUntilEditable() {
  var editable by mutableStateOf(false)
  var writes=0
  val category=InterestsData.categories.first()
  compose.setContent {
   val colors=ThemeFactory.createColors(AppTheme.DARK)
   CompositionLocalProvider(LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    ProvideResponsiveLayout { MaterialTheme { Column(Modifier.width(320.dp).height(600.dp).verticalScroll(rememberScrollState())) {
     TravelStyleChoices(emptySet(),editable) {writes++}
     PersonalizationAccordionItem(category,true,{},emptySet(),{writes++},enabled=editable)
    } } }
   }
  }
  val style=compose.onNodeWithText(PersonalizationDefaults.TRAVEL_STYLES.first()).performScrollTo()
  style.assertIsNotEnabled().performClick()
  val interest=compose.onNodeWithText(category.interests.first().label).performScrollTo()
  interest.assertIsNotEnabled().performClick()
  compose.runOnIdle { org.junit.Assert.assertEquals(0,writes);editable=true }
  style.performScrollTo().assertIsEnabled().performClick()
  interest.performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle {org.junit.Assert.assertEquals(2,writes)}
 }
 private fun capture(name:String) {
  compose.mainClock.advanceTimeBy(1000)
  val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
  val values=android.content.ContentValues().apply {
   put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
   put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
   put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Havamania-QA")
  }
  val uri=checkNotNull(context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
  context.contentResolver.openOutputStream(uri)!!.use { check(compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }
 }
}
