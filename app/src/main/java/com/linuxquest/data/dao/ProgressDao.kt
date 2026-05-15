package com.linuxquest.data.dao

import androidx.room.*
import com.linuxquest.data.entities.LevelProgress
import com.linuxquest.data.entities.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM level_progress ORDER BY levelId")
    fun getAllProgress(): Flow<List<LevelProgress>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    suspend fun getProgress(levelId: Int): LevelProgress?

    @Query("SELECT COUNT(*) FROM level_progress WHERE completed = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM level_progress WHERE completed = 1")
    suspend fun getCompletedCountSync(): Int

    @Upsert
    suspend fun upsertProgress(progress: LevelProgress)

    @Query("UPDATE level_progress SET completed = 1, password = :password, stars = :stars, hintsUsed = :hints, commandsUsed = :commands, timeTakenSeconds = :time, completedAt = :completedAt, xpEarned = :xp WHERE levelId = :levelId")
    suspend fun completeLevel(levelId: Int, password: String, stars: Int, hints: Int, commands: Int, time: Long, completedAt: Long, xp: Int)

    @Query("UPDATE level_progress SET attempted = 1, hintsUsed = :hints, commandsUsed = :commands WHERE levelId = :levelId")
    suspend fun attemptLevel(levelId: Int, hints: Int, commands: Int)

    @Query("SELECT MIN(levelId) FROM level_progress WHERE attempted = 1 AND completed = 0")
    suspend fun getLastAttemptedLevel(): Int?

    @Query("DELETE FROM level_progress")
    suspend fun resetAllProgress()

    // User profile queries
    @Upsert
    suspend fun upsertProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfile?

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("UPDATE user_profile SET totalXp = totalXp + :xp, levelsCompleted = levelsCompleted + 1 WHERE id = 1")
    suspend fun addXp(xp: Int)

    @Query("UPDATE user_profile SET currentRank = :rank WHERE id = 1")
    suspend fun updateRank(rank: String)
}
