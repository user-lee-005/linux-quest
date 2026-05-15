package com.linuxquest.shell.commands

import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment
import java.text.SimpleDateFormat
import java.util.*

class EchoCommand : Command {
    override val name = "echo"
    override val description = "Display a line of text"
    override val usage = "echo [-n] [-e] [string...]"
    override val manPage = """
NAME
    echo - display a line of text

SYNOPSIS
    echo [-n] [-e] [STRING...]

OPTIONS
    -n    Do not output trailing newline
    -e    Enable interpretation of backslash escapes
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var noNewline = false; var interpretEscapes = false
        val textParts = mutableListOf<String>()
        for (a in args) {
            when (a) {
                "-n" -> noNewline = true
                "-e" -> interpretEscapes = true
                "-ne", "-en" -> { noNewline = true; interpretEscapes = true }
                else -> textParts.add(a)
            }
        }
        var output = textParts.joinToString(" ")
        if (interpretEscapes) {
            output = output
                .replace("\\n", "\n").replace("\\t", "\t")
                .replace("\\\\", "\\").replace("\\a", "\u0007")
                .replace("\\033", "\u001B").replace("\\e", "\u001B")
        }
        return CommandResult(stdout = output)
    }
}

class EnvCommand : Command {
    override val name = "env"
    override val description = "Print environment variables"
    override val usage = "env"
    override val manPage = "Print the current environment variables."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val vars = env.getAllVariables()
        return CommandResult(stdout = vars.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }
}

class ExportCommand : Command {
    override val name = "export"
    override val description = "Set environment variables"
    override val usage = "export [VAR=value]"
    override val manPage = "Set or export environment variables."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) {
            val vars = env.getAllVariables()
            return CommandResult(stdout = vars.entries.joinToString("\n") { "declare -x ${it.key}=\"${it.value}\"" })
        }
        for (a in args) {
            val eqIdx = a.indexOf('=')
            if (eqIdx > 0) {
                env.setVariable(a.substring(0, eqIdx), a.substring(eqIdx + 1))
            }
        }
        return CommandResult()
    }
}

class AliasCommand : Command {
    override val name = "alias"
    override val description = "Define or display aliases"
    override val usage = "alias [name=value]"
    override val manPage = "Define or display command aliases."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) {
            val aliases = env.getAllAliases()
            return CommandResult(stdout = aliases.entries.joinToString("\n") { "alias ${it.key}='${it.value}'" })
        }
        for (a in args) {
            val eqIdx = a.indexOf('=')
            if (eqIdx > 0) {
                val name = a.substring(0, eqIdx)
                val value = a.substring(eqIdx + 1).removeSurrounding("'").removeSurrounding("\"")
                env.setAlias(name, value)
            }
        }
        return CommandResult()
    }
}

class UnaliasCommand : Command {
    override val name = "unalias"
    override val description = "Remove aliases"
    override val usage = "unalias name"
    override val manPage = "Remove alias definitions."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) return CommandResult(stderr = "unalias: missing operand", exitCode = 1)
        for (a in args) env.removeAlias(a)
        return CommandResult()
    }
}

class HistoryCommand : Command {
    override val name = "history"
    override val description = "Display command history"
    override val usage = "history"
    override val manPage = "Display the command history list."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val history = env.getHistory()
        val output = history.mapIndexed { i, cmd -> "%5d  %s".format(i + 1, cmd) }.joinToString("\n")
        return CommandResult(stdout = output)
    }
}

class ClearCommand : Command {
    override val name = "clear"
    override val description = "Clear the terminal screen"
    override val usage = "clear"
    override val manPage = "Clear the terminal screen."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        return CommandResult(stdout = "\u001B[2J\u001B[H")
    }
}

class ManCommand : Command {
    override val name = "man"
    override val description = "Display manual pages"
    override val usage = "man command"
    override val manPage = "Display the manual page for a command."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) return CommandResult(stderr = "What manual page do you want?", exitCode = 1)
        val cmdName = args.first()
        val cmd = env.getCommand(cmdName)
        return if (cmd != null) {
            CommandResult(stdout = cmd.manPage)
        } else {
            CommandResult(stderr = "No manual entry for $cmdName", exitCode = 1)
        }
    }
}

class HelpCommand : Command {
    override val name = "help"
    override val description = "Display help information"
    override val usage = "help [command]"
    override val manPage = "Display help for built-in commands."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isNotEmpty()) {
            val cmd = env.getCommand(args.first())
            return if (cmd != null) CommandResult(stdout = cmd.manPage)
            else CommandResult(stderr = "help: no help for '${args.first()}'", exitCode = 1)
        }
        val commands = env.getAllCommands().keys.toList()
        val sb = StringBuilder()
        sb.appendLine("LinuxQuest Shell — Available Commands:")
        sb.appendLine()
        commands.sorted().chunked(6).forEach { row ->
            sb.appendLine(row.joinToString("  ") { it.padEnd(14) })
        }
        sb.appendLine()
        sb.appendLine("Type 'man <command>' for detailed help on a specific command.")
        return CommandResult(stdout = sb.toString().trimEnd())
    }
}

class TypeCommand : Command {
    override val name = "type"
    override val description = "Describe a command type"
    override val usage = "type command"
    override val manPage = "Indicate how a command name is interpreted."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) return CommandResult(stderr = "type: missing argument", exitCode = 1)
        val cmd = args.first()
        val alias = env.getAlias(cmd)
        return when {
            alias != null -> CommandResult(stdout = "$cmd is aliased to '$alias'")
            cmd in listOf("cd", "export", "alias", "unalias", "source", "exit") ->
                CommandResult(stdout = "$cmd is a shell builtin")
            env.getCommand(cmd) != null -> CommandResult(stdout = "$cmd is /usr/bin/$cmd")
            else -> CommandResult(stderr = "type: $cmd: not found", exitCode = 1)
        }
    }
}

class DateCommand : Command {
    override val name = "date"
    override val description = "Display date and time"
    override val usage = "date [+format]"
    override val manPage = "Display the current date and time."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val now = Date()
        if (args.isNotEmpty() && args[0].startsWith("+")) {
            var fmt = args[0].drop(1)
            fmt = fmt.replace("%Y", SimpleDateFormat("yyyy", Locale.US).format(now))
                .replace("%m", SimpleDateFormat("MM", Locale.US).format(now))
                .replace("%d", SimpleDateFormat("dd", Locale.US).format(now))
                .replace("%H", SimpleDateFormat("HH", Locale.US).format(now))
                .replace("%M", SimpleDateFormat("mm", Locale.US).format(now))
                .replace("%S", SimpleDateFormat("ss", Locale.US).format(now))
                .replace("%s", (now.time / 1000).toString())
            return CommandResult(stdout = fmt)
        }
        return CommandResult(stdout = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(now))
    }
}

class CalCommand : Command {
    override val name = "cal"
    override val description = "Display a calendar"
    override val usage = "cal [month] [year]"
    override val manPage = "Display a calendar for the current or specified month."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val cal = Calendar.getInstance()
        val month = args.getOrNull(0)?.toIntOrNull()?.minus(1) ?: cal.get(Calendar.MONTH)
        val year = args.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        cal.set(year, month, 1)

        val monthName = SimpleDateFormat("MMMM", Locale.US).format(cal.time)
        val sb = StringBuilder()
        sb.appendLine("    $monthName $year".padStart(20))
        sb.appendLine("Su Mo Tu We Th Fr Sa")

        val startDay = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        sb.append("   ".repeat(startDay))
        for (day in 1..daysInMonth) {
            sb.append("%2d ".format(day))
            if ((startDay + day) % 7 == 0) sb.appendLine()
        }
        return CommandResult(stdout = sb.toString().trimEnd())
    }
}

class UnameCommand : Command {
    override val name = "uname"
    override val description = "Print system information"
    override val usage = "uname [-a]"
    override val manPage = "Print system information (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val all = args.any { it == "-a" }
        return if (all) {
            CommandResult(stdout = "Linux linuxquest 5.15.0-quest #1 SMP PREEMPT LinuxQuest x86_64 GNU/Linux")
        } else {
            CommandResult(stdout = "Linux")
        }
    }
}

class DuCommand : Command {
    override val name = "du"
    override val description = "Estimate file space usage"
    override val usage = "du [-h] [-s] [path]"
    override val manPage = "Estimate file space usage (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var human = false; var summary = false; var path = "."
        for (a in args) {
            when (a) {
                "-h" -> human = true; "-s" -> summary = true
                "-sh", "-hs" -> { human = true; summary = true }
                else -> if (!a.startsWith("-")) path = a
            }
        }
        return try {
            val size = calculateSize(vfs, vfs.getAbsolutePath(path))
            val sizeStr = if (human) humanSize(size) else size.toString()
            if (summary) {
                CommandResult(stdout = "$sizeStr\t$path")
            } else {
                val sb = StringBuilder()
                if (vfs.isDirectory(vfs.getAbsolutePath(path))) {
                    listDu(vfs, vfs.getAbsolutePath(path), human, sb)
                }
                sb.append("$sizeStr\t$path")
                CommandResult(stdout = sb.toString())
            }
        } catch (e: Exception) {
            CommandResult(stderr = "du: $path: ${e.message}", exitCode = 1)
        }
    }

    private fun calculateSize(vfs: VirtualFileSystem, path: String): Long {
        val node = vfs.getNode(path) ?: return 0
        return if (vfs.isFile(path)) {
            node.size
        } else {
            var total = 4096L
            vfs.listDirectory(path).forEach { child ->
                total += calculateSize(vfs, if (path == "/") "/${child.name}" else "$path/${child.name}")
            }
            total
        }
    }

    private fun listDu(vfs: VirtualFileSystem, path: String, human: Boolean, sb: StringBuilder) {
        vfs.listDirectory(path).forEach { child ->
            val childPath = if (path == "/") "/${child.name}" else "$path/${child.name}"
            if (vfs.isDirectory(childPath)) {
                val size = calculateSize(vfs, childPath)
                val sizeStr = if (human) humanSize(size) else size.toString()
                listDu(vfs, childPath, human, sb)
                sb.appendLine("$sizeStr\t$childPath")
            }
        }
    }

    private fun humanSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1fG".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1fM".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1fK".format(bytes / 1024.0)
        else -> "${bytes}B"
    }
}
