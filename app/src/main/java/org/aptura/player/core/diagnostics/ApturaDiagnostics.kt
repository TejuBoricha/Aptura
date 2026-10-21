package org.aptura.player.core.diagnostics

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Global singleton managing privacy-preserving local diagnostics (spec/14, FR-DIAG-001..004).
 * Bounded ring buffer (max 500 events / 2 MB) completely on-device. Zero telemetry.
 */
object ApturaDiagnostics {

    val ringBuffer = DiagnosticRingBuffer()

    fun logEvent(code: EventCode, sessionId: String = "app_session", payload: Map<String, String> = emptyMap()) {
        val event = DiagnosticEvent(
            code = code,
            timestampMs = System.currentTimeMillis(),
            sessionId = sessionId,
            payload = payload
        )
        ringBuffer.record(event)
    }

    fun generateDiagnosticMarkdown(context: Context, settingsSnapshot: Map<String, String> = emptyMap()): String {
        val deviceContext = DiagnosticBundleExporter.DeviceContext(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            locale = Locale.getDefault().toLanguageTag()
        )

        val bundle = DiagnosticBundleExporter.createBundle(
            ringBuffer = ringBuffer,
            deviceContext = deviceContext,
            appContext = DiagnosticBundleExporter.AppContext(
                appVersion = "1.0.0",
                buildType = "debug",
                buildHash = "aptura-part63"
            ),
            settings = settingsSnapshot
        )

        return DiagnosticBundleExporter.exportToMarkdown(bundle)
    }
}
