package org.aptura.player

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.data.scanner.MediaScanScheduler
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.domain.model.DomainMediaItem
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Plays a real video, which nothing in this repository did before.
 *
 * Every other test either runs on the JVM, where no codec exists, or opens the app and looks at the screen. So a build
 * in which no video started still passed the whole gate; the first person to find out was the owner, on a phone. These
 * tests put a four-second H.264 and AAC clip into MediaStore and require the player to be playing it, with the position
 * moving, both when the player is driven directly and when the clip is picked in the library like a person would.
 *
 * When the player does not start, the failure message says what the player was doing (state, play-when-ready,
 * suppression reason, error, position) instead of only "timed out", because the next question is always why.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackSmokeTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var clipUri: Uri? = null

    private class SeedRule : TestRule {
        override fun apply(base: Statement, description: Description) = object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.grantRuntimePermission(
                    instrumentation.targetContext.packageName,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
                runBlocking { ApturaSettingsRepository(instrumentation.targetContext).setOnboardingCompleted(true) }
                base.evaluate()
            }
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(SeedRule()).around(composeRule)

    @Before
    fun putClipInMediaStore() {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, CLIP_NAME)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AppturaSmoke")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused to create the test clip")
        resolver.openOutputStream(uri)!!.use { out ->
            InstrumentationRegistry.getInstrumentation().context.assets.open("smoke_clip.mp4").use { it.copyTo(out) }
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
        clipUri = uri
    }

    @After
    fun removeClip() {
        runCatching { PlayerManager.activeController?.player?.let { p -> onMain { p.stop() } } }
        clipUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
    }

    @Test
    fun theController_playsAVideo() {
        val item = DomainMediaItem(
            uri = clipUri.toString(),
            title = "Smoke clip",
            durationMs = 4_000L,
            sizeBytes = 44_000L,
            mimeType = "video/mp4"
        )
        val controller = PlayerManager.getOrCreate(context)
        runBlocking(Dispatchers.Main) { controller.playQueue(listOf(item), 0) }

        assertPlays(controller.player, "the controller, one item")
    }

    @Test
    fun aVideoPickedInTheLibrary_plays() {
        MediaScanScheduler.scheduleImmediateScan(context)

        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasText("smoke", substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasText("smoke", substring = true, ignoreCase = true))[0].performClick()

        val controller = PlayerManager.getOrCreate(context)
        assertPlays(controller.player, "a tap on the card in the library")
    }

    /** Waits for the player to be playing with the position moving, and fails with what the player was doing. */
    private fun assertPlays(player: Player, how: String) {
        val deadline = System.currentTimeMillis() + PLAY_TIMEOUT_MS
        var last = ""
        while (System.currentTimeMillis() < deadline) {
            val snapshot = onMain { Snapshot.of(player) }
            last = snapshot.describe()
            if (snapshot.isPlaying && snapshot.positionMs >= MIN_POSITION_MS) return
            Thread.sleep(200)
        }
        fail("No video started playing through $how within ${PLAY_TIMEOUT_MS / 1000} s. The player was: $last")
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private data class Snapshot(
        val state: Int,
        val playWhenReady: Boolean,
        val isPlaying: Boolean,
        val suppression: Int,
        val positionMs: Long,
        val items: Int,
        val index: Int,
        val error: String?
    ) {
        fun describe() = "state=${stateName(state)} playWhenReady=$playWhenReady isPlaying=$isPlaying " +
            "suppression=$suppression position=${positionMs}ms item=${index + 1}/$items error=${error ?: "none"}"

        companion object {
            fun of(player: Player) = Snapshot(
                state = player.playbackState,
                playWhenReady = player.playWhenReady,
                isPlaying = player.isPlaying,
                suppression = player.playbackSuppressionReason,
                positionMs = player.currentPosition,
                items = player.mediaItemCount,
                index = player.currentMediaItemIndex,
                error = player.playerError?.let { "${it.errorCodeName}: ${it.message}" }
            )

            private fun stateName(state: Int) = when (state) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> state.toString()
            }
        }
    }

    private companion object {
        const val CLIP_NAME = "smoke_clip.mp4"
        const val PLAY_TIMEOUT_MS = 20_000L
        const val MIN_POSITION_MS = 700L
    }
}
