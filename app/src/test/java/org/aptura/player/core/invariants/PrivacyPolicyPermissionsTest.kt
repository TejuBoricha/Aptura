package org.aptura.player.core.invariants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit SEC-04: `docs/PRIVACY.md` listed four permissions and said `ACCESS_NETWORK_STATE` was not requested, while the
 * release APK held eight (found with `aapt2`). The privacy policy is a promise to the person installing the app, so it
 * is held to the same merged manifest the invariants use. A library that adds a permission already fails
 * [MergedManifestInvariantsTest] until it is approved; this test makes the same approval step require the policy text.
 */
class PrivacyPolicyPermissionsTest {

    private fun policy(): String {
        val file = listOf("docs/PRIVACY.md", "../docs/PRIVACY.md").map(::File).firstOrNull { it.isFile }
        assertTrue("docs/PRIVACY.md was not found from ${File(".").absolutePath}", file != null)
        return file!!.readText(Charsets.UTF_8)
    }

    private fun shortName(permission: String) = permission.substringAfterLast('.')

    private val shippedNames by lazy {
        MergedManifest.parse(InvariantFiles.releaseManifest).permissions.keys.map(::shortName).toSet()
    }

    /** The permission names in the first column of the "Permissions" table. */
    private fun tableNames(doc: String): Set<String> {
        val section = doc.substringAfter("## Permissions", "").substringBefore("\n## ")
        return Regex("^\\|\\s*`([A-Z_]+)`", RegexOption.MULTILINE).findAll(section).map { it.groupValues[1] }.toSet()
    }

    /** The permissions the policy says it does NOT request. */
    private fun claimedAbsent(doc: String): Set<String> {
        val afterMarker = doc.substringAfter("does **not** request", "").lines().dropWhile { !it.startsWith("- ") }
        val bullets = afterMarker.takeWhile { it.startsWith("- ") }
        return bullets.mapNotNull { Regex("^- `(?:android\\.permission\\.)?([A-Z_]+)`").find(it)?.groupValues?.get(1) }.toSet()
    }

    /** Everything wrong between the policy text and the permissions the app holds. */
    private fun problems(doc: String, shipped: Set<String>): List<String> = buildList {
        val inTable = tableNames(doc)
        val absent = claimedAbsent(doc)
        (shipped - inTable).sorted().forEach {
            // The signature-level permission is explained in a sentence under the table, not in it.
            if (!doc.contains("`$it`")) add("$it is held by the app but the policy does not mention it")
        }
        (shipped intersect absent).sorted().forEach { add("$it is held by the app but the policy says it is not requested") }
        (inTable - shipped).sorted().forEach { add("$it is in the policy's table but the app does not request it") }
    }

    // ---- the real thing --------------------------------------------------------------------------------------------

    @Test
    fun thePolicy_matchesEveryPermissionTheShippedAppHolds() {
        val found = problems(policy(), shippedNames)

        assertTrue("docs/PRIVACY.md disagrees with the merged release manifest:\n" + found.joinToString("\n") { " - $it" }, found.isEmpty())
    }

    @Test
    fun thePolicyParser_findsTheTableAndTheNotRequestedList() {
        // Guards the guard: a heading rename that made both lists empty would turn the test above into a no-op.
        val doc = policy()

        assertTrue("table rows were not found: ${tableNames(doc)}", tableNames(doc).containsAll(setOf("READ_MEDIA_VIDEO", "WAKE_LOCK")))
        assertTrue("not-requested list was not found: ${claimedAbsent(doc)}", "INTERNET" in claimedAbsent(doc))
    }

    // ---- fixtures: each rule must fire on the text that was wrong ---------------------------------------------------

    private val approved = ManifestInvariants.approvedPermissions("org.aptura.player").keys.map(::shortName).toSet()

    private val textBeforeSec04 = """
        ## Permissions

        | Permission | Why |
        |---|---|
        | `READ_MEDIA_VIDEO` (Android 13+) | Read videos. |
        | `READ_EXTERNAL_STORAGE` (Android 12 and lower) | The same on older releases. |
        | `FOREGROUND_SERVICE` | Keep playback alive. |
        | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Foreground service type. |

        Aptura does **not** request:

        - `android.permission.INTERNET`
        - `android.permission.ACCESS_NETWORK_STATE`
        - `android.permission.MANAGE_EXTERNAL_STORAGE`

        ## Next section
    """.trimIndent()

    @Test
    fun theTextBeforeSec04_isFlaggedForEachMistakeItHad() {
        val found = problems(textBeforeSec04, approved)

        assertTrue("WAKE_LOCK was missing from the table: $found", "WAKE_LOCK is held by the app but the policy does not mention it" in found)
        assertTrue(
            "RECEIVE_BOOT_COMPLETED was missing from the table: $found",
            "RECEIVE_BOOT_COMPLETED is held by the app but the policy does not mention it" in found
        )
        assertTrue(
            "ACCESS_NETWORK_STATE was claimed absent: $found",
            "ACCESS_NETWORK_STATE is held by the app but the policy says it is not requested" in found
        )
    }

    @Test
    fun aRowForAPermissionTheAppNoLongerHolds_isFlagged() {
        val withStaleRow = textBeforeSec04.replace(
            "| `FOREGROUND_SERVICE` |",
            "| `READ_CONTACTS` | A row nobody removed. |\n| `FOREGROUND_SERVICE` |"
        )

        assertTrue(problems(withStaleRow, approved).contains("READ_CONTACTS is in the policy's table but the app does not request it"))
    }

    @Test
    fun aPolicyThatMatches_hasNoProblems() {
        val full = buildString {
            appendLine("## Permissions")
            appendLine()
            appendLine("| Permission | Why |")
            appendLine("|---|---|")
            (approved - "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION").sorted().forEach { appendLine("| `$it` | reason |") }
            appendLine()
            appendLine("The release build also holds one permission only Aptura can use (`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`).")
            appendLine()
            appendLine("Aptura does **not** request:")
            appendLine()
            appendLine("- `android.permission.INTERNET`")
        }

        assertEquals(emptyList<String>(), problems(full, approved))
    }
}
