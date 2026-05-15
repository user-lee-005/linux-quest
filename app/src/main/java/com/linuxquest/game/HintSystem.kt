package com.linuxquest.game

class HintSystem {

    private var currentHintIndex = 0

    fun reset() {
        currentHintIndex = 0
    }

    fun getNextHint(level: Level): String? {
        if (currentHintIndex >= level.hints.size) return null
        return level.hints[currentHintIndex++]
    }

    fun getHintsUsed(): Int = currentHintIndex

    fun hasMoreHints(level: Level): Boolean = currentHintIndex < level.hints.size
}
