package com.linuxquest.filesystem

enum class AccessAction { READ, WRITE, EXECUTE }

data class Permissions(
    val ownerRead: Boolean = false,
    val ownerWrite: Boolean = false,
    val ownerExecute: Boolean = false,
    val groupRead: Boolean = false,
    val groupWrite: Boolean = false,
    val groupExecute: Boolean = false,
    val otherRead: Boolean = false,
    val otherWrite: Boolean = false,
    val otherExecute: Boolean = false,
    val suid: Boolean = false,
    val sgid: Boolean = false,
    val sticky: Boolean = false
) {

    fun toOctal(): Int {
        val special = (if (suid) 4 else 0) or (if (sgid) 2 else 0) or (if (sticky) 1 else 0)
        val owner = (if (ownerRead) 4 else 0) or (if (ownerWrite) 2 else 0) or (if (ownerExecute) 1 else 0)
        val group = (if (groupRead) 4 else 0) or (if (groupWrite) 2 else 0) or (if (groupExecute) 1 else 0)
        val other = (if (otherRead) 4 else 0) or (if (otherWrite) 2 else 0) or (if (otherExecute) 1 else 0)
        return special * 1000 + owner * 100 + group * 10 + other
    }

    fun toSymbolic(): String = buildString {
        append(if (ownerRead) 'r' else '-')
        append(if (ownerWrite) 'w' else '-')
        append(
            when {
                suid && ownerExecute -> 's'
                suid -> 'S'
                ownerExecute -> 'x'
                else -> '-'
            }
        )
        append(if (groupRead) 'r' else '-')
        append(if (groupWrite) 'w' else '-')
        append(
            when {
                sgid && groupExecute -> 's'
                sgid -> 'S'
                groupExecute -> 'x'
                else -> '-'
            }
        )
        append(if (otherRead) 'r' else '-')
        append(if (otherWrite) 'w' else '-')
        append(
            when {
                sticky && otherExecute -> 't'
                sticky -> 'T'
                otherExecute -> 'x'
                else -> '-'
            }
        )
    }

    override fun toString(): String = toSymbolic()

    fun canRead(user: String, userGroups: List<String>, nodeOwner: String, nodeGroup: String): Boolean =
        checkAccess(user, userGroups, nodeOwner, nodeGroup, AccessAction.READ)

    fun canWrite(user: String, userGroups: List<String>, nodeOwner: String, nodeGroup: String): Boolean =
        checkAccess(user, userGroups, nodeOwner, nodeGroup, AccessAction.WRITE)

    fun canExecute(user: String, userGroups: List<String>, nodeOwner: String, nodeGroup: String): Boolean =
        checkAccess(user, userGroups, nodeOwner, nodeGroup, AccessAction.EXECUTE)

    fun checkAccess(
        user: String,
        userGroups: List<String>,
        nodeOwner: String,
        nodeGroup: String,
        action: AccessAction
    ): Boolean {
        if (user == "root") return true

        if (user == nodeOwner) {
            return when (action) {
                AccessAction.READ -> ownerRead
                AccessAction.WRITE -> ownerWrite
                AccessAction.EXECUTE -> ownerExecute
            }
        }

        if (nodeGroup in userGroups) {
            return when (action) {
                AccessAction.READ -> groupRead
                AccessAction.WRITE -> groupWrite
                AccessAction.EXECUTE -> groupExecute
            }
        }

        return when (action) {
            AccessAction.READ -> otherRead
            AccessAction.WRITE -> otherWrite
            AccessAction.EXECUTE -> otherExecute
        }
    }

    companion object {
        val DEFAULT_FILE = fromOctal(644)
        val DEFAULT_DIRECTORY = fromOctal(755)
        val DEFAULT_SYMLINK = fromOctal(777)

        fun fromOctal(octal: Int): Permissions {
            val digits = "%04d".format(octal)
            require(digits.length == 4 && digits.all { it in '0'..'7' }) {
                "Invalid octal permission: $octal"
            }
            val special = digits[0].digitToInt()
            val owner = digits[1].digitToInt()
            val group = digits[2].digitToInt()
            val other = digits[3].digitToInt()

            return Permissions(
                ownerRead = owner and 4 != 0,
                ownerWrite = owner and 2 != 0,
                ownerExecute = owner and 1 != 0,
                groupRead = group and 4 != 0,
                groupWrite = group and 2 != 0,
                groupExecute = group and 1 != 0,
                otherRead = other and 4 != 0,
                otherWrite = other and 2 != 0,
                otherExecute = other and 1 != 0,
                suid = special and 4 != 0,
                sgid = special and 2 != 0,
                sticky = special and 1 != 0
            )
        }

        fun fromSymbolic(symbolic: String): Permissions {
            require(symbolic.length == 9) { "Symbolic permission must be 9 characters: $symbolic" }
            return Permissions(
                ownerRead = symbolic[0] == 'r',
                ownerWrite = symbolic[1] == 'w',
                ownerExecute = symbolic[2] == 'x' || symbolic[2] == 's',
                groupRead = symbolic[3] == 'r',
                groupWrite = symbolic[4] == 'w',
                groupExecute = symbolic[5] == 'x' || symbolic[5] == 's',
                otherRead = symbolic[6] == 'r',
                otherWrite = symbolic[7] == 'w',
                otherExecute = symbolic[8] == 'x' || symbolic[8] == 't',
                suid = symbolic[2] == 's' || symbolic[2] == 'S',
                sgid = symbolic[5] == 's' || symbolic[5] == 'S',
                sticky = symbolic[8] == 't' || symbolic[8] == 'T'
            )
        }
    }
}
