package com.example.chalkak

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// synchronize three table
@Database(
    entities = [PhotoLog::class, DetectedObject::class, ExampleSentence::class],
    version = 2, // Incremented version due to index addition
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Function that brings DAO
    abstract fun photoLogDao(): PhotoLogDao
    abstract fun detectedObjectDao(): DetectedObjectDao
    abstract fun exampleSentenceDao(): ExampleSentenceDao

    companion object {
        // singleton
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // If existed, bring
            return INSTANCE ?: synchronized(this) {
                // else make
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chalkak_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}