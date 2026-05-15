package com.linuxquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linuxquest.data.AppDatabase
import com.linuxquest.data.SettingsDataStore
import com.linuxquest.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }
    val db = remember { AppDatabase.getInstance(context) }

    val fontSize by settings.fontSize.collectAsState(initial = 14)
    val showHints by settings.showHints.collectAsState(initial = true)
    val vibration by settings.vibration.collectAsState(initial = true)

    var showResetDialog by remember { mutableStateOf(false) }
    var resetInput by remember { mutableStateOf("") }
    var isResetting by remember { mutableStateOf(false) }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                resetInput = ""
            },
            title = {
                Text(
                    text = "⚠ DANGER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TerminalRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This will erase ALL progress.\nType 'RESET' to confirm.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepNavy, RoundedCornerShape(6.dp))
                            .border(1.dp, SubtleBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (resetInput.isEmpty()) {
                            Text(
                                text = "$ type RESET...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = resetInput,
                            onValueChange = { resetInput = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = TerminalRed
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(TerminalRed),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (resetInput == "RESET") {
                            isResetting = true
                            scope.launch {
                                db.progressDao().resetAllProgress()
                                settings.resetAll()
                                isResetting = false
                                showResetDialog = false
                                resetInput = ""
                            }
                        }
                    },
                    enabled = resetInput == "RESET" && !isResetting
                ) {
                    Text(
                        text = if (isResetting) "RESETTING..." else "CONFIRM",
                        fontFamily = FontFamily.Monospace,
                        color = if (resetInput == "RESET") TerminalRed else TextMuted
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    resetInput = ""
                }) {
                    Text(
                        text = "CANCEL",
                        fontFamily = FontFamily.Monospace,
                        color = TerminalCyan
                    )
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
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "⚙ SETTINGS",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Font Size
            SettingsSection(title = "FONT SIZE") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terminal font size",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${fontSize}sp",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { newSize ->
                            scope.launch { settings.setFontSize(newSize.toInt()) }
                        },
                        valueRange = 10f..24f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = TerminalCyan,
                            activeTrackColor = TerminalCyan,
                            inactiveTrackColor = SubtleBorder
                        )
                    )

                    // Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepNavy, RoundedCornerShape(6.dp))
                            .border(1.dp, SubtleBorder, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "bandit0@linuxquest:~$ ls -la",
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize.sp,
                            color = TerminalCyan
                        )
                    }
                }
            }

            // Toggles
            SettingsSection(title = "GAMEPLAY") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsToggle(
                        label = "Show Hints",
                        description = "Display hint button during gameplay",
                        checked = showHints,
                        onCheckedChange = { scope.launch { settings.setShowHints(it) } }
                    )

                    HorizontalDivider(color = SubtleBorder, thickness = 1.dp)

                    SettingsToggle(
                        label = "Vibration",
                        description = "Haptic feedback on key events",
                        checked = vibration,
                        onCheckedChange = { scope.launch { settings.setVibration(it) } }
                    )
                }
            }

            // Danger Zone
            Text(
                text = "── DANGER ZONE ──",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TerminalRed,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalRed.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reset All Progress",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TerminalRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Erase all level progress, achievements, and settings.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalRed.copy(alpha = 0.15f),
                            contentColor = TerminalRed
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "RESET PROGRESS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Version info
            Text(
                text = "LinuxQuest v1.0\nInspired by OverTheWire Bandit",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = description,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TerminalCyan,
                checkedTrackColor = TerminalCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SubtleBorder
            )
        )
    }
}
