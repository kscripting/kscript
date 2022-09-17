package io.github.kscripting.kscript.shell

import arrow.core.Either
import arrow.core.left
import arrow.core.right

//Path representation for different OSes
data class OsPath(val osType: OsType, val pathType: PathType, val pathParts: List<String>, val pathSeparator: Char) {

    fun resolve(vararg pathParts: String): OsPath {
        return resolve(createOrThrow(osType, pathParts.joinToString(pathSeparator.toString())))
    }

    fun resolve(path: OsPath): OsPath {
        require(osType == path.osType) {
            "Paths from different OS's: '${this.osType.name}' path can not be resolved with '${path.osType.name}' path"
        }

        require(path.pathType != PathType.ABSOLUTE) {
            "Can not resolve absolute, relative or undefined path '${stringPath()}' using absolute path '${path.stringPath()}'"
        }

        val newPath = stringPath() + pathSeparator + path.stringPath()
        val newPathParts = buildList {
            addAll(pathParts)
            addAll(path.pathParts)
        }

        val normalizedPath = when (val result = normalize(newPath, newPathParts, pathType)) {
            is Either.Right -> result.value
            is Either.Left -> throw IllegalArgumentException(result.value)
        }

        return OsPath(osType, pathType, normalizedPath, pathSeparator)
    }

    //Not all conversions make sense: only Windows to CygWin and Msys and vice versa
    fun convert(targetOsType: OsType): OsPath {
        if (this.osType == targetOsType) {
            return this
        }

        if ((this.osType.isPosixLike() && targetOsType.isPosixLike()) || (this.osType.isWindowsLike() && targetOsType.isWindowsLike())) {
            return OsPath(targetOsType, pathType, pathParts, pathSeparator)
        }

        val toPosix = osType.isWindowsLike() && targetOsType.isPosixHostedOnWindows()
        val fromPosix = osType.isPosixHostedOnWindows() && targetOsType.isWindowsLike()

        require(toPosix || fromPosix) {
            "Only conversion between Windows and Posix hosted on Windows paths are supported"
        }

        val newParts = mutableListOf<String>()

        when {
            toPosix -> {
                val drive: String

                if (pathType == PathType.ABSOLUTE) {
                    drive = pathParts[0][0].lowercase()

                    newParts.add("/")

                    if (targetOsType == OsType.CYGWIN) {
                        newParts.add("cygdrive")
                        newParts.add(drive)
                    } else {
                        newParts.add(drive)
                    }

                    newParts.addAll(pathParts.subList(1, pathParts.size))
                } else {
                    newParts.addAll(pathParts)
                }
            }

            fromPosix -> {
                if (pathType == PathType.ABSOLUTE) {
                    if (osType == OsType.CYGWIN) {
                        newParts.add(pathParts[2] + ":")
                        newParts.addAll(pathParts.subList(3, pathParts.size))
                    } else {
                        newParts.add(pathParts[1] + ":")
                        newParts.addAll(pathParts.subList(2, pathParts.size))
                    }
                } else {
                    newParts.addAll(pathParts)
                }
            }

            else -> throw IllegalArgumentException("Invalid conversion: ${pathType.name} to ${targetOsType.name}")
        }

        return OsPath(targetOsType, pathType, newParts, resolvePathSeparator(targetOsType))
    }

    fun stringPath(): String {
        if (osType.isPosixLike() && pathParts.isNotEmpty() && pathParts[0] == "/") {
            return "/" + pathParts.subList(1, pathParts.size).joinToString(pathSeparator.toString()) { it }
        }

        return pathParts.joinToString(pathSeparator.toString()) { it }
    }

    override fun toString(): String = stringPath()

