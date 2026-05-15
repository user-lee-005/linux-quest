package com.linuxquest.terminal

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

enum class LineType { PROMPT, OUTPUT, ERROR, SYSTEM, INFO }

data class TerminalLine(val text: String, val type: LineType)

class TerminalState(private val maxLines: Int = 5000) {

    val lines: SnapshotStateList<TerminalLine> = mutableStateListOf()
    val currentInput = mutableStateOf("")
    val cursorPosition = mutableStateOf(0)
    val isProcessing = mutableStateOf(false)

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1
    private var savedInput = ""

    fun appendOutput(text: String, type: LineType = LineType.OUTPUT) {
        if (text.isEmpty() && type == LineType.OUTPUT) return
        text.split('\n').forEach { line ->
            lines.add(TerminalLine(line, type))
        }
        trimBuffer()
    }

    fun appendPrompt(prompt: String) {
        lines.add(TerminalLine(prompt, LineType.PROMPT))
        trimBuffer()
    }

    fun submitCommand(): String {
        val cmd = currentInput.value.trim()
        if (cmd.isNotEmpty()) {
            commandHistory.add(cmd)
        }
        currentInput.value = ""
        cursorPosition.value = 0
        historyIndex = -1
        savedInput = ""
        return cmd
    }

    fun historyUp() {
        if (commandHistory.isEmpty()) return
        if (historyIndex == -1) {
            savedInput = currentInput.value
            historyIndex = commandHistory.size - 1
        } else if (historyIndex > 0) {
            historyIndex--
        }
        currentInput.value = commandHistory[historyIndex]
        cursorPosition.value = currentInput.value.length
    }

    fun historyDown() {
        if (historyIndex == -1) return
        historyIndex++
        if (historyIndex >= commandHistory.size) {
            historyIndex = -1
            currentInput.value = savedInput
        } else {
            currentInput.value = commandHistory[historyIndex]
        }
        cursorPosition.value = currentInput.value.length
    }

    fun clear() {
        lines.clear()
    }

    fun getCommandHistory(): List<String> = commandHistory.toList()

    private fun trimBuffer() {
        while (lines.size > maxLines) {
            lines.removeAt(0)
        }
    }
}
