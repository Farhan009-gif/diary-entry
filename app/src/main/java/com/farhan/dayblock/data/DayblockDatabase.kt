package com.farhan.dayblock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DiaryEntry::class], version = 1, exportSchema = false)
abstract class DayblockDatabase : RoomDatabase() {

    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var instance: DayblockDatabase? = null

        fun create(context: Context): DayblockDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DayblockDatabase::class.java,
                    "dayblock.db"
                ).build().also { instance = it }
            }
        }
    }
}
