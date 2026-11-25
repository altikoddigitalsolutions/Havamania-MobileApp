package com.havamania

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.havamania.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class FirebaseAuthSessionValidationDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun disabledServerAccountIsSignedOutOnForegroundAndRequiresExplicitLogin() = invalidatedServerSession(false)
    @Test fun revokedRefreshTokenIsSignedOutOnForegroundAndRequiresExplicitLogin() = invalidatedServerSession(true)
    private fun invalidatedServerSession(revoked: Boolean) = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true")
        val nonce = args.getString("profileOfflineNonce")!!; UUID.fromString(nonce)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(application, options(), "disabled-auth-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = database(app)
        val repository = UserProfileRepository { db }
        val epoch = PreferenceWriteEpoch()
        val sessionFile = java.io.File(application.noBackupFilesDir, "disabled-session-$id.json")
        val email = "disabled-$id@example.invalid"
        val password = "local-test-password"
        var owner: String? = null
        var disabled = false
        var scenario: ActivityScenario<ProfileEditSdkActivity>? = null
        var stage = "setup"
        val reloadCalls = java.util.concurrent.atomic.AtomicInteger()
        val initialReload = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        val initialReloadFinished = CompletableDeferred<Unit>()
        lateinit var account: AuthViewModel
        lateinit var profile: ProfileViewModel
        fun awaitUi(predicate: () -> Boolean) = compose.waitUntil(20_000) {
            compose.mainClock.advanceTimeByFrame(); predicate()
        }
        suspend fun admin(path: String, issuedAt: Long? = null): org.json.JSONObject = withContext(Dispatchers.IO) {
            val connection = java.net.URL("http://127.0.0.1:9397/$path").openConnection() as java.net.HttpURLConnection
            try {
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.connectTimeout = 3000; connection.readTimeout = 3000
                connection.setRequestProperty("X-Emulator-Test", nonce)
                val request = org.json.JSONObject().put("uid", owner)
                issuedAt?.let { request.put("issuedAt", it) }
                connection.outputStream.use { it.write(request.toString().toByteArray()) }
                assertEquals(200, connection.responseCode)
                connection.inputStream.bufferedReader().use { org.json.JSONObject(it.readText()) }
            } finally { connection.disconnect() }
        }
        try { withTimeout(90_000) {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user!!; owner = user.uid
            user.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Old SDK" }).await()
            db.collection("users").document(user.uid).set(UserProfile(uid = user.uid, name = "Confirmed owner", bio = "Confirmed biography", email = email)).await()
            ProfileEditSdkSession.factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                    AuthViewModel::class.java -> AuthViewModel(application, auth, db,
                        sessionStore = FileRegistrationPendingStore(sessionFile),
                        sessionReloadRequest = { value ->
                            if (reloadCalls.incrementAndGet() == 1) {
                                value.reload().addOnCompleteListener { result ->
                                    if (result.isSuccessful) initialReloadFinished.complete(Unit)
                                    else initialReload.setException(result.exception ?: java.io.IOException("Initial reload failed"))
                                }
                                initialReload.task
                            } else value.reload()
                        }).also { it.refreshSession() }
                    ProfileViewModel::class.java -> ProfileViewModel(application, auth, db, repository,
                        writeGate = { true }, snapshotWriter = { value ->
                            ThemeManager.saveProfileSnapshot(application.dataStore, value,
                                { auth.currentUser?.uid }, { false }, epoch)
                        }, storageProvider = { error("No Storage access") },
                        sessionStore = FileRegistrationPendingStore(sessionFile), profileBasicsWriter = { uid, name, bio ->
                            ThemeManager.saveProfileBasics(application.dataStore, uid, name, bio,
                                { auth.currentUser?.uid }, { false }, epoch)
                        })
                    else -> error("Unexpected model")
                } as T
            }
            ProfileEditSdkSession.validateOnForeground = true
            val opened = ActivityScenario.launch(ProfileEditSdkActivity::class.java); scenario = opened
            opened.onActivity { account = it.authModel; profile = it.profileModel }
            stage = "healthy initial validation"
            awaitUi { initialReloadFinished.isCompleted &&
                (repository.readState.value as? ProfileReadState.Ready)?.serverConfirmed == true }
            assertEquals("Old SDK", auth.currentUser?.displayName)
            opened.onActivity { initialReload.setResult(null) }
            awaitUi { reloadCalls.get() > 0 && !account.validatingSession && account.currentUser?.uid == user.uid &&
                auth.currentUser?.displayName == "Confirmed owner" && !profile.profileReconciliationBusy.value }
            ThemeManager.getUserName(application, user.uid).first { it == "Confirmed owner" }
            db.disableNetwork().await()
            opened.onActivity { profile.updateProfile("Pending disabled", "Pending biography") }
            repository.readState.first { it is ProfileReadState.Ready && it.hasPendingWrites }
            opened.moveToState(Lifecycle.State.CREATED)
            stage = "disable on Auth server"
            if (revoked) {
                val issued = user.getIdToken(false).await().issuedAtTimestamp
                val validSince = admin("auth-revoke", issued).getString("validSince").toLong()
                assertTrue("validSince=$validSince issued=$issued", validSince > issued)
            } else { admin("auth-disable"); disabled = true }
            assertEquals(user.uid, auth.currentUser?.uid) // No server push; still cached before foreground check.
            stage = "offline foreground does not force network or logout"
            instrumentation.runOnMainSync { ProfileEditSdkSession.validationNetworkAllowed = false }
            val previousReloads = reloadCalls.get()
            opened.moveToState(Lifecycle.State.RESUMED)
            compose.waitForIdle()
            assertEquals(previousReloads, reloadCalls.get())
            assertEquals(user.uid, account.currentUser?.uid)
            opened.moveToState(Lifecycle.State.CREATED)
            instrumentation.runOnMainSync { ProfileEditSdkSession.validationNetworkAllowed = true }
            stage = "foreground validates cached owner"
            opened.moveToState(Lifecycle.State.RESUMED)
            awaitUi { !account.validatingSession && account.authState.value is AuthState.Error && account.currentUser == null }
            assertNull(auth.currentUser)
            assertTrue(reloadCalls.get() >= 2)
            assertTrue(AuthSessionFence(FileRegistrationPendingStore(sessionFile)).signedOut)
            assertFalse(profile.saveState.value is ProfileSaveState.Saved)
            assertEquals("Confirmed owner", ThemeManager.getUserName(application, user.uid).first())
            compose.onNodeWithText("SDK oturumu kapalı").assertIsDisplayed()
            db.enableNetwork().await()
            assertEquals("Confirmed owner", admin("profile").getString("name"))
            if (!revoked) {
            stage = "disabled credentials cannot restore session"
            opened.onActivity { account.signIn(email, password) }
            awaitUi { !account.validatingSession && account.authState.value is AuthState.Error }
            assertNull(account.currentUser)
            assertEquals("Bu kullanıcı hesabı devre dışı bırakılmış.", (account.authState.value as AuthState.Error).message)
            stage = "enable and explicitly authenticate"
            admin("auth-enable"); disabled = false
            }
            opened.onActivity { account.signIn(email, password) }
            awaitUi { account.authState.value is AuthState.Authenticated }
            awaitUi { (repository.readState.value as? ProfileReadState.Ready)?.let { it.serverConfirmed && it.profile.name == "Pending disabled" } == true }
            awaitUi { auth.currentUser?.displayName == "Pending disabled" && !profile.profileReconciliationBusy.value }
            ThemeManager.getUserName(application, user.uid).first { it == "Pending disabled" }
            assertFalse(profile.saveState.value is ProfileSaveState.Saved)
            opened.onActivity { assertEquals(0, it.backRequests); account.signOut() }
        } } catch (failure: Throwable) {
            throw AssertionError("auth disable stage=$stage auth=${runCatching { account.authState.value }.getOrNull()} read=${repository.readState.value}", failure)
        } finally {
            withContext(NonCancellable) { if (disabled) runCatching { admin("auth-enable") } }
            scenario?.close(); ProfileEditSdkSession.factory = null; ProfileEditSdkSession.validateOnForeground = false
            ProfileEditSdkSession.validationNetworkAllowed = true
            repository.stopObserving(); epoch.invalidate(); auth.signOut()
            withContext(NonCancellable) {
                try { withTimeout(10_000) {
                    db.enableNetwork().await(); db.terminate().await()
                    owner?.let { uid -> application.dataStore.edit { prefs ->
                        prefs.asMap().keys.filter { it.name.startsWith("havamania:$uid:") }.forEach { prefs.remove(it) }
                    } }
                    FileRegistrationPendingStore(sessionFile).write(null); check(sessionFile.delete() || !sessionFile.exists())
                } } finally { app.delete() }
            }
        }
    }

    @Test fun transientValidationFailureKeepsCachedOwner() = injectedValidation(false)
    @Test fun staleValidationFailureCannotInvalidateNextOwner() = injectedValidation(true)
    private fun injectedValidation(stale: Boolean) = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, options(), "injected-validation-${UUID.randomUUID()}")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = database(app)
        val store = ViewModelStore()
        val journal = MemoryRegistrationPendingStore()
        val pending = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        val password = "local-test-password"
        val emailB = "validate-b-${UUID.randomUUID()}@example.invalid"
        var loginCalls = 0
        lateinit var model: AuthViewModel
        try { withTimeout(40_000) {
            val uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user!!.uid
            val uidA = auth.createUserWithEmailAndPassword("validate-a-${UUID.randomUUID()}@example.invalid", password).await().user!!.uid
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(application, auth, db, sessionStore = journal,
                            signInRequest = { email, secret -> loginCalls++; auth.signInWithEmailAndPassword(email, secret) },
                            sessionReloadRequest = { if (stale) pending.task else com.google.android.gms.tasks.Tasks.forException<Void>(com.google.firebase.FirebaseNetworkException("local-injected-network-failure")) }) as T
                })[AuthViewModel::class.java]
                model.refreshSession()
                if (stale) {
                    model.signOut(); model.signIn(emailB, password)
                    assertEquals(0, loginCalls)
                    pending.setException(com.google.firebase.auth.FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "local-injected-stale-error"))
                }
            }
            instrumentation.waitForIdleSync()
            if (stale) {
                instrumentation.runOnMainSync { model.signIn(emailB, password) }
                val ready = model.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
                assertEquals(uidB, ready.user.uid)
                instrumentation.runOnMainSync { assertEquals(1, loginCalls); assertEquals(uidB, model.currentUser?.uid) }
            } else {
                withContext(Dispatchers.Main) { while (model.validatingSession) delay(10) }
                assertEquals(uidA, model.currentUser?.uid)
                assertTrue(model.authState.value is AuthState.Authenticated)
                assertFalse(AuthSessionFence(journal).signedOut)
            }
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            withContext(NonCancellable) { try { db.terminate().await() } finally { auth.signOut(); app.delete() } }
        }
    }
    private fun options() = FirebaseOptions.Builder().setProjectId("demo-havamania-audit")
        .setApplicationId("1:1234567890:android:0000000000000000").setApiKey("fake-emulator-api-key").build()
    private fun database(app: FirebaseApp) = FirebaseFirestore.getInstance(app).apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        useEmulator("127.0.0.1", 8086)
    }
}
