package com.linuxquest.game

import com.linuxquest.data.dao.ProgressDao
import com.linuxquest.data.entities.LevelProgress
import kotlinx.coroutines.flow.Flow

class ProgressTracker(private val progressDao: ProgressDao) {

    suspend fun markCompleted(
        levelId: Int,
        password: String,
        hintsUsed: Int,
        commandsUsed: Int,
        timeSeconds: Long
    ) {
        val stars = calculateStars(hintsUsed, commandsUsed)
        val existing = progressDao.getProgress(levelId)
        if (existing != null && existing.completed && existing.stars >= stars) {
            return
        }
        progressDao.upsertProgress(LevelProgress(levelId = levelId))
        progressDao.completeLevel(
            levelId = levelId,
            password = password,
            stars = stars,
            hints = hintsUsed,
            commands = commandsUsed,
            time = timeSeconds,
            completedAt = System.currentTimeMillis()
        )
    }

    suspend fun isCompleted(levelId: Int): Boolean {
        return progressDao.getProgress(levelId)?.completed == true
    }

    suspend fun isUnlocked(levelId: Int): Boolean {
        if (levelId == 0) return true
        return isCompleted(levelId - 1)
    }

    suspend fun getStars(levelId: Int): Int {
        val progress = progressDao.getProgress(levelId) ?: return 0
        if (!progress.completed) return 0
        return progress.stars
    }

    fun getAllProgress(): Flow<List<LevelProgress>> {
        return progressDao.getAllProgress()
    }

    fun getCompletedCount(): Flow<Int> {
        return progressDao.getCompletedCount()
    }

    private fun calculateStars(hintsUsed: Int, commandsUsed: Int): Int {
        return when {
            hintsUsed == 0 && commandsUsed < 10 -> 3
            hintsUsed <= 1 -> 2
            else -> 1
        }
    }
}
