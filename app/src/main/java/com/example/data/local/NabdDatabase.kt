package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedChatEntity::class,
        CachedMessageEntity::class,
        CachedPostEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NabdDatabase : RoomDatabase() {
    abstract fun nabdDao(): NabdDao

    companion object {
        @Volatile
        private var INSTANCE: NabdDatabase? = null

        fun getInstance(context: Context): NabdDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NabdDatabase::class.java,
                    "nabd_offline_cache.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
