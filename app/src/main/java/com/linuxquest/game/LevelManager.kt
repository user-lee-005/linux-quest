package com.linuxquest.game

import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.game.levels.createAdvancedDataLevels
import com.linuxquest.game.levels.createFileBasicsLevels
import com.linuxquest.game.levels.createMasterLevels
import com.linuxquest.game.levels.createPermissionsLevels
import com.linuxquest.game.levels.createScriptingLevels
import com.linuxquest.game.levels.createTextProcessingLevels

class LevelManager {

    private val levels: Map<Int, Level>

    init {
        val allLevels = mutableListOf<Level>()
        allLevels.addAll(createFileBasicsLevels())
        allLevels.addAll(createTextProcessingLevels())
        allLevels.addAll(createPermissionsLevels())
        allLevels.addAll(createAdvancedDataLevels())
        allLevels.addAll(createScriptingLevels())
        allLevels.addAll(createMasterLevels())
        levels = allLevels.associateBy { it.id }
    }

    fun getLevel(id: Int): Level? = levels[id]

    fun getAllLevels(): List<Level> = levels.values.sortedBy { it.id }

    fun getLevelsByCategory(category: LevelCategory): List<Level> {
        return levels.values.filter { it.category == category }.sortedBy { it.id }
    }

    fun setupLevel(level: Level, vfs: VirtualFileSystem, seed: Long = System.currentTimeMillis()) {
        vfs.switchUser("root")
        level.setupFileSystem(vfs, seed)
        vfs.switchUser("bandit0")
        vfs.cd("/home/bandit0")
    }

    fun getTotalLevelCount(): Int = levels.size
}
