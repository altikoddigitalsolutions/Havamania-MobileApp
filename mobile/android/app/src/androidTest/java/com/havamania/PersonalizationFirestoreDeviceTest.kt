package com.havamania
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
class PersonalizationFirestoreDeviceTest {
 @Test fun androidObjectsRoundTripWithoutReenablingPersonalizationOrCrossingOwners() = runBlocking<Unit> {
  assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract")=="true")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val id=java.util.UUID.randomUUID().toString()
  val app=FirebaseApp.initializeApp(context,FirebaseOptions.Builder().setProjectId("demo-havamania-audit")
   .setApplicationId("1:1234567890:android:0000000000000000").setApiKey("fake-emulator-api-key").build(),"preference-$id")
  val auth=FirebaseAuth.getInstance(app).apply {useEmulator("127.0.0.1",9096)}
  val db=FirebaseFirestore.getInstance(app).apply {
   firestoreSettings=FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
   useEmulator("127.0.0.1",8086)
  }
  try { withTimeout(60000) {
   val email="preference-$id@example.invalid"
   val password="local-test-password"
   val owner=auth.createUserWithEmailAndPassword(email,password).await().user!!.uid
   val ref=db.collection("users").document(owner)
   ref.set(mapOf("uid" to owner,"name" to "Keep","personalizationEnabled" to false,"personalizationProfile" to mapOf(
    "uid" to owner,"personalizationEnabled" to false,"futureField" to "keep"))).await()
   ref.update(personalizationPreferenceUpdates(owner,listOf("camp"),listOf("Kültür"),
    WeatherPreferences(rainSensitive=true,humiditySensitive=true),123L)).await()
   val snapshot=ref.get(Source.SERVER).await()
   val profile=snapshot.get("personalizationProfile",PersonalizationProfile::class.java)!!
   assertFalse(profile.personalizationEnabled)
   assertTrue(profile.weatherPreferences.rainSensitive)
   assertTrue(profile.weatherPreferences.humiditySensitive)
   assertEquals(listOf("camp"),profile.selectedInterests)
   assertEquals("keep",snapshot.getString("personalizationProfile.futureField"))
   assertEquals("Keep",snapshot.getString("name"))
   // Reconnect through a fresh named SDK client and the actual screen's UserProfile model.
   val reopenedApp=FirebaseApp.initializeApp(context,app.options,"reopened-$id")
   val reopenedAuth=FirebaseAuth.getInstance(reopenedApp).apply {useEmulator("127.0.0.1",9096)}
   val reopenedDb=FirebaseFirestore.getInstance(reopenedApp).apply {
    firestoreSettings=FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
    useEmulator("127.0.0.1",8086)
   }
   try {
    reopenedAuth.signInWithEmailAndPassword(email,password).await()
    val loaded=reopenedDb.collection("users").document(owner).get(Source.SERVER).await().toObject(UserProfile::class.java)!!
    assertFalse(loaded.personalizationEnabled)
    assertEquals(listOf("camp"),loaded.personalizationProfile!!.selectedInterests)
    assertEquals(listOf("Kültür"),loaded.personalizationProfile!!.travelStyles)
    assertTrue(loaded.personalizationProfile!!.weatherPreferences!!.rainSensitive)
    assertTrue(loaded.personalizationProfile!!.weatherPreferences!!.humiditySensitive)
   } finally {reopenedAuth.signOut();reopenedDb.terminate().await();reopenedApp.delete()}
   ref.set(mapOf("uid" to owner,"name" to "Legacy")).await()
   ref.update(personalizationPreferenceUpdates(owner,emptyList(),emptyList(),WeatherPreferences())).await()
   val legacy=ref.get(Source.SERVER).await()
   assertEquals("Legacy",legacy.getString("name"))
   assertNull(legacy.get("personalizationProfile.personalizationEnabled"))
   assertEquals(emptyList<String>(),legacy.get("personalizationProfile.selectedInterests"))
   val other=auth.createUserWithEmailAndPassword("other-$email",password).await().user!!
   try {
    ref.update(personalizationPreferenceUpdates(owner,listOf("wrong"),emptyList(),WeatherPreferences())).await()
    fail("Another owner must not update preferences")
   } catch(e:FirebaseFirestoreException) {assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,e.code)}
   other.delete().await()
   auth.signInWithEmailAndPassword(email,password).await()
   assertEquals(emptyList<String>(),ref.get(Source.SERVER).await().get("personalizationProfile.selectedInterests"))
   ref.delete().await()
   auth.currentUser!!.delete().await()
  } } finally { auth.signOut();db.terminate().await();app.delete() }
 }
}
