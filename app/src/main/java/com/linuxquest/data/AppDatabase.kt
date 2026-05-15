package com.linuxquest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.linuxquest.data.dao.AchievementDao
import com.linuxquest.data.dao.ProgressDao
import com.linuxquest.data.entities.Achievement
import com.linuxquest.data.entities.LevelProgress

@Database(
    entities = [LevelProgress::class, Achievement::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linuxquest.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
