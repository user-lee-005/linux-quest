package com.linuxquest.shell

import com.linuxquest.filesystem.VirtualFileSystem

// ──────────────────────────── Token types ────────────────────────────

enum class TokenType {
    WORD,
    PIPE,              // |
    REDIRECT_OUT,      // >
    REDIRECT_APPEND,   // >>
    REDIRECT_IN,       // <
    REDIRECT_ERR,      // 2>
    REDIRECT_ERR_TO_OUT, // 2>&1
    AND,               // &&
    OR,                // ||
    SEMICOLON,         // ;  (also used for newlines)
    BACKGROUND         // &
}

data class Token(
    val type: TokenType,
    val value: String,
    val globbable: Boolean = false
)

// ──────────────────────────── AST nodes ─────────────────────────────

enum class RedirectType { OUT, APPEND, IN, ERR, ERR_TO_OUT }

data class Redirect(val type: RedirectType, val target: String)

sealed class ShellCommand {
    data class SimpleCommand(
        val name: String,
        val args: List<String>,
        val redirects: List<Redirect>,
        val background: Boolean = false
    ) : ShellCommand()

    data class Pipeline(
        val commands: List<SimpleCommand>,
        val background: Boolean = false
    ) : ShellCommand()

    data class AndList(
        val left: ShellCommand,
        val right: ShellCommand
    ) : ShellCommand()

    data class OrList(
        val left: ShellCommand,
        val right: ShellCommand
    ) : ShellCommand()

    data class CommandList(
        val commands: List<ShellCommand>
    ) : ShellCommand()
}

class ShellParseException(message: String) : Exception(message)

// ──────────────────────────── Parser ────────────────────────────────

/**
 * Tokenises raw shell input and parses it into a [ShellCommand] AST.
 *
 * Handles:
 *  - Single quotes (literal), double quotes (variable expansion),
 *    backslash escaping
 *  - Glob patterns `*`, `?`, `[abc]` expanded against the VFS
 *  - Command substitution `$(cmd)` and `` `cmd` ``
 *  - Operators: `|`, `&&`, `||`, `;`, `>`, `>>`, `<`, `2>`, `2>&1`, `&`
 *  - Tilde expansion at the start of a word
 */
