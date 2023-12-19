package com.example.yatracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Session::class, Location::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun locationDao(): LocationDao
}

object YaTrackerDatabase {
    private var INSTANCE: AppDatabase? = null

    fun getDao(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ya_tracker_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}