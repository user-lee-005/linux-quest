package com.linuxquest.game

import com.linuxquest.filesystem.VirtualFileSystem

data class Level(
    val id: Int,
    val title: String,
    val category: LevelCategory,
    val description: String,
    val briefing: String,
    val hints: List<String>,
    val password: String,
    val setupFileSystem: (VirtualFileSystem) -> Unit,
    val validateCompletion: (VirtualFileSystem, String) -> Boolean,
    val teachingPoints: List<String>,
    val commandsIntroduced: List<String>
)

enum class LevelCategory(val displayName: String, val color: Long) {
    FILE_BASICS("File System Basics", 0xFF7FDBCA),
    TEXT_PROCESSING("Text Processing & Pipes", 0xFFC792EA),
    PERMISSIONS("Permissions & Users", 0xFFA9DC76),
    ADVANCED_DATA("Advanced Data", 0xFFFFCB6B),
    SCRIPTING("Shell Scripting", 0xFF82AAFF),
    MASTER("Master Challenges", 0xFFFF5370)
}
