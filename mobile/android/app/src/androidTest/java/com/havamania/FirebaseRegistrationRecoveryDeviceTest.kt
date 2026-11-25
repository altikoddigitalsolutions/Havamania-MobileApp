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
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/** Two instrumentation processes. Only named demo SDKs and UUID-owned test files. */
class FirebaseRegistrationRecoveryDeviceTest {
    @Test fun processRestartResumesBoundRegistration() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseEmulatorContract") == "true")
        val phase = args.getString("cityRestartPhase")
        assumeTrue(phase == "prepare" || phase == "resume")
        val id = UUID.fromString(args.getString("cityRestartId")).toString()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val options = FirebaseOptions.Builder().setProjectId("demo-havamania-audit")
            .setApplicationId("1:1234567890:android:0000000000000000").setApiKey("fake-emulator-api-key").build()
        val app = FirebaseApp.initializeApp(application, options, "registration-restart-$id")
        assertEquals("demo-havamania-audit", app.options.projectId)
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val file = File(application.noBackupFilesDir, "registration-restart-$id.json")
        val metadata = File(application.noBackupFilesDir, "registration-restart-$id.meta")
        val sessionFile = File(application.noBackupFilesDir, "registration-session-$id.json")
        val store = ViewModelStore()
        val reached = CompletableDeferred<String>()
        val release = CompletableDeferred<Unit>()
        var model: AuthViewModel? = null
        var keepSession = false
        try { withTimeout(45_000) {
            if (phase == "prepare") assertFalse(file.exists())
            val old = if (phase == "resume") metadata.readLines() else emptyList()
            if (phase == "resume") {
                assertNotEquals(old[1].toInt(), android.os.Process.myPid())
                assertEquals(old[0], auth.currentUser?.uid)
                assertEquals(old[0], PendingRegistration.decode(checkNotNull(FileRegistrationPendingStore(file).read())).uid)
            }
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(application, auth, db, registrationStore = FileRegistrationPendingStore(file),
                            sessionStore = FileRegistrationPendingStore(sessionFile),
                            beforeRegistrationProfileWrite = { uid -> reached.complete(uid); release.await() }) as T
                })[AuthViewModel::class.java]
                if (phase == "prepare") model!!.signUp("Durable Demo", "restart-$id@example.invalid", "local-test-password")
                assertTrue(model!!.authState.value is AuthState.Loading)
                assertNull(model!!.currentUser)
            }
            val uid = reached.await()
            assertTrue(AuthSessionFence(FileRegistrationPendingStore(sessionFile)).unresolved)
            assertEquals(uid, PendingRegistration.decode(checkNotNull(FileRegistrationPendingStore(file).read())).uid)
            if (phase == "prepare") {
                assertFalse(db.collection("users").document(uid).get(Source.SERVER).await().exists())
                metadata.writeText("$uid\n${android.os.Process.myPid()}")
                keepSession = true // The runner force-stops QA and reopens this named SDK session.
            } else {
                assertEquals(old[0], uid)
                release.complete(Unit)
                val ready = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
                assertEquals(uid, ready.user.uid)
                val saved = db.collection("users").document(uid).get(Source.SERVER).await()
                assertEquals("Durable Demo", saved.getString("name"))
                assertEquals("restart-$id@example.invalid", saved.getString("email"))
                assertNotNull(saved.getLong("createdAt"))
                assertNull(FileRegistrationPendingStore(file).read())
                assertTrue(AuthSessionFence(FileRegistrationPendingStore(sessionFile)).canRestore(uid))
                instrumentation.runOnMainSync { model!!.signOut(); assertNull(model!!.currentUser) }
                assertTrue(AuthSessionFence(FileRegistrationPendingStore(sessionFile)).signedOut)
            }
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally {
                    if (!keepSession) {
                        auth.signOut(); app.delete()
                        FileRegistrationPendingStore(file).write(null)
                        check(file.delete() || !file.exists())
                        check(metadata.delete() || !metadata.exists())
                        FileRegistrationPendingStore(sessionFile).write(null)
                        check(sessionFile.delete() || !sessionFile.exists())
                    }
                }
            }
        }
    }
    @Test fun mismatchedOwnerAndUnboundRecordRequireExplicitCredentials() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder().setProjectId("demo-havamania-audit")
            .setApplicationId("1:1234567890:android:0000000000000000").setApiKey("fake-emulator-api-key").build(), "recovery-owner-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val file = File(application.noBackupFilesDir, "registration-owner-$id.json")
        var store = ViewModelStore()
        var model: AuthViewModel? = null
        fun construct(recovering: Boolean = false) {
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(application, auth, db, registrationStore = FileRegistrationPendingStore(file)) as T
                })[AuthViewModel::class.java]
                assertTrue(if (recovering) model!!.authState.value is AuthState.Loading else model!!.authState.value is AuthState.Error)
                assertNull(model!!.currentUser)
            }
        }
        try { withTimeout(60_000) {
            val aEmail = "a-$id@example.invalid"; val bEmail = "b-$id@example.invalid"
            val a = auth.createUserWithEmailAndPassword(aEmail, "local-test-password").await().user!!
            val b = auth.createUserWithEmailAndPassword(bEmail, "local-test-password").await().user!!
            val bRef = db.collection("users").document(b.uid)
            bRef.set(mapOf("uid" to b.uid, "email" to bEmail, "name" to "Preserve B", "bio" to "Keep B", "createdAt" to 123L)).await()
            FileRegistrationPendingStore(file).write(PendingRegistration(name = "Never apply A", email = aEmail, uid = a.uid).encode())
            construct()
            b.getIdToken(false).await()
            instrumentation.runOnMainSync { assertNull(model!!.currentUser); model!!.signIn(bEmail, "local-test-password") }
            val readyB = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(b.uid, readyB.user.uid)
            assertEquals("Preserve B", bRef.get(Source.SERVER).await().getString("name"))
            try {
                db.collection("users").document(a.uid).get(Source.SERVER).await()
                fail("Another owner must not read A")
            } catch (denied: com.google.firebase.firestore.FirebaseFirestoreException) {
                assertEquals(com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED, denied.code)
            }
            assertNull(FileRegistrationPendingStore(file).read())
            instrumentation.runOnMainSync { store.clear() }; store = ViewModelStore()
            FileRegistrationPendingStore(file).write(PendingRegistration(name = "Stale bound draft", email = bEmail, uid = b.uid).encode())
            construct(recovering = true)
            val recovered = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(b.uid, recovered.user.uid)
            assertEquals("Preserve B", bRef.get(Source.SERVER).await().getString("name"))
            assertEquals(123L, bRef.get(Source.SERVER).await().getLong("createdAt"))
            assertNull(FileRegistrationPendingStore(file).read())
            instrumentation.runOnMainSync { store.clear() }; store = ViewModelStore()
            FileRegistrationPendingStore(file).write(PendingRegistration(name = "Do not apply an unbound draft", email = bEmail).encode())
            construct() // UID-less journal must not auto-complete even with a matching cached session.
            instrumentation.runOnMainSync { model!!.signIn(bEmail, "local-test-password") }
            val readyUnbound = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(b.uid, readyUnbound.user.uid)
            val saved = bRef.get(Source.SERVER).await()
            assertEquals("Preserve B", saved.getString("name")); assertEquals("Keep B", saved.getString("bio"))
            assertEquals(123L, saved.getLong("createdAt")); assertNull(FileRegistrationPendingStore(file).read())
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            auth.signOut()
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally {
                    app.delete(); FileRegistrationPendingStore(file).write(null)
                    check(file.delete() || !file.exists())
                }
            }
        }
    }
}