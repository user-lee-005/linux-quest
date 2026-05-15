package com.linuxquest.shell

import com.linuxquest.filesystem.VfsException
import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.commands.*

/**
 * Main REPL engine that ties the parser, executor, registry, and
 * environment together.
 *
 * All commands operate against the [VirtualFileSystem]; no real OS
 * processes are ever spawned.
 */
class ShellInterpreter(
    private val vfs: VirtualFileSystem,
    val env: ShellEnvironment = ShellEnvironment(),
    val registry: CommandRegistry = CommandRegistry(env)
) {
    private val executor = PipelineExecutor()

    init {
        syncEnvironmentFromVfs()
        registerAllCommands()
    }

    // ========================== public API ==========================

    /** Parse and execute a single line of shell input. */
    fun execute(input: String): CommandResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return CommandResult()

        env.addToHistory(trimmed)
        val expanded = expandAliases(trimmed)

        val parser = CommandParser(
            env = env,
            vfs = vfs,
            commandSubstituter = { cmd -> execute(cmd).stdout }
        )

        val command = try {
            parser.parse(expanded)
        } catch (e: ShellParseException) {
            env.lastExitCode = 2
            return CommandResult(stderr = "bash: syntax error: ${e.message}", exitCode = 2)
        } catch (e: Exception) {
            env.lastExitCode = 2
            return CommandResult(stderr = "bash: ${e.message}", exitCode = 2)
        } ?: return CommandResult()

        val result = executor.execute(command, vfs, env)
        env.lastExitCode = result.exitCode
        return result
    }

    /** Execute a multi-line script (handles line continuations). */
    fun executeScript(script: String): CommandResult {
        var lastResult = CommandResult()
        val lines = script.lines()
        var i = 0
        while (i < lines.size) {
            var line = lines[i].trim()
            i++
            if (line.isEmpty() || line.startsWith("#")) continue
            while (line.endsWith("\\") && i < lines.size) {
                line = line.dropLast(1) + lines[i].trim(); i++
            }
            lastResult = execute(line)
        }
        return lastResult
    }

    /** Build the prompt string by expanding PS1 escape sequences. */
    fun getPrompt(): String = expandPS1(env.getVariable("PS1") ?: "$ ")

    // ========================== alias expansion =====================

    private fun expandAliases(input: String): String {
        val parts = input.split(Regex("\\s+"), limit = 2)
        val first = parts.getOrNull(0) ?: return input
        val rest = parts.getOrNull(1)
        val seen = mutableSetOf<String>()
        var current = first
        while (current !in seen) {
            val alias = env.getAlias(current) ?: break
            seen += current; current = alias
        }
        return if (seen.isEmpty()) input
        else if (rest != null) "$current $rest" else current
    }

    // ========================== PS1 expansion =======================

    private fun expandPS1(ps1: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < ps1.length) {
            if (ps1[i] == '\\' && i + 1 < ps1.length) {
                i++
                when (ps1[i]) {
                    'u' -> sb.append(env.getVariable("USER") ?: "user")
                    'h' -> sb.append(env.getVariable("HOSTNAME") ?: "linuxquest")
                    'H' -> sb.append("${env.getVariable("HOSTNAME") ?: "linuxquest"}.local")
                    'w' -> {
                        val pwd = env.getVariable("PWD") ?: "/"
                        val home = env.getVariable("HOME") ?: ""
                        if (home.isNotEmpty() && pwd.startsWith(home))
                            sb.append("~").append(pwd.removePrefix(home))
                        else sb.append(pwd)
                    }
                    'W' -> {
                        val pwd = env.getVariable("PWD") ?: "/"
                        sb.append(pwd.substringAfterLast('/').ifEmpty { "/" })
                    }
                    '$' -> sb.append(if (env.getVariable("USER") == "root") "#" else "$")
                    'n' -> sb.append('\n')
                    '\\' -> sb.append('\\')
                    '[' -> { /* begin non-printing — ignore */ }
                    ']' -> { /* end non-printing — ignore */ }
                    else -> { sb.append('\\'); sb.append(ps1[i]) }
                }
                i++
            } else { sb.append(ps1[i]); i++ }
        }
        return sb.toString()
    }

    // ========================== VFS sync ============================

    private fun syncEnvironmentFromVfs() {
        env.setVariable("PWD", vfs.currentPath)
        env.setVariable("USER", vfs.currentUser.name)
        env.setVariable("HOME", vfs.currentUser.home)
    }

    // ========================== command registration =================

    private fun registerAllCommands() {
        // file commands
        registry.registerAll(listOf(
            LsCommand(), CatCommand(), CpCommand(), MvCommand(), RmCommand(),
            MkdirCommand(), RmdirCommand(), TouchCommand(), FileTypeCommand(),
            LnCommand(), StatCommand()
        ))
        // navigation
        registry.registerAll(listOf(CdCommand(), PwdCommand()))
        // text processing
        registry.registerAll(listOf(
            HeadCommand(), TailCommand(), SortCommand(), UniqCommand(),
            WcCommand(), CutCommand(), TrCommand(), SedCommand(), AwkCommand(),
            DiffCommand(), TeeCommand(), XargsCommand()
        ))
        // search
        registry.registerAll(listOf(FindCommand(), GrepCommand(), WhichCommand()))
        // permissions / users
        registry.registerAll(listOf(
            ChmodCommand(), ChownCommand(), ChgrpCommand(), UmaskCommand(),
            IdCommand(), WhoamiCommand(), GroupsCommand(), SuCommand(), SudoCommand()
        ))
        // system
        registry.registerAll(listOf(
            EchoCommand(), EnvCommand(), ExportCommand(), AliasCommand(),
            UnaliasCommand(), HistoryCommand(), ClearCommand(), ManCommand(),
            HelpCommand(), TypeCommand(), DateCommand(), CalCommand(),
            UnameCommand(), DuCommand()
        ))
        // network
        registry.registerAll(listOf(
            PingCommand(), CurlCommand(), NcCommand(), SshCommand(),
            IfconfigCommand(), NetstatCommand()
        ))
        // archive / encoding
        registry.registerAll(listOf(
            TarCommand(), Base64Command(), XxdCommand(), StringsCommand(),
            GzipCommand(), GunzipCommand()
        ))
        // process (simulated)
        registry.registerAll(listOf(
            PsCommand(), TopCommand(), KillCommand(), JobsCommand()
        ))
        // shell builtins that don't exist as standalone Command classes
        registry.registerAll(listOf(
            SourceBuiltin(), ExitBuiltin(), UnsetBuiltin(),
            TrueBuiltin(), FalseBuiltin(), SetBuiltin()
        ))
    }

    // ═══════════════════════════════════════════════════════════════
    //  Shell-level builtins (need interpreter / env access)
    // ═══════════════════════════════════════════════════════════════

    // ── source / . ──────────────────────────────────────────────────

    private inner class SourceBuiltin : Command {
        override val name = "source"
        override val description = "Execute commands from a file in the current shell"
        override val usage = "source filename [args]"
        override val manPage = """
NAME
    source - execute commands from a file in the current shell

SYNOPSIS
    source filename [arguments]
    . filename [arguments]

DESCRIPTION
    Read and execute commands from FILENAME in the current shell
    environment. If FILENAME does not contain a slash, use PATH to
    find the file.
""".trimIndent()

        override fun execute(args: List<String>, stdin: String?,
                             vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
            if (args.isEmpty())
                return CommandResult(stderr = "bash: source: filename argument required", exitCode = 2)
            val path = if (args[0].startsWith("/")) args[0]
            else vfs.getAbsolutePath("${env.getVariable("PWD") ?: "/"}/${args[0]}")

            if (!vfs.exists(path) || !vfs.isFile(path))
                return CommandResult(stderr = "bash: ${args[0]}: No such file or directory", exitCode = 1)

            val content = try { vfs.readFileText(path) } catch (e: Exception) {
                return CommandResult(stderr = "bash: ${args[0]}: ${e.message}", exitCode = 1)
            }
            val saved = env.positionalArgs
            if (args.size > 1) env.positionalArgs = args.drop(1)
            val result = this@ShellInterpreter.executeScript(content)
            env.positionalArgs = saved
            return result
        }
    }

    // ── exit ────────────────────────────────────────────────────────

    private inner class ExitBuiltin : Command {
        override val name = "exit"
        override val description = "Exit the shell"
        override val usage = "exit [n]"
        override val manPage = """
NAME
    exit - cause the shell to exit

SYNOPSIS
    exit [n]

DESCRIPTION
    Exit the shell with a status of N.  If N is omitted, the exit
    status is that of the last command executed.
""".trimIndent()

        override fun execute(args: List<String>, stdin: String?,
                             vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
            val code = args.firstOrNull()?.toIntOrNull() ?: env.lastExitCode
            return CommandResult(stdout = "exit\n", exitCode = code)
        }
    }

    // ── unset ───────────────────────────────────────────────────────

    private inner class UnsetBuiltin : Command {
        override val name = "unset"
        override val description = "Unset values of variables and functions"
        override val usage = "unset name ..."
        override val manPage = """
NAME
    unset - unset values and attributes of shell variables

SYNOPSIS
    unset [-v] name [name ...]

DESCRIPTION
    For each NAME, remove the corresponding variable.
""".trimIndent()

        override fun execute(args: List<String>, stdin: String?,
                             vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
            for (name in args) {
                if (name != "-v") env.removeVariable(name)
            }
            return CommandResult()
        }
    }

    // ── true / false ────────────────────────────────────────────────

    private inner class TrueBuiltin : Command {
        override val name = "true"
        override val description = "Return a successful exit status"
        override val usage = "true"
        override val manPage = "true\n  Do nothing, return exit code 0."
        override fun execute(args: List<String>, stdin: String?,
                             vfs: VirtualFileSystem, env: ShellEnvironment) = CommandResult()
    }

    private inner class FalseBuiltin : Command {
        override val name = "false"
        override val description = "Return an unsuccessful exit status"
        override val usage = "false"
        override val manPage = "false\n  Do nothing, return exit code 1."
        override fun execute(args: List<String>, stdin: String?,
                             vfs: VirtualFileSystem, env: ShellEnvironment) = CommandResult(exitCode = 1)
    }

    // ── set ─────────────────────────────────────────────────────────

    private inner class SetBuiltin : Command {
        override val name = "set"
        override val description = "Set or display shell variables and positional parameters"
        override val usage = "set [-- args ...]"
        override val manPage = """
NAME
    set - set or unset values of shell options and positional parameters

SYNOPSIS
    set [-- arg ...]

DESCRIPTION
    Without arguments, print all shell variables.
    With --, set positional parameters to the remaining arguments.
""".trimIndent()

        override fun execute(args: List<String>, stdin: String?,
                             vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
            if (args.isEmpty()) {
                val out = env.getAllVariables().entries
                    .sortedBy { it.key }
                    .joinToString("\n") { "${it.key}=${it.value}" } + "\n"
                return CommandResult(stdout = out)
            }
            if (args[0] == "--") {
                env.positionalArgs = args.drop(1)
            }
            return CommandResult()
        }
    }
}
