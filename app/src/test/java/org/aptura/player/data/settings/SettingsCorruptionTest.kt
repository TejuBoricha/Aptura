package org.aptura.player.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Audit DATA-07: the settings file had no corruption handler, so one unreadable file made every later read and write
 * fail for good. These tests open a real DataStore over a real damaged file.
 */
class SettingsCorruptionTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private val key = stringPreferencesKey("accent_color")

    @After
    fun closeStores() {
        scopes.forEach { it.cancel() }
    }

    private fun damagedFile(): File = folder.newFile("aptura_settings.preferences_pb").apply {
        // not a valid preferences protobuf: a torn write or a bad restore looks like this
        writeBytes(byteArrayOf(0x0A, 0x7F, 0x13, 0x00, 0x55, 0x66, 0x01, 0x02, 0x03))
    }

    private fun openStore(file: File, withHandler: Boolean): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        return PreferenceDataStoreFactory.create(
            corruptionHandler = if (withHandler) settingsCorruptionHandler else null,
            scope = scope,
            produceFile = { file }
        )
    }

    @Test
    fun withoutAHandler_aDamagedFileBreaksEveryReadForGood() = runBlocking {
        val store = openStore(damagedFile(), withHandler = false)

        try {
            store.data.first()
            fail("the damaged file should not have been readable")
        } catch (expected: CorruptionException) {
            // this is the failure the handler exists to repair
        }
    }

    @Test
    fun aDamagedFile_startsFromDefaultsInsteadOfFailing() = runBlocking {
        val store = openStore(damagedFile(), withHandler = true)

        assertTrue("settings start empty", store.data.first().asMap().isEmpty())
    }

    @Test
    fun afterTheDamagedFileIsReplaced_aNewChangeIsSavedAndSurvivesARestart() = runBlocking {
        val file = damagedFile()
        val first = openStore(file, withHandler = true)

        first.edit { it[key] = "GOLD" }
        assertEquals("GOLD", first.data.first()[key])

        // a new process reads the same file
        scopes.forEach { it.cancel() }
        val second = openStore(file, withHandler = true)
        assertEquals("GOLD", second.data.first()[key])
    }

    @Test
    fun aHealthyFile_isNeverReplaced() = runBlocking {
        val file = folder.newFile("healthy.preferences_pb")
        file.delete()
        val first = openStore(file, withHandler = true)
        first.edit { it[key] = "ROSE" }
        scopes.forEach { it.cancel() }

        val second = openStore(file, withHandler = true)

        assertEquals("ROSE", second.data.first()[key])
        assertNull(second.data.first()[stringPreferencesKey("never_written")])
    }

    /** The handler is only worth anything if the real store uses it, and that store is private to the repository. */
    @Test
    fun theRealSettingsStore_isBuiltWithTheHandler() {
        val source = listOf(
            "app/src/main/java/org/aptura/player/data/settings/ApturaSettingsRepository.kt",
            "../app/src/main/java/org/aptura/player/data/settings/ApturaSettingsRepository.kt"
        ).map(::File).first { it.exists() }.readText()

        val delegate = source.substringAfter("val Context.dataStore").substringBefore(")")

        assertTrue(
            "the settings DataStore must pass corruptionHandler = settingsCorruptionHandler",
            "corruptionHandler = settingsCorruptionHandler" in delegate
        )
    }
}
