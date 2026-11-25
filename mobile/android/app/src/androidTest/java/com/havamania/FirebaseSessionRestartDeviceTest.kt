package com.havamania

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.Test
import java.io.File
import java.util.UUID

/** Four independent QA processes; no default SDK or production account access. */
class FirebaseSessionRestartDeviceTest {
    @Test fun durableSessionStateSurvivesProcessRestart() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true")
        val phase = args.getString("cityRestartPhase")
        assumeTrue(phase in listOf("prepare", "accepted", "pending", "signedout"))
        val id = UUID.fromString(args.getString("cityRestartId")).toString()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "session-restart-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val journal = File(application.noBackupFilesDir, "session-restart-$id.json")
        val metadata = File(application.noBackupFilesDir, "session-restart-$id.meta")
        val store = ViewModelStore()
        val emailA = "session-a-$id@example.invalid"
        val emailB = "session-b-$id@example.invalid"
        val password = "local-test-password"
        var model: AuthViewModel? = null
        var keepSession = false
        fun openModel() = instrumentation.runOnMainSync {
            model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(application, auth, db, sessionStore = FileRegistrationPendingStore(journal)) as T
            })[AuthViewModel::class.java]
        }
        try { withTimeout(60_000) {
            val old = if (phase == "prepare") emptyList() else metadata.readLines()
            if (phase != "prepare") {
                assertNotEquals("A fresh OS process is required", old[2].toInt(), android.os.Process.myPid())
                assertEquals("SDK must really restore the cached test owner", old[0], auth.currentUser?.uid)
            }
            val uidA: String
            val uidB: String
            when (phase) {
                "prepare" -> {
                    assertFalse(journal.exists()); assertFalse(metadata.exists())
                    uidA = auth.createUserWithEmailAndPassword(emailA, password).await().user!!.uid
                    uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user!!.uid
                    auth.signOut()
                    openModel()
                    instrumentation.runOnMainSync { model!!.signIn(emailA, password) }
                    model!!.authState.first { it is AuthState.Authenticated }
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).canRestore(uidA))
                }
                "accepted" -> {
                    uidA = old[0]; uidB = old[1]
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).canRestore(uidA))
                    assertFalse(AuthSessionFence(FileRegistrationPendingStore(journal)).canRestore(uidB))
                    openModel()
                    instrumentation.runOnMainSync {
                        assertEquals(uidA, model!!.currentUser?.uid)
                        store.clear()
                        // Crash boundary: intent persisted before the new SDK request is dispatched.
                        AuthSessionFences.forAuth(auth, FileRegistrationPendingStore(journal)).begin()
                    }
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).unresolved)
                    assertEquals(uidA, auth.currentUser?.uid)
                }
                "pending" -> {
                    uidA = old[0]; uidB = old[1]
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).unresolved)
                    assertFalse(AuthSessionFence(FileRegistrationPendingStore(journal)).canRestore(uidA))
                    openModel()
                    withContext(Dispatchers.Main) {
                        while (auth.currentUser != null) delay(10)
                        assertNull(model!!.currentUser)
                        assertFalse(model!!.authState.value is AuthState.Authenticated)
                        model!!.signIn(emailB, password)
                    }
                    val ready = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
                    assertEquals(uidB, ready.user.uid)
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).canRestore(uidB))
                    instrumentation.runOnMainSync { model!!.signOut(); store.clear() }
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).signedOut)
                    // Fixture seeds a late SDK cache write after the listener was destroyed.
                    auth.signInWithEmailAndPassword(emailA, password).await()
                    assertEquals(uidA, auth.currentUser?.uid)
                }
                else -> {
                    uidA = old[0]; uidB = old[1]
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).signedOut)
                    openModel()
                    withContext(Dispatchers.Main) {
                        while (auth.currentUser != null) delay(10)
                        assertNull(model!!.currentUser)
                        assertFalse(model!!.authState.value is AuthState.Authenticated)
                        model!!.signIn(emailB, password)
                    }
                    val ready = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
                    assertEquals(uidB, ready.user.uid)
                    instrumentation.runOnMainSync { model!!.signOut() }
                    assertTrue(AuthSessionFence(FileRegistrationPendingStore(journal)).signedOut)
                }
            }
            println("Session restart phase=$phase pid=${android.os.Process.myPid()} previousPid=${old.getOrNull(2)}")
            if (phase != "signedout") {
                metadata.writeText("$uidA\n$uidB\n${android.os.Process.myPid()}")
                keepSession = true
            }
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally {
                    if (!keepSession) {
                        auth.signOut(); app.delete()
                        FileRegistrationPendingStore(journal).write(null)
                        check(journal.delete() || !journal.exists())
                        check(metadata.delete() || !metadata.exists())
                    }
                }
            }
        }
    }
}
