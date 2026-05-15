package com.linuxquest.game

import com.linuxquest.data.dao.ProgressDao
import com.linuxquest.data.entities.UserProfile

data class RankInfo(
    val name: String,
    val badge: String,
    val minXp: Int,
    val maxXp: Int
)

class XpSystem(private val progressDao: ProgressDao) {

    companion object {
        val RANKS = listOf(
            RankInfo("Newbie", "\uD83C\uDF31", 0, 499),
            RankInfo("Apprentice", "\uD83D\uDD30", 500, 1499),
            RankInfo("Explorer", "\uD83E\uDDED", 1500, 3499),
            RankInfo("Hacker", "\uD83D\uDCBB", 3500, 6999),
            RankInfo("Sysadmin", "\uD83D\uDDA5\uFE0F", 7000, 11999),
            RankInfo("Root User", "\uD83D\uDD10", 12000, 17999),
            RankInfo("Linux Master", "\uD83D\uDC51", 18000, Int.MAX_VALUE)
        )

        fun calculateXp(stars: Int, hintsUsed: Int, timeSeconds: Long): Int {
            var xp = 100 // base
            xp += stars * 50
            if (timeSeconds < 60) xp += 50
            if (hintsUsed == 0) xp += 75
            return xp
        }

        fun getRank(totalXp: Int): RankInfo {
            return RANKS.lastOrNull { totalXp >= it.minXp } ?: RANKS.first()
        }

        fun getXpToNextRank(totalXp: Int): Pair<Int, Int> {
            val current = getRank(totalXp)
            val currentIdx = RANKS.indexOf(current)
            if (currentIdx >= RANKS.size - 1) return Pair(totalXp, totalXp)
            val next = RANKS[currentIdx + 1]
            return Pair(totalXp - current.minXp, next.minXp - current.minXp)
        }

        fun getProgressPercent(totalXp: Int): Float {
            val (earned, needed) = getXpToNextRank(totalXp)
            if (needed <= 0) return 1f
            return (earned.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
        }
    }

    suspend fun awardXp(xp: Int) {
        ensureProfile()
        progressDao.addXp(xp)
        val profile = progressDao.getProfile() ?: return
        val newRank = getRank(profile.totalXp)
        if (newRank.name != profile.currentRank) {
            progressDao.updateRank(newRank.name)
        }
    }

    suspend fun getProfile(): UserProfile {
        ensureProfile()
        return progressDao.getProfile() ?: UserProfile()
    }

    private suspend fun ensureProfile() {
        if (progressDao.getProfile() == null) {
            progressDao.upsertProfile(UserProfile())
        }
    }
}
