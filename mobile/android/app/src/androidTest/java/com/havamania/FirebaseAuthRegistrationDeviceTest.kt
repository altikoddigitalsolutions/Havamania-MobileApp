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
}