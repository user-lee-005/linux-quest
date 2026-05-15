package com.linuxquest.shell.commands

import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment

class PsCommand : Command {
    override val name = "ps"
    override val description = "Report process status (simulated)"
    override val usage = "ps [-e] [-f] [-aux]"
    override val manPage = "Report a snapshot of current processes (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val full = args.any { it.contains('f') || it.contains('e') || it.contains("aux") }
        return if (full) {
            CommandResult(stdout = """
UID        PID  PPID  C STIME TTY          TIME CMD
root         1     0  0 00:00 ?        00:00:01 /sbin/init
root        10     1  0 00:00 ?        00:00:00 /usr/sbin/sshd
root        42     1  0 00:00 ?        00:00:02 /usr/sbin/httpd
root        99     1  0 00:00 ?        00:00:00 /usr/bin/challenge
bandit0    200    10  0 00:01 pts/0    00:00:00 -bash
bandit0    312   200  0 00:05 pts/0    00:00:00 ps -ef
daemon      50     1  0 00:00 ?        00:00:05 /usr/bin/secretd --leak-password
            """.trimIndent())
        } else {
            CommandResult(stdout = """
  PID TTY          TIME CMD
  200 pts/0    00:00:00 bash
  312 pts/0    00:00:00 ps
            """.trimIndent())
        }
    }
}

class TopCommand : Command {
    override val name = "top"
    override val description = "Display system processes (simulated)"
    override val usage = "top"
    override val manPage = "Display Linux processes (simulated one-shot output)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        return CommandResult(stdout = """
top - 00:05:30 up  5:30,  1 user,  load average: 0.15, 0.10, 0.05
Tasks:   7 total,   1 running,   6 sleeping,   0 stopped,   0 zombie
%Cpu(s):  2.3 us,  1.0 sy,  0.0 ni, 96.5 id,  0.2 wa,  0.0 hi,  0.0 si
MiB Mem :   2048.0 total,   1024.0 free,    512.0 used,    512.0 buff/cache
MiB Swap:   1024.0 total,   1024.0 free,      0.0 used.   1400.0 avail Mem

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
    1 root      20   0    4500   3200   2800 S   0.0   0.2   0:01.00 init
   10 root      20   0    8200   5400   4800 S   0.0   0.3   0:00.50 sshd
   42 root      20   0   12400   8600   7200 S   0.3   0.4   0:02.00 httpd
   50 daemon    20   0    6800   4200   3400 S   5.0   0.2   0:05.00 secretd
   99 root      20   0    5600   3800   3200 S   0.0   0.2   0:00.10 challenge
  200 bandit0   20   0    8400   5600   4800 S   0.0   0.3   0:00.30 bash
  312 bandit0   20   0    4200   2800   2400 R   0.3   0.1   0:00.01 top
        """.trimIndent())
    }
}

class KillCommand : Command {
    override val name = "kill"
    override val description = "Send signal to a process (simulated)"
    override val usage = "kill [-signal] pid"
    override val manPage = "Send a signal to a process (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        if (args.isEmpty()) return CommandResult(stderr = "kill: missing operand", exitCode = 1)
        var signal = "TERM"; var pid: String? = null
        for (a in args) {
            when {
                a.startsWith("-") && a.length > 1 -> signal = a.drop(1).uppercase()
                else -> pid = a
            }
        }
        if (pid == null) return CommandResult(stderr = "kill: missing PID", exitCode = 1)
        return CommandResult(stdout = "Sent SIG$signal to process $pid (simulated)")
    }
}

class JobsCommand : Command {
    override val name = "jobs"
    override val description = "List background jobs (simulated)"
    override val usage = "jobs"
    override val manPage = "Display status of jobs in the current shell session (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        return CommandResult(stdout = "[1]+  Running                 sleep 100 &")
    }
}
