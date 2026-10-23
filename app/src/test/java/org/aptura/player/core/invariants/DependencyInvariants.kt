package org.aptura.player.core.invariants

import java.io.File

/** One library in the release runtime graph and the libraries that pull it in. */
data class ResolvedLibrary(val coordinate: String, val requiredBy: Set<String>) {
    val group get() = coordinate.substringBefore(':')
    val name get() = coordinate.substringAfter(':').substringBefore(':')

    companion object {
        /** Parses the file written by the `writeReleaseRuntimeDependencies` Gradle task. */
        fun parseReport(text: String): List<ResolvedLibrary> = text.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                ResolvedLibrary(
                    coordinate = line.substringBefore(" <- ").trim(),
                    requiredBy = line.substringAfter(" <- ", "").split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                )
            }
            .toList()
    }
}

object DependencyInvariants {

    /** Group (or group:artifact) prefixes of analytics, crash-reporting, attribution and advertising SDKs (ADR-007). */
    val telemetryAndAds = listOf(
        "com.google.firebase", "com.google.android.gms", "com.google.android.datatransport", "com.google.android.ump",
        "com.google.android.libraries.ads", "com.google.ads", "io.sentry", "com.mixpanel", "com.datadoghq", "com.newrelic",
        "com.appsflyer", "com.adjust", "com.amplitude", "com.segment", "com.bugsnag", "ch.acra", "org.acra", "com.flurry",
        "com.microsoft.appcenter", "io.fabric", "com.crashlytics", "com.facebook.android", "com.onesignal", "com.braze",
        "com.instabug", "com.clevertap", "com.leanplum", "com.kochava", "com.singular", "io.branch", "com.applovin",
        "com.unity3d.ads", "com.ironsource", "com.vungle", "com.mopub"
    )

    /** HTTP / RPC client stacks: Aptura R1 has no INTERNET permission, so none belongs on the classpath without a reason. */
    val httpStacks = listOf(
        "com.squareup.okhttp3", "com.squareup.okhttp", "com.squareup.retrofit2", "com.squareup.retrofit", "io.ktor:ktor-client",
        "org.apache.httpcomponents", "com.android.volley", "com.loopj.android", "io.grpc", "com.google.api-client"
    )

    /**
     * A library that is on the classpath for a reason other than being used, accepted on purpose.
     * Each entry is checked both ways: it may only be reached through [onlyRequiredBy], and it must still exist
     * (a stale exception would silently allow it back later).
     */
    data class Exception(val coordinatePrefix: String, val onlyRequiredBy: Set<String>, val reason: String)

    // Empty on purpose. It used to hold OkHttp: Coil 2 depended on it for its HTTP image fetcher, and R8 did NOT remove it
    // (the release APK shipped 119 OkHttp classes, including OkHttpClient and RealConnection). Coil 3 without a
    // coil-network artifact has no HTTP stack, so the exception was deleted rather than documented. Add an entry here only
    // with a reason, a bounded `onlyRequiredBy`, and a passing verifyReleaseApk.
    val approvedExceptions = emptyList<Exception>()

    /** A prefix matches on a boundary, so `com.adjust` matches `com.adjust.sdk:...` but never `androidx.x:adjustable`. */
    fun matches(coordinate: String, prefix: String): Boolean {
        if (!coordinate.startsWith(prefix)) return false
        if (coordinate.length == prefix.length || prefix.last() in ":.-") return true
        return coordinate[prefix.length] in ":.-"
    }

    fun violations(libraries: List<ResolvedLibrary>, exceptions: List<Exception> = approvedExceptions): List<String> = buildList {
        for (library in libraries) {
            telemetryAndAds.firstOrNull { matches(library.coordinate, it) }?.let {
                add("telemetry/advertising SDK on the release classpath: ${library.coordinate} (required by ${library.requiredBy})")
            }
            val stack = httpStacks.firstOrNull { matches(library.coordinate, it) } ?: continue
            val exception = exceptions.firstOrNull { library.coordinate.startsWith(it.coordinatePrefix) }
            if (exception == null) {
                add("HTTP client on the release classpath: ${library.coordinate} (matches $stack; required by ${library.requiredBy})")
            } else {
                val strangers = library.requiredBy.filterNot { parent -> exception.onlyRequiredBy.any { parent.startsWith(it) } }
                if (strangers.isNotEmpty()) {
                    add("${library.coordinate} is only excused when required by ${exception.onlyRequiredBy}, but is also required by $strangers")
                }
            }
        }
    }

    /** Exceptions that no longer match anything: remove them so they cannot quietly excuse a future dependency. */
    fun staleExceptions(libraries: List<ResolvedLibrary>, exceptions: List<Exception> = approvedExceptions): List<String> =
        exceptions.filter { exception -> libraries.none { it.coordinate.startsWith(exception.coordinatePrefix) } }
            .map { "approved exception ${it.coordinatePrefix} is no longer on the classpath; delete it" }

    /** Plugins that add telemetry at build time without necessarily appearing as a library. */
    val telemetryPluginIds = listOf(
        "com.google.gms.google-services", "com.google.firebase.crashlytics", "com.google.firebase.firebase-perf",
        "io.sentry.android.gradle", "com.datadoghq.dd-sdk-android-gradle-plugin", "com.bugsnag.android.gradle",
        "com.newrelic.agent.android", "com.appsflyer.af-gradle-plugin"
    )

    fun telemetryPluginsIn(files: List<File>): List<String> = files.filter { it.exists() }.flatMap { file ->
        val text = file.readText()
        telemetryPluginIds.filter { text.contains(it) }.map { "${file.name} references the build plugin $it" }
    }
}
