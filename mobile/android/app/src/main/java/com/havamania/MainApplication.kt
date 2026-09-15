package com.havamania

import android.app.Application
import com.havamania.BuildConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope

class MainApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
        android.util.Log.d("HavamaniaApp", "🚀 Havamania starting...")
    }

    // Configure Crashlytics collection policy (auto-init handled by FirebaseInitProvider)
    try {
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e("HavamaniaApp", "Crashlytics init failed", e)
        }
    }

    // Schedule background tasks in scope
    MainScope().launch {
        // Schedule daily travel weather analysis
        TravelNotificationWorker.schedule(this@MainApplication)
    }
  }
}
