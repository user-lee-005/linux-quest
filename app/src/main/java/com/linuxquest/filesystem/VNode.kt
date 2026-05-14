package com.linuxquest.filesystem

sealed class VNode {
    abstract val name: String
    abstract var permissions: Permissions
    abstract var owner: String
    abstract var group: String
    abstract var createdAt: Long
    abstract var modifiedAt: Long

    val isFile: Boolean get() = this is FileNode
    val isDirectory: Boolean get() = this is DirectoryNode
    val isSymlink: Boolean get() = this is SymlinkNode

    fun typeIndicator(): Char = when (this) {
        is FileNode -> '-'
        is DirectoryNode -> 'd'
        is SymlinkNode -> 'l'
    }

    fun modeString(): String = "${typeIndicator()}${permissions.toSymbolic()}"
}

data class FileNode(
    override val name: String,
    var content: ByteArray = ByteArray(0),
    override var permissions: Permissions = Permissions.DEFAULT_FILE,
    override var owner: String = "root",
    override var group: String = "root",
    override var createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis()
) : VNode() {

    val size: Long get() = content.size.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileNode) return false
        return name == other.name &&
            content.contentEquals(other.content) &&
            permissions == other.permissions &&
            owner == other.owner &&
            group == other.group
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + permissions.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + group.hashCode()
        return result
    }

    fun deepCopy(): FileNode = copy(content = content.copyOf())
}

data class DirectoryNode(
    override val name: String,
    val children: MutableMap<String, VNode> = mutableMapOf(),
    override var permissions: Permissions = Permissions.DEFAULT_DIRECTORY,
    override var owner: String = "root",
    override var group: String = "root",
    override var createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis()
) : VNode() {

    val size: Long get() = 4096L

    fun addChild(node: VNode) {
        children[node.name] = node
    }

    fun removeChild(name: String): VNode? = children.remove(name)

    fun getChild(name: String): VNode? = children[name]

    fun hasChild(name: String): Boolean = children.containsKey(name)

    fun deepCopy(): DirectoryNode {
        val copiedChildren = mutableMapOf<String, VNode>()
        for ((childName, child) in children) {
            copiedChildren[childName] = when (child) {
                is FileNode -> child.deepCopy()
                is DirectoryNode -> child.deepCopy()
                is SymlinkNode -> child.copy()
            }
        }
        return copy(children = copiedChildren)
    }
}

data class SymlinkNode(
    override val name: String,
    var target: String,
    override var permissions: Permissions = Permissions.DEFAULT_SYMLINK,
    override var owner: String = "root",
    override var group: String = "root",
    override var createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis()
) : VNode()
