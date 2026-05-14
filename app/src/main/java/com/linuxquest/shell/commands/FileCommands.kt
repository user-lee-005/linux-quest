package com.linuxquest.shell.commands

import com.linuxquest.filesystem.*
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────
// ANSI colour helpers
// ──────────────────────────────────────────────────────────────
private const val ANSI_RESET = "\u001B[0m"
private const val ANSI_BLUE = "\u001B[1;34m"
private const val ANSI_GREEN = "\u001B[1;32m"
private const val ANSI_CYAN = "\u001B[1;36m"

private fun colorize(node: VNode, text: String): String = when {
    node is SymlinkNode -> "$ANSI_CYAN$text$ANSI_RESET"
    node is DirectoryNode -> "$ANSI_BLUE$text$ANSI_RESET"
    node is FileNode && node.permissions.ownerExecute -> "$ANSI_GREEN$text$ANSI_RESET"
    else -> text
}

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1fG".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1fM".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.1fK".format(bytes / 1024.0)
    else -> "${bytes}B"
}

private fun nodeSize(node: VNode): Long = when (node) {
    is FileNode -> node.size
    is DirectoryNode -> node.size
    is SymlinkNode -> node.name.length.toLong()
}

private val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US)

// ──────────────────────────────────────────────────────────────
// ls
// ──────────────────────────────────────────────────────────────
class LsCommand : Command {
    override val name = "ls"
    override val description = "List directory contents"
    override val usage = "ls [-l] [-a] [-h] [-R] [-t] [-S] [path]"
    override val manPage = """
NAME
    ls - list directory contents

SYNOPSIS
    ls [-l] [-a] [-h] [-R] [-t] [-S] [path]

DESCRIPTION
    List information about the FILEs (the current directory by default).
    Sort entries alphabetically unless -t or -S is specified.

OPTIONS
    -l    Use a long listing format
    -a    Do not ignore entries starting with .
    -h    With -l, print human-readable sizes (e.g., 1K, 234M)
    -R    List subdirectories recursively
    -t    Sort by modification time, newest first
    -S    Sort by file size, largest first

EXAMPLES
    ls
    ls -la /home
    ls -lhR /etc
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val flags = listOf(
            CommandFlag('l', "long"),
            CommandFlag('a', "all"),
            CommandFlag('h', "human-readable"),
            CommandFlag('R', "recursive"),
            CommandFlag('t', "time"),
            CommandFlag('S', "size-sort")
        )
        val (matched, positional) = parseFlags(args, flags)
        val long = "long" in matched
        val all = "all" in matched
        val human = "human-readable" in matched
        val recursive = "recursive" in matched
        val sortTime = "time" in matched
        val sortSize = "size-sort" in matched

        val path = positional.firstOrNull() ?: "."
        return try {
            val out = StringBuilder()
            listDir(path, vfs, long, all, human, recursive, sortTime, sortSize, out, showHeader = false)
            CommandResult(stdout = out.toString().trimEnd('\n'))
        } catch (e: VfsException) {
            CommandResult(stderr = "ls: ${e.message}", exitCode = 1)
        }
    }

    private fun listDir(
        path: String,
        vfs: VirtualFileSystem,
        long: Boolean,
        all: Boolean,
        human: Boolean,
        recursive: Boolean,
        sortTime: Boolean,
        sortSize: Boolean,
        out: StringBuilder,
        showHeader: Boolean
    ) {
        val absPath = vfs.getAbsolutePath(path)
        if (showHeader || recursive) out.appendLine("$absPath:")

        var entries = vfs.listDirectory(path)
        if (!all) entries = entries.filter { !it.name.startsWith(".") }

        entries = when {
            sortTime -> entries.sortedByDescending { it.modifiedAt }
            sortSize -> entries.sortedByDescending { nodeSize(it) }
            else -> entries.sortedBy { it.name.lowercase() }
        }

        if (long) {
            for (node in entries) {
                val size = nodeSize(node)
                val sizeStr = if (human) humanSize(size) else size.toString()
                val date = dateFormat.format(Date(node.modifiedAt))
                val nameStr = colorize(node, node.name)
                val suffix = if (node is SymlinkNode) " -> ${node.target}" else ""
                out.appendLine(
                    "${node.modeString()} ${node.owner.padEnd(8)} ${node.group.padEnd(8)} ${sizeStr.padStart(8)} $date $nameStr$suffix"
                )
            }
        } else {
            out.appendLine(entries.joinToString("  ") { colorize(it, it.name) })
        }

        if (recursive) {
            for (node in entries) {
                if (node is DirectoryNode) {
                    out.appendLine()
                    listDir("$absPath/${node.name}", vfs, long, all, human, recursive, sortTime, sortSize, out, showHeader = true)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// cat
// ──────────────────────────────────────────────────────────────
class CatCommand : Command {
    override val name = "cat"
    override val description = "Concatenate and print files"
    override val usage = "cat [-n] [file...]"
    override val manPage = """
NAME
    cat - concatenate files and print on the standard output

SYNOPSIS
    cat [-n] [file...]

DESCRIPTION
    Concatenate FILE(s) to standard output.
    With no FILE, or when FILE is -, read standard input.

OPTIONS
    -n    Number all output lines

EXAMPLES
    cat file.txt
    cat -n file.txt
    cat file1.txt file2.txt
    echo "hello" | cat
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val flags = listOf(CommandFlag('n', "number"))
        val (matched, positional) = parseFlags(args, flags)
        val number = "number" in matched

