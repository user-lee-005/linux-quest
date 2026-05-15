package com.linuxquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linuxquest.data.AppDatabase
import com.linuxquest.data.entities.LevelProgress
import com.linuxquest.game.LevelManager
import com.linuxquest.game.XpSystem
import com.linuxquest.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LevelCompleteScreen(
    levelId: Int,
    password: String,
    onNextLevel: () -> Unit,
    onBackToLevels: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val levelManager = remember { LevelManager() }
    val level = remember { levelManager.getLevel(levelId) }

    var progress by remember { mutableStateOf<LevelProgress?>(null) }
    LaunchedEffect(levelId) {
        progress = db.progressDao().getProgress(levelId)
    }

    val isFinalLevel = levelId == 55

    // Typing animation for "LEVEL COMPLETE"
    val fullText = if (isFinalLevel) "🎉 CONGRATULATIONS!" else "LEVEL COMPLETE"
    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(fullText) {
        displayedText = ""
        for (i in fullText.indices) {
            displayedText = fullText.substring(0, i + 1)
            delay(50)
        }
    }

    // Blinking cursor after typing
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Title with typing animation
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = displayedText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TerminalGreen,
                textAlign = TextAlign.Center
            )
            Text(
                text = "█",
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                color = TerminalGreen,
                modifier = Modifier.alpha(cursorAlpha)
            )
        }

        if (isFinalLevel) {
            Text(
                text = "You are now a Linux Master!",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = TerminalPurple,
                textAlign = TextAlign.Center
            )
        }

        // Password box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, TerminalGreen)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PASSWORD",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextMuted,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = password,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TerminalGreen,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Stats panel
        progress?.let { prog ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "── STATS ──",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    StatRow("Commands used", "${prog.commandsUsed}")
                    StatRow("Hints used", "${prog.hintsUsed}/3")
                    StatRow("Time", formatTime(prog.timeTakenSeconds))

                    HorizontalDivider(color = SubtleBorder, thickness = 1.dp)

                    // XP earned
                    val xpEarned = prog.xpEarned
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "XP earned",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "+$xpEarned XP",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan
                        )
                    }

                    HorizontalDivider(color = SubtleBorder, thickness = 1.dp)

                    // Stars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stars earned",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "★".repeat(prog.stars) + "☆".repeat((3 - prog.stars).coerceAtLeast(0)),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            color = StarGold
                        )
                    }
                }
            }
        }

        // Teaching summary
        level?.let { lv ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "── YOU LEARNED ──",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    lv.teachingPoints.forEach { point ->
                        Text(
                            text = "• $point",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    if (lv.commandsIntroduced.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Commands: ${lv.commandsIntroduced.joinToString(", ")}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TerminalCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        if (!isFinalLevel) {
            Button(
                onClick = onNextLevel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalCyan,
                    contentColor = DeepNavy
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "NEXT LEVEL →",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        OutlinedButton(
            onClick = onBackToLevels,
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "BACK TO LEVELS",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s}s"
}
