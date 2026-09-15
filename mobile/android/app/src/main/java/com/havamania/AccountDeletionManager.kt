package com.havamania

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.util.AtomicFile
import androidx.datastore.preferences.core.edit
import androidx.work.WorkManager
import androidx.work.await
import coil.imageLoader
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.havamania.ui.theme.dataStore
import java.io.File
import java.security.SecureRandom
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AccountDeletionState(
    val pending: Boolean = false,
    val busy: Boolean = false,
    val complete: Boolean = false,
    val needsAuthentication: Boolean = false,
    val message: String? = null
)

@Serializable
internal data class DeletionJournal(val uid: String, val key: String, val remoteComplete: Boolean = false)

/** Process-owned scope: leaving Settings must not cancel an accepted deletion. */
class AccountDeletionManager private constructor(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val file = AtomicFile(File(app.noBackupFilesDir, "account-deletion.json"))
    private var journal: DeletionJournal? = null
    private val mutableState = MutableStateFlow(AccountDeletionState())
    val state = mutableState.asStateFlow()
    private val api by lazy { BackendChatbotFactory.create() }
    private val auth get() = FirebaseAuth.getInstance()

    init {
        try {
            if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                journal = Json.decodeFromString<DeletionJournal>(file.openRead().bufferedReader().use { it.readText() })
                require(journal!!.uid.isNotBlank() && journal!!.key.matches(Regex("[0-9a-f]{64}")))
                mutableState.value = AccountDeletionState(pending = true)
            }
        } catch (_: Exception) {
            journal = null
            mutableState.value = AccountDeletionState(pending = true,
                message = "Silme isteğinin cihazdaki kaydı okunamadı. Lütfen destek ile iletişime geçin.")
        }
    }

    fun start(password: String, onComplete: (Boolean, String?) -> Unit) {
        if (state.value.busy || state.value.pending) return
        mutableState.value = AccountDeletionState(busy = true)
        scope.launch {
            try {
                val availability = api.deletionAvailability()
                val available = availability.isSuccessful && availability.body()?.available == true
                availability.errorBody()?.close()
                if (!available) {
                    val message = "Hesap silme hizmeti şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."
                    mutableState.value = AccountDeletionState(message = message)
                    onComplete(false, message)
                    return@launch
                }
                val user = auth.currentUser ?: error("Oturum bulunamadı")
                val email = user.email ?: error("E-posta bulunamadı")
                user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
                val token = user.getIdToken(true).await().token ?: error("Kimlik doğrulanamadı")
                val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                val record = DeletionJournal(user.uid, key)
                saveJournal(record)
                mutableState.value = AccountDeletionState(pending = true, busy = true)
                // The recovery screen now owns progress and errors.
                send(record, token)
                onComplete(state.value.complete, state.value.message)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val message = "Silme işlemi başlatılamadı. Şifrenizi ve internet bağlantınızı kontrol edin."
                mutableState.value = state.value.copy(busy = false, message = message)
                onComplete(false, message)
            }
        }
    }

    fun retry(email: String = "", password: String = "") {
        val record = journal ?: return
        if (state.value.busy || state.value.complete) return
        mutableState.value = state.value.copy(busy = true, message = null)
        scope.launch {
            try {
                if (record.remoteComplete) {
                    finishLocally(record)
                    return@launch
                }
                var token: String? = null
                if (password.isNotEmpty()) {
                    var user = auth.currentUser
                    if (user == null) {
                        user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
                    } else {
                        user.reauthenticate(EmailAuthProvider.getCredential(user.email ?: email, password)).await()
                    }
                    if (user?.uid != record.uid) {
                        auth.signOut()
                        error("Different account")
                    }
                    token = user.getIdToken(true).await().token
                } else if (auth.currentUser?.uid == record.uid) {
                    try {
                        token = auth.currentUser?.getIdToken(false)?.await()?.token
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // An accepted request may already have disabled/deleted Auth.
                    }
                }
                send(record, token)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = state.value.copy(busy = false,
                    message = "İşlem tamamlanamadı. Bağlantınızı ve giriş bilgilerinizi kontrol edip tekrar deneyin.")
            }
        }
    }

    private suspend fun send(record: DeletionJournal, token: String?) {
        val response = api.deleteAccount(token?.let { "Bearer $it" }, AccountDeletionRequest(record.key))
        val result = deletionResult(response.code(), response.body()?.status)
        response.errorBody()?.close()
        when (result) {
            DeletionResult.COMPLETE -> {
                val completed = record.copy(remoteComplete = true)
                saveJournal(completed)
                finishLocally(completed)
            }
            DeletionResult.AUTHENTICATE -> mutableState.value = AccountDeletionState(
                pending = true, needsAuthentication = true,
                message = "İstek henüz kabul edilmedi. Silmek istediğiniz hesabın giriş bilgileriyle tekrar doğrulayın.")
            DeletionResult.PENDING -> mutableState.value = AccountDeletionState(pending = true,
                message = "Silme isteğiniz alındı. Temizlik henüz tamamlanmadı; durumu tekrar kontrol edin.")
            DeletionResult.RETRY -> mutableState.value = AccountDeletionState(pending = true,
                message = "Silme durumu doğrulanamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.")
        }
    }

    private suspend fun saveJournal(record: DeletionJournal) {
        withContext(Dispatchers.IO) {
            val output = file.startWrite()
            try {
                output.write(Json.encodeToString(record).toByteArray(Charsets.UTF_8))
                file.finishWrite(output)
            } catch (e: Exception) {
                file.failWrite(output)
                throw e
            }
        }
        journal = record
    }

    private suspend fun finishLocally(record: DeletionJournal) {
        mutableState.value = AccountDeletionState(pending = true, busy = true,
            message = "Sunucudaki silme tamamlandı. Cihazdaki hesap verileri temizleniyor.")
        auth.signOut()
        UserProfileRepository.getInstance().stopObserving()
        yield()
        cleanLocalData(record.uid)
        withContext(Dispatchers.IO) {
            file.delete()
            check(!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists())
        }
        journal = null
        mutableState.value = AccountDeletionState(pending = true, complete = true,
            message = "Hesabınız ve cihazdaki hesap verileri silindi.")
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private suspend fun cleanLocalData(uid: String) {
        WorkManager.getInstance(app).cancelUniqueWork("travel_notifications_daily").await()
        val firestore = FirebaseFirestore.getInstance()
        firestore.terminate().await()
        firestore.clearPersistence().await()
        val weather = WeatherDatabase.getDatabase(app).weatherDao()
        weather.clearAllTravelPlans(uid)
        weather.clearAllAiHistory(uid)
        weather.clearAllWeatherCache()
        WeatherRepository.getInstance(app).clearCurrentWeather()
        NotificationDatabase.getDatabase(app).notificationDao().deleteAll(uid)
        app.dataStore.edit { preferences ->
            preferences.asMap().keys.filter { it.name.startsWith("havamania:$uid:") }
                .forEach { preferences.remove(it) }
        }
        app.imageLoader.memoryCache?.clear()
        withContext(Dispatchers.IO) { app.imageLoader.diskCache?.clear() }
        (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    fun acknowledgeCompletion() {
        if (state.value.complete) {
            mutableState.value = AccountDeletionState()
            TravelNotificationWorker.schedule(app)
        }
    }

    companion object {
        @Volatile private var instance: AccountDeletionManager? = null
        fun getInstance(app: Application): AccountDeletionManager = instance ?: synchronized(this) {
            instance ?: AccountDeletionManager(app).also { instance = it }
        }
    }
}

internal enum class DeletionResult { COMPLETE, PENDING, AUTHENTICATE, RETRY }

internal fun deletionResult(code: Int, status: String?): DeletionResult = when {
    code == 200 && status == "complete" -> DeletionResult.COMPLETE
    code == 202 && status == "pending" -> DeletionResult.PENDING
    code == 401 -> DeletionResult.AUTHENTICATE
    else -> DeletionResult.RETRY
}
