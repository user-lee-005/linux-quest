package com.linuxquest.data.dao

import androidx.room.*
import com.linuxquest.data.entities.LevelProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM level_progress ORDER BY levelId")
    fun getAllProgress(): Flow<List<LevelProgress>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    suspend fun getProgress(levelId: Int): LevelProgress?

    @Query("SELECT COUNT(*) FROM level_progress WHERE completed = 1")
    fun getCompletedCount(): Flow<Int>

    @Upsert
    suspend fun upsertProgress(progress: LevelProgress)

    @Query("UPDATE level_progress SET completed = 1, password = :password, stars = :stars, hintsUsed = :hints, commandsUsed = :commands, timeTakenSeconds = :time, completedAt = :completedAt WHERE levelId = :levelId")
    suspend fun completeLevel(levelId: Int, password: String, stars: Int, hints: Int, commands: Int, time: Long, completedAt: Long)

    @Query("DELETE FROM level_progress")
    suspend fun resetAllProgress()
}
