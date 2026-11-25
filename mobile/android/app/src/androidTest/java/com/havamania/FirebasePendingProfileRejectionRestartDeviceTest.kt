package com.havamania

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
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
import org.junit.Test
import java.io.File
import java.util.UUID

/** Rejected pending disk mutation is rolled back, then cannot replay on the next process. */
class FirebasePendingProfileRejectionRestartDeviceTest {
    @Test fun rejectedPendingProfileCannotReplayAfterAnotherRestart() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true")
        val phase = args.getString("cityRestartPhase")
        assumeTrue(phase in listOf("prepare", "resume", "verify"))
        val id = UUID.fromString(args.getString("cityRestartId")).toString()
        val nonce = args.getString("profileOfflineNonce")!!; UUID.fromString(nonce)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "reject-profile-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
            useEmulator("127.0.0.1", 8086)
        }
        val repository = UserProfileRepository { db }
        val store = ViewModelStore()
        val epoch = PreferenceWriteEpoch()
        val metadata = File(application.noBackupFilesDir, "reject-profile-$id.meta")
        val sessionFile = File(application.noBackupFilesDir, "reject-profile-session-$id.json")
        var owner: String? = null
        var marker = false
        var keepSession = false
        var stage = "setup"
        lateinit var model: ProfileViewModel
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
        fun openModels() = instrumentation.runOnMainSync {
            val provider = ViewModelProvider(store, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                    AuthViewModel::class.java -> AuthViewModel(application, auth, db,
                        sessionStore = FileRegistrationPendingStore(sessionFile))
                    ProfileViewModel::class.java -> ProfileViewModel(application, auth, db, repository,
                        writeGate = { true }, snapshotWriter = { profile ->
                            ThemeManager.saveProfileSnapshot(application.dataStore, profile,
                                { auth.currentUser?.uid }, { false }, epoch)
                        }, storageProvider = { error("No Storage use") },
                        sessionStore = FileRegistrationPendingStore(sessionFile),
                        profileBasicsWriter = { uid, name, bio ->
                            ThemeManager.saveProfileBasics(application.dataStore, uid, name, bio,
                                { auth.currentUser?.uid }, { false }, epoch)
                        })
                    else -> error("Unexpected model")
                } as T
            })
            provider[AuthViewModel::class.java]
            model = provider[ProfileViewModel::class.java]
        }
        try { withTimeout(75_000) {
            if (phase == "prepare") {
                val user = auth.createUserWithEmailAndPassword("reject-profile-$id@example.invalid", "local-test-password").await().user!!
                owner = user.uid
                user.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Confirmed profile" }).await()
                db.collection("users").document(user.uid).set(UserProfile(uid = user.uid,
                    name = "Confirmed profile", bio = "Confirmed biography", email = user.email.orEmpty(), createdAt = 123L)).await()
                openModels()
                repository.readState.first { it is ProfileReadState.Ready && it.serverConfirmed }
                ThemeManager.getUserName(application, user.uid).first { it == "Confirmed profile" }
                stage = "persist pending write offline"
                db.disableNetwork().await()
                instrumentation.runOnMainSync { model.updateProfile("Rejected draft", "Rejected biography") }
                repository.readState.first { it is ProfileReadState.Ready && it.hasPendingWrites }
                assertTrue(db.collection("users").document(user.uid).get(Source.CACHE).await().metadata.hasPendingWrites())
                assertEquals("Confirmed profile", ThemeManager.getUserName(application, user.uid).first())
            } else {
                val previous = metadata.readLines(); owner = previous[0]
                assertNotEquals(previous[1].toInt(), android.os.Process.myPid())
                assertEquals(owner, auth.currentUser?.uid)
                db.disableNetwork().await()
                openModels()
                if (phase == "resume") {
                    stage = "restore pending disk write"
                    val pending = repository.readState.first { it is ProfileReadState.Ready && it.hasPendingWrites } as ProfileReadState.Ready
                    assertEquals("Rejected draft", pending.profile.name)
                    assertFalse(pending.serverConfirmed)
                    assertEquals("Confirmed profile", ThemeManager.getUserName(application, owner!!).first())
                    stage = "reject restored mutation using production rules"
                    access(true); marker = true
                    db.enableNetwork().await()
                    repository.readState.first { it is ProfileReadState.Failed }
                    db.waitForPendingWrites().await()
                    val rolledBack = db.collection("users").document(owner!!).get(Source.CACHE).await()
                    assertFalse(rolledBack.metadata.hasPendingWrites())
                    assertEquals("Confirmed profile", rolledBack.getString("name"))
                    assertEquals("Confirmed profile", auth.currentUser?.displayName)
                    assertEquals("Confirmed profile", ThemeManager.getUserName(application, owner!!).first())
                    assertEquals("Confirmed biography", ThemeManager.getUserBio(application, owner!!).first())
                    assertFalse(model.saveState.value is ProfileSaveState.Saved)
                    stage = "restore access without replaying rejected draft"
                    access(false); marker = false
                    instrumentation.runOnMainSync { model.refresh() }
                    repository.readState.first { it is ProfileReadState.Ready && it.serverConfirmed && !it.hasPendingWrites }
                } else {
                    stage = "rejected queue must remain empty after second restart"
                    val cached = db.collection("users").document(owner!!).get(Source.CACHE).await()
                    assertFalse(cached.metadata.hasPendingWrites())
                    assertEquals("Confirmed profile", cached.getString("name"))
                    db.enableNetwork().await()
                    repository.readState.first { it is ProfileReadState.Ready && it.serverConfirmed && !it.hasPendingWrites }
                    db.waitForPendingWrites().await()
                }
                val server = db.collection("users").document(owner!!).get(Source.SERVER).await()
                assertEquals("Confirmed profile", server.getString("name"))
                assertEquals("Confirmed biography", server.getString("bio"))
                assertEquals(123L, server.getLong("createdAt"))
                assertEquals("Confirmed profile", auth.currentUser?.displayName)
                assertEquals("Confirmed profile", ThemeManager.getUserName(application, owner!!).first())
                assertFalse(model.saveState.value is ProfileSaveState.Saved)
            }
            if (phase != "verify") {
                metadata.writeText("$owner\n${android.os.Process.myPid()}")
                keepSession = true
            }
        } } catch (failure: Throwable) {
            throw AssertionError("pending rejection phase=$phase stage=$stage read=${repository.readState.value}", failure)
        } finally {
            withContext(NonCancellable) { if (marker) runCatching { access(false) } }
            instrumentation.runOnMainSync { store.clear() }
            repository.stopObserving(); epoch.invalidate()
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally {
                    if (!keepSession) {
                        auth.signOut(); db.clearPersistence().await()
                        owner?.let { uid -> application.dataStore.edit { prefs ->
                            prefs.asMap().keys.filter { it.name.startsWith("havamania:$uid:") }.forEach { prefs.remove(it) }
                        } }
                        FileRegistrationPendingStore(sessionFile).write(null)
                        check(sessionFile.delete() || !sessionFile.exists())
                        check(metadata.delete() || !metadata.exists()); app.delete()
                    }
                }
            }
        }
    }
}
