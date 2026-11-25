package com.havamania

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.edit
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

/** Server commit -> process death -> offline disk cache -> server confirmation -> SDK/local repair. */
class FirebaseProfileRestartDeviceTest {
    @Test fun confirmedProfileRepairsSdkAfterRestartButCacheCannotAuthorizeRepair() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true")
        val phase = args.getString("cityRestartPhase")
        assumeTrue(phase == "prepare" || phase == "resume")
        val id = UUID.fromString(args.getString("cityRestartId")).toString()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "profile-restart-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
            useEmulator("127.0.0.1", 8086)
        }
        val repository = UserProfileRepository { db }
        val store = ViewModelStore()
        val epoch = PreferenceWriteEpoch()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val metadata = File(application.noBackupFilesDir, "profile-restart-$id.meta")
        var model: ProfileViewModel? = null
        var owner: String? = null
        var keepSession = false
        try { withTimeout(60_000) {
            if (phase == "prepare") {
                val user = auth.createUserWithEmailAndPassword("profile-restart-$id@example.invalid", "local-test-password").await().user!!
                owner = user.uid
                user.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Old SDK" }).await()
                db.collection("users").document(user.uid).set(UserProfile(uid = user.uid,
                    name = "Old SDK", bio = "Original biography", email = user.email.orEmpty(), createdAt = 123L)).await()
            } else {
                val previous = metadata.readLines()
                owner = previous[0]
                assertNotEquals(previous[1].toInt(), android.os.Process.myPid())
                assertEquals(owner, auth.currentUser?.uid)
                assertEquals("Old SDK", auth.currentUser?.displayName)
                assertEquals("Old local", ThemeManager.getUserName(application, owner!!).first())
                db.disableNetwork().await()
            }
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        ProfileViewModel(application, auth, db, repository,
                            writeGate = { true }, snapshotWriter = { profile ->
                                ThemeManager.saveProfileSnapshot(application.dataStore, profile,
                                    { auth.currentUser?.uid }, { false }, epoch)
                            }, storageProvider = { error("No Storage use") },
                            profileBasicsWriter = { uid, name, bio ->
                                ThemeManager.saveProfileBasics(application.dataStore, uid, name, bio,
                                    { auth.currentUser?.uid }, { false }, epoch)
                            }, beforeProfileSdkWrite = { entered.complete(Unit); release.await() }) as T
                })[ProfileViewModel::class.java]
            }
            if (phase == "prepare") {
                model!!.profileState.first { it is ProfileState.Success }
                instrumentation.runOnMainSync { model!!.updateProfile("Server new name", "Server new biography") }
                entered.await()
                val remote = db.collection("users").document(owner!!).get(Source.SERVER).await()
                assertEquals("Server new name", remote.getString("name"))
                assertEquals(123L, remote.getLong("createdAt"))
                assertEquals(ProfileSaveState.Saving, model!!.saveState.value)
                assertEquals("Old SDK", auth.currentUser?.displayName)
                instrumentation.runOnMainSync { store.clear() }
                repository.stopObserving(); epoch.invalidate()
                ThemeManager.saveProfileBasics(application.dataStore, owner!!, "Old local", "Old local bio",
                    { auth.currentUser?.uid }, { false }, PreferenceWriteEpoch())
                metadata.writeText("$owner\n${android.os.Process.myPid()}")
                keepSession = true
            } else {
                val cached = repository.readState.first { it is ProfileReadState.Ready } as ProfileReadState.Ready
                assertFalse(cached.serverConfirmed)
                assertEquals("Server new name", cached.profile.name)
                assertEquals("Old SDK", auth.currentUser?.displayName)
                assertFalse(model!!.profileReconciliationBusy.value)
                db.enableNetwork().await()
                repository.readState.first { it is ProfileReadState.Ready && it.serverConfirmed }
                withContext(Dispatchers.Main) {
                    while (auth.currentUser?.displayName != "Server new name" || model!!.profileReconciliationBusy.value) delay(10)
                }
                ThemeManager.getUserName(application, owner!!).first { it == "Server new name" }
                assertEquals("Server new biography", ThemeManager.getUserBio(application, owner!!).first())
                assertFalse(model!!.saveState.value is ProfileSaveState.Saved)
                val remote = db.collection("users").document(owner!!).get(Source.SERVER).await()
                assertEquals("Server new name", remote.getString("name"))
                assertEquals(123L, remote.getLong("createdAt"))
                // A newer server value wins; recovery never replays the interrupted draft to Firestore.
                db.collection("users").document(owner!!).update("name", "Newer server name").await()
                withContext(Dispatchers.Main) {
                    while (auth.currentUser?.displayName != "Newer server name") delay(10)
                }
                assertEquals("Newer server name", db.collection("users").document(owner!!).get(Source.SERVER).await().getString("name"))
            }
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            repository.stopObserving(); epoch.invalidate()
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally {
                    if (!keepSession) {
                        auth.signOut()
                        db.clearPersistence().await()
                        owner?.let { uid -> application.dataStore.edit { prefs ->
                            prefs.asMap().keys.filter { it.name.startsWith("havamania:$uid:") }.forEach { prefs.remove(it) }
                        } }
                        app.delete(); check(metadata.delete() || !metadata.exists())
                    }
                }
            }
        }
    }
}