        return try {
            val content = if (positional.isEmpty()) {
                stdin ?: ""
            } else {
                val sb = StringBuilder()
                for (file in positional) {
                    if (file == "-") {
                        sb.append(stdin ?: "")
                    } else {
                        val bytes = vfs.readFile(file)
                        sb.append(String(bytes))
                    }
                }
                sb.toString()
            }

            val output = if (number) {
                content.lines().mapIndexed { i, line ->
                    "%6d\t%s".format(i + 1, line)
                }.joinToString("\n")
            } else {
                content
            }

            CommandResult(stdout = output)
        } catch (e: VfsException) {
            CommandResult(stderr = "cat: ${e.message}", exitCode = 1)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// cp
// ──────────────────────────────────────────────────────────────
class CpCommand : Command {
    override val name = "cp"
    override val description = "Copy files and directories"
    override val usage = "cp [-r] source dest"
    override val manPage = """
NAME
    cp - copy files and directories

SYNOPSIS
    cp [-r] source dest

DESCRIPTION
    Copy SOURCE to DEST, or multiple SOURCE(s) to DIRECTORY.

OPTIONS
    -r, -R    Copy directories recursively

EXAMPLES
    cp file.txt backup.txt
    cp -r dir1 dir2
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val flags = listOf(
            CommandFlag('r', "recursive"),
            CommandFlag('R', "recursive")
        )
        val (matched, positional) = parseFlags(args, flags)
        val recursive = "recursive" in matched

        if (positional.size < 2) {
            return CommandResult(stderr = "cp: missing operand\nUsage: $usage", exitCode = 2)
        }

        val src = positional[0]
        val dst = positional[1]

        return try {
            if (vfs.isDirectory(src) && !recursive) {
                return CommandResult(stderr = "cp: omitting directory '$src' (use -r)", exitCode = 1)
            }
            vfs.copy(src, dst)
            CommandResult()
        } catch (e: VfsException) {
            CommandResult(stderr = "cp: ${e.message}", exitCode = 1)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// mv
// ──────────────────────────────────────────────────────────────
class MvCommand : Command {
    override val name = "mv"
    override val description = "Move or rename files and directories"
    override val usage = "mv source dest"
    override val manPage = """
NAME
    mv - move (rename) files

SYNOPSIS
    mv source dest

DESCRIPTION
    Rename SOURCE to DEST, or move SOURCE to DIRECTORY.

EXAMPLES
    mv old.txt new.txt
    mv file.txt /home/user/
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        if (args.size < 2) {
            return CommandResult(stderr = "mv: missing operand\nUsage: $usage", exitCode = 2)
        }
        val src = args[0]
        val dst = args[1]
        return try {
            vfs.move(src, dst)
            CommandResult()
        } catch (e: VfsException) {
            CommandResult(stderr = "mv: ${e.message}", exitCode = 1)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// rm
// ──────────────────────────────────────────────────────────────
class RmCommand : Command {
    override val name = "rm"
    override val description = "Remove files or directories"
    override val usage = "rm [-r] [-f] file..."
    override val manPage = """
NAME
    rm - remove files or directories

SYNOPSIS
    rm [-r] [-f] file...

DESCRIPTION
    Remove (unlink) the FILE(s).

OPTIONS
    -r, -R    Remove directories and their contents recursively
    -f        Ignore nonexistent files, never prompt

EXAMPLES
    rm file.txt
    rm -rf dir/
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val flags = listOf(
            CommandFlag('r', "recursive"),
            CommandFlag('R', "recursive"),
            CommandFlag('f', "force")
        )
        val (matched, positional) = parseFlags(args, flags)
        val recursive = "recursive" in matched
        val force = "force" in matched

        if (positional.isEmpty()) {
            return CommandResult(stderr = "rm: missing operand\nUsage: $usage", exitCode = 2)
        }

        val errors = mutableListOf<String>()
        for (file in positional) {
            try {
                if (!vfs.exists(file)) {
                    if (!force) errors.add("rm: cannot remove '$file': No such file or directory")
                    continue
                }
                if (vfs.isDirectory(file) && !recursive) {
                    errors.add("rm: cannot remove '$file': Is a directory")
                    continue
                }
                vfs.delete(file, recursive)
            } catch (e: VfsException) {
                if (!force) errors.add("rm: ${e.message}")
            }
        }

        return if (errors.isEmpty()) CommandResult()
        else CommandResult(stderr = errors.joinToString("\n"), exitCode = 1)
    }
}

// ──────────────────────────────────────────────────────────────
// mkdir
// ──────────────────────────────────────────────────────────────
class MkdirCommand : Command {
    override val name = "mkdir"
    override val description = "Create directories"
    override val usage = "mkdir [-p] dir..."
    override val manPage = """
NAME
    mkdir - make directories

SYNOPSIS
    mkdir [-p] dir...

DESCRIPTION
    Create the DIRECTORY(ies), if they do not already exist.

OPTIONS
    -p    No error if existing, make parent directories as needed

EXAMPLES
    mkdir newdir
    mkdir -p path/to/deep/dir
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val flags = listOf(CommandFlag('p', "parents"))
        val (matched, positional) = parseFlags(args, flags)
        val parents = "parents" in matched

        if (positional.isEmpty()) {
            return CommandResult(stderr = "mkdir: missing operand\nUsage: $usage", exitCode = 2)
        }

        val errors = mutableListOf<String>()
        for (dir in positional) {
            try {
                if (parents) {
                    createParents(dir, vfs)
                } else {
                    vfs.createDirectory(dir)
                }
            } catch (e: VfsException) {
                if (!(parents && vfs.isDirectory(dir))) {
                    errors.add("mkdir: ${e.message}")
                }
            }
        }

        return if (errors.isEmpty()) CommandResult()
        else CommandResult(stderr = errors.joinToString("\n"), exitCode = 1)
    }

    private fun createParents(path: String, vfs: VirtualFileSystem) {
        val absPath = vfs.getAbsolutePath(path)
        val parts = absPath.removePrefix("/").split("/")
        var current = ""
        for (part in parts) {
            current = "$current/$part"
            if (!vfs.exists(current)) {
                vfs.createDirectory(current)
            } else if (!vfs.isDirectory(current)) {
                throw NotADirectoryException(current)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// rmdir
// ──────────────────────────────────────────────────────────────
class RmdirCommand : Command {
    override val name = "rmdir"
    override val description = "Remove empty directories"
    override val usage = "rmdir dir..."
    override val manPage = """
NAME
    rmdir - remove empty directories

SYNOPSIS
    rmdir dir...

DESCRIPTION
    Remove the DIRECTORY(ies), if they are empty.

EXAMPLES
    rmdir emptydir
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(stderr = "rmdir: missing operand\nUsage: $usage", exitCode = 2)
        }

        val errors = mutableListOf<String>()
        for (dir in args) {
            try {
                if (!vfs.exists(dir)) {
                    errors.add("rmdir: failed to remove '$dir': No such file or directory")
                    continue
                }
                if (!vfs.isDirectory(dir)) {
                    errors.add("rmdir: failed to remove '$dir': Not a directory")
                    continue
                }
                vfs.delete(dir, recursive = false)
            } catch (e: DirectoryNotEmptyException) {
                errors.add("rmdir: failed to remove '$dir': Directory not empty")
            } catch (e: VfsException) {
                errors.add("rmdir: ${e.message}")
            }
        }

        return if (errors.isEmpty()) CommandResult()
        else CommandResult(stderr = errors.joinToString("\n"), exitCode = 1)
    }
}

// ──────────────────────────────────────────────────────────────
// touch
// ──────────────────────────────────────────────────────────────
class TouchCommand : Command {
    override val name = "touch"
    override val description = "Change file timestamps or create empty files"
    override val usage = "touch file..."
    override val manPage = """
NAME
    touch - change file timestamps

SYNOPSIS
    touch file...

DESCRIPTION
    Update the access and modification times of each FILE to the current time.
    A FILE that does not exist is created empty.

EXAMPLES
    touch newfile.txt
    touch file1.txt file2.txt
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(stderr = "touch: missing operand\nUsage: $usage", exitCode = 2)
        }

        val errors = mutableListOf<String>()
        for (file in args) {
            try {
                val node = vfs.getNode(file)
                if (node != null) {
                    node.modifiedAt = System.currentTimeMillis()
                } else {
                    vfs.createFile(file)
                }
            } catch (e: VfsException) {
                errors.add("touch: ${e.message}")
            }
        }

        return if (errors.isEmpty()) CommandResult()
        else CommandResult(stderr = errors.joinToString("\n"), exitCode = 1)
    }
}

// ──────────────────────────────────────────────────────────────
// file  (FileTypeCommand to avoid naming conflicts)
// ──────────────────────────────────────────────────────────────
class FileTypeCommand : Command {
    override val name = "file"
    override val description = "Determine file type"
    override val usage = "file file..."
    override val manPage = """
NAME
    file - determine file type

SYNOPSIS
    file file...

DESCRIPTION
    Determine the type of each FILE by examining its contents.

EXAMPLES
    file document.txt
    file /bin/ls
    file /home
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(stderr = "file: missing operand\nUsage: $usage", exitCode = 2)
        }

        val results = mutableListOf<String>()
        val errors = mutableListOf<String>()
        for (file in args) {
            try {
                val node = vfs.getNode(file)
                if (node == null) {
                    errors.add("file: $file: No such file or directory")
                    continue
                }
                val typeStr = when (node) {
                    is DirectoryNode -> "directory"
                    is SymlinkNode -> "symbolic link to ${node.target}"
                    is FileNode -> detectFileType(node)
                }
                results.add("$file: $typeStr")
            } catch (e: VfsException) {
                errors.add("file: ${e.message}")
            }
        }

        val stdout = results.joinToString("\n")
        val stderr = errors.joinToString("\n")
        return CommandResult(
            stdout = stdout,
            stderr = stderr,
            exitCode = if (errors.isNotEmpty()) 1 else 0
        )
    }

    private fun detectFileType(node: FileNode): String {
        if (node.content.isEmpty()) return "empty"

        val text = try { String(node.content) } catch (_: Exception) { null }
        if (text != null) {
            if (text.startsWith("#!/")) {
                val interpreter = text.lineSequence().first().removePrefix("#!")
                return "script, $interpreter, ASCII text executable"
            }
            val isAscii = node.content.all { byte ->
                val b = byte.toInt() and 0xFF
                b in 0x09..0x0D || b in 0x20..0x7E
            }
            if (isAscii) return "ASCII text"
            val isUtf8 = text.length > 0
            if (isUtf8) return "UTF-8 Unicode text"
        }
        // Check for common binary signatures
        val header = node.content.take(4).map { it.toInt() and 0xFF }
        if (header.size >= 4) {
            if (header[0] == 0x7F && header[1] == 0x45 && header[2] == 0x4C && header[3] == 0x46) {
                return "ELF executable"
            }
            if (header[0] == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                return "PNG image data"
            }
            if (header[0] == 0xFF && header[1] == 0xD8 && header[2] == 0xFF) {
                return "JPEG image data"
            }
            if (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04) {
                return "Zip archive data"
            }
            if (header[0] == 0x1F && header[1] == 0x8B) {
                return "gzip compressed data"
            }
        }
        return "data"
    }
}

// ──────────────────────────────────────────────────────────────
// ln
// ──────────────────────────────────────────────────────────────
class LnCommand : Command {
    override val name = "ln"
    override val description = "Make links between files"
    override val usage = "ln [-s] target link_name"
    override val manPage = """
NAME
    ln - make links between files

SYNOPSIS
    ln [-s] target link_name

DESCRIPTION
    Create a link to TARGET with the name LINK_NAME.
    By default, creates hard links; with -s, creates symbolic links.

OPTIONS
    -s    Create a symbolic link instead of a hard link

EXAMPLES
    ln -s /etc/passwd passwd_link
    ln original.txt hardlink.txt
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val flags = listOf(CommandFlag('s', "symbolic"))
        val (matched, positional) = parseFlags(args, flags)
        val symbolic = "symbolic" in matched

        if (positional.size < 2) {
            return CommandResult(stderr = "ln: missing operand\nUsage: $usage", exitCode = 2)
        }

        val target = positional[0]
        val linkName = positional[1]

        return try {
            if (symbolic) {
                vfs.createSymlink(linkName, target)
            } else {
                // Hard link: copy the file data (simplified simulation)
                if (!vfs.isFile(target)) {
                    return CommandResult(stderr = "ln: '$target': hard links to directories are not allowed", exitCode = 1)
                }
                val content = vfs.readFile(target)
                vfs.createFile(linkName, content)
            }
            CommandResult()
        } catch (e: VfsException) {
            CommandResult(stderr = "ln: ${e.message}", exitCode = 1)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// stat
// ──────────────────────────────────────────────────────────────
class StatCommand : Command {
    override val name = "stat"
    override val description = "Display file or file system status"
    override val usage = "stat file..."
    override val manPage = """
NAME
    stat - display file or file system status

SYNOPSIS
    stat file...

DESCRIPTION
    Display detailed information about each FILE.

EXAMPLES
    stat file.txt
    stat /home
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(stderr = "stat: missing operand\nUsage: $usage", exitCode = 2)
        }

        val results = mutableListOf<String>()
        val errors = mutableListOf<String>()
        for (file in args) {
            try {
                val node = vfs.getNode(file)
                if (node == null) {
                    errors.add("stat: cannot stat '$file': No such file or directory")
                    continue
                }
                val absPath = vfs.getAbsolutePath(file)
                val type = when (node) {
                    is FileNode -> "regular file"
                    is DirectoryNode -> "directory"
                    is SymlinkNode -> "symbolic link"
                }
                val size = nodeSize(node)
                val octal = "%04d".format(node.permissions.toOctal())
                val symbolic = node.modeString()
                val created = dateFormat.format(Date(node.createdAt))
                val modified = dateFormat.format(Date(node.modifiedAt))

                results.add(buildString {
                    appendLine("  File: $absPath")
                    appendLine("  Size: $size\tType: $type")
                    appendLine("Access: ($octal/${symbolic})\tUid: ${node.owner}\tGid: ${node.group}")
                    appendLine("Modify: $modified")
                    append("Create: $created")
                })
            } catch (e: VfsException) {
                errors.add("stat: ${e.message}")
            }
        }

        val stdout = results.joinToString("\n")
        val stderr = errors.joinToString("\n")
        return CommandResult(
            stdout = stdout,
            stderr = stderr,
            exitCode = if (errors.isNotEmpty()) 1 else 0
        )
    }
}
