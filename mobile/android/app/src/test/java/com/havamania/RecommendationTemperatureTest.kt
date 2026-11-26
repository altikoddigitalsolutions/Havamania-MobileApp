package com.havamania
import com.havamania.ui.theme.AssistantTone
import org.junit.Assert.*
import org.junit.Test
class RecommendationTemperatureTest {
 private fun weather(temp:String="18.5°")=WeatherData(cityName="Bursa",temperature=temp,feelsLike="18.5°",humidity=50,windSpeed=5.0,uvIndex=1,precipitationProbability=10,condition="Bulutlu",weatherCode=3)
 @Test fun decimalPointIsNotRemovedFromDailyTemperature() {
  val text=RecommendationEngine.generateTodayRecommendation(weather()).message
  assertTrue(text,text.contains("18,5°C"));assertFalse(text.contains("185"))
 }
 @Test fun shortSummaryDoesNotTreatDecimalAsExtremeHeat() {
  assertFalse(RecommendationEngine.getShortWeatherSummary(weather()).contains("oldukça sıcak"))
 }
 @Test fun negativeFractionKeepsItsSignAndPrecision() {
  val text=RecommendationEngine.generateTodayRecommendation(weather("-0.5°")).message
  assertTrue(text,text.contains("-0,5°C"));assertFalse(text.contains("-5°C"))
 }
 @Test fun decimalCommaAndCelsiusUnitAreAccepted() {
  val text=RecommendationEngine.generateTodayRecommendation(weather("18,5 °C")).message
  assertTrue(text,text.contains("18,5°C"));assertFalse(text.contains("eksik"))
 }
 @Test fun invalidOrMissingTemperatureCannotBecomeTwenty() {
  for(value in listOf("--","","NaN","Infinity","999°","12° / 18°","1.2.3°")) {
   val text=RecommendationEngine.getShortWeatherSummary(weather(value))
   assertTrue("$value: $text",text.contains("eksik"));assertFalse(text.contains("20°C"))
  }
 }
 @Test fun coldDecimalStillHighlightsCampPreparation() {
  assertTrue(RecommendationEngine.generateTodayRecommendation(weather("9.5°"),userInterests=setOf("kamp")).highlightedWords.contains("kamp"))
 }
 @Test fun genuineZeroRemainsAvailable() {
  val text=RecommendationEngine.generateTodayRecommendation(weather("0°")).message
  assertTrue(text,text.contains("0°C"));assertFalse(text.contains("eksik"))
 }
 @Test fun assistantFeelsLikeRetainsNegativeFraction() {
  val text=RecommendationEngine.generateAssistantFallbackReply("Bugün ne giymeliyim?",weather().copy(feelsLike="-0.5°"),tone=AssistantTone.DETAYLI_UZMAN)
  assertTrue(text,text.contains("-0,5°C"))
 }
 @Test fun activityDoesNotRoundThirtyPointFiveDownToSafeBand() {
  val text=RecommendationEngine.generateAssistantFallbackReply("Dışarı çıkmak uygun mu?",weather("30.5°"))
  assertTrue(text,text.contains("biraz riskli"));assertFalse(text.contains("oldukça uygun"))
  assertFalse(text.contains("hiçbir engel"));assertTrue(text,text.contains("30,5°C"))
 }
 @Test fun staleSummaryIsExplicitInsteadOfCurrent() {
  assertTrue(RecommendationEngine.getShortWeatherSummary(weather().copy(isStale=true)).contains("güncel değil"))
 }
 @Test fun knownStormSurvivesMissingNumbersAndNeverLooksFavorable() {
  val storm=weather("20°").copy(weatherCode=95,condition="Fırtınalı")
  assertFalse(RecommendationEngine.generateTodayRecommendation(storm).message.contains("elverişli"))
  assertEquals(RecommendationPriority.HIGH,RecommendationEngine.generateTodayRecommendation(storm).priority)
  val activity=RecommendationEngine.generateAssistantFallbackReply("Dışarı çıkmak uygun mu?",storm)
  assertFalse(activity.contains("oldukça uygun"));assertTrue(activity,activity.contains("risk"))

  assertTrue(RecommendationEngine.getShortWeatherSummary(storm.copy(temperature="--")).contains("fırtına",true))
 }
 @Test fun nightSummaryCannotInventSunshine() {
  assertFalse(RecommendationEngine.getShortWeatherSummary(weather("20°").copy(weatherCode=0,condition="Açık gece")).contains("Güneşli"))
 }
}
