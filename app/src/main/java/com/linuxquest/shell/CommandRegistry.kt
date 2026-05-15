package com.linuxquest.shell

/**
 * Facade over the command map in [ShellEnvironment] that provides
 * registration, lookup, listing, and man-page retrieval.
 *
 * Internally delegates to [ShellEnvironment.registerCommand] /
 * [ShellEnvironment.getCommand] so that existing commands (e.g.
 * `ManCommand`, `HelpCommand`) that call `env.getCommand(name)`
 * continue to work.
 */
class CommandRegistry(private val env: ShellEnvironment) {

    /** Register a single command. */
    fun register(command: Command) {
        env.registerCommand(command)
    }

    /** Register many commands at once. */
    fun registerAll(commands: Iterable<Command>) {
        for (cmd in commands) env.registerCommand(cmd)
    }

    /** Look up a command by name. Resolves through aliases first. */
    fun lookup(name: String): Command? {
        env.getCommand(name)?.let { return it }
        val aliased = env.getAlias(name) ?: return null
        val aliasCmd = aliased.trim().split(Regex("\\s+")).firstOrNull() ?: return null
        return env.getCommand(aliasCmd)
    }

    /** Sorted list of all registered command names. */
    fun listCommands(): List<String> =
        env.getAllCommands().keys.sorted()

    /** Return the man page text for [name], or null. */
    fun getManPage(name: String): String? =
        env.getCommand(name)?.manPage

    /** True if a command (or alias resolving to a command) is registered. */
    fun isRegistered(name: String): Boolean =
        env.getCommand(name) != null
}
