package org.aptura.player.core.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.database.BookmarkEntity
import org.aptura.player.data.database.MediaItemEntity
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApturaBackupCoordinatorTest {

    private lateinit var context: Context
    private lateinit var db: ApturaDatabase
    private lateinit var settingsRepo: ApturaSettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ApturaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsRepo = ApturaSettingsRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun defaultBackupFileName_followsExpectedPattern() {
        val fileName = ApturaBackupCoordinator.defaultBackupFileName()
        assertTrue(fileName.startsWith("aptura-backup-"))
        assertTrue(fileName.endsWith(".json"))
    }

    @Test
    fun createBackupJson_serializesSettingsMediaAndBookmarks() = runBlocking {
        val settings = ApturaSettings(
            themeMode = ThemeMode.DARK,
            accentColor = AccentColor.BLUE,
            defaultPlaybackSpeed = 1.5f,
            seekDeltaSeconds = 15
        )
        val mediaItems = listOf(
            MediaItemEntity(
                mediaStoreId = 1L,
                uri = "content://media/external/video/media/1",
                displayName = "Episode1.mp4",
                title = "Episode 1",
                durationMs = 60000L,
                sizeBytes = 1000L,
                mimeType = "video/mp4",
                playbackPositionMs = 45000L,
                isCompleted = false,
                lastPlayedTimestamp = 1000L
            ),
            MediaItemEntity(
                mediaStoreId = 2L,
                uri = "content://media/external/video/media/2",
                displayName = "Untouched.mp4",
                title = "Untouched",
                durationMs = 60000L,
                sizeBytes = 1000L,
                mimeType = "video/mp4",
                playbackPositionMs = 0L,
                isCompleted = false
            )
        )
        val bookmarks = listOf(
            BookmarkEntity(
                mediaUri = "content://media/external/video/media/1",
                timestampMs = 30000L,
                note = "Important scene",
                createdAtSec = 500L
            )
        )

        val jsonString = ApturaBackupCoordinator.createBackupJson(settings, mediaItems, bookmarks)
        val json = JSONObject(jsonString)

        assertEquals("aptura_backup", json.getString("format"))
        assertEquals(1, json.getInt("version"))

        val settingsObj = json.getJSONObject("settings")
        assertEquals("DARK", settingsObj.getString("themeMode"))
        assertEquals("BLUE", settingsObj.getString("accentColor"))
        assertEquals(1.5, settingsObj.getDouble("defaultPlaybackSpeed"), 0.01)

        val itemsArray = json.getJSONArray("mediaItems")
        assertEquals(1, itemsArray.length()) // Untouched item without user state is filtered out
        val item0 = itemsArray.getJSONObject(0)
        assertEquals("content://media/external/video/media/1", item0.getString("uri"))
        assertEquals(45000L, item0.getLong("playbackPositionMs"))

        val bookmarksArray = json.getJSONArray("bookmarks")
        assertEquals(1, bookmarksArray.length())
        val bm0 = bookmarksArray.getJSONObject(0)
        assertEquals("Important scene", bm0.getString("note"))
    }

    @Test
    fun exportAndRestore_restoresSettingsAndDatabaseState() = runBlocking {
        val settings = ApturaSettings(
            themeMode = ThemeMode.LIGHT,
            accentColor = AccentColor.CYAN,
            defaultPlaybackSpeed = 1.25f,
            seekDeltaSeconds = 20,
            swapSwipeGestures = true,
            volumeGestureEnabled = false,
            brightnessGestureEnabled = false,
            swipeToSeekEnabled = false,
            doubleTapToSeekEnabled = false,
            holdFor2xSpeedEnabled = false,
            matchVideoOrientation = false
        )
        val media = listOf(
            MediaItemEntity(
                mediaStoreId = 1L,
                uri = "content://media/test/1",
                displayName = "Video1.mp4",
                title = "Video 1",
                durationMs = 120000L,
                sizeBytes = 2000L,
                mimeType = "video/mp4",
                playbackPositionMs = 99000L,
                isCompleted = true,
                playbackSpeed = 1.25f,
                customTitle = "My Custom Title"
            )
        )
        val bookmarks = listOf(
            BookmarkEntity(
                mediaUri = "content://media/test/1",
                timestampMs = 15000L,
                note = "Test Bookmark",
                createdAtSec = 1234L
            )
        )

        val out = ByteArrayOutputStream()
        ApturaBackupCoordinator.exportToStream(out, settings, media, bookmarks)

        val input = ByteArrayInputStream(out.toByteArray())
        val summary = ApturaBackupCoordinator.restoreFromStream(input, settingsRepo, db)

        assertEquals(1, summary.videosRestored)
        assertEquals(1, summary.bookmarksRestored)
        assertTrue(summary.settingsRestored)

        // Verify DB media item restored
        val restoredVideo = db.mediaItemDao().getVideoByUri("content://media/test/1")
        assertNotNull(restoredVideo)
        assertEquals(99000L, restoredVideo!!.playbackPositionMs)
        assertTrue(restoredVideo.isCompleted)
        assertEquals("My Custom Title", restoredVideo.customTitle)

        // Verify DB bookmark restored
        val restoredBookmarks = db.bookmarkDao().getAllBookmarksRaw()
        assertEquals(1, restoredBookmarks.size)
        assertEquals("Test Bookmark", restoredBookmarks[0].note)
        assertEquals(15000L, restoredBookmarks[0].timestampMs)

        // Verify settings restored
        val restoredSettings = settingsRepo.settingsFlow.first()
        assertTrue(restoredSettings.swapSwipeGestures)
        assertFalse(restoredSettings.volumeGestureEnabled)
        assertFalse(restoredSettings.brightnessGestureEnabled)
        assertFalse(restoredSettings.swipeToSeekEnabled)
        assertFalse(restoredSettings.doubleTapToSeekEnabled)
        assertFalse(restoredSettings.holdFor2xSpeedEnabled)
        assertFalse(restoredSettings.matchVideoOrientation)
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreFromStream_rejectsInvalidFormat() = runBlocking {
        val invalidJson = JSONObject().apply {
            put("format", "unknown_format")
            put("version", 1)
        }.toString()

        val input = ByteArrayInputStream(invalidJson.toByteArray())
        ApturaBackupCoordinator.restoreFromStream(input, settingsRepo, db)
        Unit
    }
}
