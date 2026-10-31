package org.aptura.player.data.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import org.aptura.player.domain.model.AudioEffectsSettings
import org.aptura.player.domain.model.EqualizerPreset
import org.aptura.player.domain.model.FolderLayout
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.domain.repository.AudioEffectsStore
import org.aptura.player.theme.AccentColor
import org.aptura.player.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aptura_settings")

/** A stored enum by name; a missing or unknown name (a downgrade, a hand-edited file) falls back to [default]. */
private inline fun <reified E : Enum<E>> Preferences.enumOr(key: Preferences.Key<String>, default: E): E =
    this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

@Immutable
data class ApturaSettings(
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultResizeMode: Int = 0,
    val seekDeltaSeconds: Int = DEFAULT_SEEK_DELTA_SECONDS,
    val backgroundAudioEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    /** The app version whose welcome guide was last completed. A mismatch means the guide is shown again. */
    val lastSeenGuideVersion: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.CYAN,
    val autoAdvanceCountdownSeconds: Int = 10,
    val subtitleFontSizeSp: Int = 18,
    val subtitleTextColorHex: String = "#FFFFFF",
    val subtitleBackgroundEnabled: Boolean = false,
    val defaultDecoderStrategy: Int = 0, // 0: Auto, 1: Hardware, 2: Software
    val persistedSafFolders: Set<String> = emptySet(),
    val audioEffects: AudioEffectsSettings = AudioEffectsSettings(),
    val isSupporter: Boolean = false,
    val libraryLayout: LibraryLayout = LibraryLayout.GRID,
    val libraryDensity: LibraryDensity = LibraryDensity.COMFORTABLE,
    val libraryDetail: LibraryDetail = LibraryDetail.STANDARD,
    val folderLayout: FolderLayout = FolderLayout.LIST,
    /** The name of the sort the user picked in the library, or null to let the library choose by folder. */
    val librarySortName: String? = null
) {
    companion object {
        const val DEFAULT_SEEK_DELTA_SECONDS = 10
    }
}

/**
 * Asynchronous, DataStore-backed repository for player and library settings (SCR-013, FR-SET-001).
 * Safe against process death and thread concurrency.
 */
class ApturaSettingsRepository(private val context: Context) : AudioEffectsStore {

    private object PreferencesKeys {
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val DEFAULT_RESIZE_MODE = intPreferencesKey("default_resize_mode")
        val SEEK_DELTA_SECONDS = intPreferencesKey("seek_delta_seconds")
        val BACKGROUND_AUDIO_ENABLED = booleanPreferencesKey("background_audio_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LAST_SEEN_GUIDE_VERSION = intPreferencesKey("last_seen_guide_version")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val AUTO_ADVANCE_COUNTDOWN = intPreferencesKey("auto_advance_countdown")
        val SUBTITLE_FONT_SIZE_SP = intPreferencesKey("subtitle_font_size_sp")
        val SUBTITLE_TEXT_COLOR_HEX = stringPreferencesKey("subtitle_text_color_hex")
        val SUBTITLE_BACKGROUND_ENABLED = booleanPreferencesKey("subtitle_background_enabled")
        val DEFAULT_DECODER_STRATEGY = intPreferencesKey("default_decoder_strategy")
        val PERSISTED_SAF_FOLDERS = stringSetPreferencesKey("persisted_saf_folders")
        val FX_ENABLED = booleanPreferencesKey("fx_enabled")
        val FX_PRESET = stringPreferencesKey("fx_preset")
        val FX_BAND_LEVELS = stringPreferencesKey("fx_band_levels")
        val FX_BASS = intPreferencesKey("fx_bass_percent")
        val FX_VIRTUALIZER = intPreferencesKey("fx_virtualizer_percent")
        val FX_LOUDNESS = intPreferencesKey("fx_loudness_percent")
        val IS_SUPPORTER = booleanPreferencesKey("is_supporter")
        val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
        val LIBRARY_DENSITY = stringPreferencesKey("library_density")
        val LIBRARY_DETAIL = stringPreferencesKey("library_detail")
        val FOLDER_LAYOUT = stringPreferencesKey("folder_layout")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
    }

