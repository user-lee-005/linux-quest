package com.linuxquest.terminal

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linuxquest.ui.theme.*

@Composable
fun TerminalView(
    terminalState: TerminalState,
    prompt: String,
    fontSize: Int = 14,
    onSubmitCommand: (String) -> Unit,
    onTabComplete: ((String, Int) -> TabCompletionResult)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Calculate max characters per line based on screen width
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val maxChars = remember(configuration.screenWidthDp, fontSize) {
        val availableWidthDp = configuration.screenWidthDp - 16 // padding
        val charWidthDp = fontSize * 0.6f // monospace char width approximation
        (availableWidthDp / charWidthDp).toInt().coerceAtLeast(20)
    }

    // Auto-scroll to bottom when new lines are added
    LaunchedEffect(terminalState.lines.size) {
        if (terminalState.lines.isNotEmpty()) {
            listState.animateScrollToItem(terminalState.lines.size)
        }
    }

    // Request focus on mount
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavy)
            .imePadding()
    ) {
        // Terminal output — SelectionContainer gets weight so QuickActionBar stays at bottom
        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
            ) {
                items(terminalState.lines.toList()) { line ->
                    TerminalLineRow(line = line, fontSize = fontSize, maxChars = maxChars)
                }

                // Current input line
                item {
                    CurrentInputLine(
                        prompt = prompt,
                        terminalState = terminalState,
                        fontSize = fontSize,
                        focusRequester = focusRequester,
                        onSubmit = { cmd ->
                            onSubmitCommand(cmd)
                        },
                        onHistoryUp = { terminalState.historyUp() },
                        onHistoryDown = { terminalState.historyDown() },
                        onTabComplete = onTabComplete
                    )
                }
            }
        }

        // Quick action bar — fixed at bottom, never pushed off screen
        QuickActionBar(
            onAction = { action ->
                when (action) {
                    "↑" -> terminalState.historyUp()
                    "↓" -> terminalState.historyDown()
                    "TAB" -> {
                        onTabComplete?.let { completer ->
                            val result = completer(
                                terminalState.currentInput.value,
                                terminalState.cursorPosition.value
                            )
                            if (result.suggestions.size == 1 || result.commonPrefix.length > terminalState.currentInput.value.length) {
                                val parts = terminalState.currentInput.value.split(" ").toMutableList()
                                if (parts.isNotEmpty()) {
                                    parts[parts.lastIndex] = result.commonPrefix
                                }
                                terminalState.currentInput.value = parts.joinToString(" ")
                                terminalState.cursorPosition.value = terminalState.currentInput.value.length
                            }
                            if (result.suggestions.size > 1) {
                                terminalState.appendOutput(result.suggestions.joinToString("  "), LineType.INFO)
                            }
                        }
                    }
                    "Ctrl+C" -> {
                        terminalState.currentInput.value = ""
                        terminalState.appendOutput("^C", LineType.SYSTEM)
                    }
                    else -> {
                        terminalState.currentInput.value += action
                        terminalState.cursorPosition.value = terminalState.currentInput.value.length
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

/** Break long text into lines that fit the screen width */
private fun wrapText(text: String, maxChars: Int): String {
    if (text.length <= maxChars || maxChars <= 0) return text
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
        val end = (i + maxChars).coerceAtMost(text.length)
        if (i > 0) sb.append('\n')
        sb.append(text, i, end)
        i = end
    }
    return sb.toString()
}

@Composable
private fun TerminalLineRow(line: TerminalLine, fontSize: Int, maxChars: Int) {
    val rawText = line.text
    val wrappedText = remember(rawText, maxChars) { wrapText(rawText, maxChars) }
    val segments = AnsiParser.parse(wrappedText)
    val baseColor = when (line.type) {
        LineType.PROMPT -> TerminalCyan
        LineType.ERROR -> TerminalRed
        LineType.SYSTEM -> TerminalYellow
        LineType.INFO -> TerminalPurple
        LineType.OUTPUT -> TextPrimary
    }

    Text(
        text = buildAnnotatedString {
            if (segments.size == 1 && segments.first().color == TextPrimary) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(segments.first().text)
                }
            } else {
                segments.forEach { seg ->
                    withStyle(
                        SpanStyle(
                            color = seg.color,
                            fontWeight = if (seg.bold) FontWeight.Bold else FontWeight.Normal,
                            textDecoration = if (seg.underline) TextDecoration.Underline else TextDecoration.None
                        )
                    ) {
                        append(seg.text)
                    }
                }
            }
        },
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        softWrap = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    )
}

@Composable
private fun CurrentInputLine(
    prompt: String,
    terminalState: TerminalState,
    fontSize: Int,
    focusRequester: FocusRequester,
    onSubmit: (String) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    onTabComplete: ((String, Int) -> TabCompletionResult)?
) {
    // Blinking cursor
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prompt,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = TerminalCyan
        )

        BasicTextField(
            value = terminalState.currentInput.value,
            onValueChange = {
                terminalState.currentInput.value = it
                terminalState.cursorPosition.value = it.length
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = TextPrimary
            ),
            cursorBrush = SolidColor(TerminalCyan.copy(alpha = cursorAlpha)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val cmd = terminalState.submitCommand()
                    onSubmit(cmd)
                }
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionUp -> { onHistoryUp(); true }
                            Key.DirectionDown -> { onHistoryDown(); true }
                            Key.Tab -> {
                                onTabComplete?.let { completer ->
                                    val result = completer(
                                        terminalState.currentInput.value,
                                        terminalState.cursorPosition.value
                                    )
                                    if (result.commonPrefix.isNotEmpty()) {
                                        terminalState.currentInput.value = result.commonPrefix
                                        terminalState.cursorPosition.value = result.commonPrefix.length
                                    }
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        )
    }
}

@Composable
private fun QuickActionBar(
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttons = listOf("TAB", "↑", "↓", "Ctrl+C", "/", "|", ">", "-", "~", "$", "'", "\"")

    Row(
        modifier = modifier
            .background(DarkSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        buttons.forEach { label ->
            Box(
                modifier = Modifier
                    .background(CardSurface, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .clickable { onAction(label) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TerminalCyan
                )
            }
        }
    }
}
