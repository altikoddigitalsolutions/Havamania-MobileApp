package com.havamania

import android.app.Application
import com.havamania.BuildConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope

class MainApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    android.util.Log.d("HavamaniaApp", "🚀 Havamania starting...")

    // 1. Firebase must be first
    try {
        com.google.firebase.FirebaseApp.initializeApp(this)
    } catch (e: Exception) {
        android.util.Log.e("HavamaniaApp", "Firebase init failed", e)
    }

    // Schedule background tasks in scope
    MainScope().launch {
        // Schedule daily travel weather analysis
        TravelNotificationWorker.schedule(this@MainApplication)
    }
  }
}
