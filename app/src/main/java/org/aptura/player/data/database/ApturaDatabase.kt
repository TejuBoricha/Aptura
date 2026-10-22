package org.aptura.player.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Local Room SQLite database for Aptura Video Player.
 * Zero network, local offline storage only (DEC-001, ADR-004).
 */
@Database(
    entities = [MediaItemEntity::class, BookmarkEntity::class],
    version = 5,
    exportSchema = false
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

        @Volatile
        private var INSTANCE: ApturaDatabase? = null

        fun getInstance(context: Context): ApturaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApturaDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
