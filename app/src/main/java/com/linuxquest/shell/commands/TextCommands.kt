package com.linuxquest.shell.commands

import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment

class HeadCommand : Command {
    override val name = "head"
    override val description = "Output the first part of files"
    override val usage = "head [-n N] [file...]"
    override val manPage = """
NAME
    head - output the first part of files

SYNOPSIS
    head [-n N] [file...]

DESCRIPTION
    Print the first N lines of each FILE (default 10).
    With no FILE, read from standard input.
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var n = 10
        val files = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-n" && i + 1 < args.size -> { n = args[++i].toIntOrNull() ?: 10 }
                args[i].startsWith("-n") -> { n = args[i].removePrefix("-n").toIntOrNull() ?: 10 }
                args[i].startsWith("-") && args[i].toIntOrNull() != null -> { n = -args[i].toInt() }
                else -> files.add(args[i])
            }
            i++
        }
        val content = if (files.isEmpty()) {
            stdin ?: return CommandResult(stderr = "head: no input", exitCode = 1)
        } else {
            try { String(vfs.readFile(files.first())) } catch (e: Exception) {
                return CommandResult(stderr = "head: ${files.first()}: ${e.message}", exitCode = 1)
            }
        }
        return CommandResult(stdout = content.lines().take(n).joinToString("\n"))
    }
}

class TailCommand : Command {
    override val name = "tail"
    override val description = "Output the last part of files"
    override val usage = "tail [-n N] [file...]"
    override val manPage = """
NAME
    tail - output the last part of files

SYNOPSIS
    tail [-n N] [file...]

DESCRIPTION
    Print the last N lines of each FILE (default 10).
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var n = 10
        val files = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-n" && i + 1 < args.size -> { n = args[++i].toIntOrNull() ?: 10 }
                args[i].startsWith("-n") -> { n = args[i].removePrefix("-n").toIntOrNull() ?: 10 }
                args[i].startsWith("+") -> { n = -(args[i].toIntOrNull() ?: 1) }
                else -> files.add(args[i])
            }
            i++
        }
        val content = if (files.isEmpty()) {
            stdin ?: return CommandResult(stderr = "tail: no input", exitCode = 1)
        } else {
            try { String(vfs.readFile(files.first())) } catch (e: Exception) {
                return CommandResult(stderr = "tail: ${files.first()}: ${e.message}", exitCode = 1)
            }
        }
        val lines = content.lines()
        val result = if (n < 0) lines.drop(-n - 1) else lines.takeLast(n)
        return CommandResult(stdout = result.joinToString("\n"))
    }
}

