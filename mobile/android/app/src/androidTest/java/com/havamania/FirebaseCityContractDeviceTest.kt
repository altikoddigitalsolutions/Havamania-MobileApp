package com.havamania

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import java.util.UUID

/** Opt in explicitly. A separate FirebaseApp, demo project and loopback endpoints only. */
class FirebaseCityContractDeviceTest {
    private var app: FirebaseApp? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private val paris = GeocodingResultDto(1, "Paris", 48.8, 2.3, countryCode = "FR")

    @Before fun connect() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        withTimeout(30_000) {
            val options = FirebaseOptions.Builder().setProjectId("demo-havamania-audit")
                .setApplicationId("1:1234567890:android:0000000000000000")
                .setApiKey("fake-emulator-api-key").build()
            val localApp = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext,
                options, "city-contract-" + UUID.randomUUID())
            app = localApp
            assertEquals("demo-havamania-audit", localApp.options.projectId)
            auth = FirebaseAuth.getInstance(localApp)
            auth.useEmulator("127.0.0.1", 9096)
            db = FirebaseFirestore.getInstance(localApp)
            db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            db.useEmulator("127.0.0.1", 8086)
            uid = auth.signInAnonymously().await().user!!.uid
        }
    }
    @After fun disconnect() = runBlocking {
        if (app != null) {
            try { withTimeout(10_000) { db.terminate().await() } }
            finally { auth.signOut(); app?.delete(); app = null }
        }
    }
    @Test fun actualAndroidSerializationRoundTripsAndPreservesProfile() = runBlocking {
        withTimeout(30_000) {
            val ref = db.collection("users").document(uid)
            ref.set(mapOf("displayName" to "Preserved", "defaultCity" to "Ankara")).await()
            ref.set(defaultCityProfileFields(paris), SetOptions.merge()).await()
            val snapshot = ref.get(com.google.firebase.firestore.Source.SERVER).await()
            assertFalse(snapshot.metadata.isFromCache)
            assertFalse(snapshot.metadata.hasPendingWrites())
            assertEquals("Preserved", snapshot.getString("displayName"))
            assertEquals(paris, resolveDefaultCityProfile(snapshot.getString("defaultCity"),
                snapshot.get("defaultCityLocation"), emptyList()))
            val wrong = defaultCityProfileFields(paris.copy(latitude = 49.0)).toMutableMap()
            wrong["defaultCity"] = "Ankara"
            try { ref.set(wrong, SetOptions.merge()).await(); fail("Conflicting name accepted") }
            catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
            assertEquals("Paris", ref.get(com.google.firebase.firestore.Source.SERVER).await().getString("defaultCity"))
        }
    }
    @Test fun actualTransactionRejectsOtherIdentityAndAllowsMatchingDelete() = runBlocking {
        withTimeout(30_000) {
            val ref = db.collection("users").document(uid).collection("cities").document("1")
            val other = paris.copy(latitude = 33.6, longitude = -95.5, countryCode = "US")
            ref.set(other).await()
            suspend fun apply(change: CitySyncChange, allowed: Boolean = true): Boolean = db.runTransaction { tx ->
                stageCityDocumentMutation(change, { allowed },
                    read = { val snap = tx.get(ref); if (snap.exists()) snap.toObject(GeocodingResultDto::class.java) else null },
                    write = { tx.set(ref, change.city) }, delete = { tx.delete(ref) })
            }.await()
            try { apply(CitySyncChange(paris, true)); fail("Wrong identity deleted") }
            catch (e: Exception) { assertTrue("SDK must preserve conflict cause", isCityIdentityConflict(e)) }
            assertEquals(other, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
            assertFalse(apply(CitySyncChange(other, true), false))
            assertTrue(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
            assertTrue(apply(CitySyncChange(other, true)))
            assertFalse(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
            assertTrue(apply(CitySyncChange(other, true))) // Missing delete is idempotent.
            assertTrue(apply(CitySyncChange(paris, false)))
            assertEquals(paris, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
        }
    }
    @Test fun authenticatedAndroidCannotAccessAnotherAccount() = runBlocking {
        withTimeout(30_000) {
            val other = db.collection("users").document("another-" + UUID.randomUUID())
            try { other.set(defaultCityProfileFields(paris)).await(); fail("Other account written") }
            catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
            try { other.get(com.google.firebase.firestore.Source.SERVER).await(); fail("Other account read") }
            catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
        }
    }
}
