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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class FirebaseProfileEditDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun formWaitsForAllWritesRetriesFailureAndIsolatesAccountSwitch() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "edit-form-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val repository = UserProfileRepository { db }
        val epoch = PreferenceWriteEpoch()
        val gates = Channel<CompletableDeferred<Unit>>(Channel.UNLIMITED)
        val owners = mutableListOf<String>()
        val password = "local-test-password"
        val emailB = "edit-b-$id@example.invalid"
        val emailA = "edit-a-$id@example.invalid"
        var holdSdk = true
        var failLocal = true
        var blockLocal = false
        var scenario: ActivityScenario<ProfileEditSdkActivity>? = null
        lateinit var profile: ProfileViewModel
        lateinit var account: AuthViewModel
        fun awaitUi(predicate: () -> Boolean) = compose.waitUntil(15_000) {
            compose.mainClock.advanceTimeByFrame(); predicate()
        }
        fun fill(name: String, bio: String) {
            compose.onAllNodes(hasSetTextAction())[0].performTextReplacement(name)
            compose.onAllNodes(hasSetTextAction())[1].performTextReplacement(bio)
        }
        fun save() = compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().performClick()
        try { withTimeout(90_000) {
            val uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user!!.uid
            owners.add(uidB)
            db.collection("users").document(uidB).set(UserProfile(uid = uidB, name = "B Seed", bio = "B biography", email = emailB)).await()
            ThemeManager.saveProfileBasics(application.dataStore, uidB, "Local B", "Local B bio",
                { auth.currentUser?.uid }, { false }, epoch)
            val userA = auth.createUserWithEmailAndPassword(emailA, password).await().user!!
            val uidA = userA.uid; owners.add(uidA)
            db.collection("users").document(uidA).set(UserProfile(uid = uidA, name = "Initial A", bio = "Initial bio", email = emailA)).await()
            ProfileEditSdkSession.factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                    AuthViewModel::class.java -> AuthViewModel(application, auth, db)
                    ProfileViewModel::class.java -> ProfileViewModel(application, auth, db, repository,
                        writeGate = { true }, snapshotWriter = {},
                        storageProvider = { error("No Storage access in edit test") },
                        profileBasicsWriter = { uid, name, bio ->
                            if (failLocal) throw java.io.IOException("local-injected-write-failure")
                            ThemeManager.saveProfileBasics(application.dataStore, uid, name, bio,
                                { auth.currentUser?.uid }, { blockLocal }, epoch)
                        }, beforeProfileSdkWrite = {
                            if (holdSdk) {
                                val gate = CompletableDeferred<Unit>(); gates.send(gate); gate.await()
                            }
                        })
                    else -> error("Unexpected ViewModel")
                } as T
            }
            val opened = ActivityScenario.launch(ProfileEditSdkActivity::class.java); scenario = opened
            opened.onActivity { profile = it.profileModel; account = it.authModel }
            awaitUi { (profile.profileState.value as? ProfileState.Success)?.profile?.uid == uidA &&
                !profile.profileReconciliationBusy.value && auth.currentUser?.displayName == "Initial A" }
            compose.onNodeWithText("Initial A").assertExists()
            compose.onNodeWithText(emailA).assertExists()
            fill("Edited A", "Edited biography")
            save()
            val firstGate = gates.receive()
            assertEquals("Edited A", db.collection("users").document(uidA).get(Source.SERVER).await().getString("name"))
            assertEquals(ProfileSaveState.Saving, profile.saveState.value)
            opened.onActivity { assertEquals(0, it.backRequests) }
            compose.waitForIdle(); opened.recreate()
            opened.onActivity { assertSame(profile, it.profileModel); assertSame(account, it.authModel); assertEquals(0, it.backRequests) }
            compose.onNodeWithText("Edited A").assertExists()
            firstGate.complete(Unit)
            awaitUi { profile.saveState.value is ProfileSaveState.Failed }
            assertEquals("Edited A", auth.currentUser?.displayName)
            compose.onNodeWithText("Profil kaydedilemedi. Lütfen tekrar deneyin.").performScrollTo().assertIsDisplayed()
            opened.onActivity { assertEquals(0, it.backRequests) }
            assertEquals("", ThemeManager.getUserName(application, uidA).first())
            holdSdk = false; failLocal = false; blockLocal = true
            save()
            awaitUi { profile.saveState.value is ProfileSaveState.Failed }
            opened.onActivity { assertEquals(0, it.backRequests) }
            assertEquals("", ThemeManager.getUserName(application, uidA).first())
            blockLocal = false
            save()
            awaitUi { var backs = 0; opened.onActivity { backs = it.backRequests }; backs == 1 }
            assertEquals("Edited A", ThemeManager.getUserName(application, uidA).first())
            assertEquals("Edited biography", ThemeManager.getUserBio(application, uidA).first())
            assertEquals("Edited A", auth.currentUser?.displayName)
            assertEquals("Edited biography", db.collection("users").document(uidA).get(Source.SERVER).await().getString("bio"))
            holdSdk = true
            fill("Late A", "Late biography"); save()
            val cancelledGate = gates.receive()
            assertEquals(ProfileSaveState.Saving, profile.saveState.value)
            opened.onActivity { account.signOut(); account.signIn(emailB, password) }
            account.authState.first { it is AuthState.Authenticated }
            awaitUi { (profile.profileState.value as? ProfileState.Success)?.profile?.uid == uidB &&
                !profile.profileReconciliationBusy.value && auth.currentUser?.displayName == "B Seed" }
            cancelledGate.complete(Unit)
            compose.waitForIdle()
            compose.onNodeWithText("B Seed").assertExists()
            compose.onNodeWithText("Late A").assertDoesNotExist()
            assertEquals(uidB, auth.currentUser?.uid)
            assertEquals("B Seed", auth.currentUser?.displayName)
            assertEquals("Edited A", userA.displayName)
            assertEquals("Local B", ThemeManager.getUserName(application, uidB).first())
            assertEquals("Local B bio", ThemeManager.getUserBio(application, uidB).first())
            assertEquals("Edited A", ThemeManager.getUserName(application, uidA).first())
            assertFalse(profile.saveState.value is ProfileSaveState.Saved)
            opened.onActivity { assertEquals(1, it.backRequests); account.signOut() }
        } } finally {
            scenario?.close(); ProfileEditSdkSession.factory = null
            repository.stopObserving(); epoch.invalidate(); auth.signOut(); gates.close()
            withContext(NonCancellable) {
                try { withTimeout(10_000) {
                    application.dataStore.edit { prefs ->
                        prefs.asMap().keys.filter { key -> owners.any { key.name.startsWith("havamania:$it:") } }.forEach { prefs.remove(it) }
                    }
                    db.terminate().await()
                } } finally { app.delete() }
            }
        }
    }
}
