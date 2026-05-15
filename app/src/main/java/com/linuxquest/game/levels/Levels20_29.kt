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

fun createPermissionsLevels(): List<Level> {
    val ps = PasswordSystem()

    return listOf(
        // Level 20 — Who Am I
        Level(
            id = 20,
            title = "Who Am I",
            category = LevelCategory.PERMISSIONS,
            description = "Find out your username and group memberships to unlock the password.",
            briefing = "In Linux, every process runs as a user. Understanding your identity — who you are, what groups you belong to — is fundamental to understanding what you can access.",
            hints = listOf(
                "Try 'whoami' to see your username",
                "Use 'id' to see your user ID and group memberships",
                "The password is in secret.txt — read it after exploring your identity"
            ),
            password = ps.getPassword(20),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(20)
                vfs.createFile(
                    "/home/bandit0/identity.txt",
                    "Run 'whoami' and 'id' to discover your identity.\nYour password is waiting in /home/bandit0/secret.txt".toByteArray()
                )
                vfs.createFile(
                    "/home/bandit0/secret.txt",
                    password.toByteArray()
                )
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(20))
            },
            teachingPoints = listOf(
                "Understanding user identity with whoami",
                "Viewing user/group info with id"
            ),
            commandsIntroduced = listOf("whoami", "id", "groups")
        ),

        // Level 21 — Read Write Execute
        Level(
            id = 21,
            title = "Read Write Execute",
            category = LevelCategory.PERMISSIONS,
            description = "Several files have different permissions. Find the one that bandit0 can read.",
            briefing = "Linux permissions control who can read, write, and execute files. Each file has permissions for the owner, group, and others.",
            hints = listOf(
                "Check file permissions with 'ls -l data/'",
                "Look for files where 'other' has read permission",
                "file_c is readable by others — try 'cat data/file_c'"
            ),
            password = ps.getPassword(21),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(21)
                mkdirs(vfs, "/home/bandit0/data")
                vfs.createFile("/home/bandit0/data/file_a", "decoy".toByteArray(), Permissions.fromOctal(0))
                vfs.chown("/home/bandit0/data/file_a", "root", "root")
                vfs.createFile("/home/bandit0/data/file_b", "decoy".toByteArray(), Permissions.fromOctal(200))
                vfs.chown("/home/bandit0/data/file_b", "root", "root")
                vfs.createFile("/home/bandit0/data/file_c", password.toByteArray(), Permissions.fromOctal(44))
                vfs.chown("/home/bandit0/data/file_c", "root", "root")
                vfs.createFile("/home/bandit0/data/file_d", "decoy".toByteArray(), Permissions.fromOctal(100))
                vfs.chown("/home/bandit0/data/file_d", "root", "root")
                vfs.createFile("/home/bandit0/data/file_e", "decoy".toByteArray(), Permissions.fromOctal(300))
                vfs.chown("/home/bandit0/data/file_e", "root", "root")
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(21))
            },
            teachingPoints = listOf(
                "Reading permission strings (rwx)",
                "Understanding owner/group/other permissions"
            ),
            commandsIntroduced = listOf("ls -l")
        ),

        // Level 22 — Permission Granted
        Level(
            id = 22,
            title = "Permission Granted",
            category = LevelCategory.PERMISSIONS,
            description = "A file contains the password but you can't read it yet. Change its permissions.",
            briefing = "The 'chmod' command changes file permissions. You need to be the file's owner (or root) to change its permissions.",
            hints = listOf(
                "The file exists but has no permissions set",
                "Since you own it, you can change permissions with 'chmod'",
                "Try: chmod 644 locked.txt && cat locked.txt"
            ),
            password = ps.getPassword(22),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(22)
                vfs.createFile("/home/bandit0/locked.txt", password.toByteArray(), Permissions.fromOctal(0))
                vfs.chown("/home/bandit0/locked.txt", "bandit0", "bandit0")
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(22))
            },
            teachingPoints = listOf(
                "Changing permissions with chmod",
                "Octal permission notation (644, 755, etc.)"
            ),
            commandsIntroduced = listOf("chmod")
        ),

        // Level 23 — Symbolic Modes
        Level(
            id = 23,
            title = "Symbolic Modes",
            category = LevelCategory.PERMISSIONS,
            description = "Use symbolic mode to add read permission and reveal the password.",
            briefing = "Besides octal notation (like 755), chmod also supports symbolic notation: u/g/o for user/group/other, and +/- for add/remove permissions.",
            hints = listOf(
                "Symbolic mode uses letters: u=user, g=group, o=other, a=all",
                "Use + to add permissions: 'chmod u+r file'",
                "Try: chmod u+r message.txt && cat message.txt"
            ),
            password = ps.getPassword(23),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(23)
                vfs.createFile("/home/bandit0/message.txt", password.toByteArray(), Permissions.fromOctal(0))
                vfs.chown("/home/bandit0/message.txt", "bandit0", "bandit0")
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(23))
            },
            teachingPoints = listOf(
                "Symbolic permission notation (u+r, g+w, o+x)",
                "Difference between octal and symbolic modes"
            ),
            commandsIntroduced = listOf("chmod u+r")
        ),

        // Level 24 — The SUID Bit
        Level(
            id = 24,
            title = "The SUID Bit",
            category = LevelCategory.PERMISSIONS,
            description = "A SUID program can read files you normally can't. Use it to get the password.",
            briefing = "The SUID (Set User ID) bit is a special permission. When set on an executable, it runs with the file owner's permissions, not the user's. This is how commands like 'passwd' can modify /etc/shadow.",
            hints = listOf(
                "Look for files with special permissions using 'ls -l'",
                "The 's' in permission string indicates SUID — the program runs as its owner",
                "Execute ./reader to run it with bandit1's permissions and read the secret"
            ),
            password = ps.getPassword(24),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(24)
                vfs.createFile(
                    "/home/bandit0/reader",
                    "SUID-BINARY:cat /home/bandit0/secret.txt".toByteArray(),
                    Permissions.fromOctal(4755)
                )
                vfs.chown("/home/bandit0/reader", "bandit1", "bandit1")
                vfs.createFile(
                    "/home/bandit0/secret.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(600)
                )
                vfs.chown("/home/bandit0/secret.txt", "bandit1", "bandit1")
                vfs.createFile(
                    "/home/bandit0/hint.txt",
                    ("The reader binary has SUID bit set and is owned by bandit1.\n" +
                        "It will display the contents of secret.txt when executed.").toByteArray()
                )
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(24))
            },
            teachingPoints = listOf(
                "Understanding SUID bit",
                "How SUID programs work",
                "Security implications of SUID"
            ),
            commandsIntroduced = listOf("ls -l", "./suid_binary")
        ),

        // Level 25 — Group Dynamics
        Level(
            id = 25,
            title = "Group Dynamics",
            category = LevelCategory.PERMISSIONS,
            description = "A file is readable only by the 'users' group. You're a member — read it.",
            briefing = "Group permissions allow shared access among team members. If you're in the right group, you can access group-readable files even if you're not the owner.",
            hints = listOf(
                "Check your groups with 'groups' or 'id'",
                "The file is owned by root:users with group read permission",
                "You're in the 'users' group — 'cat shared.txt' should work"
            ),
            password = ps.getPassword(25),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(25)
                vfs.createFile(
                    "/home/bandit0/shared.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(40)
                )
                vfs.chown("/home/bandit0/shared.txt", "root", "users")
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(25))
            },
            teachingPoints = listOf(
                "Group-based file access",
                "Understanding group ownership"
            ),
            commandsIntroduced = listOf("groups", "id")
        ),

        // Level 26 — The Sticky Bit
        Level(
            id = 26,
            title = "The Sticky Bit",
            category = LevelCategory.PERMISSIONS,
            description = "Understand the sticky bit on /tmp to find the password left by another user.",
            briefing = "The sticky bit is a special permission on directories. When set, only the file's owner can delete or rename files within it — even though others can write to the directory. /tmp commonly uses this.",
            hints = listOf(
                "Look at the permissions on /tmp — notice the 't' at the end",
                "List all files in /tmp to find interesting ones",
                "Read /tmp/secret_note.txt for the password"
            ),
            password = ps.getPassword(26),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(26)
                vfs.createFile(
                    "/tmp/bandit0_message.txt",
                    "Check the other files in /tmp for the password.".toByteArray()
                )
                vfs.chown("/tmp/bandit0_message.txt", "bandit0", "bandit0")
                vfs.createFile(
                    "/tmp/secret_note.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.chown("/tmp/secret_note.txt", "root", "root")
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(26))
            },
            teachingPoints = listOf(
                "Understanding the sticky bit",
                "Why /tmp uses sticky bit",
                "File deletion restrictions in sticky directories"
            ),
            commandsIntroduced = listOf("ls -ld /tmp")
        ),

        // Level 27 — Mask It
        Level(
            id = 27,
            title = "Mask It",
            category = LevelCategory.PERMISSIONS,
            description = "Your umask is set wrong. Understand umask to find the password.",
            briefing = "The umask determines the default permissions for newly created files. Understanding umask helps you predict and control file security.",
            hints = listOf(
                "Read the readme.txt first to understand umask",
                "The password file is in the secret directory",
                "You own the directory and file, so you can access them: cat secret/password.txt"
            ),
            password = ps.getPassword(27),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(27)
                vfs.createFile(
                    "/home/bandit0/readme.txt",
                    ("The umask 0022 means new files get 644 (666-022) and dirs get 755 (777-022).\n" +
                        "The password file was created with umask 0077.\n" +
                        "Check /home/bandit0/secret/ for a file with restricted permissions.").toByteArray()
                )
                mkdirs(vfs, "/home/bandit0/secret")
                vfs.chmod("/home/bandit0/secret", Permissions.fromOctal(700))
                vfs.chown("/home/bandit0/secret", "bandit0", "bandit0")
                vfs.createFile(
                    "/home/bandit0/secret/password.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(600)
                )
                vfs.chown("/home/bandit0/secret/password.txt", "bandit0", "bandit0")
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(27))
            },
            teachingPoints = listOf(
                "Understanding umask",
                "How umask affects file creation permissions",
                "Default permission calculation"
            ),
            commandsIntroduced = listOf("umask")
        ),

        // Level 28 — Sudo Power
        Level(
            id = 28,
            title = "Sudo Power",
            category = LevelCategory.PERMISSIONS,
            description = "The password is in a root-only file. Use sudo to read it.",
            briefing = "The 'sudo' command lets authorized users execute commands as root. It's the gateway to administrative privileges on Linux systems.",
            hints = listOf(
                "The file is owned by root with permissions 600",
                "You need elevated privileges to read it",
                "Use 'sudo cat restricted.txt' to run cat as root"
            ),
            password = ps.getPassword(28),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(28)
                vfs.createFile(
                    "/home/bandit0/restricted.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(600)
                )
                vfs.chown("/home/bandit0/restricted.txt", "root", "root")
                vfs.createFile(
                    "/home/bandit0/readme.txt",
                    ("The file restricted.txt is owned by root.\n" +
                        "Use 'sudo cat restricted.txt' to read it with elevated privileges.").toByteArray()
                )
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(28))
            },
            teachingPoints = listOf(
                "Using sudo for elevated privileges",
                "When and why to use sudo",
                "Root access and security"
            ),
            commandsIntroduced = listOf("sudo")
        ),

        // Level 29 — Shadow Secrets
        Level(
            id = 29,
            title = "Shadow Secrets",
            category = LevelCategory.PERMISSIONS,
            description = "Parse /etc/passwd to find the home directory of the bandit29 user. The password is there.",
            briefing = "The /etc/passwd file contains user account information. Each line has fields separated by colons: username:password:uid:gid:info:home:shell. It's world-readable and a goldmine of information.",
            hints = listOf(
                "Look at /etc/passwd to find user information",
                "Each line is colon-separated: username:x:uid:gid:info:home:shell",
                "Find bandit29's home directory, then read the password file there"
            ),
            password = ps.getPassword(29),
            setupFileSystem = { vfs ->
                val password = ps.getPassword(29)
                mkdirs(vfs, "/home/bandit29")
                vfs.createFile(
                    "/home/bandit29/password.txt",
                    password.toByteArray(),
                    Permissions.fromOctal(644)
                )
                vfs.chown("/home/bandit29/password.txt", "root", "root")
            },
            validateCompletion = { _, lastOutput ->
                lastOutput.contains(ps.getPassword(29))
            },
            teachingPoints = listOf(
                "Understanding /etc/passwd format",
                "Parsing colon-separated data",
                "User account information"
            ),
            commandsIntroduced = listOf("cat /etc/passwd", "grep")
        )
    )
}
