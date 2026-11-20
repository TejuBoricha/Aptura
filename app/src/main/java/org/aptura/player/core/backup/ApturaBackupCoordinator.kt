package org.aptura.player.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.data.database.ApturaDatabase
import org.aptura.player.data.database.BookmarkEntity
import org.aptura.player.data.database.MediaItemEntity
import org.aptura.player.data.settings.ApturaSettings
import org.aptura.player.data.settings.ApturaSettingsRepository
import org.aptura.player.domain.model.AudioEffectsSettings
import org.aptura.player.domain.model.EqualizerPreset
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages local, offline Storage Access Framework (SAF) JSON backups and restores (FEAT-05).
 * Encapsulates serialization of user settings, playback history, per-video preferences, and bookmarks.
 * Zero network, local offline only (DEC-012, DEC-011).
 */
object ApturaBackupCoordinator {

    const val BACKUP_VERSION = 1
    const val BACKUP_FORMAT = "aptura_backup"

    fun defaultBackupFileName(date: Date = Date()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "aptura-backup-${formatter.format(date)}.json"
    }

    suspend fun createBackupJson(
        settings: ApturaSettings,
        mediaItems: List<MediaItemEntity>,
        bookmarks: List<BookmarkEntity>
    ): String = withContext(Dispatchers.Default) {
        val root = JSONObject()
        root.put("format", BACKUP_FORMAT)
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Settings
        val settingsObj = JSONObject().apply {
            put("themeMode", settings.themeMode.name)
            put("accentColor", settings.accentColor.name)
            put("defaultPlaybackSpeed", settings.defaultPlaybackSpeed.toDouble())
            put("defaultResizeMode", settings.defaultResizeMode)
            put("seekDeltaSeconds", settings.seekDeltaSeconds)
            put("backgroundAudioEnabled", settings.backgroundAudioEnabled)
            put("autoAdvanceCountdownSeconds", settings.autoAdvanceCountdownSeconds)
            put("subtitleFontSizeSp", settings.subtitleFontSizeSp)
            put("subtitleTextColorHex", settings.subtitleTextColorHex)
            put("subtitleBackgroundEnabled", settings.subtitleBackgroundEnabled)
            put("subtitleUseSystemStyle", settings.subtitleUseSystemStyle)
            settings.subtitleEncoding?.let { put("subtitleEncoding", it) }
            put("defaultDecoderStrategy", settings.defaultDecoderStrategy)
            put("defaultAudioDelayMs", settings.defaultAudioDelayMs)
            put("libraryLayout", settings.libraryLayout.name)
            put("libraryDensity", settings.libraryDensity.name)
            put("libraryDetail", settings.libraryDetail.name)
            put("folderLayout", settings.folderLayout.name)
            settings.librarySortName?.let { put("librarySortName", it) }
            put("isPrivateMode", settings.isPrivateMode)
            val safArray = JSONArray()
            settings.persistedSafFolders.forEach { safArray.put(it) }
            put("persistedSafFolders", safArray)
        }
        root.put("settings", settingsObj)

        // 2. Media Items (playback progress, per-video settings)
        val itemsArray = JSONArray()
        mediaItems.forEach { item ->
            // Only export items with user state
            if (item.playbackPositionMs > 0 || item.isCompleted || item.customTitle != null ||
                item.playbackSpeed != 1.0f || item.resizeMode != "FIT" || item.audioDelayOverridden ||
                item.subtitleDelayMs != 0L || item.isHidden
            ) {
                val itemObj = JSONObject().apply {
                    put("uri", item.uri)
                    put("title", item.title)
                    put("displayName", item.displayName)
                    put("playbackPositionMs", item.playbackPositionMs)
                    put("isCompleted", item.isCompleted)
                    put("lastPlayedTimestamp", item.lastPlayedTimestamp)
                    put("resizeMode", item.resizeMode)
                    put("playbackSpeed", item.playbackSpeed.toDouble())
                    put("audioDelayMs", item.audioDelayMs)
                    put("audioDelayOverridden", item.audioDelayOverridden)
                    put("subtitleDelayMs", item.subtitleDelayMs)
                    item.customTitle?.let { put("customTitle", it) }
                    put("isHidden", item.isHidden)
                }
                itemsArray.put(itemObj)
            }
        }
        root.put("mediaItems", itemsArray)

        // 3. Bookmarks
        val bookmarksArray = JSONArray()
        bookmarks.forEach { bm ->
            val bmObj = JSONObject().apply {
                put("mediaUri", bm.mediaUri)
                put("timestampMs", bm.timestampMs)
                put("note", bm.note)
                put("createdAtSec", bm.createdAtSec)
            }
            bookmarksArray.put(bmObj)
        }
        root.put("bookmarks", bookmarksArray)

        root.toString(2)
    }

