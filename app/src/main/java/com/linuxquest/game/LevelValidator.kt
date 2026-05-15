package com.linuxquest.game

import com.linuxquest.filesystem.VirtualFileSystem

class LevelValidator {

    fun checkCompletion(level: Level, vfs: VirtualFileSystem, lastCommandOutput: String): Boolean {
        return level.validateCompletion(vfs, lastCommandOutput)
    }

    fun checkPassword(level: Level, input: String): Boolean {
        return input.trim() == level.password
    }
}
