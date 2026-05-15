package com.linuxquest.game.levels

import com.linuxquest.filesystem.Permissions
import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.game.Level
import com.linuxquest.game.LevelCategory
import com.linuxquest.game.PasswordSystem
import java.util.Base64

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

fun createMasterLevels(): List<Level> {
    val ps = PasswordSystem()

    val password50 = ps.getPassword(50)
    val password51 = ps.getPassword(51)
    val password52 = ps.getPassword(52)
    val password53 = ps.getPassword(53)
    val password54 = ps.getPassword(54)
    val password55 = ps.getPassword(55)

    return listOf(
        // Level 50 — Log Detective
        Level(
            id = 50,
            title = "Log Detective",
            category = LevelCategory.MASTER,
            description = "Analyze the web server access log to find which IP accessed /secret the most. The password is in that IP's session file.",
            briefing = "Log analysis is a critical skill for system administrators and security analysts. Real investigations start with parsing logs.",
            password = password50,
            setupFileSystem = { vfs ->
                val accessLog = buildString {
                    appendLine("""192.168.1.10 - - [15/Jan/2024:10:00:01] "GET /index.html HTTP/1.1" 200 1024""")
                    appendLine("""192.168.1.20 - - [15/Jan/2024:10:00:02] "GET /about HTTP/1.1" 200 512""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:03] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.10 - - [15/Jan/2024:10:00:04] "GET /api/data HTTP/1.1" 200 2048""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:05] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.30 - - [15/Jan/2024:10:00:06] "GET /index.html HTTP/1.1" 200 1024""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:07] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.30 - - [15/Jan/2024:10:00:08] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.10 - - [15/Jan/2024:10:00:09] "GET /contact HTTP/1.1" 200 768""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:10] "GET /index.html HTTP/1.1" 200 1024""")
                    appendLine("""192.168.1.20 - - [15/Jan/2024:10:00:11] "GET /api/users HTTP/1.1" 200 4096""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:12] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.10 - - [15/Jan/2024:10:00:13] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.20 - - [15/Jan/2024:10:00:14] "GET /images/logo.png HTTP/1.1" 200 8192""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:15] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.30 - - [15/Jan/2024:10:00:16] "GET /about HTTP/1.1" 200 512""")
                    appendLine("""192.168.1.10 - - [15/Jan/2024:10:00:17] "GET /api/data HTTP/1.1" 200 2048""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:18] "GET /api/status HTTP/1.1" 200 128""")
                    appendLine("""192.168.1.20 - - [15/Jan/2024:10:00:19] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:20] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.30 - - [15/Jan/2024:10:00:21] "GET /contact HTTP/1.1" 200 768""")
                    appendLine("""192.168.1.10 - - [15/Jan/2024:10:00:22] "GET /index.html HTTP/1.1" 200 1024""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:23] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.20 - - [15/Jan/2024:10:00:24] "GET /about HTTP/1.1" 200 512""")
                    appendLine("""192.168.1.30 - - [15/Jan/2024:10:00:25] "GET /api/data HTTP/1.1" 200 2048""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:26] "GET /login HTTP/1.1" 200 512""")
                    appendLine("""192.168.1.10 - - [15/Jan/2024:10:00:27] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""10.0.0.5 - - [15/Jan/2024:10:00:28] "GET /secret HTTP/1.1" 200 256""")
                    appendLine("""192.168.1.20 - - [15/Jan/2024:10:00:29] "GET /contact HTTP/1.1" 200 768""")
                    appendLine("""192.168.1.30 - - [15/Jan/2024:10:00:30] "GET /index.html HTTP/1.1" 200 1024""")
                }
                vfs.createFile("/var/log/access.log", accessLog.toByteArray())

                mkdirs(vfs, "/home/bandit0/sessions")
                vfs.createFile("/home/bandit0/sessions/192.168.1.10.txt", "Session data: normal user activity".toByteArray())
                vfs.createFile("/home/bandit0/sessions/10.0.0.5.txt", password50.toByteArray())
                vfs.createFile("/home/bandit0/sessions/192.168.1.20.txt", "Session data: browsing activity".toByteArray())
                vfs.createFile("/home/bandit0/sessions/192.168.1.30.txt", "Session data: single access".toByteArray())
            },
            validateCompletion = { _, output -> output.contains(password50) },
            hints = listOf(
                "Analyze /var/log/access.log for requests to /secret",
                "Use grep and awk to count IPs: grep '/secret' /var/log/access.log | awk '{print \$1}' | sort | uniq -c | sort -rn",
                "The top IP is 10.0.0.5 — check its session file"
            ),
            teachingPoints = listOf(
                "Apache log format analysis",
                "Combining grep, awk, sort, uniq for analysis",
                "IP-based investigation"
            ),
            commandsIntroduced = listOf("grep", "awk", "sort", "uniq -c")
        ),

        // Level 51 — System Monitor
        Level(
            id = 51,
            title = "System Monitor",
            category = LevelCategory.MASTER,
            description = "Find the suspicious process in the process list. Its command arguments contain the password.",
            briefing = "Monitoring running processes is essential for system health and security. A rogue process might leak sensitive data in its command-line arguments.",
            password = password51,
            setupFileSystem = { vfs ->
                val psOutput = buildString {
                    appendLine("USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND")
                    appendLine("root         1  0.0  0.1   1234   567 ?        Ss   10:00   0:01 /sbin/init")
                    appendLine("root       102  0.0  0.2   2345   890 ?        S    10:00   0:00 /usr/sbin/sshd")
                    appendLine("www-data   201  0.1  0.5   5678  1234 ?        S    10:01   0:05 /usr/sbin/apache2")
                    appendLine("bandit0    305  0.0  0.1   1111   222 pts/0    S    10:02   0:00 bash")
                    appendLine("root       666 99.0  0.1   9999   333 ?        R    10:05   0:00 /tmp/.hidden/backdoor --key=$password51")
                    appendLine("bandit0    789  0.0  0.0    500   100 pts/0    R+   10:10   0:00 ps aux")
                }
                vfs.createFile("/home/bandit0/ps_output.txt", psOutput.toByteArray())
            },
            validateCompletion = { _, output -> output.contains(password51) },
            hints = listOf(
                "Look at the process list for anything unusual",
                "Check for high CPU usage or suspicious paths",
                "PID 666 is running from /tmp/.hidden/ — the --key argument has the password"
            ),
            teachingPoints = listOf(
                "Reading ps output",
                "Identifying suspicious processes",
                "Process security monitoring"
            ),
            commandsIntroduced = listOf("ps", "ps aux")
        ),

        // Level 52 — Network Recon
        Level(
            id = 52,
            title = "Network Recon",
            category = LevelCategory.MASTER,
            description = "Parse the nmap scan results to find the open port running an unusual service. The service banner contains the password.",
            briefing = "Network reconnaissance with nmap reveals open ports and services. Analyzing scan results is fundamental to network security assessment.",
            password = password52,
            setupFileSystem = { vfs ->
                val scanResults = buildString {
                    appendLine("Starting Nmap 7.94 ( https://nmap.org )")
                    appendLine("Nmap scan report for target.linuxquest.local (10.10.10.1)")
                    appendLine("Host is up (0.001s latency).")
                    appendLine()
                    appendLine("PORT      STATE    SERVICE     VERSION")
                    appendLine("22/tcp    open     ssh         OpenSSH 8.9")
                    appendLine("80/tcp    open     http        Apache 2.4.52")
                    appendLine("443/tcp   open     https       Apache 2.4.52")
                    appendLine("3306/tcp  filtered mysql")
                    appendLine("8080/tcp  open     http-proxy  --")
                    appendLine("31337/tcp open     unknown     $password52")
                    appendLine("9090/tcp  closed   zeus-admin")
                    appendLine()
                    appendLine("Service detection performed.")
                    appendLine("Nmap done: 1 IP address (1 host up) scanned in 12.34 seconds")
                }
                vfs.createFile("/home/bandit0/scan_results.txt", scanResults.toByteArray())
            },
            validateCompletion = { _, output -> output.contains(password52) },
            hints = listOf(
                "Look at the nmap scan results for unusual ports",
                "Port 31337 is a classic hacker port number",
                "The service version on port 31337 contains the password"
            ),
            teachingPoints = listOf(
                "Reading nmap scan results",
                "Common and suspicious port numbers",
                "Network service identification"
            ),
            commandsIntroduced = listOf("nmap", "grep")
        ),

        // Level 53 — Security Audit
        Level(
            id = 53,
            title = "Security Audit",
            category = LevelCategory.MASTER,
            description = "Find world-writable files and SUID binaries to locate the password.",
            briefing = "Security auditing involves finding misconfigurations. World-writable files and unauthorized SUID binaries are common security risks.",
            password = password53,
            setupFileSystem = { vfs ->
                mkdirs(vfs, "/home/bandit0/system/bin")
                mkdirs(vfs, "/home/bandit0/system/data")
                mkdirs(vfs, "/home/bandit0/system/tmp")

                vfs.createFile(
                    "/home/bandit0/system/bin/normal_app",
                    "Normal application".toByteArray(),
                    Permissions.fromOctal(755)
                )
                vfs.chown("/home/bandit0/system/bin/normal_app", "root", "root")

                vfs.createFile(
                    "/home/bandit0/system/bin/suspicious_suid",
                    "SUID binary: look at world-writable files".toByteArray(),
                    Permissions.fromOctal(4755)
                )
                vfs.chown("/home/bandit0/system/bin/suspicious_suid", "root", "root")

                vfs.createFile(
                    "/home/bandit0/system/data/config.txt",
                    "Normal config".toByteArray(),
                    Permissions.fromOctal(644)
                )

                vfs.createFile(
                    "/home/bandit0/system/data/world_writable.txt",
                    password53.toByteArray(),
                    Permissions.fromOctal(666)
                )

                vfs.createFile(
                    "/home/bandit0/system/data/readonly.txt",
                    "Read only data".toByteArray(),
                    Permissions.fromOctal(444)
                )

                vfs.createFile(
                    "/home/bandit0/system/tmp/notes.txt",
                    "Decoy".toByteArray(),
                    Permissions.fromOctal(777)
                )
            },
            validateCompletion = { _, output -> output.contains(password53) },
            hints = listOf(
                "Look for files with insecure permissions",
                "Find world-writable files: find . -perm -o+w -type f",
                "The world-writable file in system/data/ contains the password"
            ),
            teachingPoints = listOf(
                "Finding world-writable files",
                "Identifying SUID binaries",
                "Security permission auditing"
            ),
            commandsIntroduced = listOf("find -perm", "ls -l")
        ),

        // Level 54 — Automation Pro
        Level(
            id = 54,
            title = "Automation Pro",
            category = LevelCategory.MASTER,
            description = "Process multiple data files: find the ones with 'VALID' marker, extract their codes, and combine them to get the password.",
            briefing = "Real system administration requires chaining multiple operations. This challenge combines everything you've learned into an automated workflow.",
            password = password54,
            setupFileSystem = { vfs ->
                mkdirs(vfs, "/home/bandit0/inbox")

                val seg1 = password54.substring(0, 8)
                val seg2 = password54.substring(8, 16)
                val seg3 = password54.substring(16, 24)
                val seg4 = password54.substring(24, 32)

                val files = listOf(
                    Triple("msg_01.txt", "INVALID", "a1b2c3d4"),
                    Triple("msg_02.txt", "VALID", seg1),
                    Triple("msg_03.txt", "INVALID", "deadbeef"),
                    Triple("msg_04.txt", "VALID", seg2),
                    Triple("msg_05.txt", "VALID", seg3),
                    Triple("msg_06.txt", "INVALID", "f00dcafe"),
                    Triple("msg_07.txt", "VALID", seg4),
                    Triple("msg_08.txt", "INVALID", "12345678")
                )

                for ((name, status, code) in files) {
                    val content = buildString {
                        appendLine("Status: $status")
                        appendLine("Code: $code")
                        appendLine("Timestamp: 2024-01-15T10:00:00")
                    }
                    vfs.createFile("/home/bandit0/inbox/$name", content.toByteArray())
                }
            },
            validateCompletion = { _, output -> output.contains(password54) },
            hints = listOf(
                "Check each file's Status field — only VALID messages matter",
                "Extract the Code from VALID files: grep -l 'VALID' inbox/* | sort | xargs grep 'Code:'",
                "Combine the codes in order from the VALID files"
            ),
            teachingPoints = listOf(
                "Multi-step data processing",
                "Automating workflows",
                "Chaining grep, sort, xargs, cut"
            ),
            commandsIntroduced = listOf("grep -l", "xargs", "cut")
        ),

        // Level 55 — Final Challenge
        Level(
            id = 55,
            title = "Final Challenge",
            category = LevelCategory.MASTER,
            description = "The ultimate challenge: decode a multi-layer puzzle combining file navigation, text processing, permissions, and scripting.",
            briefing = "Congratulations on reaching the final challenge! This level combines everything you've learned. Multiple steps, multiple techniques — prove you've mastered the Linux command line.",
            password = password55,
            setupFileSystem = { vfs ->
                val firstHalf = password55.substring(0, 16)
                val secondHalf = password55.substring(16)

                // Stage 1: starting clue
                vfs.createFile(
                    "/home/bandit0/start.txt",
                    "The journey begins. Check /home/bandit0/stage1/ for the first clue.".toByteArray()
                )

                // Stage 2: hidden file with base64 hint
                mkdirs(vfs, "/home/bandit0/stage1")
                vfs.createFile(
                    "/home/bandit0/stage1/.clue",
                    "The next piece is encoded in base64 in /home/bandit0/stage2/encoded.txt".toByteArray()
                )

                // Stage 3: base64-encoded pointer to stage3
                mkdirs(vfs, "/home/bandit0/stage2")
                val stage2Message = "Look in /home/bandit0/stage3/data.log for the line with FINAL marker"
                val encoded = Base64.getEncoder().encodeToString(stage2Message.toByteArray())
                vfs.createFile("/home/bandit0/stage2/encoded.txt", encoded.toByteArray())

                // Stage 4: log file with first half and hint to stage4
                mkdirs(vfs, "/home/bandit0/stage3")
                val dataLog = buildString {
                    appendLine("2024-01-15 INFO System starting up")
                    appendLine("2024-01-15 DEBUG Loading configuration")
                    appendLine("2024-01-15 INFO Service initialized")
                    appendLine("2024-01-15 WARN Low disk space on /dev/sda1")
                    appendLine("2024-01-15 INFO Processing request from 192.168.1.1")
                    appendLine("2024-01-15 DEBUG Cache hit ratio: 0.85")
                    appendLine("2024-01-15 INFO User login: admin")
                    appendLine("2024-01-15 ERROR Connection timeout to db-server")
                    appendLine("2024-01-15 INFO Retrying connection...")
                    appendLine("2024-01-15 INFO Connection restored")
                    appendLine("2024-01-15 FINAL $firstHalf")
                    appendLine("2024-01-15 DEBUG Memory usage: 45%")
                    appendLine("2024-01-15 INFO Backup completed successfully")
                    appendLine("2024-01-15 WARN Certificate expires in 30 days")
                    appendLine("2024-01-15 INFO remaining piece in /home/bandit0/stage4/locked.txt")
                    appendLine("2024-01-15 DEBUG Garbage collection: 12ms")
                    appendLine("2024-01-15 INFO Health check passed")
                    appendLine("2024-01-15 INFO Cron job executed: cleanup.sh")
                    appendLine("2024-01-15 DEBUG Network latency: 2ms")
                    appendLine("2024-01-15 INFO System status: nominal")
                }
                vfs.createFile("/home/bandit0/stage3/data.log", dataLog.toByteArray())

                // Stage 5: locked file with second half
                mkdirs(vfs, "/home/bandit0/stage4")
                vfs.createFile(
                    "/home/bandit0/stage4/locked.txt",
                    secondHalf.toByteArray(),
                    Permissions.fromOctal(0)
                )
                vfs.chown("/home/bandit0/stage4/locked.txt", "bandit0", "bandit0")
            },
            validateCompletion = { _, output -> output.contains(password55) },
            hints = listOf(
                "Start with start.txt and follow the trail of clues",
                "You'll need: cat, ls -a, base64 -d, grep, chmod",
                "Stage 3 has half the password, stage 4 has the other half — combine them"
            ),
            teachingPoints = listOf(
                "Multi-step problem solving",
                "Combining all Linux skills",
                "Systematic investigation"
            ),
            commandsIntroduced = listOf("All previously learned commands")
        )
    )
}
