package com.havamania
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test
class WeatherHeroPhaseTest {
 private fun spec(code:Int=0,time:LocalTime=LocalTime.NOON,rise:LocalTime?=null,set:LocalTime?=null)=WeatherHeroStyleManager.resolveWeatherHeroTheme(code,"",time,41.0,29.0,rise,set)
 @Test fun absentSolarTimesCannotInventSunshine() {
  val value=spec();assertEquals("Açık",value.displayTitle);assertEquals(VisualEffectType.NONE,value.effectType)
 }
 @Test fun reversedSolarIntervalCannotInventMoon() {
  assertEquals(VisualEffectType.NONE,spec(rise=LocalTime.of(18,0),set=LocalTime.of(6,0)).effectType)
 }
 @Test fun oneSolarBoundaryIsNotEnoughForDayPhase() {
  assertEquals(VisualEffectType.NONE,spec(rise=LocalTime.of(6,0)).effectType)
 }
 @Test fun sunsetCannotHaveSunEffectAndNightTitleTogether() {
  val value=spec(time=LocalTime.of(18,0),rise=LocalTime.of(6,0),set=LocalTime.of(18,0))
  assertEquals(VisualEffectType.MOON,value.effectType);assertEquals("Açık gece",value.displayTitle)
 }
 @Test fun validSolarDayKeepsSun() {
  assertEquals(VisualEffectType.SUN,spec(rise=LocalTime.of(6,0),set=LocalTime.of(18,0)).effectType)
 }
 @Test fun unknownPhaseStillKeepsKnownStormEffect() { assertEquals(VisualEffectType.THUNDER,spec(code=95).effectType) }
 @Test fun providerNightOverridesNoonAndMissingSolarTimes() {
  val value=WeatherHeroStyleManager.resolveWeatherHeroTheme(0,"",LocalTime.NOON,41.0,29.0,null,null,false)
  assertTrue(value.phaseKnown);assertEquals(VisualEffectType.MOON,value.effectType);assertEquals("Açık gece",value.displayTitle)
 }
 @Test fun knownProviderDayDoesNotNeedAnInventedClock() {
  val value=WeatherHeroStyleManager.resolveWeatherHeroTheme(0,"",null,41.0,29.0,null,null,true)
  assertTrue(value.phaseKnown);assertEquals(VisualEffectType.SUN,value.effectType)
  assertFalse(WeatherHeroStyleManager.resolveWeatherHeroTheme(0,"",null,41.0,29.0,null,null).phaseKnown)
 }
 @Test fun mapperOnlyPreservesValidatedProviderFlag() {
  for(flag in listOf(null,0,1,2)) {
   val value=WeatherMapper.mapToDomain(OpenMeteoResponse(41.0,29.0,current=CurrentWeatherDto(isDay=flag)),"İstanbul")
   assertEquals(when(flag) {0->false;1->true;else->null},value.verifiedIsDay)
  }
 }
 @Test fun staleAndDailyViewsDoNotBorrowCurrentPhaseOrLegacyBoolean() {
  val value=WeatherData(isDay=true,verifiedIsDay=false)
  assertEquals(false,weatherSelectionDaylight(value,null,null))
  assertNull(weatherSelectionDaylight(value.copy(isStale=true),null,null))
  assertNull(weatherSelectionDaylight(WeatherData(isDay=true),null,null))
  assertNull(weatherSelectionDaylight(value,null,DailyForecast("Day","2026-11-26",10,20)))
 }
}