class SortCommand : Command {
    override val name = "sort"
    override val description = "Sort lines of text"
    override val usage = "sort [-n] [-r] [-u] [-k N] [-t delim] [file]"
    override val manPage = """
NAME
    sort - sort lines of text files

SYNOPSIS
    sort [-n] [-r] [-u] [-k N] [-t delim] [file]

OPTIONS
    -n    Numeric sort
    -r    Reverse order
    -u    Output only unique lines
    -k N  Sort by field N
    -t D  Use D as field delimiter
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var numeric = false; var reverse = false; var unique = false
        var keyField = 0; var delimiter = "\\s+"
        val files = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "-n" -> numeric = true
                "-r" -> reverse = true
                "-u" -> unique = true
                "-k" -> { i++; keyField = (args.getOrNull(i)?.toIntOrNull() ?: 1) }
                "-t" -> { i++; delimiter = Regex.escape(args.getOrNull(i) ?: "\t") }
                else -> if (!args[i].startsWith("-")) files.add(args[i])
                        else args[i].drop(1).forEach { c -> when(c) { 'n' -> numeric = true; 'r' -> reverse = true; 'u' -> unique = true } }
            }
            i++
        }
        val content = if (files.isEmpty()) stdin ?: ""
        else try { String(vfs.readFile(files.first())) } catch (e: Exception) {
            return CommandResult(stderr = "sort: ${files.first()}: ${e.message}", exitCode = 1)
        }
        var lines = content.lines().filter { it.isNotEmpty() }
        val comparator: Comparator<String> = if (keyField > 0) {
            val regex = Regex(delimiter)
            if (numeric) compareBy { it.split(regex).getOrNull(keyField - 1)?.trim()?.toDoubleOrNull() ?: 0.0 }
            else compareBy { it.split(regex).getOrNull(keyField - 1)?.trim() ?: "" }
        } else {
            if (numeric) compareBy { it.trim().toDoubleOrNull() ?: 0.0 } else compareBy { it }
        }
        lines = lines.sortedWith(comparator)
        if (reverse) lines = lines.reversed()
        if (unique) lines = lines.distinct()
        return CommandResult(stdout = lines.joinToString("\n"))
    }
}

class UniqCommand : Command {
    override val name = "uniq"
    override val description = "Report or omit repeated lines"
    override val usage = "uniq [-c] [-d] [-u] [file]"
    override val manPage = """
NAME
    uniq - report or omit repeated lines

SYNOPSIS
    uniq [-c] [-d] [-u] [file]

OPTIONS
    -c    Prefix lines by number of occurrences
    -d    Only print duplicate lines
    -u    Only print unique lines
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var count = false; var duplicatesOnly = false; var uniqueOnly = false
        val files = mutableListOf<String>()
        for (a in args) {
            when (a) {
                "-c" -> count = true; "-d" -> duplicatesOnly = true; "-u" -> uniqueOnly = true
                else -> if (!a.startsWith("-")) files.add(a)
                        else a.drop(1).forEach { c -> when(c) { 'c' -> count = true; 'd' -> duplicatesOnly = true; 'u' -> uniqueOnly = true } }
            }
        }
        val content = if (files.isEmpty()) stdin ?: ""
        else try { String(vfs.readFile(files.first())) } catch (e: Exception) {
            return CommandResult(stderr = "uniq: ${files.first()}: ${e.message}", exitCode = 1)
        }
        val lines = content.lines()
        val groups = mutableListOf<Pair<String, Int>>()
        for (line in lines) {
            if (groups.isNotEmpty() && groups.last().first == line) {
                groups[groups.lastIndex] = groups.last().copy(second = groups.last().second + 1)
            } else {
                groups.add(line to 1)
            }
        }
        val filtered = groups.filter { (_, cnt) ->
            when {
                duplicatesOnly -> cnt > 1
                uniqueOnly -> cnt == 1
                else -> true
            }
        }
        val result = filtered.joinToString("\n") { (line, cnt) ->
            if (count) "%7d %s".format(cnt, line) else line
        }
        return CommandResult(stdout = result)
    }
}

