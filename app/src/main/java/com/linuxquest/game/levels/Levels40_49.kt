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

fun createScriptingLevels(): List<Level> {
    val ps = PasswordSystem()

    return listOf(
        // Level 40 — Variable World
        run {
            val password = ps.getPassword(40)
            val part1 = password.substring(0, 16)
            val part2 = password.substring(16, 32)
            Level(
                id = 40,
                title = "Variable World",
                category = LevelCategory.SCRIPTING,
                description = "Read the script file to understand how to construct the password from variables.",
                briefing = "Shell variables store data for reuse. Understanding how to set, read, and combine variables is the foundation of shell scripting.",
                hints = listOf(
                    "Read the script file to see the variables",
                    "The password is split into two variables",
                    "Run the script with bash — or echo the variables yourself"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val scriptName = r.randomFileName() + ".sh"
                    val readmeName = r.randomFileNameWithExt()

                    val scriptContent = "#!/bin/bash\nPART1=\"$part1\"\nPART2=\"$part2\"\necho \"\$PART1\$PART2\"\n"
                    vfs.createFile(
                        "/home/bandit0/$scriptName",
                        scriptContent.toByteArray(),
                        Permissions.fromOctal(755)
                    )
                    vfs.chown("/home/bandit0/$scriptName", "bandit0", "bandit0")

                    val readmeContent = "Run the script $scriptName to get the password, or figure out the variables yourself.\n"
                    vfs.createFile(
                        "/home/bandit0/$readmeName",
                        readmeContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$readmeName", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Shell variables",
                    "Variable assignment and expansion",
                    "String concatenation"
                ),
                commandsIntroduced = listOf("echo", "\$VARIABLE", "bash")
            )
        },

        // Level 41 — Exit Status
        run {
            val password = ps.getPassword(41)
            Level(
                id = 41,
                title = "Exit Status",
                category = LevelCategory.SCRIPTING,
                description = "Find which command in the list succeeds (exit code 0). Its output contains the password.",
                briefing = "Every command returns an exit status: 0 for success, non-zero for failure. The special variable \$? holds the last command's exit code.",
                hints = listOf(
                    "Try each command — check which one succeeds",
                    "Use 'echo \$?' after each command to see the exit code",
                    "The command that returns 0 (success) has the password"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val commandsFile = r.randomFileNameWithExt()
                    val correctFile = r.randomFileNameWithExt()

                    val commandsContent = buildString {
                        appendLine("cat /nonexistent/file")
                        appendLine("cat /home/bandit0/wrong.txt")
                        appendLine("cat /home/bandit0/$correctFile")
                        appendLine("cat /missing/path")
                    }
                    vfs.createFile(
                        "/home/bandit0/$commandsFile",
                        commandsContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$commandsFile", "bandit0", "bandit0")

                    vfs.createFile(
                        "/home/bandit0/$correctFile",
                        "$password\n".toByteArray()
                    )
                    vfs.chown("/home/bandit0/$correctFile", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Exit status codes",
                    "Checking \$? for success/failure",
                    "Error handling basics"
                ),
                commandsIntroduced = listOf("\$?", "echo \$?")
            )
        },

        // Level 42 — If Then Else
        run {
            val password = ps.getPassword(42)
            Level(
                id = 42,
                title = "If Then Else",
                category = LevelCategory.SCRIPTING,
                description = "A script checks a condition to decide which file contains the password. Read the script to find the answer.",
                briefing = "Conditional logic with if/then/else lets scripts make decisions. Understanding conditionals is key to writing useful scripts.",
                hints = listOf(
                    "Read the script to understand the conditional logic",
                    "The script checks if a flag file exists",
                    "The flag file exists, so the 'if' branch runs — read the file it cats"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val scriptName = r.randomFileName() + ".sh"
                    val flagFile = r.randomFileNameWithExt()
                    val alphaFile = r.randomFileNameWithExt()
                    val betaFile = r.randomFileNameWithExt()

                    val scriptContent = buildString {
                        appendLine("#!/bin/bash")
                        appendLine("if [ -f /home/bandit0/$flagFile ]; then")
                        appendLine("    cat /home/bandit0/$alphaFile")
                        appendLine("else")
                        appendLine("    cat /home/bandit0/$betaFile")
                        appendLine("fi")
                    }
                    vfs.createFile(
                        "/home/bandit0/$scriptName",
                        scriptContent.toByteArray(),
                        Permissions.fromOctal(755)
                    )
                    vfs.chown("/home/bandit0/$scriptName", "bandit0", "bandit0")

                    vfs.createFile("/home/bandit0/$flagFile", ByteArray(0))
                    vfs.chown("/home/bandit0/$flagFile", "bandit0", "bandit0")

                    vfs.createFile(
                        "/home/bandit0/$alphaFile",
                        "$password\n".toByteArray()
                    )
                    vfs.chown("/home/bandit0/$alphaFile", "bandit0", "bandit0")

                    vfs.createFile(
                        "/home/bandit0/$betaFile",
                        "Wrong path!\n".toByteArray()
                    )
                    vfs.chown("/home/bandit0/$betaFile", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "If/then/else conditionals",
                    "File test operators (-f, -d, -e)",
                    "Script control flow"
                ),
                commandsIntroduced = listOf("if", "then", "else", "fi", "test")
            )
        },

        // Level 43 — Loop De Loop
        run {
            val password = ps.getPassword(43)
            val segments = (0 until 8).map { i ->
                password.substring(i * 4, i * 4 + 4)
            }
            Level(
                id = 43,
                title = "Loop De Loop",
                category = LevelCategory.SCRIPTING,
                description = "The password is split across numbered files. Use a loop to concatenate them in order.",
                briefing = "Loops let you repeat operations — essential for processing multiple files, iterating over data, or automating repetitive tasks.",
                hints = listOf(
                    "The password is split across 8 fragment files",
                    "Use a for loop to read them in order",
                    "Try: for i in \$(seq 1 8); do cat <dir>/frag_\$i; done"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val fragDir = r.randomDirName()

                    mkdirs(vfs, "/home/bandit0/$fragDir")
                    vfs.chown("/home/bandit0/$fragDir", "bandit0", "bandit0")

                    for (i in 1..8) {
                        val path = "/home/bandit0/$fragDir/frag_$i"
                        vfs.createFile(path, segments[i - 1].toByteArray())
                        vfs.chown(path, "bandit0", "bandit0")
                    }

                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "For loops in bash",
                    "Iterating over sequences",
                    "Automating multi-file operations"
                ),
                commandsIntroduced = listOf("for", "do", "done", "seq")
            )
        },

        // Level 44 — Function Call
        run {
            val password = ps.getPassword(44)
            val reversed = password.reversed()
            Level(
                id = 44,
                title = "Function Call",
                category = LevelCategory.SCRIPTING,
                description = "Define a function that reverses a string, then use it to decode the password.",
                briefing = "Functions in shell scripts let you reuse code. They can accept arguments and return results — building blocks for larger scripts.",
                hints = listOf(
                    "One of the files contains the password but reversed",
                    "You can write a bash function to reverse it",
                    "Simpler: pipe the file through 'rev' to reverse the string"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val reversedFile = r.randomFileNameWithExt()
                    val readmeName = r.randomFileNameWithExt()

                    vfs.createFile(
                        "/home/bandit0/$reversedFile",
                        "$reversed\n".toByteArray()
                    )
                    vfs.chown("/home/bandit0/$reversedFile", "bandit0", "bandit0")

                    val readmeContent = "The password is stored reversed in $reversedFile. Write a function to reverse it back, or use the rev command.\n"
                    vfs.createFile(
                        "/home/bandit0/$readmeName",
                        readmeContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$readmeName", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Bash functions",
                    "The rev command",
                    "String manipulation in shell"
                ),
                commandsIntroduced = listOf("rev", "function")
            )
        },

        // Level 45 — Script Args
        run {
            val password = ps.getPassword(45)
            Level(
                id = 45,
                title = "Script Args",
                category = LevelCategory.SCRIPTING,
                description = "A script expects the correct argument to reveal the password. Find the right argument.",
                briefing = "Scripts can accept command-line arguments via \$1, \$2, etc. Processing arguments is how scripts become flexible and reusable tools.",
                hints = listOf(
                    "The script needs an argument — read the script source code",
                    "Look at what value \$1 is compared against",
                    "Run the script with the correct passphrase as argument"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val scriptName = r.randomFileName() + ".sh"
                    val readmeName = r.randomFileNameWithExt()

                    val scriptContent = buildString {
                        appendLine("#!/bin/bash")
                        appendLine("if [ \"\$1\" = \"opensesame\" ]; then")
                        appendLine("    echo \"$password\"")
                        appendLine("else")
                        appendLine("    echo \"Wrong passphrase! Hint: think of Ali Baba.\"")
                        appendLine("fi")
                    }
                    vfs.createFile(
                        "/home/bandit0/$scriptName",
                        scriptContent.toByteArray(),
                        Permissions.fromOctal(755)
                    )
                    vfs.chown("/home/bandit0/$scriptName", "bandit0", "bandit0")

                    val readmeContent = "Run $scriptName with the correct argument to get the password.\n"
                    vfs.createFile(
                        "/home/bandit0/$readmeName",
                        readmeContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$readmeName", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Script arguments (\$1, \$2, \$@)",
                    "Processing command-line arguments",
                    "Argument validation"
                ),
                commandsIntroduced = listOf("\$1", "\$@", "\$#")
            )
        },

        // Level 46 — Trap Handler
        run {
            val password = ps.getPassword(46)
            Level(
                id = 46,
                title = "Trap Handler",
                category = LevelCategory.SCRIPTING,
                description = "A script sets up a signal trap. Understand it to find the password.",
                briefing = "Signal traps let scripts handle interrupts and events gracefully. They're used for cleanup, logging, and controlling script behavior.",
                hints = listOf(
                    "Read the script source code carefully",
                    "The trap command defines what happens when a signal is received",
                    "The SECRET variable in the script contains the password"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val scriptName = r.randomFileName() + ".sh"

                    val scriptContent = buildString {
                        appendLine("#!/bin/bash")
                        appendLine("SECRET=\"$password\"")
                        appendLine("trap 'echo \"Signal caught! The secret is: \$SECRET\"' INT TERM")
                        appendLine("echo \"This script traps signals.\"")
                        appendLine("echo \"Send it SIGINT (Ctrl+C) to reveal the secret.\"")
                        appendLine("echo \"Or just read the source code...\"")
                        appendLine("sleep 10")
                    }
                    vfs.createFile(
                        "/home/bandit0/$scriptName",
                        scriptContent.toByteArray(),
                        Permissions.fromOctal(755)
                    )
                    vfs.chown("/home/bandit0/$scriptName", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(3)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Signal handling with trap",
                    "Common signals (INT, TERM, EXIT)",
                    "Script cleanup patterns"
                ),
                commandsIntroduced = listOf("trap", "kill")
            )
        },

        // Level 47 — Here & There
        run {
            val password = ps.getPassword(47)
            Level(
                id = 47,
                title = "Here & There",
                category = LevelCategory.SCRIPTING,
                description = "Use a heredoc to understand the pattern, then find the password in the generated file.",
                briefing = "Heredocs (here documents) let you embed multi-line text in scripts. They're perfect for generating configuration files, emails, or templates.",
                hints = listOf(
                    "Look at the generator script — it creates a file with a heredoc",
                    "The output file has already been generated",
                    "Read the output file or the script itself to find the password"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val generatorName = r.randomFileName() + ".sh"
                    val outputName = r.randomFileNameWithExt()

                    val scriptContent = buildString {
                        appendLine("#!/bin/bash")
                        appendLine("cat << 'EOF' > /home/bandit0/$outputName")
                        appendLine("Configuration Report")
                        appendLine("====================")
                        appendLine("Server: linuxquest")
                        appendLine("Password: $password")
                        appendLine("Status: active")
                        appendLine("EOF")
                        appendLine("echo \"File generated at /home/bandit0/$outputName\"")
                    }
                    vfs.createFile(
                        "/home/bandit0/$generatorName",
                        scriptContent.toByteArray(),
                        Permissions.fromOctal(755)
                    )
                    vfs.chown("/home/bandit0/$generatorName", "bandit0", "bandit0")

                    val outputContent = buildString {
                        appendLine("Configuration Report")
                        appendLine("====================")
                        appendLine("Server: linuxquest")
                        appendLine("Password: $password")
                        appendLine("Status: active")
                    }
                    vfs.createFile(
                        "/home/bandit0/$outputName",
                        outputContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$outputName", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Heredoc syntax (<<)",
                    "Generating files from scripts",
                    "Quoted vs unquoted heredoc delimiters"
                ),
                commandsIntroduced = listOf("<<", "heredoc")
            )
        },

        // Level 48 — Process Sub
        run {
            val password = ps.getPassword(48)
            val decoys = listOf(
                "0a1b2c3d4e5f6a7b",
                "1122334455667788",
                "1a2b3c4d5e6f7a8b",
                "2233445566778899",
                "2b3c4d5e6f7a8b9c",
                "3344556677889900",
                "3c4d5e6f7a8b9c0d",
                "4455667788990011",
                "4d5e6f7a8b9c0d1e",
                "5566778899001122",
                "5e6f7a8b9c0d1e2f",
                "6677889900112233",
                "6f7a8b9c0d1e2f3a",
                "7788990011223344",
                "7a8b9c0d1e2f3a4b",
                "8899001122334455",
                "8b9c0d1e2f3a4b5c",
                "9900112233445566",
                "9c0d1e2f3a4b5c6d",
                "aabbccddeeff0011"
            )
            val listA = decoys.sorted()
            val listB = (decoys + password).sorted()
            Level(
                id = 48,
                title = "Process Sub",
                category = LevelCategory.SCRIPTING,
                description = "Compare two command outputs to find the password difference.",
                briefing = "Process substitution <() lets you use command output as if it were a file. It's perfect for comparing outputs of different commands.",
                hints = listOf(
                    "Compare the two list files to find the extra entry",
                    "Use diff to compare them, or use process substitution",
                    "Try: diff <file_a> <file_b> — or: comm -13 <file_a> <file_b>"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val listAName = r.randomFileNameWithExt()
                    val listBName = r.randomFileNameWithExt()

                    val listAContent = listA.joinToString("\n") + "\n"
                    val listBContent = listB.joinToString("\n") + "\n"

                    vfs.createFile(
                        "/home/bandit0/$listAName",
                        listAContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$listAName", "bandit0", "bandit0")

                    vfs.createFile(
                        "/home/bandit0/$listBName",
                        listBContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$listBName", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Process substitution with <()",
                    "Comparing command outputs",
                    "Using comm for sorted file comparison"
                ),
                commandsIntroduced = listOf("<()", "comm", "diff")
            )
        },

        // Level 49 — Cron Time
        run {
            val password = ps.getPassword(49)
            Level(
                id = 49,
                title = "Cron Time",
                category = LevelCategory.SCRIPTING,
                description = "Parse the crontab file to find when and what script runs. The script contains the password.",
                briefing = "Cron is the Linux task scheduler. Crontab files define when commands run automatically. Understanding cron expressions is essential for system administration.",
                hints = listOf(
                    "Read the crontab file to see what scripts are scheduled",
                    "Look at the script paths — one is in your home directory",
                    "Read the script in your home directory to find the password variable"
                ),
                password = password,
                setupFileSystem = { vfs, seed ->
                    val r = LevelRandomizer(seed)
                    val crontabName = r.randomFileNameWithExt()
                    val scriptsDir = r.randomDirName()
                    val reportScript = r.randomFileName() + ".sh"

                    val crontabContent = buildString {
                        appendLine("# LinuxQuest Cron Jobs")
                        appendLine("# m h dom mon dow command")
                        appendLine("*/5 * * * * /usr/bin/health_check.sh")
                        appendLine("0 2 * * * /opt/backup/daily_backup.sh")
                        appendLine("30 8 * * 1 /home/bandit0/$scriptsDir/$reportScript")
                        appendLine("0 0 1 * * /var/log/rotate.sh")
                    }
                    vfs.createFile(
                        "/home/bandit0/$crontabName",
                        crontabContent.toByteArray()
                    )
                    vfs.chown("/home/bandit0/$crontabName", "bandit0", "bandit0")

                    mkdirs(vfs, "/home/bandit0/$scriptsDir")
                    vfs.chown("/home/bandit0/$scriptsDir", "bandit0", "bandit0")

                    val reportContent = buildString {
                        appendLine("#!/bin/bash")
                        appendLine("# Weekly report generator")
                        appendLine("PASSWORD=\"$password\"")
                        appendLine("echo \"Weekly report generated with code: \$PASSWORD\"")
                    }
                    vfs.createFile(
                        "/home/bandit0/$scriptsDir/$reportScript",
                        reportContent.toByteArray(),
                        Permissions.fromOctal(755)
                    )
                    vfs.chown("/home/bandit0/$scriptsDir/$reportScript", "bandit0", "bandit0")

                    for ((name, content) in r.randomDecoyFiles(2)) {
                        vfs.createFile("/home/bandit0/$name", content.toByteArray())
                        vfs.chown("/home/bandit0/$name", "bandit0", "bandit0")
                    }
                },
                validateCompletion = { _, output -> output.contains(password) },
                teachingPoints = listOf(
                    "Understanding cron expressions",
                    "Crontab format (minute hour day month weekday)",
                    "Scheduled task management"
                ),
                commandsIntroduced = listOf("crontab", "cron")
            )
        }
    )
}
