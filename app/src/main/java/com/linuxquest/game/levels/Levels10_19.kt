package com.linuxquest.game.levels

import com.linuxquest.filesystem.Permissions
import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.game.Level
import com.linuxquest.game.LevelCategory
import com.linuxquest.game.LevelRandomizer
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

fun createTextProcessingLevels(): List<Level> {
    val ps = PasswordSystem()

    return listOf(
        // Level 10 — Pipe Dream
        run {
            val password = ps.getPassword(10)
            Level(
                id = 10,
                title = "Pipe Dream",
                category = LevelCategory.TEXT_PROCESSING,
                description = "Find the only line that occurs exactly once in the data file.",
                briefing = "Combining commands with pipes is the heart of Unix philosophy. Each command does one thing well, and pipes connect them into powerful workflows.",
                hints = listOf(
                    "The file has many lines, most appear multiple times",
                    "Use 'sort data.txt | uniq -c' to count occurrences",
                    "Chain it: 'sort data.txt | uniq -c | sort -n' — the line with count 1 is the password"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val dataFile = r.randomFileNameWithExt()
                    val lines = mutableListOf<String>()
                    val fakeData = listOf(
                        "alpha bravo charlie" to 3,
                        "delta echo foxtrot" to 2,
                        "golf hotel india" to 3,
                        "juliet kilo lima" to 2,
                        "mike november oscar" to 2,
                        "papa quebec romeo" to 3,
                        "sierra tango uniform" to 2,
                        "victor whiskey xray" to 3,
                        "yankee zulu one" to 2,
                        "two three four" to 2
                    )
                    for ((line, count) in fakeData) {
                        repeat(count) { lines.add(line) }
                    }
                    lines.add(password)
                    lines.shuffle()
                    vfs.writeFileText("/home/bandit0/$dataFile", lines.joinToString("\n") + "\n")
                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Combining sort and uniq with count",
                    "Multi-stage pipelines",
                    "Sorting numerically with sort -n"
                ),
                commandsIntroduced = listOf("sort", "uniq -c", "sort -n")
            )
        },

        // Level 11 — Head & Tail
        run {
            val password = ps.getPassword(11)
            Level(
                id = 11,
                title = "Head & Tail",
                category = LevelCategory.TEXT_PROCESSING,
                description = "The password is on line 42 of the data file.",
                briefing = "Sometimes you need a specific line from a file. The head and tail commands let you slice files precisely.",
                hints = listOf(
                    "The password is on a specific line number",
                    "Use 'head -n 42' to get the first 42 lines",
                    "Pipe them: 'head -n 42 data.txt | tail -n 1' to get only line 42"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val dataFile = r.randomFileNameWithExt()
                    val lines = (1..100).map { i ->
                        if (i == 42) password
                        else {
                            val num = String.format("%02d", i)
                            val words = listOf(
                                "alpha bravo", "charlie delta", "echo foxtrot",
                                "golf hotel", "india juliet", "kilo lima",
                                "mike november", "oscar papa", "quebec romeo",
                                "sierra tango"
                            )
                            "Line $num: ${words[(i - 1) % words.size]}"
                        }
                    }
                    vfs.writeFileText("/home/bandit0/$dataFile", lines.joinToString("\n") + "\n")
                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Extracting specific lines with head and tail",
                    "Combining head and tail in pipelines"
                ),
                commandsIntroduced = listOf("head", "tail")
            )
        },

        // Level 12 — Count & Sort
        run {
            val password = ps.getPassword(12)
            Level(
                id = 12,
                title = "Count & Sort",
                category = LevelCategory.TEXT_PROCESSING,
                description = "One of the files in the data directory has exactly 10 lines. The password is in that file.",
                briefing = "When dealing with many files, knowing their line counts helps you find the right one quickly.",
                hints = listOf(
                    "Check how many lines each file has",
                    "Use 'wc -l data/*' to count lines in all files",
                    "Find the file with 10 lines and read it"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val dirName = r.randomDirName()
                    mkdirs(vfs, "/home/bandit0/$dirName")
                    val fileCounts = listOf(3, 15, 7, 10, 22, 5, 18, 12)
                    for ((index, count) in fileCounts.withIndex()) {
                        val fileName = r.randomFileNameWithExt()
                        val lines = if (count == 10) {
                            (1..9).map { "filler data line $it" } + password
                        } else {
                            (1..count).map { "file${index + 1} filler line $it: some random text" }
                        }
                        vfs.writeFileText(
                            "/home/bandit0/$dirName/$fileName",
                            lines.joinToString("\n") + "\n"
                        )
                    }
                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.writeFileText("/home/bandit0/$dirName/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Counting lines with wc -l",
                    "Using wildcards to process multiple files"
                ),
                commandsIntroduced = listOf("wc", "wc -l")
            )
        },

        // Level 13 — Column Extraction
        run {
            val password = ps.getPassword(13)
            Level(
                id = 13,
                title = "Column Extraction",
                category = LevelCategory.TEXT_PROCESSING,
                description = "The password is in the third column of the CSV data file.",
                briefing = "Real-world data often comes in structured formats like CSV. Extracting specific fields is a fundamental data processing skill.",
                hints = listOf(
                    "The data is comma-separated — look at the structure",
                    "Use 'cut' to extract a specific column with a delimiter",
                    "Try: cut -d',' -f3 data.csv"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val csvFile = r.randomFileName() + ".csv"
                    val rows = listOf(
                        "name,email,code,department",
                        "alice,alice@corp.com,a1b2c3d4,engineering",
                        "bob,bob@corp.com,e5f6g7h8,marketing",
                        "charlie,charlie@corp.com,$password,engineering",
                        "diana,diana@corp.com,i9j0k1l2,sales",
                        "eve,eve@corp.com,m3n4o5p6,operations",
                        "frank,frank@corp.com,q7r8s9t0,engineering",
                        "grace,grace@corp.com,u1v2w3x4,marketing",
                        "hank,hank@corp.com,y5z6a7b8,sales",
                        "iris,iris@corp.com,c9d0e1f2,operations"
                    )
                    vfs.writeFileText("/home/bandit0/$csvFile", rows.joinToString("\n") + "\n")
                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Extracting columns with cut",
                    "Specifying delimiters with -d",
                    "Selecting fields with -f"
                ),
                commandsIntroduced = listOf("cut")
            )
        },

        // Level 14 — Character Swap
        run {
            val password = ps.getPassword(14)
            Level(
                id = 14,
                title = "Character Swap",
                category = LevelCategory.TEXT_PROCESSING,
                description = "The password has been ROT13 encoded. Decode it.",
                briefing = "ROT13 is a simple letter substitution cipher that replaces each letter with the letter 13 positions after it in the alphabet. It's the simplest cipher and is used to hide spoilers online.",
                hints = listOf(
                    "The text is encoded with ROT13 — a simple rotation cipher",
                    "Use the 'tr' command to translate characters",
                    "Try: cat encoded.txt | tr 'A-Za-z' 'N-ZA-Mn-za-m'"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val encodedFile = r.randomFileNameWithExt()
                    val encoded = password.map { ch ->
                        when (ch) {
                            in 'a'..'m' -> (ch + 13).toChar()
                            in 'n'..'z' -> (ch - 13).toChar()
                            in 'A'..'M' -> (ch + 13).toChar()
                            in 'N'..'Z' -> (ch - 13).toChar()
                            else -> ch
                        }
                    }.joinToString("")
                    vfs.writeFileText("/home/bandit0/$encodedFile", encoded + "\n")
                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Character translation with tr",
                    "Understanding ROT13 encoding"
                ),
                commandsIntroduced = listOf("tr")
            )
        },

        // Level 15 — Stream Editor
        run {
            val password = ps.getPassword(15)
            Level(
                id = 15,
                title = "Stream Editor",
                category = LevelCategory.TEXT_PROCESSING,
                description = "Extract the password from a configuration file where it's stored as a key=value pair.",
                briefing = "The stream editor 'sed' is a powerful tool for text transformation. It can search, replace, delete, and extract text using patterns.",
                hints = listOf(
                    "The password is stored in a key=value format in config.txt",
                    "Use 'sed' to extract text after the = sign",
                    "Try: grep 'password=' config.txt | sed 's/password=//'"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val configFile = r.randomFileNameWithExt()
                    val configLines = listOf(
                        "hostname=linuxquest",
                        "port=8080",
                        "debug=false",
                        "password=$password",
                        "timeout=30",
                        "mode=production"
                    )
                    vfs.writeFileText("/home/bandit0/$configFile", configLines.joinToString("\n") + "\n")
                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Text substitution with sed",
                    "Using sed for extraction",
                    "Pattern-based text processing"
                ),
                commandsIntroduced = listOf("sed")
            )
        },

        // Level 16 — Pattern Master
        run {
            val password = ps.getPassword(16)
            Level(
                id = 16,
                title = "Pattern Master",
                category = LevelCategory.TEXT_PROCESSING,
                description = "Extract the password from the third column of the tab-separated log file.",
                briefing = "AWK is a powerful pattern-scanning language. It excels at processing structured text data, especially when you need to work with specific fields.",
                hints = listOf(
                    "The data is tab-separated — look at the columns",
                    "AWK splits lines into fields automatically",
                    "Try: awk -F'\\t' '{print \$3}' access.log | grep -v action"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val logFile = r.randomFileNameWithExt()
                    val logLines = listOf(
                        "timestamp\tuser\taction\tstatus",
                        "2024-01-15\talice\tlogin\tSUCCESS",
                        "2024-01-15\tbob\tlogout\tSUCCESS",
                        "2024-01-16\tcharlie\tupload\tFAILED",
                        "2024-01-16\tadmin\t$password\tSUCCESS",
                        "2024-01-17\talice\tdownload\tSUCCESS",
                        "2024-01-17\tbob\tdelete\tFAILED",
                        "2024-01-18\tcharlie\tlogin\tSUCCESS",
                        "2024-01-18\tdiana\tupload\tSUCCESS",
                        "2024-01-19\teve\tlogout\tSUCCESS"
                    )
                    vfs.writeFileText("/home/bandit0/$logFile", logLines.joinToString("\n") + "\n")
                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Field extraction with awk",
                    "Setting field separator with -F",
                    "Printing specific fields"
                ),
                commandsIntroduced = listOf("awk")
            )
        },

        // Level 17 — Spot the Diff
        run {
            val password = ps.getPassword(17)
            Level(
                id = 17,
                title = "Spot the Diff",
                category = LevelCategory.TEXT_PROCESSING,
                description = "Two files are nearly identical. The password is the line that differs between them.",
                briefing = "Comparing files to find differences is essential for debugging, code review, and security analysis. The 'diff' command is your go-to tool.",
                hints = listOf(
                    "Compare the two files to find what changed",
                    "Use the 'diff' command to compare them",
                    "Try: diff file1.txt file2.txt — the changed line contains the password"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val fileName1 = r.randomFileNameWithExt()
                    val fileName2 = r.randomFileNameWithExt()
                    val commonLines = (1..20).map { i ->
                        if (i == 12) null
                        else "This is line $i of the shared data file content."
                    }
                    val file1Lines = commonLines.map { it ?: "placeholder_text" }
                    val file2Lines = commonLines.map { it ?: password }
                    vfs.writeFileText("/home/bandit0/$fileName1", file1Lines.joinToString("\n") + "\n")
                    vfs.writeFileText("/home/bandit0/$fileName2", file2Lines.joinToString("\n") + "\n")
                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Comparing files with diff",
                    "Reading diff output format"
                ),
                commandsIntroduced = listOf("diff")
            )
        },

        // Level 18 — Split Stream
        run {
            val password = ps.getPassword(18)
            Level(
                id = 18,
                title = "Split Stream",
                category = LevelCategory.TEXT_PROCESSING,
                description = "Read the input file and write its content to output.txt while also viewing it. The password is in the input.",
                briefing = "Sometimes you need to both see data and save it to a file simultaneously. The 'tee' command splits a stream, sending it to both a file and standard output.",
                hints = listOf(
                    "You need to view the file AND save it somewhere else",
                    "The 'tee' command writes to both a file and stdout",
                    "Try: cat input.txt | tee output.txt"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val inputFile = r.randomFileNameWithExt()
                    val lines = listOf(
                        "System initialization complete.",
                        "Loading configuration...",
                        "Configuration loaded successfully.",
                        password,
                        "Starting services...",
                        "All services running.",
                        "Ready for input."
                    )
                    vfs.writeFileText("/home/bandit0/$inputFile", lines.joinToString("\n") + "\n")
                    val hiddenFile = r.randomHiddenFileName()
                    vfs.writeFileText("/home/bandit0/$hiddenFile", "Use tee to complete this level.\n")
                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.writeFileText("/home/bandit0/$name", content + "\n")
                    }
                },
                validateCompletion = { vfs, output ->
                    output.contains(password) && try {
                        vfs.readFileText("/home/bandit0/output.txt").contains(password)
                    } catch (_: Exception) { false }
                },
                teachingPoints = listOf(
                    "Splitting output with tee",
                    "Writing to file and stdout simultaneously"
                ),
                commandsIntroduced = listOf("tee")
            )
        },

        // Level 19 — Build Commands
        run {
            val password = ps.getPassword(19)
            Level(
                id = 19,
                title = "Build Commands",
                category = LevelCategory.TEXT_PROCESSING,
                description = "A directory contains numbered files with 'part' in their name. Concatenate them in order to reveal the password.",
                briefing = "The xargs command builds and executes commands from standard input. Combined with find and sort, it's a powerful way to process multiple files.",
                hints = listOf(
                    "The password is split across multiple numbered files containing 'part' in the name",
                    "Use 'ls' to find the directory, then look for files with 'part' in the name",
                    "Try: find <dir> -name '*part*' | sort | xargs cat"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val dirName = r.randomDirName()
                    mkdirs(vfs, "/home/bandit0/$dirName")
                    val baseName = r.randomFileNameWithKeyword("part")
                    val segments = listOf(
                        password.substring(0, 6),
                        password.substring(6, 12),
                        password.substring(12, 19),
                        password.substring(19, 26),
                        password.substring(26, 32)
                    )
                    for ((index, segment) in segments.withIndex()) {
                        val fileName = String.format("${baseName}%02d", index + 1)
                        vfs.writeFileText("/home/bandit0/$dirName/$fileName", segment)
                    }
                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.writeFileText("/home/bandit0/$dirName/$name", content + "\n")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Building commands with xargs",
                    "Combining find, sort, and xargs",
                    "Processing multiple files in order"
                ),
                commandsIntroduced = listOf("xargs", "find | sort | xargs")
            )
        }
    )
}
