package com.linuxquest.game.levels

import com.linuxquest.filesystem.Permissions
import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.game.Level
import com.linuxquest.game.LevelCategory
import com.linuxquest.game.PasswordSystem

private fun mkdirs(vfs: VirtualFileSystem, path: String) {
    val parts = path.removePrefix("/").split("/")
    var current = ""
    for (part in parts) {
        current = "$current/$part"
        if (!vfs.exists(current)) {
            vfs.createDirectory(current)
        }
    }
}

fun createFileBasicsLevels(): List<Level> {
    val ps = PasswordSystem()

    return listOf(
        // Level 0 — Welcome
        Level(
            id = 0,
            title = "Welcome",
            category = LevelCategory.FILE_BASICS,
            description = "Find the password stored in the readme file in your home directory.",
            briefing = "Welcome to LinuxQuest! You've just connected to the server. Every level has a password hidden somewhere. Your first task is simple: read the contents of a file.",
            hints = listOf(
                "Try listing files with 'ls'",
                "There's a file called 'readme' — read it",
                "Use 'cat readme' to display the file contents"
            ),
            password = ps.getPassword(0),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(0)
                vfs.createFile("/home/bandit0/readme", password.toByteArray(), Permissions.fromOctal(644))
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(0)
                lastOutput.contains(password)
            },
            teachingPoints = listOf("Listing files with ls", "Reading files with cat"),
            commandsIntroduced = listOf("ls", "cat")
        ),

        // Level 1 — Moving Around
        Level(
            id = 1,
            title = "Moving Around",
            category = LevelCategory.FILE_BASICS,
            description = "The password is in a file deep within nested directories. Navigate to find it.",
            briefing = "Files aren't always right in front of you. Sometimes you need to navigate through directories to find what you're looking for.",
            hints = listOf(
                "Use 'ls' to see what's in each directory",
                "Navigate with 'cd deep' then keep going deeper",
                "The full path is deep/nested/directory/password.txt — use cd or cat with the path"
            ),
            password = ps.getPassword(1),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(1)
                mkdirs(vfs, "/home/bandit0/deep/nested/directory")
                vfs.createFile(
                    "/home/bandit0/deep/nested/directory/password.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(1)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Navigating directories with cd",
                "Relative and absolute paths",
                "Using paths with cat"
            ),
            commandsIntroduced = listOf("cd", "pwd")
        ),

        // Level 2 — Reading Files
        Level(
            id = 2,
            title = "Reading Files",
            category = LevelCategory.FILE_BASICS,
            description = "The password is in a file called '-' (a single dash). Standard 'cat -' won't work.",
            briefing = "Some filenames can be tricky. A dash is special in Linux because it usually means 'read from standard input'. You need to find another way.",
            hints = listOf(
                "There's a file called '-' — find it with 'ls'",
                "Prefix the filename with ./ to treat it as a path",
                "Use 'cat ./-' to read the file"
            ),
            password = ps.getPassword(2),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(2)
                vfs.createFile(
                    "/home/bandit0/readme.txt",
                    "This is not the password.".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/notes.txt",
                    "Nothing to see here.".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/-",
                    password.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(2)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Handling special filenames",
                "Using ./ prefix for current directory"
            ),
            commandsIntroduced = listOf("cat ./-")
        ),

        // Level 3 — Hidden Secrets
        Level(
            id = 3,
            title = "Hidden Secrets",
            category = LevelCategory.FILE_BASICS,
            description = "The password is in a hidden file in your home directory.",
            briefing = "In Linux, files starting with a dot (.) are hidden from normal directory listings. They're often used for configuration, but sometimes they hide secrets.",
            hints = listOf(
                "Regular 'ls' won't show all files",
                "Use 'ls -a' to show hidden files (those starting with .)",
                "Read the .hidden file with 'cat .hidden'"
            ),
            password = ps.getPassword(3),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(3)
                vfs.createFile(
                    "/home/bandit0/.hidden",
                    password.toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/notes.txt",
                    "No password here.".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/todo.txt",
                    "Keep searching.".toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(3)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Hidden files in Linux",
                "Using ls flags",
                "The -a flag for ls"
            ),
            commandsIntroduced = listOf("ls -a")
        ),

        // Level 4 — File Types
        Level(
            id = 4,
            title = "File Types",
            category = LevelCategory.FILE_BASICS,
            description = "Only one file in the 'data' directory is human-readable. Find it and read the password.",
            briefing = "Not all files contain text. Some are binary data, compressed archives, or other formats. The 'file' command can tell you what type a file is.",
            hints = listOf(
                "Look in the 'data' directory",
                "Use the 'file' command to check what type each file is",
                "The human-readable file contains ASCII text — use 'cat' on it"
            ),
            password = ps.getPassword(4),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(4)
                mkdirs(vfs, "/home/bandit0/data")

                val elfHeader = byteArrayOf(
                    0x7f, 0x45, 0x4c, 0x46, 0x02, 0x01, 0x01, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                )
                val gzipHeader = byteArrayOf(
                    0x1f, 0x8b.toByte(), 0x08, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x03, 0x4b, 0x4c, 0x28, 0x49, 0x2d, 0x2e
                )
                val bzip2Header = byteArrayOf(
                    0x42, 0x5a, 0x68, 0x39, 0x31, 0x41, 0x59, 0x26,
                    0x53, 0x59, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                )
                val pngHeader = byteArrayOf(
                    0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
                    0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52
                )

                vfs.createFile("/home/bandit0/data/file00", elfHeader, Permissions.fromOctal(644))
                vfs.createFile("/home/bandit0/data/file01", gzipHeader, Permissions.fromOctal(644))
                vfs.createFile(
                    "/home/bandit0/data/file02",
                    "password: $password\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile("/home/bandit0/data/file03", bzip2Header, Permissions.fromOctal(644))
                vfs.createFile("/home/bandit0/data/file04", pngHeader, Permissions.fromOctal(644))
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(4)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Identifying file types with file command",
                "Distinguishing text from binary files"
            ),
            commandsIntroduced = listOf("file")
        ),

        // Level 5 — Spaces & Specials
        Level(
            id = 5,
            title = "Spaces & Specials",
            category = LevelCategory.FILE_BASICS,
            description = "The password is in a file whose name contains spaces.",
            briefing = "Spaces in filenames are common in the real world but tricky on the command line. You need to learn how to handle them.",
            hints = listOf(
                "There's a file with spaces in its name — 'ls' will show it",
                "Wrap the filename in quotes to handle spaces",
                "Use: cat \"spaces in this name\""
            ),
            password = ps.getPassword(5),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(5)
                vfs.createFile(
                    "/home/bandit0/spaces in this name",
                    password.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(5)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Quoting filenames with spaces",
                "Escape characters with backslash"
            ),
            commandsIntroduced = listOf("cat with quotes")
        ),

        // Level 6 — Deeper Dive
        Level(
            id = 6,
            title = "Deeper Dive",
            category = LevelCategory.FILE_BASICS,
            description = "The password file is hidden somewhere in the /home/bandit0/data directory tree. Find it.",
            briefing = "When you have a large directory tree, manually navigating every folder is impractical. The 'find' command lets you search for files by name, size, type, and more.",
            hints = listOf(
                "The file is somewhere in ~/data — don't search manually",
                "Use 'find ~/data -name secret.txt' to locate it",
                "Once found, use 'cat' on the full path to read it"
            ),
            password = ps.getPassword(6),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(6)

                mkdirs(vfs, "/home/bandit0/data/alpha/sub1")
                mkdirs(vfs, "/home/bandit0/data/alpha/sub2")
                mkdirs(vfs, "/home/bandit0/data/beta/sub1")
                mkdirs(vfs, "/home/bandit0/data/beta/sub2")
                mkdirs(vfs, "/home/bandit0/data/gamma/sub1")
                mkdirs(vfs, "/home/bandit0/data/gamma/sub2")

                vfs.createFile(
                    "/home/bandit0/data/alpha/sub1/info.txt",
                    "Nothing useful here.\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/data/alpha/sub2/readme.txt",
                    "This is a decoy file.\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/data/beta/sub1/data.txt",
                    "No password in this file.\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/data/beta/sub2/secret.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/data/gamma/sub1/notes.txt",
                    "Keep looking elsewhere.\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/data/gamma/sub2/log.txt",
                    "System log: all clear.\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(6)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Searching for files with find",
                "Using -name flag",
                "Recursive directory searching"
            ),
            commandsIntroduced = listOf("find")
        ),

        // Level 7 — Size Matters
        Level(
            id = 7,
            title = "Size Matters",
            category = LevelCategory.FILE_BASICS,
            description = "Among many files, the password is in the one that is exactly 1033 bytes.",
            briefing = "Sometimes you need to find files based on their properties rather than their names. The 'find' command can filter by file size.",
            hints = listOf(
                "All files are in the data directory — check their sizes",
                "Use 'find ~/data -size 1033c' to find files of exactly 1033 bytes",
                "The 'c' suffix in -size means bytes (characters)"
            ),
            password = ps.getPassword(7),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(7)
                mkdirs(vfs, "/home/bandit0/data")

                val sizes = listOf(500, 750, 2000, 100, 1500, 300, 800, 1033, 1200, 450)
                for (i in 0..9) {
                    val name = "/home/bandit0/data/chunk%02d".format(i)
                    if (i == 7) {
                        val content = password + "\n"
                        val padded = content.padEnd(1033, ' ')
                        vfs.createFile(name, padded.toByteArray(), Permissions.fromOctal(644))
                    } else {
                        val filler = "x".repeat(sizes[i])
                        vfs.createFile(name, filler.toByteArray(), Permissions.fromOctal(644))
                    }
                }
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(7)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Finding files by size with find -size",
                "Size suffixes: c for bytes, k for kilobytes"
            ),
            commandsIntroduced = listOf("find -size")
        ),

        // Level 8 — Unique Content
        Level(
            id = 8,
            title = "Unique Content",
            category = LevelCategory.FILE_BASICS,
            description = "The password is the only line that appears exactly once in the data file.",
            briefing = "Data often has duplicates. Learning to find unique entries is a crucial skill for log analysis, data processing, and security forensics.",
            hints = listOf(
                "The file has many duplicate lines and one unique line",
                "First sort the file, then find unique lines: 'sort data.txt | uniq -u'",
                "The -u flag for uniq shows only lines that appear exactly once"
            ),
            password = ps.getPassword(8),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(8)
                val duplicateLines = listOf(
                    "alpha bravo charlie",
                    "delta echo foxtrot",
                    "golf hotel india",
                    "juliet kilo lima",
                    "mike november oscar",
                    "papa quebec romeo",
                    "sierra tango uniform",
                    "victor whiskey xray",
                    "yankee zulu apache"
                )
                val lines = mutableListOf<String>()
                for (line in duplicateLines) {
                    val repeats = if (duplicateLines.indexOf(line) % 2 == 0) 3 else 2
                    repeat(repeats) { lines.add(line) }
                }
                lines.add(password)
                lines.shuffle(java.util.Random(42))
                val content = lines.joinToString("\n") + "\n"
                vfs.createFile(
                    "/home/bandit0/data.txt",
                    content.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(8)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Sorting text with sort",
                "Finding unique lines with uniq",
                "Piping commands together with |"
            ),
            commandsIntroduced = listOf("sort", "uniq", "|")
        ),

        // Level 9 — Needle in Haystack
        Level(
            id = 9,
            title = "Needle in Haystack",
            category = LevelCategory.FILE_BASICS,
            description = "The password is on a line starting with 'password:' in a large data file.",
            briefing = "Searching through large files manually is tedious. The 'grep' command is one of the most powerful tools in Linux — it finds lines matching a pattern.",
            hints = listOf(
                "The password is somewhere in haystack.txt — don't read the whole file",
                "Use 'grep' to search for a specific pattern",
                "Try: grep 'password:' haystack.txt"
            ),
            password = ps.getPassword(9),
            setupFileSystem = { vfs ->
                val password = PasswordSystem().getPassword(9)
                val noiseLines = listOf(
                    "server-01 status: online",
                    "error: connection timeout on port 8080",
                    "user logged in from 192.168.1.1",
                    "debug: cache miss for key session_abc",
                    "warning: disk usage at 78%",
                    "info: scheduled backup completed",
                    "server-02 status: degraded",
                    "notice: new deployment v2.4.1 rolling out",
                    "error: failed to resolve hostname db.internal",
                    "user logged out session id 7fa3b",
                    "info: certificate renewed for *.example.com",
                    "debug: query took 342ms on table orders",
                    "warning: memory usage exceeded 90% threshold",
                    "server-03 status: online",
                    "error: permission denied for /var/log/secure",
                    "info: cron job cleanup_tmp executed successfully",
                    "notice: firewall rule updated — allow 443/tcp",
                    "debug: websocket connection established client_id=9x2k",
                    "warning: rate limit approaching for api-gateway",
                    "info: log rotation completed for access.log",
                    "server-04 status: maintenance",
                    "error: SSL handshake failed with upstream",
                    "debug: DNS resolution took 12ms for cdn.example.com",
                    "notice: user account bandit12 locked after 5 failed attempts",
                    "info: health check passed all 6 endpoints",
                    "warning: swap usage detected — consider adding memory",
                    "error: timeout waiting for response from auth-service",
                    "debug: loaded 2048 entries from configuration cache",
                    "info: system uptime 47 days 12 hours 33 minutes"
                )
                val lines = mutableListOf<String>()
                lines.addAll(noiseLines)
                lines.add("password: $password")
                lines.shuffle(java.util.Random(99))
                val content = lines.joinToString("\n") + "\n"
                vfs.createFile(
                    "/home/bandit0/haystack.txt",
                    content.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, lastOutput ->
                val password = PasswordSystem().getPassword(9)
                lastOutput.contains(password)
            },
            teachingPoints = listOf(
                "Searching file contents with grep",
                "Pattern matching basics"
            ),
            commandsIntroduced = listOf("grep")
        )
    )
}
