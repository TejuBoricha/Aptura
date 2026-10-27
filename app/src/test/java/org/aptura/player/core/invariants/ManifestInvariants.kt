package org.aptura.player.core.invariants

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * What the installed app really declares: the MERGED manifest (our manifest plus every library's), release variant.
 * The old tests read `src/main/AndroidManifest.xml`, which showed 4 of the 8 permissions the shipped app requests and
 * none of the components libraries add.
 */
class MergedManifest(
    val packageName: String,
    /** permission name -> maxSdkVersion (null when unbounded) */
    val permissions: Map<String, Int?>,
    /** Every `<uses-feature>` the store will filter on, with its required flag (absent means required). */
    val features: List<Feature>,
    val components: List<Component>,
    /** Categories declared by component intent filters, i.e. the form factors the app advertises itself for. */
    val intentCategories: List<String>,
    val application: Map<String, String>,
    val queriedPackages: List<String>
) {
    data class Feature(val name: String, val required: Boolean)

    data class Component(
        val kind: String,
        val name: String,
        val exported: Boolean,
        val permission: String?,
        val supportsPictureInPicture: Boolean = false,
        /** Provider authorities only; empty for every other component kind. */
        val authorities: List<String> = emptyList()
    )

    companion object {
        private const val ANDROID = "http://schemas.android.com/apk/res/android"

        fun parse(xml: String): MergedManifest {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val root = factory.newDocumentBuilder().parse(InputSource(StringReader(xml))).documentElement

            fun Element.android(name: String): String? = getAttributeNS(ANDROID, name).takeIf { it.isNotEmpty() }
            fun Element.children(tag: String): List<Element> =
                (0 until childNodes.length).map { childNodes.item(it) }
                    .filter { it.nodeType == Node.ELEMENT_NODE && it.nodeName == tag }
                    .map { it as Element }

            val permissions = (root.children("uses-permission") + root.children("uses-permission-sdk-23"))
                .associate { it.android("name")!! to it.android("maxSdkVersion")?.toInt() }

            val features = root.children("uses-feature").map {
                // Absent `required` means true in the platform's own manifest grammar.
                Feature(it.android("name")!!, it.android("required")?.toBooleanStrictOrNull() ?: true)
            }

            val application = root.children("application").single()
            val componentKinds = listOf("activity", "activity-alias", "service", "receiver", "provider")
            val components = componentKinds.flatMap { kind ->
                application.children(kind).map { element ->
                    val declared = element.android("exported")
                    Component(
                        kind = kind,
                        name = element.android("name")!!,
                        // Without an explicit value a component is exported exactly when it has an intent filter
                        // (providers never are by default).
                        exported = declared?.toBooleanStrictOrNull() ?: (kind != "provider" && element.children("intent-filter").isNotEmpty()),
                        permission = element.android("permission"),
                        supportsPictureInPicture = element.android("supportsPictureInPicture")?.toBooleanStrictOrNull() ?: false,
                        authorities = element.android("authorities")
                            ?.split(';')
                            ?.map(String::trim)
                            ?.filter(String::isNotBlank)
                            .orEmpty()
                    )
                }
            }
            val categories = componentKinds.flatMap { kind ->
                application.children(kind).flatMap { it.children("intent-filter") }
            }.flatMap { it.children("category") }.mapNotNull { it.android("name") }
            val attributes = (0 until application.attributes.length)
                .map { application.attributes.item(it) }
                .filter { it.namespaceURI == ANDROID }
                .associate { it.localName to it.nodeValue }

            val queried = root.children("queries").flatMap { it.children("package") }.mapNotNull { it.android("name") }
            return MergedManifest(
                root.getAttribute("package"),
                permissions,
                features,
                components,
                categories,
                attributes,
                queried
            )
        }

        fun parse(file: File) = parse(file.readText())
    }
}

object ManifestInvariants {

    /** Every permission the shipped app may request, and why. Anything else fails until a person has reviewed it. */
    fun approvedPermissions(packageName: String): Map<String, String> = mapOf(
        "android.permission.READ_MEDIA_VIDEO" to "scoped video access on Android 13+ (DEC-011)",
        "android.permission.READ_EXTERNAL_STORAGE" to "video access before Android 13; must stay capped with maxSdkVersion 32",
        "android.permission.FOREGROUND_SERVICE" to "background playback service",
        "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" to "background playback service type (API 34+)",
        "android.permission.WAKE_LOCK" to "added by WorkManager / Media3 to keep a scan or playback running",
        "android.permission.ACCESS_NETWORK_STATE" to "added by WorkManager for its network constraints; reads connectivity, cannot connect",
        "android.permission.RECEIVE_BOOT_COMPLETED" to "added by WorkManager so the library scan is rescheduled after a reboot",
        "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" to "AndroidX's own signature permission that keeps dynamic receivers private"
    )

    /** Components other than the launcher that are reachable by other apps, with the system permission that gates each. */
    val approvedExportedComponents: Map<String, String?> = mapOf(
        "org.aptura.player.MainActivity" to null, // launcher + "Open with" for video files
        "androidx.work.impl.background.systemjob.SystemJobService" to "android.permission.BIND_JOB_SERVICE",
        "androidx.work.impl.diagnostics.DiagnosticsReceiver" to "android.permission.DUMP",
        "androidx.profileinstaller.ProfileInstallReceiver" to "android.permission.DUMP"
    )

    /**
     * Features the app may advertise, and why. An absent entry is a failure, so a library cannot quietly narrow the
     * devices the store offers Aptura to. All approved features must be optional.
     */
    val approvedOptionalFeatures: Map<String, String> = mapOf(
        "android.software.picture_in_picture" to "floating playback window (FR-CTRL-012); optional so devices without PiP can still install (F-07)"
    )

    /** Prefix match so camera.front, telephony.gsm, location.gps, bluetooth_le, nfc.hce are all caught. */
    private val neverAllowedFeaturePrefixes = listOf(
        "android.hardware.camera" to "Aptura never opens a camera (DEC-005)",
        "android.hardware.telephony" to "Aptura is a video player, not a phone",
        "android.hardware.location" to "no location permission is requested (zero telemetry)",
        "android.hardware.microphone" to "playback only; nothing records audio",
        "android.hardware.bluetooth" to "routing is the platform's job; playback must not require Bluetooth",
        "android.hardware.nfc" to "no tag or payment flow",
        "android.software.leanback" to "TV is out of R1 scope (DEC-005)",
        "android.hardware.type.television" to "TV is out of R1 scope (DEC-005)",
        "android.hardware.type.automotive" to "Android Auto and Automotive are out of R1 scope (DEC-005)",
        "android.hardware.type.watch" to "Wear is out of R1 scope (DEC-005)"
    )

    /** The only categories R1 needs: the launcher and "Open with" for video files. */
    val approvedIntentCategories: Set<String> = setOf(
        "android.intent.category.DEFAULT",
        "android.intent.category.LAUNCHER",
        "android.intent.category.BROWSABLE"
    )

    const val PIP_FEATURE = "android.software.picture_in_picture"

    /** Always forbidden, regardless of any allow-list entry: these would contradict the product's promises. */
    private val neverAllowed = mapOf(
        "android.permission.INTERNET" to "Aptura is offline-only (DEC-012)",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "all-files access is never requested (DEC-011)",
        "android.permission.POST_NOTIFICATIONS" to "playback notifications are platform-exempt (FR-PLAT-002)",
        "android.permission.READ_MEDIA_AUDIO" to "Aptura is a video player",
        "android.permission.QUERY_ALL_PACKAGES" to "no inventory of installed apps"
    )

    fun violations(manifest: MergedManifest): List<String> = buildList {
        neverAllowed.forEach { (permission, why) ->
            if (permission in manifest.permissions) add("forbidden permission $permission: $why")
        }

        val approved = approvedPermissions(manifest.packageName)
        (manifest.permissions.keys - approved.keys).sorted().forEach {
            add("unapproved permission $it (a library or a change added it; review it, then add it to ManifestInvariants with the reason)")
        }
        val cap = manifest.permissions["android.permission.READ_EXTERNAL_STORAGE"]
        if ("android.permission.READ_EXTERNAL_STORAGE" in manifest.permissions && (cap == null || cap > 32)) {
            add("READ_EXTERNAL_STORAGE must be capped with maxSdkVersion=32 (was $cap): Android 13+ uses READ_MEDIA_VIDEO only")
        }

        manifest.features.forEach { feature ->
            if (feature.name in approvedOptionalFeatures) {
                if (feature.required) {
                    add(
                        "${feature.name} must be declared required=\"false\": requiring it would hide Aptura from " +
                            "devices without the feature instead of adapting to them (F-07)"
                    )
                }
            } else {
                val forbidden = neverAllowedFeaturePrefixes.firstOrNull { feature.name.startsWith(it.first) }
                if (forbidden != null) add("forbidden feature ${feature.name}: ${forbidden.second}")
                else add("unapproved feature ${feature.name} (review it, then add it to ManifestInvariants with the reason)")
            }
        }

        // The activity flag is what makes PiP possible at runtime; the feature is what tells the store to keep the
        // app installable without PiP. Shipping one without the other is the F-07 failure in either direction.
        val pipActivities = manifest.components.filter { it.kind == "activity" && it.supportsPictureInPicture }
        val pipDeclared = manifest.features.any { it.name == PIP_FEATURE }
        if (pipActivities.isNotEmpty() && !pipDeclared) {
            add(
                "${pipActivities.joinToString { it.name }} declares android:supportsPictureInPicture but the manifest " +
                    "does not declare $PIP_FEATURE as optional (F-07)"
            )
        }
        if (pipDeclared && pipActivities.isEmpty()) {
            add("$PIP_FEATURE is declared but no activity sets android:supportsPictureInPicture=\"true\"")
        }

        manifest.intentCategories.filterNot { it in approvedIntentCategories }.distinct().sorted().forEach {
            add("unapproved intent category $it (TV, Auto and desktop form factors are out of R1 scope, DEC-005)")
        }

        val exported = manifest.components.filter { it.exported }
        (exported.map { it.name }.toSet() - approvedExportedComponents.keys).sorted().forEach {
            add("unapproved exported component $it (other apps can reach it)")
        }
        exported.filter { it.name in approvedExportedComponents }.forEach { component ->
            val expected = approvedExportedComponents.getValue(component.name)
            if (component.permission != expected) {
                add("exported component ${component.name} must be protected by $expected but is protected by ${component.permission}")
            }
        }
        manifest.components.filter { it.kind == "provider" && it.exported }.forEach {
            add("exported content provider ${it.name}")
        }

        val app = manifest.application
        if (app["allowBackup"] != "false") add("android:allowBackup must be \"false\" (was ${app["allowBackup"]})")
        if (app["debuggable"] == "true") add("the release build is debuggable")
        if (app["testOnly"] == "true") add("the release build is marked testOnly")
        if (app["usesCleartextTraffic"] == "true") add("android:usesCleartextTraffic=\"true\"")
        if ("networkSecurityConfig" in app) add("a networkSecurityConfig implies network traffic")
        if ("requestLegacyExternalStorage" in app) add("requestLegacyExternalStorage reopens broad storage access")
        if (manifest.queriedPackages.isNotEmpty()) add("<queries> names specific packages: ${manifest.queriedPackages}")
    }
}
