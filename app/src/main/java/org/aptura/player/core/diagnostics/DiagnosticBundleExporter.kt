package org.aptura.player.core.diagnostics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Diagnostic bundle exporter.
 * Source of truth: spec/14_ANALYTICS_AND_SUPPORT.md §4
 * Prepares the user-previewable, redacted diagnostic bundle for SCR-015 share flow.
 */
object DiagnosticBundleExporter {

    private val json = Json { prettyPrint = true }

    @Serializable
    data class AppContext(
        val appVersion: String = "1.0.0",
        val buildType: String = "debug",
        val buildHash: String = "aptura-b0-dev"
    )

    @Serializable
    data class DeviceContext(
        val manufacturer: String,
        val model: String,
        val apiLevel: Int,
        val abi: String,
        val locale: String
    )

    @Serializable
    data class DiagnosticBundle(
        val appContext: AppContext,
        val deviceContext: DeviceContext,
        val errorHistory: List<DiagnosticEvent>,
        val eventLogExcerpt: List<DiagnosticEvent>,
        val settingsSnapshot: Map<String, String>,
        val exportTimestampMs: Long = System.currentTimeMillis()
    )

    fun createBundle(
        ringBuffer: DiagnosticRingBuffer,
        deviceContext: DeviceContext,
        appContext: AppContext = AppContext(),
        settings: Map<String, String> = emptyMap(),
        includeFileName: Boolean = false
    ): DiagnosticBundle {
        val allRedacted = ringBuffer.getRedactedEvents(includeFileName)
        val errors = allRedacted.filter { it.code == EventCode.EV_011 || it.code == EventCode.EV_006 }
        val recentEvents = allRedacted.takeLast(50)

        return DiagnosticBundle(
            appContext = appContext,
            deviceContext = deviceContext,
            errorHistory = errors,
            eventLogExcerpt = recentEvents,
            settingsSnapshot = settings
        )
    }

    fun exportToJson(bundle: DiagnosticBundle): String {
        return json.encodeToString(DiagnosticBundle.serializer(), bundle)
    }

    fun exportToMarkdown(bundle: DiagnosticBundle): String {
        val sb = StringBuilder()
        sb.appendLine("# Aptura Diagnostic Report")
        sb.appendLine("> Generated on device for user review. No personal media or unredacted paths included.")
        sb.appendLine()
        sb.appendLine("## 1. App Context")
        sb.appendLine("- **Version:** ${bundle.appContext.appVersion}")
        sb.appendLine("- **Build Type:** ${bundle.appContext.buildType} (${bundle.appContext.buildHash})")
        sb.appendLine()
        sb.appendLine("## 2. Device Context")
        sb.appendLine("- **Device:** ${bundle.deviceContext.manufacturer} ${bundle.deviceContext.model}")
        sb.appendLine("- **Android API:** ${bundle.deviceContext.apiLevel} | ABI: ${bundle.deviceContext.abi}")
        sb.appendLine("- **Locale:** ${bundle.deviceContext.locale}")
        sb.appendLine()
        sb.appendLine("## 3. Error History (${bundle.errorHistory.size} recorded)")
        if (bundle.errorHistory.isEmpty()) {
            sb.appendLine("No errors logged in recent sessions.")
        } else {
            for (err in bundle.errorHistory) {
                sb.appendLine("- **[${err.code.name}]** Timestamp: ${err.timestampMs} | Payload: ${err.payload}")
            }
        }
        sb.appendLine()
        sb.appendLine("## 4. Recent Event Log (${bundle.eventLogExcerpt.size} events)")
        for (evt in bundle.eventLogExcerpt) {
            sb.appendLine("- `${evt.code.eventName}` (${evt.code.category}) [Session: ${evt.sessionId}]: ${evt.payload}")
        }
        sb.appendLine()
        sb.appendLine("## 5. Non-Sensitive Settings Snapshot")
        if (bundle.settingsSnapshot.isEmpty()) {
            sb.appendLine("Default player settings active.")
        } else {
            for ((k, v) in bundle.settingsSnapshot) {
                sb.appendLine("- `$k`: `$v`")
            }
        }
        return sb.toString()
    }
}
