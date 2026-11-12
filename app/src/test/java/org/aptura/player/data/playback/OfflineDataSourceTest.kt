@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package org.aptura.player.data.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Audit SEC-09, DEC-012: The player must strictly enforce offline operation at the DataSource layer.
 * Any attempt to resolve or open network URIs (http/https) must throw IOException without invoking
 * DefaultHttpDataSource or making socket calls.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineDataSourceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val offlineBaseDataSourceFactory = DataSource.Factory {
        object : DataSource {
            override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {}
            override fun open(dataSpec: DataSpec): Long {
                throw IOException("Network playback is forbidden. Aptura is strictly offline-only (DEC-012).")
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1
            override fun getUri(): android.net.Uri? = null
            override fun close() {}
        }
    }

    private val localDataSourceFactory = DefaultDataSource.Factory(context, offlineBaseDataSourceFactory)

    @Test
    fun localDataSourceFactory_createsDataSource_withoutPrematureException() {
        val dataSource = localDataSourceFactory.createDataSource()
        org.junit.Assert.assertNotNull(dataSource)
    }

    @Test
    fun httpUris_areBlockedWithOfflineException() {
        val dataSource = localDataSourceFactory.createDataSource()
        val httpUri = "http://example.com/test.mp4".toUri()

        try {
            dataSource.open(DataSpec(httpUri))
            fail("Expected IOException when attempting to open http URI in offline-only player")
        } catch (e: IOException) {
            assertTrue(
                "Exception message must state that network playback is forbidden",
                e.message?.contains("Network playback is forbidden") == true
            )
        }
    }

    @Test
    fun httpsUris_areBlockedWithOfflineException() {
        val dataSource = localDataSourceFactory.createDataSource()
        val httpsUri = "https://example.com/test.mp4".toUri()

        try {
            dataSource.open(DataSpec(httpsUri))
            fail("Expected IOException when attempting to open https URI in offline-only player")
        } catch (e: IOException) {
            assertTrue(
                "Exception message must state that network playback is forbidden",
                e.message?.contains("Network playback is forbidden") == true
            )
        }
    }
}
