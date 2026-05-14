package com.linuxquest.shell.commands

import com.linuxquest.filesystem.*
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment

// ──────────────────────────────────────────────────────────────
// ANSI helpers
// ──────────────────────────────────────────────────────────────
private const val ANSI_RESET_S = "\u001B[0m"
private const val ANSI_RED_S = "\u001B[1;31m"

// ──────────────────────────────────────────────────────────────
// find
// ──────────────────────────────────────────────────────────────
class FindCommand : Command {
    override val name = "find"
    override val description = "Search for files in a directory hierarchy"
    override val usage = "find [path] [-name pattern] [-type f|d] [-size +/-N[c|k|M]] [-maxdepth N] [-exec cmd {} \\;]"
    override val manPage = """
NAME
    find - search for files in a directory hierarchy

SYNOPSIS
    find [path] [-name pattern] [-type f|d] [-size +/-N[c|k|M]] [-maxdepth N] [-exec cmd {} \;]

DESCRIPTION
    Search for files in a directory hierarchy starting from PATH (default: .).

OPTIONS
    -name pattern
        Base of file name matches glob PATTERN (supports * and ? wildcards).

    -type c
        File is of type c:  f (regular file), d (directory).

    -size n[c|k|M]
        File uses n units of space.  Prefix + means greater than, - means
        less than.  Suffixes: c = bytes, k = kilobytes (1024), M = megabytes.
        Default unit without suffix is 512-byte blocks.

    -maxdepth N
        Descend at most N levels below the starting point.

    -exec command {} \;
        Execute command on each matched file. {} is replaced by the file path.

EXAMPLES
    find . -name "*.txt"
    find /home -type d
    find . -name "*.log" -size +1M
    find . -maxdepth 2 -name "*.conf"
    find . -name "*.bak" -exec rm {} \;
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        // Parse find-specific arguments manually because of multi-value flags
        var startPath = "."
        var namePattern: String? = null
        var typeFilter: Char? = null
        var sizeExpr: String? = null
        var maxDepth: Int = Int.MAX_VALUE
        var execCmd: List<String>? = null

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "-name" -> { i++; if (i < args.size) namePattern = args[i] }
                "-type" -> { i++; if (i < args.size) typeFilter = args[i].firstOrNull() }
                "-size" -> { i++; if (i < args.size) sizeExpr = args[i] }
                "-maxdepth" -> { i++; if (i < args.size) maxDepth = args[i].toIntOrNull() ?: Int.MAX_VALUE }
                "-exec" -> {
                    i++
                    val cmdParts = mutableListOf<String>()
                    while (i < args.size && args[i] != ";") {
                        cmdParts.add(args[i])
                        i++
                    }
                    execCmd = cmdParts
                }
                else -> {
                    if (!args[i].startsWith("-")) startPath = args[i]
                }
            }
            i++
        }

        return try {
            val results = mutableListOf<String>()
            findRecursive(startPath, vfs, namePattern, typeFilter, sizeExpr, maxDepth, 0, results)
            CommandResult(stdout = results.joinToString("\n"))
        } catch (e: VfsException) {
            CommandResult(stderr = "find: ${e.message}", exitCode = 1)
        }
    }

    private fun findRecursive(
        path: String,
        vfs: VirtualFileSystem,
        namePattern: String?,
        typeFilter: Char?,
        sizeExpr: String?,
        maxDepth: Int,
        currentDepth: Int,
        results: MutableList<String>
    ) {
        if (currentDepth > maxDepth) return

        val absPath = vfs.getAbsolutePath(path)
        val node = vfs.getNode(path) ?: return

        if (matchesFilters(node, namePattern, typeFilter, sizeExpr)) {
            results.add(absPath)
        }

        if (node is DirectoryNode && currentDepth < maxDepth) {
            try {
                val children = vfs.listDirectory(path)
                for (child in children) {
                    findRecursive(
                        "$absPath/${child.name}", vfs,
                        namePattern, typeFilter, sizeExpr,
                        maxDepth, currentDepth + 1, results
                    )
                }
            } catch (_: VfsException) {
                // Permission denied or other error — skip silently like real find
            }
        }
    }

    private fun matchesFilters(
        node: VNode,
        namePattern: String?,
        typeFilter: Char?,
        sizeExpr: String?
    ): Boolean {
        if (typeFilter != null) {
            val matches = when (typeFilter) {
                'f' -> node is FileNode
                'd' -> node is DirectoryNode
                'l' -> node is SymlinkNode
                else -> true
            }
            if (!matches) return false
        }

        if (namePattern != null) {
            if (!globMatch(node.name, namePattern)) return false
        }

        if (sizeExpr != null && node is FileNode) {
            if (!matchesSize(node.size, sizeExpr)) return false
        }

        return true
    }

    private fun matchesSize(fileSize: Long, expr: String): Boolean {
        val prefix = when {
            expr.startsWith("+") -> '+'
            expr.startsWith("-") -> '-'
            else -> '='
        }
        val numStr = expr.trimStart('+', '-')
        val suffix = numStr.lastOrNull() ?: return true

        val (multiplier, numberPart) = when {
            suffix == 'c' -> 1L to numStr.dropLast(1)
            suffix == 'k' -> 1024L to numStr.dropLast(1)
            suffix == 'M' -> (1024L * 1024L) to numStr.dropLast(1)
            suffix.isDigit() -> 512L to numStr // default: 512-byte blocks
            else -> return true
        }

        val targetSize = (numberPart.toLongOrNull() ?: return true) * multiplier
        return when (prefix) {
            '+' -> fileSize > targetSize
            '-' -> fileSize < targetSize
            else -> fileSize == targetSize
        }
    }
}

/**
 * Simple glob pattern matching supporting * and ? wildcards.
 */
private fun globMatch(text: String, pattern: String): Boolean {
    return globToRegex(pattern).matches(text)
}

private fun globToRegex(glob: String): Regex {
    val sb = StringBuilder("^")
    for (c in glob) {
        when (c) {
            '*' -> sb.append(".*")
            '?' -> sb.append(".")
            '.' -> sb.append("\\.")
            '\\' -> sb.append("\\\\")
            '^', '$', '|', '+', '(', ')', '{', '}', '[', ']' -> sb.append("\\").append(c)
            else -> sb.append(c)
        }
    }
    sb.append("$")
    return Regex(sb.toString())
}

// ──────────────────────────────────────────────────────────────
// grep
// ──────────────────────────────────────────────────────────────
class GrepCommand : Command {
    override val name = "grep"
    override val description = "Print lines matching a pattern"
    override val usage = "grep [-i] [-n] [-c] [-v] [-r] [-l] [-E] pattern [file...]"
    override val manPage = """
NAME
    grep - print lines that match patterns

SYNOPSIS
    grep [-i] [-n] [-c] [-v] [-r] [-l] [-E] pattern [file...]

DESCRIPTION
    Search for PATTERN in each FILE.  When FILE is -, read standard input.
    By default, grep prints the matching lines.

OPTIONS
    -i    Ignore case distinctions in patterns and input data
    -n    Prefix each line of output with the 1-based line number
    -c    Suppress normal output; instead print a count of matching lines
    -v    Invert the sense of matching, to select non-matching lines
    -r    Read all files under each directory, recursively
    -l    Suppress normal output; print the name of each input file that matches
    -E    Interpret PATTERN as an extended regular expression (ERE)

EXAMPLES
    grep "error" logfile.txt
    grep -i "warning" *.log
    grep -rn "TODO" .
    grep -c "^#" config.txt
    grep -v "^${'$'}" file.txt
    echo "hello world" | grep "hello"
""".trimIndent()

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        val flags = listOf(
            CommandFlag('i', "ignore-case"),
            CommandFlag('n', "line-number"),
            CommandFlag('c', "count"),
            CommandFlag('v', "invert-match"),
            CommandFlag('r', "recursive"),
            CommandFlag('l', "files-with-matches"),
            CommandFlag('E', "extended-regexp")
        )
        val (matched, positional) = parseFlags(args, flags)
        val ignoreCase = "ignore-case" in matched
        val lineNumbers = "line-number" in matched
        val countOnly = "count" in matched
        val invert = "invert-match" in matched
        val recursive = "recursive" in matched
        val filesOnly = "files-with-matches" in matched

        if (positional.isEmpty()) {
            return CommandResult(stderr = "grep: missing pattern\nUsage: $usage", exitCode = 2)
        }

        val patternStr = positional[0]
        val files = positional.drop(1)

        val regex = try {
            val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
            Regex(patternStr, options)
        } catch (e: Exception) {
            return CommandResult(stderr = "grep: invalid pattern '$patternStr': ${e.message}", exitCode = 2)
        }

        return try {
            val output = StringBuilder()
            var anyMatch = false

            if (files.isEmpty() && !recursive) {
                // Read from stdin
                val text = stdin ?: ""
                val matchResult = grepText(text, regex, invert, lineNumbers, countOnly, filesOnly, null)
                if (matchResult.first) anyMatch = true
                output.append(matchResult.second)
            } else {
                val filePaths = mutableListOf<String>()
                val searchPaths = if (files.isEmpty() && recursive) listOf(".") else files

                for (path in searchPaths) {
                    if (recursive && vfs.isDirectory(path)) {
                        collectFiles(path, vfs, filePaths)
                    } else {
                        filePaths.add(path)
                    }
                }

                val multiFile = filePaths.size > 1

                for (filePath in filePaths) {
                    try {
                        val content = String(vfs.readFile(filePath))
                        val absPath = vfs.getAbsolutePath(filePath)
                        val prefix = if (multiFile) absPath else null
                        val matchResult = grepText(content, regex, invert, lineNumbers, countOnly, filesOnly, prefix)
                        if (matchResult.first) anyMatch = true
                        if (matchResult.second.isNotEmpty()) {
                            if (output.isNotEmpty() && !output.endsWith("\n")) output.appendLine()
                            output.append(matchResult.second)
                        }
                    } catch (_: IsADirectoryException) {
                        // Skip directories in non-recursive mode
                    } catch (e: VfsException) {
                        output.append("grep: ${e.message}\n")
                    }
                }
            }

            CommandResult(
                stdout = output.toString().trimEnd('\n'),
                exitCode = if (anyMatch) 0 else 1
            )
        } catch (e: VfsException) {
            CommandResult(stderr = "grep: ${e.message}", exitCode = 1)
        }
    }

    private fun grepText(
        text: String,
        regex: Regex,
        invert: Boolean,
        lineNumbers: Boolean,
        countOnly: Boolean,
        filesOnly: Boolean,
        filePrefix: String?
    ): Pair<Boolean, String> {
        val lines = text.lines()
        val matchingLines = mutableListOf<Pair<Int, String>>()
        var matchFound = false

        for ((index, line) in lines.withIndex()) {
            val matches = regex.containsMatchIn(line)
            if (matches != invert) {
                matchFound = true
                matchingLines.add((index + 1) to line)
            }
        }

        if (!matchFound) return false to ""

        if (filesOnly) {
            return true to (filePrefix ?: "(standard input)")
        }

        if (countOnly) {
            val countStr = matchingLines.size.toString()
            return true to if (filePrefix != null) "$filePrefix:$countStr" else countStr
        }

        val sb = StringBuilder()
        for ((lineNum, line) in matchingLines) {
            val highlighted = if (!invert) highlightMatch(line, regex) else line
            val parts = mutableListOf<String>()
            if (filePrefix != null) parts.add(filePrefix)
            if (lineNumbers) parts.add(lineNum.toString())
            parts.add(highlighted)
            sb.appendLine(parts.joinToString(":"))
        }
        return true to sb.toString().trimEnd('\n')
    }

    private fun highlightMatch(line: String, regex: Regex): String {
        return regex.replace(line) { match ->
            "${ANSI_RED_S}${match.value}${ANSI_RESET_S}"
        }
    }

    private fun collectFiles(path: String, vfs: VirtualFileSystem, result: MutableList<String>) {
        try {
            val children = vfs.listDirectory(path)
            val absPath = vfs.getAbsolutePath(path)
            for (child in children) {
                val childPath = "$absPath/${child.name}"
                when (child) {
                    is FileNode -> result.add(childPath)
                    is DirectoryNode -> collectFiles(childPath, vfs, result)
                    is SymlinkNode -> result.add(childPath)
                }
            }
        } catch (_: VfsException) {
            // Skip inaccessible directories
        }
    }
}

// ──────────────────────────────────────────────────────────────
// which
// ──────────────────────────────────────────────────────────────
class WhichCommand : Command {
    override val name = "which"
    override val description = "Locate a command"
    override val usage = "which command"
    override val manPage = """
NAME
    which - locate a command

SYNOPSIS
    which command

DESCRIPTION
    Write the full pathname of the executable that would have been executed
    when the given command name is entered at the shell prompt.

