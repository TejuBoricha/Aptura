package org.aptura.player.core.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.aptura.player.R
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Global entry point for privacy-preserving local diagnostics (spec/14, FR-DIAG-001..004). Zero telemetry.
 *
 * Events go to a bounded in-memory ring buffer and — once [initialize] has run — also to a bounded on-device file, so
 * they survive the process. A crash handler writes a crash report before the process dies. Nothing leaves the device
 * unless the user exports it.
 */
object ApturaDiagnostics {

    val ringBuffer = DiagnosticRingBuffer()

    /** Rotating per-process session id (spec/14 §2): stitches one run's events together; not a device identifier. */
    @Volatile
    var currentSessionId: String = "app_session"
        private set

    @Volatile
    private var store: DiagnosticStore? = null

    @Volatile
    private var persistence: Executor = Executor { it.run() }

    @Volatile
    private var cachedApp: DiagnosticBundleExporter.AppContext? = null

    private val backgroundWriter: Executor by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "aptura-diagnostics").apply { isDaemon = true }
        }
    }

    /**
     * Starts a new session: restores the previous sessions' events, installs the crash handler and records the start.
     * Safe to call again (the newest call wins), which tests rely on.
     */
    fun initialize(
        context: Context,
        diagnosticStore: DiagnosticStore = DiagnosticStore(File(context.applicationContext.filesDir, "diagnostics")),
        executor: Executor = backgroundWriter,
        installCrashHandler: Boolean = true,
        previousExit: String? = previousProcessExit(context)
    ) {
        store = diagnosticStore
        persistence = executor
        cachedApp = null
        currentSessionId = newSessionId()

        val restored = diagnosticStore.loadEvents()
        restored.forEach { ringBuffer.record(it) }

        if (installCrashHandler) {
            val existing = Thread.getDefaultUncaughtExceptionHandler()
            // Re-initialising must not stack a handler on top of our own previous one.
            val delegate = (existing as? CrashHandler)?.delegate ?: existing
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(
                    store = diagnosticStore,
                    ringBuffer = ringBuffer,
                    sessionId = { currentSessionId },
                    app = { appContext(context) },
                    device = { deviceContext() },
                    delegate = delegate
                )
            )
        }

        val app = appContext(context)
        logEvent(
            EventCode.EV_001,
            payload = buildMap {
                put("version", app.appVersion)
                put("buildType", app.buildType)
                put("build", app.buildHash)
                put("restoredEvents", restored.size.toString())
                previousExit?.let { put("previousExit", it) }
            }
        )
    }

    fun logEvent(code: EventCode, sessionId: String = currentSessionId, payload: Map<String, String> = emptyMap()) {
        val event = DiagnosticEvent(
            code = code,
            timestampMs = System.currentTimeMillis(),
            sessionId = sessionId,
            payload = payload
        )
        ringBuffer.record(event)
        store?.let { target -> persistence.execute { target.append(event) } }
    }

    /** The newest crash the user has not been told about yet, if any (spec/14 §2 post-crash prompt). */
    fun pendingCrashNotice(): DiagnosticStore.StoredCrash? =
        store?.loadCrashes()?.firstOrNull { !it.acknowledged }

    /** The user dismissed or acted on the crash prompt: keep the report, stop asking. */
    fun acknowledgeCrash(name: String) {
        store?.let { target -> persistence.execute { target.acknowledgeCrash(name) } }
    }

    /** "Delete diagnostic data": memory and disk, immediately (spec/14 §2). */
    fun clearAll() {
        ringBuffer.clear()
        store?.let { target -> persistence.execute { target.clear() } }
    }

    fun generateDiagnosticMarkdown(context: Context, settingsSnapshot: Map<String, String> = emptyMap()): String {
        val bundle = DiagnosticBundleExporter.createBundle(
            ringBuffer = ringBuffer,
            deviceContext = deviceContext(),
            appContext = appContext(context),
            settings = settingsSnapshot,
            crashReports = store?.loadCrashes()?.map { it.text }.orEmpty()
        )
        return DiagnosticBundleExporter.exportToMarkdown(bundle)
    }

    /** What the app really is, read from the installed package — not typed in. */
    fun appContext(context: Context): DiagnosticBundleExporter.AppContext {
        cachedApp?.let { return it }
        val info = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Throwable) {
            null
        }
        @Suppress("DEPRECATION")
        val code = info?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong() }
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val hash = runCatching { context.getString(R.string.aptura_build_hash) }.getOrNull().orEmpty().ifBlank { "unknown" }
        return DiagnosticBundleExporter.AppContext(
            appVersion = "${info?.versionName ?: "unknown"} (${code ?: "?"})",
            buildType = if (debuggable) "debug" else "release",
            buildHash = hash
        ).also { cachedApp = it }
    }

    private fun deviceContext() = DiagnosticBundleExporter.DeviceContext(
        manufacturer = Build.MANUFACTURER ?: "Unknown",
        model = Build.MODEL ?: "Unknown",
        apiLevel = Build.VERSION.SDK_INT,
        abi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "unknown",
        locale = Locale.getDefault().toLanguageTag()
    )

    private fun newSessionId(): String =
        "s" + System.currentTimeMillis().toString(36) + Random.nextInt(36 * 36 * 36).toString(36).padStart(3, '0')

    /** Why the previous process ended (Android 11+): the crashes and kills a Java handler can never see (ANR, native, memory). */
    private fun previousProcessExit(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val manager = context.getSystemService(ActivityManager::class.java) ?: return null
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()?.reason?.let(::exitReasonName)
        } catch (_: Throwable) {
            null
        }
    }

    private fun exitReasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "native_crash"
        ApplicationExitInfo.REASON_ANR -> "anr"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "low_memory"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive_resource_usage"
        ApplicationExitInfo.REASON_SIGNALED -> "signaled"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "user_requested"
        ApplicationExitInfo.REASON_USER_STOPPED -> "user_stopped"
        ApplicationExitInfo.REASON_EXIT_SELF -> "exit_self"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization_failure"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission_change"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency_died"
        else -> "other"
    }
}
