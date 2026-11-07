package org.aptura.player

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * Audit SEC-07: the manifest's intent filter only admits `content` and `file`, but a filter is not a check. Another app can
 * send an explicit intent to [MainActivity], which skips the filter, and the activity handed any `data` on to the player
 * under the app's own identity. [MainActivity.acceptedViewUri] now accepts a `content` URI, or a `file` URI that is not
 * inside the app's private storage, and nothing else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewIntentUriTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val dataDir: File by lazy { temp.newFolder("data").canonicalFile }
    private val privateDirs get() = listOf(dataDir)

    private fun view(uri: String) = Intent(Intent.ACTION_VIEW).setData(Uri.parse(uri))

    private fun accepted(uri: String): String? = MainActivity.acceptedViewUri(view(uri), privateDirs)

    // ---- what is let through ---------------------------------------------------------------------------------------

    @Test
    fun aContentUri_isAccepted() {
        assertEquals("content://media/external/video/media/42", accepted("content://media/external/video/media/42"))
    }

    @Test
    fun aFileUriInSharedStorage_isAccepted() {
        assertEquals("file:///storage/emulated/0/Movies/My%20Movie.mp4", accepted("file:///storage/emulated/0/Movies/My%20Movie.mp4"))
    }

    @Test
    fun theSchemeIsMatchedWithoutRegardToCase() {
        assertEquals("CONTENT://media/external/video/media/1", accepted("CONTENT://media/external/video/media/1"))
        assertEquals("FILE:///storage/emulated/0/a.mp4", accepted("FILE:///storage/emulated/0/a.mp4"))
    }

    @Test
    fun aFileNextToThePrivateFolderWithTheSamePrefix_isAccepted() {
        // "<tmp>/data2" starts with "<tmp>/data" as text but is not inside it.
        val sibling = File(dataDir.parentFile, "data2/video.mp4")
        assertEquals(Uri.fromFile(sibling).toString(), accepted(Uri.fromFile(sibling).toString()))
    }

    // ---- what is refused -------------------------------------------------------------------------------------------

    @Test
    fun otherSchemes_areRefused() {
        listOf(
            "https://example.com/a.mp4",
            "http://example.com/a.mp4",
            "ftp://example.com/a.mp4",
            "android.resource://org.aptura.player/raw/x",
            "intent://scan/#Intent;scheme=zxing;end",
            "javascript:alert(1)"
        ).forEach { assertNull("$it must be refused", accepted(it)) }
    }

    @Test
    fun aFileInsideThePrivateFolder_isRefused() {
        val db = File(dataDir, "databases/aptura.db")
        assertNull(accepted(Uri.fromFile(db).toString()))
        assertNull("the folder itself", accepted(Uri.fromFile(dataDir).toString()))
    }

    @Test
    fun aPathThatClimbsIntoThePrivateFolder_isRefused() {
        val sneaky = "file://" + dataDir.parentFile!!.path + "/elsewhere/../data/shared_prefs/settings.xml"
        assertNull(accepted(sneaky))
    }

    @Test
    fun aSymbolicLinkIntoThePrivateFolder_isRefused() {
        val outside = temp.newFolder("outside")
        val target = File(dataDir, "files/secret.bin").apply { parentFile!!.mkdirs(); writeText("x") }
        val link = File(outside, "movie.mp4")
        Files.createSymbolicLink(link.toPath(), target.toPath())

        assertNull("a link resolves to the private file", accepted(Uri.fromFile(link).toString()))
    }

    @Test
    fun aFileUriWithNoPath_isRefused() {
        assertNull(accepted("file://"))
    }

    // ---- what does not change --------------------------------------------------------------------------------------

    @Test
    fun anIntentThatIsNotAView_hasNoLaunchUri() {
        assertNull(MainActivity.acceptedViewUri(Intent(Intent.ACTION_MAIN), privateDirs))
        assertNull(MainActivity.acceptedViewUri(Intent(Intent.ACTION_SEND).setData(Uri.parse("content://media/external/video/media/1")), privateDirs))
        assertNull(MainActivity.acceptedViewUri(null, privateDirs))
        assertNull("a VIEW with no data", MainActivity.acceptedViewUri(Intent(Intent.ACTION_VIEW), privateDirs))
    }
}
