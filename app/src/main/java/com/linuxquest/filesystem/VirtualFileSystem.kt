package com.linuxquest.filesystem

sealed class VfsException(message: String) : Exception(message)
class PermissionDeniedException(path: String, action: String) : VfsException("Permission denied: $action $path")
class FileNotFoundException(path: String) : VfsException("No such file or directory: $path")
class NotADirectoryException(path: String) : VfsException("Not a directory: $path")
class IsADirectoryException(path: String) : VfsException("Is a directory: $path")
class FileExistsException(path: String) : VfsException("File exists: $path")
class DirectoryNotEmptyException(path: String) : VfsException("Directory not empty: $path")
class SymlinkLoopException(path: String) : VfsException("Too many levels of symbolic links: $path")

class VirtualFileSystem(
    val userManager: UserManager = UserManager()
) {
    val root: DirectoryNode = DirectoryNode("/", permissions = Permissions.fromOctal(755))

    @Volatile
    var currentPath: String = "/home/bandit0"
        private set

    private val symlinkDepthLimit = 20
    private val lock = Any()

    val currentUser: VUser get() = userManager.currentUser

    init {
        initializeFileSystem()
    }

    private fun initializeFileSystem() {
        val standardDirs = listOf(
            "/home" to 755, "/tmp" to 1777, "/etc" to 755, "/var" to 755,
            "/usr" to 755, "/usr/bin" to 755, "/usr/lib" to 755, "/usr/share" to 755,
            "/bin" to 755, "/sbin" to 755, "/opt" to 755, "/root" to 700,
            "/var/log" to 755, "/var/tmp" to 1777, "/dev" to 755, "/proc" to 555
        )

        for ((path, perm) in standardDirs) {
            createDirectoryInternal(path, Permissions.fromOctal(perm), "root", "root")
        }

        for (i in 0..55) {
            val name = "bandit$i"
            createDirectoryInternal("/home/$name", Permissions.fromOctal(750), name, name)
        }
        createDirectoryInternal("/home/guest", Permissions.fromOctal(750), "guest", "users")

        val passwdContent = userManager.generatePasswdEntries().toByteArray()
        val shadowContent = userManager.generateShadowEntries().toByteArray()
        val groupContent = userManager.generateGroupEntries().toByteArray()

        createFileInternal("/etc/passwd", passwdContent, Permissions.fromOctal(644), "root", "root")
        createFileInternal("/etc/shadow", shadowContent, Permissions.fromOctal(640), "root", "root")
        createFileInternal("/etc/group", groupContent, Permissions.fromOctal(644), "root", "root")
        createFileInternal("/etc/hostname", "linuxquest\n".toByteArray(), Permissions.fromOctal(644), "root", "root")

        currentPath = userManager.currentUser.home
    }

    // --- Path resolution ---

    fun getAbsolutePath(path: String): String = synchronized(lock) {
        normalizePath(resolvePath(path))
    }

    private fun resolvePath(path: String): String {
        val expanded = when {
            path.startsWith("~/") -> "${userManager.currentUser.home}/${path.substring(2)}"
            path == "~" -> userManager.currentUser.home
            path.startsWith("~") -> {
                val user = path.substring(1).takeWhile { it != '/' }
                val rest = path.substring(1 + user.length)
                val vUser = userManager.getUser(user)
                if (vUser != null) "${vUser.home}$rest" else path
            }
            else -> path
        }
        return if (expanded.startsWith("/")) expanded else "$currentPath/$expanded"
    }

    private fun normalizePath(path: String): String {
        val parts = path.split("/").filter { it.isNotEmpty() }
        val resolved = mutableListOf<String>()
        for (part in parts) {
            when (part) {
                "." -> { /* skip */ }
                ".." -> { if (resolved.isNotEmpty()) resolved.removeLast() }
                else -> resolved.add(part)
            }
        }
        return "/" + resolved.joinToString("/")
    }

    private fun splitPath(path: String): List<String> {
        val normalized = normalizePath(resolvePath(path))
        return if (normalized == "/") emptyList() else normalized.removePrefix("/").split("/")
    }

    private fun parentPath(path: String): String {
        val normalized = normalizePath(resolvePath(path))
        val lastSlash = normalized.lastIndexOf('/')
        return if (lastSlash <= 0) "/" else normalized.substring(0, lastSlash)
    }

    private fun baseName(path: String): String {
        val normalized = normalizePath(resolvePath(path))
        return normalized.substringAfterLast('/')
    }

    // --- Node traversal ---

    private fun traverseTo(path: String, followSymlinks: Boolean = true): VNode? {
        return traverseToImpl(normalizePath(resolvePath(path)), followSymlinks, 0)
    }

    private fun traverseToImpl(absolutePath: String, followSymlinks: Boolean, depth: Int): VNode? {
        if (depth > symlinkDepthLimit) throw SymlinkLoopException(absolutePath)
        if (absolutePath == "/") return root

        val parts = absolutePath.removePrefix("/").split("/")
        var current: VNode = root

        for ((index, part) in parts.withIndex()) {
            when (current) {
                is DirectoryNode -> {
                    val child = current.getChild(part) ?: return null
                    current = if (child is SymlinkNode && (followSymlinks || index < parts.lastIndex)) {
                        val resolvedTarget = resolveSymlinkTarget(child.target, absolutePath.substringBeforeLast("/$part"))
                        traverseToImpl(resolvedTarget, true, depth + 1) ?: return null
                    } else {
                        child
                    }
                }
                is SymlinkNode -> {
                    val resolvedTarget = resolveSymlinkTarget(current.target, absolutePath)
                    current = traverseToImpl(resolvedTarget, true, depth + 1) ?: return null
                    // Continue resolving remaining parts
                    if (current !is DirectoryNode) return null
                    val child = current.getChild(part) ?: return null
                    current = child
                }
                is FileNode -> return null
            }
        }
        return current
    }

    private fun resolveSymlinkTarget(target: String, contextPath: String): String {
        return if (target.startsWith("/")) {
            normalizePath(target)
        } else {
            normalizePath("$contextPath/$target")
        }
    }

    private fun getParentDirectory(path: String): DirectoryNode? {
        val parent = parentPath(path)
        val node = traverseTo(parent)
        return node as? DirectoryNode
    }

    // --- Permission checks ---

    private fun checkReadPermission(node: VNode, path: String) {
        val groups = userManager.getUserGroups(currentUser.name)
        if (!node.permissions.canRead(currentUser.name, groups, node.owner, node.group)) {
            throw PermissionDeniedException(path, "read")
        }
    }

    private fun checkWritePermission(node: VNode, path: String) {
        val groups = userManager.getUserGroups(currentUser.name)
        if (!node.permissions.canWrite(currentUser.name, groups, node.owner, node.group)) {
            throw PermissionDeniedException(path, "write")
        }
    }

    private fun checkExecutePermission(node: VNode, path: String) {
        val groups = userManager.getUserGroups(currentUser.name)
        if (!node.permissions.canExecute(currentUser.name, groups, node.owner, node.group)) {
            throw PermissionDeniedException(path, "execute")
        }
    }

    private fun checkTraversePermissions(path: String) {
        val parts = splitPath(path)
        var traversed = ""
        var current: VNode = root
        for (part in parts) {
            if (current is DirectoryNode) {
                checkExecutePermission(current, traversed.ifEmpty { "/" })
                current = current.getChild(part) ?: return
                traversed = "$traversed/$part"
            } else {
                return
            }
        }
    }

    // --- Public operations ---

    fun getNode(path: String): VNode? = synchronized(lock) {
        traverseTo(path)
    }

    fun exists(path: String): Boolean = synchronized(lock) {
        traverseTo(path) != null
    }

    fun isFile(path: String): Boolean = synchronized(lock) {
        traverseTo(path) is FileNode
    }

    fun isDirectory(path: String): Boolean = synchronized(lock) {
        traverseTo(path) is DirectoryNode
    }

    fun isSymlink(path: String): Boolean = synchronized(lock) {
        traverseTo(path, followSymlinks = false) is SymlinkNode
    }

    fun listDirectory(path: String): List<VNode> = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        checkTraversePermissions(absPath)
        val node = traverseTo(path) ?: throw FileNotFoundException(absPath)
        if (node !is DirectoryNode) throw NotADirectoryException(absPath)
        checkReadPermission(node, absPath)
        node.children.values.toList().sortedBy { it.name }
    }

    fun readFile(path: String): ByteArray = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        checkTraversePermissions(absPath)
        val node = traverseTo(path) ?: throw FileNotFoundException(absPath)
        if (node is DirectoryNode) throw IsADirectoryException(absPath)
        checkReadPermission(node, absPath)
        when (node) {
            is FileNode -> node.content.copyOf()
            is SymlinkNode -> throw FileNotFoundException(absPath)
            is DirectoryNode -> throw IsADirectoryException(absPath)
        }
    }

    fun readFileText(path: String): String = String(readFile(path))

    fun writeFile(path: String, content: ByteArray) = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        checkTraversePermissions(absPath)
        val node = traverseTo(path)
        if (node == null) {
            createFile(path, content)
            return@synchronized
        }
        if (node is DirectoryNode) throw IsADirectoryException(absPath)
        if (node !is FileNode) throw FileNotFoundException(absPath)
        checkWritePermission(node, absPath)
        node.content = content.copyOf()
        node.modifiedAt = System.currentTimeMillis()
    }

    fun writeFileText(path: String, content: String) = writeFile(path, content.toByteArray())

    fun createFile(
        path: String,
        content: ByteArray = ByteArray(0),
        permissions: Permissions = Permissions.DEFAULT_FILE
    ) = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        val parentDir = getParentDirectory(absPath)
            ?: throw FileNotFoundException(parentPath(absPath))
        checkWritePermission(parentDir, parentPath(absPath))

        val name = baseName(absPath)
        if (parentDir.hasChild(name)) throw FileExistsException(absPath)

        val now = System.currentTimeMillis()
        parentDir.addChild(
            FileNode(
                name = name,
                content = content.copyOf(),
                permissions = permissions,
                owner = currentUser.name,
                group = userManager.getUserGroups(currentUser.name).firstOrNull() ?: currentUser.name,
                createdAt = now,
                modifiedAt = now
            )
        )
        parentDir.modifiedAt = now
    }

    fun createDirectory(path: String, permissions: Permissions = Permissions.DEFAULT_DIRECTORY) = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        val parentDir = getParentDirectory(absPath)
            ?: throw FileNotFoundException(parentPath(absPath))
        checkWritePermission(parentDir, parentPath(absPath))

        val name = baseName(absPath)
        if (parentDir.hasChild(name)) throw FileExistsException(absPath)

        val now = System.currentTimeMillis()
        parentDir.addChild(
            DirectoryNode(
                name = name,
                permissions = permissions,
                owner = currentUser.name,
                group = userManager.getUserGroups(currentUser.name).firstOrNull() ?: currentUser.name,
                createdAt = now,
                modifiedAt = now
            )
        )
        parentDir.modifiedAt = now
    }

    fun delete(path: String, recursive: Boolean = false) = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        if (absPath == "/") throw PermissionDeniedException("/", "delete")

        val parentDir = getParentDirectory(absPath)
            ?: throw FileNotFoundException(parentPath(absPath))
        checkWritePermission(parentDir, parentPath(absPath))

        val name = baseName(absPath)
        val node = parentDir.getChild(name) ?: throw FileNotFoundException(absPath)

        if (node is DirectoryNode && node.children.isNotEmpty() && !recursive) {
            throw DirectoryNotEmptyException(absPath)
        }

        // Sticky bit check: in /tmp-like dirs, only owner or root can delete
        if (parentDir.permissions.sticky && currentUser.name != "root" &&
            currentUser.name != node.owner && currentUser.name != parentDir.owner
        ) {
            throw PermissionDeniedException(absPath, "delete")
        }

        parentDir.removeChild(name)
        parentDir.modifiedAt = System.currentTimeMillis()
    }

    fun copy(src: String, dst: String) = synchronized(lock) {
        val srcAbsPath = normalizePath(resolvePath(src))
        val dstAbsPath = normalizePath(resolvePath(dst))
        checkTraversePermissions(srcAbsPath)

        val srcNode = traverseTo(src) ?: throw FileNotFoundException(srcAbsPath)
        checkReadPermission(srcNode, srcAbsPath)

        val dstNode = traverseTo(dst)
        val (targetParent, targetName) = if (dstNode is DirectoryNode) {
            dstNode to baseName(srcAbsPath)
        } else {
            val parent = getParentDirectory(dstAbsPath)
                ?: throw FileNotFoundException(parentPath(dstAbsPath))
            parent to baseName(dstAbsPath)
        }

        checkWritePermission(targetParent, parentPath(dstAbsPath))

        val now = System.currentTimeMillis()
        val copied = when (srcNode) {
            is FileNode -> srcNode.deepCopy().copy(
                name = targetName,
                owner = currentUser.name,
                group = userManager.getUserGroups(currentUser.name).firstOrNull() ?: currentUser.name,
                createdAt = now,
                modifiedAt = now
            )
            is DirectoryNode -> srcNode.deepCopy().copy(
                name = targetName,
                owner = currentUser.name,
                group = userManager.getUserGroups(currentUser.name).firstOrNull() ?: currentUser.name,
                createdAt = now,
                modifiedAt = now
            )
            is SymlinkNode -> srcNode.copy(
                name = targetName,
                owner = currentUser.name,
                group = userManager.getUserGroups(currentUser.name).firstOrNull() ?: currentUser.name,
                createdAt = now,
                modifiedAt = now
            )
        }
        targetParent.addChild(copied)
        targetParent.modifiedAt = now
    }

    fun move(src: String, dst: String) = synchronized(lock) {
        val srcAbsPath = normalizePath(resolvePath(src))
        val dstAbsPath = normalizePath(resolvePath(dst))

        if (srcAbsPath == "/") throw PermissionDeniedException("/", "move")

        val srcParent = getParentDirectory(srcAbsPath)
            ?: throw FileNotFoundException(parentPath(srcAbsPath))
        checkWritePermission(srcParent, parentPath(srcAbsPath))

        val srcName = baseName(srcAbsPath)
        val srcNode = srcParent.getChild(srcName) ?: throw FileNotFoundException(srcAbsPath)

        val dstNode = traverseTo(dst)
        val (targetParent, targetName) = if (dstNode is DirectoryNode) {
            dstNode to srcName
        } else {
            val parent = getParentDirectory(dstAbsPath)
                ?: throw FileNotFoundException(parentPath(dstAbsPath))
            parent to baseName(dstAbsPath)
        }

        checkWritePermission(targetParent, parentPath(dstAbsPath))

        // Sticky bit check on source directory
        if (srcParent.permissions.sticky && currentUser.name != "root" &&
            currentUser.name != srcNode.owner && currentUser.name != srcParent.owner
        ) {
            throw PermissionDeniedException(srcAbsPath, "move")
        }

        srcParent.removeChild(srcName)
        srcParent.modifiedAt = System.currentTimeMillis()

        val movedNode = when (srcNode) {
            is FileNode -> srcNode.copy(name = targetName)
            is DirectoryNode -> srcNode.copy(name = targetName)
            is SymlinkNode -> srcNode.copy(name = targetName)
        }
        targetParent.addChild(movedNode)
        targetParent.modifiedAt = System.currentTimeMillis()
    }

    fun createSymlink(linkPath: String, targetPath: String) = synchronized(lock) {
        val absLinkPath = normalizePath(resolvePath(linkPath))
        val parentDir = getParentDirectory(absLinkPath)
            ?: throw FileNotFoundException(parentPath(absLinkPath))
        checkWritePermission(parentDir, parentPath(absLinkPath))

        val name = baseName(absLinkPath)
        if (parentDir.hasChild(name)) throw FileExistsException(absLinkPath)

        val now = System.currentTimeMillis()
        parentDir.addChild(
            SymlinkNode(
                name = name,
                target = targetPath,
                owner = currentUser.name,
                group = userManager.getUserGroups(currentUser.name).firstOrNull() ?: currentUser.name,
                createdAt = now,
                modifiedAt = now
            )
        )
        parentDir.modifiedAt = now
    }

    fun resolveSymlink(path: String, maxDepth: Int = symlinkDepthLimit): String = synchronized(lock) {
        resolveSymlinkImpl(path, 0, maxDepth)
    }

    private fun resolveSymlinkImpl(path: String, depth: Int, maxDepth: Int): String {
        if (depth > maxDepth) throw SymlinkLoopException(path)
        val absPath = normalizePath(resolvePath(path))
        val node = traverseTo(path, followSymlinks = false) ?: throw FileNotFoundException(absPath)
        return if (node is SymlinkNode) {
            val resolved = resolveSymlinkTarget(node.target, parentPath(absPath))
            resolveSymlinkImpl(resolved, depth + 1, maxDepth)
        } else {
            absPath
        }
    }

    fun chmod(path: String, permissions: Permissions) = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        val node = traverseTo(path) ?: throw FileNotFoundException(absPath)
        if (currentUser.name != "root" && currentUser.name != node.owner) {
            throw PermissionDeniedException(absPath, "chmod")
        }
        node.permissions = permissions
        node.modifiedAt = System.currentTimeMillis()
    }

    fun chown(path: String, owner: String? = null, group: String? = null) = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        if (currentUser.name != "root") throw PermissionDeniedException(absPath, "chown")
        val node = traverseTo(path) ?: throw FileNotFoundException(absPath)
        if (owner != null) {
            if (!userManager.userExists(owner)) throw FileNotFoundException("user '$owner'")
            node.owner = owner
        }
        if (group != null) {
            if (!userManager.groupExists(group)) throw FileNotFoundException("group '$group'")
            node.group = group
        }
        node.modifiedAt = System.currentTimeMillis()
    }

    fun cd(path: String) = synchronized(lock) {
        val absPath = normalizePath(resolvePath(path))
        val node = traverseTo(path) ?: throw FileNotFoundException(absPath)
        if (node !is DirectoryNode) throw NotADirectoryException(absPath)
        checkExecutePermission(node, absPath)
        currentPath = absPath
    }

    fun pwd(): String = synchronized(lock) { currentPath }

    fun switchUser(username: String): Boolean = synchronized(lock) {
        val success = userManager.switchUser(username)
        if (success) {
            currentPath = userManager.currentUser.home
            if (!exists(currentPath)) {
                currentPath = "/"
            }
        }
        success
    }

    // --- Internal helpers (no lock, no permission checks) for init ---

    private fun createDirectoryInternal(
        path: String,
        permissions: Permissions,
        owner: String,
        group: String
    ) {
        val parts = path.removePrefix("/").split("/")
        var current = root
        for (part in parts) {
            val existing = current.getChild(part)
            current = if (existing is DirectoryNode) {
                existing
            } else {
                val now = System.currentTimeMillis()
                val newDir = DirectoryNode(
                    name = part,
                    permissions = permissions,
                    owner = owner,
                    group = group,
                    createdAt = now,
                    modifiedAt = now
                )
                current.addChild(newDir)
                newDir
            }
        }
        current.permissions = permissions
        current.owner = owner
        current.group = group
    }

    private fun createFileInternal(
        path: String,
        content: ByteArray,
        permissions: Permissions,
        owner: String,
        group: String
    ) {
        val parentParts = path.removePrefix("/").split("/")
        val fileName = parentParts.last()
        val dirParts = parentParts.dropLast(1)

        var current = root
        for (part in dirParts) {
            current = current.getChild(part) as? DirectoryNode ?: return
        }

        val now = System.currentTimeMillis()
        current.addChild(
            FileNode(
                name = fileName,
                content = content.copyOf(),
                permissions = permissions,
                owner = owner,
                group = group,
                createdAt = now,
                modifiedAt = now
            )
        )
    }
}
