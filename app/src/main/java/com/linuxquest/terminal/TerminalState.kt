package com.linuxquest.terminal

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.json.JSONArray
import org.json.JSONObject

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

    fun serializeLines(): String {
        val arr = JSONArray()
        lines.forEach { line ->
            val obj = JSONObject()
            obj.put("t", line.text)
            obj.put("y", line.type.name)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun serializeCommandHistory(): String {
        val arr = JSONArray()
        commandHistory.forEach { arr.put(it) }
        return arr.toString()
    }

    fun restoreLines(json: String) {
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val text = obj.getString("t")
                val type = try { LineType.valueOf(obj.getString("y")) } catch (_: Exception) { LineType.OUTPUT }
                lines.add(TerminalLine(text, type))
            }
        } catch (_: Exception) { /* ignore corrupt data */ }
    }

    fun restoreCommandHistory(json: String) {
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                commandHistory.add(arr.getString(i))
            }
        } catch (_: Exception) { /* ignore corrupt data */ }
    }

    private fun trimBuffer() {
        while (lines.size > maxLines) {
            lines.removeAt(0)
        }
    }
}
