package com.linuxquest.shell

import com.linuxquest.filesystem.VirtualFileSystem

/**
 * Manages environment variables, aliases, command history, and command
 * registration for the simulated shell.
 *
 * This replaces the original stub in ShellInterfaces.kt with full
 * variable-expansion support ($VAR, ${VAR}, $?, $!, $$, $0-$9, $@, $#),
 * PATH resolution, and shell metadata needed by the interpreter.
 */
class ShellEnvironment {

    // ─────────────────── variables ───────────────────────────────────

    private val variables = mutableMapOf<String, String>(
        "HOME" to "/home/bandit0",
        "USER" to "bandit0",
        "PATH" to "/usr/local/bin:/usr/bin:/bin",
        "SHELL" to "/bin/bash",
        "PWD" to "/home/bandit0",
        "OLDPWD" to "/home/bandit0",
        "TERM" to "xterm-256color",
        "LANG" to "en_US.UTF-8",
        "HOSTNAME" to "linuxquest",
        "PS1" to "\\u@\\h:\\w\\$ "
    )

    fun getVariable(name: String): String? = variables[name]

    fun setVariable(name: String, value: String) {
        variables[name] = value
    }

    fun removeVariable(name: String) {
        variables.remove(name)
    }

    fun getAllVariables(): Map<String, String> = variables.toMap()

    // ─────────────────── special shell state ────────────────────────

    var lastExitCode: Int = 0
    var lastBackgroundPid: Int = 0
    var shellPid: Int = 1000
    var positionalArgs: List<String> = emptyList()
    var shellName: String = "bash"

    // ─────────────────── variable expansion ─────────────────────────

    /**
     * Expands `$VAR`, `${VAR}`, `$?`, `$!`, `$$`, `$0`–`$9`, `$@`, `$#`
     * in the given [input] string.  Backslash-escaped dollars (`\$`) are
     * preserved literally.
     */
    fun expandVariables(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            if (input[i] == '\\' && i + 1 < input.length && input[i + 1] == '$') {
                sb.append('$'); i += 2; continue
            }
            if (input[i] != '$') { sb.append(input[i]); i++; continue }

            i++ // skip '$'
            if (i >= input.length) { sb.append('$'); break }

            when {
                input[i] == '{' -> {
                    val end = input.indexOf('}', i + 1)
                    if (end == -1) { sb.append("\${"); i++ }
                    else { sb.append(resolveVariable(input.substring(i + 1, end))); i = end + 1 }
                }
                input[i] == '?' -> { sb.append(lastExitCode); i++ }
                input[i] == '!' -> { sb.append(lastBackgroundPid); i++ }
                input[i] == '$' -> { sb.append(shellPid); i++ }
                input[i] == '@' -> { sb.append(positionalArgs.joinToString(" ")); i++ }
                input[i] == '#' -> { sb.append(positionalArgs.size); i++ }
                input[i] == '0' -> { sb.append(shellName); i++ }
                input[i] in '1'..'9' -> {
                    sb.append(positionalArgs.getOrElse(input[i] - '1') { "" }); i++
                }
                input[i].isLetter() || input[i] == '_' -> {
                    val start = i
                    while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_')) i++
                    sb.append(variables[input.substring(start, i)] ?: "")
                }
                else -> sb.append('$')
            }
        }
        return sb.toString()
    }

    private fun resolveVariable(name: String): String = when (name) {
        "?" -> lastExitCode.toString()
        "!" -> lastBackgroundPid.toString()
        "$" -> shellPid.toString()
        "@" -> positionalArgs.joinToString(" ")
        "#" -> positionalArgs.size.toString()
        "0" -> shellName
        else -> if (name.length == 1 && name[0] in '1'..'9')
            positionalArgs.getOrElse(name[0] - '1') { "" }
        else variables[name] ?: ""
    }

    // ─────────────────── aliases ────────────────────────────────────

    private val aliases = mutableMapOf<String, String>(
        "ll" to "ls -la",
        "la" to "ls -a"
    )

    fun getAlias(name: String): String? = aliases[name]
    fun setAlias(name: String, value: String) { aliases[name] = value }
    fun removeAlias(name: String): Boolean = aliases.remove(name) != null
    fun getAllAliases(): Map<String, String> = aliases.toMap()

    // ─────────────────── command history ─────────────────────────────

    private val history = mutableListOf<String>()

    fun addToHistory(command: String) {
        if (command.isNotBlank() && (history.isEmpty() || history.last() != command)) {
            history.add(command)
        }
    }

    fun getHistory(): List<String> = history.toList()
    fun getHistoryEntry(index: Int): String? = history.getOrNull(index)
    fun historySize(): Int = history.size
    fun clearHistory() { history.clear() }

    // ─────────────────── command registry ────────────────────────────

    private val registeredCommands = mutableMapOf<String, Command>()

    fun registerCommand(command: Command) { registeredCommands[command.name] = command }
    fun getCommand(name: String): Command? = registeredCommands[name]
    fun getAllCommands(): Map<String, Command> = registeredCommands.toMap()

    // ─────────────────── umask ──────────────────────────────────────

    var umask: Int = 22
        private set

    fun setUmask(mask: Int) { umask = mask }

    // ─────────────────── PATH helpers ───────────────────────────────

    fun getPathDirectories(): List<String> =
        (variables["PATH"] ?: "").split(":").filter { it.isNotEmpty() }
}
