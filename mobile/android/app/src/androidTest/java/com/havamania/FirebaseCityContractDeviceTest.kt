package com.havamania

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import java.util.UUID

/** Opt in explicitly. A separate FirebaseApp, demo project and loopback endpoints only. */
class FirebaseCityContractDeviceTest {
    private var app: FirebaseApp? = null
    private var leaveRestartSession = false
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private val paris = GeocodingResultDto(1, "Paris", 48.8, 2.3, countryCode = "FR")

    @Before fun connect() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseEmulatorContract") == "true")
        withTimeout(30_000) {
            val options = FirebaseOptions.Builder().setProjectId("demo-havamania-audit")
                .setApplicationId("1:1234567890:android:0000000000000000")
                .setApiKey("fake-emulator-api-key").build()
            val restartId = InstrumentationRegistry.getArguments().getString("cityRestartId")
                ?.let { UUID.fromString(it).toString() }
            val appName = restartId?.let { "city-restart-$it" } ?: ("city-contract-" + UUID.randomUUID())
            val localApp = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext,
                options, appName)
            app = localApp
            assertEquals("demo-havamania-audit", localApp.options.projectId)
            auth = FirebaseAuth.getInstance(localApp)
            auth.useEmulator("127.0.0.1", 9096)
            db = FirebaseFirestore.getInstance(localApp)
            db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            db.useEmulator("127.0.0.1", 8086)
            uid = auth.signInAnonymously().await().user!!.uid
        }
    }
    @After fun disconnect() = runBlocking {
        if (app != null && !leaveRestartSession) {
            try { withTimeout(10_000) { db.terminate().await() } }
            finally { auth.signOut(); app?.delete(); app = null }
        }
    }
    private suspend fun seedProfileMirror(owner: String, mode: String, nonce: String) = withContext(Dispatchers.IO) {
        val connection = java.net.URL("http://127.0.0.1:9396/seed").openConnection() as java.net.HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Emulator-Test", nonce)
            connection.outputStream.use { it.write("{\"uid\":\"$owner\",\"mode\":\"$mode\"}".toByteArray()) }
            assertEquals(200, connection.responseCode)
            connection.inputStream.close()
        } finally { connection.disconnect() }
    }

    @Test fun profileAccountSwitchRejectsQueuedOldOwnerEvents() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("cityProfileContract") == "true")
        withTimeout(45_000) {
            val nonce = InstrumentationRegistry.getArguments().getString("cityProfileNonce")!!
            UUID.fromString(nonce)
            val credentials = retainLocalAccount()
            val ownerA = uid
            val refA = db.collection("users").document(ownerA)
            refA.set(UserProfile(uid = ownerA, name = "Owner A", defaultCity = "Ankara")).await()
            seedProfileMirror(ownerA, "canonical_true", nonce)
            val queued = java.util.concurrent.ConcurrentLinkedQueue<Runnable>()
            val hold = java.util.concurrent.atomic.AtomicBoolean(false)
            val executor = java.util.concurrent.Executor { command ->
                synchronized(queued) {
                    if (hold.get()) queued.add(command)
                    else com.google.android.gms.tasks.TaskExecutors.MAIN_THREAD.execute(command)
                }
            }
            val repository = UserProfileRepository(callbackExecutor = executor, firestore = { db })
            var collector: Job? = null
            try {
                repository.startObserving(ownerA)
                repository.profile.first { it?.uid == ownerA && it.isPremium }
                hold.set(true)
                refA.update("name", "Late A").await()
                while (queued.isEmpty()) delay(10) // Wait for a real SDK event, bounded by the scenario timeout.
                auth.signOut()
                val ownerB = auth.signInAnonymously().await().user!!.uid
                assertNotEquals(ownerA, ownerB)
                val refB = db.collection("users").document(ownerB)
                refB.set(UserProfile(uid = ownerB, name = "Owner B", defaultCity = "Bursa")).await()
                val afterSwitch = java.util.concurrent.CopyOnWriteArrayList<UserProfile>()
                collector = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                    repository.profile.collect { it?.let(afterSwitch::add) }
                }
                afterSwitch.clear() // Discard the initial A value before selecting B.
                repository.startObserving(ownerB)
                assertNull(repository.profile.value)
                assertNull(repository.serverDefaultCity.value)
                assertEquals(ProfileReadState.Loading(ownerB), repository.readState.value)
                synchronized(queued) { hold.set(false) }
                // Drain delayed SDK events only after B is selected, including any first B snapshot.
                withContext(Dispatchers.Main) { while (true) (queued.poll() ?: break).run() }
                repository.profile.first { it?.uid == ownerB && it.name == "Owner B" }
                repository.serverDefaultCity.first { it?.uid == ownerB }
                assertTrue(afterSwitch.all { it.uid == ownerB && !it.isPremium })
                assertFalse(repository.profile.value!!.isPremium)
                assertEquals("Bursa", repository.serverDefaultCity.value?.name)

                try {
                    seedProfileMirror(ownerA, "malformed", nonce)
                    refB.update("name", "B barrier").await()
                    repository.profile.first { it?.name == "B barrier" }
                    assertTrue(afterSwitch.all { it.uid == ownerB && !it.isPremium })
                    try { refA.get(com.google.firebase.firestore.Source.SERVER).await(); fail("B read A profile") }
                    catch (e: FirebaseFirestoreException) {
                        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
                    }
                } finally { collector?.cancelAndJoin() }
                repository.stopObserving()
                seedProfileMirror(ownerB, "canonical_true", nonce)
                assertEquals(ProfileReadState.Idle, repository.readState.value)
                assertNull(repository.profile.value)
                // Restore A with its actual emulator credentials; malformed A must not reuse B data.
                auth.signOut()
                assertEquals(ownerA, auth.signInWithEmailAndPassword(credentials.first, credentials.second).await().user!!.uid)
                repository.startObserving(ownerA)
                repository.readState.first { it is ProfileReadState.Failed }
                assertNull(repository.profile.value)
                seedProfileMirror(ownerA, "agreed_false", nonce)
                repository.profile.first { it?.uid == ownerA && it.name == "Late A" && !it.isPremium }
                assertEquals("Ankara", repository.profile.value?.defaultCity)
            } finally { repository.stopObserving(); collector?.cancelAndJoin() }
        }
    }
    @Test fun serverPremiumMirrorUpdatesAndRevokesWithoutProfileOverwrite() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("cityProfileContract") == "true")
        withTimeout(45_000) {
            val nonce = InstrumentationRegistry.getArguments().getString("cityProfileNonce")!!
            UUID.fromString(nonce)
            suspend fun seed(mode: String) = seedProfileMirror(uid, mode, nonce)
            val ref = db.collection("users").document(uid)
            ref.set(UserProfile(uid = uid, name = "Owner")).await()
            suspend fun legacyServerSnapshot() = callbackFlow {
                val registration = ref.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) close(error)
                    else if (snapshot != null) trySend(snapshot)
                }
                awaitClose { registration.remove() }
            }.first { snapshot ->
                !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites() &&
                    !snapshot.contains("isPremium") && snapshot.getBoolean("premium") == true
            }
            val repository = UserProfileRepository { db }
            try {
                repository.startObserving(uid)
                repository.serverDefaultCity.first { it?.uid == uid }
                assertEquals(false, repository.profile.value?.isPremium)
                seed("canonical_true")
                repository.profile.first { it?.isPremium == true }
                ref.set(UserProfile(uid = uid, name = "Updated", isPremium = false), SetOptions.merge()).await()
                repository.profile.first { it?.name == "Updated" }
                assertEquals(true, repository.profile.value?.isPremium)
                var stored = ref.get(com.google.firebase.firestore.Source.SERVER).await()
                assertEquals(true, stored.getBoolean("isPremium"))
                assertFalse(stored.contains("premium"))
                try { ref.update("isPremium", false).await(); fail("Client revoked server mirror") }
                catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
                db.waitForPendingWrites().await()
                seed("legacy_true")
                stored = legacyServerSnapshot()
                assertFalse("Unexpected canonical field: ${stored.get("isPremium")}; legacy=${stored.get("premium")}; pending=${stored.metadata.hasPendingWrites()}", stored.contains("isPremium"))
                assertEquals(true, stored.getBoolean("premium"))
                repository.startObserving(uid, forceRefresh = true)
                repository.profile.first { it?.isPremium == true }
                seed("conflict")
                repository.readState.first { it is ProfileReadState.Failed }
                assertNull(repository.profile.value)
                assertNull(repository.serverDefaultCity.value)
                seed("agreed_false")
                repository.profile.first { it?.isPremium == false }
                seed("canonical_true")
                repository.profile.first { it?.isPremium == true }
                seed("agreed_false")
                repository.profile.first { it?.isPremium == false }
                seed("malformed")
                val failed = repository.readState.first { it is ProfileReadState.Failed }
                assertEquals(ProfileReadState.Failed(uid, PROFILE_DATA_ERROR), failed)
                assertNull(repository.profile.value)
                seed("canonical_true")
                repository.profile.first { it?.isPremium == true }
                assertEquals("Updated", repository.profile.value?.name)
            } finally { repository.stopObserving() }
        }
    }
    @Test fun premiumMirrorIsExcludedFromClientWritesAndPendingGrantsAreIgnored() = runBlocking {
        withTimeout(30_000) {
            val ref = db.collection("users").document(uid)
            ref.set(UserProfile(uid = uid, name = "Owner", isPremium = true)).await()
            val stored = ref.get(com.google.firebase.firestore.Source.SERVER).await()
            assertFalse("Unexpected canonical field: ${stored.get("isPremium")}; legacy=${stored.get("premium")}; pending=${stored.metadata.hasPendingWrites()}", stored.contains("isPremium"))
            assertFalse(stored.contains("premium"))
            val repository = UserProfileRepository { db }
            val observed = java.util.concurrent.CopyOnWriteArrayList<Boolean>()
            val collector = launch(Dispatchers.Unconfined) {
                repository.profile.collect { profile -> profile?.let { observed.add(it.isPremium) } }
            }
            try {
                repository.startObserving(uid)
                repository.serverDefaultCity.first { it?.uid == uid }
                assertEquals(false, repository.profile.value?.isPremium)
                for (field in listOf("isPremium", "premium")) {
                    try { ref.update(field, true).await(); fail("Client premium grant accepted") }
                    catch (e: FirebaseFirestoreException) {
                        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
                    }
                }
                ref.update("name", "Updated").await()
                repository.profile.first { it?.name == "Updated" }
                assertFalse(observed.any { it })
                val final = ref.get(com.google.firebase.firestore.Source.SERVER).await()
                assertEquals("Updated", final.getString("name"))
                assertFalse(final.contains("isPremium"))
                assertFalse(final.contains("premium"))
            } finally { repository.stopObserving(); collector.cancelAndJoin() }
        }
    }
    @Test fun actualAndroidSerializationRoundTripsAndPreservesProfile() = runBlocking {
        withTimeout(30_000) {
            val ref = db.collection("users").document(uid)
            ref.set(mapOf("displayName" to "Preserved", "defaultCity" to "Ankara")).await()
            ref.set(defaultCityProfileFields(paris), SetOptions.merge()).await()
            val snapshot = ref.get(com.google.firebase.firestore.Source.SERVER).await()
            assertFalse(snapshot.metadata.isFromCache)
            assertFalse(snapshot.metadata.hasPendingWrites())
            assertEquals("Preserved", snapshot.getString("displayName"))
            assertEquals(paris, resolveDefaultCityProfile(snapshot.getString("defaultCity"),
                snapshot.get("defaultCityLocation"), emptyList()))
            val wrong = defaultCityProfileFields(paris.copy(latitude = 49.0)).toMutableMap()
            wrong["defaultCity"] = "Ankara"
            try { ref.set(wrong, SetOptions.merge()).await(); fail("Conflicting name accepted") }
            catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
            assertEquals("Paris", ref.get(com.google.firebase.firestore.Source.SERVER).await().getString("defaultCity"))
        }
    }
    @Test fun actualTransactionRejectsOtherIdentityAndAllowsMatchingDelete() = runBlocking {
        withTimeout(30_000) {
            val ref = db.collection("users").document(uid).collection("cities").document("1")
            val other = paris.copy(latitude = 33.6, longitude = -95.5, countryCode = "US")
            ref.set(other).await()
            suspend fun apply(change: CitySyncChange, allowed: Boolean = true): Boolean = db.runTransaction { tx ->
                stageCityDocumentMutation(change, { allowed },
                    read = { val snap = tx.get(ref); if (snap.exists()) snap.toObject(GeocodingResultDto::class.java) else null },
                    write = { tx.set(ref, change.city) }, delete = { tx.delete(ref) })
            }.await()
            try { apply(CitySyncChange(paris, true)); fail("Wrong identity deleted") }
            catch (e: Exception) { assertTrue("SDK must preserve conflict cause", isCityIdentityConflict(e)) }
            assertEquals(other, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
            assertFalse(apply(CitySyncChange(other, true), false))
            assertTrue(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
            assertTrue(apply(CitySyncChange(other, true)))
            assertFalse(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
            assertTrue(apply(CitySyncChange(other, true))) // Missing delete is idempotent.
            assertTrue(apply(CitySyncChange(paris, false)))
            assertEquals(paris, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
        }
    }
    @Test fun authenticatedAndroidCannotAccessAnotherAccount() = runBlocking {
        withTimeout(30_000) {
            val other = db.collection("users").document("another-" + UUID.randomUUID())
            try { other.set(defaultCityProfileFields(paris)).await(); fail("Other account written") }
            catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
            try { other.get(com.google.firebase.firestore.Source.SERVER).await(); fail("Other account read") }
            catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
        }
    }

    private suspend fun applyCityTransaction(ref: com.google.firebase.firestore.DocumentReference,
        change: CitySyncChange): Boolean = db.runTransaction { tx ->
        stageCityDocumentMutation(change, { true },
            read = { val snap = tx.get(ref); if (snap.exists()) snap.toObject(GeocodingResultDto::class.java) else null },
            write = { tx.set(ref, change.city) }, delete = { tx.delete(ref) })
    }.await()

    @Test fun disabledNetworkKeepsProfileWritePendingAndReconnectRechecksCityIdentity() = runBlocking {
        withTimeout(45_000) {
            val ref = db.collection("users").document(uid).collection("cities").document("1")
            val profile = db.collection("users").document(uid)
            ref.set(paris).await()
            db.disableNetwork().await()
            val queued = profile.set(defaultCityProfileFields(paris), SetOptions.merge())
            try {
                val cached = profile.get(com.google.firebase.firestore.Source.CACHE).await()
                assertTrue(cached.metadata.isFromCache)
                assertTrue(cached.metadata.hasPendingWrites())
                assertFalse("Local cache is not a server acknowledgement", queued.isComplete)
            } finally { db.enableNetwork().await() }
            queued.await()
            val confirmed = profile.get(com.google.firebase.firestore.Source.SERVER).await()
            assertFalse(confirmed.metadata.isFromCache)
            assertFalse(confirmed.metadata.hasPendingWrites())
            assertEquals(paris, resolveDefaultCityProfile(confirmed.getString("defaultCity"),
                confirmed.get("defaultCityLocation"), emptyList()))
            assertEquals(paris, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
            val changed = paris.copy(latitude = 33.6, longitude = -95.5, countryCode = "US")
            ref.set(changed).await()
            try { applyCityTransaction(ref, CitySyncChange(paris, true)); fail("Stale identity deleted after reconnect") }
            catch (e: Exception) { assertTrue(isCityIdentityConflict(e)); assertFalse(canManuallyRetryCitySync(e)) }
            assertEquals(changed, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
        }
    }

    private suspend fun retainLocalAccount(): Pair<String, String> {
        val email = "sdk-" + UUID.randomUUID() + "@example.invalid"
        val password = UUID.randomUUID().toString()
        auth.currentUser!!.linkWithCredential(EmailAuthProvider.getCredential(email, password)).await()
        return email to password
    }
    private suspend fun switchToOtherLocalAccount(): String {
        auth.signOut()
        return auth.signInAnonymously().await().user!!.uid
    }
    private suspend fun restoreLocalAccount(credentials: Pair<String, String>) {
        auth.signOut()
        assertEquals(uid, auth.signInWithEmailAndPassword(credentials.first, credentials.second).await().user!!.uid)
    }

    @Test fun accountSwitchAfterReservationPreventsSendAndAcknowledgement() = runBlocking {
        withTimeout(45_000) {
            val credentials = retainLocalAccount()
            val ref = db.collection("users").document(uid).collection("cities").document("1")
            ref.set(paris).await()
            val pending = CitySyncChange(paris, true)
            var sends = 0
            var acknowledgements = 0
            var otherUid = ""
            CitySyncDispatcher.sendLatest("$uid:sdk-reserve", uid, { auth.currentUser?.uid ?: "legacy" },
                read = { pending },
                reserve = { change, _ -> otherUid = switchToOtherLocalAccount(); change },
                send = { sends++; applyCityTransaction(ref, it) },
                acknowledge = { acknowledgements++ })
            assertNotEquals(uid, otherUid)
            assertEquals(0, sends)
            assertEquals(0, acknowledgements)
            assertFalse(db.collection("users").document(otherUid).collection("cities").document("1")
                .get(com.google.firebase.firestore.Source.SERVER).await().exists())
            restoreLocalAccount(credentials)
            assertEquals(paris, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
        }
    }

    @Test fun switchAfterRemoteCommitSuppressesLocalAcknowledgementButCannotUndoCommit() = runBlocking {
        withTimeout(45_000) {
            val credentials = retainLocalAccount()
            val ref = db.collection("users").document(uid).collection("cities").document("1")
            ref.set(paris).await()
            var pending: CitySyncChange? = CitySyncChange(paris, true)
            var acknowledgements = 0
            var otherUid = ""
            CitySyncDispatcher.sendLatest("$uid:sdk-commit", uid, { auth.currentUser?.uid ?: "legacy" },
                read = { pending },
                send = { assertTrue(applyCityTransaction(ref, it)); otherUid = switchToOtherLocalAccount() },
                acknowledge = { acknowledgements++; pending = null })
            assertNotEquals(uid, otherUid)
            assertEquals(0, acknowledgements)
            assertNotNull(pending)
            assertFalse(db.collection("users").document(otherUid).collection("cities").document("1")
                .get(com.google.firebase.firestore.Source.SERVER).await().exists())
            restoreLocalAccount(credentials)
            assertFalse(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
            // Retrying the retained operation for A is an idempotent missing-record deletion.
            CitySyncDispatcher.sendLatest("$uid:sdk-commit", uid, { auth.currentUser?.uid ?: "legacy" },
                read = { pending }, send = { assertTrue(applyCityTransaction(ref, it)) },
                acknowledge = { acknowledgements++; pending = null })
            assertEquals(1, acknowledgements)
            assertNull(pending)
        }
    }

    @Test fun durableDeletionReopensAfterCommitAndAccountSwitchWithoutPrematureRetry() = runBlocking {
        withTimeout(60_000) {
            val file = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "sdk-durable-" + UUID.randomUUID() + ".preferences_pb")
            var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                fun openStore(): CitySyncStore = CitySyncStore(
                    androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope) { file })
                var store = openStore()
                val credentials = retainLocalAccount()
                val cities = db.collection("users").document(uid).collection("cities")
                val ref = cities.document("1")
                val keep = paris.copy(id = 2, name = "Lyon", latitude = 45.76, longitude = 4.84)
                ref.set(paris).await()
                cities.document("2").set(keep).await()
                suspend fun remoteCities() = cities.get(com.google.firebase.firestore.Source.SERVER).await()
                    .documents.mapNotNull { it.toObject(GeocodingResultDto::class.java) }
                store.mergeRemoteCities(uid, remoteCities())
                assertTrue(store.recordCityChange(uid, paris, true))
                val revision = store.pendingCityChanges(uid).single().revision
                var now = 1_000L
                var sends = 0
                var otherUid = ""
                suspend fun dispatch(switchAfterCommit: Boolean) {
                    CitySyncDispatcher.sendLatest("$uid:sdk-durable", uid, { auth.currentUser?.uid ?: "legacy" },
                        read = { singleCitySyncChange(store.pendingCityChanges(uid), paris.id) },
                        now = { now },
                        reserve = { change, wall -> store.reserveAttempt(uid, change, false, wall) },
                        send = {
                            sends++
                            assertTrue(applyCityTransaction(ref, it))
                            if (switchAfterCommit) otherUid = switchToOtherLocalAccount()
                        }, acknowledge = { store.acknowledge(uid, it) })
                }
                dispatch(true)
                assertNotEquals(uid, otherUid)
                assertEquals(1, sends)
                val reserved = store.pendingCityChanges(uid).single()
                assertEquals(revision, reserved.revision)
                assertFalse(reserved.acknowledged)
                assertTrue(reserved.retryNotBefore > now)
                store.recordDefaultCity(otherUid, keep)
                val otherPending = store.pendingDefaultCity(otherUid)

                // Close the real DataStore and open its on-disk file in a fresh scope.
                // This is a store restart, not an Android process-kill simulation.
                scope.coroutineContext[Job]!!.cancelAndJoin()
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                store = openStore()
                assertEquals(reserved, store.pendingCityChanges(uid).single())
                assertEquals(listOf(keep), store.observeCities(uid).first())
                assertEquals(otherPending, store.pendingDefaultCity(otherUid))
                dispatch(false) // Still B: A's queue must not be sent or acknowledged.
                assertEquals(1, sends)
                assertEquals(reserved, store.pendingCityChanges(uid).single())

                restoreLocalAccount(credentials)
                assertFalse(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
                try { dispatch(false); fail("Uncertain-outcome reservation was ignored") }
                catch (e: CitySyncWaitingException) { assertEquals(reserved.retryNotBefore, e.retryAt) }
                assertEquals(1, sends)
                assertEquals(reserved, store.pendingCityChanges(uid).single())

                now = reserved.retryNotBefore // Advance the injected wall clock; no real sleep.
                dispatch(false)
                assertEquals(2, sends)
                assertTrue(store.pendingCityChanges(uid).single().acknowledged)
                assertEquals(listOf(keep), store.mergeRemoteCities(uid, remoteCities()))
                assertTrue(store.pendingCityChanges(uid).isEmpty())
                assertEquals(otherPending, store.pendingDefaultCity(otherUid))
                assertEquals(keep, store.observeDefaultCity(otherUid).first())
                scope.coroutineContext[Job]!!.cancelAndJoin()
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                store = openStore()
                assertTrue(store.pendingCityChanges(uid).isEmpty())
                assertEquals(listOf(keep), store.observeCities(uid).first())
                assertEquals(otherPending, store.pendingDefaultCity(otherUid))
            } finally {
                scope.coroutineContext[Job]!!.cancelAndJoin()
                file.delete()
            }
        }
    }

    @Test fun processRestartMaintainsDeletionFence() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("cityRestartPhase")
        assumeTrue(phase == "prepare" || phase == "resume")
        val scenario = UUID.fromString(args.getString("cityRestartId")).toString()
        withTimeout(60_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val dataFile = java.io.File(context.filesDir, "sdk-restart-$scenario.preferences_pb")
            val metadataFile = java.io.File(context.filesDir, "sdk-restart-$scenario.json")
            var clock = CitySyncClocks.capture(context)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            var keepOpenForStop = false
            var finished = false
            try {
                val store = CitySyncStore(androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope) { dataFile }, clock = { clock })
                val cities = db.collection("users").document(uid).collection("cities")
                val ref = cities.document("1")
                val keep = paris.copy(id = 2, name = "Lyon", latitude = 45.76, longitude = 4.84)
                suspend fun remoteCities() = cities.get(com.google.firebase.firestore.Source.SERVER).await()
                    .documents.mapNotNull { it.toObject(GeocodingResultDto::class.java) }
                if (phase == "prepare") {
                    assertFalse(metadataFile.exists())
                    ref.set(paris).await()
                    cities.document("2").set(keep).await()
                    store.mergeRemoteCities(uid, remoteCities())
                    assertTrue(store.recordCityChange(uid, paris, true))
                    val reserved = store.reserveAttempt(uid, store.pendingCityChanges(uid).single(), false, clock.wall)!!
                    assertTrue(applyCityTransaction(ref, reserved))
                    assertFalse(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
                    assertFalse(store.pendingCityChanges(uid).single().acknowledged)
                    val metadata = org.json.JSONObject().put("uid", uid).put("revision", reserved.revision)
                        .put("retryAt", reserved.retryNotBefore).put("pid", android.os.Process.myPid())
                    java.io.FileOutputStream(metadataFile).use { output ->
                        output.write(metadata.toString().toByteArray(Charsets.UTF_8)); output.fd.sync()
                    }
                    // Intentionally leave this test-only DataStore and named FirebaseApp open.
                    // The host stops the QA process before the resume instrumentation starts.
                    keepOpenForStop = true
                    leaveRestartSession = true
                } else {
                    assertTrue(metadataFile.exists())
                    val metadata = org.json.JSONObject(metadataFile.readText())
                    assertNotEquals(metadata.getInt("pid"), android.os.Process.myPid())
                    assertEquals("Named emulator Auth session must survive process restart", metadata.getString("uid"), uid)
                    val pending = store.pendingCityChanges(uid).single()
                    assertEquals(metadata.getString("revision"), pending.revision)
                    assertEquals(metadata.getLong("retryAt"), pending.retryNotBefore)
                    assertTrue(pending.deleted)
                    assertFalse(pending.acknowledged)
                    assertEquals(listOf(keep), store.observeCities(uid).first())
                    assertFalse(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
                    assertTrue(pending.retryDuration > 0)
                    store.refreshRetryClock(uid, pending, false)
                    var sends = 0
                    suspend fun dispatch() {
                        CitySyncDispatcher.sendLatest("$uid:restart", uid, { auth.currentUser?.uid ?: "legacy" },
                            read = { singleCitySyncChange(store.pendingCityChanges(uid), paris.id) }, now = { clock.wall }, clock = { clock },
                            reserve = { change, wall -> store.reserveAttempt(uid, change, false, wall) },
                            send = { sends++; assertTrue(applyCityTransaction(ref, it)) },
                            acknowledge = { store.acknowledge(uid, it) })
                    }
                    try { dispatch(); fail("Restart bypassed reservation") }
                    catch (e: CitySyncWaitingException) { assertTrue(e.retryAt > clock.wall) }
                    assertEquals(0, sends)
                    recoverCityQueueAutomatically(store, { clock }, advance = { millis ->
                        clock = clock.copy(wall = clock.wall + millis / 1_000, elapsed = clock.elapsed + millis / 1_000)
                    }, send = { dispatch() }, confirm = {
                        assertEquals(listOf(keep), store.mergeRemoteCities(uid, remoteCities()))
                    })
                    assertEquals(1, sends)
                    assertTrue(store.pendingCityChanges(uid).isEmpty())
                    finished = true
                }
            } finally {
                if (!keepOpenForStop) {
                    scope.coroutineContext[Job]!!.cancelAndJoin()
                    if (finished) { dataFile.delete(); metadataFile.delete() }
                }
            }
        }
    }

    @Test fun actualConflictRetryRereadsIdentityBeforeDeleting() = runBlocking {
        withTimeout(45_000) {
            val ref = db.collection("users").document(uid).collection("cities").document("1")
            ref.set(paris).await()
            val changed = paris.copy(latitude = 33.6, longitude = -95.5, countryCode = "US")
            val firstRead = CompletableDeferred<Unit>()
            val resume = java.util.concurrent.CountDownLatch(1)
            val attempts = java.util.concurrent.atomic.AtomicInteger()
            val stagedDeletes = java.util.concurrent.atomic.AtomicInteger()
            val task = db.runTransaction { tx ->
                val attempt = attempts.incrementAndGet()
                stageCityDocumentMutation(CitySyncChange(paris, true), { true },
                    read = {
                        val snap = tx.get(ref)
                        if (attempt == 1) {
                            firstRead.complete(Unit)
                            check(resume.await(15, java.util.concurrent.TimeUnit.SECONDS)) { "Competing write did not finish" }
                        }
                        if (snap.exists()) snap.toObject(GeocodingResultDto::class.java) else null
                    }, write = { tx.set(ref, paris) },
                    delete = { stagedDeletes.incrementAndGet(); tx.delete(ref) })
            }
            try {
                withTimeout(10_000) {
                    firstRead.await()
                    ref.set(changed).await() // Must be acknowledged before releasing the old transaction read.
                }
            } finally { resume.countDown() }
            try { task.await(); fail("Retried transaction deleted a changed identity") }
            catch (e: Exception) {
                assertTrue("Conflict must be detected on a fresh SDK attempt", attempts.get() >= 2)
                assertTrue("Original stale attempt must have staged one delete", stagedDeletes.get() == 1)
                assertTrue(isCityIdentityConflict(e))
                assertFalse(canManuallyRetryCitySync(e))
            }
            assertEquals(changed, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
        }
    }

    @Test fun transportCutPreservesReservationAndRejectsLateDelete() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("cityNetworkCut") == "true")
        val nonce = UUID.fromString(args.getString("cityNetworkNonce")).toString()
        fun control(action: String) = proxyControl(action, nonce)
        withTimeout(60_000) {
            val file = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "sdk-transport-" + UUID.randomUUID() + ".preferences_pb")
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                var clock = CitySyncClock(1_000, 100, "transport-test-boot")
                val store = CitySyncStore(androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope) { file }, clock = { clock })
                val cities = db.collection("users").document(uid).collection("cities")
                val ref = cities.document("1")
                val keep = paris.copy(id = 2, name = "Lyon", latitude = 45.76, longitude = 4.84)
                ref.set(paris).await(); cities.document("2").set(keep).await()
                suspend fun remoteCities() = cities.get(com.google.firebase.firestore.Source.SERVER).await()
                    .documents.mapNotNull { it.toObject(GeocodingResultDto::class.java) }
                store.mergeRemoteCities(uid, remoteCities())
                assertTrue(store.recordCityChange(uid, paris, true))
                var sdkTask: com.google.android.gms.tasks.Task<Boolean>? = null
                val reads = java.util.concurrent.atomic.AtomicInteger()
                val stagedDeletes = java.util.concurrent.atomic.AtomicInteger()
                suspend fun dispatch(timeout: Long) {
                    CitySyncDispatcher.sendLatest("$uid:transport", uid, { auth.currentUser?.uid ?: "legacy" },
                        read = { singleCitySyncChange(store.pendingCityChanges(uid), paris.id) }, now = { clock.wall }, clock = { clock },
                        timeoutMillis = timeout,
                        reserve = { change, wall -> store.reserveAttempt(uid, change, false, wall) },
                        send = { change ->
                            val requestJob = currentCoroutineContext()[Job]!!
                            val task = db.runTransaction { tx ->
                                stageCityDocumentMutation(change, { requestJob.isActive && auth.currentUser?.uid == uid },
                                    read = {
                                        reads.incrementAndGet()
                                        val snap = tx.get(ref)
                                        if (snap.exists()) snap.toObject(GeocodingResultDto::class.java) else null
                                    }, write = { tx.set(ref, change.city) },
                                    delete = { stagedDeletes.incrementAndGet(); tx.delete(ref) })
                            }
                            sdkTask = task
                            if (!task.await()) throw CancellationException("Operation no longer active")
                        }, acknowledge = { store.acknowledge(uid, it) })
                }
                val cut = control("cut")
                assertTrue(cut.getBoolean("cut"))
                assertTrue("A live TCP connection must actually be destroyed", cut.getInt("destroyedSockets") > 0)
                try { dispatch(250); fail("Cut transport did not time out") }
                catch (_: CitySyncTimeoutException) { }
                assertTrue("The SDK must attempt a server read while transport is cut", reads.get() > 0)
                val reserved = store.pendingCityChanges(uid).single()
                assertFalse(reserved.acknowledged)
                assertTrue(reserved.retryNotBefore > clock.wall)
                val lateTask = requireNotNull(sdkTask)
                assertFalse(control("restore").getBoolean("cut"))
                try { assertFalse("Cancelled request must not apply a late delete", withTimeout(30_000) { lateTask.await() }) }
                catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.UNAVAILABLE, e.code) }
                assertEquals(0, stagedDeletes.get())
                assertEquals(paris, ref.get(com.google.firebase.firestore.Source.SERVER).await().toObject(GeocodingResultDto::class.java))
                assertEquals(reserved, store.pendingCityChanges(uid).single())
                try { dispatch(30_000); fail("Timeout reservation bypassed") }
                catch (_: CitySyncWaitingException) { }
                recoverCityQueueAutomatically(store, { clock }, advance = { millis ->
                    clock = clock.copy(wall = clock.wall + millis / 1_000, elapsed = clock.elapsed + millis / 1_000)
                }, send = { dispatch(30_000) }, confirm = {
                    assertEquals(listOf(keep), store.mergeRemoteCities(uid, remoteCities()))
                })
                assertEquals(1, stagedDeletes.get())
                assertTrue(store.pendingCityChanges(uid).isEmpty())
            } finally {
                try { control("restore") }
                finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
            }
        }
    }
    private fun proxyControl(action: String, nonce: String): org.json.JSONObject {
            val connection = java.net.URL("http://127.0.0.1:9296/$action").openConnection() as java.net.HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("X-Emulator-Test", nonce)
                connection.connectTimeout = 3_000; connection.readTimeout = 3_000
                assertEquals(200, connection.responseCode)
                return org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            } finally { connection.disconnect() }
        }

    @Test fun lostCommitReplyKeepsQueueUntilIdempotentConfirmation() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("cityReplyLoss") == "true")
        val nonce = UUID.fromString(args.getString("cityNetworkNonce")).toString()
        withTimeout(60_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val file = java.io.File(context.cacheDir, "sdk-reply-" + UUID.randomUUID() + ".preferences_pb")
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            var oracleApp: FirebaseApp? = null
            var oracleDb: FirebaseFirestore? = null
            var oracleAuth: FirebaseAuth? = null
            val resume = java.util.concurrent.CountDownLatch(1)
            try {
                val credentials = retainLocalAccount()
                val observerApp = FirebaseApp.initializeApp(context, app!!.options, "city-oracle-" + UUID.randomUUID())
                oracleApp = observerApp
                val observerAuth = FirebaseAuth.getInstance(observerApp)
                oracleAuth = observerAuth
                observerAuth.useEmulator("127.0.0.1", 9096)
                assertEquals(uid, observerAuth.signInWithEmailAndPassword(credentials.first, credentials.second).await().user!!.uid)
                val observer = FirebaseFirestore.getInstance(observerApp)
                oracleDb = observer
                observer.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
                observer.useEmulator("127.0.0.1", 8088) // Direct loopback route, outside the reply-dropping proxy.
                val oracleRef = observer.collection("users").document(uid).collection("cities").document("1")
                val cities = db.collection("users").document(uid).collection("cities")
                val ref = cities.document("1")
                val keep = paris.copy(id = 2, name = "Lyon", latitude = 45.76, longitude = 4.84)
                ref.set(paris).await(); cities.document("2").set(keep).await()
                assertTrue(oracleRef.get(com.google.firebase.firestore.Source.SERVER).await().exists())
                suspend fun remoteCities() = cities.get(com.google.firebase.firestore.Source.SERVER).await()
                    .documents.mapNotNull { it.toObject(GeocodingResultDto::class.java) }
                var clock = CitySyncClock(1_000, 100, "reply-loss-test-boot")
                val store = CitySyncStore(androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope) { file }, clock = { clock })
                store.mergeRemoteCities(uid, remoteCities())
                assertTrue(store.recordCityChange(uid, paris, true))
                val firstRead = CompletableDeferred<Unit>()
                val reads = java.util.concurrent.atomic.AtomicInteger()
                val stagedDeletes = java.util.concurrent.atomic.AtomicInteger()
                var sdkTask: com.google.android.gms.tasks.Task<Boolean>? = null
                suspend fun dispatch(timeout: Long) {
                    CitySyncDispatcher.sendLatest("$uid:reply-loss", uid, { auth.currentUser?.uid ?: "legacy" },
                        read = { singleCitySyncChange(store.pendingCityChanges(uid), paris.id) }, now = { clock.wall }, clock = { clock },
                        timeoutMillis = timeout,
                        reserve = { change, wall -> store.reserveAttempt(uid, change, false, wall) },
                        send = { change ->
                            val job = currentCoroutineContext()[Job]!!
                            val task = db.runTransaction { tx ->
                                stageCityDocumentMutation(change, { job.isActive && auth.currentUser?.uid == uid },
                                    read = {
                                        val snap = tx.get(ref)
                                        if (reads.incrementAndGet() == 1) {
                                            firstRead.complete(Unit)
                                            check(resume.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                        }
                                        if (snap.exists()) snap.toObject(GeocodingResultDto::class.java) else null
                                    }, write = { tx.set(ref, change.city) },
                                    delete = { stagedDeletes.incrementAndGet(); tx.delete(ref) })
                            }
                            sdkTask = task
                            if (!task.await()) throw CancellationException("Operation no longer active")
                        }, acknowledge = { store.acknowledge(uid, it) })
                }
                val outcome = async { runCatching { dispatch(4_000) } }
                try {
                    withTimeout(10_000) { firstRead.await() }
                    assertTrue(proxyControl("mute", nonce).getBoolean("muted"))
                } finally { resume.countDown() }
                withTimeout(10_000) {
                    while (oracleRef.get(com.google.firebase.firestore.Source.SERVER).await().exists()) delay(25)
                }
                val lateTask = requireNotNull(sdkTask)
                assertFalse("Remote commit must be observed before its original SDK acknowledgement", lateTask.isComplete)
                assertTrue(outcome.await().exceptionOrNull() is CitySyncTimeoutException)
                assertTrue(proxyControl("status", nonce).getLong("droppedReplyBytes") > 0)
                assertEquals(1, stagedDeletes.get())
                val reserved = store.pendingCityChanges(uid).single()
                assertFalse(reserved.acknowledged)
                assertTrue(reserved.retryNotBefore > clock.wall)
                assertFalse(proxyControl("restore", nonce).getBoolean("muted"))
                try { withTimeout(30_000) { lateTask.await() } }
                catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.UNAVAILABLE, e.code) }
                assertEquals(reserved, store.pendingCityChanges(uid).single())
                assertFalse(oracleRef.get(com.google.firebase.firestore.Source.SERVER).await().exists())
                assertEquals(1, stagedDeletes.get())
                try { dispatch(30_000); fail("Reply loss bypassed reservation") }
                catch (_: CitySyncWaitingException) { }
                recoverCityQueueAutomatically(store, { clock }, advance = { millis ->
                    clock = clock.copy(wall = clock.wall + millis / 1_000, elapsed = clock.elapsed + millis / 1_000)
                }, send = { dispatch(30_000) }, confirm = {
                    assertEquals(listOf(keep), store.mergeRemoteCities(uid, remoteCities()))
                })
                assertEquals(1, stagedDeletes.get()) // Automatic confirmation must not stage a second delete.
                assertTrue(store.pendingCityChanges(uid).isEmpty())
            } finally {
                resume.countDown()
                try { proxyControl("restore", nonce) }
                finally {
                    scope.coroutineContext[Job]!!.cancelAndJoin()
                    try { oracleDb?.terminate()?.await() }
                    finally { oracleAuth?.signOut(); oracleApp?.delete(); file.delete() }
                }
            }
        }
    }

    @Test fun recoveryLoopAutomaticallyResumesPersistedDueDeletion() = runBlocking {
        withTimeout(45_000) {
            val file = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "sdk-auto-recovery-" + UUID.randomUUID() + ".preferences_pb")
            var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            var clock = CitySyncClock(1_000, 100, "recovery-test-boot")
            try {
                fun open() = CitySyncStore(androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope) { file },
                    clock = { clock })
                var store = open()
                val cities = db.collection("users").document(uid).collection("cities")
                val ref = cities.document("1")
                val keep = paris.copy(id = 2, name = "Lyon", latitude = 45.76, longitude = 4.84)
                ref.set(paris).await(); cities.document("2").set(keep).await()
                suspend fun remoteCities() = cities.get(com.google.firebase.firestore.Source.SERVER).await()
                    .documents.mapNotNull { it.toObject(GeocodingResultDto::class.java) }
                store.mergeRemoteCities(uid, remoteCities())
                assertTrue(store.recordCityChange(uid, paris, true))
                val reserved = store.reserveAttempt(uid, store.pendingCityChanges(uid).single(), false, clock.wall)!!
                scope.coroutineContext[Job]!!.cancelAndJoin()
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                store = open()
                assertEquals(reserved, store.pendingCityChanges(uid).single())
                var finished = false
                var sends = 0
                var waitedMillis = 0L
                runCitySyncRecovery(uid, { if (finished) "different-owner" else uid }, { true },
                    read = { store.pendingCityChanges(uid).map { CitySyncRecoveryWork(it) } }, clock = { clock },
                    send = { work ->
                        assertTrue(clock.elapsed - reserved.retryElapsedStart >= reserved.retryDuration)
                        sends++
                        CitySyncDispatcher.sendLatest("$uid:auto-recovery", uid, { auth.currentUser?.uid ?: "legacy" },
                            read = { singleCitySyncChange(store.pendingCityChanges(uid), work.change.city.id) },
                            now = { clock.wall }, clock = { clock },
                            reserve = { change, wall -> store.reserveAttempt(uid, change, false, wall) },
                            send = { assertTrue(applyCityTransaction(ref, it)) },
                            acknowledge = { store.acknowledge(uid, it) })
                        assertFalse(ref.get(com.google.firebase.firestore.Source.SERVER).await().exists())
                        assertEquals(listOf(keep), store.mergeRemoteCities(uid, remoteCities()))
                        finished = true
                    }, onFailure = { throw it }, pause = { millis ->
                        assertEquals(0, sends)
                        assertTrue(millis in 1_000L..30_000L)
                        waitedMillis += millis
                        clock = clock.copy(wall = clock.wall + millis / 1_000, elapsed = clock.elapsed + millis / 1_000)
                    })
                assertEquals(1, sends)
                assertEquals(reserved.retryDuration * 1_000, waitedMillis)
                assertTrue(store.pendingCityChanges(uid).isEmpty())
            } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
        }
    }

    /** Virtual waiting drives the production loop; the test never invokes the final retry itself. */
    private suspend fun recoverCityQueueAutomatically(
        store: CitySyncStore, clock: () -> CitySyncClock, advance: (Long) -> Unit,
        send: suspend () -> Unit, confirm: suspend () -> Unit
    ) {
        val remaining = store.pendingCityChanges(uid).single().retrySecondsLeft(clock())
        assertTrue(remaining > 0)
        var done = false
        var sends = 0
        var waitedMillis = 0L
        runCitySyncRecovery(uid, { if (done) "fixture-finished" else auth.currentUser?.uid ?: "legacy" }, { true },
            read = { store.pendingCityChanges(uid).map { CitySyncRecoveryWork(it) } }, clock = clock,
            send = {
                sends++
                send()
                assertTrue(store.pendingCityChanges(uid).single().acknowledged)
                confirm()
                assertTrue(store.pendingCityChanges(uid).isEmpty())
                done = true
            }, onFailure = { throw it }, pause = { millis ->
                assertEquals("No early automatic send", 0, sends)
                assertTrue(millis in 1_000L..30_000L)
                waitedMillis += millis
                advance(millis)
            })
        assertEquals(1, sends)
        assertEquals(remaining * 1_000, waitedMillis)
    }

    @Test fun recoverySnapshotCannotSendSupersededRevisionOfAnotherCity() = runBlocking {
        withTimeout(45_000) {
            val file = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "sdk-revision-recovery-" + UUID.randomUUID() + ".preferences_pb")
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val clock = CitySyncClock(1_000, 100, "revision-test-boot")
            try {
                val store = CitySyncStore(androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope) { file },
                    clock = { clock })
                val second = paris.copy(id = 2, name = "Lyon", latitude = 45.76, longitude = 4.84)
                val latest = second.copy(admin1 = "Auvergne-Rhône-Alpes")
                assertTrue(store.recordCityChange(uid, paris, false))
                assertTrue(store.recordCityChange(uid, second, false))
                val oldRevision = singleCitySyncChange(store.pendingCityChanges(uid), 2)!!.revision
                var latestRevision = ""
                var done = false
                val sent = mutableListOf<CitySyncChange>()
                val cities = db.collection("users").document(uid).collection("cities")
                runCitySyncRecovery(uid, { if (done) "fixture-finished" else auth.currentUser?.uid ?: "legacy" }, { true },
                    read = { store.pendingCityChanges(uid).map { CitySyncRecoveryWork(it) } }, clock = { clock },
                    send = { work ->
                        CitySyncDispatcher.sendLatest("$uid:revision:${work.change.city.id}", uid,
                            { auth.currentUser?.uid ?: "legacy" },
                            read = { singleCitySyncChange(store.pendingCityChanges(uid), work.change.city.id) },
                            now = { clock.wall }, clock = { clock },
                            reserve = { change, wall -> store.reserveAttempt(uid, change, false, wall) },
                            send = { change ->
                                sent += change
                                assertTrue(applyCityTransaction(cities.document(change.city.id.toString()), change))
                            }, acknowledge = { store.acknowledge(uid, it) })
                        if (work.change.city.id == 1L) {
                            // The recovery batch already holds the old second-row snapshot.
                            assertTrue(store.recordCityChange(uid, second, true))
                            assertTrue(store.recordCityChange(uid, latest, false))
                            latestRevision = singleCitySyncChange(store.pendingCityChanges(uid), 2)!!.revision
                            assertNotEquals(oldRevision, latestRevision)
                        } else {
                            assertEquals(oldRevision, work.change.revision)
                            assertEquals(latest, cities.document("2").get(com.google.firebase.firestore.Source.SERVER)
                                .await().toObject(GeocodingResultDto::class.java))
                            val remote = cities.get(com.google.firebase.firestore.Source.SERVER).await()
                                .documents.mapNotNull { it.toObject(GeocodingResultDto::class.java) }
                            store.mergeRemoteCities(uid, remote)
                            done = true
                        }
                    }, onFailure = { throw it }, pause = { fail("Unexpected idle recovery") })
                assertEquals(2, sent.size)
                val secondSend = sent.single { it.city.id == 2L }
                assertEquals(latestRevision, secondSend.revision)
                assertEquals(latest, secondSend.city)
                assertFalse(sent.any { it.revision == oldRevision })
                assertTrue(store.pendingCityChanges(uid).isEmpty())
            } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
        }
    }

    @Test fun malformedProfileListenerDoesNotCrashOrReuseOldAuthoritativeCity() = runBlocking {
        withTimeout(30_000) {
            val ref = db.collection("users").document(uid)
            val repository = UserProfileRepository { db }
            try {
                ref.set(UserProfile(uid = uid, name = "Owner", defaultCity = "Paris")).await()
                repository.startObserving(uid)
                repository.serverDefaultCity.first { it?.uid == uid && it.name == "Paris" }
                ref.set(mapOf("name" to listOf("invalid-text")), SetOptions.merge()).await()
                val error = repository.readState.first { it is ProfileReadState.Failed } as ProfileReadState.Failed
                assertEquals(uid, error.uid)
                assertEquals(PROFILE_DATA_ERROR, error.message)
                assertNull(repository.profile.value)
                assertNull(repository.serverDefaultCity.value)
                assertEquals(listOf("invalid-text"), ref.get(com.google.firebase.firestore.Source.SERVER).await().get("name"))
                ref.set(mapOf("name" to "Owner"), SetOptions.merge()).await()
                val ready = repository.readState.first { it is ProfileReadState.Ready } as ProfileReadState.Ready
                assertEquals(uid, ready.profile.uid)
                repository.serverDefaultCity.first { it?.uid == uid && it.name == "Paris" }
                val deniedUid = "other-" + UUID.randomUUID()
                repository.startObserving(deniedUid)
                val denied = repository.readState.first { it is ProfileReadState.Failed && it.uid == deniedUid }
                assertEquals(deniedUid, denied.uid)
                assertEquals(PROFILE_ACCESS_ERROR, (denied as ProfileReadState.Failed).message)
                assertNull(repository.profile.value)
                assertNull(repository.serverDefaultCity.value)
            } finally { repository.stopObserving() }
        }
    }

    @Test fun partialProfileNormalizesPathOwnerAndExplicitRefreshReplacesListener() = runBlocking {
        withTimeout(30_000) {
            val ref = db.collection("users").document(uid)
            ref.set(mapOf("name" to "Partial", "defaultCity" to "Paris")).await()
            val connections = java.util.concurrent.atomic.AtomicInteger()
            val repository = UserProfileRepository { connections.incrementAndGet(); db }
            try {
                repository.startObserving(uid)
                val ready = repository.readState.first { it is ProfileReadState.Ready } as ProfileReadState.Ready
                assertEquals(uid, ready.profile.uid)
                assertEquals("Partial", ready.profile.name)
                assertNull(ref.get(com.google.firebase.firestore.Source.SERVER).await().get("uid"))
                assertEquals(1, connections.get())
                repository.startObserving(uid)
                assertEquals(1, connections.get())
                repository.startObserving(uid, forceRefresh = true)
                assertEquals(2, connections.get())
                repository.serverDefaultCity.first { it?.uid == uid && it.name == "Paris" }
                ref.set(mapOf("uid" to "wrong-owner"), SetOptions.merge()).await()
                repository.readState.first { it is ProfileReadState.Failed }
                assertNull(repository.profile.value)
                assertNull(repository.serverDefaultCity.value)
                assertEquals("wrong-owner", ref.get(com.google.firebase.firestore.Source.SERVER).await().get("uid"))
            } finally { repository.stopObserving() }
            assertEquals(ProfileReadState.Idle, repository.readState.value)
        }
    }
}
