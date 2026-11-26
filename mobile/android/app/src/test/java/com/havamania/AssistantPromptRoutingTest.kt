package com.havamania

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AssistantPromptRoutingTest {
 @Test fun onlyWholeGreetingsStayLocal() {
  for(prompt in listOf("merhaba"," SELAM ","hi","hello","hey","GÜNAYDIN","İYİ GÜNLER"))
   assertTrue(prompt,isLocalAssistantPrompt(prompt))
 }
 @Test fun questionsAndSubstringCollisionsMustReachPlatform() {
  for(prompt in listOf("Bugün hava nasıl?","Yarın Ankara'da ne giysem?","Bursa",
   "Bu rota uygun mu?","Karar vermeme yardım et","Kardeşimle gezi planlıyorum",
   "Önemli bir toplantım var","Merhaba, yarın yağmur var mı?","Selam bana öneri ver",
   "İstanbul'da hafta sonu etkinlik öner","Nasıl hazırlanmalıyım?","Bugün","yarın","merhaba!"))
   assertFalse(prompt,isLocalAssistantPrompt(prompt))
 }
 @Test fun greetingNeverCallsPlatformButMixedGreetingDoes() = runBlocking {
  assertTrue(assistantPromptReply(" SELAM ") { error("Greeting must stay local") }.startsWith("Merhaba"))
  var calls=0
  assertEquals("remote",assistantPromptReply("Merhaba, yarın hava nasıl?") { calls++;"remote" })
  assertEquals(1,calls)
 }
 @Test fun weatherQuestionReachesPlatformWithOriginalIdsAndStructuredWeather() = runBlocking {
  val now=java.time.Instant.parse("2026-11-26T12:00:00Z").toEpochMilli()
  val weather=WeatherData(cityName="Ankara",temperature="18°",timestamp=now,timezone="Europe/Istanbul")
  val user=assistantMessageForSend(emptyList(),"Yarın Ankara'da ne giysem?",false,null)!!
  val retry=assistantMessageForSend(listOf(user),user.text,true,user.id)!!
  val conversation=java.util.UUID.randomUUID().toString()
  var calls=0
  for(message in listOf(user,retry)) {
   assertEquals("platform answer",assistantPromptReply(message.text) {
    calls++
    val request=BackendChatbotAskRequest(message.text,request_id=message.id,conversation_id=conversation,
     context=assistantClientContext(null,weather,emptyList(),false,now))
    assertEquals(user.id,request.request_id)
    assertEquals(conversation,request.conversation_id)
    assertEquals("Ankara",request.context!!.weather_data!!.location!!.name)
    assertTrue(Json.encodeToString(request).contains("weather_data"))
    "platform answer"
   })
  }
  assertEquals(2,calls)
 }
 @Test fun missingOrOldWeatherStillAllowsPlatformTools() {
  val missing=assistantClientContext(null,null,emptyList(),false)
  assertNull(missing.weather_data)
  assertTrue(missing.weather!!.contains("platformun hava araçlarını kullan"))
  val now=java.time.Instant.parse("2026-11-26T12:00:00Z").toEpochMilli()
  val old=WeatherData(cityName="Ankara",temperature="18°",timestamp=now-14400000,timezone="Europe/Istanbul")
  val context=assistantClientContext(null,old,emptyList(),false,now)
  assertTrue(context.weather_data!!.stale)
  assertTrue(context.weather!!.contains("platformun hava araçlarıyla doğrula"))
 }
 @Test fun remoteFailureAndCancellationNeverBecomeLocalGreeting() = runBlocking {
  try {
   assistantPromptReply("Selam, önemli bir karar vereceğim") { throw IllegalStateException("remote failed") }
   fail("Error must propagate")
  } catch(e:IllegalStateException) { assertEquals("remote failed",e.message) }
  try {
   assistantPromptReply("Bugün hava nasıl?") { throw CancellationException("cancel") }
   fail("Cancellation must propagate")
  } catch(_:CancellationException) { }
 }
}