    companion object {
        //https://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names
        //The rule here is more strict than necessary, but it is at least good practice to follow such a rule.
        private val forbiddenCharacters = buildSet {
            add('<')
            add('>')
            add(':')
            add('"')
            add('|')
            add('?')
            add('*')
            for (i in 0 until 32) {
                add(i.toChar())
            }
        }

        private const val alphaChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        fun resolvePathSeparator(osType: OsType) = if (osType.isPosixLike()) {
            '/'
        } else {
            '\\'
        }

        fun createOrThrow(osType: OsType, root: String, vararg pathParts: String): OsPath {
            return when (val result = internalCreate(osType, root, *pathParts)) {
                is Either.Right -> result.value
                is Either.Left -> throw IllegalArgumentException(result.value)
            }
        }

        fun create(osType: OsType, root: String, vararg pathParts: String): OsPath? {
            return when (val result = internalCreate(osType, root, *pathParts)) {
                is Either.Right -> result.value
                is Either.Left -> null
            }
        }

        //Relaxed validation:
        //1. It doesn't matter if there is '/' or '\' used as path separator - both are treated he same
        //2. Duplicated or trailing slashes '/' and backslashes '\' are just ignored
        private fun internalCreate(osType: OsType, root: String, vararg pathParts: String): Either<String, OsPath> {
            val pathSeparatorCharacter = resolvePathSeparator(osType)

            val path = listOf(root, *pathParts).joinToString(pathSeparatorCharacter.toString())
            val pathPartsResolved = path.split('/', '\\').toMutableList()

            //Validate root element of path and find out if it is absolute or relative
            val rootElementSizeInInputPath: Int
            val pathType: PathType

            when {
                pathPartsResolved.isEmpty() -> {
                    pathType = PathType.UNDEFINED
                    rootElementSizeInInputPath = 0
                }

                pathPartsResolved[0] == "~" -> {
                    pathType = PathType.ABSOLUTE
                    rootElementSizeInInputPath = 1
                }

                pathPartsResolved[0] == ".." || pathPartsResolved[0] == "." -> {
                    pathType = PathType.RELATIVE
                    rootElementSizeInInputPath = pathPartsResolved[0].length
                }

                osType.isPosixLike() && path.startsWith("/") -> {
                    //After split first element is empty for absolute paths on Linux; assigning correct value below
                    pathPartsResolved.add(0, "/")
                    pathType = PathType.ABSOLUTE
                    rootElementSizeInInputPath = 1
                }

                osType.isWindowsLike() && pathPartsResolved[0].length == 2 && pathPartsResolved[0][1] == ':' && alphaChars.contains(
                    pathPartsResolved[0][0]
                ) -> {
                    pathType = PathType.ABSOLUTE
                    rootElementSizeInInputPath = 2
                }

                else -> {
                    //This is undefined path
                    pathType = PathType.UNDEFINED
                    rootElementSizeInInputPath = 0
                }
            }

            val forbiddenCharacter =
                path.substring(rootElementSizeInInputPath).find { forbiddenCharacters.contains(it) }

            if (forbiddenCharacter != null) {
                return "Invalid character '$forbiddenCharacter' in path '$path'".left()
            }

            //Remove empty path parts - there were duplicated or trailing slashes / backslashes in initial path
            pathPartsResolved.removeAll { it.isEmpty() }

            val normalizedPath = when (val result = normalize(path, pathPartsResolved, pathType)) {
                is Either.Right -> result.value
                is Either.Left -> return result.value.left()
            }

            return OsPath(osType, pathType, normalizedPath, pathSeparatorCharacter).right()
        }

        fun normalize(path: String, pathParts: List<String>, pathType: PathType): Either<String, List<String>> {
            //Relative:
            // ./../ --> ../
            // ./a/../ --> ./
            // ./a/ --> ./a
            // ../a --> ../a
            // ../../a --> ../../a

            //Absolute:
            // /../ --> invalid (above root)
            // /a/../ --> /

            val newParts = mutableListOf<String>()
            var index = 0

            if (pathType != PathType.UNDEFINED) {
                newParts.add(pathParts[0])
                index = 1
            }

            while (index < pathParts.size) {
                if (pathParts[index] == ".") {
                    //Just skip . without adding it to newParts
                } else if (pathParts[index] == "..") {

                    if (pathType == PathType.ABSOLUTE && newParts.size == 1) {
                        return "Path after normalization goes beyond root element: '$path'".left()
                    }

                    if (newParts.size > 0) {
                        when (newParts.last()) {
                            "." -> {
                                //It's the first element - other dots should be already removed before
                                newParts.removeAt(newParts.size - 1)
                                newParts.add("..")
                            }

                            ".." -> {
                                newParts.add("..")
                            }

                            else -> {
                                newParts.removeAt(newParts.size - 1)
                            }
                        }
                    } else {
                        newParts.add("..")
                    }
                } else {
                    newParts.add(pathParts[index])
                }

                index += 1
            }

            return newParts.right()
        }
    }
}
