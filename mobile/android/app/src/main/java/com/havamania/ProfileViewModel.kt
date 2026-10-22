package com.havamania

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import com.havamania.ui.theme.AssistantTone
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _activeUid = MutableStateFlow(auth.currentUser?.uid)
    internal val activeUid: StateFlow<String?> = _activeUid
    private val accountTasks = AccountTaskScope(viewModelScope) { auth.currentUser?.uid ?: "legacy" }
    private val saveController = ProfileSaveController()
    internal val saveState = saveController.state
    internal fun resetSaveState() = saveController.reset()
    private val personalizationSave = PersonalizationSaveController(viewModelScope, { auth.currentUser?.uid ?: "legacy" })
    internal val personalizationSaveState = personalizationSave.state
    internal fun resetPersonalizationSave() = personalizationSave.reset()
    private val _aboutMeSaveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    internal val aboutMeSaveState: StateFlow<ProfileSaveState> = _aboutMeSaveState
    private val aboutMeRunner = AssistantTaskRunner(viewModelScope, { auth.currentUser?.uid ?: "legacy" }, {}, 20_000)
    internal fun resetAboutMeSave() { aboutMeRunner.reset(); _aboutMeSaveState.value = ProfileSaveState.Idle }

    // RCA FIX: Standard initialization. We will log the bucket name to verify.
    private val storage = FirebaseStorage.getInstance()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _uploadProgress = MutableStateFlow(false)
    val uploadProgress: StateFlow<Boolean> = _uploadProgress

    private val _avatarVersion = MutableStateFlow(System.currentTimeMillis())
    val avatarVersion: StateFlow<Long> = _avatarVersion

    private val userProfileRepository = UserProfileRepository.getInstance()
    private var profileJob: kotlinx.coroutines.Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        accountTasks.reset()
        saveController.reset()
        personalizationSave.reset()
        resetAboutMeSave()
        _uploadProgress.value = false
        val user = firebaseAuth.currentUser
        _activeUid.value = user?.uid
        if (user == null) {
            profileJob?.cancel()
            userProfileRepository.stopObserving()
            _profileState.value = ProfileState.Idle
        } else {
            observeProfile(user.uid)
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: return
        observeProfile(uid)
    }

    private fun observeProfile(uid: String) {
        profileJob?.cancel()
        _profileState.value = userProfileRepository.profile.value
            ?.takeIf { it.uid == uid }?.let { ProfileState.Success(it) } ?: ProfileState.Loading
        userProfileRepository.startObserving(uid)

        profileJob = accountTasks.launch launch@ { ownerUid ->
            try {
            userProfileRepository.profile.collect { profile ->
                if (auth.currentUser?.uid != uid) return@collect
                if (profile != null && profile.uid == uid) {
                    _profileState.value = ProfileState.Success(profile)
                    ThemeManager.saveProfileSnapshot(getApplication(), profile)
                } else {
                    _profileState.value = ProfileState.Loading
                    // Handled if we need to create it
                    val user = auth.currentUser
                    if (user != null) {
                        // Check once if it actually doesn't exist to avoid redundant checks
                        db.collection("users").document(user.uid).get().await().also {
                            if (!it.exists()) createNewProfile(user.uid)
                        }
                    }
                }
            }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _profileState.value = ProfileState.Error("Profil yüklenemedi. Lütfen bağlantınızı kontrol edip tekrar deneyin.")
            }
        }
    }

    private fun createNewProfile(uid: String) {
        if (auth.currentUser?.uid != uid) return
        val authName = auth.currentUser?.displayName ?: ""
        val newProfile = UserProfile(
            uid = uid,
            email = auth.currentUser?.email ?: "",
            name = authName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        accountTasks.launch launch@ { ownerUid ->
            try {
                db.collection("users").document(uid).set(newProfile).await()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                    Log.e("ProfileVM", "Failed to create new profile", e)
}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        personalizationSave.reset()
        resetAboutMeSave()
        // We don't stopObserving in repository here because other ViewModels might still need it.
        // It's a singleton.
    }

    fun fetchProfile() {
        val uid = auth.currentUser?.uid ?: return
        observeProfile(uid)
    }

    fun updateProfile(name: String, bio: String) {
        val user = auth.currentUser ?: return
        accountTasks.launch { ownerUid ->
            if (user.uid != ownerUid) return@launch
            saveController.save(name, bio) { finalName, finalBio ->
                val updates = mapOf(
                    "name" to finalName,
                    "bio" to finalBio,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(ownerUid).update(updates).await()
                // Keep the Firebase user object captured before the first suspension.
                user.updateProfile(com.google.firebase.auth.userProfileChangeRequest {
                    displayName = finalName
                }).await()
                val context = getApplication<Application>()
                ThemeManager.saveUserName(context, ownerUid, finalName)
                ThemeManager.saveUserBio(context, ownerUid, finalBio)
            }
        }
    }

    fun updatePersonalization(interests: List<String>, travelStyles: List<String>, weatherPrefs: WeatherPreferences) {
        if (auth.currentUser == null) return
        personalizationSave.save { owner ->
            val personalization = PersonalizationProfile(
                uid = owner, selectedInterests = interests.toList(),
                travelStyles = travelStyles.toList(), weatherPreferences = weatherPrefs)
            db.collection("users").document(owner).update(mapOf(
                "personalizationProfile" to personalization,
                "onboardingCompleted" to true,
                "updatedAt" to System.currentTimeMillis()
            )).await()
            val context = getApplication<Application>()
            ThemeManager.saveUserInterests(context, owner, interests.toSet())
            ThemeManager.saveOnboardingCompleted(context, owner, true)
        }
    }

    fun setUserAboutMe(aboutMe: String) {
        if (auth.currentUser == null) return
        aboutMeRunner.start(
            onStart = { _aboutMeSaveState.value = ProfileSaveState.Saving },
            operation = { owner ->
                db.collection("users").document(owner).update("aboutMe", aboutMe.trim(), "updatedAt", System.currentTimeMillis()).await()
                ThemeManager.saveUserAboutMe(getApplication(), owner, aboutMe.trim())
            },
            onSuccess = { _aboutMeSaveState.value = ProfileSaveState.Saved },
            onError = { _aboutMeSaveState.value = ProfileSaveState.Failed("Kayıt onayı alınamadı. Metnin korundu; tekrar deneyebilirsin.") }
        )
    }

    fun toggleInterest(interestId: String, currentInterests: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        accountTasks.launch launch@ { ownerUid ->
            try {
                val newList = currentInterests.toMutableList()
                if (newList.contains(interestId)) newList.remove(interestId)
                else newList.add(interestId)

                db.collection("users").document(uid).update(
                    "personalizationProfile.selectedInterests", newList,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                    android.util.Log.e("ProfileVM", "Toggle interest failed", e)
}
                _profileState.value = ProfileState.Error("İlgi alanı güncellenemedi.")
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
if (BuildConfig.DEBUG) {
                android.util.Log.e("PHOTO_DEBUG", "[PHOTO] Step 2 FAILED: No authenticated user")
}
            return
        }
        val uid = currentUser.uid

        accountTasks.launch launch@ { ownerUid ->
            if (BuildConfig.DEBUG) {
                android.util.Log.i("PHOTO_DEBUG", "--- UPLOAD TRACE START ---")
                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 2 OK: Uploading URI = $uri")
            }

            _uploadProgress.value = true
            try {
                val context = getApplication<Application>()

                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 2.1: Checking URI accessibility...")
                }
                context.contentResolver.openInputStream(uri)?.use { it.close() }
                    ?: throw Exception("URI access failed")

                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 2.2: Processing image...")
                }
                val bitmap = processImage(uri) ?: throw Exception("Görsel işlenemedi.")

                val data = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    java.io.ByteArrayOutputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
                        output.toByteArray()
                    }
                }

                // Path must match rules 'profile-images/{userId}/{allPaths=**}'
                val storagePath = "profile-images/$uid/avatar.jpg"
                val storageRef = storage.reference.child(storagePath)

                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 3: PRE-UPLOAD DATA:")
                    android.util.Log.i("PHOTO_DEBUG", ">> Bucket: ${storageRef.bucket}")
                    android.util.Log.i("PHOTO_DEBUG", ">> Path: $storagePath")
if (BuildConfig.DEBUG) {
                        android.util.Log.i("PHOTO_DEBUG", ">> Size: ${data.size} bytes")
}
                }

                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()

                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 3.1: storageRef.putBytes() starting...")
                }

                storageRef.putBytes(data, metadata).await()
                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 3 OK: Upload Success")
                }

                val downloadUrl = storageRef.downloadUrl.await().toString()
                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 4 OK: Download URL = $downloadUrl")

                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 5: Saving imageUrl = $downloadUrl")
                }
                db.collection("users").document(uid).update("photoURL", downloadUrl, "updatedAt", System.currentTimeMillis()).await()
                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 6 OK: Firestore Updated")
                }

                _avatarVersion.value = System.currentTimeMillis()

                // CRITICAL SYNC: Update local DataStore immediately to prevent UI flicker
                ThemeManager.saveUserImageUriByUid(context, uid, downloadUrl)

                if (BuildConfig.DEBUG) {
                    android.util.Log.i("PHOTO_DEBUG", "--- UPLOAD TRACE SUCCESS ---")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (BuildConfig.DEBUG) {
                    android.util.Log.e("PHOTO_DEBUG", "--- UPLOAD TRACE FAILED ---")
                }
                val friendlyError = when {
                    e is com.google.firebase.storage.StorageException && e.errorCode == com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED ->
                        "Yetki hatası: Fotoğraf yükleme izniniz bulunmuyor."
                    e is com.google.firebase.storage.StorageException && e.errorCode == com.google.firebase.storage.StorageException.ERROR_QUOTA_EXCEEDED ->
                        "Sunucu kotası doldu, lütfen daha sonra tekrar deneyin."
                    e is com.google.firebase.storage.StorageException && e.httpResultCode == 402 ->
                        "Fotoğraf hizmeti şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."
                    e is java.io.IOException ->
                        "Bağlantı hatası: Lütfen internetinizi kontrol edin."
                    e.message?.contains("unknown", true) == true ->
                        "Fotoğraf yüklenemedi. Lütfen daha sonra tekrar deneyin."
                    else -> "Beklenmedik bir hata oluştu: ${e.localizedMessage ?: "Bilinmiyor"}"
                }
if (BuildConfig.DEBUG) {
                    android.util.Log.e("PHOTO_DEBUG", "Mapped Error: $friendlyError", e)
}
                _profileState.value = ProfileState.Error(friendlyError)
            } finally {
                _uploadProgress.value = false
            }
        }
    }

    fun removeProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        accountTasks.launch launch@ { ownerUid ->
            _uploadProgress.value = true
            try {
                val context = getApplication<Application>()
                try {
                    storage.reference.child("profile-images/$uid/avatar.jpg").delete().await()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                }

                db.collection("users").document(uid).update("photoURL", null, "updatedAt", System.currentTimeMillis()).await()
                ThemeManager.saveUserImageUriByUid(context, uid, null)

                _avatarVersion.value = System.currentTimeMillis()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _profileState.value = ProfileState.Error("İşlem başarısız.")
            } finally {
                _uploadProgress.value = false
            }
        }
    }

    private suspend fun processImage(uri: Uri): Bitmap? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val context = getApplication<Application>()
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return@withContext null

            val exif = try {
                ExifInterface(inputStream)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                null
            }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL

            inputStream.close()
            inputStream = context.contentResolver.openInputStream(uri)

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var scale = 1
            while (options.outWidth / scale / 2 >= 512 || options.outHeight / scale / 2 >= 512) {
                scale *= 2
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return@withContext null

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            if (rotatedBitmap.width > 512 || rotatedBitmap.height > 512) {
                val ratio = rotatedBitmap.width.toFloat() / rotatedBitmap.height.toFloat()
                val width = if (ratio > 1) 512 else (512 * ratio).toInt()
                val height = if (ratio > 1) (512 / ratio).toInt() else 512
                Bitmap.createScaledBitmap(rotatedBitmap, width.coerceAtLeast(1), height.coerceAtLeast(1), true)
            } else {
                rotatedBitmap
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                android.util.Log.e("PHOTO_DEBUG", "Error processing image", e)
}
            null
        } finally {
            try { inputStream?.close() } catch (e: Exception) {}
        }
    }
}
