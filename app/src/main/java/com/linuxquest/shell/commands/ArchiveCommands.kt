package com.linuxquest.shell.commands

import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment
import java.util.Base64

class TarCommand : Command {
    override val name = "tar"
    override val description = "Archive utility"
    override val usage = "tar [-c|-x|-t] [-z|-j] [-f file] [-v] [files...]"
    override val manPage = """
NAME
    tar - archive files

SYNOPSIS
    tar [-c|-x|-t] [-z|-j] [-f file] [-v] [files...]

OPTIONS
    -c    Create archive
    -x    Extract archive
    -t    List archive contents
    -z    gzip compression (simulated)
    -j    bzip2 compression (simulated)
    -f    Archive file name
    -v    Verbose output
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var create = false; var extract = false; var list = false; var verbose = false
        var archiveFile: String? = null
        val files = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-f" && i + 1 < args.size -> archiveFile = args[++i]
                args[i].startsWith("-") -> {
                    for (c in args[i].drop(1)) {
                        when (c) {
                            'c' -> create = true; 'x' -> extract = true; 't' -> list = true
                            'v' -> verbose = true; 'z', 'j' -> {} // compression simulated
                            'f' -> { i++; if (i < args.size) archiveFile = args[i] }
                        }
                    }
                }
                else -> files.add(args[i])
            }
            i++
        }

        if (archiveFile == null) return CommandResult(stderr = "tar: no archive file specified", exitCode = 1)

        return when {
            create -> {
                val sb = StringBuilder()
                sb.appendLine("# tar archive")
                for (f in files) {
                    try {
                        if (vfs.isDirectory(f)) {
                            collectFiles(vfs, f).forEach { (path, content) ->
                                sb.appendLine("FILE:$path")
                                sb.appendLine("CONTENT:${Base64.getEncoder().encodeToString(content)}")
                                sb.appendLine("END")
                            }
                        } else {
                            val content = vfs.readFile(f)
                            sb.appendLine("FILE:$f")
                            sb.appendLine("CONTENT:${Base64.getEncoder().encodeToString(content)}")
                            sb.appendLine("END")
                        }
                    } catch (e: Exception) {
                        return CommandResult(stderr = "tar: $f: ${e.message}", exitCode = 1)
                    }
                }
                vfs.createFile(archiveFile!!, sb.toString().toByteArray())
                val msg = if (verbose) files.joinToString("\n") { "a $it" } else ""
                CommandResult(stdout = msg)
            }
            extract -> {
                try {
                    val archiveContent = String(vfs.readFile(archiveFile!!))
                    val entries = parseArchive(archiveContent)
                    val sb = StringBuilder()
                    for ((path, content) in entries) {
                        val dir = path.substringBeforeLast("/", "")
                        if (dir.isNotEmpty() && !vfs.exists(dir)) vfs.createDirectory(dir)
                        if (vfs.exists(path)) vfs.writeFile(path, content)
                        else vfs.createFile(path, content)
                        if (verbose) sb.appendLine("x $path")
                    }
                    CommandResult(stdout = sb.toString().trimEnd())
                } catch (e: Exception) {
                    CommandResult(stderr = "tar: ${e.message}", exitCode = 1)
                }
            }
            list -> {
                try {
                    val archiveContent = String(vfs.readFile(archiveFile!!))
                    val entries = parseArchive(archiveContent)
                    CommandResult(stdout = entries.joinToString("\n") { it.first })
                } catch (e: Exception) {
                    CommandResult(stderr = "tar: ${e.message}", exitCode = 1)
                }
            }
            else -> CommandResult(stderr = "tar: specify -c, -x, or -t", exitCode = 1)
        }
    }

    private fun collectFiles(vfs: VirtualFileSystem, path: String): List<Pair<String, ByteArray>> {
        val result = mutableListOf<Pair<String, ByteArray>>()
        vfs.listDirectory(path).forEach { node ->
            val childPath = if (path == "/") "/${node.name}" else "$path/${node.name}"
            if (vfs.isFile(childPath)) result.add(childPath to vfs.readFile(childPath))
            else if (vfs.isDirectory(childPath)) result.addAll(collectFiles(vfs, childPath))
        }
        return result
    }

    private fun parseArchive(content: String): List<Pair<String, ByteArray>> {
        val entries = mutableListOf<Pair<String, ByteArray>>()
        var currentFile: String? = null
        var currentContent: String? = null
        for (line in content.lines()) {
            when {
                line.startsWith("FILE:") -> currentFile = line.removePrefix("FILE:")
                line.startsWith("CONTENT:") -> currentContent = line.removePrefix("CONTENT:")
                line == "END" && currentFile != null && currentContent != null -> {
                    entries.add(currentFile to Base64.getDecoder().decode(currentContent))
                    currentFile = null; currentContent = null
                }
            }
        }
        return entries
    }
}

class Base64Command : Command {
    override val name = "base64"
    override val description = "Base64 encode/decode"
    override val usage = "base64 [-d] [file]"
    override val manPage = """
NAME
    base64 - base64 encode/decode data

SYNOPSIS
    base64 [-d] [FILE]

OPTIONS
    -d    Decode
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var decode = false; var file: String? = null
        for (a in args) { if (a == "-d" || a == "--decode") decode = true else file = a }
        val input = if (file != null) {
            try { String(vfs.readFile(file)) } catch (e: Exception) {
                return CommandResult(stderr = "base64: $file: ${e.message}", exitCode = 1)
            }
        } else stdin ?: ""

