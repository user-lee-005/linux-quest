package com.linuxquest.data.dao

import androidx.room.*
import com.linuxquest.data.entities.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY id")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE unlocked = 1")
    fun getUnlockedAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: Achievement)

    @Query("UPDATE achievements SET unlocked = 1, unlockedAt = :at WHERE id = :id")
    suspend fun unlockAchievement(id: String, at: Long)
}
