# Aptura Video Player ProGuard & R8 Optimization Rules (B-03)
# Optimization, obfuscation, and shrinking rules for release builds.

# Media3 & ExoPlayer keep rules
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# Room Persistence Library keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines internal dispatchers
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Coil image decoding & pipelines
-keep class coil.** { *; }
-dontwarn coil.**
