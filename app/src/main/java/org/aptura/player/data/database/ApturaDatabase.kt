package org.aptura.player.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local Room SQLite database for Aptura Video Player.
 * Zero network, local offline storage only (DEC-001, ADR-004).
 */
@Database(
    entities = [MediaItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ApturaDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao

    companion object {
        private const val DATABASE_NAME = "aptura_media.db"

        @Volatile
        private var INSTANCE: ApturaDatabase? = null

        fun getInstance(context: Context): ApturaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApturaDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
