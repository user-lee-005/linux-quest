package com.linuxquest.game

import java.security.MessageDigest

class PasswordSystem {

    companion object {
        private const val SALT = "LinuxQuest-v1-BanditStyle"
    }

    fun getPassword(levelId: Int): String {
        val seed = "$SALT-Level-$levelId"
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest(seed.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
