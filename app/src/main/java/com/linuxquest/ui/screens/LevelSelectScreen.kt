package com.linuxquest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linuxquest.data.AppDatabase
import com.linuxquest.data.entities.LevelProgress
import com.linuxquest.game.Level
import com.linuxquest.game.LevelCategory
import com.linuxquest.game.LevelManager
import com.linuxquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    onLevelSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val levelManager = remember { LevelManager() }

    val allProgress by db.progressDao().getAllProgress()
        .collectAsState(initial = emptyList())
    val progressMap = remember(allProgress) {
        allProgress.associateBy { it.levelId }
    }

    val categories = remember { LevelCategory.entries.toList() }
    var selectedCategory by remember { mutableIntStateOf(0) }

    val allLevels = remember { levelManager.getAllLevels() }
    val filteredLevels = remember(selectedCategory) {
        levelManager.getLevelsByCategory(categories[selectedCategory])
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
                    text = "SELECT LEVEL",
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

        // Category Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            containerColor = DarkSurface,
            contentColor = TerminalCyan,
            edgePadding = 8.dp,
            indicator = { tabPositions ->
                if (selectedCategory < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.fillMaxWidth(),
                        color = Color(categories[selectedCategory].color)
                    )
                }
            },
            divider = {}
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategory == index,
                    onClick = { selectedCategory = index },
                    text = {
                        Text(
                            text = category.displayName.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = if (selectedCategory == index)
                                Color(category.color) else TextMuted
                        )
                    }
                )
            }
        }

        // Level Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredLevels, key = { it.id }) { level ->
                val progress = progressMap[level.id]
                val isUnlocked = isLevelUnlocked(level.id, progressMap)
                val isCompleted = progress?.completed == true

                LevelCard(
                    level = level,
                    progress = progress,
                    isUnlocked = isUnlocked,
                    isCompleted = isCompleted,
                    onClick = { if (isUnlocked) onLevelSelected(level.id) }
                )
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: Level,
    progress: LevelProgress?,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isCompleted -> LevelCompleted
        isUnlocked -> LevelUnlocked
        else -> LevelLocked
    }
    val borderColor = when {
        isCompleted -> TerminalGreen
        isUnlocked -> TerminalCyan
        else -> SubtleBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .alpha(if (isUnlocked) 1f else 0.5f)
            .clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: level number + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%02d".format(level.id),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = when {
                        isCompleted -> TerminalGreen
                        isUnlocked -> TerminalCyan
                        else -> TextMuted
                    }
                )

                Text(
                    text = when {
                        isCompleted -> "✅"
                        isUnlocked -> "⬜"
                        else -> "\uD83D\uDD12"
                    },
                    fontSize = 18.sp
                )
            }

            // Title
            Text(
                text = level.title,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (isUnlocked) TextPrimary else TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            // Stars
            if (isCompleted && progress != null) {
                Text(
                    text = buildStars(progress.stars),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = StarGold,
                    textAlign = TextAlign.Start
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

private fun buildStars(count: Int): String {
    return "★".repeat(count.coerceIn(0, 3)) + "☆".repeat((3 - count).coerceIn(0, 3))
}

private fun isLevelUnlocked(levelId: Int, progressMap: Map<Int, LevelProgress>): Boolean {
    if (levelId == 0) return true
    val prev = progressMap[levelId - 1]
    return prev?.completed == true
}