        return try {
            if (decode) {
                CommandResult(stdout = String(Base64.getDecoder().decode(input.trim())))
            } else {
                CommandResult(stdout = Base64.getEncoder().encodeToString(input.toByteArray()))
            }
        } catch (e: Exception) {
            CommandResult(stderr = "base64: invalid input", exitCode = 1)
        }
    }
}

class XxdCommand : Command {
    override val name = "xxd"
    override val description = "Make a hex dump"
    override val usage = "xxd [-r] [file]"
    override val manPage = """
NAME
    xxd - make a hex dump or do the reverse

SYNOPSIS
    xxd [-r] [FILE]

OPTIONS
    -r    Reverse: convert hex dump back to binary
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var reverse = false; var file: String? = null
        for (a in args) { if (a == "-r") reverse = true else file = a }
        val input = if (file != null) {
            try { vfs.readFile(file) } catch (e: Exception) {
                return CommandResult(stderr = "xxd: $file: ${e.message}", exitCode = 1)
            }
        } else (stdin ?: "").toByteArray()

        return if (reverse) {
            val hex = String(input).lines().joinToString("") { line ->
                val hexPart = line.substringAfter(": ").substringBefore("  ").replace(" ", "")
                hexPart
            }
            val bytes = hex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
            CommandResult(stdout = String(bytes))
        } else {
            val sb = StringBuilder()
            val bytes = input
            for (offset in bytes.indices step 16) {
                sb.append("%08x: ".format(offset))
                val chunk = bytes.sliceArray(offset until minOf(offset + 16, bytes.size))
                sb.append(chunk.joinToString(" ") { "%02x".format(it) }.padEnd(48))
                sb.append("  ")
                sb.appendLine(chunk.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString(""))
            }
            CommandResult(stdout = sb.toString().trimEnd())
        }
    }
}

class StringsCommand : Command {
    override val name = "strings"
    override val description = "Print printable character sequences"
    override val usage = "strings [-n N] [file]"
    override val manPage = """
NAME
    strings - print the sequences of printable characters in files

SYNOPSIS
    strings [-n MIN-LEN] [FILE]

OPTIONS
    -n N    Print sequences of at least N characters (default 4)
    """.trimIndent()

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var minLen = 4; var file: String? = null
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-n" && i + 1 < args.size -> { minLen = args[++i].toIntOrNull() ?: 4 }
                else -> file = args[i]
            }
            i++
        }
        val input = if (file != null) {
            try { vfs.readFile(file) } catch (e: Exception) {
                return CommandResult(stderr = "strings: $file: ${e.message}", exitCode = 1)
            }
        } else (stdin ?: "").toByteArray()

        val sb = StringBuilder()
        val current = StringBuilder()
        for (b in input) {
            if (b in 32..126) {
                current.append(b.toInt().toChar())
            } else {
                if (current.length >= minLen) sb.appendLine(current)
                current.clear()
            }
        }
        if (current.length >= minLen) sb.appendLine(current)
        return CommandResult(stdout = sb.toString().trimEnd())
    }
}

class GzipCommand : Command {
    override val name = "gzip"
    override val description = "Compress files"
    override val usage = "gzip [-d] file"
    override val manPage = "Compress or decompress files (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var decompress = false; var file: String? = null
        for (a in args) { if (a == "-d") decompress = true else file = a }
        if (file == null) return CommandResult(stderr = "gzip: no file specified", exitCode = 1)
        return try {
            if (decompress) {
                val content = vfs.readFile(file)
                val outName = file.removeSuffix(".gz")
                vfs.createFile(outName, content)
                vfs.delete(file)
                CommandResult()
            } else {
                val content = vfs.readFile(file)
                vfs.createFile("$file.gz", content)
                vfs.delete(file)
                CommandResult()
            }
        } catch (e: Exception) {
            CommandResult(stderr = "gzip: ${e.message}", exitCode = 1)
        }
    }
}

class GunzipCommand : Command {
    override val name = "gunzip"
    override val description = "Decompress files"
    override val usage = "gunzip file"
    override val manPage = "Decompress gzip files (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val file = args.firstOrNull() ?: return CommandResult(stderr = "gunzip: no file specified", exitCode = 1)
        return try {
            val content = vfs.readFile(file)
            val outName = file.removeSuffix(".gz")
            vfs.createFile(outName, content)
            vfs.delete(file)
            CommandResult()
        } catch (e: Exception) {
            CommandResult(stderr = "gunzip: ${e.message}", exitCode = 1)
        }
    }
}
