package com.havamania
import org.junit.Assert.*
import org.junit.Test
class PersonalizationPreferenceUpdatesTest {
 @Test fun editedPreferencesNeverReplaceOptInOrWholeProfile() {
  val changes=personalizationPreferenceUpdates("A",listOf("camp"),listOf("Kültür"),WeatherPreferences(rainSensitive=true),123L)
  assertFalse(changes.containsKey("personalizationProfile"))
  assertFalse(changes.containsKey("personalizationProfile.personalizationEnabled"))
  assertEquals("A",changes["personalizationProfile.uid"])
  assertEquals(listOf("camp"),changes["personalizationProfile.selectedInterests"])
  assertEquals(123L,changes["personalizationProfile.lastUpdated"])
  assertEquals(true,changes["onboardingCompleted"])
 }
 @Test fun explicitEmptySelectionsClearOnlyEditableListsAndSnapshotInputs() {
  val interests=mutableListOf("camp")
  val styles=mutableListOf("Kültür")
  val changes=personalizationPreferenceUpdates("B",interests,styles,WeatherPreferences())
  interests.clear();styles.clear()
  assertEquals(listOf("camp"),changes["personalizationProfile.selectedInterests"])
  val empty=personalizationPreferenceUpdates("B",emptyList(),emptyList(),WeatherPreferences())
  assertEquals(emptyList<String>(),empty["personalizationProfile.selectedInterests"])
  assertEquals(emptyList<String>(),empty["personalizationProfile.travelStyles"])
 }
}
