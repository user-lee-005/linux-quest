package com.linuxquest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LinuxQuestColorScheme = darkColorScheme(
    primary = TerminalCyan,
    onPrimary = DeepNavy,
    primaryContainer = DarkSurface,
    onPrimaryContainer = TerminalCyan,
    secondary = TerminalPurple,
    onSecondary = DeepNavy,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = TerminalPurple,
    tertiary = TerminalGreen,
    onTertiary = DeepNavy,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = SubtleBorder,
    error = TerminalRed,
    onError = DeepNavy
)

@Composable
fun LinuxQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LinuxQuestColorScheme,
        typography = LinuxQuestTypography,
        content = content
    )
}
