package com.linuxquest.game

import com.linuxquest.data.dao.AchievementDao
import com.linuxquest.data.dao.ProgressDao
import com.linuxquest.data.entities.Achievement
import kotlinx.coroutines.flow.Flow

class AchievementManager(
    private val achievementDao: AchievementDao,
    private val progressDao: ProgressDao
) {

    private data class AchievementDef(
        val id: String,
        val title: String,
        val description: String,
        val icon: String
    )

    private val definitions = listOf(
        AchievementDef("first_blood", "First Blood", "Complete your first level", "\uD83C\uDFAF"),
        AchievementDef("quick_learner", "Quick Learner", "Complete 10 levels", "\uD83D\uDCDA"),
        AchievementDef("halfway", "Halfway There", "Complete 28 levels", "\uD83C\uDFD4\uFE0F"),
        AchievementDef("master", "Linux Master", "Complete all 56 levels", "\uD83C\uDFC6"),
        AchievementDef("no_hints", "No Help Needed", "Complete a level without hints", "\uD83E\uDDE0"),
        AchievementDef("speedrunner", "Speedrunner", "Complete a level in under 60 seconds", "\u26A1"),
        AchievementDef("perfectionist", "Perfectionist", "Get 3 stars on 10 levels", "\u2B50"),
        AchievementDef("pipe_master", "Pipe Master", "Complete all text processing levels", "\uD83D\uDD27"),
        AchievementDef("root_access", "Root Access", "Complete all permission levels", "\uD83D\uDD10"),
        AchievementDef("script_kiddie", "Script Kiddie", "Complete your first scripting level", "\uD83D\uDCDC"),
        AchievementDef("script_master", "Script Master", "Complete all scripting levels", "\uD83E\uDDD9"),
        AchievementDef("hacker", "Elite Hacker", "Complete all master challenges", "\uD83D\uDC80"),
        // Rank badges
        AchievementDef("rank_apprentice", "Apprentice", "Reach Apprentice rank (500 XP)", "\uD83D\uDD30"),
        AchievementDef("rank_explorer", "Explorer", "Reach Explorer rank (1500 XP)", "\uD83E\uDDED"),
        AchievementDef("rank_hacker", "Hacker", "Reach Hacker rank (3500 XP)", "\uD83D\uDCBB"),
        AchievementDef("rank_sysadmin", "Sysadmin", "Reach Sysadmin rank (7000 XP)", "\uD83D\uDDA5\uFE0F"),
        AchievementDef("rank_root", "Root User", "Reach Root User rank (12000 XP)", "\uD83D\uDD10"),
        AchievementDef("rank_master", "Linux Master", "Reach Linux Master rank (18000 XP)", "\uD83D\uDC51")
    )

    suspend fun initAchievements() {
        for (def in definitions) {
            achievementDao.insertAchievement(
                Achievement(
                    id = def.id,
                    title = def.title,
                    description = def.description,
                    icon = def.icon
                )
            )
        }
    }

    suspend fun checkAndUnlock(
        levelId: Int,
        hintsUsed: Int,
        timeSeconds: Long,
        totalCompleted: Int,
        totalXp: Int = 0
    ) {
        val now = System.currentTimeMillis()

        if (totalCompleted >= 1) unlock("first_blood", now)
        if (totalCompleted >= 10) unlock("quick_learner", now)
        if (totalCompleted >= 28) unlock("halfway", now)
        if (totalCompleted >= 56) unlock("master", now)
        if (hintsUsed == 0) unlock("no_hints", now)
        if (timeSeconds < 60) unlock("speedrunner", now)

        if (levelId in 10..19 && allCompleted(10..19)) unlock("pipe_master", now)
        if (levelId in 20..29 && allCompleted(20..29)) unlock("root_access", now)
        if (levelId in 40..49) {
            unlock("script_kiddie", now)
            if (allCompleted(40..49)) unlock("script_master", now)
        }
        if (levelId in 50..55 && allCompleted(50..55)) unlock("hacker", now)

        if (countThreeStarLevels() >= 10) unlock("perfectionist", now)

        // Rank badges
        if (totalXp >= 500) unlock("rank_apprentice", now)
        if (totalXp >= 1500) unlock("rank_explorer", now)
        if (totalXp >= 3500) unlock("rank_hacker", now)
        if (totalXp >= 7000) unlock("rank_sysadmin", now)
        if (totalXp >= 12000) unlock("rank_root", now)
        if (totalXp >= 18000) unlock("rank_master", now)
    }

    fun getAllAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements()
    }

    private suspend fun unlock(id: String, timestamp: Long) {
        achievementDao.unlockAchievement(id, timestamp)
    }

    private suspend fun allCompleted(range: IntRange): Boolean {
        for (id in range) {
            val progress = progressDao.getProgress(id)
            if (progress?.completed != true) return false
        }
        return true
    }

    private suspend fun countThreeStarLevels(): Int {
        var count = 0
        for (id in 0..55) {
            val progress = progressDao.getProgress(id)
            if (progress != null && progress.completed && progress.stars >= 3) count++
        }
        return count
    }
}