class CommandParser(
    private val env: ShellEnvironment,
    private val vfs: VirtualFileSystem? = null,
    private val commandSubstituter: ((String) -> String)? = null
) {
    private var tokens = listOf<Token>()
    private var pos = 0

    private val wordBreakChars = setOf('|', '&', ';', '>', '<', '(', ')', '\n')

    // ========================== public API ==========================

    fun parse(input: String): ShellCommand? {
        tokens = tokenize(input)
        if (tokens.isEmpty()) return null
        pos = 0
        if (vfs != null) tokens = expandGlobs(tokens)
        return parseCommandList()
    }

    // ========================== Tokenizer ===========================

    fun tokenize(input: String): List<Token> {
        val result = mutableListOf<Token>()
        var i = 0
        val len = input.length

        while (i < len) {
            while (i < len && (input[i] == ' ' || input[i] == '\t')) i++
            if (i >= len) break

            when {
                input[i] == '#' -> { while (i < len && input[i] != '\n') i++ }
                input[i] == '\n' -> { result += Token(TokenType.SEMICOLON, "\n"); i++ }

                // 2>&1
                i + 3 < len && input[i] == '2' && input[i + 1] == '>'
                        && input[i + 2] == '&' && input[i + 3] == '1' ->
                    { result += Token(TokenType.REDIRECT_ERR_TO_OUT, "2>&1"); i += 4 }

                // 2>  (not 2>>)
                i + 1 < len && input[i] == '2' && input[i + 1] == '>'
                        && (i + 2 >= len || input[i + 2] != '>') ->
                    { result += Token(TokenType.REDIRECT_ERR, "2>"); i += 2 }

                i + 1 < len && input[i] == '|' && input[i + 1] == '|' ->
                    { result += Token(TokenType.OR, "||"); i += 2 }
                input[i] == '|' ->
                    { result += Token(TokenType.PIPE, "|"); i++ }

                i + 1 < len && input[i] == '&' && input[i + 1] == '&' ->
                    { result += Token(TokenType.AND, "&&"); i += 2 }
                input[i] == '&' ->
                    { result += Token(TokenType.BACKGROUND, "&"); i++ }

                input[i] == ';' ->
                    { result += Token(TokenType.SEMICOLON, ";"); i++ }

                i + 1 < len && input[i] == '>' && input[i + 1] == '>' ->
                    { result += Token(TokenType.REDIRECT_APPEND, ">>"); i += 2 }
                input[i] == '>' ->
                    { result += Token(TokenType.REDIRECT_OUT, ">"); i++ }
                input[i] == '<' ->
                    { result += Token(TokenType.REDIRECT_IN, "<"); i++ }

                else -> {
                    val (word, newPos, glob) = readWord(input, i)
                    if (word.isNotEmpty()) result += Token(TokenType.WORD, word, glob)
                    i = newPos
                }
            }
        }
        return result
    }

    // ─────────────── word reader (quoting / expansion) ──────────────

    private fun readWord(input: String, startPos: Int): Triple<String, Int, Boolean> {
        val word = StringBuilder()
        var i = startPos
        val len = input.length
        var hasUnquotedGlob = false

        while (i < len && input[i] != ' ' && input[i] != '\t' && input[i] !in wordBreakChars) {
            when {
                // single quotes — literal
                input[i] == '\'' -> {
                    i++
                    while (i < len && input[i] != '\'') { word.append(input[i]); i++ }
                    if (i < len) i++
                }
                // double quotes — expand $ and `
                input[i] == '"' -> {
                    i++
                    while (i < len && input[i] != '"') {
                        when {
                            input[i] == '\\' && i + 1 < len
                                    && input[i + 1] in DQ_ESCAPABLE ->
                                { word.append(input[i + 1]); i += 2 }
                            input[i] == '$' && i + 1 < len && input[i + 1] == '(' -> {
                                val (s, ni) = readCommandSub(input, i); word.append(s); i = ni
                            }
                            input[i] == '`' -> {
                                val (s, ni) = readBacktickSub(input, i); word.append(s); i = ni
                            }
                            input[i] == '$' -> {
                                val (s, ni) = readDollar(input, i); word.append(s); i = ni
                            }
                            else -> { word.append(input[i]); i++ }
                        }
                    }
                    if (i < len) i++
                }
                // backslash escape
                input[i] == '\\' -> {
                    if (i + 1 < len) { word.append(input[i + 1]); i += 2 }
                    else { word.append('\\'); i++ }
                }
                // $(cmd) substitution
                input[i] == '$' && i + 1 < len && input[i + 1] == '(' -> {
                    val (s, ni) = readCommandSub(input, i); word.append(s); i = ni
                }
                // `cmd` substitution
                input[i] == '`' -> {
                    val (s, ni) = readBacktickSub(input, i); word.append(s); i = ni
                }
                // $VAR expansion
                input[i] == '$' -> {
                    val (s, ni) = readDollar(input, i); word.append(s); i = ni
                }
                // tilde at start
                input[i] == '~' && word.isEmpty() -> {
                    word.append(env.getVariable("HOME") ?: "~"); i++
                }
                // glob meta-characters (unquoted)
                input[i] in GLOB_CHARS -> { hasUnquotedGlob = true; word.append(input[i]); i++ }
                // plain character
                else -> { word.append(input[i]); i++ }
            }
        }
        return Triple(word.toString(), i, hasUnquotedGlob)
    }

    // ─────────────── $ expansion ────────────────────────────────────

    private fun readDollar(input: String, startPos: Int): Pair<String, Int> {
        var i = startPos + 1
        if (i >= input.length) return "$" to i
        return when {
            input[i] == '{' -> {
                val end = input.indexOf('}', i + 1)
                if (end == -1) "\${" to (i + 1)
                else resolveVar(input.substring(i + 1, end)) to (end + 1)
            }
            input[i] == '?' -> env.lastExitCode.toString() to (i + 1)
            input[i] == '!' -> env.lastBackgroundPid.toString() to (i + 1)
            input[i] == '$' -> env.shellPid.toString() to (i + 1)
            input[i] == '@' -> env.positionalArgs.joinToString(" ") to (i + 1)
            input[i] == '#' -> env.positionalArgs.size.toString() to (i + 1)
            input[i] == '0' -> env.shellName to (i + 1)
            input[i] in '1'..'9' ->
                env.positionalArgs.getOrElse(input[i] - '1') { "" } to (i + 1)
            input[i].isLetter() || input[i] == '_' -> {
                val s = i
                while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_')) i++
                (env.getVariable(input.substring(s, i)) ?: "") to i
            }
            else -> "$" to (startPos + 1)
        }
    }

    private fun resolveVar(name: String): String = when (name) {
        "?" -> env.lastExitCode.toString()
        "!" -> env.lastBackgroundPid.toString()
        "$" -> env.shellPid.toString()
        "@" -> env.positionalArgs.joinToString(" ")
        "#" -> env.positionalArgs.size.toString()
        "0" -> env.shellName
        else -> if (name.length == 1 && name[0] in '1'..'9')
            env.positionalArgs.getOrElse(name[0] - '1') { "" }
        else env.getVariable(name) ?: ""
    }

    // ─────────────── command substitution ────────────────────────────

    private fun readCommandSub(input: String, startPos: Int): Pair<String, Int> {
        var i = startPos + 2        // skip $(
        var depth = 1
        val cmd = StringBuilder()
        while (i < input.length && depth > 0) {
            when (input[i]) {
                '(' -> { depth++; cmd.append('(') }
                ')' -> { depth--; if (depth > 0) cmd.append(')') }
                else -> cmd.append(input[i])
            }
            i++
        }
        return (commandSubstituter?.invoke(cmd.toString())?.trimEnd('\n') ?: "") to i
    }

    private fun readBacktickSub(input: String, startPos: Int): Pair<String, Int> {
        var i = startPos + 1            // skip opening `
        val cmd = StringBuilder()
        while (i < input.length && input[i] != '`') {
            if (input[i] == '\\' && i + 1 < input.length && input[i + 1] == '`') {
                cmd.append('`'); i += 2
            } else { cmd.append(input[i]); i++ }
        }
        if (i < input.length) i++       // skip closing `
        return (commandSubstituter?.invoke(cmd.toString())?.trimEnd('\n') ?: "") to i
    }

    // ========================== Glob expansion ======================

    private fun expandGlobs(tokens: List<Token>): List<Token> =
        tokens.flatMap { t ->
            if (t.type == TokenType.WORD && t.globbable && vfs != null) {
                val m = expandGlob(t.value)
                if (m.isNotEmpty()) m.sorted().map { Token(TokenType.WORD, it) }
                else listOf(t.copy(globbable = false))
            } else listOf(t)
        }

    private fun expandGlob(pattern: String): List<String> {
        val vfs = this.vfs ?: return emptyList()
        val cwd = env.getVariable("PWD") ?: "/"
        val lastSlash = pattern.lastIndexOf('/')
        val dirPath: String
        val filePattern: String
        val prefix: String

        if (lastSlash >= 0) {
            val raw = pattern.substring(0, lastSlash).ifEmpty { "/" }
            filePattern = pattern.substring(lastSlash + 1)
            dirPath = if (raw.startsWith("/")) raw else "$cwd/$raw"
            prefix = pattern.substring(0, lastSlash + 1)
        } else {
            dirPath = cwd; filePattern = pattern; prefix = ""
        }
        if (!containsGlob(filePattern)) return emptyList()
        val regex = globToRegex(filePattern)
        return try {
            vfs.listDirectory(dirPath)
                .map { it.name }
                .filter { regex.matches(it) }
                .filter { !it.startsWith(".") || filePattern.startsWith(".") }
                .map { "$prefix$it" }
        } catch (_: Exception) { emptyList() }
    }

    // ========================== Recursive-descent parser =============

    private fun peek(): Token? = tokens.getOrNull(pos)
    private fun advance(): Token {
        return tokens.getOrNull(pos)?.also { pos++ }
            ?: throw ShellParseException("Unexpected end of input")
    }
    private fun expectWord(): Token {
        val t = peek() ?: throw ShellParseException("Expected word, got end of input")
        if (t.type != TokenType.WORD) throw ShellParseException("Expected word, got '${t.value}'")
        return advance()
    }

    // command_list : and_or ((';'|'&'|'\n') and_or)*
    private fun parseCommandList(): ShellCommand {
        skipSeparators()
        if (peek() == null) throw ShellParseException("Empty command")
        val cmds = mutableListOf(parseAndOr())

        while (peek() != null) {
            when (peek()!!.type) {
                TokenType.SEMICOLON -> {
                    advance(); skipSeparators()
                    if (peek() != null && isCommandStart()) cmds += parseAndOr()
                }
                TokenType.BACKGROUND -> {
                    advance()
                    cmds[cmds.lastIndex] = markBackground(cmds.last())
                    skipSeparators()
                    if (peek() != null && isCommandStart()) cmds += parseAndOr()
                }
                else -> break
            }
        }
        return if (cmds.size == 1) cmds[0] else ShellCommand.CommandList(cmds)
    }

    // and_or : pipeline (('&&'|'||') pipeline)*
    private fun parseAndOr(): ShellCommand {
        var node = parsePipeline()
        while (true) {
            node = when (peek()?.type) {
                TokenType.AND -> { advance(); ShellCommand.AndList(node, parsePipeline()) }
                TokenType.OR  -> { advance(); ShellCommand.OrList(node, parsePipeline()) }
                else -> return node
            }
        }
    }

    // pipeline : simple_command ('|' simple_command)*
    private fun parsePipeline(): ShellCommand {
        val cmds = mutableListOf(parseSimpleCommand())
        while (peek()?.type == TokenType.PIPE) { advance(); cmds += parseSimpleCommand() }
        return if (cmds.size == 1) cmds[0] else ShellCommand.Pipeline(cmds)
    }

    // simple_command : (WORD | redirect)+
    private fun parseSimpleCommand(): ShellCommand.SimpleCommand {
        val words = mutableListOf<String>()
        val redirects = mutableListOf<Redirect>()

        while (peek() != null) {
            when (peek()!!.type) {
                TokenType.WORD -> words += advance().value
                TokenType.REDIRECT_OUT ->
                    { advance(); redirects += Redirect(RedirectType.OUT, expectWord().value) }
                TokenType.REDIRECT_APPEND ->
                    { advance(); redirects += Redirect(RedirectType.APPEND, expectWord().value) }
                TokenType.REDIRECT_IN ->
                    { advance(); redirects += Redirect(RedirectType.IN, expectWord().value) }
                TokenType.REDIRECT_ERR ->
                    { advance(); redirects += Redirect(RedirectType.ERR, expectWord().value) }
                TokenType.REDIRECT_ERR_TO_OUT ->
                    { advance(); redirects += Redirect(RedirectType.ERR_TO_OUT, "") }
                else -> break
            }
        }
        if (words.isEmpty()) throw ShellParseException("Expected command name")
        return ShellCommand.SimpleCommand(words[0], words.drop(1), redirects)
    }

    // helpers
    private fun skipSeparators() { while (peek()?.type == TokenType.SEMICOLON) advance() }
    private fun isCommandStart(): Boolean {
        val t = peek() ?: return false
        return t.type == TokenType.WORD || t.type in REDIRECT_TOKENS
    }
    private fun markBackground(cmd: ShellCommand): ShellCommand = when (cmd) {
        is ShellCommand.SimpleCommand -> cmd.copy(background = true)
        is ShellCommand.Pipeline      -> cmd.copy(background = true)
        else -> cmd
    }

    companion object {
        private val GLOB_CHARS = setOf('*', '?', '[')
        private val DQ_ESCAPABLE = setOf('"', '\\', '$', '`', '\n')
        private val REDIRECT_TOKENS = setOf(
            TokenType.REDIRECT_OUT, TokenType.REDIRECT_APPEND, TokenType.REDIRECT_IN,
            TokenType.REDIRECT_ERR, TokenType.REDIRECT_ERR_TO_OUT
        )

        fun containsGlob(s: String): Boolean = s.any { it in GLOB_CHARS }

        fun globToRegex(glob: String): Regex {
            val sb = StringBuilder("^")
            var i = 0
            while (i < glob.length) {
                when (glob[i]) {
                    '*' -> sb.append("[^/]*")
                    '?' -> sb.append("[^/]")
                    '[' -> {
                        sb.append('['); i++
                        if (i < glob.length && glob[i] == '!') { sb.append('^'); i++ }
                        while (i < glob.length && glob[i] != ']') {
                            if (glob[i] == '-') sb.append('-')
                            else sb.append(Regex.escape(glob[i].toString()))
                            i++
                        }
                        sb.append(']')
                    }
                    else -> sb.append(Regex.escape(glob[i].toString()))
                }
                i++
            }
            sb.append('$')
            return sb.toString().toRegex()
        }
    }
}
