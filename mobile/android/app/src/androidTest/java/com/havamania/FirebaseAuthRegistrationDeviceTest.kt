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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/** Explicit opt-in, named demo app and loopback endpoints only. No default SDK/account access. */
class FirebaseAuthRegistrationDeviceTest {
    private data class PendingWrite(val uid: String, val reply: CompletableDeferred<Unit>)
    @Test fun productionViewModelWaitsRetriesOneAccountAndPreservesProfileFields() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "registration-${UUID.randomUUID()}")
        assertEquals("demo-havamania-audit", app.options.projectId)
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val writes = Channel<PendingWrite>(Channel.UNLIMITED)
        val event = CompletableDeferred<String>()
        val listener = FirebaseAuth.AuthStateListener { a -> a.currentUser?.uid?.let { event.complete(it) } }
        val store = ViewModelStore()
        var model: AuthViewModel? = null
        val email = "registration-${UUID.randomUUID()}@example.invalid"
        try { withTimeout(60_000) {
            auth.addAuthStateListener(listener)
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(application, auth, db) { uid ->
                            val request = PendingWrite(uid, CompletableDeferred())
                            writes.send(request); request.reply.await()
                        } as T
                })[AuthViewModel::class.java]
                model!!.signUp(" Demo Gezgin ", " $email ", "local-test-password")
            }
            val first = writes.receive()
            assertEquals(first.uid, event.await())
            instrumentation.runOnMainSync {
                assertTrue(model!!.authState.value is AuthState.Loading)
                assertNull(model!!.currentUser)
                model!!.signUp("Duplicate", email, "local-test-password")
            }
            assertTrue(writes.tryReceive().isFailure)
            val ref = db.collection("users").document(first.uid)
            ref.set(mapOf("uid" to first.uid, "name" to "Earlier profile", "email" to email,
                "createdAt" to 123L, "bio" to "Keep biography", "interests" to listOf("culture"),
                "onboardingCompleted" to true)).await()
            first.reply.completeExceptionally(java.io.IOException("local-injected-profile-failure"))
            model!!.authState.first { it is AuthState.Error }
            instrumentation.runOnMainSync {
                assertNull(model!!.currentUser)
                assertEquals(first.uid, auth.currentUser?.uid)
                model!!.signUp(" Demo Gezgin ", email, "local-test-password")
            }
            val retry = writes.receive()
            assertEquals(first.uid, retry.uid)
            instrumentation.runOnMainSync {
                assertTrue(model!!.authState.value is AuthState.Loading); assertNull(model!!.currentUser)
            }
            retry.reply.complete(Unit)
            val ready = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(first.uid, ready.user.uid)
            instrumentation.runOnMainSync { assertEquals(first.uid, model!!.currentUser?.uid) }
            val saved = ref.get(Source.SERVER).await()
            assertEquals("Demo Gezgin", saved.getString("name")); assertEquals(email, saved.getString("email"))
            assertEquals(123L, saved.getLong("createdAt")); assertEquals("Keep biography", saved.getString("bio"))
            assertEquals(listOf("culture"), saved.get("interests"))
            assertEquals(true, saved.getBoolean("onboardingCompleted")); assertNotNull(saved.getLong("updatedAt"))
            assertEquals("Demo Gezgin", auth.currentUser?.displayName)
            instrumentation.runOnMainSync {
                model!!.signOut(); assertNull(model!!.currentUser); assertTrue(model!!.authState.value is AuthState.Idle)
            }
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            auth.removeAuthStateListener(listener); writes.close()
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally { auth.signOut(); app.delete() }
            }
        }
    }
    @Test fun lateSignInAfterLogoutCannotRestoreOrOverwriteNextOwner() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "late-login-${UUID.randomUUID()}")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val store = ViewModelStore()
        val delayed = com.google.android.gms.tasks.TaskCompletionSource<com.google.firebase.auth.AuthResult>()
        val emailA = "late-a-${UUID.randomUUID()}@example.invalid"
        val emailB = "late-b-${UUID.randomUUID()}@example.invalid"
        val password = "local-test-password"
        var calls = 0
        var model: AuthViewModel? = null
        try { withTimeout(60_000) {
            auth.createUserWithEmailAndPassword(emailA, password).await()
            val uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user!!.uid
            auth.signOut()
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(application, auth, db, signInRequest = { email, secret ->
                            calls++
                            if (calls == 1) delayed.task else auth.signInWithEmailAndPassword(email, secret)
                        }) as T
                })[AuthViewModel::class.java]
                model!!.signIn(emailA, password)
                model!!.signOut()
                model!!.signIn(emailB, password)
                assertEquals(1, calls)
                assertNull(model!!.currentUser)
            }
            // The real SDK completes after logout; cancelling Task.await cannot cancel this mutation.
            val late = auth.signInWithEmailAndPassword(emailA, password).await()
            instrumentation.runOnMainSync { delayed.setResult(late) }
            withContext(Dispatchers.Main) {
                while (auth.currentUser != null) { yield(); delay(10) }
                assertNull(model!!.currentUser)
                model!!.signIn(emailB, password)
            }
            val ready = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(uidB, ready.user.uid)
            instrumentation.runOnMainSync {
                assertEquals(2, calls)
                assertEquals(uidB, model!!.currentUser?.uid)
                model!!.signOut()
            }
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally { auth.signOut(); app.delete() }
            }
        }
    }

    @Test fun cancelledProfileUpdateCannotReviveOwnerOrOverwriteNextLogin() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "profile-race-${UUID.randomUUID()}")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val store = ViewModelStore()
        val delayed = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        val emailA = "profile-a-${UUID.randomUUID()}@example.invalid"
        val emailB = "profile-b-${UUID.randomUUID()}@example.invalid"
        val password = "local-test-password"
        var model: AuthViewModel? = null
        var mutation: Job? = null
        try { withTimeout(60_000) {
            auth.createUserWithEmailAndPassword(emailA, password).await()
            val uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user!!.uid
            auth.signOut()
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(application, auth, db) as T
                })[AuthViewModel::class.java]
                model!!.signIn(emailA, password)
            }
            model!!.authState.first { it is AuthState.Authenticated }
            val userA = checkNotNull(auth.currentUser)
            val fence = AuthSessionFences.forAuth(auth, MemoryRegistrationPendingStore())
            withContext(Dispatchers.Main) {
                awaitOwnedSdkMutation<Void>(auth, fence, userA.uid) {
                    userA.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Current owner" })
                }
                assertEquals(userA.uid, model!!.currentUser?.uid)
                assertEquals("Current owner", auth.currentUser?.displayName)
                mutation = this@runBlocking.launch(Dispatchers.Main, start = CoroutineStart.UNDISPATCHED) {
                    awaitOwnedSdkMutation<Void>(auth, fence, userA.uid) { delayed.task }
                }
                assertEquals(userA.uid, model!!.currentUser?.uid)
                model!!.signOut()
                mutation!!.cancelAndJoin()
                model!!.signIn(emailB, password)
                assertTrue(model!!.authState.value is AuthState.Error)
                assertNull(model!!.currentUser)
                // Completing the uncancellable SDK operation can mutate cached SDK state after await was cancelled.
                userA.updateProfile(com.google.firebase.auth.userProfileChangeRequest { displayName = "Late owner" }).await()
                delayed.setResult(null)
                while (fence.unresolved) { yield(); delay(10) }
                assertNull(auth.currentUser)
                assertNull(model!!.currentUser)
                model!!.signIn(emailB, password)
            }
            val ready = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(uidB, ready.user.uid)
            assertNotEquals("Late owner", auth.currentUser?.displayName)
            instrumentation.runOnMainSync { model!!.signOut() }
        } } finally {
            mutation?.cancel()
            instrumentation.runOnMainSync { store.clear() }
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally { auth.signOut(); app.delete() }
            }
        }
    }

    @Test fun lateAccountCreationAfterCancelCannotPublishOrWriteProfile() =
        runLateAccountCreation(replaceViewModel = false)

    @Test fun lateAccountCreationAfterViewModelReplacementRequiresExplicitLogin() =
        runLateAccountCreation(replaceViewModel = true)

    private fun runLateAccountCreation(replaceViewModel: Boolean) = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "late-create-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val pendingFile = java.io.File(application.noBackupFilesDir, "late-create-$id.json")
        val sessionFile = java.io.File(application.noBackupFilesDir, "late-create-session-$id.json")
        val store = ViewModelStore()
        val delayed = com.google.android.gms.tasks.TaskCompletionSource<com.google.firebase.auth.AuthResult>()
        val emailA = "late-create-a-$id@example.invalid"
        val emailB = "late-create-b-$id@example.invalid"
        val password = "local-test-password"
        var creates = 0
        var logins = 0
        var profileWrites = 0
        var model: AuthViewModel? = null
        fun openModel() {
            model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(application, auth, db,
                        registrationStore = FileRegistrationPendingStore(pendingFile),
                        sessionStore = FileRegistrationPendingStore(sessionFile),
                        signInRequest = { email, secret -> logins++; auth.signInWithEmailAndPassword(email, secret) },
                        signUpRequest = { _, _ -> creates++; delayed.task },
                        beforeRegistrationProfileWrite = { profileWrites++ }) as T
            })[AuthViewModel::class.java]
        }
        try { withTimeout(60_000) {
            val uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user!!.uid
            val profileB = mapOf("uid" to uidB, "name" to "Keep B", "email" to emailB, "bio" to "Keep biography")
            db.collection("users").document(uidB).set(profileB).await()
            auth.signOut()
            instrumentation.runOnMainSync {
                openModel()
                model!!.signUp("Cancelled draft", emailA, password)
                assertEquals(1, creates)
                assertNotNull(FileRegistrationPendingStore(pendingFile).read())
                if (replaceViewModel) { store.clear(); openModel() } else model!!.cancelRegistration()
                assertNull(model!!.currentUser)
                model!!.signIn(emailB, password)
                assertEquals("No new SDK login while old creation is pending", 0, logins)
                assertTrue(model!!.authState.value is AuthState.Error)
            }
            val result = auth.createUserWithEmailAndPassword(emailA, password).await()
            val lateUser = checkNotNull(result.user)
            instrumentation.runOnMainSync { delayed.setResult(result) }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertNull(auth.currentUser)
                assertNull(model!!.currentUser)
                assertEquals(0, profileWrites)
                assertNull(lateUser.displayName)
                if (!replaceViewModel) assertNull(FileRegistrationPendingStore(pendingFile).read())
                // Only explicit credentials may restore the newly created SDK account.
                model!!.signIn(emailA, password)
            }
            val readyA = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(lateUser.uid, readyA.user.uid)
            assertNull(FileRegistrationPendingStore(pendingFile).read())
            assertFalse(db.collection("users").document(lateUser.uid).get(Source.SERVER).await().exists())
            assertEquals(0, profileWrites)
            assertNull(auth.currentUser?.displayName)
            instrumentation.runOnMainSync { model!!.signOut(); model!!.signIn(emailB, password) }
            val readyB = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(uidB, readyB.user.uid)
            assertEquals(profileB, db.collection("users").document(uidB).get(Source.SERVER).await().data)
            assertEquals(1, creates)
            instrumentation.runOnMainSync { model!!.signOut() }
            assertTrue(AuthSessionFence(FileRegistrationPendingStore(sessionFile)).signedOut)
        } } finally {
            instrumentation.runOnMainSync { store.clear() }
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally {
                    auth.signOut(); app.delete()
                    for (file in listOf(pendingFile, sessionFile)) {
                        FileRegistrationPendingStore(file).write(null)
                        check(file.delete() || !file.exists())
                    }
                }
            }
        }
    }

    @Test fun deletionReauthenticationRejectsWrongOwnerAndLateResultAfterLogout() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(application, FirebaseOptions.Builder()
            .setProjectId("demo-havamania-audit").setApplicationId("1:1234567890:android:0000000000000000")
            .setApiKey("fake-emulator-api-key").build(), "deletion-reauth-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 9096) }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("127.0.0.1", 8086)
        }
        val store = ViewModelStore()
        val delayed = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        val emailA = "deletion-a-$id@example.invalid"
        val emailB = "deletion-b-$id@example.invalid"
        val password = "local-test-password"
        var model: AuthViewModel? = null
        var work: Deferred<Result<Unit>>? = null
        var continued = 0
        try { withTimeout(60_000) {
            val uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user!!.uid
            val userA = auth.createUserWithEmailAndPassword(emailA, password).await().user!!
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(application, auth, db) as T
                })[AuthViewModel::class.java]
            }
            val fence = AuthSessionFences.forAuth(auth, MemoryRegistrationPendingStore())
            withContext(Dispatchers.Main) {
                reauthenticateDeletionOwner(auth, fence, userA, emailA, password, userA.uid)
                assertEquals(userA.uid, model!!.currentUser?.uid)
                try {
                    reauthenticateDeletionOwner(auth, fence, userA, emailA, password, uidB) {
                        fail("Wrong owner cannot issue reauthentication"); delayed.task
                    }
                    fail("Wrong owner must be rejected")
                } catch (_: IllegalArgumentException) { }
                assertEquals(userA.uid, auth.currentUser?.uid)
                work = this@runBlocking.async(Dispatchers.Main, start = CoroutineStart.UNDISPATCHED) {
                    runCatching {
                        reauthenticateDeletionOwner(auth, fence, userA, emailA, password, userA.uid) { delayed.task }
                        continued++; Unit // Represents reaching the token/journal stage; no deletion API exists in this fixture.
                    }
                }
                model!!.signOut()
                model!!.signIn(emailB, password)
                assertTrue(model!!.authState.value is AuthState.Error)
                assertNull(model!!.currentUser)
                val late = userA.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(emailA, password)).await()
                delayed.setResult(late)
            }
            assertTrue(work!!.await().isFailure)
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(0, continued)
                assertNull(auth.currentUser)
                assertNull(model!!.currentUser)
                model!!.signIn(emailB, password)
            }
            val ready = model!!.authState.first { it is AuthState.Authenticated } as AuthState.Authenticated
            assertEquals(uidB, ready.user.uid)
            instrumentation.runOnMainSync { model!!.signOut() }
        } } finally {
            work?.cancel()
            instrumentation.runOnMainSync { store.clear() }
            withContext(NonCancellable) {
                try { withTimeout(10_000) { db.terminate().await() } }
                finally { auth.signOut(); app.delete() }
            }
        }
    }

}