class WcCommand : Command {
    override val name = "wc"
    override val description = "Word, line, and byte count"
    override val usage = "wc [-l] [-w] [-c] [file...]"
    override val manPage = """
NAME
    wc - print newline, word, and byte counts

SYNOPSIS
    wc [-l] [-w] [-c] [file...]

OPTIONS
    -l    Print line count
    -w    Print word count
    -c    Print byte count
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var showLines = false; var showWords = false; var showBytes = false
        val files = mutableListOf<String>()
        for (a in args) {
            when (a) {
                "-l" -> showLines = true; "-w" -> showWords = true; "-c" -> showBytes = true
                else -> if (!a.startsWith("-")) files.add(a)
                        else a.drop(1).forEach { c -> when(c) { 'l' -> showLines = true; 'w' -> showWords = true; 'c' -> showBytes = true } }
            }
        }
        if (!showLines && !showWords && !showBytes) { showLines = true; showWords = true; showBytes = true }
        val contents = if (files.isEmpty()) listOf("" to (stdin ?: ""))
        else files.map { f ->
            f to try { String(vfs.readFile(f)) } catch (e: Exception) { return CommandResult(stderr = "wc: $f: ${e.message}", exitCode = 1) }
        }
        val sb = StringBuilder()
        for ((name, content) in contents) {
            val parts = mutableListOf<String>()
            if (showLines) parts.add("%8d".format(content.lines().size))
            if (showWords) parts.add("%8d".format(content.split(Regex("\\s+")).count { it.isNotEmpty() }))
            if (showBytes) parts.add("%8d".format(content.toByteArray().size))
            if (name.isNotEmpty()) parts.add(name)
            sb.appendLine(parts.joinToString(" "))
        }
        return CommandResult(stdout = sb.toString().trimEnd())
    }
}

class CutCommand : Command {
    override val name = "cut"
    override val description = "Remove sections from each line"
    override val usage = "cut -d delim -f fields [file]"
    override val manPage = """
NAME
    cut - remove sections from each line of files

SYNOPSIS
    cut -d DELIM -f FIELDS [file]

OPTIONS
    -d DELIM    Use DELIM as field delimiter (default TAB)
    -f FIELDS   Select fields (e.g., 1,3 or 1-3)
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var delimiter = "\t"; var fieldSpec = ""; var file: String? = null
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-d" && i + 1 < args.size -> { delimiter = args[++i] }
                args[i].startsWith("-d") -> { delimiter = args[i].removePrefix("-d") }
                args[i] == "-f" && i + 1 < args.size -> { fieldSpec = args[++i] }
                args[i].startsWith("-f") -> { fieldSpec = args[i].removePrefix("-f") }
                !args[i].startsWith("-") -> file = args[i]
            }
            i++
        }
        if (fieldSpec.isEmpty()) return CommandResult(stderr = "cut: you must specify a list of fields", exitCode = 1)
        val fields = parseFieldSpec(fieldSpec)
        val content = if (file == null) stdin ?: ""
        else try { String(vfs.readFile(file)) } catch (e: Exception) {
            return CommandResult(stderr = "cut: $file: ${e.message}", exitCode = 1)
        }
        val result = content.lines().joinToString("\n") { line ->
            val parts = line.split(delimiter)
            fields.mapNotNull { f -> parts.getOrNull(f - 1) }.joinToString(delimiter)
        }
        return CommandResult(stdout = result)
    }

    private fun parseFieldSpec(spec: String): List<Int> {
        val fields = mutableListOf<Int>()
        spec.split(",").forEach { part ->
            if (part.contains("-")) {
                val (start, end) = part.split("-", limit = 2)
                val s = start.toIntOrNull() ?: 1
                val e = end.toIntOrNull() ?: s
                fields.addAll(s..e)
            } else {
                part.toIntOrNull()?.let { fields.add(it) }
            }
        }
        return fields.distinct().sorted()
    }
}

class TrCommand : Command {
    override val name = "tr"
    override val description = "Translate or delete characters"
    override val usage = "tr [-d] [-s] set1 [set2]"
    override val manPage = """
NAME
    tr - translate or delete characters

SYNOPSIS
    tr [-d] [-s] SET1 [SET2]

DESCRIPTION
    Translate, squeeze, or delete characters from stdin.

OPTIONS
    -d    Delete characters in SET1
    -s    Squeeze repeated characters in SET1 to single
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var delete = false; var squeeze = false
        val sets = mutableListOf<String>()
        for (a in args) {
            when (a) {
                "-d" -> delete = true; "-s" -> squeeze = true
                else -> sets.add(a)
            }
        }
        if (sets.isEmpty()) return CommandResult(stderr = "tr: missing operand", exitCode = 1)
        val input = stdin ?: ""
        val set1 = expandSet(sets[0])
        return when {
            delete -> {
                val result = input.filter { it !in set1 }
                CommandResult(stdout = result)
            }
            squeeze && sets.size == 1 -> {
                val result = buildString {
                    var prev: Char? = null
                    for (c in input) {
                        if (c in set1 && c == prev) continue
                        append(c); prev = c
                    }
                }
                CommandResult(stdout = result)
            }
            sets.size >= 2 -> {
                val set2 = expandSet(sets[1])
                val map = mutableMapOf<Char, Char>()
                set1.forEachIndexed { i, c -> map[c] = set2.getOrElse(i) { set2.lastOrNull() ?: c } }
                var result = input.map { map[it] ?: it }.joinToString("")
                if (squeeze) {
                    result = buildString {
                        var prev: Char? = null
                        for (c in result) {
                            if (c in set2.toSet() && c == prev) continue
                            append(c); prev = c
                        }
                    }
                }
                CommandResult(stdout = result)
            }
            else -> CommandResult(stderr = "tr: missing operand", exitCode = 1)
        }
    }

    private fun expandSet(set: String): List<Char> {
        val chars = mutableListOf<Char>()
        var i = 0
        while (i < set.length) {
            if (i + 2 < set.length && set[i + 1] == '-') {
                val start = set[i]; val end = set[i + 2]
                (start..end).forEach { chars.add(it) }
                i += 3
            } else {
                chars.add(set[i]); i++
            }
        }
        return chars
    }
}

class SedCommand : Command {
    override val name = "sed"
    override val description = "Stream editor"
    override val usage = "sed 's/pattern/replacement/[g]' [file]"
    override val manPage = """
NAME
    sed - stream editor for filtering and transforming text

SYNOPSIS
    sed 's/PATTERN/REPLACEMENT/[FLAGS]' [file]

DESCRIPTION
    Perform text substitution. Supports s command with g (global) flag.
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) return CommandResult(stderr = "sed: no expression given", exitCode = 1)
        val expr = args[0]
        val file = args.getOrNull(1)
        val content = if (file != null) {
            try { String(vfs.readFile(file)) } catch (e: Exception) {
                return CommandResult(stderr = "sed: $file: ${e.message}", exitCode = 1)
            }
        } else stdin ?: ""

        if (!expr.startsWith("s")) return CommandResult(stderr = "sed: unsupported command", exitCode = 1)
        val delim = expr[1]
        val parts = expr.drop(2).split(delim)
        if (parts.size < 2) return CommandResult(stderr = "sed: invalid expression", exitCode = 1)
        val pattern = parts[0]
        val replacement = parts[1]
        val flags = parts.getOrNull(2) ?: ""
        val global = 'g' in flags

        val regex = try { Regex(pattern) } catch (_: Exception) {
            return CommandResult(stderr = "sed: invalid regex: $pattern", exitCode = 1)
        }
        val result = content.lines().joinToString("\n") { line ->
            if (global) regex.replace(line, replacement)
            else regex.replaceFirst(line, replacement)
        }
        return CommandResult(stdout = result)
    }
}

