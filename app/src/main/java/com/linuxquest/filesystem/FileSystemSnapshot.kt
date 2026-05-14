package com.linuxquest.filesystem

import java.io.Serializable

data class FSSnapshot(
    val tree: SnapshotNode,
    val currentPath: String,
    val currentUser: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

sealed class SnapshotNode : Serializable {
    abstract val name: String
    abstract val permissions: Int
    abstract val owner: String
    abstract val group: String
    abstract val createdAt: Long
    abstract val modifiedAt: Long

    companion object {
        private const val serialVersionUID = 1L
    }
}

data class SnapshotFile(
    override val name: String,
    val content: ByteArray,
    override val permissions: Int,
    override val owner: String,
    override val group: String,
    override val createdAt: Long,
    override val modifiedAt: Long
) : SnapshotNode() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapshotFile) return false
        return name == other.name &&
            content.contentEquals(other.content) &&
            permissions == other.permissions &&
            owner == other.owner &&
            group == other.group
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + permissions
        result = 31 * result + owner.hashCode()
        result = 31 * result + group.hashCode()
        return result
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

data class SnapshotDirectory(
    override val name: String,
    val children: List<SnapshotNode>,
    override val permissions: Int,
    override val owner: String,
    override val group: String,
    override val createdAt: Long,
    override val modifiedAt: Long
) : SnapshotNode() {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class SnapshotSymlink(
    override val name: String,
    val target: String,
    override val permissions: Int,
    override val owner: String,
    override val group: String,
    override val createdAt: Long,
    override val modifiedAt: Long
) : SnapshotNode() {
    companion object {
        private const val serialVersionUID = 1L
    }
}

object FileSystemSnapshot {

    fun captureSnapshot(vfs: VirtualFileSystem): FSSnapshot {
        val tree = captureNode(vfs.root)
        return FSSnapshot(
            tree = tree,
            currentPath = vfs.pwd(),
            currentUser = vfs.currentUser.name
        )
    }

    fun restoreSnapshot(snapshot: FSSnapshot): VirtualFileSystem {
        val vfs = VirtualFileSystem()
        // Clear the default root children and rebuild from snapshot
        vfs.root.children.clear()
        restoreNodeInto(vfs.root, snapshot.tree as SnapshotDirectory)
        vfs.root.permissions = Permissions.fromOctal(snapshot.tree.permissions)
        vfs.root.owner = snapshot.tree.owner
        vfs.root.group = snapshot.tree.group
        vfs.root.createdAt = snapshot.tree.createdAt
        vfs.root.modifiedAt = snapshot.tree.modifiedAt

        vfs.userManager.switchUser(snapshot.currentUser)
        vfs.cd(snapshot.currentPath)
        return vfs
    }

    private fun captureNode(node: VNode): SnapshotNode = when (node) {
        is FileNode -> SnapshotFile(
            name = node.name,
            content = node.content.copyOf(),
            permissions = node.permissions.toOctal(),
            owner = node.owner,
            group = node.group,
            createdAt = node.createdAt,
            modifiedAt = node.modifiedAt
        )
        is DirectoryNode -> SnapshotDirectory(
            name = node.name,
            children = node.children.values.map { captureNode(it) },
            permissions = node.permissions.toOctal(),
            owner = node.owner,
            group = node.group,
            createdAt = node.createdAt,
            modifiedAt = node.modifiedAt
        )
        is SymlinkNode -> SnapshotSymlink(
            name = node.name,
            target = node.target,
            permissions = node.permissions.toOctal(),
            owner = node.owner,
            group = node.group,
            createdAt = node.createdAt,
            modifiedAt = node.modifiedAt
        )
    }

    private fun restoreNodeInto(targetDir: DirectoryNode, snapshotDir: SnapshotDirectory) {
        for (child in snapshotDir.children) {
            val restored = when (child) {
                is SnapshotFile -> FileNode(
                    name = child.name,
                    content = child.content.copyOf(),
                    permissions = Permissions.fromOctal(child.permissions),
                    owner = child.owner,
                    group = child.group,
                    createdAt = child.createdAt,
                    modifiedAt = child.modifiedAt
                )
                is SnapshotDirectory -> {
                    val dir = DirectoryNode(
                        name = child.name,
                        permissions = Permissions.fromOctal(child.permissions),
                        owner = child.owner,
                        group = child.group,
                        createdAt = child.createdAt,
                        modifiedAt = child.modifiedAt
                    )
                    restoreNodeInto(dir, child)
                    dir
                }
                is SnapshotSymlink -> SymlinkNode(
                    name = child.name,
                    target = child.target,
                    permissions = Permissions.fromOctal(child.permissions),
                    owner = child.owner,
                    group = child.group,
                    createdAt = child.createdAt,
                    modifiedAt = child.modifiedAt
                )
            }
            targetDir.addChild(restored)
        }
    }
}
