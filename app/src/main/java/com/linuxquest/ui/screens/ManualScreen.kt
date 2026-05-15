package com.linuxquest.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linuxquest.ui.theme.*

private data class ManualEntry(
    val command: String,
    val brief: String,
    val usage: String,
    val description: String,
    val examples: List<String>,
    val category: String
)

private val manualEntries = listOf(
    // Navigation
    ManualEntry("cd", "Change directory", "cd [dir]",
        "Change the current working directory. Without arguments, changes to HOME. 'cd -' returns to the previous directory. Supports ~ for home directory.",
        listOf("cd /home/user", "cd ..", "cd ~", "cd -"), "Navigation"),
    ManualEntry("pwd", "Print working directory", "pwd",
        "Print the full pathname of the current working directory.",
        listOf("pwd"), "Navigation"),
    ManualEntry("ls", "List directory contents", "ls [-la] [path...]",
        "List files and directories. Use -a to show hidden files (starting with .), -l for long format showing permissions, owner, size.",
        listOf("ls", "ls -la", "ls -l /etc", "ls -a /home"), "Navigation"),

    // File Operations
    ManualEntry("cat", "Concatenate and print files", "cat [file...]",
        "Read files sequentially and write their contents to standard output.",
        listOf("cat readme.txt", "cat file1 file2", "cat -n file.txt"), "File Operations"),
    ManualEntry("cp", "Copy files", "cp source dest",
        "Copy SOURCE to DEST. If DEST is a directory, copies into that directory.",
        listOf("cp file.txt backup.txt", "cp report.txt /tmp/"), "File Operations"),
    ManualEntry("mv", "Move/rename files", "mv source dest",
        "Move or rename SOURCE to DEST.",
        listOf("mv old.txt new.txt", "mv file.txt /archive/"), "File Operations"),
    ManualEntry("rm", "Remove files", "rm [-r] file...",
        "Remove files or directories. Use -r for recursive removal of directories.",
        listOf("rm temp.txt", "rm -r old_dir/"), "File Operations"),
    ManualEntry("mkdir", "Make directories", "mkdir dir...",
        "Create directories if they do not already exist.",
        listOf("mkdir newdir", "mkdir -p path/to/dir"), "File Operations"),
    ManualEntry("touch", "Create empty file", "touch file...",
        "Create empty files or update timestamps of existing files.",
        listOf("touch newfile.txt"), "File Operations"),
    ManualEntry("ln", "Create links", "ln -s target link",
        "Create symbolic links between files. Use -s for symbolic (soft) links.",
        listOf("ln -s /etc/passwd link_to_passwd"), "File Operations"),
    ManualEntry("readlink", "Resolve symbolic link", "readlink link",
        "Print the target of a symbolic link.",
        listOf("readlink mylink"), "File Operations"),
    ManualEntry("file", "Determine file type", "file file...",
        "Determine the type of a file by examining its content (magic bytes, not extension).",
        listOf("file mystery.dat", "file /bin/ls", "file *.txt"), "File Operations"),

    // Search
    ManualEntry("find", "Search for files", "find [path] [-name pattern] [-type f|d]",
        "Search for files in a directory hierarchy. -name filters by filename pattern (supports * and ? globs). -type f finds files, -type d finds directories.",
        listOf("find . -name '*.txt'", "find /home -type f", "find . -name 'readme*'"), "Search"),
    ManualEntry("grep", "Search text patterns", "grep [-inv] pattern [file...]",
        "Search for lines matching a pattern. -i ignores case, -n shows line numbers, -v inverts the match (show non-matching lines). Supports basic regex.",
        listOf("grep 'password' file.txt", "grep -i 'error' log.txt", "grep -rn 'TODO' src/"), "Search"),

    // Text Processing
    ManualEntry("head", "Output first lines", "head [-n N] [file]",
        "Output the first N lines of a file (default 10). Can read from stdin via pipe.",
        listOf("head -n 5 file.txt", "cat log.txt | head -20"), "Text Processing"),
    ManualEntry("tail", "Output last lines", "tail [-n N] [file]",
        "Output the last N lines of a file (default 10). Can read from stdin via pipe.",
        listOf("tail -n 5 file.txt", "cat log.txt | tail -3"), "Text Processing"),
    ManualEntry("sort", "Sort lines", "sort [-rnu] [file]",
        "Sort lines of text. -r reverses order, -n sorts numerically, -u removes duplicates.",
        listOf("sort names.txt", "sort -n numbers.txt", "sort -rn scores.txt"), "Text Processing"),
    ManualEntry("uniq", "Remove duplicate lines", "uniq [-c] [file]",
        "Filter out adjacent duplicate lines. -c prefixes each line with occurrence count. Often used after sort.",
        listOf("sort data.txt | uniq", "sort data.txt | uniq -c"), "Text Processing"),
    ManualEntry("wc", "Word/line/byte count", "wc [file]",
        "Print line, word, and byte counts for each file.",
        listOf("wc file.txt", "cat data.txt | wc -l"), "Text Processing"),
    ManualEntry("strings", "Extract printable strings", "strings [file]",
        "Extract sequences of printable characters (4+ chars) from binary files.",
        listOf("strings binary.dat", "strings /bin/ls | grep version"), "Text Processing"),

    // Data Encoding
    ManualEntry("base64", "Base64 encode/decode", "base64 [-d] [file]",
        "Encode or decode data in Base64 format. -d decodes input.",
        listOf("base64 secret.txt", "base64 -d encoded.txt", "echo 'hello' | base64"), "Data Encoding"),
    ManualEntry("xxd", "Hex dump", "xxd [file]",
        "Create a hexadecimal dump of a file. Shows offset, hex values, and ASCII representation.",
        listOf("xxd data.bin", "xxd -l 64 file.dat"), "Data Encoding"),

    // Permissions
    ManualEntry("chmod", "Change permissions", "chmod mode file",
        "Change file mode (permissions) using octal notation. r=4, w=2, x=1. E.g., 755 = rwxr-xr-x.",
        listOf("chmod 755 script.sh", "chmod 644 readme.txt", "chmod 600 secret.key"), "Permissions"),
    ManualEntry("whoami", "Print current user", "whoami",
        "Print the user name associated with the current effective user ID.",
        listOf("whoami"), "Permissions"),
    ManualEntry("id", "Print user identity", "id",
        "Print user and group IDs and names.",
        listOf("id"), "Permissions"),
    ManualEntry("su", "Switch user", "su [user]",
        "Switch to another user account.",
        listOf("su bandit1", "su root"), "Permissions"),

    // System
    ManualEntry("echo", "Display text", "echo [-neE] [text...]",
        "Display a line of text. -n omits trailing newline, -e enables escape sequences.",
        listOf("echo 'Hello World'", "echo -n 'no newline'", "echo -e 'line1\\nline2'"), "System"),
    ManualEntry("history", "Command history", "history",
        "Display the list of previously executed commands.",
        listOf("history"), "System"),
    ManualEntry("clear", "Clear terminal", "clear",
        "Clear the terminal screen.",
        listOf("clear"), "System"),
    ManualEntry("help", "Show help", "help",
        "Display a list of available commands and their brief descriptions.",
        listOf("help"), "System")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = remember { manualEntries.map { it.category }.distinct() }

    val filteredEntries = remember(searchQuery) {
        if (searchQuery.isBlank()) manualEntries
        else manualEntries.filter { entry ->
            entry.command.contains(searchQuery, ignoreCase = true) ||
                entry.brief.contains(searchQuery, ignoreCase = true) ||
                entry.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedEntries = remember(filteredEntries) {
        filteredEntries.groupBy { it.category }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "\uD83D\uDCD6 LINUX MANUAL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TerminalCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(CardSurface, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = "$ man <command>...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = TextMuted
                )
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                singleLine = true,
                cursorBrush = SolidColor(TerminalCyan),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Entries
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            groupedEntries.forEach { (category, entries) ->
                item(key = "header_$category") {
                    Text(
                        text = "── ${category.uppercase()} ──",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalPurple,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }

                items(entries, key = { it.command }) { entry ->
                    ManualEntryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun ManualEntryCard(entry: ManualEntry) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.command,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TerminalCyan
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = entry.brief,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = if (expanded)
                        Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = "Expand",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Usage
                    Text(
                        text = "USAGE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "  ${entry.usage}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TerminalGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Description
                    Text(
                        text = "DESCRIPTION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = entry.description,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Examples
                    if (entry.examples.isNotEmpty()) {
                        Text(
                            text = "EXAMPLES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        entry.examples.forEach { ex ->
                            Text(
                                text = "  $ $ex",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TerminalYellow,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
