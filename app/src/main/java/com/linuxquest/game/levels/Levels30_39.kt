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

fun createAdvancedDataLevels(): List<Level> {
    val ps = PasswordSystem()

    val password30 = ps.getPassword(30)
    val password31 = ps.getPassword(31)
    val password32 = ps.getPassword(32)
    val password33 = ps.getPassword(33)
    val password34 = ps.getPassword(34)
    val password35 = ps.getPassword(35)
    val password36 = ps.getPassword(36)
    val password37 = ps.getPassword(37)
    val password38 = ps.getPassword(38)
    val password39 = ps.getPassword(39)

    return listOf(
        // Level 30 — Base64 Basics
        Level(
            id = 30,
            title = "Base64 Basics",
            category = LevelCategory.ADVANCED_DATA,
            description = "Decode the base64-encoded password in the data file.",
            briefing = "Base64 encoding converts binary data to ASCII text. It's widely used in email, web, and configuration files. Decoding it is a common task.",
            hints = listOf(
                "The file contains base64-encoded text",
                "Use the 'base64' command to decode it",
                "Try: base64 -d encoded.txt"
            ),
            password = password30,
            setupFileSystem = { vfs ->
                val encoded = Base64.getEncoder().encodeToString(password30.toByteArray())
                vfs.createFile(
                    "/home/bandit0/encoded.txt",
                    encoded.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password30) },
            teachingPoints = listOf(
                "Understanding base64 encoding",
                "Decoding with base64 -d"
            ),
            commandsIntroduced = listOf("base64", "base64 -d")
        ),

        // Level 31 — Hex Dump
        Level(
            id = 31,
            title = "Hex Dump",
            category = LevelCategory.ADVANCED_DATA,
            description = "The password is hidden in a hex dump. Reverse the hex dump to get the original data.",
            briefing = "Hex dumps show binary data as hexadecimal values. The xxd tool creates and reverses hex dumps — essential for binary data analysis.",
            hints = listOf(
                "The file contains hexadecimal data",
                "Use 'xxd' to work with hex dumps",
                "Try: xxd -r -p hexdump.txt"
            ),
            password = password31,
            setupFileSystem = { vfs ->
                val hexContent = password31.toByteArray().joinToString(" ") { "%02x".format(it) }
                vfs.createFile(
                    "/home/bandit0/hexdump.txt",
                    hexContent.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password31) },
            teachingPoints = listOf(
                "Understanding hex dumps",
                "Using xxd for hex conversion",
                "Reversing hex dumps with xxd -r"
            ),
            commandsIntroduced = listOf("xxd", "xxd -r")
        ),

        // Level 32 — String Theory
        Level(
            id = 32,
            title = "String Theory",
            category = LevelCategory.ADVANCED_DATA,
            description = "The password is a readable string hidden in a binary file.",
            briefing = "Binary files often contain readable strings — configuration values, error messages, or hidden data. The 'strings' command extracts them.",
            hints = listOf(
                "The file is mostly binary data, but contains readable text",
                "Use the 'strings' command to extract readable strings",
                "Try: strings binary.dat"
            ),
            password = password32,
            setupFileSystem = { vfs ->
                val prefix = ByteArray(50) { ((it * 7 + 13) % 256).toByte() }
                val suffix = ByteArray(50) { ((it * 11 + 37) % 256).toByte() }
                val passwordBytes = password32.toByteArray()
                val content = prefix + passwordBytes + suffix
                vfs.createFile(
                    "/home/bandit0/binary.dat",
                    content,
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password32) },
            teachingPoints = listOf(
                "Extracting strings from binary files",
                "Using strings command"
            ),
            commandsIntroduced = listOf("strings")
        ),

        // Level 33 — Regex Warrior
        Level(
            id = 33,
            title = "Regex Warrior",
            category = LevelCategory.ADVANCED_DATA,
            description = "Use an extended regex pattern to find the password line in the data file.",
            briefing = "Regular expressions are patterns that describe text. Extended regex (-E flag) adds powerful features like alternation, quantifiers, and grouping.",
            hints = listOf(
                "Look for a specific pattern in the data file",
                "Use grep with extended regex: grep -E 'pattern'",
                "Try: grep -E 'KEY-[a-f0-9]{32}' data.txt"
            ),
            password = password33,
            setupFileSystem = { vfs ->
                val lines = mutableListOf<String>()
                lines.add("item-abc123")
                lines.add("KEY-invalid123")
                lines.add("key-wrongformat")
                lines.add("KEY_notthis")
                lines.add("item-def456")
                lines.add("KEY-zzzz1111gggg2222")
                lines.add("data-record-001")
                lines.add("KEY-short")
                lines.add("item-ghi789")
                lines.add("KEY-UPPERCASE12345678901234567890")
                lines.add("entry-alpha-01")
                lines.add("KEY-${password33}")
                lines.add("KEY-xyz12345")
                lines.add("key-lowercase0000000000000000000")
                lines.add("item-jkl012")
                lines.add("KEY_underscore_separator_data123")
                lines.add("data-record-002")
                lines.add("KEY-toolong1234567890abcdef12345678XX")
                lines.add("entry-beta-02")
                lines.add("item-mno345")
                lines.add("KEY-g1h2i3j4k5")
                lines.add("data-record-003")
                lines.add("KEY-onlyfourteen1")
                lines.add("entry-gamma-03")
                lines.add("item-pqr678")
                lines.add("KEY-ABCDEF01234567890123456789abcdef")
                lines.add("data-record-004")
                lines.add("entry-delta-04")
                lines.add("item-stu901")
                lines.add("KEY-nothex_gghhiijj00112233445566")
                val content = lines.joinToString("\n") + "\n"
                vfs.createFile(
                    "/home/bandit0/data.txt",
                    content.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password33) },
            teachingPoints = listOf(
                "Extended regular expressions with grep -E",
                "Character classes and quantifiers",
                "Pattern matching"
            ),
            commandsIntroduced = listOf("grep -E")
        ),

        // Level 34 — Sed Master
        Level(
            id = 34,
            title = "Sed Master",
            category = LevelCategory.ADVANCED_DATA,
            description = "Apply multiple sed transformations to decode the scrambled password.",
            briefing = "Sed can chain multiple transformations. By combining substitutions, you can decode text that's been through multiple encoding steps.",
            hints = listOf(
                "The text has been scrambled with character replacements",
                "You need to reverse the transformations with sed",
                "Try: sed 's/@/a/g; s/3/e/g; s/BEGIN_//; s/_END//' scrambled.txt"
            ),
            password = password34,
            setupFileSystem = { vfs ->
                val scrambled = "BEGIN_" +
                    password34.replace('a', '@').replace('e', '3') +
                    "_END"
                vfs.createFile(
                    "/home/bandit0/scrambled.txt",
                    scrambled.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password34) },
            teachingPoints = listOf(
                "Chaining sed commands",
                "Global substitution with g flag",
                "Multi-step text transformation"
            ),
            commandsIntroduced = listOf("sed")
        ),

        // Level 35 — Awk Wizard
        Level(
            id = 35,
            title = "Awk Wizard",
            category = LevelCategory.ADVANCED_DATA,
            description = "Analyze the log file to find the user with the most failed logins. Their password field contains the answer.",
            briefing = "AWK is a full programming language for text processing. It can aggregate data, perform calculations, and generate reports from structured text.",
            hints = listOf(
                "Analyze auth.log to find who has the most failed logins",
                "Use awk to count FAILED entries per user",
                "The user with most failures is 'admin' — look up their password in passwords.txt"
            ),
            password = password35,
            setupFileSystem = { vfs ->
                val logLines = mutableListOf<String>()
                logLines.add("2024-01-15 08:01:00 FAILED user=alice")
                logLines.add("2024-01-15 08:02:00 SUCCESS user=bob")
                logLines.add("2024-01-15 08:03:00 FAILED user=charlie")
                logLines.add("2024-01-15 08:04:00 FAILED user=admin")
                logLines.add("2024-01-15 08:05:00 SUCCESS user=alice")
                logLines.add("2024-01-15 08:06:00 FAILED user=admin")
                logLines.add("2024-01-15 08:07:00 FAILED user=bob")
                logLines.add("2024-01-15 08:08:00 FAILED user=admin")
                logLines.add("2024-01-15 08:09:00 SUCCESS user=charlie")
                logLines.add("2024-01-15 08:10:00 FAILED user=admin")
                logLines.add("2024-01-15 08:11:00 FAILED user=alice")
                logLines.add("2024-01-15 08:12:00 FAILED user=admin")
                logLines.add("2024-01-15 08:13:00 SUCCESS user=admin")
                logLines.add("2024-01-15 08:14:00 FAILED user=charlie")
                logLines.add("2024-01-15 08:15:00 FAILED user=admin")
                logLines.add("2024-01-15 08:16:00 FAILED user=bob")
                logLines.add("2024-01-15 08:17:00 FAILED user=admin")
                logLines.add("2024-01-15 08:18:00 SUCCESS user=alice")
                logLines.add("2024-01-15 08:19:00 FAILED user=admin")
                logLines.add("2024-01-15 08:20:00 FAILED user=charlie")
                val authLog = logLines.joinToString("\n") + "\n"
                vfs.createFile(
                    "/home/bandit0/auth.log",
                    authLog.toByteArray(),
                    Permissions.fromOctal(644)
                )

                val passwordLines = listOf(
                    "alice:f4e8a1b2c3d4e5f6a7b8c9d0e1f2a3b4",
                    "bob:1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d",
                    "admin:${password35}",
                    "charlie:9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c"
                )
                val passwordsContent = passwordLines.joinToString("\n") + "\n"
                vfs.createFile(
                    "/home/bandit0/passwords.txt",
                    passwordsContent.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password35) },
            teachingPoints = listOf(
                "AWK for data aggregation",
                "Counting patterns",
                "Correlating data across files"
            ),
            commandsIntroduced = listOf("awk")
        ),

        // Level 36 — Archive Arts
        Level(
            id = 36,
            title = "Archive Arts",
            category = LevelCategory.ADVANCED_DATA,
            description = "The password is in a file that has been archived multiple times. Unwrap the layers.",
            briefing = "Files can be compressed and archived in layers. You need to identify each layer and apply the right decompression tool.",
            hints = listOf(
                "The password is buried in nested directories simulating archive layers",
                "Navigate through the archive directory structure",
                "Check archive/layer1/layer2/password.txt"
            ),
            password = password36,
            setupFileSystem = { vfs ->
                mkdirs(vfs, "/home/bandit0/archive/layer1/layer2")
                vfs.createFile(
                    "/home/bandit0/archive/readme.txt",
                    ("This simulates a nested archive.\n" +
                        "The actual password is in the innermost file.\n").toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/archive/layer1/layer2/password.txt",
                    (password36 + "\n").toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password36) },
            teachingPoints = listOf(
                "Understanding archive formats (tar, gzip)",
                "Extracting nested archives",
                "Identifying file compression types"
            ),
            commandsIntroduced = listOf("tar", "gzip", "gunzip")
        ),

        // Level 37 — Crypto Intro
        Level(
            id = 37,
            title = "Crypto Intro",
            category = LevelCategory.ADVANCED_DATA,
            description = "Verify the file's integrity using its checksum to confirm which file has the password.",
            briefing = "Checksums verify data integrity. If a file's checksum matches the expected value, you know it hasn't been tampered with.",
            hints = listOf(
                "You need to verify which file is authentic using checksums",
                "Use 'sha256sum' or 'md5sum' to compute checksums",
                "Check each file's checksum against the reference in checksum.txt"
            ),
            password = password37,
            setupFileSystem = { vfs ->
                vfs.createFile(
                    "/home/bandit0/file_a.txt",
                    "This is a decoy file with no useful data.\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/file_b.txt",
                    (password37 + "\n").toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/file_c.txt",
                    "Another decoy. Keep looking.\n".toByteArray(),
                    Permissions.fromOctal(644)
                )
                val hint = "The verified file has sha256 starting with the first 8 chars of the password.\n" +
                    "Check file_a.txt, file_b.txt, and file_c.txt\n"
                vfs.createFile(
                    "/home/bandit0/checksum.txt",
                    hint.toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password37) },
            teachingPoints = listOf(
                "File integrity verification",
                "Using checksum tools (md5sum, sha256sum)"
            ),
            commandsIntroduced = listOf("md5sum", "sha256sum")
        ),

        // Level 38 — Key Exchange
        Level(
            id = 38,
            title = "Key Exchange",
            category = LevelCategory.ADVANCED_DATA,
            description = "An SSH key file has the password embedded in its comment field.",
            briefing = "SSH keys are used for secure authentication. They often have metadata like comments that can contain useful information.",
            hints = listOf(
                "Look for SSH key files in the .ssh directory",
                "SSH public keys have a comment at the end of the line",
                "The comment field contains the password: cat .ssh/id_rsa.pub"
            ),
            password = password38,
            setupFileSystem = { vfs ->
                mkdirs(vfs, "/home/bandit0/.ssh")
                val pubKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC7z9X2k3f8qR5mN1vYpLbT" +
                    "cWdHs6jK0xAe4uGhRnMcVfNpOiS3tB7aRkLdYm8fWqZv0hXjK9pA2bC4nM6" +
                    "dFgH8iJkL1oP3qR5sT7uV9wX0yZ2aB4cD6eF8gH0iJ ${password38}@linuxquest"
                vfs.createFile(
                    "/home/bandit0/.ssh/id_rsa.pub",
                    (pubKey + "\n").toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password38) },
            teachingPoints = listOf(
                "SSH key file format",
                "Understanding key comments",
                "Hidden data in metadata"
            ),
            commandsIntroduced = listOf("cat", "ssh-keygen")
        ),

        // Level 39 — Net Basics
        Level(
            id = 39,
            title = "Net Basics",
            category = LevelCategory.ADVANCED_DATA,
            description = "Fetch the password from a simulated web server.",
            briefing = "The 'curl' command transfers data from URLs. It's essential for interacting with web services and APIs from the command line.",
            hints = listOf(
                "There's a simulated web server with the password",
                "Use 'curl' to fetch data from a URL",
                "Try: curl http://localhost:8080/secret — or read server_response.txt"
            ),
            password = password39,
            setupFileSystem = { vfs ->
                vfs.createFile(
                    "/home/bandit0/server_response.txt",
                    (password39 + "\n").toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.createFile(
                    "/home/bandit0/readme.txt",
                    ("A web server is running locally.\n" +
                        "Use 'curl http://localhost:8080/secret' to fetch the password.\n" +
                        "Alternatively, the cached response is in server_response.txt\n").toByteArray(),
                    Permissions.fromOctal(644)
                )
            },
            validateCompletion = { _, output -> output.contains(password39) },
            teachingPoints = listOf(
                "Fetching data with curl",
                "HTTP basics",
                "Command-line web requests"
            ),
            commandsIntroduced = listOf("curl")
        )
    )
}