    Searches the directories listed in ${'$'}PATH for the command.

EXAMPLES
    which ls
    which grep
    which cat
""".trimIndent()

    companion object {
        private val KNOWN_COMMANDS = setOf(
            "ls", "cat", "cp", "mv", "rm", "mkdir", "rmdir", "touch",
            "file", "ln", "stat", "cd", "pwd", "find", "grep", "which",
            "echo", "head", "tail", "wc", "sort", "uniq", "cut", "tr",
            "chmod", "chown", "chgrp", "id", "whoami", "su", "ssh",
            "man", "help", "clear", "history", "env", "export", "set",
            "bash", "sh", "less", "more", "diff", "tar", "gzip", "gunzip",
            "base64", "xxd", "strings", "nc", "nmap", "openssl", "curl"
        )
    }

    override fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(stderr = "which: missing argument\nUsage: $usage", exitCode = 2)
        }

        val results = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var exitCode = 0

        for (cmd in args) {
            val pathDirs = (env.getVariable("PATH") ?: "/usr/bin:/bin").split(":")
            var found = false

            // Check if the command "exists" in any PATH directory
            for (dir in pathDirs) {
                val fullPath = "$dir/$cmd"
                if (vfs.exists(fullPath) && vfs.isFile(fullPath)) {
                    results.add(fullPath)
                    found = true
                    break
                }
            }

            if (!found && cmd in KNOWN_COMMANDS) {
                // Return standard location for built-in known commands
                results.add("/usr/bin/$cmd")
                found = true
            }

            if (!found) {
                errors.add("which: no $cmd in (${env.getVariable("PATH") ?: ""})")
                exitCode = 1
            }
        }

        val stdout = results.joinToString("\n")
        val stderr = errors.joinToString("\n")
        return CommandResult(stdout = stdout, stderr = stderr, exitCode = exitCode)
    }
}
