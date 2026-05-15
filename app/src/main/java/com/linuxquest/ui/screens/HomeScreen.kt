package com.linuxquest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linuxquest.data.AppDatabase
import com.linuxquest.data.entities.UserProfile
import com.linuxquest.game.XpSystem
import com.linuxquest.ui.theme.*
import kotlinx.coroutines.launch

private val asciiTitle = """
╦  ╦╔╗╔╦ ╦═╗ ╔═╗╦ ╦╔═╗╔═╗╔╦╗
║  ║║║║║ ║╔╝ ║ ║║ ║║╣ ╚═╗ ║ 
╩═╝╩╝╚╝╚═╝╩  ╚═╝╚═╝╚═╝╚═╝ ╩
""".trimIndent()

@Composable
fun HomeScreen(
    onStartGame: (Int) -> Unit,
    onSelectLevel: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val completedCount by db.progressDao().getCompletedCount()
        .collectAsState(initial = 0)
    val achievements by db.achievementDao().getAllAchievements()
        .collectAsState(initial = emptyList())
    val unlockedCount = remember(achievements) { achievements.count { it.unlocked } }
    val profile by db.progressDao().getProfileFlow()
        .collectAsState(initial = null)

    // Determine next level
    var nextLevel by remember { mutableIntStateOf(0) }
    var hasProgress by remember { mutableStateOf(false) }
    LaunchedEffect(completedCount) {
        val xpSystem = XpSystem(db.progressDao())
        xpSystem.getProfile() // ensure profile exists
        // Find next incomplete level
        for (id in 0..55) {
            val progress = db.progressDao().getProgress(id)
            if (progress == null || !progress.completed) {
                nextLevel = id
                break
            }
        }
        hasProgress = completedCount > 0 || db.progressDao().getLastAttemptedLevel() != null
    }

    val currentProfile = profile ?: UserProfile()
    val rank = XpSystem.getRank(currentProfile.totalXp)
    val xpProgress = XpSystem.getProgressPercent(currentProfile.totalXp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ASCII Art Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = asciiTitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = TerminalCyan,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Master the Terminal. Own the System.",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$completedCount/56 levels · $unlockedCount achievements",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            // XP & Rank Badge
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = rank.badge,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = rank.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TerminalCyan
                    )
                    Text(
                        text = "${currentProfile.totalXp} XP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // XP Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = TerminalCyan,
                    trackColor = CardSurface
                )
                val (earned, needed) = XpSystem.getXpToNextRank(currentProfile.totalXp)
                if (rank.name != "Linux Master") {
                    Text(
                        text = "$earned / $needed XP to next rank",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "MAX RANK ACHIEVED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = StarGold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary: Continue / Start
            Button(
                onClick = { onStartGame(nextLevel) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalCyan,
                    contentColor = DeepNavy
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text(
                    text = if (hasProgress) "▶  CONTINUE — Level $nextLevel" else "▶  START QUEST",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (hasProgress) 15.sp else 18.sp
                )
            }

            // Secondary: Select Level
            MenuButton(label = "\uD83D\uDDC2  SELECT LEVEL", onClick = onSelectLevel)
            MenuButton(label = "\uD83D\uDCD6  MANUAL", onClick = onOpenManual)
            MenuButton(label = "\uD83C\uDFC6  ACHIEVEMENTS", onClick = onOpenAchievements)
            MenuButton(label = "⚙  SETTINGS", onClick = onOpenSettings)
        }

        // Version
        Text(
            text = "v1.1 — Inspired by OverTheWire Bandit",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun MenuButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(48.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}
