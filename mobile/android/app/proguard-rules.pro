# ==============================================================================
# ProGuard / R8 Configuration for Havamania (Production / Google Play)
# ==============================================================================

# ------------------------------------------------------------------------------
# General & Attributes
# ------------------------------------------------------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod, Deprecated
-keepattributes SourceFile, LineNumberTable
-keepattributes *Annotation*
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ------------------------------------------------------------------------------
# Havamania App Models, DTOs & State
# ------------------------------------------------------------------------------
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

-keep class com.havamania.**Dto** { *; }
-keep class com.havamania.**Model** { *; }
-keep class com.havamania.**State** { *; }
-keep class com.havamania.**Response** { *; }
-keep class com.havamania.**Entity** { *; }
-keep class com.havamania.UserProfile { *; }

# ------------------------------------------------------------------------------
# Kotlinx Serialization
# ------------------------------------------------------------------------------
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer $serializer;
}
-keep class kotlinx.serialization.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keep class com.havamania.**$$serializer { *; }
-keepclasseswithmembers class com.havamania.** {
    @kotlinx.serialization.Serializable <fields>;
}

# ------------------------------------------------------------------------------
# AndroidX WorkManager
# Reflection is used to instantiate workers by Class name.
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.havamania.WeatherNotificationWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.havamania.TravelNotificationWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ------------------------------------------------------------------------------
# AndroidX Room
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.havamania.**_Impl { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}

# ------------------------------------------------------------------------------
# Retrofit 2, OkHttp 3, Okio
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.**

# ------------------------------------------------------------------------------
# MapLibre GL Native SDK (JNI interface preservation)
# ------------------------------------------------------------------------------
-keep class org.maplibre.android.** { *; }
-keep interface org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# ------------------------------------------------------------------------------
# Firebase (Auth, Firestore, Storage, Crashlytics) & Play Services
# ------------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# Coil Image Loader
# ------------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ------------------------------------------------------------------------------
# AndroidX DataStore
# ------------------------------------------------------------------------------
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ------------------------------------------------------------------------------
# Kotlin Coroutines
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
