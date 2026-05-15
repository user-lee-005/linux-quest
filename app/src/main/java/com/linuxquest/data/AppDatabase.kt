package com.linuxquest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.linuxquest.data.dao.AchievementDao
import com.linuxquest.data.dao.ProgressDao
import com.linuxquest.data.entities.Achievement
import com.linuxquest.data.entities.LevelProgress
import com.linuxquest.data.entities.UserProfile

@Database(
    entities = [LevelProgress::class, Achievement::class, UserProfile::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE level_progress ADD COLUMN attempted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE level_progress ADD COLUMN xpEarned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS user_profile (id INTEGER NOT NULL PRIMARY KEY, totalXp INTEGER NOT NULL DEFAULT 0, currentRank TEXT NOT NULL DEFAULT 'Newbie', levelsCompleted INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT OR IGNORE INTO user_profile (id, totalXp, currentRank, levelsCompleted) VALUES (1, 0, 'Newbie', 0)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE level_progress ADD COLUMN terminalHistory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE level_progress ADD COLUMN commandHistory TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linuxquest.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
