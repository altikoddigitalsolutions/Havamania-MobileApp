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

    // Firebase/WorkManager automatic initialization belongs to the default process.
    // Secondary processes must not schedule work through its uninitialized instance.
    val processName = if (android.os.Build.VERSION.SDK_INT >= 28) {
        Application.getProcessName()
    } else {
        val manager = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        manager.runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() }?.processName
    }
    if (processName != packageName) return

    // Auth can change while no Activity is visible. Remove the previous account's
    // visible notifications from the system tray as well as resetting UI state.
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    var notificationOwner = auth.currentUser?.uid
    auth.addAuthStateListener { currentAuth ->
        val nextOwner = currentAuth.currentUser?.uid
        if (nextOwner != notificationOwner) {
            notificationOwner = nextOwner
            (getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancelAll()
        }
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
