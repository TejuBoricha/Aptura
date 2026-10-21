# Aptura Video Player ProGuard & R8 rules (B-03)
#
# Philosophy: Media3, Room, WorkManager, Coil and kotlinx.serialization all ship their own
# *consumer* keep rules inside their AARs, so the app file stays minimal. Blanket
# `-keep class androidx.media3.** { *; }` rules were removed on purpose: they would stop R8
# from shrinking ~all of Media3 and defeat the point of enabling minification (B-02).

# Keep line numbers in crash stack traces surfaced through Play pre-launch / vitals reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Optional / compile-time-only references inside the playback and imaging stacks.
-dontwarn androidx.media3.**
-dontwarn coil.**
-dontwarn androidx.room.paging.**

# Coroutines: Main dispatcher is resolved through ServiceLoader.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
