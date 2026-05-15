package com.linuxquest.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgress(
    @PrimaryKey val levelId: Int,
    val completed: Boolean = false,
    val attempted: Boolean = false,
    val stars: Int = 0,
    val hintsUsed: Int = 0,
    val commandsUsed: Int = 0,
    val timeTakenSeconds: Long = 0,
    val password: String? = null,
    val completedAt: Long? = null,
    val xpEarned: Int = 0,
    val terminalHistory: String? = null,
    val commandHistory: String? = null
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlockedAt: Long? = null,
    val unlocked: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 0,
    val currentRank: String = "Newbie",
    val levelsCompleted: Int = 0
)
