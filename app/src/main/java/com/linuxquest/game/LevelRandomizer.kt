package com.linuxquest.game

import kotlin.random.Random

class LevelRandomizer(seed: Long) {

    private val rng = Random(seed)

    private val fileNouns = listOf(
        "report", "notes", "data", "backup", "config", "memo", "draft",
        "archive", "journal", "ledger", "manifest", "roster", "catalog",
        "digest", "index", "records", "summary", "transcript", "log",
        "bulletin", "dispatch", "invoice", "receipt", "ticket", "voucher",
        "schedule", "blueprint", "schema", "spec", "protocol", "handbook"
    )

    private val dirNouns = listOf(
        "projects", "documents", "storage", "workspace", "vault", "archive",
        "files", "resources", "assets", "library", "depot", "repository",
        "cache", "staging", "incoming", "outgoing", "private", "shared",
        "temp", "backups", "exports", "imports", "uploads", "downloads",
        "logs", "records", "data", "configs", "scripts", "tools"
    )

    private val adjectives = listOf(
        "old", "new", "secret", "hidden", "final", "draft", "temp",
        "legacy", "beta", "alpha", "stable", "latest", "archived",
        "encrypted", "compressed", "raw", "processed", "pending",
        "approved", "rejected", "urgent", "critical", "normal"
    )

    private val extensions = listOf(
        ".txt", ".log", ".dat", ".conf", ".bak", ".tmp", ".md", ".csv"
    )

    private val decoyContents = listOf(
        "Nothing useful here.",
        "This is not what you're looking for.",
        "Keep searching!",
        "Try another file.",
        "Access denied... just kidding, but no password here.",
        "Almost! But not quite.",
        "The password is definitely not in this file.",
        "Move along, nothing to see here.",
        "You're getting warmer... or maybe not.",
        "Better luck in the next file!",
        "Nice try, but this is a decoy.",
        "This file contains no secrets.",
        "TODO: put something important here later",
        "DEBUG: test data - ignore",
        "DEPRECATED: old data, do not use"
    )

    fun randomFileName(): String {
        val noun = fileNouns[rng.nextInt(fileNouns.size)]
        return if (rng.nextBoolean()) {
            val adj = adjectives[rng.nextInt(adjectives.size)]
            "${adj}_$noun"
        } else {
            noun
        }
    }

    fun randomFileNameWithExt(): String {
        return randomFileName() + extensions[rng.nextInt(extensions.size)]
    }

    fun randomDirName(): String {
        return dirNouns[rng.nextInt(dirNouns.size)]
    }

    fun randomHiddenFileName(): String {
        return ".${randomFileName()}"
    }

    fun randomPath(depth: Int = 2): String {
        val parts = (1..depth).map { randomDirName() }
        return parts.joinToString("/")
    }

    fun randomDecoyContent(): String {
        return decoyContents[rng.nextInt(decoyContents.size)]
    }

    fun randomDecoyFiles(count: Int): List<Pair<String, String>> {
        return (1..count).map {
            randomFileNameWithExt() to randomDecoyContent()
        }
    }

    fun randomFileNameWithKeyword(keyword: String): String {
        return if (rng.nextBoolean()) {
            val adj = adjectives[rng.nextInt(adjectives.size)]
            "${adj}_$keyword"
        } else {
            val noun = fileNouns[rng.nextInt(fileNouns.size)]
            "${keyword}_$noun"
        }
    }

    fun randomFileNameWithKeywordAndExt(keyword: String): String {
        return randomFileNameWithKeyword(keyword) + extensions[rng.nextInt(extensions.size)]
    }

    fun randomHiddenFileNameWithKeyword(keyword: String): String {
        return ".${randomFileNameWithKeyword(keyword)}"
    }

    fun nextInt(bound: Int): Int = rng.nextInt(bound)
    fun nextInt(from: Int, until: Int): Int = rng.nextInt(from, until)
}
