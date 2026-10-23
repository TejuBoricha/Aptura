package org.aptura.player.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Room used to be configured with `fallbackToDestructiveMigration` and `exportSchema = false`, so a forgotten
 * migration silently wiped every user's history, resume positions and bookmarks. These checks make that mistake fail
 * in the unit tests instead.
 */
class ApturaDatabaseMigrationTest {

    @Test
    fun everySchemaVersionHasAMigrationToTheNextOne() {
        val byStart = ApturaDatabase.ALL_MIGRATIONS.associateBy { it.startVersion }

        for (version in 2 until APTURA_DB_VERSION) {
            val migration = byStart[version]
            assertTrue("no migration from schema $version to ${version + 1}", migration != null)
            assertEquals("migration from $version must end at ${version + 1}", version + 1, migration!!.endVersion)
        }
        assertEquals(
            "a migration targets a version above the current schema",
            APTURA_DB_VERSION,
            ApturaDatabase.ALL_MIGRATIONS.maxOf { it.endVersion }
        )
    }

    @Test
    fun theCurrentSchemaIsExportedAndCommitted() {
        val file = File("schemas/org.aptura.player.data.database.ApturaDatabase/$APTURA_DB_VERSION.json")

        assertTrue("schema $APTURA_DB_VERSION was not exported to ${file.path} (build the app and commit it)", file.exists())
        val json = file.readText()
        assertTrue(json.contains("\"version\": $APTURA_DB_VERSION"))
        // The column added by the latest migration must be in the exported schema, not just in the migration SQL.
        assertTrue(json.contains("\"decoderOverridden\""))
    }
}
