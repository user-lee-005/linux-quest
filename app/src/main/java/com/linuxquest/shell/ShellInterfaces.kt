package com.linuxquest.shell

import com.linuxquest.filesystem.VirtualFileSystem

data class CommandResult(
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0
) {
    val isSuccess: Boolean get() = exitCode == 0
}

interface Command {
    val name: String
    val description: String
    val usage: String
    val manPage: String
    fun execute(
        args: List<String>,
        stdin: String?,
        vfs: VirtualFileSystem,
        env: ShellEnvironment
    ): CommandResult
}
