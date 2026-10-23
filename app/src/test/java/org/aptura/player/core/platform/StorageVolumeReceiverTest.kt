package org.aptura.player.core.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.aptura.player.core.diagnostics.ApturaDiagnostics
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The storage-eject receiver reacts to system broadcasts only. It used to be exported without a permission, so any
 * installed app could poke it, and it logged whatever action/path it was handed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageVolumeReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        try {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
        } catch (_: Throwable) {}
    }

    @Test
    fun receiver_isNotExported_inTheMergedManifest() {
        val info = context.packageManager.getReceiverInfo(ComponentName(context, StorageVolumeReceiver::class.java), 0)

        assertFalse("another app could send this receiver an intent", info.exported)
    }

    @Test
    fun aBroadcastWithAnUnrelatedAction_isIgnored_andNeverLogged() {
        val spoof = Intent("com.evil.SPOOFED_EJECT_7731").setData(Uri.parse("file:///storage/secret-path-7731"))

        StorageVolumeReceiver().onReceive(context, spoof)

        val diagnostics = ApturaDiagnostics.generateDiagnosticMarkdown(context)
        assertFalse(diagnostics.contains("SPOOFED_EJECT_7731"))
        assertFalse(diagnostics.contains("secret-path-7731"))
    }

    @Test
    fun aValidBroadcast_redactsRawPath_beforeLogging() {
        val validMount = Intent(Intent.ACTION_MEDIA_MOUNTED)
            .setData(Uri.parse("file:///storage/emulated/0/DCIM/SecretFolder/movie.mp4"))

        StorageVolumeReceiver().onReceive(context, validMount)

        val diagnostics = ApturaDiagnostics.generateDiagnosticMarkdown(context)
        assertFalse(diagnostics.contains("SecretFolder"))
        assertFalse(diagnostics.contains("/storage/emulated/0/DCIM"))
    }
}