class AwkCommand : Command {
    override val name = "awk"
    override val description = "Pattern scanning and processing"
    override val usage = "awk [-F sep] 'program' [file]"
    override val manPage = """
NAME
    awk - pattern scanning and processing language

SYNOPSIS
    awk [-F sep] 'PROGRAM' [file]

DESCRIPTION
    A simplified awk supporting print, ${'$'}0-${'$'}NF, NR, NF, FS.
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var fieldSep = "\\s+"
        var program = ""
        var file: String? = null
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-F" && i + 1 < args.size -> { fieldSep = Regex.escape(args[++i]) }
                args[i].startsWith("-F") -> { fieldSep = Regex.escape(args[i].removePrefix("-F")) }
                program.isEmpty() -> program = args[i]
                else -> file = args[i]
            }
            i++
        }
        val content = if (file != null) {
            try { String(vfs.readFile(file)) } catch (e: Exception) {
                return CommandResult(stderr = "awk: $file: ${e.message}", exitCode = 1)
            }
        } else stdin ?: ""

        val sepRegex = Regex(fieldSep)
        val output = StringBuilder()
        val lines = content.lines()
        lines.forEachIndexed { idx, line ->
            val fields = line.split(sepRegex).filter { it.isNotEmpty() }
            val nr = idx + 1
            val nf = fields.size
            val printExpr = extractPrintExpr(program) ?: return@forEachIndexed
            val parts = parsePrintArgs(printExpr)
            val lineOut = parts.joinToString(" ") { part ->
                when {
                    part == "\$0" || part == "\\$0" -> line
                    part.startsWith("\$") || part.startsWith("\\$") -> {
                        val num = part.removePrefix("\\$").removePrefix("\$").toIntOrNull()
                        if (num != null && num in 1..nf) fields[num - 1] else ""
                    }
                    part == "NR" -> nr.toString()
                    part == "NF" -> nf.toString()
                    part.startsWith("\"") && part.endsWith("\"") -> part.drop(1).dropLast(1)
                    else -> part
                }
            }
            output.appendLine(lineOut)
        }
        return CommandResult(stdout = output.toString().trimEnd())
    }

    private fun extractPrintExpr(program: String): String? {
        val cleaned = program.trim().removeSurrounding("{", "}")
        val printMatch = Regex("^\\s*print\\s+(.+)$").find(cleaned.trim()) ?: run {
            if (cleaned.trim() == "print") return "\$0"
            return null
        }
        return printMatch.groupValues[1]
    }

    private fun parsePrintArgs(expr: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuote = false
        for (c in expr) {
            when {
                c == '"' -> { inQuote = !inQuote; current.append(c) }
                c == ',' && !inQuote -> {
                    if (current.isNotBlank()) result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) result.add(current.toString().trim())
        return result
    }
}

class DiffCommand : Command {
    override val name = "diff"
    override val description = "Compare files line by line"
    override val usage = "diff file1 file2"
    override val manPage = """
NAME
    diff - compare files line by line

SYNOPSIS
    diff FILE1 FILE2
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.size < 2) return CommandResult(stderr = "diff: missing operand", exitCode = 2)
        val content1 = try { String(vfs.readFile(args[0])) } catch (e: Exception) {
            return CommandResult(stderr = "diff: ${args[0]}: ${e.message}", exitCode = 2)
        }
        val content2 = try { String(vfs.readFile(args[1])) } catch (e: Exception) {
            return CommandResult(stderr = "diff: ${args[1]}: ${e.message}", exitCode = 2)
        }
        val lines1 = content1.lines(); val lines2 = content2.lines()
        if (lines1 == lines2) return CommandResult(exitCode = 0)
        val sb = StringBuilder()
        val maxLen = maxOf(lines1.size, lines2.size)
        for (i in 0 until maxLen) {
            val l1 = lines1.getOrNull(i); val l2 = lines2.getOrNull(i)
            when {
                l1 == l2 -> {}
                l1 != null && l2 != null -> { sb.appendLine("${i + 1}c${i + 1}"); sb.appendLine("< $l1"); sb.appendLine("---"); sb.appendLine("> $l2") }
                l1 == null -> { sb.appendLine("${i + 1}a${i + 1}"); sb.appendLine("> $l2") }
                l2 == null -> { sb.appendLine("${i + 1}d${i + 1}"); sb.appendLine("< $l1") }
            }
        }
        return CommandResult(stdout = sb.toString().trimEnd(), exitCode = 1)
    }
}