    suspend fun exportToStream(
        outputStream: OutputStream,
        settings: ApturaSettings,
        mediaItems: List<MediaItemEntity>,
        bookmarks: List<BookmarkEntity>
    ) = withContext(Dispatchers.IO) {
        val json = createBackupJson(settings, mediaItems, bookmarks)
        outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
    }

    data class RestoreSummary(
        val videosRestored: Int,
        val bookmarksRestored: Int,
        val settingsRestored: Boolean
    )

    suspend fun restoreFromStream(
        inputStream: InputStream,
        settingsRepo: ApturaSettingsRepository,
        database: ApturaDatabase
    ): RestoreSummary = withContext(Dispatchers.IO) {
        val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(content)

        val format = root.optString("format")
        require(format == BACKUP_FORMAT) { "Unrecognised backup format: '$format'" }

        // 1. Restore Settings
        var settingsRestored = false
        if (root.has("settings")) {
            val s = root.getJSONObject("settings")
            s.optString("themeMode").takeIf { it.isNotBlank() }?.let { name ->
                try { settingsRepo.updateThemeMode(ThemeMode.valueOf(name)) } catch (_: Exception) {}
            }
            s.optString("accentColor").takeIf { it.isNotBlank() }?.let { name ->
                try { settingsRepo.updateAccentColor(AccentColor.valueOf(name)) } catch (_: Exception) {}
            }
            if (s.has("defaultPlaybackSpeed")) {
                settingsRepo.updateDefaultPlaybackSpeed(s.getDouble("defaultPlaybackSpeed").toFloat())
            }
            if (s.has("defaultResizeMode")) {
                settingsRepo.updateDefaultResizeMode(s.getInt("defaultResizeMode"))
            }
            if (s.has("seekDeltaSeconds")) {
                settingsRepo.updateSeekDeltaSeconds(s.getInt("seekDeltaSeconds"))
            }
            if (s.has("backgroundAudioEnabled")) {
                settingsRepo.updateBackgroundAudioEnabled(s.getBoolean("backgroundAudioEnabled"))
            }
            if (s.has("autoAdvanceCountdownSeconds")) {
                settingsRepo.updateAutoAdvanceCountdown(s.getInt("autoAdvanceCountdownSeconds"))
            }
            if (s.has("subtitleFontSizeSp")) {
                settingsRepo.updateSubtitleFontSize(s.getInt("subtitleFontSizeSp"))
            }
            if (s.has("subtitleTextColorHex")) {
                settingsRepo.updateSubtitleTextColor(s.getString("subtitleTextColorHex"))
            }
            if (s.has("subtitleBackgroundEnabled")) {
                settingsRepo.updateSubtitleBackground(s.getBoolean("subtitleBackgroundEnabled"))
            }
            if (s.has("subtitleUseSystemStyle")) {
                settingsRepo.updateSubtitleUseSystemStyle(s.getBoolean("subtitleUseSystemStyle"))
            }
            if (s.has("subtitleEncoding")) {
                settingsRepo.updateSubtitleEncoding(s.optString("subtitleEncoding").takeIf { it.isNotBlank() })
            }
            if (s.has("defaultDecoderStrategy")) {
                settingsRepo.updateDefaultDecoderStrategy(s.getInt("defaultDecoderStrategy"))
            }
            if (s.has("defaultAudioDelayMs")) {
                settingsRepo.updateDefaultAudioDelayMs(s.getLong("defaultAudioDelayMs"))
            }
            if (s.has("isPrivateMode")) {
                settingsRepo.updatePrivateMode(s.getBoolean("isPrivateMode"))
            }
            settingsRestored = true
        }

        // 2. Restore Media Items
        var videosRestored = 0
        if (root.has("mediaItems")) {
            val itemsArray = root.getJSONArray("mediaItems")
            val mediaDao = database.mediaItemDao()
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                val uri = itemObj.getString("uri")
                val existing = mediaDao.getVideoByUri(uri)
                if (existing != null) {
                    val updated = existing.copy(
                        playbackPositionMs = itemObj.optLong("playbackPositionMs", existing.playbackPositionMs),
                        isCompleted = itemObj.optBoolean("isCompleted", existing.isCompleted),
                        lastPlayedTimestamp = itemObj.optLong("lastPlayedTimestamp", existing.lastPlayedTimestamp),
                        resizeMode = itemObj.optString("resizeMode", existing.resizeMode),
                        playbackSpeed = itemObj.optDouble("playbackSpeed", existing.playbackSpeed.toDouble()).toFloat(),
                        audioDelayMs = itemObj.optLong("audioDelayMs", existing.audioDelayMs),
                        audioDelayOverridden = itemObj.optBoolean("audioDelayOverridden", existing.audioDelayOverridden),
                        subtitleDelayMs = itemObj.optLong("subtitleDelayMs", existing.subtitleDelayMs),
                        customTitle = if (itemObj.has("customTitle")) itemObj.optString("customTitle") else existing.customTitle,
                        isHidden = itemObj.optBoolean("isHidden", existing.isHidden)
                    )
                    mediaDao.insertOrUpdate(updated)
                    videosRestored++
                } else {
                    val newEntity = MediaItemEntity(
                        mediaStoreId = 0L,
                        uri = uri,
                        displayName = itemObj.optString("displayName", ""),
                        title = itemObj.optString("title", ""),
                        customTitle = if (itemObj.has("customTitle")) itemObj.optString("customTitle") else null,
                        durationMs = 0L,
                        sizeBytes = 0L,
                        dateModifiedSec = 0L,
                        dateAddedSec = 0L,
                        mimeType = "video/*",
                        folderPath = "",
                        playbackPositionMs = itemObj.optLong("playbackPositionMs", 0L),
                        isCompleted = itemObj.optBoolean("isCompleted", false),
                        lastPlayedTimestamp = itemObj.optLong("lastPlayedTimestamp", 0L),
                        resizeMode = itemObj.optString("resizeMode", "FIT"),
                        playbackSpeed = itemObj.optDouble("playbackSpeed", 1.0).toFloat(),
                        audioDelayMs = itemObj.optLong("audioDelayMs", 0L),
                        audioDelayOverridden = itemObj.optBoolean("audioDelayOverridden", false),
                        subtitleDelayMs = itemObj.optLong("subtitleDelayMs", 0L),
                        isHidden = itemObj.optBoolean("isHidden", false),
                        isMissing = true
                    )
                    mediaDao.insertOrUpdate(newEntity)
                    videosRestored++
                }
            }
        }

        // 3. Restore Bookmarks
        var bookmarksRestored = 0
        if (root.has("bookmarks")) {
            val bookmarksArray = root.getJSONArray("bookmarks")
            val bookmarkDao = database.bookmarkDao()
            for (i in 0 until bookmarksArray.length()) {
                val bmObj = bookmarksArray.getJSONObject(i)
                val entity = BookmarkEntity(
                    mediaUri = bmObj.getString("mediaUri"),
                    timestampMs = bmObj.getLong("timestampMs"),
                    note = bmObj.optString("note", ""),
                    createdAtSec = bmObj.optLong("createdAtSec", System.currentTimeMillis() / 1000L)
                )
                bookmarkDao.insertBookmark(entity)
                bookmarksRestored++
            }
        }

        RestoreSummary(
            videosRestored = videosRestored,
            bookmarksRestored = bookmarksRestored,
            settingsRestored = settingsRestored
        )
    }
}
