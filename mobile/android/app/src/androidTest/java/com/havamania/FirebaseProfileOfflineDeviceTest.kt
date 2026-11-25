package com.havamania

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.havamania.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Actual offline SDK queue and production rules; no live service or account deletion API. */
class FirebaseProfileOfflineDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun acceptedOfflineWriteDoesNotAcknowledgeTimedOutForm() = runOfflineProfile(false)
    @Test fun rejectedOfflineWritePreservesConfirmedPreferencesAndCanRetry() = runOfflineProfile(true)

    private fun runOfflineProfile(reject: Boolean) = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true")
        val nonce = args.getString("profileOfflineNonce")
        assumeTrue(nonce != null)
        UUID.fromString(nonce)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "profile-offline-${UUID.randomUUID()}")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val repository = UserProfileRepository { db }
        val epoch = PreferenceWriteEpoch()
        var scenario: ActivityScenario<ProfileEditSdkActivity>? = null
        var owner: String? = null
        var marker = false
        var stage = "setup"
        lateinit var model: ProfileViewModel
        val snapshotWrites = java.util.concurrent.atomic.AtomicInteger()
        fun awaitUi(timeout: Long = 15_000, predicate: () -> Boolean) = compose.waitUntil(timeout) {
            compose.mainClock.advanceTimeByFrame(); predicate()
        }
        suspend fun access(blocked: Boolean) = withContext(Dispatchers.IO) {
            val connection = java.net.URL("http://127.0.0.1:9397/${if (blocked) "block" else "unblock"}")
                .openConnection() as java.net.HttpURLConnection
            try {
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.connectTimeout = 3000; connection.readTimeout = 3000
                connection.setRequestProperty("X-Emulator-Test", nonce)
                connection.outputStream.use { it.write("{\"uid\":\"$owner\"}".toByteArray()) }
                assertEquals(200, connection.responseCode); connection.inputStream.close()
            } finally { connection.disconnect() }
        }
        try { withTimeout(100_000) {
            val user = auth.createUserWithEmailAndPassword("offline-${UUID.randomUUID()}@example.invalid", "local-test-password").await().user!!
            owner = user.uid
            user.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Confirmed name" }).await()
            db.collection("users").document(user.uid).set(UserProfile(uid = user.uid,
                name = "Confirmed name", bio = "Confirmed biography", email = user.email.orEmpty(), createdAt = 123L)).await()
            ProfileEditSdkSession.factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                    AuthViewModel::class.java -> AuthViewModel(application, auth, db)
                    ProfileViewModel::class.java -> ProfileViewModel(application, auth, db, repository,
                        writeGate = { true }, snapshotWriter = { profile ->
                            ThemeManager.saveProfileSnapshot(application.dataStore, profile,
                                { auth.currentUser?.uid }, { false }, epoch)
                            snapshotWrites.incrementAndGet()
                        }, storageProvider = { error("No Storage access") }, profileBasicsWriter = { uid, name, bio ->
                            ThemeManager.saveProfileBasics(application.dataStore, uid, name, bio,
                                { auth.currentUser?.uid }, { false }, epoch)
                        })
                    else -> error("Unexpected model")
                } as T
            }
            val opened = ActivityScenario.launch(ProfileEditSdkActivity::class.java); scenario = opened
            opened.onActivity { model = it.profileModel }
            stage = "confirmed baseline"
            awaitUi {
                val state = repository.readState.value
                state is ProfileReadState.Ready && state.serverConfirmed && snapshotWrites.get() > 0
            }
            stage = "confirmed baseline preferences"
            assertEquals("Confirmed name", ThemeManager.getUserName(application, user.uid).first())
            awaitUi { model.profileState.value is ProfileState.Success }
            stage = "offline draft"
            db.disableNetwork().await()
            compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Offline draft")
            compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("Offline biography")
            compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().performClick()
            awaitUi { (repository.readState.value as? ProfileReadState.Ready)?.hasPendingWrites == true }
            val pending = repository.readState.value as ProfileReadState.Ready
            assertFalse(pending.serverConfirmed)
            assertEquals("Offline draft", pending.profile.name)
            assertEquals(ProfileSaveState.Saving, model.saveState.value)
            opened.onActivity { assertEquals(0, it.backRequests) }
            assertEquals("Confirmed name", auth.currentUser?.displayName)
            if (reject) {
                stage = "block owner in emulator rules"
                access(true); marker = true
                db.enableNetwork().await()
                awaitUi(30_000) { model.saveState.value is ProfileSaveState.Failed }
                awaitUi { repository.readState.value is ProfileReadState.Failed }
                assertEquals("Profil kaydedilemedi. Lütfen tekrar deneyin.", (model.saveState.value as ProfileSaveState.Failed).message)
                assertEquals("Confirmed name", ThemeManager.getUserName(application, user.uid).first())
                assertEquals("Confirmed biography", ThemeManager.getUserBio(application, user.uid).first())
                assertEquals("Confirmed name", auth.currentUser?.displayName)
                opened.onActivity { assertEquals(0, it.backRequests) }
                compose.onNodeWithText("Offline draft").assertExists()
                stage = "restore owner and refresh rejected draft"
                access(false); marker = false
                opened.onActivity { model.refresh() }
                awaitUi { (repository.readState.value as? ProfileReadState.Ready)?.serverConfirmed == true }
                awaitUi { model.profileState.value is ProfileState.Success }
                val unchanged = db.collection("users").document(user.uid).get(Source.SERVER).await()
                assertEquals("Confirmed name", unchanged.getString("name"))
                assertEquals("Confirmed biography", unchanged.getString("bio"))
            } else {
                stage = "offline timeout is not success"
                awaitUi(30_000) { model.saveState.value is ProfileSaveState.Failed }
                assertEquals("Kayıt onayı alınamadı. Bağlantınızı kontrol edip tekrar deneyin.", (model.saveState.value as ProfileSaveState.Failed).message)
                assertEquals("Confirmed name", ThemeManager.getUserName(application, user.uid).first())
                stage = "late server acceptance and mirror repair"
                db.enableNetwork().await()
                awaitUi { val state = repository.readState.value
                    state is ProfileReadState.Ready && state.serverConfirmed && state.profile.name == "Offline draft" }
                awaitUi { auth.currentUser?.displayName == "Offline draft" && !model.profileReconciliationBusy.value }
                ThemeManager.getUserName(application, user.uid).first { it == "Offline draft" }
                assertTrue(model.saveState.value is ProfileSaveState.Failed)
                opened.onActivity { assertEquals(0, it.backRequests) }
            }
            stage = "explicit retry"
            compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().performClick()
            awaitUi { var backs = 0; opened.onActivity { backs = it.backRequests }; backs == 1 }
            assertEquals("Offline draft", auth.currentUser?.displayName)
            assertEquals("Offline draft", ThemeManager.getUserName(application, user.uid).first())
            assertEquals("Offline biography", ThemeManager.getUserBio(application, user.uid).first())
            val saved = db.collection("users").document(user.uid).get(Source.SERVER).await()
            assertEquals("Offline draft", saved.getString("name"))
            assertEquals(123L, saved.getLong("createdAt"))
        } } catch (failure: Throwable) {
            throw AssertionError("offline reject=$reject stage=$stage ownerMatches=${auth.currentUser?.uid == owner} snapshots=${snapshotWrites.get()} read=${repository.readState.value} profile=${runCatching { model.profileState.value }.getOrNull()} save=${runCatching { model.saveState.value }.getOrNull()}", failure)
        } finally {
            withContext(NonCancellable) { if (marker) runCatching { access(false) } }
            scenario?.close(); ProfileEditSdkSession.factory = null
            repository.stopObserving(); epoch.invalidate(); auth.signOut()
            withContext(NonCancellable) {
                try { withTimeout(10_000) {
                    db.enableNetwork().await()
                    owner?.let { uid -> application.dataStore.edit { prefs ->
                        prefs.asMap().keys.filter { it.name.startsWith("havamania:$uid:") }.forEach { prefs.remove(it) }
                    } }
                    db.terminate().await()
                } } finally { app.delete() }
            }
        }
    }
}