class TeeCommand : Command {
    override val name = "tee"
    override val description = "Read from stdin and write to stdout and files"
    override val usage = "tee [-a] file"
    override val manPage = """
NAME
    tee - read from stdin, write to stdout and files

SYNOPSIS
    tee [-a] FILE

OPTIONS
    -a    Append to file instead of overwriting
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var append = false
        val files = mutableListOf<String>()
        for (a in args) {
            when (a) { "-a" -> append = true; else -> files.add(a) }
        }
        val input = stdin ?: ""
        for (f in files) {
            try {
                if (append && vfs.exists(f)) {
                    val existing = String(vfs.readFile(f))
                    vfs.writeFile(f, (existing + input).toByteArray())
                } else {
                    if (vfs.exists(f)) vfs.writeFile(f, input.toByteArray())
                    else vfs.createFile(f, input.toByteArray())
                }
            } catch (e: Exception) {
                return CommandResult(stdout = input, stderr = "tee: $f: ${e.message}", exitCode = 1)
            }
        }
        return CommandResult(stdout = input)
    }
}

class XargsCommand : Command {
    override val name = "xargs"
    override val description = "Build and execute commands from stdin"
    override val usage = "xargs [-n N] command [args]"
    override val manPage = """
NAME
    xargs - build and execute command lines from standard input

SYNOPSIS
    xargs [-n N] COMMAND [INITIAL-ARGS]

DESCRIPTION
    Read items from stdin and execute COMMAND with those items as arguments.

OPTIONS
    -n N    Use at most N arguments per command line
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var maxArgs = Int.MAX_VALUE
        val cmdArgs = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-n" && i + 1 < args.size -> { maxArgs = args[++i].toIntOrNull() ?: Int.MAX_VALUE }
                else -> cmdArgs.add(args[i])
            }
            i++
        }
        val items = (stdin ?: "").split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (cmdArgs.isEmpty()) cmdArgs.add("echo")
        val output = StringBuilder()
        items.chunked(maxArgs).forEach { chunk ->
            val fullArgs = cmdArgs.drop(1) + chunk
            output.appendLine((listOf(cmdArgs.first()) + fullArgs).joinToString(" "))
        }
        return CommandResult(stdout = output.toString().trimEnd())
    }
}