    val settingsFlow: Flow<ApturaSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ApturaSettings(
                defaultPlaybackSpeed = preferences[PreferencesKeys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
                defaultResizeMode = preferences[PreferencesKeys.DEFAULT_RESIZE_MODE] ?: 0,
                seekDeltaSeconds = preferences[PreferencesKeys.SEEK_DELTA_SECONDS] ?: ApturaSettings.DEFAULT_SEEK_DELTA_SECONDS,
                backgroundAudioEnabled = preferences[PreferencesKeys.BACKGROUND_AUDIO_ENABLED] ?: true,
                onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                lastSeenGuideVersion = preferences[PreferencesKeys.LAST_SEEN_GUIDE_VERSION] ?: 0,
                themeMode = preferences[PreferencesKeys.THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                accentColor = preferences[PreferencesKeys.ACCENT_COLOR]
                    ?.let { name -> AccentColor.entries.firstOrNull { it.name == name } }
                    ?: AccentColor.CYAN,
                autoAdvanceCountdownSeconds = preferences[PreferencesKeys.AUTO_ADVANCE_COUNTDOWN] ?: 10,
                subtitleFontSizeSp = preferences[PreferencesKeys.SUBTITLE_FONT_SIZE_SP] ?: 18,
                subtitleTextColorHex = preferences[PreferencesKeys.SUBTITLE_TEXT_COLOR_HEX] ?: "#FFFFFF",
                subtitleBackgroundEnabled = preferences[PreferencesKeys.SUBTITLE_BACKGROUND_ENABLED] ?: false,
                defaultDecoderStrategy = preferences[PreferencesKeys.DEFAULT_DECODER_STRATEGY] ?: 0,
                persistedSafFolders = preferences[PreferencesKeys.PERSISTED_SAF_FOLDERS] ?: emptySet(),
                audioEffects = AudioEffectsSettings(
                    enabled = preferences[PreferencesKeys.FX_ENABLED] ?: false,
                    preset = preferences[PreferencesKeys.FX_PRESET]
                        ?.let { name -> EqualizerPreset.entries.firstOrNull { it.name == name } }
                        ?: EqualizerPreset.FLAT,
                    bandLevelsMb = preferences[PreferencesKeys.FX_BAND_LEVELS]
                        ?.split(',')?.mapNotNull { it.trim().toIntOrNull() }.orEmpty(),
                    bassBoostPercent = preferences[PreferencesKeys.FX_BASS] ?: 0,
                    virtualizerPercent = preferences[PreferencesKeys.FX_VIRTUALIZER] ?: 0,
                    loudnessBoostPercent = preferences[PreferencesKeys.FX_LOUDNESS] ?: 0
                ),
                isSupporter = preferences[PreferencesKeys.IS_SUPPORTER] ?: false,
                libraryLayout = preferences.enumOr(PreferencesKeys.LIBRARY_LAYOUT, LibraryLayout.GRID),
                libraryDensity = preferences.enumOr(PreferencesKeys.LIBRARY_DENSITY, LibraryDensity.COMFORTABLE),
                libraryDetail = preferences.enumOr(PreferencesKeys.LIBRARY_DETAIL, LibraryDetail.STANDARD),
                folderLayout = preferences.enumOr(PreferencesKeys.FOLDER_LAYOUT, FolderLayout.LIST),
                librarySortName = preferences[PreferencesKeys.LIBRARY_SORT]
            )
        }

    suspend fun updateLibraryLayout(layout: LibraryLayout) {
        context.dataStore.edit { it[PreferencesKeys.LIBRARY_LAYOUT] = layout.name }
    }

    suspend fun updateLibraryDensity(density: LibraryDensity) {
        context.dataStore.edit { it[PreferencesKeys.LIBRARY_DENSITY] = density.name }
    }

    suspend fun updateLibraryDetail(detail: LibraryDetail) {
        context.dataStore.edit { it[PreferencesKeys.LIBRARY_DETAIL] = detail.name }
    }

    suspend fun updateFolderLayout(layout: FolderLayout) {
        context.dataStore.edit { it[PreferencesKeys.FOLDER_LAYOUT] = layout.name }
    }

    /** Pass null to go back to the library's own choice of sort. */
    suspend fun updateLibrarySortName(name: String?) {
        context.dataStore.edit { preferences ->
            if (name == null) preferences.remove(PreferencesKeys.LIBRARY_SORT) else preferences[PreferencesKeys.LIBRARY_SORT] = name
        }
    }

    suspend fun updateDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PLAYBACK_SPEED] = speed
        }
    }

    suspend fun updateDefaultResizeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_RESIZE_MODE] = mode
        }
    }

    suspend fun updateSeekDeltaSeconds(deltaSeconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEEK_DELTA_SECONDS] = deltaSeconds
        }
    }

    suspend fun updateBackgroundAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_AUDIO_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setLastSeenGuideVersion(versionCode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_GUIDE_VERSION] = versionCode
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateAccentColor(accent: AccentColor) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = accent.name
        }
    }

    suspend fun updateSupporter(isSupporter: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SUPPORTER] = isSupporter
        }
    }

    suspend fun updateAutoAdvanceCountdown(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_ADVANCE_COUNTDOWN] = seconds
        }
    }

    suspend fun resetPlaybackDefaults() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PLAYBACK_SPEED] = 1.0f
            preferences[PreferencesKeys.DEFAULT_RESIZE_MODE] = 0
            preferences[PreferencesKeys.SEEK_DELTA_SECONDS] = 10
            preferences[PreferencesKeys.BACKGROUND_AUDIO_ENABLED] = true
            preferences[PreferencesKeys.AUTO_ADVANCE_COUNTDOWN] = 10
            preferences[PreferencesKeys.DEFAULT_DECODER_STRATEGY] = 0
        }
    }

    suspend fun updateSubtitleFontSize(sizeSp: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_FONT_SIZE_SP] = sizeSp
        }
    }

    suspend fun updateSubtitleTextColor(colorHex: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_TEXT_COLOR_HEX] = colorHex
        }
    }

    suspend fun updateSubtitleBackground(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_BACKGROUND_ENABLED] = enabled
        }
    }

    suspend fun updateDefaultDecoderStrategy(strategy: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_DECODER_STRATEGY] = strategy
        }
    }

    override suspend fun load(): AudioEffectsSettings = settingsFlow.first().audioEffects

    override suspend fun save(settings: AudioEffectsSettings) = updateAudioEffects(settings)

    suspend fun updateAudioEffects(effects: AudioEffectsSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FX_ENABLED] = effects.enabled
            preferences[PreferencesKeys.FX_PRESET] = effects.preset.name
            preferences[PreferencesKeys.FX_BAND_LEVELS] = effects.bandLevelsMb.joinToString(",")
            preferences[PreferencesKeys.FX_BASS] = effects.bassBoostPercent
            preferences[PreferencesKeys.FX_VIRTUALIZER] = effects.virtualizerPercent
            preferences[PreferencesKeys.FX_LOUDNESS] = effects.loudnessBoostPercent
        }
    }

    suspend fun addSafFolder(treeUri: String) {
        val clean = treeUri.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PERSISTED_SAF_FOLDERS] ?: emptySet()
            preferences[PreferencesKeys.PERSISTED_SAF_FOLDERS] = current + clean
        }
    }

    suspend fun removeSafFolder(treeUri: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PERSISTED_SAF_FOLDERS] ?: emptySet()
            preferences[PreferencesKeys.PERSISTED_SAF_FOLDERS] = current - treeUri
        }
    }

    /** Stores [value] under a raw string [key], as an older or newer build might have. */
    @androidx.annotation.VisibleForTesting
    suspend fun writeRawForTesting(key: String, value: String) {
        context.dataStore.edit { preferences -> preferences[stringPreferencesKey(key)] = value }
    }

    @androidx.annotation.VisibleForTesting
    suspend fun resetForTesting() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
