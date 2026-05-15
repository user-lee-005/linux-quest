package com.linuxquest.shell

import com.linuxquest.filesystem.VirtualFileSystem

/**
 * Executes parsed [ShellCommand] trees, handling pipelines, `&&`/`||`
 * chains, command lists, and I/O redirections.
 *
 * All I/O goes through the [VirtualFileSystem]; no real OS processes
 * are ever spawned.
 */
class PipelineExecutor {

    fun execute(
        command: ShellCommand,
        vfs: VirtualFileSystem,
        env: ShellEnvironment,
        stdin: String? = null
    ): CommandResult = when (command) {
        is ShellCommand.SimpleCommand -> executeSimple(command, vfs, env, stdin)
        is ShellCommand.Pipeline      -> executePipeline(command, vfs, env, stdin)
        is ShellCommand.AndList       -> executeAnd(command, vfs, env, stdin)
        is ShellCommand.OrList        -> executeOr(command, vfs, env, stdin)
        is ShellCommand.CommandList   -> executeList(command, vfs, env, stdin)
    }

    // ─────────────────── simple command ──────────────────────────────

    private fun executeSimple(
        command: ShellCommand.SimpleCommand,
        vfs: VirtualFileSystem,
        env: ShellEnvironment,
        pipeStdin: String?
    ): CommandResult {
        val cmd = env.getCommand(command.name)
            ?: return CommandResult(
                stderr = "${command.name}: command not found",
                exitCode = 127
            )

        // input redirect
        var effectiveStdin = pipeStdin
        for (r in command.redirects) {
            if (r.type == RedirectType.IN) {
                val path = resolvePath(r.target, vfs, env)
                try {
                    effectiveStdin = vfs.readFileText(path)
                } catch (e: Exception) {
                    return CommandResult(
                        stderr = "bash: ${r.target}: ${e.message ?: "No such file or directory"}",
                        exitCode = 1
                    )
                }
            }
        }

        val result = try {
            cmd.execute(command.args, effectiveStdin, vfs, env)
        } catch (e: Exception) {
            CommandResult(stderr = "${command.name}: ${e.message}", exitCode = 1)
        }

        return applyOutputRedirects(result, command.redirects, vfs, env, command.name)
    }

    // ─────────────────── pipeline ────────────────────────────────────

    private fun executePipeline(
        pipeline: ShellCommand.Pipeline,
        vfs: VirtualFileSystem,
        env: ShellEnvironment,
        stdin: String?
    ): CommandResult {
        var currentStdin = stdin
        var last = CommandResult()
        for ((idx, cmd) in pipeline.commands.withIndex()) {
            last = executeSimple(cmd, vfs, env, currentStdin)
            env.lastExitCode = last.exitCode
            if (idx < pipeline.commands.size - 1) currentStdin = last.stdout
        }
        return last
    }

    // ─────────────────── && / || / list ──────────────────────────────

    private fun executeAnd(
        node: ShellCommand.AndList,
        vfs: VirtualFileSystem,
        env: ShellEnvironment,
        stdin: String?
    ): CommandResult {
        val left = execute(node.left, vfs, env, stdin)
        env.lastExitCode = left.exitCode
        return if (left.isSuccess) execute(node.right, vfs, env, stdin) else left
    }

    private fun executeOr(
        node: ShellCommand.OrList,
        vfs: VirtualFileSystem,
        env: ShellEnvironment,
        stdin: String?
    ): CommandResult {
        val left = execute(node.left, vfs, env, stdin)
        env.lastExitCode = left.exitCode
        return if (!left.isSuccess) execute(node.right, vfs, env, stdin) else left
    }

    private fun executeList(
        node: ShellCommand.CommandList,
        vfs: VirtualFileSystem,
        env: ShellEnvironment,
        stdin: String?
    ): CommandResult {
        var last = CommandResult()
        for (cmd in node.commands) {
            last = execute(cmd, vfs, env, stdin)
            env.lastExitCode = last.exitCode
        }
        return last
    }

    // ─────────────────── redirect helpers ────────────────────────────

    private fun applyOutputRedirects(
        result: CommandResult,
        redirects: List<Redirect>,
        vfs: VirtualFileSystem,
        env: ShellEnvironment,
        cmdName: String
    ): CommandResult {
        var stdout = result.stdout
        var stderr = result.stderr
        var exitCode = result.exitCode

        for (r in redirects) {
            when (r.type) {
                RedirectType.OUT -> {
                    try {
                        vfs.writeFileText(resolvePath(r.target, vfs, env), stdout)
                        stdout = ""
                    } catch (e: Exception) {
                        stderr = "bash: ${r.target}: ${e.message ?: "Permission denied"}"
                        exitCode = 1
                    }
                }
                RedirectType.APPEND -> {
                    try {
                        val path = resolvePath(r.target, vfs, env)
                        val existing = try { vfs.readFileText(path) } catch (_: Exception) { "" }
                        vfs.writeFileText(path, existing + stdout)
                        stdout = ""
                    } catch (e: Exception) {
                        stderr = "bash: ${r.target}: ${e.message ?: "Permission denied"}"
                        exitCode = 1
                    }
                }
                RedirectType.ERR -> {
                    try {
                        vfs.writeFileText(resolvePath(r.target, vfs, env), stderr)
                        stderr = ""
                    } catch (e: Exception) {
                        stderr = "bash: ${r.target}: ${e.message ?: "Permission denied"}"
                        exitCode = 1
                    }
                }
                RedirectType.ERR_TO_OUT -> {
                    stdout += stderr; stderr = ""
                }
                RedirectType.IN -> { /* handled before execution */ }
            }
        }
        return CommandResult(stdout, stderr, exitCode)
    }

    private fun resolvePath(path: String, vfs: VirtualFileSystem, env: ShellEnvironment): String {
        if (path.startsWith("/")) return path
        val pwd = env.getVariable("PWD") ?: "/"
        return vfs.getAbsolutePath("$pwd/$path")
    }
}
