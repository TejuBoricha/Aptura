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
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aptura_settings")

@Immutable
data class ApturaSettings(
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultResizeMode: Int = 0,
    val seekDeltaSeconds: Int = 10,
    val backgroundAudioEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val autoAdvanceCountdownSeconds: Int = 10
)

/**
 * Asynchronous, DataStore-backed repository for player and library settings (SCR-013, FR-SET-001).
 * Safe against process death and thread concurrency.
 */
class ApturaSettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val DEFAULT_RESIZE_MODE = intPreferencesKey("default_resize_mode")
        val SEEK_DELTA_SECONDS = intPreferencesKey("seek_delta_seconds")
        val BACKGROUND_AUDIO_ENABLED = booleanPreferencesKey("background_audio_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val AUTO_ADVANCE_COUNTDOWN = intPreferencesKey("auto_advance_countdown")
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
                seekDeltaSeconds = preferences[PreferencesKeys.SEEK_DELTA_SECONDS] ?: 10,
                backgroundAudioEnabled = preferences[PreferencesKeys.BACKGROUND_AUDIO_ENABLED] ?: true,
                onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                autoAdvanceCountdownSeconds = preferences[PreferencesKeys.AUTO_ADVANCE_COUNTDOWN] ?: 10
            )
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

    suspend fun updateAutoAdvanceCountdown(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_ADVANCE_COUNTDOWN] = seconds
        }
    }

    @androidx.annotation.VisibleForTesting
    suspend fun resetForTesting() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
