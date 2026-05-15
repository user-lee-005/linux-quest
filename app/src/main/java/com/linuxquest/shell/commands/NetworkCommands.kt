package com.linuxquest.shell.commands

import com.linuxquest.filesystem.VirtualFileSystem
import com.linuxquest.shell.Command
import com.linuxquest.shell.CommandResult
import com.linuxquest.shell.ShellEnvironment

class PingCommand : Command {
    override val name = "ping"
    override val description = "Send ICMP echo requests (simulated)"
    override val usage = "ping [-c N] host"
    override val manPage = "Send ICMP ECHO_REQUEST to network hosts (simulated output)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var count = 4; var host: String? = null
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-c" && i + 1 < args.size -> count = args[++i].toIntOrNull() ?: 4
                !args[i].startsWith("-") -> host = args[i]
            }
            i++
        }
        if (host == null) return CommandResult(stderr = "ping: missing host operand", exitCode = 1)
        val ip = when (host) {
            "localhost", "127.0.0.1" -> "127.0.0.1"
            "linuxquest" -> "10.0.0.1"
            else -> "93.184.${(host.hashCode() and 0xFF)}.${((host.hashCode() shr 8) and 0xFF)}"
        }
        val sb = StringBuilder()
        sb.appendLine("PING $host ($ip) 56(84) bytes of data.")
        repeat(count) { seq ->
            val time = "%.1f".format(0.5 + (seq * 0.3))
            sb.appendLine("64 bytes from $ip: icmp_seq=${seq + 1} ttl=64 time=${time} ms")
        }
        sb.appendLine()
        sb.appendLine("--- $host ping statistics ---")
        sb.appendLine("$count packets transmitted, $count received, 0% packet loss")
        return CommandResult(stdout = sb.toString().trimEnd())
    }
}

class CurlCommand : Command {
    override val name = "curl"
    override val description = "Transfer data from URLs (simulated)"
    override val usage = "curl [-s] [-o file] url"
    override val manPage = "Transfer data from or to a server (simulated)."

    private val responses = mapOf(
        "http://localhost:8080/password" to "d9b2a4f8c7e1a3b5d6f0e2c4a8b9d1f3",
        "http://localhost:8080/" to "<html><body><h1>Welcome to LinuxQuest Server</h1></body></html>",
        "http://localhost:30000" to "c4a8b9d1f3e7a2b6d9f0e1c3a5b8d2f4",
        "http://linuxquest.local/secret" to "e7a2b6d9f0e1c3a5b8d2f4c6a9b1d3f5"
    )

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var silent = false; var outputFile: String? = null; var url: String? = null
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-s" -> silent = true
                args[i] == "-o" && i + 1 < args.size -> outputFile = args[++i]
                !args[i].startsWith("-") -> url = args[i]
            }
            i++
        }
        if (url == null) return CommandResult(stderr = "curl: no URL specified", exitCode = 1)
        val body = responses[url] ?: "<!DOCTYPE html><html><body>404 Not Found</body></html>"
        val headerInfo = if (!silent) "  % Total    % Received\n  100   ${body.length}  100   ${body.length}\n\n" else ""
        if (outputFile != null) {
            try {
                if (vfs.exists(outputFile)) vfs.writeFile(outputFile, body.toByteArray())
                else vfs.createFile(outputFile, body.toByteArray())
            } catch (e: Exception) {
                return CommandResult(stderr = "curl: cannot write to $outputFile: ${e.message}", exitCode = 1)
            }
            return CommandResult(stdout = if (silent) "" else headerInfo.trimEnd())
        }
        return CommandResult(stdout = body)
    }
}

class NcCommand : Command {
    override val name = "nc"
    override val description = "Netcat - network utility (simulated)"
    override val usage = "nc [-l] [-p port] host port"
    override val manPage = "Arbitrary TCP/UDP connections and listens (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        var listen = false; val positional = mutableListOf<String>()
        for (a in args) {
            when (a) { "-l" -> listen = true; "-p" -> {}; else -> if (!a.startsWith("-")) positional.add(a) }
        }
        if (listen) return CommandResult(stdout = "Listening on 0.0.0.0 (simulated)")
        if (positional.size < 2) return CommandResult(stderr = "nc: missing host or port", exitCode = 1)
        val host = positional[0]; val port = positional[1]
        val response = when {
            port == "30000" -> "b7a2f9c4d8e1a3b5f6d0c2e4a8b1d9f3"
            port == "31337" -> "Welcome to the secret service.\nPassword: f5d8a1c3e7b2f9d4a6c0e8b5a3d1f7c9"
            else -> "Connection to $host $port port [tcp/*] succeeded!"
        }
        return CommandResult(stdout = response)
    }
}

class SshCommand : Command {
    override val name = "ssh"
    override val description = "OpenSSH client (simulated)"
    override val usage = "ssh [user@]host"
    override val manPage = "SSH client for remote login (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        val target = args.firstOrNull() ?: return CommandResult(stderr = "ssh: missing destination", exitCode = 1)
        val (user, host) = if ("@" in target) target.split("@", limit = 2) else listOf("bandit0", target)
        return CommandResult(stdout = """
Welcome to LinuxQuest SSH Server
$user@$host's password: ********
Last login: Thu Jan  1 00:00:00 2025
$user@$host:~${'$'} 
Connection simulated. In a real system, this would open a remote shell.
        """.trimIndent())
    }
}

class IfconfigCommand : Command {
    override val name = "ifconfig"
    override val description = "Configure network interfaces (simulated)"
    override val usage = "ifconfig"
    override val manPage = "Display network interface configuration (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        return CommandResult(stdout = """
eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 10.0.0.2  netmask 255.255.255.0  broadcast 10.0.0.255
        inet6 fe80::1  prefixlen 64  scopeid 0x20<link>
        ether 02:42:ac:11:00:02  txqueuelen 0
        RX packets 1024  bytes 102400 (100.0 KB)
        TX packets 512  bytes 51200 (50.0 KB)

lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        inet6 ::1  prefixlen 128  scopeid 0x10<host>
        loop  txqueuelen 1000
        RX packets 256  bytes 25600 (25.0 KB)
        TX packets 256  bytes 25600 (25.0 KB)
        """.trimIndent())
    }
}

class NetstatCommand : Command {
    override val name = "netstat"
    override val description = "Print network connections (simulated)"
    override val usage = "netstat [-tulnp]"
    override val manPage = "Print network connections, routing tables, interface statistics (simulated)."

    override fun execute(args: List<String>, stdin: String?, vfs: VirtualFileSystem, env: ShellEnvironment): CommandResult {
        return CommandResult(stdout = """
Active Internet connections (servers and established)
Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program
tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN      1/sshd
tcp        0      0 0.0.0.0:8080            0.0.0.0:*               LISTEN      42/httpd
tcp        0      0 0.0.0.0:30000           0.0.0.0:*               LISTEN      99/challenge
tcp        0      0 10.0.0.2:22             10.0.0.1:54321          ESTABLISHED 100/sshd
udp        0      0 0.0.0.0:68              0.0.0.0:*                           5/dhclient
        """.trimIndent())
    }
}
