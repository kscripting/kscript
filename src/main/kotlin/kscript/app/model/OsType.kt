package kscript.app.model

enum class OsType(val osName: String) {
    LINUX("linux-gnu"), DARWIN("darwin"), WINDOWS("windows"), CYGWIN("cygwin"), MSYS("msys"), FREEBSD("freebsd");

    fun isUnixLike() = (this == LINUX || this == DARWIN || this == FREEBSD)
    fun isWindowsLike() = (this == WINDOWS)
    fun isUnixHostedOnWindows() = (this == CYGWIN || this == MSYS)

    companion object {
        fun findOrThrow(name: String) =
            values().find { name.contains(it.osName, true) } ?: throw IllegalArgumentException("Unsupported OS: $name")
    }
}
