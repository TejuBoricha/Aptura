package org.aptura.player.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Current schema version. Bump it together with a new migration in [ApturaDatabase.ALL_MIGRATIONS]. */
const val APTURA_DB_VERSION = 5

/**
 * Local Room SQLite database for Aptura Video Player.
 * Zero network, local offline storage only (DEC-001, ADR-004).
 */
@Database(
    entities = [MediaItemEntity::class, BookmarkEntity::class],
    version = APTURA_DB_VERSION,
    exportSchema = true // schemas/ is committed; migrations are checked against it (see ApturaDatabaseMigrationTest)
)
abstract class ApturaDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        private const val DATABASE_NAME = "aptura_media.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN isMissing INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN resizeMode TEXT NOT NULL DEFAULT 'FIT'")
                db.execSQL("ALTER TABLE media_items ADD COLUMN playbackSpeed REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN decoderStrategy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN audioDelayMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN subtitleDelayMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN customTitle TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Every migration, in order. A schema bump without a matching entry here fails the migration-chain test. */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        @Volatile
        private var INSTANCE: ApturaDatabase? = null

        fun getInstance(context: Context): ApturaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApturaDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(*ALL_MIGRATIONS)
                // A forgotten migration must fail loudly (and in the chain test), not silently wipe the user's
                // watch history, resume positions and bookmarks. Only the pre-migration v1 and downgrades may reset.
                .fallbackToDestructiveMigrationFrom(true, 1)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
