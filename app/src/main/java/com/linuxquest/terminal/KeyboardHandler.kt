package com.linuxquest.terminal

import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.CommandRegistry

data class TabCompletionResult(
    val suggestions: List<String>,
    val commonPrefix: String
)

class KeyboardHandler(
    private val registry: CommandRegistry,
    private val vfs: VirtualFileSystem
) {

    fun handleTabCompletion(currentInput: String, cursorPos: Int): TabCompletionResult {
        val textBeforeCursor = currentInput.substring(0, cursorPos.coerceAtMost(currentInput.length))
        val parts = textBeforeCursor.split(" ")

        return if (parts.size <= 1) {
            completeCommand(parts.lastOrNull() ?: "")
        } else {
            completePath(parts.last())
        }
    }

    private fun completeCommand(prefix: String): TabCompletionResult {
        val matches = registry.listCommands().filter { it.startsWith(prefix) }.sorted()
        return buildResult(prefix, matches)
    }

    private fun completePath(prefix: String): TabCompletionResult {
        val (dirPath, filePrefix) = splitPathPrefix(prefix)

        val resolvedDir = try {
            vfs.getAbsolutePath(dirPath.ifEmpty { "." })
        } catch (_: Exception) { return TabCompletionResult(emptyList(), prefix) }

        val entries = try {
            vfs.listDirectory(resolvedDir).map { it.name }
        } catch (_: Exception) { return TabCompletionResult(emptyList(), prefix) }

        val matches = entries.filter { it.startsWith(filePrefix) }.sorted()
        val suggestions = matches.map { name ->
            val fullPath = if (dirPath.isEmpty()) name else "$dirPath/$name"
            val isDir = try { vfs.isDirectory(vfs.getAbsolutePath("$resolvedDir/$name")) } catch (_: Exception) { false }
            if (isDir) "$fullPath/" else fullPath
        }

        val completed = if (suggestions.size == 1) {
            suggestions.first()
        } else {
            val common = findCommonPrefix(suggestions)
            common.ifEmpty { prefix }
        }

        return TabCompletionResult(suggestions, completed)
    }

    private fun splitPathPrefix(prefix: String): Pair<String, String> {
        val lastSlash = prefix.lastIndexOf('/')
        return if (lastSlash < 0) {
            "" to prefix
        } else {
            prefix.substring(0, lastSlash + 1) to prefix.substring(lastSlash + 1)
        }
    }

    private fun buildResult(prefix: String, matches: List<String>): TabCompletionResult {
        val common = if (matches.size == 1) matches.first() + " "
        else findCommonPrefix(matches).ifEmpty { prefix }
        return TabCompletionResult(matches, common)
    }

    private fun findCommonPrefix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        var prefix = strings.first()
        for (s in strings.drop(1)) {
            while (!s.startsWith(prefix) && prefix.isNotEmpty()) {
                prefix = prefix.dropLast(1)
            }
        }
        return prefix
    }
}
