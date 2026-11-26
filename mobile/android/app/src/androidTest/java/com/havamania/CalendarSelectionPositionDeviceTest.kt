package com.havamania
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class CalendarSelectionPositionDeviceTest {
 @get:Rule val compose=createComposeRule()
 @Test fun visibleSelectionStaysPutAndExternalDateBecomesVisible() {
  val today=java.time.LocalDate.of(2026,11,26)
  var selected by mutableStateOf(today)
  compose.setContent {
   val colors=ThemeFactory.createColors(AppTheme.AUTUMN)
   CompositionLocalProvider(LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    ProvideResponsiveLayout { Box(Modifier.width(360.dp)) { TravelCalendarStripe(selected,{selected=it},emptySet(),today=today) } }
   }
  }
  val target=compose.onNodeWithContentDescription("28 Kasım 2026").assertIsDisplayed()
  val before=target.getUnclippedBoundsInRoot()
  target.performClick().assertIsSelected()
  compose.waitForIdle()
  assertEquals(before.left.value,target.getUnclippedBoundsInRoot().left.value,0.5f)
  compose.runOnIdle { selected=java.time.LocalDate.of(2027,6,15) }
  compose.onNodeWithContentDescription("15 Haziran 2027").assertIsDisplayed().assertIsSelected()
 }
 @Test fun restoredCalendarKeepsManualScrollInsteadOfReturningToSelection() {
  val restore=androidx.compose.ui.test.junit4.StateRestorationTester(compose)
  val today=java.time.LocalDate.of(2026,11,26)
  restore.setContent {
   val colors=ThemeFactory.createColors(AppTheme.LIGHT)
   CompositionLocalProvider(LocalHavamaniaColors provides colors,LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
    ProvideResponsiveLayout { Box(Modifier.width(360.dp)) { TravelCalendarStripe(today,{},emptySet(),today=today) } }
   }
  }
  compose.onNodeWithContentDescription("26 Kasım 2026").assertIsDisplayed()
  val list=compose.onNode(hasScrollToIndexAction())
  list.performScrollToIndex(45)
  val target=compose.onNodeWithContentDescription("11 Aralık 2026").assertIsDisplayed()
  val before=target.getUnclippedBoundsInRoot()
  restore.emulateSavedInstanceStateRestore()
  compose.waitForIdle()
  target.assertIsDisplayed()
  assertEquals(before.left.value,target.getUnclippedBoundsInRoot().left.value,0.5f)
 }
}
