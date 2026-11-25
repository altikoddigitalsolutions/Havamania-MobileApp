package com.havamania

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
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

/** Real demo SDKs, production ViewModels, personalization form and navigation in a debug Activity. */
class FirebaseAccountEntryActivityDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun awaitRoute(scenario: ActivityScenario<AccountEntrySdkActivity>, target: String) {
        try {
            compose.waitUntil(15_000) {
                compose.mainClock.advanceTimeByFrame()
                var route: String? = null
                scenario.onActivity { route = it.route }
                route == target
            }
        } catch (failure: Throwable) {
            var detail = ""
            scenario.onActivity {
                val profile = (it.profileModel.profileState.value as? ProfileState.Success)?.profile
                detail = "expected=$target route=${it.route} actual=${it.nav.currentDestination?.route} auth=${it.authModel.authState.value::class.simpleName} error=${(it.authModel.authState.value as? AuthState.Error)?.message} profile=${it.profileModel.profileState.value::class.simpleName} profileError=${(it.profileModel.profileState.value as? ProfileState.Error)?.message} uid=${profile?.uid} completed=${profile?.onboardingCompleted} visits=${it.visited}"
            }
            throw AssertionError(detail, failure)
        }
    }
    private fun recreate(scenario: ActivityScenario<AccountEntrySdkActivity>, target: String) {
        try {
            compose.waitForIdle()
            scenario.recreate()
            awaitRoute(scenario, target)
        } catch (failure: Throwable) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val power = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            val keyguard = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            var activity = "unavailable"
            runCatching { scenario.onActivity {
                activity = "state=${it.lifecycle.currentState} focus=${it.hasWindowFocus()} finishing=${it.isFinishing} destroyed=${it.isDestroyed}"
            } }
            throw AssertionError("recreate target=$target scenario=${scenario.state} interactive=${power.isInteractive} locked=${keyguard.isDeviceLocked} activity=$activity lifecycle=${AccountEntrySdkSession.lifecycleEvents}", failure)
        }
    }

    @Test fun newAccountRecreatesPersonalizesPersistsAndNavigatesWithRealSdk() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        AccountEntrySdkSession.lifecycleEvents.clear()
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "entry-sdk-${UUID.randomUUID()}")
        assertEquals("demo-havamania-audit", app.options.projectId)
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val repository = UserProfileRepository { db }
        val epoch = PreferenceWriteEpoch()
        val entered = CompletableDeferred<String>()
        val release = CompletableDeferred<Unit>()
        var owner: String? = null
        var scenario: ActivityScenario<AccountEntrySdkActivity>? = null
        try { withTimeout(90_000) {
            AccountEntrySdkSession.factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                    AuthViewModel::class.java -> AuthViewModel(application, auth, db) { uid ->
                        entered.complete(uid); release.await()
                    }
                    ProfileViewModel::class.java -> ProfileViewModel(application, auth, db, repository,
                        writeGate = { true }, snapshotWriter = { profile ->
                            ThemeManager.saveProfileSnapshot(application.dataStore, profile,
                                { auth.currentUser?.uid }, { false }, epoch)
                        }, storageProvider = { error("No Storage use in account entry test") },
                        personalizationBasicsWriter = { uid, interests ->
                            ThemeManager.savePersonalizationBasics(application.dataStore, uid, interests,
                                { auth.currentUser?.uid }, { false }, epoch)
                        })
                    else -> error("Unexpected model")
                } as T
            }
            val opened = ActivityScenario.launch(AccountEntrySdkActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.AUTH_WELCOME)
            lateinit var originalAuth: AuthViewModel
            lateinit var originalProfile: ProfileViewModel
            opened.onActivity {
                originalAuth = it.authModel; originalProfile = it.profileModel
                it.authModel.signUp("Demo Gezgin", "entry-${UUID.randomUUID()}@example.invalid", "local-test-password")
            }
            val uid = entered.await(); owner = uid
            compose.waitUntil(15_000) {
                compose.mainClock.advanceTimeByFrame()
                (originalProfile.profileState.value as? ProfileState.Success)?.profile?.uid == uid
            }
            assertEquals(false, db.collection("users").document(uid).get(Source.SERVER).await().getBoolean("onboardingCompleted"))
            opened.onActivity {
                assertTrue(it.authModel.authState.value is AuthState.Loading)
                assertNull(it.authModel.currentUser)
                assertFalse(it.visited.contains(Routes.PERSONALIZATION))
                assertFalse(it.visited.contains(Routes.WEATHER_ROOT))
            }
            recreate(opened, Routes.AUTH_WELCOME)
            opened.onActivity { assertSame(originalAuth, it.authModel); assertSame(originalProfile, it.profileModel) }
            release.complete(Unit)
            awaitRoute(opened, Routes.PERSONALIZATION)
            opened.onActivity { assertFalse(it.visited.contains(Routes.WEATHER_ROOT)) }
            compose.onNodeWithText("Meteoroloji").performScrollTo().assertIsEnabled().performClick()
            compose.onNodeWithText("Meteoroloji").assertIsSelected()
            recreate(opened, Routes.PERSONALIZATION)
            compose.onNodeWithText("Meteoroloji").performScrollTo().assertIsSelected()
            // A real stopped Activity must retain the selected form and models across recreation.
            opened.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            opened.recreate()
            assertEquals(androidx.lifecycle.Lifecycle.State.CREATED, opened.state)
            opened.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            awaitRoute(opened, Routes.PERSONALIZATION)
            opened.onActivity { assertSame(originalAuth, it.authModel); assertSame(originalProfile, it.profileModel) }
            compose.onNodeWithText("Meteoroloji").performScrollTo().assertIsSelected()
            compose.onNodeWithText("TERCİHLERİ KAYDET").performScrollTo().assertIsEnabled().performClick()
            awaitRoute(opened, Routes.WEATHER_ROOT)
            val saved = db.collection("users").document(uid).get(Source.SERVER).await()
            assertEquals(true, saved.getBoolean("onboardingCompleted"))
            assertEquals("Demo Gezgin", saved.getString("name"))
            assertTrue((saved.get("personalizationProfile.selectedInterests") as List<*>).contains("meteoroloji"))
            withTimeout(10_000) { ThemeManager.getUserInterests(application, uid).first { "meteoroloji" in it } }
            assertEquals(true, application.dataStore.data.first()[booleanPreferencesKey("havamania:$uid:onboarding_completed")])
            compose.waitUntil(15_000) {
                compose.mainClock.advanceTimeByFrame()
                val roundTrip = (originalProfile.profileState.value as? ProfileState.Success)?.profile
                roundTrip?.uid == uid && roundTrip.onboardingCompleted &&
                    roundTrip.personalizationProfile?.selectedInterests?.contains("meteoroloji") == true
            }
            val interestsBefore = ThemeManager.getUserInterests(application, uid).first()
            ThemeManager.savePersonalizationBasics(application.dataStore, uid, setOf("wrong-owner-write"),
                { "another-owner" }, { false }, epoch)
            assertEquals(interestsBefore, ThemeManager.getUserInterests(application, uid).first())
            ThemeManager.savePersonalizationBasics(application.dataStore, uid, emptySet(),
                { auth.currentUser?.uid }, { true }, epoch)
            assertEquals(interestsBefore, ThemeManager.getUserInterests(application, uid).first())
            recreate(opened, Routes.WEATHER_ROOT)
            opened.onActivity { assertNull(it.nav.previousBackStackEntry); it.authModel.signOut() }
            awaitRoute(opened, Routes.AUTH_WELCOME)
        } } finally {
            scenario?.close(); AccountEntrySdkSession.factory = null
            repository.stopObserving(); epoch.invalidate()
            val cleanupOwner = owner ?: auth.currentUser?.uid
            auth.signOut()
            withContext(NonCancellable) {
                try { withTimeout(10_000) {
                    if (cleanupOwner != null) application.dataStore.edit { prefs ->
                        prefs.asMap().keys.filter { it.name.startsWith("havamania:$cleanupOwner:") }.forEach { prefs.remove(it) }
                    }
                    db.terminate().await()
                } } finally { app.delete() }
            }
        }
    }
}