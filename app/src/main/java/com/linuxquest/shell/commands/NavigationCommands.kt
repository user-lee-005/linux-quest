package com.linuxquest.shell.commands

import com.linuxquest.filesystem.VfsException
import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment

// ──────────────────────────────────────────────────────────────
// cd
// ──────────────────────────────────────────────────────────────
class CdCommand : Command {
    override val name = "cd"
    override val description = "Change the current working directory"
    override val usage = "cd [dir]"
    override val manPage = """
NAME
    cd - change the working directory

SYNOPSIS
    cd [dir]

DESCRIPTION
    Change the current directory to DIR.  The default DIR is the value of
    the HOME shell variable.

    The variable OLDPWD is set to the previous working directory.

    If DIR is -, changes to the previous directory (${'$'}OLDPWD) and prints
    the new working directory.

EXAMPLES
    cd
    cd /home/user
    cd ..
    cd -
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val target = when {
            args.isEmpty() -> env.getVariable("HOME") ?: "/home/${vfs.currentUser.name}"
            args[0] == "-" -> {
                val oldpwd = env.getVariable("OLDPWD")
                    ?: return CommandResult(stderr = "cd: OLDPWD not set", exitCode = 1)
                oldpwd
            }
            else -> args[0]
        }

        val previousDir = vfs.currentPath

        return try {
            vfs.cd(target)
            env.setVariable("OLDPWD", previousDir)
            env.setVariable("PWD", vfs.currentPath)

            if (args.firstOrNull() == "-") {
                CommandResult(stdout = vfs.currentPath)
            } else {
                CommandResult()
            }
        } catch (e: VfsException) {
            CommandResult(stderr = "cd: ${e.message}", exitCode = 1)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// pwd
// ──────────────────────────────────────────────────────────────
class PwdCommand : Command {
    override val name = "pwd"
    override val description = "Print the current working directory"
    override val usage = "pwd"
    override val manPage = """
NAME
    pwd - print name of current/working directory

SYNOPSIS
    pwd

DESCRIPTION
    Print the full filename of the current working directory.

EXAMPLES
    pwd
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        return CommandResult(stdout = vfs.pwd())
    }
}
