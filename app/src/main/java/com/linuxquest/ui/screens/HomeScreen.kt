package com.linuxquest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linuxquest.data.AppDatabase
import com.linuxquest.ui.theme.*

private val asciiTitle = """
╦  ╦╔╗╔╦ ╦═╗ ╔═╗╦ ╦╔═╗╔═╗╔╦╗
║  ║║║║║ ║╔╝ ║ ║║ ║║╣ ╚═╗ ║ 
╩═╝╩╝╚╝╚═╝╩  ╚═╝╚═╝╚═╝╚═╝ ╩
""".trimIndent()

@Composable
fun HomeScreen(
    onStartGame: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    val completedCount by db.progressDao().getCompletedCount()
        .collectAsState(initial = 0)
    val achievements by db.achievementDao().getAllAchievements()
        .collectAsState(initial = emptyList())
    val unlockedCount = remember(achievements) { achievements.count { it.unlocked } }

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
        Spacer(modifier = Modifier.height(48.dp))

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
        }

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartGame,
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
                    text = "▶  PLAY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            MenuButton(label = "\uD83D\uDCD6  MANUAL", onClick = onOpenManual)
            MenuButton(label = "\uD83C\uDFC6  ACHIEVEMENTS", onClick = onOpenAchievements)
            MenuButton(label = "⚙  SETTINGS", onClick = onOpenSettings)
        }

        // Version
        Text(
            text = "v1.0 — Inspired by OverTheWire Bandit",
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
