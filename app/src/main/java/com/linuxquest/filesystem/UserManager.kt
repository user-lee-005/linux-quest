package com.linuxquest.filesystem

data class VUser(
    val name: String,
    val uid: Int,
    val gid: Int,
    val home: String,
    val shell: String = "/bin/bash"
)

data class VGroup(
    val name: String,
    val gid: Int,
    val members: MutableList<String> = mutableListOf()
)

class UserManager {

    private val users = mutableMapOf<String, VUser>()
    private val groups = mutableMapOf<String, VGroup>()

    @Volatile
    var currentUser: VUser
        private set

    init {
        addGroup(VGroup("root", 0, mutableListOf("root")))
        addGroup(VGroup("users", 100))

        addUser(VUser("root", 0, 0, "/root"))
        addUser(VUser("guest", 65534, 100, "/home/guest"))

        for (i in 0..55) {
            val name = "bandit$i"
            val uid = 1000 + i
            val gid = 1000 + i
            addGroup(VGroup(name, gid, mutableListOf(name)))
            addUser(VUser(name, uid, gid, "/home/$name"))
            groups["users"]!!.members.add(name)
        }

        currentUser = users["bandit0"]!!
    }

    private fun addUser(user: VUser) {
        users[user.name] = user
    }

    private fun addGroup(group: VGroup) {
        groups[group.name] = group
    }

    fun getUser(name: String): VUser? = users[name]

    fun getGroup(name: String): VGroup? = groups[name]

    fun getAllUsers(): List<VUser> = users.values.toList()

    fun getAllGroups(): List<VGroup> = groups.values.toList()

    fun getUserGroups(username: String): List<String> {
        val result = mutableListOf<String>()
        val user = users[username] ?: return result
        // Primary group
        val primaryGroup = groups.values.find { it.gid == user.gid }
        if (primaryGroup != null) result.add(primaryGroup.name)
        // Secondary groups
        for (group in groups.values) {
            if (username in group.members && group.name !in result) {
                result.add(group.name)
            }
        }
        return result
    }

    fun switchUser(name: String): Boolean {
        val user = users[name] ?: return false
        currentUser = user
        return true
    }

    fun userExists(name: String): Boolean = users.containsKey(name)

    fun groupExists(name: String): Boolean = groups.containsKey(name)

    fun generatePasswdEntries(): String = buildString {
        for (user in users.values.sortedBy { it.uid }) {
            val primaryGroup = groups.values.find { it.gid == user.gid }?.name ?: user.name
            appendLine("${user.name}:x:${user.uid}:${user.gid}:${user.name}:${user.home}:${user.shell}")
        }
    }

    fun generateShadowEntries(): String = buildString {
        for (user in users.values.sortedBy { it.uid }) {
            appendLine("${user.name}:*:19000:0:99999:7:::")
        }
    }

    fun generateGroupEntries(): String = buildString {
        for (group in groups.values.sortedBy { it.gid }) {
            appendLine("${group.name}:x:${group.gid}:${group.members.joinToString(",")}")
        }
    }
}
