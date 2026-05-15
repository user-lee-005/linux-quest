package com.linuxquest.shell.commands

import com.linuxquest.filesystem.Permissions
import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment

class ChmodCommand : Command {
    override val name = "chmod"
    override val description = "Change file mode bits"
    override val usage = "chmod [-R] mode file..."
    override val manPage = """
NAME
    chmod - change file mode bits

SYNOPSIS
    chmod [-R] MODE FILE...

DESCRIPTION
    Change the permissions of files. MODE can be octal (755) or symbolic (u+x, g-w).

OPTIONS
    -R    Recursive
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var recursive = false
        val positional = mutableListOf<String>()
        for (a in args) {
            when (a) { "-R" -> recursive = true; else -> positional.add(a) }
        }
        if (positional.size < 2) return CommandResult(stderr = "chmod: missing operand", exitCode = 1)
        val mode = positional[0]
        val files = positional.drop(1)

        for (f in files) {
            try {
                val octal = mode.toIntOrNull()
                if (octal != null) {
                    vfs.chmod(f, Permissions.fromOctal(octal))
                } else {
                    val node = vfs.getNode(f) ?: return CommandResult(stderr = "chmod: $f: No such file", exitCode = 1)
                    val newPerms = applySymbolicMode(mode, node.permissions)
                    vfs.chmod(f, newPerms)
                }
                if (recursive && vfs.isDirectory(f)) {
                    chmodRecursive(vfs, f, mode)
                }
            } catch (e: Exception) {
                return CommandResult(stderr = "chmod: $f: ${e.message}", exitCode = 1)
            }
        }
        return CommandResult()
    }

    private fun chmodRecursive(vfs: VirtualFileSystem, path: String, mode: String) {
        vfs.listDirectory(path).forEach { node ->
            val childPath = if (path == "/") "/${node.name}" else "$path/${node.name}"
            val octal = mode.toIntOrNull()
            if (octal != null) vfs.chmod(childPath, Permissions.fromOctal(octal))
            else vfs.chmod(childPath, applySymbolicMode(mode, node.permissions))
            if (vfs.isDirectory(childPath)) chmodRecursive(vfs, childPath, mode)
        }
    }

    private fun applySymbolicMode(mode: String, current: Permissions): Permissions {
        var p = current
        val parts = mode.split(",")
        for (part in parts) {
            val match = Regex("([ugoa]*)([+\\-=])([rwxst]*)").find(part) ?: continue
            val who = match.groupValues[1].ifEmpty { "a" }
            val op = match.groupValues[2]
            val what = match.groupValues[3]

            val r = 'r' in what; val w = 'w' in what; val x = 'x' in what
            val s = 's' in what; val t = 't' in what

            for (target in who) {
                p = when (target) {
                    'u', 'a' -> {
                        var tmp = p
                        when (op) {
                            "+" -> { if (r) tmp = tmp.copy(ownerRead = true); if (w) tmp = tmp.copy(ownerWrite = true); if (x) tmp = tmp.copy(ownerExecute = true); if (s) tmp = tmp.copy(suid = true) }
                            "-" -> { if (r) tmp = tmp.copy(ownerRead = false); if (w) tmp = tmp.copy(ownerWrite = false); if (x) tmp = tmp.copy(ownerExecute = false); if (s) tmp = tmp.copy(suid = false) }
                            "=" -> { tmp = tmp.copy(ownerRead = r, ownerWrite = w, ownerExecute = x) }
                        }
                        if (target == 'a') {
                            when (op) {
                                "+" -> { if (r) tmp = tmp.copy(groupRead = true, otherRead = true); if (w) tmp = tmp.copy(groupWrite = true, otherWrite = true); if (x) tmp = tmp.copy(groupExecute = true, otherExecute = true) }
                                "-" -> { if (r) tmp = tmp.copy(groupRead = false, otherRead = false); if (w) tmp = tmp.copy(groupWrite = false, otherWrite = false); if (x) tmp = tmp.copy(groupExecute = false, otherExecute = false) }
                                "=" -> { tmp = tmp.copy(groupRead = r, groupWrite = w, groupExecute = x, otherRead = r, otherWrite = w, otherExecute = x) }
                            }
                        }
                        tmp
                    }
                    'g' -> {
                        when (op) {
                            "+" -> { var tmp = p; if (r) tmp = tmp.copy(groupRead = true); if (w) tmp = tmp.copy(groupWrite = true); if (x) tmp = tmp.copy(groupExecute = true); if (s) tmp = tmp.copy(sgid = true); tmp }
                            "-" -> { var tmp = p; if (r) tmp = tmp.copy(groupRead = false); if (w) tmp = tmp.copy(groupWrite = false); if (x) tmp = tmp.copy(groupExecute = false); if (s) tmp = tmp.copy(sgid = false); tmp }
                            "=" -> p.copy(groupRead = r, groupWrite = w, groupExecute = x)
                            else -> p
                        }
                    }
                    'o' -> {
                        when (op) {
                            "+" -> { var tmp = p; if (r) tmp = tmp.copy(otherRead = true); if (w) tmp = tmp.copy(otherWrite = true); if (x) tmp = tmp.copy(otherExecute = true); if (t) tmp = tmp.copy(sticky = true); tmp }
                            "-" -> { var tmp = p; if (r) tmp = tmp.copy(otherRead = false); if (w) tmp = tmp.copy(otherWrite = false); if (x) tmp = tmp.copy(otherExecute = false); if (t) tmp = tmp.copy(sticky = false); tmp }
                            "=" -> p.copy(otherRead = r, otherWrite = w, otherExecute = x)
                            else -> p
                        }
                    }
                    else -> p
                }
            }
        }
        return p
    }
}

class ChownCommand : Command {
    override val name = "chown"
    override val description = "Change file owner and group"
    override val usage = "chown [-R] owner[:group] file..."
    override val manPage = "Change ownership of files. Use owner:group format."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var recursive = false
        val positional = mutableListOf<String>()
        for (a in args) { if (a == "-R") recursive = true else positional.add(a) }
        if (positional.size < 2) return CommandResult(stderr = "chown: missing operand", exitCode = 1)
        val spec = positional[0]
        val parts = spec.split(":")
        val owner = parts[0].ifEmpty { null }
        val group = parts.getOrNull(1)?.ifEmpty { null }
        for (f in positional.drop(1)) {
            try { vfs.chown(f, owner ?: vfs.getNode(f)?.owner ?: "root", group)
                if (recursive && vfs.isDirectory(f)) chownRecursive(vfs, f, owner, group)
            } catch (e: Exception) { return CommandResult(stderr = "chown: $f: ${e.message}", exitCode = 1) }
        }
        return CommandResult()
    }

    private fun chownRecursive(vfs: VirtualFileSystem, path: String, owner: String?, group: String?) {
        vfs.listDirectory(path).forEach { node ->
            val cp = if (path == "/") "/${node.name}" else "$path/${node.name}"
            vfs.chown(cp, owner ?: node.owner, group)
            if (vfs.isDirectory(cp)) chownRecursive(vfs, cp, owner, group)
        }
    }
}

class ChgrpCommand : Command {
    override val name = "chgrp"
    override val description = "Change group ownership"
    override val usage = "chgrp [-R] group file..."
    override val manPage = "Change group ownership of files."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var recursive = false
        val positional = mutableListOf<String>()
        for (a in args) { if (a == "-R") recursive = true else positional.add(a) }
        if (positional.size < 2) return CommandResult(stderr = "chgrp: missing operand", exitCode = 1)
        val group = positional[0]
        for (f in positional.drop(1)) {
            try { vfs.chown(f, vfs.getNode(f)?.owner ?: "root", group)
                if (recursive && vfs.isDirectory(f)) chgrpRecursive(vfs, f, group)
            } catch (e: Exception) { return CommandResult(stderr = "chgrp: $f: ${e.message}", exitCode = 1) }
        }
        return CommandResult()
    }

    private fun chgrpRecursive(vfs: VirtualFileSystem, path: String, group: String) {
        vfs.listDirectory(path).forEach { node ->
            val cp = if (path == "/") "/${node.name}" else "$path/${node.name}"
            vfs.chown(cp, node.owner, group)
            if (vfs.isDirectory(cp)) chgrpRecursive(vfs, cp, group)
        }
    }
}

class UmaskCommand : Command {
    override val name = "umask"
    override val description = "Set file mode creation mask"
    override val usage = "umask [mask]"
    override val manPage = "Display or set the file mode creation mask."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) {
            val mask = env.getVariable("UMASK") ?: "0022"
            return CommandResult(stdout = mask)
        }
        env.setVariable("UMASK", args[0])
        return CommandResult()
    }
}

class IdCommand : Command {
    override val name = "id"
    override val description = "Print user and group IDs"
    override val usage = "id [user]"
    override val manPage = "Print real and effective user and group IDs."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val username = args.firstOrNull() ?: (env.getVariable("USER") ?: "bandit0")
        val uid = if (username == "root") 0 else username.removePrefix("bandit").toIntOrNull()?.plus(1000) ?: 1000
        val gid = uid
        return CommandResult(stdout = "uid=$uid($username) gid=$gid($username) groups=$gid($username)")
    }
}

class WhoamiCommand : Command {
    override val name = "whoami"
    override val description = "Print effective username"
    override val usage = "whoami"
    override val manPage = "Print the user name associated with the current effective user ID."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        return CommandResult(stdout = env.getVariable("USER") ?: "bandit0")
    }
}

class GroupsCommand : Command {
    override val name = "groups"
    override val description = "Print group memberships"
    override val usage = "groups [user]"
    override val manPage = "Print the groups a user is in."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val user = args.firstOrNull() ?: (env.getVariable("USER") ?: "bandit0")
        return CommandResult(stdout = "$user : $user users")
    }
}

class SuCommand : Command {
    override val name = "su"
    override val description = "Switch user"
    override val usage = "su [user]"
    override val manPage = "Switch to another user (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val target = args.firstOrNull() ?: "root"
        env.setVariable("USER", target)
        env.setVariable("HOME", if (target == "root") "/root" else "/home/$target")
        return CommandResult(stdout = "Switched to user $target")
    }
}

class SudoCommand : Command {
    override val name = "sudo"
    override val description = "Execute a command as another user"
    override val usage = "sudo command [args]"
    override val manPage = "Execute a command as the superuser (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) return CommandResult(stderr = "sudo: no command specified", exitCode = 1)
        val prevUser = env.getVariable("USER") ?: "bandit0"
        env.setVariable("USER", "root")
        val result = CommandResult(stdout = "[sudo] running '${args.joinToString(" ")}' as root")
        env.setVariable("USER", prevUser)
        return result
    }
}
