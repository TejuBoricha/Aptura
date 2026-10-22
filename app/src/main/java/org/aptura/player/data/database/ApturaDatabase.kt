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
    version = 3,
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

        @Volatile
        private var INSTANCE: ApturaDatabase? = null

        fun getInstance(context: Context): ApturaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApturaDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
