package org.aptura.player.data.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafTreeScannerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun computeSyntheticId_returnsDeterministic63BitNegativeValues_withoutCollisions() {
        val scanner = SafTreeScanner(context)
        val uriA = "content://com.android.externalstorage.documents/tree/primary%3AMovies/document/primary%3AMovies%2Fep1.mkv"
        val uriB = "content://com.android.externalstorage.documents/tree/primary%3AMovies/document/primary%3AMovies%2Fep2.mkv"

        val idA1 = scanner.computeSyntheticId(uriA)
        val idA2 = scanner.computeSyntheticId(uriA)
        val idB = scanner.computeSyntheticId(uriB)

        assertEquals(idA1, idA2)
        assertNotEquals(idA1, idB)
        assertTrue("Synthetic SAF IDs must be strictly negative to avoid MediaStore ID collisions", idA1 < 0L)
        assertTrue("Synthetic SAF IDs must be strictly negative to avoid MediaStore ID collisions", idB < 0L)
        assertTrue("Synthetic SAF IDs should use the 63-bit range beyond 32-bit Int.MIN_VALUE", idA1 < Int.MIN_VALUE.toLong() || idB < Int.MIN_VALUE.toLong())
    }
}
