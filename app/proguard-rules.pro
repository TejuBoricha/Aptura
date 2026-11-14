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
-dontwarn coil3.**
-dontwarn androidx.room.paging.**

# FFmpeg audio extension (LEG-02). Its five Java classes are vendored from AndroidX Media into this app
# (app/src/main/java/androidx/media3/decoder/ffmpeg), so the consumer rules the old prebuilt AAR carried are ours to write.
# Without them a release build still works and silently loses the extension: R8 renames what the native library finds by
# name, FfmpegLibrary.isAvailable() turns false, nothing crashes, and audio the device cannot decode simply stops playing.
# VerifyShippedContents (app/build.gradle.kts) reads the R8 mapping and fails the release build if this happens.
#
# ExoPlayer creates the renderer by reflection (DefaultRenderersFactory.Class.forName), which R8 cannot see. Media3's own rules
# keep only the constructor of a class R8 may already have removed, so the class is kept here.
-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer {
    <init>(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}
# The native methods are found by JNI from their Java class and method names (Java_androidx_media3_decoder_ffmpeg_...).
-keepclasseswithmembernames class androidx.media3.decoder.ffmpeg.** {
    native <methods>;
}
# JNI_OnLoad looks this class and method up by name and calls it back with a SimpleDecoderOutputBuffer.
-keep, includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
    private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int);
}

# Coroutines: Main dispatcher is resolved through ServiceLoader.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Strip logging in release builds to eliminate log overhead and prevent leaking framework/playback internals (SEC-12).
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
