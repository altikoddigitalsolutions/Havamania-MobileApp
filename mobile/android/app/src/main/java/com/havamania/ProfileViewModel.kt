package com.havamania

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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

    // RCA FIX: Standard initialization. We will log the bucket name to verify.
    private val storage = FirebaseStorage.getInstance()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _uploadProgress = MutableStateFlow(false)
    val uploadProgress: StateFlow<Boolean> = _uploadProgress

    private val _avatarVersion = MutableStateFlow(System.currentTimeMillis())
    val avatarVersion: StateFlow<Long> = _avatarVersion

    private val userProfileRepository = UserProfileRepository.getInstance()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user == null) {
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
        userProfileRepository.startObserving(uid)

        viewModelScope.launch {
            userProfileRepository.profile.collect { profile ->
                if (profile != null) {
                    _profileState.value = ProfileState.Success(profile)
                    syncDataStoreFromProfile(profile)
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
        }
    }

    private fun createNewProfile(uid: String) {
        val authName = auth.currentUser?.displayName ?: ""
        val newProfile = UserProfile(
            uid = uid,
            email = auth.currentUser?.email ?: "",
            name = authName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).set(newProfile).await()
            } catch (e: Exception) {
                Log.e("ProfileVM", "Failed to create new profile", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        // We don't stopObserving in repository here because other ViewModels might still need it.
        // It's a singleton.
    }

    fun fetchProfile() {
        val uid = auth.currentUser?.uid ?: return
        observeProfile(uid)
    }

    private fun syncDataStoreFromProfile(profile: UserProfile) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            ThemeManager.saveUserName(context, profile.uid, profile.name)
            ThemeManager.saveUserBio(context, profile.uid, profile.bio)
            ThemeManager.saveUserAboutMe(context, profile.uid, profile.aboutMe)
            ThemeManager.saveAssistantTone(context, profile.uid, try { AssistantTone.valueOf(profile.assistantTone.uppercase()) } catch(e: Exception) { AssistantTone.DENGELI })
            ThemeManager.savePersonalizationEnabled(context, profile.uid, profile.personalizationEnabled)
            ThemeManager.saveOnboardingCompleted(context, profile.uid, profile.onboardingCompleted)
            ThemeManager.saveUserImageUriByUid(context, profile.uid, profile.photoURL)

            profile.personalizationProfile?.let {
                ThemeManager.saveUserInterests(context, profile.uid, it.selectedInterests.toSet())
            }
        }
    }

    fun updateProfile(name: String, bio: String) {
        val trimmedName = name.trim()
        val trimmedBio = bio.trim()

        if (trimmedName.isEmpty()) {
            _profileState.value = ProfileState.Error("İsim boş olamaz.")
            return
        }

        val finalName = if (trimmedName.length > 50) trimmedName.take(50) else trimmedName
        val finalBio = if (trimmedBio.length > 500) trimmedBio.take(500) else trimmedBio

        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "name" to finalName,
                    "bio" to finalBio,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(uid).update(updates).await()

                auth.currentUser?.updateProfile(com.google.firebase.auth.userProfileChangeRequest {
                    displayName = finalName
                })?.await()

                // Explicit local sync
                val context = getApplication<Application>()
                ThemeManager.saveUserName(context, uid, finalName)
                ThemeManager.saveUserBio(context, uid, finalBio)

            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Update profile failed", e)
                _profileState.value = ProfileState.Error("Profil güncellenemedi. Lütfen tekrar deneyin.")
            }
        }
    }

    fun updatePersonalization(interests: List<String>, travelStyles: List<String>, weatherPrefs: WeatherPreferences) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val personalization = PersonalizationProfile(
                    uid = uid,
                    selectedInterests = interests,
                    travelStyles = travelStyles,
                    weatherPreferences = weatherPrefs
                )

                val updates = mapOf(
                    "personalizationProfile" to personalization,
                    "onboardingCompleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                )

                db.collection("users").document(uid).update(updates).await()

                val context = getApplication<Application>()
                ThemeManager.saveUserInterests(context, uid, interests.toSet())
                ThemeManager.saveOnboardingCompleted(context, uid, true)
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Update personalization failed", e)
                _profileState.value = ProfileState.Error("Tercihler güncellenemedi.")
            }
        }
    }

    fun setUserAboutMe(aboutMe: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).update("aboutMe", aboutMe, "updatedAt", System.currentTimeMillis()).await()
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Update aboutMe failed", e)
                _profileState.value = ProfileState.Error("Bilgiler güncellenemedi.")
            }
        }
    }

    fun toggleInterest(interestId: String, currentInterests: Set<String>) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val newList = currentInterests.toMutableList()
                if (newList.contains(interestId)) newList.remove(interestId)
                else newList.add(interestId)

                db.collection("users").document(uid).update(
                    "personalizationProfile.selectedInterests", newList,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Toggle interest failed", e)
                _profileState.value = ProfileState.Error("İlgi alanı güncellenemedi.")
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.util.Log.e("PHOTO_DEBUG", "[PHOTO] Step 2 FAILED: No authenticated user")
            return
        }
        val uid = currentUser.uid

        viewModelScope.launch {
            android.util.Log.i("PHOTO_DEBUG", "--- UPLOAD TRACE START ---")
            android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 2 OK: Uploading URI = $uri")

            _uploadProgress.value = true
            try {
                val context = getApplication<Application>()

                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 2.1: Checking URI accessibility...")
                context.contentResolver.openInputStream(uri)?.use { it.close() }
                    ?: throw Exception("URI access failed")

                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 2.2: Processing image...")
                val bitmap = processImage(uri) ?: throw Exception("Görsel işlenemedi.")

                val baos = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                val data = baos.toByteArray()

                // Path must match rules 'profile-images/{userId}/{allPaths=**}'
                val storagePath = "profile-images/$uid/avatar.jpg"
                val storageRef = storage.reference.child(storagePath)

                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 3: PRE-UPLOAD DATA:")
                android.util.Log.i("PHOTO_DEBUG", ">> Bucket: ${storageRef.bucket}")
                android.util.Log.i("PHOTO_DEBUG", ">> Path: $storagePath")
                android.util.Log.i("PHOTO_DEBUG", ">> Size: ${data.size} bytes")

                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()

                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 3.1: storageRef.putBytes() starting...")

                storageRef.putBytes(data, metadata).await()
                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 3 OK: Upload Success")

                val downloadUrl = storageRef.downloadUrl.await().toString()
                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 4 OK: Download URL = $downloadUrl")

                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 5: Saving imageUrl = $downloadUrl")
                db.collection("users").document(uid).update("photoURL", downloadUrl, "updatedAt", System.currentTimeMillis()).await()
                android.util.Log.i("PHOTO_DEBUG", "[PHOTO] Step 6 OK: Firestore Updated")

                _avatarVersion.value = System.currentTimeMillis()

                // CRITICAL SYNC: Update local DataStore immediately to prevent UI flicker
                ThemeManager.saveUserImageUriByUid(context, uid, downloadUrl)

                android.util.Log.i("PHOTO_DEBUG", "--- UPLOAD TRACE SUCCESS ---")
            } catch (e: Exception) {
                android.util.Log.e("PHOTO_DEBUG", "--- UPLOAD TRACE FAILED ---")
                if (e is com.google.firebase.storage.StorageException) {
                    android.util.Log.e("PHOTO_DEBUG", "StorageException Error: ${e.errorCode}, HTTP: ${e.httpResultCode}")
                    android.util.Log.e("PHOTO_DEBUG", "Message: ${e.message}")
                    e.cause?.let { android.util.Log.e("PHOTO_DEBUG", "Cause: ${it.message}") }
                }
                android.util.Log.e("PHOTO_DEBUG", "Exception:", e)
                _profileState.value = ProfileState.Error("Profil fotoğrafı yüklenemedi: ${e.message}")
            } finally {
                _uploadProgress.value = false
            }
        }
    }

    fun removeProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uploadProgress.value = true
            try {
                val context = getApplication<Application>()
                try {
                    storage.reference.child("profile-images/$uid/avatar.jpg").delete().await()
                } catch (e: Exception) { /* ignore */ }

                db.collection("users").document(uid).update("photoURL", null, "updatedAt", System.currentTimeMillis()).await()
                ThemeManager.saveUserImageUriByUid(context, uid, null)

                _avatarVersion.value = System.currentTimeMillis()
            } catch (e: Exception) {
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
            while (options.outWidth / scale / 2 >= 512 && options.outHeight / scale / 2 >= 512) {
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
                Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)
            } else {
                rotatedBitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("PHOTO_DEBUG", "Error processing image", e)
            null
        } finally {
            try { inputStream?.close() } catch (e: Exception) {}
        }
    }
}
