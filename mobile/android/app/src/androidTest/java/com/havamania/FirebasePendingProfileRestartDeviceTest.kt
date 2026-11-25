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

/** Pending disk mutation stays owned by A across two process deaths and B's session. */
class FirebasePendingProfileRestartDeviceTest {
    @Test fun pendingProfileQueueSurvivesRestartAndCannotWriteAsAnotherOwner() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true")
        val phase = args.getString("cityRestartPhase")
        assumeTrue(phase in listOf("prepare", "resume", "switchback"))
        val id = UUID.fromString(args.getString("cityRestartId")).toString()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "pending-profile-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
            useEmulator("127.0.0.1", 8086)
        }
        val repository = UserProfileRepository { db }
        val store = ViewModelStore()
        val epoch = PreferenceWriteEpoch()
        val metadata = File(application.noBackupFilesDir, "pending-profile-$id.meta")
        val sessionFile = File(application.noBackupFilesDir, "pending-profile-session-$id.json")
        val emailA = "pending-a-$id@example.invalid"
        val emailB = "pending-b-$id@example.invalid"
        val password = "local-test-password"
        var uidA: String? = null
        var uidB: String? = null
        var keepSession = false
        var stage = "setup"
        lateinit var model: ProfileViewModel
        lateinit var account: AuthViewModel
        suspend fun ready(uid: String, pending: Boolean? = null, confirmed: Boolean = false): ProfileReadState.Ready =
            repository.readState.first {
                it is ProfileReadState.Ready && it.profile.uid == uid &&
                    (pending == null || it.hasPendingWrites == pending) && (!confirmed || it.serverConfirmed)
            } as ProfileReadState.Ready
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
            account = provider[AuthViewModel::class.java]
            model = provider[ProfileViewModel::class.java]
        }
        try { withTimeout(75_000) {
            if (phase == "prepare") {
                stage = "seed B"
                val b = auth.createUserWithEmailAndPassword(emailB, password).await().user!!; uidB = b.uid
                b.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Confirmed B" }).await()
                db.collection("users").document(b.uid).set(UserProfile(uid = b.uid, name = "Confirmed B", bio = "B biography", email = emailB, createdAt = 456L)).await()
                db.collection("users").document(b.uid).get(Source.SERVER).await()
                stage = "seed A"
                val a = auth.createUserWithEmailAndPassword(emailA, password).await().user!!; uidA = a.uid
                a.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Confirmed A" }).await()
                db.collection("users").document(a.uid).set(UserProfile(uid = a.uid, name = "Confirmed A", bio = "A biography", email = emailA, createdAt = 123L)).await()
                openModels()
                ready(a.uid, confirmed = true)
                ThemeManager.getUserName(application, a.uid).first { it == "Confirmed A" }
                stage = "enqueue A offline"
                db.disableNetwork().await()
                instrumentation.runOnMainSync { model.updateProfile("Pending A", "Pending biography") }
                val pending = ready(a.uid, pending = true)
                assertFalse(pending.serverConfirmed)
                assertEquals("Pending A", pending.profile.name)
                assertEquals(ProfileSaveState.Saving, model.saveState.value)
                val cached = db.collection("users").document(a.uid).get(Source.CACHE).await()
                assertTrue(cached.metadata.hasPendingWrites())
                assertEquals("Confirmed A", auth.currentUser?.displayName)
                assertEquals("Confirmed A", ThemeManager.getUserName(application, a.uid).first())
            } else {
                val old = metadata.readLines(); uidA = old[0]; uidB = old[1]
                assertNotEquals(old[2].toInt(), android.os.Process.myPid())
                assertEquals(if (phase == "resume") uidA else uidB, auth.currentUser?.uid)
                db.disableNetwork().await()
                openModels()
                if (phase == "resume") {
                    stage = "restore A pending disk queue"
                    val pending = ready(uidA!!, pending = true)
                    assertFalse(pending.serverConfirmed)
                    assertEquals("Pending A", pending.profile.name)
                    assertEquals("Confirmed A", auth.currentUser?.displayName)
                    assertEquals("Confirmed A", ThemeManager.getUserName(application, uidA!!).first())
                    assertFalse(model.saveState.value is ProfileSaveState.Saved)
                    stage = "switch to B offline"
                    instrumentation.runOnMainSync { account.signOut(); account.signIn(emailB, password) }
                    account.authState.first { it is AuthState.Authenticated }
                    val b = ready(uidB!!, pending = false)
                    assertEquals("Confirmed B", b.profile.name)
                    stage = "connect only as B"
                    db.enableNetwork().await()
                    ready(uidB!!, pending = false, confirmed = true)
                    val remoteB = db.collection("users").document(uidB!!).get(Source.SERVER).await()
                    assertEquals("Confirmed B", remoteB.getString("name"))
                    assertEquals("B biography", remoteB.getString("bio"))
                    assertEquals("Confirmed B", auth.currentUser?.displayName)
                    ThemeManager.getUserName(application, uidB!!).first { it == "Confirmed B" }
                    assertEquals("Confirmed A", ThemeManager.getUserName(application, uidA!!).first())
                    assertFalse(model.saveState.value is ProfileSaveState.Saved)
                } else {
                    stage = "return to A offline after second restart"
                    instrumentation.runOnMainSync { account.signOut(); account.signIn(emailA, password) }
                    account.authState.first { it is AuthState.Authenticated }
                    val pending = ready(uidA!!, pending = true)
                    assertEquals("Pending A", pending.profile.name)
                    assertEquals("Confirmed A", auth.currentUser?.displayName)
                    assertEquals("Confirmed A", ThemeManager.getUserName(application, uidA!!).first())
                    stage = "commit pending A as A"
                    db.enableNetwork().await()
                    ready(uidA!!, pending = false, confirmed = true)
                    withContext(Dispatchers.Main) {
                        while (auth.currentUser?.displayName != "Pending A" || model.profileReconciliationBusy.value) delay(10)
                    }
                    ThemeManager.getUserName(application, uidA!!).first { it == "Pending A" }
                    assertEquals("Pending biography", ThemeManager.getUserBio(application, uidA!!).first())
                    assertFalse(model.saveState.value is ProfileSaveState.Saved)
                    val remoteA = db.collection("users").document(uidA!!).get(Source.SERVER).await()
                    assertEquals("Pending A", remoteA.getString("name")); assertEquals(123L, remoteA.getLong("createdAt"))
                    stage = "verify B still unchanged"
                    instrumentation.runOnMainSync { account.signOut(); account.signIn(emailB, password) }
                    account.authState.first { it is AuthState.Authenticated }
                    ready(uidB!!, confirmed = true)
                    val b = db.collection("users").document(uidB!!).get(Source.SERVER).await()
                    assertEquals("Confirmed B", b.getString("name")); assertEquals("B biography", b.getString("bio"))
                    assertEquals(456L, b.getLong("createdAt"))
                    assertEquals("Confirmed B", auth.currentUser?.displayName)
                    assertEquals("Confirmed B", ThemeManager.getUserName(application, uidB!!).first())
                }
            }
            if (phase != "switchback") {
                metadata.writeText("$uidA\n$uidB\n${android.os.Process.myPid()}")
                keepSession = true
            }
        } } catch (failure: Throwable) {
            throw AssertionError("pending restart phase=$phase stage=$stage read=${repository.readState.value}", failure)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            repository.stopObserving(); epoch.invalidate()
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally {
                    if (!keepSession) {
                        auth.signOut(); db.clearPersistence().await()
                        application.dataStore.edit { prefs ->
                            prefs.asMap().keys.filter { key -> listOfNotNull(uidA, uidB).any { key.name.startsWith("havamania:$it:") } }.forEach { prefs.remove(it) }
                        }
                        FileRegistrationPendingStore(sessionFile).write(null)
                        check(sessionFile.delete() || !sessionFile.exists())
                        check(metadata.delete() || !metadata.exists()); app.delete()
                    }
                }
            }
        }
    }
}
