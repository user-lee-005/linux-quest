package com.linuxquest.shell.commands

/**
 * Represents a parsed command-line flag.
 */
data class CommandFlag(
    val short: Char?,
    val long: String?,
    val hasValue: Boolean = false,
    val description: String = ""
)

/**
 * Parses command-line arguments against a list of known flags.
 *
 * @return A pair of (matched flags with optional values, remaining positional arguments).
 *         For boolean flags the value is null; for value-flags it is the next argument.
 */
fun parseFlags(
    args: List<String>,
    flags: List<CommandFlag>
): Pair<Map<String, String?>, List<String>> {
    val matched = mutableMapOf<String, String?>()
    val positional = mutableListOf<String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg == "--" -> {
                positional.addAll(args.subList(i + 1, args.size))
                return matched to positional
            }
            arg.startsWith("--") -> {
                val longName = arg.removePrefix("--")
                val flag = flags.find { it.long == longName }
                if (flag != null) {
                    val key = flag.long ?: flag.short.toString()
                    if (flag.hasValue) {
                        i++
                        matched[key] = if (i < args.size) args[i] else null
                    } else {
                        matched[key] = null
                    }
                } else {
                    positional.add(arg)
                }
            }
            arg.startsWith("-") && arg.length > 1 -> {
                val chars = arg.removePrefix("-")
                var j = 0
                while (j < chars.length) {
                    val c = chars[j]
                    val flag = flags.find { it.short == c }
                    if (flag != null) {
                        val key = flag.long ?: c.toString()
                        if (flag.hasValue) {
                            val remaining = chars.substring(j + 1)
                            if (remaining.isNotEmpty()) {
                                matched[key] = remaining
                                j = chars.length
                            } else {
                                i++
                                matched[key] = if (i < args.size) args[i] else null
                            }
                        } else {
                            matched[key] = null
                        }
                    }
                    j++
                }
            }
            else -> positional.add(arg)
        }
        i++
    }
    return matched to positional
}

