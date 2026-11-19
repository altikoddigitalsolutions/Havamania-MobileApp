package com.havamania

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.edit
import com.havamania.ui.theme.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Real production ViewModels + guarded snapshot + actual Activity, demo services only. */
class FirebaseProfileActivityDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun <T> awaitValue(label: String, flow: kotlinx.coroutines.flow.StateFlow<T>, predicate: (T) -> Boolean): T {
        try { compose.waitUntil(timeoutMillis = 10_000) { predicate(flow.value) } }
        catch (error: AssertionError) { throw AssertionError("$label; latest=${flow.value}", error) }
        return flow.value
    }
    @Test fun remoteProfilePersistsIntoSettingsAcrossActualRecreationAndAccountSwitch() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true" && args.getString("cityProfileContract") == "true")
        var stage = "setup"
        try { withTimeout(60_000) {
            val nonce = args.getString("cityProfileNonce")!!
            UUID.fromString(nonce)
            val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as android.app.Application
            val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
                .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
                .setApiKey("fake-emulator-api-key").build(), "profile-activity-${UUID.randomUUID()}")
            val auth = FirebaseAuth.getInstance(app)
            auth.useEmulator("127.0.0.1", 9096)
            val db = FirebaseFirestore.getInstance(app)
            db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            db.useEmulator("127.0.0.1", 8086)
            val repository = UserProfileRepository { db }
            val epoch = PreferenceWriteEpoch()
            val owners = mutableListOf<String>()
            var scenario: ActivityScenario<ProfileSettingsIntegrationActivity>? = null
            suspend fun premium(owner: String) = withContext(Dispatchers.IO) {
                val connection = java.net.URL("http://127.0.0.1:9396/seed").openConnection() as java.net.HttpURLConnection
                try {
                    connection.requestMethod = "POST"; connection.doOutput = true
                    connection.connectTimeout = 3000; connection.readTimeout = 3000
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("X-Emulator-Test", nonce)
                    connection.outputStream.use { it.write("{\"uid\":\"$owner\",\"mode\":\"canonical_true\"}".toByteArray()) }
                    assertEquals(200, connection.responseCode); connection.inputStream.close()
                } finally { connection.disconnect() }
            }
            try {
                val ownerA = auth.signInAnonymously().await().user!!.uid
                owners.add(ownerA)
                db.collection("users").document(ownerA).set(UserProfile(uid = ownerA, name = "Birleşik A")).await()
                premium(ownerA)
                ThemeManager.saveDefaultCity(application, ownerA,
                    GeocodingResultDto(323786, "Ankara", 39.9, 32.8, countryCode = "TR"))
                ProfileSettingsIntegrationSession.factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        val model = when (modelClass) {
                            ProfileViewModel::class.java -> ProfileViewModel(application, auth, db, repository,
                                writeGate = { true }, snapshotWriter = { profile ->
                                    ThemeManager.saveProfileSnapshot(application.dataStore, profile,
                                        { auth.currentUser?.uid }, { false }, epoch)
                                }, storageProvider = { error("No Storage access in read-only Activity test") })
                            ThemeViewModel::class.java -> ThemeViewModel(application, auth, db, repository,
                                weatherAccountChanged = {}, syncGate = { false })
                            else -> error("Unexpected ViewModel")
                        }
                        @Suppress("UNCHECKED_CAST") return model as T
                    }
                }
                stage = "launch Activity"
                scenario = ActivityScenario.launch(ProfileSettingsIntegrationActivity::class.java)
                lateinit var profileVm: ProfileViewModel
                lateinit var themeVm: ThemeViewModel
                scenario.onActivity { profileVm = it.profileModel; themeVm = it.themeModel }
                stage = "wait A snapshot"
                awaitValue("A snapshot to settings", themeVm.userName) { it == "Birleşik A" }
                awaitValue("A premium confirmation", themeVm.isPremium) { it }
                stage = "assert A visible"
                compose.onNodeWithText("Yerel ad: Birleşik A").assertIsDisplayed()
                compose.onNodeWithText("Havamania Premium Üyesi").assertIsDisplayed()
                compose.onNodeWithText("Silme penceresini aç").performClick()
                compose.onAllNodes(hasSetTextAction())[0].performTextInput("integration-test-password")
                compose.onAllNodes(hasSetTextAction())[1].performTextInput("SİL")
                stage = "recreate Activity"
                scenario.recreate()
                scenario.onActivity { assertSame(profileVm, it.profileModel); assertSame(themeVm, it.themeModel) }
                compose.onNodeWithText("Hesabımı Sil").assertDoesNotExist()
                stage = "assert A visible"
                compose.onNodeWithText("Yerel ad: Birleşik A").assertIsDisplayed()
                compose.onNodeWithText("Yerel şehir: Ankara").assertIsDisplayed()
                compose.onNodeWithText("Havamania Premium Üyesi").assertIsDisplayed()
                db.collection("users").document(ownerA).update("name", "Birleşik A güncel").await()
                awaitValue("A updated snapshot", themeVm.userName) { it == "Birleşik A güncel" }
                assertEquals("Birleşik A güncel", ThemeManager.getUserName(application, ownerA).first())
                stage = "switch account"
                auth.signOut()
                awaitValue("Logout state", profileVm.profileState) { it is ProfileState.Idle }
                val ownerB = auth.signInAnonymously().await().user!!.uid
                owners.add(ownerB)
                ThemeManager.saveDefaultCity(application, ownerB,
                    GeocodingResultDto(750269, "Bursa", 40.2, 29.1, countryCode = "TR"))
                db.collection("users").document(ownerB).set(mapOf("uid" to ownerB, "name" to "Birleşik B"),
                    com.google.firebase.firestore.SetOptions.merge()).await()
                awaitValue("B snapshot to settings", themeVm.userName) { it == "Birleşik B" }
                awaitValue("B default city", themeVm.defaultCity) { it?.name == "Bursa" }
                assertFalse(themeVm.isPremium.value)
                stage = "recreate Activity"
                scenario.recreate()
                compose.onNodeWithText("Yerel ad: Birleşik B").assertIsDisplayed()
                compose.onNodeWithText("Yerel şehir: Bursa").assertIsDisplayed()
                compose.onNodeWithText("Birleşik B").assertExists()
                compose.onNodeWithText("Birleşik A güncel").assertDoesNotExist()
                compose.onNodeWithText("Havamania Premium Üyesi").assertDoesNotExist()
                assertEquals("Birleşik A güncel", ThemeManager.getUserName(application, ownerA).first())
            } finally { withContext(NonCancellable) { withTimeout(10_000) {
                stage = "close Activity (previous: $stage)"
                scenario?.close()
                ProfileSettingsIntegrationSession.factory = null
                repository.stopObserving()
                stage = "cleanup DataStore (previous: $stage)"
                application.dataStore.edit { prefs ->
                    prefs.asMap().keys.filter { key -> owners.any { key.name.startsWith("havamania:$it:") } }
                        .forEach { prefs.remove(it) }
                }
                try { withTimeout(10_000) { db.terminate().await() } }
                finally { auth.signOut(); app.delete() }
            } } }
        } } catch (error: TimeoutCancellationException) { throw AssertionError("Integration timeout at $stage", error) }
    }
}
