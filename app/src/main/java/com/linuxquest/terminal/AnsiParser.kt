package com.linuxquest.terminal

import androidx.compose.ui.graphics.Color
import com.linuxquest.ui.theme.*

data class StyledSegment(
    val text: String,
    val color: Color = TextPrimary,
    val bold: Boolean = false,
    val underline: Boolean = false
)

object AnsiParser {

    private val ANSI_REGEX = Regex("\u001B\\[([0-9;]*)m")

    private val COLOR_MAP = mapOf(
        30 to DeepNavy,      // black
        31 to TerminalRed,
        32 to TerminalGreen,
        33 to TerminalYellow,
        34 to TerminalBlue,
        35 to TerminalPurple,
        36 to TerminalCyan,
        37 to TextPrimary,   // white
        90 to TextMuted,     // bright black
        91 to TerminalRed,
        92 to TerminalGreen,
        93 to TerminalYellow,
        94 to TerminalBlue,
        95 to TerminalPurple,
        96 to TerminalCyan,
        97 to Color.White
    )

    fun parse(text: String): List<StyledSegment> {
        if (!text.contains('\u001B')) {
            return listOf(StyledSegment(text))
        }

        val segments = mutableListOf<StyledSegment>()
        var currentColor = TextPrimary
        var bold = false
        var underline = false
        var lastEnd = 0

        ANSI_REGEX.findAll(text).forEach { match ->
            val before = text.substring(lastEnd, match.range.first)
            if (before.isNotEmpty()) {
                segments.add(StyledSegment(before, currentColor, bold, underline))
            }

            val codes = match.groupValues[1].split(';').mapNotNull { it.toIntOrNull() }
            for (code in codes) {
                when (code) {
                    0 -> { currentColor = TextPrimary; bold = false; underline = false }
                    1 -> bold = true
                    4 -> underline = true
                    in 30..37, in 90..97 -> currentColor = COLOR_MAP[code] ?: TextPrimary
                }
            }
            lastEnd = match.range.last + 1
        }

        val remaining = text.substring(lastEnd)
        if (remaining.isNotEmpty()) {
            segments.add(StyledSegment(remaining, currentColor, bold, underline))
        }

        return segments
    }

    fun stripAnsi(text: String): String = text.replace(ANSI_REGEX, "")
}
