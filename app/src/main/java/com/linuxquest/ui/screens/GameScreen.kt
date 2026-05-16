package com.linuxquest.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.linuxquest.data.AppDatabase
import com.linuxquest.data.SettingsDataStore
import com.linuxquest.data.entities.LevelProgress
import com.linuxquest.data.entities.UserProfile
import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.game.HintSystem
import com.linuxquest.game.Level
import com.linuxquest.game.LevelManager
import com.linuxquest.game.LevelValidator
import com.linuxquest.game.XpSystem
import com.linuxquest.shell.ShellInterpreter
import com.linuxquest.terminal.KeyboardHandler
import com.linuxquest.terminal.LineType
import com.linuxquest.terminal.TerminalState
import com.linuxquest.terminal.TerminalView
import com.linuxquest.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    levelId: Int,
    onLevelComplete: (password: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val settings = remember { SettingsDataStore(context) }
    val fontSize by settings.fontSize.collectAsState(initial = 14)

    val levelManager = remember { LevelManager() }
    val level = remember { levelManager.getLevel(levelId) }
    val hintSystem = remember { HintSystem() }
    val validator = remember { LevelValidator() }

    val vfs = remember { VirtualFileSystem() }
    val interpreter = remember { ShellInterpreter(vfs) }
    val terminalState = remember { TerminalState() }
    val keyboardHandler = remember { KeyboardHandler(interpreter.registry, vfs) }

    var commandsUsed by remember { mutableIntStateOf(0) }
    var hintsUsed by remember { mutableIntStateOf(0) }
    val startTime = remember { System.currentTimeMillis() }
    var currentPrompt by remember { mutableStateOf(interpreter.getPrompt()) }

    var objectiveExpanded by remember { mutableStateOf(true) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }
    var levelCompleted by remember { mutableStateOf(false) }

    // Save current level to DataStore
    LaunchedEffect(levelId) {
        settings.setCurrentLevel(levelId)
    }

    // Setup level on mount + restore session if available
    LaunchedEffect(levelId) {
        level?.let { lv ->
            levelManager.setupLevel(lv, vfs)

            // Check for saved session (incomplete levels only)
            val existing = db.progressDao().getProgress(levelId)
            val hasSession = existing != null && existing.attempted && !existing.completed
                    && existing.terminalHistory != null

            if (hasSession) {
                terminalState.restoreLines(existing!!.terminalHistory!!)
                existing.commandHistory?.let { terminalState.restoreCommandHistory(it) }
                hintsUsed = existing.hintsUsed
                commandsUsed = existing.commandsUsed
                terminalState.appendOutput("", LineType.OUTPUT)
                terminalState.appendOutput("── Session restored ──", LineType.SYSTEM)
                terminalState.appendOutput("", LineType.OUTPUT)
            } else {
                terminalState.appendOutput(lv.briefing, LineType.SYSTEM)
                terminalState.appendOutput("", LineType.OUTPUT)
                terminalState.appendOutput(
                    "Type 'help' for available commands. Use the \uD83D\uDCA1 Hint button if you get stuck.",
                    LineType.INFO
                )
                terminalState.appendOutput("", LineType.OUTPUT)
            }
        }
    }

    // Auto-save on app background (ON_STOP) — only for incomplete levels
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !levelCompleted) {
                scope.launch {
                    val existing = db.progressDao().getProgress(levelId)
                    if (existing == null || !existing.completed) {
                        db.progressDao().ensureLevelExists(levelId)
                        db.progressDao().attemptLevel(levelId, hintsUsed, commandsUsed)
                        db.progressDao().saveSessionState(
                            levelId,
                            terminalState.serializeLines(),
                            terminalState.serializeCommandHistory()
                        )
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (level == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavy),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Level $levelId not found",
                fontFamily = FontFamily.Monospace,
                color = TerminalRed,
                fontSize = 16.sp
            )
        }
        return
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Leave Level?",
                    fontFamily = FontFamily.Monospace,
                    color = TerminalYellow
                )
            },
            text = {
                Text(
                    text = "Your progress will be saved. You can resume later.",
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    scope.launch {
                        val existing = db.progressDao().getProgress(levelId)
                        if (existing == null || !existing.completed) {
                            // Save progress + terminal state for incomplete levels
                            db.progressDao().ensureLevelExists(levelId)
                            db.progressDao().attemptLevel(levelId, hintsUsed, commandsUsed)
                            db.progressDao().saveSessionState(
                                levelId,
                                terminalState.serializeLines(),
                                terminalState.serializeCommandHistory()
                            )
                        }
                        // Completed levels: skip save to protect highest score
                        onBack()
                    }
                }) {
                    Text("SAVE & LEAVE", fontFamily = FontFamily.Monospace, color = TerminalYellow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("STAY", fontFamily = FontFamily.Monospace, color = TerminalCyan)
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }

    // Hint dialog
    if (showHintDialog) {
        val revealedHints = remember { mutableStateListOf<String>() }
        LaunchedEffect(showHintDialog) {
            if (revealedHints.isEmpty()) {
                // Reveal hints up to current count
                repeat(hintsUsed) { i ->
                    if (i < level.hints.size) revealedHints.add(level.hints[i])
                }
            }
            // Reveal next hint
            val nextHint = hintSystem.getNextHint(level)
            if (nextHint != null && !revealedHints.contains(nextHint)) {
                revealedHints.add(nextHint)
                hintsUsed = hintSystem.getHintsUsed()
            }
        }
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            title = {
                Text(
                    text = "\uD83D\uDCA1 Hints (${hintsUsed}/${level.hints.size})",
                    fontFamily = FontFamily.Monospace,
                    color = TerminalYellow
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (revealedHints.isEmpty()) {
                        Text(
                            text = "No more hints available!",
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    } else {
                        revealedHints.forEachIndexed { idx, hint ->
                            Text(
                                text = "${idx + 1}. $hint",
                                fontFamily = FontFamily.Monospace,
                                color = if (idx == revealedHints.lastIndex) TerminalYellow else TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        if (hintSystem.hasMoreHints(level)) {
                            Text(
                                text = "Tap \uD83D\uDCA1 again for more hints",
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text("GOT IT", fontFamily = FontFamily.Monospace, color = TerminalCyan)
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
    ) {
        // Collapsible Objective Panel
        AnimatedVisibility(
            visible = objectiveExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TerminalCyan
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LEVEL ${level.id}: ${level.title}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TerminalCyan
                        )
                    }

                    TextButton(
                        onClick = { showHintDialog = true },
                        enabled = hintSystem.hasMoreHints(level)
                    ) {
                        Text(
                            text = "\uD83D\uDCA1 Hint",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (hintSystem.hasMoreHints(level))
                                TerminalYellow else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = level.description,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        // Toggle bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .clickable { objectiveExpanded = !objectiveExpanded }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (objectiveExpanded)
                    Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle objective",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (objectiveExpanded) "HIDE OBJECTIVE" else "SHOW OBJECTIVE",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        // Terminal
        TerminalView(
            terminalState = terminalState,
            prompt = currentPrompt,
            fontSize = fontSize,
            onSubmitCommand = { command ->
                if (command.isNotEmpty()) {
                    commandsUsed++

                    terminalState.appendOutput("$currentPrompt$command", LineType.PROMPT)

                    val result = interpreter.execute(command)

                    if (result.stdout.isNotEmpty()) {
                        terminalState.appendOutput(result.stdout.trimEnd('\n'), LineType.OUTPUT)
                    }
                    if (result.stderr.isNotEmpty()) {
                        terminalState.appendOutput(result.stderr.trimEnd('\n'), LineType.ERROR)
                    }

                    // Update prompt (may change after cd, su, etc.)
                    currentPrompt = interpreter.getPrompt()

                    // Check level completion (guard against double-trigger)
                    val output = result.stdout + result.stderr
                    if (!levelCompleted && validator.checkCompletion(level, vfs, output)) {
                        levelCompleted = true // set immediately to prevent re-entry
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000
                        val stars = calculateStars(hintsUsed, commandsUsed, elapsed)
                        val xp = XpSystem.calculateXp(stars, hintsUsed, elapsed)

                        scope.launch {
                            try {
                                val xpSystem = XpSystem(db.progressDao())
                                val existing = db.progressDao().getProgress(levelId)
                                val isNewCompletion = existing == null || !existing.completed
                                val isBetterScore = existing == null || stars > existing.stars

                                db.progressDao().ensureLevelExists(levelId)

                                if (isNewCompletion || isBetterScore) {
                                    db.progressDao().completeLevel(
                                        levelId = levelId,
                                        password = level.password,
                                        stars = stars,
                                        hints = hintsUsed,
                                        commands = commandsUsed,
                                        time = elapsed,
                                        completedAt = System.currentTimeMillis(),
                                        xp = xp
                                    )
                                }

                                if (isNewCompletion) {
                                    xpSystem.awardXp(xp)
                                } else if (isBetterScore) {
                                    val xpDiff = xp - (existing?.xpEarned ?: 0)
                                    if (xpDiff > 0) xpSystem.addXpOnly(xpDiff)
                                }

                                // Check achievements
                                val completedCount = db.progressDao().getCompletedCountSync()
                                val profile = xpSystem.getProfile()
                                val achievementMgr = com.linuxquest.game.AchievementManager(db.achievementDao(), db.progressDao())
                                achievementMgr.checkAndUnlock(
                                    levelId = levelId,
                                    hintsUsed = hintsUsed,
                                    timeSeconds = elapsed,
                                    totalCompleted = completedCount,
                                    totalXp = profile.totalXp
                                )

                                // Clear saved session state on completion
                                db.progressDao().clearSessionState(levelId)

                                onLevelComplete(level.password)
                            } catch (e: Exception) {
                                // Fallback: still navigate even if DB ops fail
                                onLevelComplete(level.password)
                            }
                        }
                    }
                }
            },
            onTabComplete = { input, cursor ->
                keyboardHandler.handleTabCompletion(input, cursor)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun calculateStars(hints: Int, commands: Int, timeSeconds: Long): Int {
    var stars = 3
    if (hints > 0) stars--
    if (hints > 1) stars--
    if (commands > 20) stars--
    if (timeSeconds > 300) stars--
    return stars.coerceIn(1, 3)
}
