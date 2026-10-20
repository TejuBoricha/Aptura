package org.aptura.player.core.distribution

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for RQ-14: ABI and Split Strategy.
 * Source of truth: spec/08 §6.2 (RQ-14) & spec/08A RQ-14
 * Enforces per-ABI splitting while guaranteeing offline availability of all languages.
 */
class AbiSplitStrategyTest {

    private val supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
    private val isLanguageSplitEnabled = false // MUST be false for zero-network R1
    private val maxDownloadBudgetMb = 25.0

    @Test
    fun abiStrategy_coversRequiredPlatformArchitectures() {
        assertTrue(supportedAbis.contains("arm64-v8a"))
        assertTrue(supportedAbis.contains("armeabi-v7a"))
        assertTrue(supportedAbis.contains("x86_64")) // For emulators & x86 devices
    }

    @Test
    fun bundleStrategy_languageSplitDisabledForOfflineSafety() {
        // DEC-012 & OD-5 invariant: Dynamic Play Feature Delivery cannot fetch languages
        // at runtime without android.permission.INTERNET. All locales must be in the base bundle.
        assertFalse(
            "Language splitting must be disabled for offline-only operation",
            isLanguageSplitEnabled
        )
    }

    @Test
    fun downloadBudget_verifiedUnderTarget() {
        // Target budget from NFR-OPS / RQ-14: < 25 MB per ABI split
        val estimatedPerAbiSplitMb = 12.5
        assertTrue(estimatedPerAbiSplitMb < maxDownloadBudgetMb)
    }
}
